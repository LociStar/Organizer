package com.locibot.locibot.database.events_db.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import com.locibot.locibot.database.groups.bean.DBGroupMemberBean;
import reactor.util.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

public class DBEventBean implements Bean {
    @JsonProperty("_id")
    private String groupName;
    @Nullable
    @JsonProperty("description")
    private String description;
    @Nullable
    @JsonProperty("creationDate")
    private Long creationDate;
    @Nullable
    @JsonProperty("members")
    private List<DBGroupMemberBean> members;
    @Nullable
    @JsonProperty("scheduledDate")
    private Long scheduledDate;

    public DBEventBean(String groupName, @Nullable String description, @Nullable List<DBGroupMemberBean> members, @Nullable Long scheduledDate) {
        this.groupName = groupName;
        this.description = description;
        this.creationDate = ZonedDateTime.now(ZoneId.of("Europe/Berlin")).toEpochSecond(); //TODO: Make ZoneOffset dependent on event location
        this.members = members;
        this.scheduledDate = scheduledDate;
    }

    public DBEventBean(String eventName) {
        this(eventName, null, null, null);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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
    public List<DBGroupMemberBean> getMembers() {
        return members;
    }

    public void setMembers(@Nullable List<DBGroupMemberBean> members) {
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
                "groupName='" + groupName + '\'' +
                ", description='" + description + '\'' +
                ", creationDate=" + creationDate +
                ", members=" + members +
                ", scheduledDate=" + scheduledDate +
                '}';
    }
}

