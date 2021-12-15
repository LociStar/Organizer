package com.locibot.webapi.weather;

import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.utils.weather.HourlyWeatherForecastClass;
import com.locibot.webapi.login.Login;
import net.aksingh.owmjapis.core.OWM;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Arrays;

@Component
public class WeatherHandler {

    public Mono<ServerResponse> weather(ServerRequest request) {
        String city = request.queryParam("city").orElse("invalid city");
        OWM owm = new OWM(CredentialManager.get(Credential.OPENWEATHERMAP_API_KEY));
        HourlyWeatherForecastClass weatherForecastClass = new HourlyWeatherForecastClass(owm, city);
        //TokenVerification tokenVerification = new TokenVerification(token);
//        boolean valid = false;
//        try {
//            valid = tokenVerification.verify(Objects.requireNonNull(CredentialManager.get(Credential.JWT_SECRET)));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        try {
            System.out.println("City: " + city);
            DataBuffer buffer = new DefaultDataBufferFactory().wrap(weatherForecastClass.createRainMap());
            return ServerResponse.ok().contentType(MediaType.IMAGE_PNG)
                    .body(BodyInserters.fromDataBuffers(Flux.just(buffer)));
        } catch (IOException e) {
            e.printStackTrace();
            return ServerResponse.badRequest().build();
        }
    }
}