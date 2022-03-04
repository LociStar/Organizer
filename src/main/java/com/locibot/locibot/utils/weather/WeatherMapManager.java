package com.locibot.locibot.utils.weather;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.prominence.openweathermap.api.model.forecast.Forecast;

import java.io.IOException;
import java.util.Collections;

public class WeatherMapManager {
    private Forecast weather;

    public WeatherMapManager(String data) {
        String data1 = data.substring(8, data.length() - 1);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            this.weather = objectMapper.readValue(data1, Forecast.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace(); //TODO: Proper exception handling
        }
        Collections.reverse(this.weather.getWeatherForecasts());
    }

    public byte[] createHeatMap() throws IOException {
        CreateHeatMap createHeatMap = new CreateHeatMap();
        return createHeatMap.create(this.weather);
    }

    public byte[] createRainMap() throws IOException {
        CreateRainMap createRainMap = new CreateRainMap();
        return createRainMap.create(this.weather);
    }

    public Forecast getForecast() {
        return weather;
    }
}
