package com.locibot.locibot.utils.weather;

import net.aksingh.owmjapis.model.HourlyWeatherForecast;
import org.knowm.xchart.XYChartBuilder;

public class CreateRainDiagram {

    public byte[] create(HourlyWeatherForecast hwf){

        XYChartBuilder xyChartBuilder = new XYChartBuilder();

        xyChartBuilder.xAxisTitle("Time");
        xyChartBuilder.yAxisTitle("Volume/Percentile");


        return null;

    }

}
