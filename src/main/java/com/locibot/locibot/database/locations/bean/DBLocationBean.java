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
    @Nullable
    @JsonProperty("fiveDayWeatherData")
    private String fiveDayWeatherData;
    @Nullable
    @JsonProperty("fiveDayCreationTime")
    private long fiveDayCreationTime;
    @Nullable
    @JsonProperty("currentWeatherData")
    private String currentWeatherData;
    @Nullable
    @JsonProperty("currentWeatherCreationTime")
    private long currentWeatherCreationTime;

    public DBLocationBean(String name, double longitude, double latitude, @Nullable String weatherData, @Nullable long creationTime, @Nullable String fiveDayWeatherData, @Nullable long fiveDayCreationTime, @Nullable String currentWeatherData, @Nullable long currentWeatherCreationTime) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.weatherData = weatherData;
        this.creationTime = creationTime;
        this.fiveDayWeatherData = fiveDayWeatherData;
        this.fiveDayCreationTime = fiveDayCreationTime;
    }

    public DBLocationBean(String name, double longitude, double latitude) {
        this(name, longitude, latitude, null, 0L, null, 0L, null, 0L);
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

    @Nullable
    public String getFiveDayWeatherData() {
        return fiveDayWeatherData;
    }

    public long getFiveDayCreationTime() {
        return fiveDayCreationTime;
    }

    @Nullable
    public String getCurrentWeatherData() {
        return currentWeatherData;
    }

    public long getCurrentWeatherCreationTime() {
        return currentWeatherCreationTime;
    }
}
