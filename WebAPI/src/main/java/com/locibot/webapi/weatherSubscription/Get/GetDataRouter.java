package com.locibot.webapi.weatherSubscription.Get;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class GetDataRouter {
    @Bean
    public RouterFunction<ServerResponse> weatherSubscriptionDataGetRoute(GetDataHandler getDataHandler) {

        return RouterFunctions.route()
                .GET("/weatherSubscriptionDataGet", getDataHandler::weatherSubscriptionDataGet)
                .build();
    }
}
