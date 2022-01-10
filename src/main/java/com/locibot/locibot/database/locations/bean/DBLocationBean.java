package com.locibot.locibot.database.locations.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

public class DBLocationBean implements Bean {
    @JsonProperty("name")
    private String name;
    @JsonProperty("longitude")
    private long longitude;
    @JsonProperty("latitude")
    private long latitude;
    @Nullable
    @JsonProperty("weatherData")
    private String weatherData;

    public DBLocationBean(String name, long longitude, long latitude, @Nullable String weatherData) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.weatherData = weatherData;
    }

    public DBLocationBean(String name, long longitude, long latitude) {
        this(name, longitude, latitude, null);
    }

    public String getName() {
        return name;
    }

    public long getLongitude() {
        return longitude;
    }

    public long getLatitude() {
        return latitude;
    }

    public String getWeatherData() {
        return weatherData;
    }
}
