package com.locibot.locibot.listener;

import com.locibot.locibot.core.command.CommandManager;
import com.locibot.locibot.data.Telemetry;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import reactor.core.publisher.Mono;

import static com.locibot.locibot.LociBot.DEFAULT_LOGGER;

public class GuildCreateListener implements EventListener<GuildCreateEvent> {

    private final DiscordClient client;

    public GuildCreateListener(DiscordClient client) {
        this.client = client;
    }

    @Override
    public Class<GuildCreateEvent> getEventType() {
        return GuildCreateEvent.class;
    }

    @Override
    public Mono<?> execute(GuildCreateEvent event) {
        Telemetry.GUILD_IDS.add(event.getGuild().getId().asLong());

        if (DEFAULT_LOGGER.isDebugEnabled()) {
            final Snowflake guildId = event.getGuild().getId();
            final int memberCount = event.getGuild().getMemberCount();
            DEFAULT_LOGGER.debug("{Guild ID: {}} Connected ({} users)", guildId.asString(), memberCount);
        }
        DEFAULT_LOGGER.info("Registering commands");
        return event.getClient().getApplicationInfo().flatMap(applicationInfo -> CommandManager.registerGuildCommands(client.getApplicationService(), applicationInfo.getId().asLong(), event.getGuild().getId().asLong()));
    }

}
