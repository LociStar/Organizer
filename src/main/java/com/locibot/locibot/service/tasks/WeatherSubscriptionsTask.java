package com.locibot.locibot.service.tasks;

import com.locibot.locibot.command.register.RegisterWeather;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.weather.HourlyWeatherForecastClass;
import discord4j.core.GatewayDiscordClient;
import net.aksingh.owmjapis.core.OWM;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;

public class WeatherSubscriptionsTask implements Task{

    private static final Logger LOGGER = LogUtil.getLogger(WeatherSubscriptionsTask.class, LogUtil.Category.TASK);
    private final GatewayDiscordClient gateway;

    public WeatherSubscriptionsTask(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Disposable schedule(Scheduler scheduler) {
        LOGGER.info("Scheduling weather subscriptions");//RegisterWeather.getDelay(), Duration.ofDays(1)
        //final Disposable task = Flux.interval(Duration.ofSeconds(10), Duration.ofMinutes(1), DEFAULT_SCHEDULER)
        return Flux.interval(RegisterWeather.getDelay(), Duration.ofDays(1), scheduler)
                .doOnNext(__ -> {
                    LOGGER.info("Sending weather subscriptions");
                    OWM owm = new OWM(CredentialManager.get(Credential.OPENWEATHERMAP_API_KEY));
                    owm.setUnit(OWM.Unit.METRIC);
                    DatabaseManager.getGuilds().getDBGuilds().forEach(dbGuild ->
                            dbGuild.getMembers().forEach(dbMember ->
                                    dbMember.getWeatherRegistered().forEach(city -> {

                                        //TODO: This is a inefficient implementation. This needs to be reworked.
                                        //1 sek delay for each City-API-Call
                                        long expectedTime = System.currentTimeMillis();
                                        long sleepTime = 1000;
                                        expectedTime += sleepTime;//Sample expectedTime += 1000; 1 second sleep
                                        while (System.currentTimeMillis() < expectedTime) {
                                            //Empty Loop
                                        }

                                        gateway.getUserById(dbMember.getId()).flatMap(user ->
                                                user.getPrivateChannel().flatMap(privateChannel ->
                                                        privateChannel.createMessage("Daily weather forecast of " + city + ":")
                                                                .then(Mono.just(new HourlyWeatherForecastClass(owm, city)).flatMap(hwfc ->
                                                                        privateChannel.createMessage(messageCreateSpec -> {
                                                                            try {
                                                                                byte[] bytes = hwfc.createHeatMap();
                                                                                if (bytes.length > 0)
                                                                                    messageCreateSpec.addFile("temperature.png",
                                                                                            new ByteArrayInputStream(bytes));
                                                                                else
                                                                                    messageCreateSpec.setContent("Please provide a real city ;)");
                                                                                bytes = hwfc.createRainMap();
                                                                                if (bytes.length > 0)
                                                                                    messageCreateSpec.addFile("rain.png",
                                                                                            new ByteArrayInputStream(bytes));
                                                                                else
                                                                                    messageCreateSpec.setContent("No City No Rain");
                                                                                LOGGER.info(city + " weather send to " + user.getUsername());
                                                                            } catch (IOException e) {
                                                                                e.printStackTrace();
                                                                            }
                                                                        })))))
                                                .subscribe(null, ExceptionHandler::handleUnknownError);
                                    })));
                    LOGGER.info("weather subscriptions send");
                }).subscribe(null, ExceptionHandler::handleUnknownError);
    }
}
