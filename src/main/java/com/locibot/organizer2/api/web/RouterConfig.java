package com.locibot.organizer2.api.web;

import com.locibot.organizer2.api.handlers.AnalyticsHandler;
import com.locibot.organizer2.api.handlers.ServerHandler;
import com.locibot.organizer2.api.handlers.UserHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration(proxyBeanMethods = false)
public class RouterConfig {

    @Bean
    protected RouterFunction<ServerResponse> analyticsRoutes(AnalyticsHandler analyticsHandler) {
        return RouterFunctions.nest(path("/analytics"),
                RouterFunctions.route(GET("/"), analyticsHandler::getStatus)
                        .andRoute(GET("/{id}"), analyticsHandler::getByID));
//                        .andRoute(GET("/{id}/member/count"),analyticsHandler::getMemberCountById)
//                        .andRoute(GET("/{id}/member/online"),analyticsHandler::getMemberOnlineById));
    }

    @Bean
    protected RouterFunction<ServerResponse> serverRoutes(ServerHandler serverHandler) {
        return RouterFunctions.nest(path("/server"),
                RouterFunctions.route(GET("/"), serverHandler::getServers)
                        .andRoute(GET("/{id}/stats"), serverHandler::getGuildStats));
//                        .andRoute(GET("/{id}/member/count"),analyticsHandler::getMemberCountById)
//                        .andRoute(GET("/{id}/member/online"),analyticsHandler::getMemberOnlineById));
    }

    @Bean
    protected RouterFunction<ServerResponse> userRoutes(UserHandler userHandler) {
        return RouterFunctions.nest(path("/user"),
                RouterFunctions.route(GET("/zoneId"), userHandler::getZoneId)
                        .andRoute(POST("/zoneId/set"), userHandler::setZoneId)
                        .andRoute(DELETE("/delete"), userHandler::deleteUser));
    }
}
