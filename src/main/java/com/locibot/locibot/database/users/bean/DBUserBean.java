package com.locibot.locibot.database.users.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;

public class DBUserBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @Nullable
    @JsonProperty("achievements")
    private Integer achievements;
    @Nullable
    @JsonProperty("events")
    private ArrayList<Long> events;

    public DBUserBean(String id, @Nullable Integer achievements, @Nullable ArrayList<Long> events) {
        this.id = id;
        this.achievements = achievements;
        this.events = events;
    }

    public DBUserBean(String id) {
        this(id, null, null);
    }

    public DBUserBean() {
    }

    public String getId() {
        return this.id;
    }

    public int getAchievements() {
        return this.achievements == null ? 0 : this.achievements;
    }

    public ArrayList<Long> getEvents() {
        return this.events == null ? new ArrayList<>() : this.events;
    }

    @Override
    public String toString() {
        return "DBUserBean{" +
                "id='" + this.id + '\'' +
                ", achievements=" + this.achievements +
                '}';
    }
}
