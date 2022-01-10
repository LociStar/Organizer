package com.locibot.locibot.utils.weather;

import net.aksingh.owmjapis.api.APIException;
import net.aksingh.owmjapis.core.OWM;
import net.aksingh.owmjapis.model.HourlyWeatherForecast;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

public class HourlyWeatherForecastClass {
    private final HourlyWeatherForecast hwf;

    public HourlyWeatherForecastClass(OWM owm, String city) {
        HourlyWeatherForecast hwf1;
        try {
            hwf1 = owm.hourlyWeatherForecastByCityName(city);
        } catch (APIException e) {
            e.printStackTrace();
            hwf1 = new HourlyWeatherForecast();
        }
        hwf = hwf1;
        Collections.reverse(Objects.requireNonNull(hwf.getDataList()));
    }

    public byte[] createHeatMap() throws IOException {
        CreateHeatMap createHeatMap = new CreateHeatMap();
        return createHeatMap.create(this.hwf);
    }

    public byte[] createRainMap() throws IOException {
        CreateRainMap createRainMap = new CreateRainMap();
        return createRainMap.create(this.hwf);
    }

    public HourlyWeatherForecast getHwf() {
        return hwf;
    }
}
