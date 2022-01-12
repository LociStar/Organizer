package com.locibot.webapi.login;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class LoginRouter {

    @Bean
    public RouterFunction<ServerResponse> route(LoginHandler loginHandler) {

        return RouterFunctions.route()
                .GET("/login", loginHandler::login) //, RequestPredicates.queryParam("token", t -> true)
                .build();
    }
}
