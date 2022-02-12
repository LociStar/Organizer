package com.locibot.webapi.weatherData;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;


@Configuration(proxyBeanMethods = false)
public class WeatherDataRouter {

    @Bean
    public RouterFunction<ServerResponse> weatherDataRoute(WeatherDataHandler weatherDataHandler) {

        return RouterFunctions.route()
                .GET("/weatherData", weatherDataHandler::weatherData) //, RequestPredicates.queryParam("token", t -> true)
                .build();
    }
}
