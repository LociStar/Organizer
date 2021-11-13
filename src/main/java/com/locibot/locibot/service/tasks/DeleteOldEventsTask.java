package com.locibot.locibot.service.tasks;

import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.LogUtil;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.Logger;

import java.time.*;

public class DeleteOldEventsTask implements Task{

    private static final Logger LOGGER = LogUtil.getLogger(DeleteOldEventsTask.class, LogUtil.Category.TASK);

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Disposable schedule(Scheduler scheduler) {
        LOGGER.info("Scheduling event disposer");
        return Flux.interval(Duration.ofMinutes(1), Duration.ofHours(24), scheduler)
                .flatMap(__ -> {
                    LOGGER.info("Starting event disposer");
                    DatabaseManager.getEvents().getAllEvents().forEach(dbEvent -> {
                        Long date = dbEvent.getBean().getScheduledDate();
                        ZoneId zoneId = ZoneId.of("Europe/Berlin");
                        if (date != null && ZonedDateTime.now(zoneId).isAfter(ZonedDateTime.ofInstant(Instant.ofEpochSecond(date), zoneId))) {
                            dbEvent.delete().subscribe();
                            LOGGER.info("Event " + dbEvent.getEventName() + " deleted");
                        }
                    });
                    LOGGER.info("Closing event disposer");
                    return Mono.empty();
                }).subscribe(null, ExceptionHandler::handleUnknownError);
    }
}
