package com.locibot.locibot.database.locations.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

public class DBLocationBean implements Bean {
    @JsonProperty("name")
    private String name;
    @JsonProperty("longitude")
    private double longitude;
    @JsonProperty("latitude")
    private double latitude;
    @Nullable
    @JsonProperty("weatherData")
    private String weatherData;

    public DBLocationBean(String name, double longitude, double latitude, @Nullable String weatherData) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.weatherData = weatherData;
    }

    public DBLocationBean(String name, double longitude, double latitude) {
        this(name, longitude, latitude, null);
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

    public String getWeatherData() {
        return weatherData;
    }
}
