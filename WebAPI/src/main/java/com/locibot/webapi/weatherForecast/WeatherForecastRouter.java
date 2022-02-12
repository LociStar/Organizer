package com.locibot.webapi.weatherForecast;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class WeatherForecastRouter {
    @Bean
    public RouterFunction<ServerResponse> weatherForecastRoute(WeatherForecastHandler weatherForecastHandler) {

        return RouterFunctions.route()
                .GET("/weatherForecast", weatherForecastHandler::weatherForecast) //, RequestPredicates.queryParam("token", t -> true)
                .build();
    }
}
