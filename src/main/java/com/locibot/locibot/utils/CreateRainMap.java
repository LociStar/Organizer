package com.locibot.locibot.utils;

import net.aksingh.owmjapis.api.APIException;
import net.aksingh.owmjapis.core.OWM;
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
import java.util.Objects;

public abstract class CreateRainMap {

    public static byte[] create(String city, OWM owm) throws IOException {
        // Create Chart
        HeatMapChart heatMapChart = new HeatMapChartBuilder().width(800).height(500).title("Rain (mm)").yAxisTitle("date").xAxisTitle("time").build();

        List<String> xData = new ArrayList<>();
        List<String> yData = new ArrayList<>();
        List<Number[]> heatData = new ArrayList<>();
        xData.add("00:00");
        xData.add("03:00");
        xData.add("06:00");
        xData.add("09:00");
        xData.add("12:00");
        xData.add("15:00");
        xData.add("18:00");
        xData.add("21:00");

        HourlyWeatherForecast hwf = null;
        try {
            hwf = owm.hourlyWeatherForecastByCityName(city);
        } catch (APIException e) {
            return new byte[0];
        }
        Collections.reverse(Objects.requireNonNull(hwf.getDataList()));

        String lastDate = "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        int countX;
        int countY = 0;
        for (int i = 0; i < hwf.getDataCount(); i++) {
            String[] time = hwf.getDataList().get(i).getDateTimeText().split(" ");
            countX = xData.indexOf(time[1].substring(0, time[1].length() - 3));
            if (!time[0].equals(lastDate)) {
                lastDate = time[0];
                yData.add(LocalDate.parse(time[0]).format(formatter));
                countY++;
            }
            var rainData = hwf.getDataList().get(i).getRainData();
            Number[] numbers = {
                    countX,
                    countY - 1,
                    hwf.getDataList().get(i).hasRainData() ? rainData.hasPrecipVol3h() ? rainData.getPrecipVol3h() : 0 : 0
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

        BitmapEncoder.saveBitmap(heatMapChart, "D:\\Programms\\Java\\IdeaProjects\\ShadbotOriginal\\src\\main\\resources\\Pictures\\Rain", BitmapEncoder.BitmapFormat.PNG);


        return BitmapEncoder.getBitmapBytes(heatMapChart, BitmapEncoder.BitmapFormat.PNG);

    }
}
