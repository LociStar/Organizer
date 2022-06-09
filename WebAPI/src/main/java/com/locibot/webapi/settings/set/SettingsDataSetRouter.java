package com.locibot.webapi.settings.set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class SettingsDataSetRouter {
    @Bean
    public RouterFunction<ServerResponse> settingsDataSetRoute(SettingsDataSetHandler settingsDataSetHandler) {
        return RouterFunctions.route()
                .POST("/settingsDataSet", settingsDataSetHandler::settingsDataSet)
                .build();
    }
}
