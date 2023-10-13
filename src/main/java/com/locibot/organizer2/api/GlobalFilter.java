package com.locibot.organizer2.api;

import com.locibot.organizer2.api.web.util.WebUtil;
import com.locibot.organizer2.database.repositories.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-1)
public class GlobalFilter implements WebFilter {

    private final UserRepository userRepository;

    public GlobalFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public @NotNull Mono<Void> filter(@NotNull ServerWebExchange exchange, WebFilterChain chain) {
        return trackUser().then(chain.filter(exchange));
    }

    private Mono<?> trackUser() {
        return WebUtil.getUserAttributes()
                .map(stringObjectMap -> Long.parseLong(stringObjectMap.get("id").toString()))
                .flatMap(userRepository::updateLastUsedTimestamp);
    }
}
