package com.locibot.webapi.weatherForecast;

public class WeatherForecastError {
    private final String error;

    public WeatherForecastError(String errorType) {
        this.error = errorType;
    }

    public String getError() {
        return error;
    }
}
