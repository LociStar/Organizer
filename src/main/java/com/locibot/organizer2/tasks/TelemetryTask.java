package com.locibot.organizer2.tasks;

import com.locibot.organizer2.data.Config;
import com.locibot.organizer2.data.Telemetry;
import com.locibot.organizer2.utils.LogUtil;
import com.locibot.organizer2.utils.SystemUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.GatewayClientGroup;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.util.Logger;

import java.time.Duration;

@Service
public class TelemetryTask {
    private static final Logger LOGGER = LogUtil.getLogger(TelemetryTask.class, LogUtil.Category.TASK);
    private final GatewayDiscordClient gateway;

    public TelemetryTask(GatewayDiscordClient gateway) {
        this.gateway = gateway;
        LOGGER.info("Scheduling Telemetry task");
    }

    @Scheduled(fixedRate = 15000)
    public void sendTelemetry() {
        if (Config.IS_SNAPSHOT) {
            return;
        }
        final GatewayClientGroup group = this.gateway.getGatewayClientGroup();
        LOGGER.debug("Updating Telemetry statistics");
        Telemetry.UPTIME_GAUGE.set(SystemUtil.getUptime().toMillis());
        Telemetry.PROCESS_CPU_USAGE_GAUGE.set(SystemUtil.getProcessCpuUsage());
        Telemetry.SYSTEM_CPU_USAGE_GAUGE.set(SystemUtil.getSystemCpuUsage());
        Telemetry.MAX_HEAP_MEMORY_GAUGE.set(SystemUtil.getMaxHeapMemory());
        Telemetry.TOTAL_HEAP_MEMORY_GAUGE.set(SystemUtil.getTotalHeapMemory());
        Telemetry.USED_HEAP_MEMORY_GAUGE.set(SystemUtil.getUsedHeapMemory());
        Telemetry.SYSTEM_TOTAL_MEMORY_GAUGE.set(SystemUtil.getSystemTotalMemory());
        Telemetry.SYSTEM_FREE_MEMORY_GAUGE.set(SystemUtil.getSystemFreeMemory());
        Telemetry.GC_COUNT_GAUGE.set(SystemUtil.getGCCount());
        Telemetry.GC_TIME_GAUGE.set(SystemUtil.getGCTime().toMillis());
        Telemetry.THREAD_COUNT_GAUGE.set(SystemUtil.getThreadCount());
        Telemetry.DAEMON_THREAD_COUNT_GAUGE.set(SystemUtil.getDaemonThreadCount());

        Telemetry.PROCESS_TOTAL_MEMORY.set(SystemUtil.getProcessTotalMemory());
        Telemetry.PROCESS_FREE_MEMORY.set(SystemUtil.getProcessFreeMemory());
        Telemetry.PROCESS_MAX_MEMORY.set(SystemUtil.getProcessMaxMemory());

        Telemetry.GUILD_COUNT_GAUGE.set(Telemetry.GUILD_IDS.size());
        Telemetry.UNIQUE_INTERACTING_USERS.set(Telemetry.INTERACTING_USERS.size());

        for (int i = 0; i < group.getShardCount(); ++i) {
            final long responseTime = group.find(i)
                    .map(GatewayClient::getResponseTime)
                    .map(Duration::toMillis)
                    .orElse(0L);
            Telemetry.RESPONSE_TIME_GAUGE.labels(Integer.toString(i)).set(responseTime);
        }
        LOGGER.debug("Telemetry statistics updated");
    }
}
