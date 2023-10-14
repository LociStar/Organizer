package com.locibot.organizer2.api.handlers;

import com.locibot.organizer2.api.util.WebUtil;
import com.locibot.organizer2.data.Config;
import com.locibot.organizer2.database.repositories.GuildRepository;
import com.locibot.organizer2.database.tables.Guild;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.PartialMember;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.presence.Status;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Map;

@Component
public class ServerHandler {

    private final GuildRepository guildRepository;
    private final GatewayDiscordClient client;

    public ServerHandler(GuildRepository guildRepository, GatewayDiscordClient client) {
        this.guildRepository = guildRepository;
        this.client = client;
    }

    public Mono<ServerResponse> getServers(ServerRequest ignored) {

        return WebUtil.getUserAttributes().flatMap(stringObjectMap -> {
                    if (Long.parseLong(stringObjectMap.get("id").toString()) == Config.OWNER_USER_ID)
                        return guildRepository.getAllGuilds()
                                .collectMap(Guild::getName, guild -> String.valueOf(guild.getId()))
                                .flatMap(stringLongMap -> ServerResponse.ok().bodyValue(stringLongMap))
                                .switchIfEmpty(ServerResponse.notFound().build());
                    else
                        return guildRepository
                                .getGuildsByOwnerId(Long.parseLong(stringObjectMap.get("id").toString()))
                                .collectMap(Guild::getName, guild -> String.valueOf(guild.getId()))
                                .flatMap(stringLongMap -> ServerResponse.ok().bodyValue(stringLongMap))
                                .switchIfEmpty(ServerResponse.notFound().build());
                }
        );
    }

    public Mono<ServerResponse> getGuildStats(ServerRequest serverRequest) {
        return WebUtil.isOwner(guildRepository, Long.parseLong(serverRequest.pathVariable("id"))).flatMap(aBoolean -> {
            if (!aBoolean) {
                return ServerResponse.badRequest().build();
            }
            return client.getGuildById(Snowflake.of(Long.parseLong(serverRequest.pathVariable("id"))))
                    .flatMap(guild -> {
                        Mono<Long> memberOnlineMono = guild.getMembers()
                                .flatMap(PartialMember::getPresence)
                                .map(Presence::getStatus)
                                .filter(status -> status.equals(Status.ONLINE)).count();
                        return Mono.zip(Mono.just(guild.getMemberCount()), memberOnlineMono);
                    })
                    .flatMap(TupleUtils.function((memberCount, memberOnline) -> ServerResponse.ok().bodyValue(
                            Map.of(
                                    "memberCount", memberCount,
                                    "memberOnline", memberOnline)
                    )))
                    .switchIfEmpty(ServerResponse.notFound().build());
        });
    }
}
