package com.locibot.locibot.database.events_db.entity;

import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.events_db.bean.DBEventBean;
import com.locibot.locibot.database.events_db.bean.DBEventMemberBean;
import com.locibot.locibot.database.groups.GroupsCollection;
import com.locibot.locibot.database.guilds.GuildsCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import discord4j.core.object.entity.User;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DBEvent extends SerializableEntity<DBEventBean> implements DatabaseEntity {
    public DBEvent(DBEventBean bean) {
        super(bean);
    }

    public DBEvent(String groupName) {
        super(new DBEventBean(groupName));
    }

    public DBEvent(String groupName, String description, String icon) {
        super(new DBEventBean(groupName, description, icon));
    }

    public String getEventName() {
        return this.getBean().getEventName();
    }

    public ObjectId getId() {
        return this.getBean().getId();
    }

    public String getEventDescription() {
        return this.getBean().getDescription();
    }

    public String getIcon() {
        return this.getBean().getIcon();
    }

    @Override
    public Mono<Void> insert() {
        return getInsertOneResultMono()
                .then();
    }


    public Mono<InsertOneResult> insertOne() {
        return getInsertOneResultMono();
    }

    @NotNull
    private Mono<InsertOneResult> getInsertOneResultMono() {
        return Mono.from(DatabaseManager.getEvents()
                        .getCollection()
                        .insertOne(this.toDocument()))
                .doOnSubscribe(__ -> {
                    GroupsCollection.LOGGER.debug("[DBGroup {}] Insertion", this.getId());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getEvents().getName()).inc();
                })
                .doOnNext(result -> GroupsCollection.LOGGER.trace("[DBGroup {}] Insertion result: {}", this.getId(), result))
                .doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.getId()));
    }

    @Override
    public Mono<Void> delete() {

        Mono<UpdateResult> deleteResult = DatabaseManager.getUsers().getDBUser(this.getOwner().getUId()).flatMap(dbUser -> dbUser.removeEvent(this.getId()));

        return deleteResult.map(updateResult -> updateResult).then(Mono.from(DatabaseManager.getEvents()
                                .getCollection()
                                .deleteOne(Filters.eq("_id", this.getId())))
                        .doOnSubscribe(__ -> {
                            GuildsCollection.LOGGER.debug("[DBEvent {}] Deletion", this.getId());
                            Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getEvents().getName()).inc();
                        })
                        .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBEvent {}] Deletion result: {}", this.getId(), result))
                        .doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.getId())))
                .then();
    }

    public List<DBEventMember> getMembers() {
        if (this.getBean().getMembers() == null) {
            return Collections.emptyList();
        }

        return this.getBean()
                .getMembers()
                .stream()
                .map(memberBean -> new DBEventMember(memberBean, this.getId()))
                .toList();
    }

    public DBEventMember getOwner() {
        for (DBEventMember member : getMembers()) {
            if (member.getBean().isOwner()) {
                return member;
            }
        }
        return getMembers().get(0);
    }

    public Mono<UpdateResult> updateSchedules(ZonedDateTime zonedDateTime) {
        return Mono.from(DatabaseManager.getEvents()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getId()),
                                List.of(Updates.set("scheduledDate", zonedDateTime.toEpochSecond())),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBEvent {}] Event update: {}", this.getId(), zonedDateTime.toEpochSecond());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBEvent {}] Event update result: {}", this.getId(), result))
                .doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.getId()));
    }

    public Mono<UpdateResult> addMember(User user, int accepted) {
        assert this.getBean().getMembers() != null;
        this.getBean().getMembers().add(new DBEventMemberBean(user.getId().asLong(), accepted, false));
        return Mono.from(DatabaseManager.getEvents()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getId()),
                                Updates.set("members", this.toDocument().get("members"))))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBEvent {}] EventMember added: {}", this.getId(), user.getId());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBEvent {}] EventMember added result: {}", this.getId(), result))
                .doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.getId()));
    }

    public boolean isScheduled() {
        return this.getBean().getScheduledDate() != null;
    }

    public Mono<UpdateResult> removeMember(DBEventMember dbEventMember) {
        DBEventMember member = this.getMembers().stream().filter(eventMember -> eventMember.getUId().asLong() == dbEventMember.getUId().asLong()).collect(Collectors.toList()).get(0);
        if (member == null || this.getBean().getMembers() == null)
            return Mono.empty();
        this.getBean().getMembers().remove(member.getBean());
        return Mono.from(DatabaseManager.getEvents()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getId()),
                                Updates.set("members", this.toDocument().get("members"))))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBEvent {}] DBEventMember removed: {}", this.getId());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBEvent {}] DBEventMember remove result: {}", this.getId(), result))
                .doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.getId()));
    }
}
