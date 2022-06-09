package com.locibot.webapi.settings.set;

import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.utils.JWT.TokenVerification;
import com.locibot.webapi.settings.get.SettingsData;
import com.locibot.webapi.utils.Verification;
import discord4j.common.util.Snowflake;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.util.Objects;

@Component
public class SettingsDataSetHandler {
    @NotNull
    public Mono<ServerResponse> settingsDataSet(ServerRequest request) {
        if (Verification.isAuthenticationInvalid(request))
            return ServerResponse.badRequest().body(BodyInserters.fromValue(new Error("bad token")));
        String token = request.headers().header("Authentication").get(0).split(" ")[1];
        boolean dm = Boolean.parseBoolean(request.queryParam("dm").orElse("false"));
        String timeZone = request.queryParam("timeZone").orElseThrow();
        ZoneId zoneId = ZoneId.of("Europe/Berlin");

        if (!timeZone.equals(""))
            try {
                zoneId = ZoneId.of(timeZone);
            } catch (Exception ignored) {
                return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new Error("bad timeZone")));
            }

        TokenVerification tokenVerification = new TokenVerification(token);

        try {
            if (tokenVerification.verify(Objects.requireNonNull(CredentialManager.get(Credential.JWT_SECRET)))) {
                ZoneId finalZoneId = zoneId;
                return DatabaseManager.getUsers().getDBUser(Snowflake.of(Long.parseLong(tokenVerification.getPayload().get("uid").toString()))).flatMap(dbUser ->
                        dbUser.setDM(dm).then(timeZone.equals("") ? Mono.empty() : dbUser.setZoneId(finalZoneId)).then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(new SettingsData(dm, timeZone))))
                );
            }
        } catch (Exception e) {
            return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new Error("bad token")));
        }

        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new Error("fatal error")));
    }
}
