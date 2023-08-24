package com.locibot.organizer2.listeners;

import com.locibot.organizer2.data.Telemetry;
import com.locibot.organizer2.database.repositories.GuildRepository;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GuildDeleteEventListener {
    private final Logger LOGGER = LoggerFactory.getLogger(GuildDeleteEvent.class);
    private final GuildRepository guildRepository;

    public GuildDeleteEventListener(GatewayDiscordClient client, GuildRepository guildRepository) {
        this.guildRepository = guildRepository;

        client.on(GuildDeleteEvent.class, this::handle).subscribe();
    }

    public Mono<?> handle(GuildDeleteEvent event) {
        Telemetry.GUILD_IDS.remove(event.getGuildId().asLong());
        LOGGER.info("Guild ID: {} Disconnected", event.getGuildId().asLong());

        return guildRepository.deleteById(event.getGuildId().asLong());
    }
}
