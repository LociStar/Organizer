package com.locibot.organizer2;

import com.locibot.organizer2.core.retriever.SpyRestEntityRetriever;
import com.locibot.organizer2.data.Config;
import com.locibot.organizer2.data.credential.Credential;
import com.locibot.organizer2.data.credential.CredentialManager;
import discord4j.common.ReactorResources;
import discord4j.common.store.Store;
import discord4j.common.store.impl.LocalStoreLayout;
import discord4j.common.store.legacy.LegacyStoreLayout;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.retriever.FallbackEntityRetriever;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.discordjson.json.MessageData;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.util.AllowedMentions;
import discord4j.store.api.mapping.MappingStoreService;
import discord4j.store.caffeine.CaffeineStoreService;
import discord4j.store.jdk.JdkStoreService;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class Organizer2Application {

    public final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) {
        SpringApplication.run(Organizer2Application.class, args);
    }

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {

        Locale.setDefault(Config.DEFAULT_LOCALE);
        DEFAULT_LOGGER.info("Starting LociBot V{}", Config.VERSION);
        if (Config.IS_SNAPSHOT) {
            DEFAULT_LOGGER.info("[SNAPSHOT] Enabling Reactor operator stack recorder");
            Hooks.onOperatorDebug();
        }

        final String discordToken = CredentialManager.get(Credential.DISCORD_TOKEN);
        Objects.requireNonNull(discordToken, "Missing Discord bot token");

        ReactorResources reactor = ReactorResources.builder()
                .httpClient(ReactorResources.DEFAULT_HTTP_CLIENT.get()
                        .resolver(DefaultAddressResolverGroup.INSTANCE))
                .build();

        DiscordClient client = DiscordClientBuilder.create(discordToken)
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .setDefaultAllowedMentions(AllowedMentions.suppressEveryone())
                .setReactorResources(reactor)
                .build();

        return client.gateway()
                .setStore(Store.fromLayout(LegacyStoreLayout.of(MappingStoreService.create()
                        .setMapping(new CaffeineStoreService(
                                builder -> builder.maximumSize(10_000)
                                        .expireAfterWrite(15, TimeUnit.MINUTES)), MessageData.class)
                        .setFallback(new JdkStoreService()))))
                .setEntityRetrievalStrategy(gateway -> new FallbackEntityRetriever(
                        EntityRetrievalStrategy.STORE.apply(gateway), new SpyRestEntityRetriever(gateway)))
                .setEnabledIntents(IntentSet.of(
                        Intent.GUILDS,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_VOICE_STATES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.GUILD_MESSAGES,
                        Intent.DIRECT_MESSAGES))
                .setInitialPresence(__ -> ClientPresence.online(ClientActivity.listening("/help")))
                .setMemberRequestFilter(MemberRequestFilter.none())
                .login()
                .block();
    }

    @Bean
    public RestClient discordRestClient(GatewayDiscordClient client) {
        return client.getRestClient();
    }

}
