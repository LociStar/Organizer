package com.locibot.locibot.database.groups.entity;

import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.groups.GroupsCollection;
import com.locibot.locibot.database.groups.bean.DBGroupBean;
import com.locibot.locibot.database.groups.bean.DBGroupMemberBean;
import com.locibot.locibot.database.guilds.GuildsCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

public class DBGroup extends SerializableEntity<DBGroupBean> implements DatabaseEntity {

    public DBGroup(DBGroupBean groupBean) {
        super(groupBean);
    }

    public DBGroup(String groupName) {
        super(new DBGroupBean(groupName));
    }

    public DBGroup(String groupName, int groupType) {
        super(new DBGroupBean(groupName, groupType));
    }

    public String getGroupName() {
        return this.getBean().getGroupName();
    }

    public List<DBGroupMember> getMembers() {
        if (this.getBean().getMembers() == null) {
            return Collections.emptyList();
        }

        return this.getBean()
                .getMembers()
                .stream()
                .map(memberBean -> new DBGroupMember(memberBean, getGroupName()))
                .toList();
    }

    public DBGroupMember getOwner() {
        for (DBGroupMember member : getMembers()) {
            if (member.getBean().isOwner()) {
                return member;
            }
        }
        return getMembers().get(0);
    }

    public Mono<UpdateResult> updateSchedules(LocalDate localDate, LocalTime localTime) {
        return Mono.from(DatabaseManager.getGroups()
                .getCollection()
                .updateOne(
                        Filters.eq("_id", this.getGroupName()),
                        List.of(Updates.set("scheduledDate", localDate.toString()),
                                Updates.set("scheduledTime", localTime.toString())),
                        new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBGroup {}] Group update: {}", this.getGroupName(), localDate.toString() + " " + localTime.toString());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBGroup {}] Group update result: {}",
                        this.getGroupName(), result))
                .doOnTerminate(() -> DatabaseManager.getGroups().invalidateCache(this.getGroupName()));
    }

    public Mono<UpdateResult> updateInvited(Snowflake id, boolean state) {
        return Mono.from(DatabaseManager.getGroups()
                .getCollection()
                .updateOne(
                        Filters.and(Filters.eq("_id", this.getGroupName()),
                                Filters.eq("members._id", id.asLong())),
                        Updates.set("members.$.invited", state)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBGroup {}] Group update: {}", this.getGroupName(), state);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBGroup {}] Group update result: {}",
                        this.getGroupName(), result))
                .doOnTerminate(() -> DatabaseManager.getGroups().invalidateCache(this.getGroupName()));
    }

    public Mono<UpdateResult> updateAccept(Snowflake id, int state) {
        return Mono.from(DatabaseManager.getGroups()
                .getCollection()
                .updateOne(
                        Filters.and(Filters.eq("_id", this.getGroupName()),
                                Filters.eq("members._id", id.asLong())),
                        Updates.set("members.$.accepted", state)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBGroup {}] Group update: {}", this.getGroupName(), state);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBGroup {}] Group update result: {}",
                        this.getGroupName(), result))
                .doOnTerminate(() -> DatabaseManager.getGroups().invalidateCache(this.getGroupName()));
    }

    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getGroups()
                .getCollection()
                .insertOne(this.toDocument()))
                .doOnSubscribe(__ -> {
                    GroupsCollection.LOGGER.debug("[DBGroup {}] Insertion", this.getGroupName());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGroups().getName()).inc();
                })
                .doOnNext(result -> GroupsCollection.LOGGER.trace("[DBGroup {}] Insertion result: {}",
                        this.getGroupName(), result))
                .doOnTerminate(() -> DatabaseManager.getGroups().invalidateCache(this.getGroupName()))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return Mono.from(DatabaseManager.getGroups()
                .getCollection()
                .deleteOne(Filters.eq("_id", this.getGroupName())))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBGroup {}] Deletion", this.getGroupName());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGroups().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBGroup {}] Deletion result: {}", this.getGroupName(), result))
                .doOnTerminate(() -> DatabaseManager.getGroups().invalidateCache(this.getGroupName()))
                .then();
    }

    public Mono<UpdateResult> addMember(User user) {
        this.getBean().getMembers().add(new DBGroupMemberBean(user.getId().asLong(), this.getBean().getGroupName(), true, false, 0, false));
        return Mono.from(DatabaseManager.getGroups()
                .getCollection()
                .updateOne(
                        Filters.eq("_id", this.getGroupName()),
                        Updates.set("members", this.toDocument().get("members"))))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBGroup {}] GroupMember added: {}", this.getGroupName(), user.getId());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getUsers().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBGroup {}] GroupMember added result: {}",
                        this.getGroupName(), result))
                .doOnTerminate(() -> DatabaseManager.getGroups().invalidateCache(this.getGroupName()));
    }
}
