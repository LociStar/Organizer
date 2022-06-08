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
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;

public class DBEventMember extends SerializableEntity<DBEventMemberBean> implements DatabaseEntity {

    private final ObjectId eventId;

    public DBEventMember(DBEventMemberBean bean, ObjectId eventId) {
        super(bean);
        this.eventId = eventId;
    }

    public DBEventMember(Snowflake uId, ObjectId eventId) {
        super(new DBEventMemberBean(uId.asLong()));
        this.eventId = eventId;
    }

    public DBEventMember(Long uId, ObjectId eventId, int accepted, boolean owner) {
        super(new DBEventMemberBean(uId, accepted, owner));
        this.eventId = eventId;
    }

    public Snowflake getUId() {
        return Snowflake.of(this.getBean().getId());
    }

    public int getAccepted() {
        return this.getBean().getAccepted();
    }

    @Override
    public Mono<Void> insert() {

        Mono<Void> insertEventInvitation = DatabaseManager.getUsers().getDBUser(this.getUId()).flatMap(dbUser ->
                dbUser.addEventInvitation(this.eventId)).then();

        return Mono.from(DatabaseManager.getEvents()
                        .getCollection()
                        .updateOne(Filters.eq("_id", this.eventId),
                                Updates.push("members", this.toDocument()),
                                new UpdateOptions().upsert(false)))
                .doOnSubscribe(__ -> {
                    GroupsCollection.LOGGER.debug("[DBEventMember {}/{}] Insertion", this.getUId().asString(), this.eventId);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getEvents().getName()).inc();
                })
                .doOnNext(result -> GroupsCollection.LOGGER.trace("[DBEventMember {}/{}] Insertion result: {}",
                        this.getUId().asString(), this.eventId, result))
                .doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.eventId))
                .then(insertEventInvitation);
    }

    public Mono<UpdateResult> updateAccepted(int state) {
        return Mono.from(DatabaseManager.getEvents()
                        .getCollection()
                        .updateOne(
                                Filters.and(Filters.eq("_id", this.eventId),
                                        Filters.eq("members._id", this.getUId().asLong())),
                                Updates.set("members.$.accepted", state)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBEvent {}] Event update: {}", this.eventId, state);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBEvent {}] Event update result: {}",
                        this.eventId, result))
                .doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.eventId));
    }

    @Override
    public Mono<Void> delete() {
        throw new IllegalStateException();
    }
}
