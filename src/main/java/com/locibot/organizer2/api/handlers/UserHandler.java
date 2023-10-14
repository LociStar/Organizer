package com.locibot.organizer2.api.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.locibot.organizer2.api.util.WebUtil;
import com.locibot.organizer2.data.credential.Credential;
import com.locibot.organizer2.data.credential.CredentialManager;
import com.locibot.organizer2.database.repositories.EventRepository;
import com.locibot.organizer2.database.repositories.EventSubscriptionRepository;
import com.locibot.organizer2.database.repositories.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.util.Map;

@Component
public class UserHandler {

    private final UserRepository userRepository;
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final EventRepository eventRepository;

    private final WebClient client = WebClient.create();

    public UserHandler(UserRepository userRepository, EventSubscriptionRepository eventSubscriptionRepository, EventRepository eventRepository) {
        this.userRepository = userRepository;
        this.eventSubscriptionRepository = eventSubscriptionRepository;
        this.eventRepository = eventRepository;
    }

    public Mono<ServerResponse> getZoneId(ServerRequest ignored) {
        return WebUtil.getUserAttributes().map(stringObjectMap -> Long.parseLong(stringObjectMap.get("id").toString()))
                .flatMap(userRepository::getZoneId)
                .map(value -> Map.of("zoneId", value))
                .flatMap(zoneId -> ServerResponse.ok().bodyValue(zoneId));
    }

    public Mono<ServerResponse> setZoneId(ServerRequest serverRequest) {
        return WebUtil.getUserAttributes()
                .map(stringObjectMap -> Long.parseLong(stringObjectMap.get("id").toString()))
                .zipWith(serverRequest.bodyToMono(Map.class))
                .flatMap(tuple -> userRepository.setZoneId(tuple.getT1(), ZoneId.of(tuple.getT2().get("zoneId").toString())))
                .flatMap(__ -> ServerResponse.ok().build());
    }

    public Mono<ServerResponse> deleteUser(ServerRequest ignored) {
        return WebUtil.getUserToken()
                .flatMap((token) -> eventSubscriptionRepository.deleteAllByUserId(Long.parseLong(token.getClaims().get("id").toString()))
                        .then(eventRepository.deleteAllByOwnerId(Long.parseLong(token.getClaims().get("id").toString())))
                        .then(userRepository.deleteById(Long.parseLong(token.getClaims().get("id").toString())))
                        .then(deleteKeycloakUser(token.getClaims().get("sub").toString())))
                .flatMap(__ -> ServerResponse.ok().build());
    }

    private Mono<String> deleteKeycloakUser(String username) {
        return getAdminAccessToken().flatMap(accessToken -> WebClient.create().delete()
                .uri("https://keycloak.organizer-bot.com/admin/realms/Organizer/users/" + username)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(JsonNode::toPrettyString)
                .doOnError(throwable -> System.out.println("Error: " + throwable.getMessage())));
    }

    private Mono<String> getAdminAccessToken() {
        return WebClient.create().post()
                .uri("https://keycloak.organizer-bot.com/realms/Organizer/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue("grant_type=client_credentials&client_id=" + CredentialManager.get(Credential.CLIENT_ID) + "&client_secret=" + CredentialManager.get(Credential.CLIENT_SECRET)))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> jsonNode.get("access_token").asText())
                .doOnError(throwable -> System.out.println("Error: " + throwable.getMessage()));
    }
}
