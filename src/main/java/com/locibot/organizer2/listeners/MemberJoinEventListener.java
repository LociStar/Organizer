package com.locibot.organizer2.listeners;

import com.locibot.organizer2.database.repositories.GuildRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

@Component
public class MemberJoinEventListener {

    private final Logger LOGGER = LoggerFactory.getLogger(MemberJoinEvent.class);
    private final GuildRepository guildRepository;

    public MemberJoinEventListener(GatewayDiscordClient client, GuildRepository guildRepository) {
        this.guildRepository = guildRepository;

        client.on(MemberJoinEvent.class, this::handle).subscribe();
    }

    public static Mono<Message> sendAutoMessage(GatewayDiscordClient gateway, User user, Snowflake channelId, String message) {
        return gateway.getChannelById(channelId)
                .cast(MessageChannel.class)
                .flatMap(channel -> channel
                        .createMessage(EmbedCreateSpec.builder()
                                .description(message
                                        .replace("{username}", user.getUsername())
                                        .replace("{userId}", user.getId().asString())
                                        .replace("{mention}", user.getMention()))
                                .build()));
    }

    public Mono<?> handle(MemberJoinEvent event) {
        LOGGER.info("Member Joined: {}", event.getMember().getUsername());

        return guildRepository.findById(event.getGuildId().asLong())
                .flatMap(guild ->
                        Mono.zip(
                                Mono.justOrEmpty(guild.getJoin_message()),
                                Mono.justOrEmpty(guild.getMessage_channel_id())
                        ))
                .flatMap(TupleUtils.function((joinMessage, messageChannelId) ->
                        sendAutoMessage(event.getClient(), event.getMember(), Snowflake.of(messageChannelId), joinMessage
                        )));
    }
}

