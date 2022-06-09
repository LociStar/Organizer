package com.locibot.webapi.weatherSubscription.Add;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class AddDataRouter {
    @Bean
    public RouterFunction<ServerResponse> weatherSubscriptionDataAddRoute(AddDataHandler addDataHandler) {

        return RouterFunctions.route()
                .POST("/weatherSubscriptionDataAdd", addDataHandler::weatherSubscriptionDataAdd)
                .build();
    }
}
