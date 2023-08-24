package com.locibot.organizer2.commands.info;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import com.locibot.organizer2.data.Config;
import com.locibot.organizer2.data.Telemetry;
import com.locibot.organizer2.object.Emoji;
import com.locibot.organizer2.utils.FormatUtil;
import com.locibot.organizer2.utils.SystemUtil;
import com.locibot.organizer2.utils.TimeUtil;
import discord4j.common.GitProperties;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.ShardInfo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Component
public class BotInfoCommand implements SlashCommand {

    private static final String JAVA_VERSION = System.getProperty("java.version");
    private static final Properties D4J_PROPERTIES = GitProperties.getProperties();
    private static final String D4J_NAME = D4J_PROPERTIES.getProperty(GitProperties.APPLICATION_NAME);
    private static final String D4J_VERSION = D4J_PROPERTIES.getProperty(GitProperties.APPLICATION_VERSION);

    @Override
    public String getName() {
        return "info bot";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        ChatInputInteractionEvent interactionEvent = (ChatInputInteractionEvent) context.getEvent();
        Mono<Snowflake> monoOwnerId = context.getEvent().getClient().getApplicationInfo().map(ApplicationInfo::getOwnerId);
        return monoOwnerId.flatMap(ownerId -> Mono.zip(
                        interactionEvent.getClient().getUserById(ownerId),
                        interactionEvent.getInteraction().getChannel(),
                        interactionEvent.getClient().getGuilds().count())
                .flatMap(TupleUtils.function((owner, channel, guildCount) -> {
                    final long start = System.currentTimeMillis();
                    return context.getEvent().reply(
                            InteractionApplicationCommandCallbackSpec.builder().addEmbed(formatEmbed(context, start, owner, guildCount)).build());
                })));
    }

    private static EmbedCreateSpec formatEmbed(CommandContext<?> context, long start, User owner, long guildCount) {
        final long gatewayLatency = context.getEvent().getClient().getGatewayClientGroup()
                .find(context.getEvent().getShardInfo().getIndex())
                .map(GatewayClient::getResponseTime)
                .map(Duration::toMillis)
                .orElseThrow();
        final String uptime = FormatUtil.formatDurationWords(context.getLocale(), SystemUtil.getUptime());
        final ShardInfo shardInfo = context.getEvent().getShardInfo();

        final String botTitle = Emoji.ROBOT + " THE ORGANIZERRR";
        final String botField = context.localize("botinfo.field.locibot")
                .formatted(uptime, owner.getTag(),
                        shardInfo.getIndex() + 1, shardInfo.getCount(),
                        context.localize(guildCount),
                        context.localize(Telemetry.VOICE_COUNT_GAUGE.get()));

        final String networkTitle = Emoji.SATELLITE + " " + context.localize("botinfo.title.network");
        final String networkField = context.localize("botinfo.field.network")
                .formatted(context.localize(TimeUtil.elapsed(start).toMillis()), context.localize(gatewayLatency));

        final String versionsTitle = Emoji.SCREWDRIVER + " " + context.localize("botinfo.title.versions");
        final String versionsField = context.localize("botinfo.field.versions")
                .formatted(JAVA_VERSION, Config.VERSION, D4J_NAME, D4J_VERSION);

        final String performanceTitle = Emoji.GEAR + " " + context.localize("botinfo.title.performance");
        final String performanceField = context.localize("botinfo.field.performance")
                .formatted(context.localize(SystemUtil.getProcessTotalMemory() - SystemUtil.getProcessFreeMemory()),
                        context.localize(SystemUtil.getProcessTotalMemory()),
                        SystemUtil.getProcessCpuUsage(),
                        context.localize(SystemUtil.getThreadCount()));

        return context.getDefaultEmbed(EmbedCreateSpec.builder()
                .author(EmbedCreateFields.Author.of(context.localize("botinfo.title"), null, context.getAuthorAvatar()))
                .fields(List.of(
                        EmbedCreateFields.Field.of(botTitle, botField, true),
                        EmbedCreateFields.Field.of(versionsTitle, versionsField, true),
                        EmbedCreateFields.Field.of(performanceTitle, performanceField, true),
                        EmbedCreateFields.Field.of(networkTitle, networkField, true))).build());
    }
}
