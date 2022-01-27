package com.locibot.webapi.weatherData;

import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.utils.JWT.TokenVerification;
import com.locibot.locibot.utils.weather.WeatherManager;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class WeatherDataHandler {
    public Mono<ServerResponse> weatherData(ServerRequest request) {

        String token = request.queryParam("token").orElse("invalid login token");
        String city = request.queryParam("city").orElse("Frankfurt");
        WeatherManager weatherManager = new WeatherManager();

        TokenVerification tokenVerification = new TokenVerification(token);

        try {
            if (tokenVerification.verify(Objects.requireNonNull(CredentialManager.get(Credential.JWT_SECRET)))) {
                return weatherManager.getSavedCurrentWeatherData(city).flatMap(data -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(data)));
            }
        } catch (Exception e) {
            return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new WeatherError("bad token")));
        }

        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new WeatherError("fatal error")));
    }

}

