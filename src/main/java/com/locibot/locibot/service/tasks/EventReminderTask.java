package com.locibot.locibot.service.tasks;

import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.LogUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.Logger;

import java.time.*;
import java.time.temporal.ChronoUnit;

public class EventReminderTask implements Task {

    private static final Logger LOGGER = LogUtil.getLogger(EventReminderTask.class, LogUtil.Category.TASK);
    private final GatewayDiscordClient gateway;

    public EventReminderTask(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Disposable schedule(Scheduler scheduler) {
        LOGGER.info("Scheduling event reminder");
        return Flux.interval(Duration.ofMinutes(1), Duration.ofMinutes(1), scheduler)
                .flatMap(__ -> {
                    LOGGER.debug("Starting group reminder");
                    DatabaseManager.getEvents().getAllEvents().forEach(dbEvent -> {
                        Long epochSeconds = dbEvent.getBean().getScheduledDate();
                        //check if group is scheduled
                        if (epochSeconds != null) {
                            ZoneId zoneId = ZoneId.of("Europe/Berlin");
                            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), zoneId);
                            //check if date_now == group scheduled date
                            if (zonedDateTime.toLocalDate().isEqual(LocalDate.now())
                                    //send invite to all members of group, if (time_now + 35min) > scheduled_time > (time_now + 25min)
                                    && zonedDateTime.toLocalTime().isAfter(LocalTime.now().plus(1740, ChronoUnit.SECONDS))
                                    && zonedDateTime.toLocalTime().isBefore(LocalTime.now().plus(1830, ChronoUnit.SECONDS))) {
                                LOGGER.debug("30min reminder for group " + dbEvent.getEventName());
                                dbEvent.getMembers().forEach(member -> {
                                    if (member.getBean().getAccepted() == 1)
                                        gateway.getUserById(member.getUId()).flatMap(user ->
                                                gateway.getUserById(dbEvent.getOwner().getUId()).flatMap(owner ->
                                                        user.getPrivateChannel().flatMap(privateChannel ->
                                                                privateChannel.createMessage(EmbedCreateSpec.builder()
                                                                        .color(Color.BLUE)
                                                                        .title("Event Reminder")
                                                                        .description("The event **" + dbEvent.getEventName() + "** will start in 30 minutes")
                                                                        .addField(EmbedCreateFields.Field.of("Description", dbEvent.getEventDescription(), false))
                                                                        .thumbnail(dbEvent.getIcon())
                                                                        .footer(EmbedCreateFields.Footer.of("Author: " + owner.getUsername(), owner.getAvatarUrl()))
                                                                        .build())
                                                        ))).subscribe();
                                });
                            }
                        }
                    });
                    LOGGER.debug("Closing group reminder");
                            return Mono.empty();
                        }
                ).subscribe(null, ExceptionHandler::handleUnknownError);
    }
}
