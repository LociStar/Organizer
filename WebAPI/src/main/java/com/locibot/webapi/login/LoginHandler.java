package com.locibot.webapi.login;

import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.utils.JWT.TokenVerification;
import com.locibot.webapi.utils.Verification;
import discord4j.common.util.Snowflake;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

@Component
public class LoginHandler {

    public Mono<ServerResponse> login(ServerRequest request) {
        String token = request.queryParam("token").orElse("Bad Token");
        TokenVerification tokenVerification = new TokenVerification(token);
        boolean valid = false;
        try {
            valid = tokenVerification.verify(Objects.requireNonNull(CredentialManager.get(Credential.JWT_SECRET)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject payload = tokenVerification.getPayload();
        String tokenSub = payload.get("sub").toString();

        if (valid && Objects.equals(tokenSub, "login")) {
            //System.out.println("OK");
            return DatabaseManager.getGuilds().getDBMember(Snowflake.of(payload.get("gid").toString()), Snowflake.of(payload.get("uid").toString())).flatMap(dbMember ->
            {
                try {
                    return dbMember.generateAccessToken()
                            .then(DatabaseManager.getGuilds().getDBMember(Snowflake.of(payload.get("gid").toString()), Snowflake.of(payload.get("uid").toString())).flatMap(member ->
                                    ServerResponse.ok()
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(BodyInserters.fromValue(new Login(member.getAccessToken())))));
                } catch (Exception e) {
                    e.printStackTrace();
                    return ServerResponse.badRequest().body(BodyInserters.fromValue(new Login("Bad Token")));
                }
            });
        }

        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(new Login("Token is Invalid")));
    }
}