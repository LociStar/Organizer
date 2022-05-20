package com.locibot.locibot.database.users.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import org.bson.types.ObjectId;
import reactor.util.annotation.Nullable;

import java.time.ZoneId;
import java.util.ArrayList;

public class DBUserBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @Nullable
    @JsonProperty("achievements")
    private Integer achievements;
    @Nullable
    @JsonProperty("events")
    private ArrayList<ObjectId> events;
    @Nullable
    @JsonProperty("zoneId")
    private ZoneId zoneId;

    public DBUserBean(String id, @Nullable Integer achievements, @Nullable ArrayList<ObjectId> events, @Nullable ZoneId zoneId) {
        this.id = id;
        this.achievements = achievements;
        this.events = events;
        this.zoneId = zoneId;
    }

    public DBUserBean(String id) {
        this(id, null, null, null);
    }

    public DBUserBean() {
    }

    public String getId() {
        return this.id;
    }

    public int getAchievements() {
        return this.achievements == null ? 0 : this.achievements;
    }

    public ArrayList<ObjectId> getEvents() {
        return this.events == null ? new ArrayList<>() : this.events;
    }

    @Nullable
    public ZoneId getZoneId() {
        return zoneId == null ? ZoneId.of("Europe/Berlin") : zoneId;
    }

    @Override
    public String toString() {
        return "DBUserBean{" +
                "id='" + this.id + '\'' +
                ", achievements=" + this.achievements +
                '}';
    }
}
