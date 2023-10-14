package com.locibot.organizer2.api.util;

import com.locibot.organizer2.database.repositories.GuildRepository;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.Map;

public class WebUtil {
    public static Mono<Map<String, Object>> getUserAttributes() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> {
                    JwtAuthenticationToken authentication = (JwtAuthenticationToken) securityContext.getAuthentication();
                    return authentication.getToken().getClaims();
                });
    }

    public static Mono<Jwt> getUserToken() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> {
                    JwtAuthenticationToken authentication = (JwtAuthenticationToken) securityContext.getAuthentication();
                    return authentication.getToken();
                });
    }

    public static Mono<Boolean> isOwner(GuildRepository guildRepository, Long guildId) {
        return getUserAttributes().flatMap(stringObjectMap -> guildRepository
                .existByIdAndOwnerIdAsBoolean(guildId, Long.parseLong(stringObjectMap.get("id").toString())));
    }
}
