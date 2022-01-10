package com.locibot.webapi.weatherData;

import com.github.prominence.openweathermap.api.OpenWeatherMapClient;
import com.github.prominence.openweathermap.api.enums.Language;
import com.github.prominence.openweathermap.api.enums.OneCallResultOptions;
import com.github.prominence.openweathermap.api.enums.UnitSystem;
import com.github.prominence.openweathermap.api.model.Coordinate;
import com.github.prominence.openweathermap.api.model.onecall.current.CurrentWeatherData;

public class WeatherData {
    private CurrentWeatherData data;

    public WeatherData(Double latitude, Double longitude) {

        OpenWeatherMapClient openWeatherClient = new OpenWeatherMapClient("9c891f167c39ac0db5d964edaeac69bd");

        this.data = openWeatherClient
                .oneCall()
                .current()
                .byCoordinate(Coordinate.of(latitude, longitude))
                .language(Language.GERMAN)
                .unitSystem(UnitSystem.METRIC)
                .exclude(OneCallResultOptions.HOURLY, OneCallResultOptions.MINUTELY)
                .retrieve()
                .asJava();

    }

    public CurrentWeatherData getData() {
        return data;
    }
}
