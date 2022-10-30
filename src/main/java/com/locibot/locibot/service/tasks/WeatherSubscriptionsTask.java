package com.locibot.locibot.service.tasks;

import com.locibot.locibot.command.register.RegisterWeather;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.weather.WeatherManager;
import com.locibot.locibot.utils.weather.WeatherMapManager;
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
import java.util.Objects;

public class WeatherSubscriptionsTask implements Task {

    private static final Logger LOGGER = LogUtil.getLogger(WeatherSubscriptionsTask.class, LogUtil.Category.TASK);
    private final GatewayDiscordClient gateway;

    public WeatherSubscriptionsTask(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean isEnabled() {
        return CredentialManager.get(Credential.OPENWEATHERMAP_API_KEY) != null;
    }

    @Override
    public Disposable schedule(Scheduler scheduler) {
        LOGGER.info("Scheduling weather subscriptions");
        //return Flux.interval(Duration.ofSeconds(10), Duration.ofSeconds(20), scheduler)
        return Flux.interval(RegisterWeather.getDelay(), Duration.ofDays(1), scheduler)
                .flatMap(__ -> {
                    LOGGER.info("Sending weather subscriptions");
                    OWM owm = new OWM(Objects.requireNonNull(CredentialManager.get(Credential.OPENWEATHERMAP_API_KEY)));
                    owm.setUnit(OWM.Unit.METRIC);
                    return DatabaseManager.getUsers().getAllUsers().flatMap(dbUser -> {
                                dbUser.getBean().getWeatherRegistered().forEach(city -> {

                                    //TODO: This is a inefficient implementation. This needs to be reworked.
                                    //1 sek delay for each City-API-Call
                                    long expectedTime = System.currentTimeMillis();
                                    long sleepTime = 1000;
                                    expectedTime += sleepTime;//Sample expectedTime += 1000; 1 second sleep
                                    while (System.currentTimeMillis() < expectedTime) {
                                        //Empty Loop
                                    }

                                    gateway.getUserById(dbUser.getId()).flatMap(user ->
                                                    user.getPrivateChannel().flatMap(privateChannel ->
                                                            privateChannel.createMessage("Daily weather forecast of " + city + ":")
                                                                    .then(new WeatherManager().getSaved5DayWeatherData(city, 0.0, 0.0).map(WeatherMapManager::new).flatMap(manager ->
                                                                            privateChannel.createMessage(messageCreateSpec -> {
                                                                                try {
                                                                                    byte[] bytes = manager.createHeatMap();
                                                                                    if (bytes.length > 0)
                                                                                        messageCreateSpec.addFile("temperature.png",
                                                                                                new ByteArrayInputStream(bytes));
                                                                                    else
                                                                                        messageCreateSpec.setContent("Please provide a real city ;)");
                                                                                    bytes = manager.createRainMap();
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

                                    LOGGER.info("weather subscriptions send");
                                });
                                return Mono.empty();
                            }
                    );
                }).subscribe(null, ExceptionHandler::handleUnknownError);

    }
}
