package com.locibot.webapi.settings.get;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class SettingsDataGetRouter {
    @Bean
    public RouterFunction<ServerResponse> settingsDataGetRoute(SettingsDataGetHandler settingsDataGetHandler) {
        return RouterFunctions.route()
                .GET("/settingsDataGet", settingsDataGetHandler::settingsDataGet)
                .build();
    }
}
