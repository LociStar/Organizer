package com.locibot.organizer2.listeners;

import com.locibot.organizer2.data.Telemetry;
import com.locibot.organizer2.database.repositories.GuildRepository;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GuildCreateListener {

    private final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GuildCreateListener.class);
    private final GuildRepository guildRepository;

    public GuildCreateListener(GatewayDiscordClient client, GuildRepository guildRepository) {
        this.guildRepository = guildRepository;

        client.on(GuildCreateEvent.class, this::handle).subscribe();
    }

    public Mono<?> handle(GuildCreateEvent event) {
        Telemetry.GUILD_IDS.add(event.getGuild().getId().asLong());

        if (LOGGER.isDebugEnabled()) {
            final long guildId = event.getGuild().getId().asLong();
            final int memberCount = event.getGuild().getMemberCount();
            LOGGER.debug("Guild ID: {} Connected ({} users)", guildId, memberCount);
        }

        return guildRepository.save(event.getGuild().getId().asLong());
    }

}
