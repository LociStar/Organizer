package com.locibot.webapi.verifyLogin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class VerifyRouter {
    @Bean
    public RouterFunction<ServerResponse> verifyRoute(VerifyHandler verifyHandler) {

        return RouterFunctions.route()
                .GET("/verify", verifyHandler::verify) //, RequestPredicates.queryParam("token", t -> true)
                .build();
    }
}
