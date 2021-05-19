package com.locibot.locibot.command.info;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import com.locibot.locibot.LociBot;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.ShadbotUtil;
import com.locibot.locibot.utils.SystemUtil;
import com.locibot.locibot.utils.TimeUtil;
import discord4j.common.GitProperties;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.ShardInfo;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.Properties;
import java.util.function.Consumer;

public class BotInfoCmd extends BaseCmd {

    private static final String JAVA_VERSION = System.getProperty("java.version");
    private static final Properties D4J_PROPERTIES = GitProperties.getProperties();
    private static final String D4J_NAME = D4J_PROPERTIES.getProperty(GitProperties.APPLICATION_NAME);
    private static final String D4J_VERSION = D4J_PROPERTIES.getProperty(GitProperties.APPLICATION_VERSION);
    private static final String LAVAPLAYER_VERSION = PlayerLibrary.VERSION;

    public BotInfoCmd() {
        super(CommandCategory.INFO, "bot", "Show bot info");
    }

    @Override
    public Mono<?> execute(Context context) {
        return Mono.zip(
                context.getClient().getUserById(LociBot.getOwnerId()),
                context.getChannel(),
                context.getClient().getGuilds().count())
                .flatMap(TupleUtils.function((owner, channel, guildCount) -> {
                    final long start = System.currentTimeMillis();
                    return context.createFollowupMessage(Emoji.GEAR, context.localize("testing.ping"))
                            .then(context.editFollowupMessage(BotInfoCmd.formatEmbed(context, start, owner, guildCount)));
                }));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, long start, User owner, long guildCount) {
        final long gatewayLatency = context.getClient().getGatewayClientGroup()
                .find(context.getEvent().getShardInfo().getIndex())
                .map(GatewayClient::getResponseTime)
                .map(Duration::toMillis)
                .orElseThrow();
        final String uptime = FormatUtil.formatDurationWords(context.getLocale(), SystemUtil.getUptime());
        final ShardInfo shardInfo = context.getEvent().getShardInfo();

        final String shadbotTitle = Emoji.ROBOT + " THE ORGANIZERRR";
        final String shadbotField = context.localize("botinfo.field.locibot")
                .formatted(uptime, owner.getTag(),
                        shardInfo.getIndex() + 1, shardInfo.getCount(),
                        context.localize(guildCount),
                        context.localize(Telemetry.VOICE_COUNT_GAUGE.get()));

        final String networkTitle = Emoji.SATELLITE + " " + context.localize("botinfo.title.network");
        final String networkField = context.localize("botinfo.field.network")
                .formatted(context.localize(TimeUtil.elapsed(start).toMillis()), context.localize(gatewayLatency));

        final String versionsTitle = Emoji.SCREWDRIVER + " " + context.localize("botinfo.title.versions");
        final String versionsField = context.localize("botinfo.field.versions")
                .formatted(JAVA_VERSION, Config.VERSION, D4J_NAME, D4J_VERSION, LAVAPLAYER_VERSION);

        final String performanceTitle = Emoji.GEAR + " " + context.localize("botinfo.title.performance");
        final String performanceField = context.localize("botinfo.field.performance")
                .formatted(context.localize(SystemUtil.getProcessTotalMemory() - SystemUtil.getProcessFreeMemory()),
                        context.localize(SystemUtil.getProcessTotalMemory()),
                        SystemUtil.getProcessCpuUsage(),
                        context.localize(SystemUtil.getThreadCount()));

        return ShadbotUtil.getDefaultEmbed(embed -> embed
                .setAuthor(context.localize("botinfo.title"), null, context.getAuthorAvatar())
                .addField(shadbotTitle, shadbotField, true)
                .addField(versionsTitle, versionsField, true)
                .addField(performanceTitle, performanceField, true)
                .addField(networkTitle, networkField, true));
    }

}
