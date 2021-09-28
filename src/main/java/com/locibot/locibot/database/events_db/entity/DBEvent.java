package com.locibot.locibot.database.events_db.entity;

import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.events_db.bean.DBEventBean;
import com.locibot.locibot.database.groups.GroupsCollection;
import reactor.core.publisher.Mono;

public class DBEvent extends SerializableEntity<DBEventBean> implements DatabaseEntity {
    public DBEvent(DBEventBean bean) {
        super(bean);
    }

    public DBEvent(String groupName) {
        super(new DBEventBean(groupName));
    }

    public String getGroupName() {
        return this.getBean().getGroupName();
    }

    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getEvents()
                .getCollection()
                .insertOne(this.toDocument()))
                .doOnSubscribe(__ -> {
                    GroupsCollection.LOGGER.debug("[DBGroup {}] Insertion", this.getGroupName());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getEvents().getName()).inc();
                })
                .doOnNext(result -> GroupsCollection.LOGGER.trace("[DBGroup {}] Insertion result: {}",
                        this.getGroupName(), result))
                //.doOnTerminate(() -> DatabaseManager.getEvents().invalidateCache(this.getGroupName()))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return null;
    }
}
