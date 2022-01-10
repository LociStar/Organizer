package com.locibot.webapi.weatherData;

public class WeatherError {
    private final String error;

    public WeatherError(String errorType){
        this.error = errorType;
    }

    public String getError() {
        return error;
    }
}
