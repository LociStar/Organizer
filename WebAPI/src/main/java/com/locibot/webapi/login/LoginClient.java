/*package com.locibot.webapi.login;

import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class LoginClient {

    private final WebClient client;

    // Spring Boot auto-configures a `WebClient.Builder` instance with nice defaults and customizations.
    // We can use it to create a dedicated `WebClient` for our component.
    public LoginClient(WebClient.Builder builder) {
        this.client = builder.baseUrl("http://localhost:8081").build();
    }

    public Mono<String> getMessage() {
        return this.client.get().uri("/login").accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Login.class)
                .map(Login::getMessage);
    }

}*/
