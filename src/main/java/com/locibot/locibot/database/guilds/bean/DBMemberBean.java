package com.locibot.locibot.database.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;

public class DBMemberBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @Nullable
    @JsonProperty("coins")
    private Long coins;
    @Nullable
    @JsonProperty("weatherRegistered")
    private ArrayList<String> weatherRegistered;
    @Nullable
    @JsonProperty("botRegistered")
    private boolean botRegistered;

    public DBMemberBean(String id, @Nullable Long coins, @Nullable ArrayList<String> weatherRegistered, @Nullable boolean botRegistered) {
        this.id = id;
        this.coins = coins;
        this.weatherRegistered = weatherRegistered;
        this.botRegistered = botRegistered;
    }

    public DBMemberBean(String id) {
        this(id, null, null, false);
    }

    public DBMemberBean() {
    }

    public String getId() {
        return this.id;
    }

    public long getCoins() {
        return this.coins == null ? 0 : this.coins;
    }

    public ArrayList<String> getWeatherRegistered() {
        return this.weatherRegistered == null ? new ArrayList<>() : this.weatherRegistered;
    }

    public boolean getBotRegister(){
        return this.botRegistered;
    }

    @Override
    public String toString() {
        return "DBMemberBean{" +
                "id=" + this.id +
                ", coins=" + this.coins +
                ", weatherRegistered=" + this.weatherRegistered +
                '}';
    }
}
