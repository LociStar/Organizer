package com.locibot.webapi.weatherSubscription.Get;

import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.utils.JWT.TokenVerification;
import com.locibot.webapi.utils.Verification;
import discord4j.common.util.Snowflake;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class GetDataHandler {
    @NotNull
    public Mono<ServerResponse> weatherSubscriptionDataGet(ServerRequest request) {
        if (Verification.isAuthenticationInvalid(request))
            return ServerResponse.badRequest().body(BodyInserters.fromValue(new Error("bad token")));
        String token = request.headers().header("Authentication").get(0).split(" ")[1];
        //String city = request.queryParam("city").orElse("XXX_No_City");

        TokenVerification tokenVerification = new TokenVerification(token);

        try {
            if (tokenVerification.verify(Objects.requireNonNull(CredentialManager.get(Credential.JWT_SECRET)))) {
                return DatabaseManager.getUsers().getDBUser(Snowflake.of(Long.parseLong(tokenVerification.getPayload().get("uid").toString()))).flatMap(dbUser ->
                        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(dbUser.getBean().getWeatherRegistered()))
                );
            }
        } catch (Exception e) {
            return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new Error("bad token")));
        }

        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new Error("fatal error")));
    }
}
