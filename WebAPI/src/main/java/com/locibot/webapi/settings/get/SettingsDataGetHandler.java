package com.locibot.webapi.settings.get;

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
public class SettingsDataGetHandler {
    @NotNull
    public Mono<ServerResponse> settingsDataGet(ServerRequest request) {
        if (Verification.isAuthenticationInvalid(request))
            return ServerResponse.badRequest().body(BodyInserters.fromValue(new Error("bad token")));
        String token = request.headers().header("Authentication").get(0).split(" ")[1];

        TokenVerification tokenVerification = new TokenVerification(token);

        try {
            if (tokenVerification.verify(Objects.requireNonNull(CredentialManager.get(Credential.JWT_SECRET)))) {
                return DatabaseManager.getUsers().getDBUser(Snowflake.of(Long.parseLong(tokenVerification.getPayload().get("uid").toString()))).flatMap(dbUser -> {
                            String timeZone = dbUser.getBean().getZoneId() == null ? "" : dbUser.getBean().getZoneId().toString();
                            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromValue(new SettingsData(dbUser.getBean().getDm(), timeZone)));
                        }
                );
            }
        } catch (Exception e) {
            return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new Error("bad token")));
        }

        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new Error("fatal error")));
    }
}
