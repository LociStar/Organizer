package com.locibot.locibot.database.events_db.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

public class DBEventMemberBean implements Bean {
    @JsonProperty("_id")
    private Long id;
    @Nullable
    @JsonProperty("eventName")
    private String name;
    @JsonProperty("accepted") // 0==invited; 1==accepted; 2==declined
    private int accepted;
    @JsonProperty("owner")
    private boolean owner;

    public DBEventMemberBean(Long id, @Nullable String name, int accepted, boolean owner) {
        this.id = id;
        this.name = name;
        this.accepted = accepted;
        this.owner = owner;
    }

    public DBEventMemberBean(long id) {
        this.id = id;
    }

    public DBEventMemberBean() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Nullable
    public String getEventName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public int getAccepted() {
        return accepted;
    }

    public void setAccepted(int accepted) {
        this.accepted = accepted;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "DBGroupMemberBean{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", accepted=" + accepted +
                ", owner=" + owner +
                '}';
    }
}
