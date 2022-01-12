package com.locibot.webapi.weatherData;

import com.github.prominence.openweathermap.api.model.onecall.current.CurrentWeatherData;

public class WeatherData {
    private final CurrentWeatherData data;

    public WeatherData(CurrentWeatherData data) {
        this.data = data;
    }

    public CurrentWeatherData getData() {
        return data;
    }
}
