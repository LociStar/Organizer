package com.locibot.locibot.database.users.entity;

import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.guilds.GuildsCollection;
import com.locibot.locibot.database.users.bean.DBUserBean;
import com.locibot.locibot.database.users.entity.achievement.Achievement;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import discord4j.common.util.Snowflake;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;

public class DBUser extends SerializableEntity<DBUserBean> implements DatabaseEntity {

    public DBUser(DBUserBean bean) {
        super(bean);
    }

    public DBUser(Snowflake id) {
        super(new DBUserBean(id.asString()));
    }

    public Snowflake getId() {
        return Snowflake.of(this.getBean().getId());
    }

    public EnumSet<Achievement> getAchievements() {
        return Achievement.of(this.getBean().getAchievements());
    }

    public Mono<UpdateResult> unlockAchievement(Achievement achievement) {
        final int achievements = this.getBean().getAchievements() | achievement.getFlag();
        return this.updateAchievement(achievements);
    }

    public Mono<UpdateResult> lockAchievement(Achievement achievement) {
        final int achievements = this.getBean().getAchievements() & ~achievement.getFlag();
        return this.updateAchievement(achievements);
    }

    private Mono<UpdateResult> updateAchievement(int achievements) {
        // If the achievement is already in this state, no need to request an update
        if (this.getBean().getAchievements() == achievements) {
            GuildsCollection.LOGGER.debug("[DBUser {}] Achievements update useless, aborting: {}", this.getId().asString(), achievements);
            return Mono.empty();
        }

        return Mono.from(DatabaseManager.getUsers()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getId().asString()),
                                Updates.set("achievements", achievements),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBUser {}] Achievements update: {}", this.getId().asString(), achievements);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBUser {}] Achievements update result: {}",
                        this.getId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getUsers().invalidateCache(this.getId()));
    }

    public Mono<UpdateResult> addEvent(ObjectId event) {
        // If the event is already in this state, no need to request an update
        if (this.getBean().getEvents().contains(event)) {
            GuildsCollection.LOGGER.debug("[DBUser {}] Add Event useless, aborting: {}", this.getId().asString(), event);
            return Mono.empty();
        }

        ArrayList<ObjectId> events = new ArrayList<>(this.getBean().getEvents());
        events.add(event);

        return Mono.from(DatabaseManager.getUsers()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getId().asString()),
                                Updates.set("events", events),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBUser {}] Event added: {}", this.getId().asString(), event);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBUser {}] Event add result: {}",
                        this.getId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getUsers().invalidateCache(this.getId()));
    }

    public Mono<UpdateResult> removeEvent(ObjectId event) {
        // If the event is already in this state, no need to request an update
        if (!this.getBean().getEvents().contains(event)) {
            GuildsCollection.LOGGER.debug("[DBUser {}] Remove Event useless, aborting: {}", this.getId().asString(), event);
            return Mono.empty();
        }

        this.getBean().getEvents().remove(event);

        return Mono.from(DatabaseManager.getUsers()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getId().asString()),
                                Updates.set("events", this.getBean().getEvents()),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBUser {}] Event removed: {}", this.getId().asString(), event);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBUser {}] Event remove result: {}",
                        this.getId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getUsers().invalidateCache(this.getId()));
    }

    public Mono<UpdateResult> addEventInvitation(ObjectId event) {
        // If the event is already in this state, no need to request an update
        if (this.getBean().getEventInvitations().contains(event)) {
            GuildsCollection.LOGGER.debug("[DBUser {}] Add EventInvitation useless, aborting: {}", this.getId().asString(), event);
            return Mono.empty();
        }

        ArrayList<ObjectId> eventInvitations = new ArrayList<>(this.getBean().getEventInvitations());
        eventInvitations.add(event);

        return Mono.from(DatabaseManager.getUsers()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getId().asString()),
                                Updates.set("event_invitations", eventInvitations),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBUser {}] EventInvitation added: {}", this.getId().asString(), event);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBUser {}] EventInvitation add result: {}",
                        this.getId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getUsers().invalidateCache(this.getId()));
    }

    public Mono<UpdateResult> removeEventInvitation(ObjectId event) {
        // If the event is already in this state, no need to request an update
        if (!this.getBean().getEventInvitations().contains(event)) {
            GuildsCollection.LOGGER.debug("[DBUser {}] Remove EventInvitation useless, aborting: {}", this.getId().asString(), event);
            return Mono.empty();
        }

        this.getBean().getEventInvitations().remove(event);

        return Mono.from(DatabaseManager.getUsers()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getId().asString()),
                                Updates.set("event_invitations", this.getBean().getEvents()),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBUser {}] EventInvitation removed: {}", this.getId().asString(), event);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBUser {}] EventInvitation remove result: {}",
                        this.getId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getUsers().invalidateCache(this.getId()));
    }

    public Mono<UpdateResult> setZoneId(ZoneId zoneId) {
        return Mono.from(DatabaseManager.getUsers()
                        .getCollection()
                        .updateOne(
                                Filters.eq("_id", this.getId().asString()),
                                Updates.set("zoneId", zoneId.toString()),
                                new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBUser {}] ZoneId set: {}", this.getId().asString(), zoneId);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBUser {}] ZoneId set result: {}",
                        this.getId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getUsers().invalidateCache(this.getId()));
    }

    public boolean hasZoneId() {
        return this.getBean().getZoneId() != null;
    }

    @Override
    public Mono<Void> insert() {
        throw new IllegalStateException();
    }

    @Override
    public Mono<Void> delete() {
        throw new IllegalStateException();
    }

    @Override
    public String toString() {
        return "DBUser{" +
                ", bean=" + this.getBean() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final DBUser dbUser = (DBUser) obj;
        return Objects.equals(this.getBean().getId(), dbUser.getBean().getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getBean().getId());
    }
}
