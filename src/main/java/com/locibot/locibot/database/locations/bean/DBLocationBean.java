package com.locibot.locibot.database.locations.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

public class DBLocationBean implements Bean {
    @JsonProperty("_id")
    private String name;
    @JsonProperty("longitude")
    private double longitude;
    @JsonProperty("latitude")
    private double latitude;
    @Nullable
    @JsonProperty("weatherData")
    private String weatherData;
    @Nullable
    @JsonProperty("creationTime")
    private long creationTime;

    public DBLocationBean(String name, double longitude, double latitude, @Nullable String weatherData, @Nullable long creationTime) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.weatherData = weatherData;
        this.creationTime = creationTime;
    }

    public DBLocationBean(String name, double longitude, double latitude) {
        this(name, longitude, latitude, null, 0L);
    }

    public DBLocationBean() {
    }

    public String getName() {
        return name;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    @org.jetbrains.annotations.Nullable
    public String getWeatherData() {
        return weatherData;
    }

    public long getCreationTime() {
        return creationTime;
    }
}
