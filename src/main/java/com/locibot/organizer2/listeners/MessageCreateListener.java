package com.locibot.organizer2.listeners;

import com.locibot.organizer2.database.repositories.AnalyticsRepository;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class MessageCreateListener {

    private final Logger LOGGER = LoggerFactory.getLogger(MessageCreateListener.class);
    private final AnalyticsRepository analyticsRepository;

    public MessageCreateListener(GatewayDiscordClient client, AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
        client.on(MessageCreateEvent.class, this::handle).subscribe();
    }


    public Mono<?> handle(MessageCreateEvent event) {
        ZonedDateTime timestamp = event.getMessage().getTimestamp().atZone(ZoneId.systemDefault());
        return analyticsRepository.updateMessageCount(event.getGuildId().get().asLong(), timestamp.getMonthValue(), timestamp.getYear());
    }
}
