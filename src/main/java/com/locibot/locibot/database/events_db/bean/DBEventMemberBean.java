package com.locibot.locibot.database.events_db.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;

public class DBEventMemberBean implements Bean {
    @JsonProperty("_id")
    private Long id;
    @JsonProperty("accepted") // 0==invited; 1==accepted; 2==declined
    private int accepted;
    @JsonProperty("owner")
    private boolean owner;

    public DBEventMemberBean(Long id, int accepted, boolean owner) {
        this.id = id;
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
        return "DBEventMemberBean{" +
                "id=" + id +
                ", accepted=" + accepted +
                ", owner=" + owner +
                '}';
    }
}
