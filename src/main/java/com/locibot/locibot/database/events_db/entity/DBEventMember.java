package com.locibot.locibot.database.events_db.entity;

import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.events_db.bean.DBEventMemberBean;
import com.locibot.locibot.database.groups.GroupsCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

public class DBEventMember extends SerializableEntity<DBEventMemberBean> implements DatabaseEntity {

    private final String groupName;

    public DBEventMember(DBEventMemberBean bean, String groupName) {
        super(bean);
        this.groupName = groupName;
    }

    public DBEventMember(Snowflake id, String groupName) {
        super(new DBEventMemberBean(id.asLong()));
        this.groupName = groupName;
    }

    public DBEventMember(Long id, @Nullable String name, int accepted, boolean owner) {
        super(new DBEventMemberBean(id, name, accepted, owner));
        this.groupName = name;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public Snowflake getId() {
        return Snowflake.of(this.getBean().getId());
    }

    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getEvents()
                .getCollection()
                .updateOne(Filters.eq("_id", this.getGroupName()),
                        Updates.push("members", this.toDocument()),
                        new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GroupsCollection.LOGGER.debug("[DBEventMember {}/{}] Insertion", this.getId().asString(), this.getGroupName());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getEvents().getName()).inc();
                })
                .doOnNext(result -> GroupsCollection.LOGGER.trace("[DBEventMember {}/{}] Insertion result: {}",
                        this.getId().asString(), this.getGroupName(), result))
                //.doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.getGroupName()))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        throw new IllegalStateException();
    }
}
