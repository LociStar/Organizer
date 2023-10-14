package com.locibot.organizer2.api.handlers;

import com.locibot.organizer2.database.repositories.AnalyticsRepository;
import com.locibot.organizer2.database.repositories.GuildRepository;
import com.locibot.organizer2.utils.LogUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import static com.locibot.organizer2.api.util.WebUtil.getUserAttributes;

@Component
public class AnalyticsHandler {

    private static final Logger LOGGER = LogUtil.getLogger(AnalyticsHandler.class);
    private final AnalyticsRepository analyticsRepository;
    private final GuildRepository guildRepository;

    public AnalyticsHandler(AnalyticsRepository analyticsRepository, GuildRepository guildRepository) {
        this.analyticsRepository = analyticsRepository;
        this.guildRepository = guildRepository;
    }

    public Mono<ServerResponse> getStatus(ServerRequest ignored) {
        return ServerResponse.ok().bodyValue("Ready");
    }

    public Mono<ServerResponse> getByID(ServerRequest serverRequest) {

        Mono<Boolean> isOwner = getUserAttributes().flatMap(stringObjectMap -> guildRepository
                .existByIdAndOwnerIdAsBoolean(Long.parseLong(serverRequest.pathVariable("id")), Long.parseLong(stringObjectMap.get("id").toString())));

        return isOwner.flatMap(owner -> {
            if (owner) {
                return analyticsRepository.getAllById(Long.parseLong(serverRequest.pathVariable("id")))
                        .collectList()
                        .flatMap(analytics -> ServerResponse.ok().bodyValue(analytics))
                        .switchIfEmpty(ServerResponse.notFound().build());
            } else {
                return ServerResponse.badRequest().build();
            }
        });
    }


}
