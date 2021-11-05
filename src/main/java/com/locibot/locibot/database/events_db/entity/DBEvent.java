package com.locibot.locibot.database.events_db.entity;

import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.events_db.bean.DBEventBean;
import com.locibot.locibot.database.groups.GroupsCollection;
import com.locibot.locibot.database.guilds.GuildsCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

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

    public String getEventDescription() {
        return this.getBean().getDescription();
    }

    public String getIcon() {
        return this.getBean().getIcon();
    }

    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getEvents()
                        .getCollection()
                        .insertOne(this.toDocument()))
                .doOnSubscribe(__ -> {
                    GroupsCollection.LOGGER.debug("[DBGroup {}] Insertion", this.getEventName());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getEvents().getName()).inc();
                })
                .doOnNext(result -> GroupsCollection.LOGGER.trace("[DBGroup {}] Insertion result: {}",
                        this.getEventName(), result))
                .doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.getEventName()))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return Mono.from(DatabaseManager.getEvents()
                        .getCollection()
                        .deleteOne(Filters.eq("_id", this.getEventName())))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBEvent {}] Deletion", this.getEventName());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getEvents().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBEvent {}] Deletion result: {}", this.getEventName(), result))
                .doOnTerminate(() -> DatabaseManager.getGroups().invalidateCache(this.getEventName()))
                .then();
    }

    public List<DBEventMember> getMembers() {
        if (this.getBean().getMembers() == null) {
            return Collections.emptyList();
        }

        return this.getBean()
                .getMembers()
                .stream()
                .map(memberBean -> new DBEventMember(memberBean, getEventName()))
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
                                Filters.eq("_id", this.getEventName()),
                                List.of(Updates.set("scheduledDate", zonedDateTime.toEpochSecond())),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBEvent {}] Event update: {}", this.getEventName(), zonedDateTime.toEpochSecond());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBEvent {}] Event update result: {}",
                        this.getEventName(), result))
                .doOnTerminate(() -> DatabaseManager.getGroups().invalidateCache(this.getEventName()));
    }
}
