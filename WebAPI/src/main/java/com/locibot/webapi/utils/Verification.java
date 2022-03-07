package com.locibot.webapi.utils;

import org.springframework.web.reactive.function.server.ServerRequest;

public abstract class Verification {
    public static Boolean checkCookie(ServerRequest request){
        return request.cookies().size() != 0;
    }
}
