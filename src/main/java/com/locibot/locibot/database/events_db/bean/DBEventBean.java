package com.locibot.locibot.database.events_db.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class DBEventBean implements Bean {
    @JsonProperty("_id")
    private String eventName;
    @Nullable
    @JsonProperty("description")
    private String description;
    @JsonProperty("creationDate")
    private Long creationDate;
    @Nullable
    @JsonProperty("members")
    private List<DBEventMemberBean> members;
    @Nullable
    @JsonProperty("scheduledDate")
    private Long scheduledDate;

    public DBEventBean(String eventName, @Nullable String description, @Nullable List<DBEventMemberBean> members, @Nullable Long scheduledDate) {
        this.eventName = eventName;
        this.description = description;
        this.creationDate = ZonedDateTime.now(ZoneId.of("Europe/Berlin")).toEpochSecond(); //TODO: Make ZoneOffset dependent on event location
        this.members = members;
        this.scheduledDate = scheduledDate;
    }

    public DBEventBean(String eventName) {
        this(eventName, null, null, null);
    }

    public DBEventBean(String eventName, String description) {
        this(eventName, description, null, null);
    }

    public DBEventBean(){
        this.creationDate = ZonedDateTime.now(ZoneId.of("Europe/Berlin")).toEpochSecond(); //TODO: Make ZoneOffset dependent on event location
    }

    public String getEventName() {
        return eventName;
    }

    public void setGroupName(String groupName) {
        this.eventName = groupName;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(@Nullable Long creationDate) {
        this.creationDate = creationDate;
    }

    @Nullable
    public List<DBEventMemberBean> getMembers() {
        return members;
    }

    public void setMembers(@Nullable List<DBEventMemberBean> members) {
        this.members = members;
    }

    @Nullable
    public Long getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(@Nullable Long scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    @Override
    public String toString() {
        return "DBEventBean{" +
                "groupName='" + eventName + '\'' +
                ", description='" + description + '\'' +
                ", creationDate=" + creationDate +
                ", members=" + members +
                ", scheduledDate=" + scheduledDate +
                '}';
    }
}

