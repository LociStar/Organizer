package com.locibot.locibot.utils.weather;

import com.github.prominence.openweathermap.api.model.forecast.Forecast;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.HeatMapChart;
import org.knowm.xchart.HeatMapChartBuilder;
import org.knowm.xchart.HeatMapSeries;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CreateHeatMap {

    public byte[] create(Forecast weather) throws IOException {
        // Create Chart
        HeatMapChart heatMapChart = new HeatMapChartBuilder().width(800).height(500).title("Temperature (°C)").yAxisTitle("date").xAxisTitle("time").build();

        List<String> xData = new ArrayList<>();
        List<String> yData = new ArrayList<>();
        List<Number[]> heatData = new ArrayList<>();
        /*xData.add("00:00");
        xData.add("03:00");
        xData.add("06:00");
        xData.add("09:00");
        xData.add("12:00");
        xData.add("15:00");
        xData.add("18:00");
        xData.add("21:00");*/

        String lastDate = "";
        DateTimeFormatter preFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        int countX;
        int countY = 0;

        //create xData list
        List<String> xDataUnsorted = new ArrayList<>(); //TODO: Here might be a more efficient solution possible
        for (int i = 0; i < weather.getWeatherForecasts().size(); i++) {
            String[] time = weather.getWeatherForecasts().get(i).getForecastTime().format(preFormatter).split(" ");
            countX = xDataUnsorted.indexOf(time[1].substring(0, time[1].length() - 3));
            if (countX == -1) {
                xDataUnsorted.add(time[1].substring(0, time[1].length() - 3));
            }
        }
        xData = xDataUnsorted.stream().sorted().collect(Collectors.toList());

        for (int i = 0; i < weather.getWeatherForecasts().size(); i++) {
            String[] time = weather.getWeatherForecasts().get(i).getForecastTime().format(preFormatter).split(" ");
            countX = xData.indexOf(time[1].substring(0, time[1].length() - 3));
            if (!time[0].equals(lastDate)) {
                lastDate = time[0];
                yData.add(LocalDate.parse(time[0]).format(DateTimeFormatter.ofPattern("EE dd.MM.yy")));
                countY++;
            }
            var mainData = weather.getWeatherForecasts().get(i);
            Number[] numbers = {
                    countX,
                    countY - 1,
                    mainData.getTemperature().getValue() == 0 ? 0 : Math.round(mainData.getTemperature().getValue())
            };
            heatData.add(numbers);
        }

        HeatMapSeries heatMapSeries = heatMapChart.addSeries("heatMap", xData, yData, heatData);
        heatMapSeries.setMin(-20);
        heatMapSeries.setMax(40);

        Color[] rangeColors = {new Color(0, 102, 255), new Color(234, 169, 5), new Color(139, 0, 0)};
        heatMapChart.getStyler().setRangeColors(rangeColors);
        heatMapChart.getStyler().setShowValue(true);
        heatMapChart.getStyler().setPlotBackgroundColor(new Color(72, 72, 72));
        heatMapChart.getStyler().setChartBackgroundColor(new Color(122, 122, 122));
        heatMapChart.getStyler().setLegendBackgroundColor(new Color(83, 83, 83));
        heatMapChart.getStyler().setLegendFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        heatMapChart.getStyler().setAxisTickLabelsFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        heatMapChart.getStyler().setChartTitleFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        heatMapChart.getStyler().setPlotContentSize(1);
        heatMapChart.getStyler().setAxisTicksLineVisible(false);
        heatMapChart.getStyler().setPlotGridLinesVisible(false);

        return BitmapEncoder.getBitmapBytes(heatMapChart, BitmapEncoder.BitmapFormat.PNG);
        //BitmapEncoder.saveBitmap(heatMapChart, "D:\\Programms\\Java\\IdeaProjects\\ShadbotOriginal\\src\\main\\resources\\Pictures\\test", BitmapEncoder.BitmapFormat.PNG);

    }
}
