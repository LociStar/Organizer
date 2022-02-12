package com.locibot.webapi.weatherForecast;

import com.github.prominence.openweathermap.api.model.forecast.Forecast;
import com.github.prominence.openweathermap.api.model.onecall.current.CurrentWeatherData;

public class WeatherForecast {
    private final Forecast data;

    public WeatherForecast(Forecast data) {
        this.data = data;
    }

    public Forecast getData() {
        return data;
    }
}
