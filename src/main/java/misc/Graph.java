package misc;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;


public class Graph {
    public static void draw(String title, String name, Stage stage, TreeMap<String, Integer> data) {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Number of Hours");
        final var lineChart = new LineChart<String, Number>(xAxis, yAxis);

        lineChart.setTitle(title);
        var series = new XYChart.Series<String, Number>();
        series.setName(name);
        List<XYChart.Data<String, Number>> dataList =
                data.entrySet().stream().map(
                        item -> {
                            return new XYChart.Data<String, Number>(item.getKey(), item.getValue());
                        }).collect(
                        Collectors.toList()
                );
        series.setData(FXCollections.observableArrayList(dataList));
        Scene scene = new Scene(lineChart, 400, 400);
        lineChart.getData().add(series);

        stage.setScene(scene);
        stage.showAndWait();
    }
}
