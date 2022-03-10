package com.locibot.webapi.weatherData;

import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.utils.JWT.TokenVerification;
import com.locibot.locibot.utils.weather.WeatherManager;
import com.locibot.webapi.utils.Verification;
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
        if (Verification.isAuthenticationInvalid(request))
            return ServerResponse.badRequest().body(BodyInserters.fromValue(new WeatherError("bad token")));
        String token = request.headers().header("Authentication").get(0).split(" ")[1];
        String city = request.queryParam("city").orElse("XXX_No_City");
        Double longitude = Double.parseDouble(request.queryParam("long").orElse("0"));
        Double latitude = Double.parseDouble(request.queryParam("lat").orElse("0"));
        WeatherManager weatherManager = new WeatherManager();

        TokenVerification tokenVerification = new TokenVerification(token);

        try {
            if (tokenVerification.verify(Objects.requireNonNull(CredentialManager.get(Credential.JWT_SECRET)))) {
                return weatherManager.getSavedCurrentWeatherData(city, longitude, latitude).flatMap(data -> {
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(data));
                });
            }
        } catch (Exception e) {
            return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new WeatherError("bad token")));
        }

        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(new WeatherError("fatal error")));
    }

}

