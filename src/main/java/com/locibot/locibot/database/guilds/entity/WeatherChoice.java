package com.locibot.locibot.database.guilds.entity;

public enum WeatherChoice {
    SUBSCRIBE(true),
    UNSUBSCRIBE(false);

    private final Boolean b;

    WeatherChoice(Boolean b) {
        this.b = b;
    }

    public Boolean getB() {
        return b;
    }
}
