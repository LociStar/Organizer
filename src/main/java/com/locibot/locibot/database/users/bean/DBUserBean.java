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
    @JsonProperty("event_invitations")
    private ArrayList<ObjectId> eventInvitations;
    @Nullable
    @JsonProperty("zoneId")
    private ZoneId zoneId;
    @Nullable
    @JsonProperty("dm")
    private Boolean dm;
    @Nullable
    @JsonProperty("weatherRegistered")
    private ArrayList<String> weatherRegistered;

    public DBUserBean(String id, @Nullable Integer achievements, @Nullable ArrayList<ObjectId> events, @Nullable ArrayList<ObjectId> eventInvitations, @Nullable ZoneId zoneId, @Nullable Boolean dm) {
        this.id = id;
        this.achievements = achievements;
        this.events = events;
        this.eventInvitations = eventInvitations;
        this.zoneId = zoneId;
        this.dm = dm;
    }

    public DBUserBean(String id) {
        this(id, null, null, null, null, null);
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

    public ArrayList<ObjectId> getEventInvitations() {
        return this.eventInvitations == null ? new ArrayList<>() : this.eventInvitations;
    }

    public ArrayList<String> getWeatherRegistered() {
        return this.weatherRegistered == null ? new ArrayList<>() : this.weatherRegistered;
    }

    @Nullable
    public ZoneId getZoneId() {
        return zoneId;// == null ? ZoneId.of("Europe/Berlin") : zoneId;
    }

    public Boolean getDm() {
        return this.dm != null && this.dm;
    }

    @Override
    public String toString() {
        return "DBUserBean{" +
                "id='" + this.id + '\'' +
                ", achievements=" + this.achievements +
                '}';
    }
}
