package com.locibot.webapi;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class CorsConfiguration implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                //.allowCredentials(true)
                //.allowedOrigins("https://organizer-bot-website.herokuapp.com")
                .allowedOrigins("https://organizer-bot-website.herokuapp.com","http://localhost:8080")
                .allowedHeaders("*")
                .exposedHeaders("Access-Control-Allow-Origin"); //, "Access-Control-Allow-Credentials", "Access-Control-Allow-Headers", "Access-Control-Allow-Methods"
    }
}
/*
{
    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/**");
    }

}*/
