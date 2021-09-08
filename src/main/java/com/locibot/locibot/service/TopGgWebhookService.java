package com.locibot.locibot.service;

import com.locibot.locibot.api.json.dbl.TopGgWebhookResponse;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.users.entity.achievement.Achievement;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.NetUtil;
import discord4j.common.util.Snowflake;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jsoup.internal.StringUtil;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.Logger;

public class TopGgWebhookService implements Service {

    private static final Logger LOGGER = LogUtil.getLogger(TopGgWebhookService.class, LogUtil.Category.SERVICE);

    private final String authorization;
    private final String port;

    private Disposable server;

    public TopGgWebhookService() {
        this.authorization = CredentialManager.get(Credential.TOP_DOT_GG_WEBHOOK_AUTHORIZATION);
        this.port = CredentialManager.get(Credential.TOP_DOT_GG_WEBHOOK_PORT);
    }

    @Override
    public boolean isEnabled() {
        return !Config.IS_SNAPSHOT && !StringUtil.isBlank(this.authorization) && !StringUtil.isBlank(this.port);
    }

    @Override
    public void start() {
        LOGGER.info("Starting top.gg WebHook service");
        this.server = HttpServer.create()
                .port(Integer.parseInt(this.port))
                .route(routes -> routes.post("/webhook",
                        (request, response) -> {
                            if (this.authorization.equals(request.requestHeaders().get(HttpHeaderNames.AUTHORIZATION))) {
                                return this.handleRequest(request, response);
                            } else {
                                return response.status(HttpResponseStatus.FORBIDDEN).send();
                            }
                        }))
                .bind()
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    private Mono<Void> handleRequest(HttpServerRequest request, HttpServerResponse response) {
        return request.receive()
                .asString()
                .doOnNext(content -> LOGGER.debug("Request received: {}", content))
                .flatMap(content -> Mono.fromCallable(() ->
                        NetUtil.MAPPER.readValue(content, TopGgWebhookResponse.class)))
                .map(TopGgWebhookResponse::userId)
                .map(Snowflake::of)
                .flatMap(DatabaseManager.getUsers()::getDBUser)
                .flatMap(dbUser -> dbUser.unlockAchievement(Achievement.VOTER))
                .then(response.status(HttpResponseStatus.OK).send());
    }

    @Override
    public void stop() {
        if (this.server != null && !this.server.isDisposed()) {
            LOGGER.info("Stopping top.gg WebHook service");
            this.server.dispose();
        }
    }
}
