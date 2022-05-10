package com.locibot.locibot.database.events_db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.locibot.locibot.core.cache.MultiValueCache;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseCollection;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.events_db.bean.DBEventBean;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.NetUtil;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class EventsCollection extends DatabaseCollection { //TODO: replace console prints with logs

    public static final Logger LOGGER = LogUtil.getLogger(EventsCollection.class, LogUtil.Category.DATABASE);
    private final MultiValueCache<ObjectId, DBEvent> eventCache;

    public EventsCollection(MongoDatabase database) {
        super(database, "events");
        this.eventCache = MultiValueCache.Builder.<ObjectId, DBEvent>builder().withInfiniteTtl().build();
    }

    public boolean containsEvent(ObjectId eventName) {
        List<Document> documents = Flux.from(this.getCollection().find()).collectList().block();
        if (documents != null) {
            for (Document document : documents) {
                try {
                    if (NetUtil.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBEventBean.class).getId().equals(eventName))
                        return true;
                } catch (JsonProcessingException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }
        return false;
    }

    public Mono<DBEvent> getDBEvent(ObjectId id) {
        Publisher<Document> document = this.getCollection().find(Filters.eq("_id", id)).first();
        return Mono.from(document)
                .map(document1 -> document1.toJson(JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, DBEventBean.class)))
                .map(DBEvent::new);
    }

    public Mono<DBEvent> getDBEvent(Snowflake uId, String eventName) {

        final Mono<List<ObjectId>> eventsRequest = DatabaseManager.getUsers().getDBUser(uId).map(dbUser ->
                dbUser.getBean().getEvents());

        final Mono<ObjectId> eventId = eventsRequest.flatMap(ids -> {
            Mono<Mono<ObjectId>> event = ids.stream().map(id ->
                    this.getDBEvent(id).map(dbEvent ->
                            dbEvent.getEventName().equals(eventName)).mapNotNull(aBoolean -> {
                        if (aBoolean) {
                            return this.getDBEvent(id).map(DBEvent::getId);
                        } else
                            return null;
                    })).collect(Collectors.toList()).get(0);

            return event.flatMap(longMono -> longMono);
        });

        AtomicReference<ObjectId> id = new AtomicReference<>(new ObjectId());

        final Mono<Publisher<Document>> request = eventId.map(objectId -> {
            id.set(objectId);
            return this.getCollection()
                    .find(Filters.eq("_id", objectId))
                    .first();
        });

//        final Publisher<Document> request = this.getCollection()
//                .find(Filters.eq("_id", eventId.block()))
//                .first();

        final Mono<DBEvent> getDBEvent = request
                .flatMap(documentPublisher -> Mono.from(documentPublisher)
                        .map(document -> document.toJson(JSON_WRITER_SETTINGS))
                        .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, DBEventBean.class)))
                        .map(DBEvent::new)
                        .doOnSuccess(consumer -> {
                            if (consumer == null) {
                                LOGGER.debug("[DBEvent {}] Not found", eventName);
                            }
                        }))
                .defaultIfEmpty(new DBEvent(eventName))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBEvent {}] Request", eventName);
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                });
        return this.eventCache.getOrCache(id.get(), getDBEvent);
    }

    /*public Mono<DBEvent> getDBEvent(Long id) {
        Publisher<Document> document = this.getCollection().find(Filters.eq("_id", id)).first();
        return Mono.from(document)
                .map(document1 -> document1.toJson(JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, DBEventBean.class)))
                .map(DBEvent::new);
    }

    public Mono<DBEvent> getDBEvent(Snowflake uId, String eventName) {

        final Mono<List<Long>> eventsRequest = DatabaseManager.getUsers().getDBUser(uId).map(dbUser ->
                dbUser.getBean().getEvents());

        final Mono<Long> eventId = eventsRequest.flatMap(longs -> {
            Mono<Mono<Long>> event = longs.stream().map(aLong ->
                    this.getDBEvent(aLong).map(dbEvent ->
                            dbEvent.getEventName().equals(eventName)).map(aBoolean -> {
                        if (aBoolean) {
                            return this.getDBEvent(aLong).map(DBEvent::getId);
                        } else
                            return Mono.just(0L);
                    })).collect(Collectors.toList()).get(0);

            return event.flatMap(longMono -> longMono);
        });

        final Mono<?> request = eventId.map(id -> this.getCollection()
                .find(Filters.eq("_id", id))
                .first());

        final Mono<DBEvent> getDBEvent = Mono.from(request)
                .map(o -> (Document) o)
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
    }*/

    public void invalidateCache(ObjectId objectId) {
        if (objectId == null)
            return;
        LOGGER.trace("{Event ID: {}} Cache invalidated", objectId);
        this.eventCache.remove(objectId);
    }

    public Flux<Object> getDBMember(String eventName, Snowflake... memberIds) {
        return null;
//        return this.getDBEvent(eventName)
//                .flatMapIterable(DBEvent::getMembers)
//                .collectMap(DBEventMember::getId)
//                .flatMapIterable(dbMembers -> {
//                    // Completes the Flux with missing members from the provided array
//                    final Set<DBEventMember> members = new HashSet<>();
//                    for (final Snowflake memberId : memberIds) {
//                        final DBEventMember member = dbMembers.getOrDefault(memberId, new DBEventMember(memberId, eventName));
//                        members.add(member);
//                    }
//                    return members;
//                });
    }

    public List<DBEvent> getAllEvents() {
        List<DBEvent> dbGroups = new ArrayList<>();
        List<Document> documents = Flux.from(this.getCollection().find()).collectList().block();
        if (documents != null)
            documents.forEach(document -> {
                try {
                    DBEvent dbEvent = new DBEvent(NetUtil.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBEventBean.class));
                    dbGroups.add(dbEvent);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        return dbGroups;
    }
}
