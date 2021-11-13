package com.locibot.locibot.database.events_db.entity;

import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.events_db.bean.DBEventMemberBean;
import com.locibot.locibot.database.groups.GroupsCollection;
import com.locibot.locibot.database.guilds.GuildsCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

public class DBEventMember extends SerializableEntity<DBEventMemberBean> implements DatabaseEntity {

    private final String eventName;

    public DBEventMember(DBEventMemberBean bean, String eventName) {
        super(bean);
        this.eventName = eventName;
    }

    public DBEventMember(Snowflake id, String eventName) {
        super(new DBEventMemberBean(id.asLong()));
        this.eventName = eventName;
    }

    public DBEventMember(Long id, @Nullable String name, int accepted, boolean owner) {
        super(new DBEventMemberBean(id, name, accepted, owner));
        this.eventName = name;
    }

    public String getEventName() {
        return this.eventName;
    }

    public Snowflake getId() {
        return Snowflake.of(this.getBean().getId());
    }

    public int getAccepted() {
        return this.getBean().getAccepted();
    }

    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getEvents()
                        .getCollection()
                        .updateOne(Filters.eq("_id", this.getEventName()),
                                Updates.push("members", this.toDocument()),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GroupsCollection.LOGGER.debug("[DBEventMember {}/{}] Insertion", this.getId().asString(), this.getEventName());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getEvents().getName()).inc();
                })
                .doOnNext(result -> GroupsCollection.LOGGER.trace("[DBEventMember {}/{}] Insertion result: {}",
                        this.getId().asString(), this.getEventName(), result))
                .doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.getEventName()))
                .then();
    }

    public Mono<UpdateResult> updateAccepted(int state) {
        return Mono.from(DatabaseManager.getEvents()
                        .getCollection()
                        .updateOne(
                                Filters.and(Filters.eq("_id", this.getEventName()),
                                        Filters.eq("members._id", this.getId().asLong())),
                                Updates.set("members.$.accepted", state)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBEvent {}] Event update: {}", this.getEventName(), state);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBEvent {}] Event update result: {}",
                        this.getEventName(), result))
                .doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.getEventName()));
    }

    @Override
    public Mono<Void> delete() {
        throw new IllegalStateException();
    }
}
