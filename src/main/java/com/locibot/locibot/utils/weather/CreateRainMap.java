package com.locibot.locibot.utils.weather;

import com.github.prominence.openweathermap.api.model.forecast.Forecast;
import net.aksingh.owmjapis.model.HourlyWeatherForecast;
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

public class CreateRainMap {

    public byte[] create(Forecast forecast) throws IOException {
        // Create Chart
        HeatMapChart heatMapChart = new HeatMapChartBuilder().width(800).height(500).title("Rain (mm)").yAxisTitle("date").xAxisTitle("time").build();

        List<String> xData = new ArrayList<>();
        List<String> yData = new ArrayList<>();
        List<Number[]> heatData = new ArrayList<>();

        String lastDate = "";
        DateTimeFormatter preFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        int countX;
        int countY = 0;

        List<String> xDataUnsorted = new ArrayList<>(); //TODO: Here might be a more efficient solution possible
        for (int i = 0; i < forecast.getWeatherForecasts().size(); i++) {
            String[] time = forecast.getWeatherForecasts().get(i).getForecastTime().format(preFormatter).split(" ");
            countX = xDataUnsorted.indexOf(time[1].substring(0, time[1].length() - 3));
            if (countX == -1) {
                xDataUnsorted.add(time[1].substring(0, time[1].length() - 3));
            }
        }
        xData = xDataUnsorted.stream().sorted().collect(Collectors.toList());

        for (int i = 0; i < forecast.getWeatherForecasts().size(); i++) {
            String[] time = forecast.getWeatherForecasts().get(i).getForecastTime().format(preFormatter).split(" ");
            countX = xData.indexOf(time[1].substring(0, time[1].length() - 3));
            if (!time[0].equals(lastDate)) {
                lastDate = time[0];
                yData.add(LocalDate.parse(time[0]).format(DateTimeFormatter.ofPattern("EE dd.MM.yy")));
                countY++;
            }
            var rainData = forecast.getWeatherForecasts().get(i).getRain();
            Number[] numbers = {
                    countX,
                    countY - 1,
                    rainData == null ? 0 : rainData.getThreeHourLevel() == 0.0 ? 0 : rainData.getThreeHourLevel()
            };
            heatData.add(numbers);
        }

        HeatMapSeries heatMapSeries = heatMapChart.addSeries("RainMap", xData, yData, heatData);
        heatMapSeries.setMin(0);
        heatMapSeries.setMax(100);

        Color[] rangeColors = {new Color(255, 255, 255), new Color(28, 73, 255), new Color(0, 22, 190), new Color(0, 19, 98)};
        heatMapChart.getStyler().setRangeColors(rangeColors);
        heatMapChart.getStyler().setShowValue(true);
        heatMapChart.getStyler().setHeatMapValueDecimalPattern("###,###.###");
        heatMapChart.getStyler().setValueFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        heatMapChart.getStyler().setPlotBackgroundColor(new Color(72, 72, 72));
        heatMapChart.getStyler().setChartBackgroundColor(new Color(122, 122, 122));
        heatMapChart.getStyler().setLegendBackgroundColor(new Color(83, 83, 83));
        heatMapChart.getStyler().setLegendFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        heatMapChart.getStyler().setAxisTickLabelsFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        heatMapChart.getStyler().setChartTitleFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        heatMapChart.getStyler().setPlotContentSize(1);
        heatMapChart.getStyler().setAxisTicksLineVisible(false);
        heatMapChart.getStyler().setPlotGridLinesVisible(false);

        //BitmapEncoder.saveBitmap(heatMapChart, "D:\\Programms\\Java\\IdeaProjects\\ShadbotOriginal\\src\\main\\resources\\Pictures\\Rain", BitmapEncoder.BitmapFormat.PNG);

        return BitmapEncoder.getBitmapBytes(heatMapChart, BitmapEncoder.BitmapFormat.PNG);

    }
}
