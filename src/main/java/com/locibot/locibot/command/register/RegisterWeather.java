package com.locibot.locibot.command.register;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.guilds.entity.WeatherChoice;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.DiscordUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZonedDateTime;

public class RegisterWeather extends BaseCmd {

    protected RegisterWeather() {
        super(CommandCategory.REGISTER, CommandPermission.USER_GUILD, "weather_subscription", "register to weather");
        this.addOption("option", "subscribe or unsubscribe", true, ApplicationCommandOption.Type.STRING, DiscordUtil.toOptions(WeatherChoice.class));
        this.addOption("city", "a city name", true, ApplicationCommandOption.Type.STRING);
    }

    public static Duration getDelay() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextDate = now
                .withHour(3)
                .withMinute(0)
                .withSecond(0);
        if (nextDate.isBefore(ZonedDateTime.now())) {
            nextDate = nextDate.plusDays(1);
        }

        return Duration.between(now, nextDate);
    }

    @Override
    public Mono<?> execute(Context context) {
        final WeatherChoice weatherChoice = context.getOptionAsEnum(WeatherChoice.class, "option").orElseThrow();
        final String city = context.getOptionAsString("city").orElseThrow();

        return DatabaseManager.getGuilds().getDBMember(context.getGuildId(), context.getAuthorId()).flatMap(dbMember -> {
            if (weatherChoice.getB()) {
                return dbMember.subscribeWeatherRegistered(city)
                        .then(context.createFollowupMessage("Subscribed to " + city))
                        .onErrorResume(err -> ExceptionHandler.handleCommandError(err, context)
                                .then(Mono.empty()));
            } else
                return dbMember.unsubscribeWeatherRegistered(city)
                        .then(context.createFollowupMessage("Unsubscribed to " + city))
                        .onErrorResume(err -> ExceptionHandler.handleCommandError(err, context)
                                .then(Mono.empty()));
        });
    }
}
