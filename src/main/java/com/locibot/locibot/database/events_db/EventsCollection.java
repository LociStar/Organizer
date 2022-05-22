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

public class EventsCollection extends DatabaseCollection { //TODO: replace console prints with logs

    public static final Logger LOGGER = LogUtil.getLogger(EventsCollection.class, LogUtil.Category.DATABASE);
    private final MultiValueCache<ObjectId, DBEvent> eventCache;

    public EventsCollection(MongoDatabase database) {
        super(database, "events");
        this.eventCache = MultiValueCache.Builder.<ObjectId, DBEvent>builder().withInfiniteTtl().build();
    }

    @Deprecated
    public boolean containsEvent(ObjectId objectId) {
        List<Document> documents = Flux.from(this.getCollection().find()).collectList().block();
        if (documents != null) {
            for (Document document : documents) {
                try {
                    if (NetUtil.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBEventBean.class).getId().equals(objectId))
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

        AtomicReference<ObjectId> id = new AtomicReference<>(new ObjectId());

        Mono<List<ObjectId>> idList = DatabaseManager.getUsers().getDBUser(uId).map(dbUser -> dbUser.getBean().getEvents());
        Flux<DBEvent> events = idList.flatMapMany(Flux::fromIterable).flatMap(this::getDBEvent);
        Mono<DBEvent> dbEventMono = events.filter(dbEvent -> {
                    return dbEvent.getEventName().equals(eventName);
                }).switchIfEmpty(Flux.empty())
                .collectList().defaultIfEmpty(List.of(new DBEvent("empty")))
                .mapNotNull(dbEvents -> {
                    if (dbEvents.size() == 0)
                        return null;
                    id.set(dbEvents.get(0).getId());
                    return dbEvents.get(0);
                });

        return this.eventCache.getOrCache(id.get(), dbEventMono);
    }

    public void invalidateCache(ObjectId objectId) {
        if (objectId == null)
            return;
        LOGGER.trace("{Event ID: {}} Cache invalidated", objectId);
        this.eventCache.remove(objectId);
    }

//    public Flux<Object> getDBMember(String eventName, Snowflake... memberIds) {
//        return null;
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
//    }

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

    public Flux<DBEvent> getAllEvents(Snowflake uId) {
        final Mono<List<ObjectId>> eventsRequest = DatabaseManager.getUsers().getDBUser(uId).map(dbUser ->
                dbUser.getBean().getEvents());

        final Flux<DBEvent> events = eventsRequest.flatMapMany(Flux::fromIterable)
                .flatMap(objectId -> DatabaseManager.getEvents().getDBEvent(objectId));

        return events
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBEvent {}] Request", events.toString());
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                });
    }
}
