package com.locibot.locibot.database.events_db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.locibot.locibot.core.cache.MultiValueCache;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseCollection;
import com.locibot.locibot.database.events_db.bean.DBEventBean;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import com.locibot.locibot.database.events_db.entity.DBEventMember;
import com.locibot.locibot.database.groups.entity.DBGroup;
import com.locibot.locibot.database.groups.entity.DBGroupMember;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.NetUtil;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventsCollection extends DatabaseCollection { //TODO: replace console prints with logs

    public static final Logger LOGGER = LogUtil.getLogger(EventsCollection.class, LogUtil.Category.DATABASE);
    private final MultiValueCache<String, DBEvent> eventCache;

    public EventsCollection(MongoDatabase database) {
        super(database, "events");
        this.eventCache = MultiValueCache.Builder.<String, DBEvent>builder().withInfiniteTtl().build();
    }

    public boolean containsEvent(String eventName) {
        List<Document> documents = Flux.from(this.getCollection().find()).collectList().block();
        if (documents != null) {
            for (Document document : documents) {
                try {
                    if (NetUtil.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBEventBean.class).getEventName().equals(eventName))
                        return true;
                } catch (JsonProcessingException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }
        return false;
    }

    public Mono<DBEvent> getDBEvent(String eventName) {
        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", eventName))
                .first();

        final Mono<DBEvent> getDBEvent = Mono.from(request)
                .map(document -> document.toJson(JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, DBEventBean.class)))
                .map(DBEvent::new)
                .doOnSuccess(consumer -> {
                    if (consumer == null) {
                        LOGGER.debug("[DBEvent {}] Not found", eventName);
                    }
                })
                .defaultIfEmpty(new DBEvent(eventName))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBEvent {}] Request", eventName);
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                });

        return this.eventCache.getOrCache(eventName, getDBEvent);
    }

    public void invalidateCache(String eventName) {
        LOGGER.trace("{Event ID: {}} Cache invalidated", eventName);
        this.eventCache.remove(eventName);
    }

    public Flux<Object> getDBMember(String eventName, Snowflake... memberIds) {
        return this.getDBEvent(eventName)
                .flatMapIterable(DBEvent::getMembers)
                .collectMap(DBEventMember::getId)
                .flatMapIterable(dbMembers -> {
                    // Completes the Flux with missing members from the provided array
                    final Set<DBEventMember> members = new HashSet<>();
                    for (final Snowflake memberId : memberIds) {
                        final DBEventMember member = dbMembers.getOrDefault(memberId, new DBEventMember(memberId, eventName));
                        members.add(member);
                    }
                    return members;
                });
    }
}
