package com.locibot.locibot.service.tasks;

import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.LogUtil;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.Logger;

import java.time.Duration;
import java.time.LocalDate;

public class DeleteOldGroupsTask implements Task {

    private static final Logger LOGGER = LogUtil.getLogger(DeleteOldGroupsTask.class, LogUtil.Category.TASK);

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Disposable schedule(Scheduler scheduler) {
        LOGGER.info("Scheduling group disposer");
        return Flux.interval(Duration.ofMinutes(1), Duration.ofHours(24), scheduler)
                .flatMap(__ -> {
                    LOGGER.info("Starting group disposer");
                    DatabaseManager.getGroups().getAllGroups().forEach(group -> {
                        String date = group.getBean().getScheduledDate();
                        if (date != null && LocalDate.now().isAfter(LocalDate.parse(date))) {
                            group.delete().subscribe();
                            LOGGER.info("Group " + group.getGroupName() + " deleted");
                        }
                    });
                    LOGGER.info("Closing group disposer");
                    return Mono.empty();
                }).subscribe(null, ExceptionHandler::handleUnknownError);
    }
}
