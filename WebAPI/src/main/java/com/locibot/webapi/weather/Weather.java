package com.locibot.webapi.weather;

import java.util.Arrays;

public class Weather {
    private byte[] weatherData;

    public Weather() {
    }

    public Weather(byte[] weatherData) {
        this.weatherData = weatherData;
    }

    public byte[] getWeatherData() {
        return this.weatherData;
    }

    public void setMessage(byte[] weatherData) {
        this.weatherData = weatherData;
    }

    @Override
    public String toString() {
        return "Login{" +
                "weatherData='" + Arrays.toString(weatherData) + '\'' +
                '}';
    }
}
