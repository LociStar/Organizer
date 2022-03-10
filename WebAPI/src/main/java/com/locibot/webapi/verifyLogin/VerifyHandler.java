package com.locibot.webapi.verifyLogin;

import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.utils.JWT.TokenVerification;
import com.locibot.webapi.utils.Verification;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class VerifyHandler {

    @NotNull
    public Mono<ServerResponse> verify(ServerRequest request) {
        if (Verification.isAuthenticationInvalid(request))
            return ServerResponse.badRequest().body(BodyInserters.fromValue(new Verify(false)));
        String token = request.headers().header("Authentication").get(0).split(" ")[1];
        TokenVerification tokenVerification = new TokenVerification(token);
        Boolean valid = false;
        try {
            valid = tokenVerification.verify(Objects.requireNonNull(CredentialManager.get(Credential.JWT_SECRET)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (valid)
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(new Verify(valid)));
        return ServerResponse.badRequest().body(BodyInserters.fromValue(new Verify(false)));
    }

}
