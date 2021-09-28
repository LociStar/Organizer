package com.locibot.locibot.database.events_db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.locibot.locibot.database.DatabaseCollection;
import com.locibot.locibot.database.groups.bean.DBGroupBean;
import com.locibot.locibot.utils.NetUtil;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import reactor.core.publisher.Flux;

import java.util.List;

public class EventsCollection extends DatabaseCollection {
    public EventsCollection(MongoDatabase database) {
        super(database, "events");
    }

    public boolean containsEvent(String groupName) {
        List<Document> documents = Flux.from(this.getCollection().find()).collectList().block();
        if (documents != null) {
            for (Document document : documents) {
                try {
                    if (NetUtil.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBGroupBean.class).getGroupName().equals(groupName))
                        return true;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
