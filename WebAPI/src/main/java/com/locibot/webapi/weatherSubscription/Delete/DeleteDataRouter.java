package com.locibot.webapi.weatherSubscription.Delete;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class DeleteDataRouter {
    @Bean
    public RouterFunction<ServerResponse> weatherSubscriptionDataDeleteRoute(DeleteDataHandler deleteDataHandler) {

        return RouterFunctions.route()
                .POST("/weatherSubscriptionDataDelete", deleteDataHandler::weatherSubscriptionDataDelete)
                .build();
    }
}
