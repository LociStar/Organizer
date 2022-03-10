package com.locibot.webapi.utils;

import org.springframework.web.reactive.function.server.ServerRequest;

public abstract class Verification {
    public static Boolean isAuthenticationInvalid(ServerRequest request){
        return request.headers().header("Authentication").isEmpty();
    }
}
