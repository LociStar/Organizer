package com.locibot.locibot.database.guilds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.locibot.locibot.database.DatabaseCollection;
import com.locibot.locibot.database.guilds.bean.DBGuildBean;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.locibot.locibot.core.cache.MultiValueCache;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.guilds.entity.DBGuild;
import com.locibot.locibot.database.guilds.entity.DBMember;
import com.locibot.locibot.database.guilds.entity.Settings;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.NetUtil;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuildsCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtil.getLogger(GuildsCollection.class, LogUtil.Category.DATABASE);

    private final MultiValueCache<Snowflake, DBGuild> guildCache;

    public GuildsCollection(MongoDatabase database) {
        super(database, "guilds");
        this.guildCache = MultiValueCache.Builder.<Snowflake, DBGuild>create().withInfiniteTtl().build();
    }

    public List<DBGuild> getDBGuilds() {
        List<DBGuild> dbGroups = new ArrayList<>();
        List<Document> documents = Flux.from(this.getCollection().find()).collectList().block();
        if (documents != null)
            LOGGER.debug("[DBGuild {}] Request", "0");
            Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
            documents.forEach(document -> {
                try {
                    dbGroups.add(new DBGuild(NetUtil.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBGuildBean.class)));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        return dbGroups;
    }

    public Mono<DBGuild> getDBGuild(Snowflake guildId) {
        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", guildId.asString()))
                .first();

        final Mono<DBGuild> getDBGuild = Mono.from(request)
                .map(document -> document.toJson(JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, DBGuildBean.class)))
                .map(DBGuild::new)
                .doOnSuccess(consumer -> {
                    if (consumer == null) {
                        LOGGER.debug("[DBGuild {}] Not found", guildId.asString());
                    }
                })
                .defaultIfEmpty(new DBGuild(guildId))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBGuild {}] Request", guildId.asString());
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                });

        return this.guildCache.getOrCache(guildId, getDBGuild);
    }

    public Mono<Settings> getSettings(Snowflake guildId) {
        return this.getDBGuild(guildId)
                .map(DBGuild::getSettings);
    }

    public Flux<DBMember> getDBMembers(Snowflake guildId, Snowflake... memberIds) {
        return this.getDBGuild(guildId)
                .flatMapIterable(DBGuild::getMembers)
                .collectMap(DBMember::getId)
                .flatMapIterable(dbMembers -> {
                    // Completes the Flux with missing members from the provided array
                    final Set<DBMember> members = new HashSet<>();
                    for (final Snowflake memberId : memberIds) {
                        final DBMember member = dbMembers.getOrDefault(memberId, new DBMember(guildId, memberId));
                        members.add(member);
                    }
                    return members;
                });
    }

    public Mono<DBMember> getDBMember(Snowflake guildId, Snowflake memberId) {
        return this.getDBMembers(guildId, memberId)
                .filter(dbMember -> dbMember.getId().equals(memberId))
                .single();
    }

    public void invalidateCache(Snowflake guildId) {
        LOGGER.trace("{Guild ID: {}} Cache invalidated", guildId.asString());
        this.guildCache.remove(guildId);
    }

}
