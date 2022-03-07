package com.locibot.webapi.weather;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Deprecated
@Configuration(proxyBeanMethods = false)
public class WeatherRounter {

    @Bean
    public RouterFunction<ServerResponse> weatherRoute(WeatherHandler weatherHandler) {

        return RouterFunctions.route()
                .GET("/weather", weatherHandler::weather) //, RequestPredicates.queryParam("token", t -> true)
                .build();
    }
}
