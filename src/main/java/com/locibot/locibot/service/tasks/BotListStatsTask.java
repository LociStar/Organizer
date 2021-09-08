package com.locibot.locibot.service.tasks;

import com.locibot.locibot.api.BotListStats;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.LogUtil;
import discord4j.core.GatewayDiscordClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.Logger;

import java.time.Duration;

public class BotListStatsTask implements Task {

    private static final Logger LOGGER = LogUtil.getLogger(BotListStatsTask.class, LogUtil.Category.TASK);

    private final GatewayDiscordClient gateway;
    private BotListStats botListStats;

    public BotListStatsTask(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean isEnabled() {
        return !Config.IS_SNAPSHOT;
    }

    @Override
    public Disposable schedule(Scheduler scheduler) {
        LOGGER.info("Scheduling bot list stats task");
        this.botListStats = new BotListStats(this.gateway);
        return Flux.interval(Duration.ofMinutes(15), Duration.ofHours(3), scheduler)
                .flatMap(__ -> this.botListStats.postStats()
                        .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err))))
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }
}
