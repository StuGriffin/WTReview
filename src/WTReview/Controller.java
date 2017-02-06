package WTReview;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ListView;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Controller {

    @FXML
    private ListView<MeasurementFile> ui_mList;
    @FXML
    private ListView<MeasurementFile> ui_rList;
    @FXML
    private LineChart<Double, Double> ui_Graph;
    private XYChart.Series measuredSeries;
    private XYChart.Series referenceSeries;
    private ObservableList<MeasurementFile> measuredData = FXCollections.observableArrayList();
    private ObservableList<MeasurementFile> referenceData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        ui_mList.setItems(measuredData);
        ui_rList.setItems(referenceData);

        ui_Graph.setCreateSymbols(false);
        ui_Graph.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);

        ui_mList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<MeasurementFile>() {
            @Override
            public void changed(ObservableValue<? extends MeasurementFile> observable, MeasurementFile oldValue, MeasurementFile newValue) {
                System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);

                if (newValue != null) {
                    ui_Graph.getData().remove(measuredSeries);
                    measuredSeries = new XYChart.Series();
                    ui_Graph.getData().addAll(measuredSeries);

                    Profile p = newValue.getProfile();
                    for (int i = 0; i < p.getxValues().size(); i++) {
                        measuredSeries.getData().add(new XYChart.Data(p.getxValues().get(i), p.getyValues().get(i)));
                    }
                }
            }
        });

        ui_rList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<MeasurementFile>() {
            @Override
            public void changed(ObservableValue<? extends MeasurementFile> observable, MeasurementFile oldValue, MeasurementFile newValue) {
                System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);

                if (newValue != null) {
                    ui_Graph.getData().remove(referenceSeries);
                    referenceSeries = new XYChart.Series();
                    ui_Graph.getData().addAll(referenceSeries);

                    Profile p = newValue.getProfile();
                    for (int i = 0; i < p.getxValues().size(); i++) {
                        referenceSeries.getData().add(new XYChart.Data(p.getxValues().get(i), p.getyValues().get(i)));
                    }
                }
            }
        });
    }

    @FXML
    private void handleMAddButtonAction(ActionEvent event) {

        String mLong = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/mlong.csv";
        String mLat = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/mLat.csv";
        String mPDD = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/mPDD.csv";

        measuredData.add(ReadInProfiles(mLong));
        measuredData.add(ReadInProfiles(mLat));
        measuredData.add(ReadInProfiles(mPDD));
    }

    @FXML
    private void handleRAddButtonAction(ActionEvent event) {

        String rLong = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/rlong.csv";
        String rLat = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/rLat.csv";
        String rPDD = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/rPDD.csv";

        referenceData.add(ReadInProfiles(rLong));
        referenceData.add(ReadInProfiles(rLat));
        referenceData.add(ReadInProfiles(rPDD));
    }

    private MeasurementFile ReadInProfiles(String fullFilePath) {

        Path path = Paths.get(fullFilePath);

        try (Scanner reader = new Scanner(path)) {

            Pattern headerPattern = Pattern.compile("\\*");
            Pattern dataPattern = Pattern.compile(",");
            int correctColumn = 2;

            // Skip first line which should contain data information.
            reader.nextLine();

            // Check version number.
            double version = Double.parseDouble(headerPattern.split(reader.nextLine())[correctColumn]);
            if (version != 1.1) {
                // TODO cleanup error handling.
                return null;
            }

            // Read horizontal orientation flag.
            Boolean isLateral = Double.parseDouble(headerPattern.split(reader.nextLine())[correctColumn]) == 0;

            // Read enabled channels.
            char[] rawChannels = headerPattern.split(reader.nextLine())[correctColumn].toCharArray();
            Boolean[] channels = new Boolean[8];
            for (int i = 0; i < 8; i++) {
                channels[i] = rawChannels[i] == '1';
            }

            // Read PDD Flag.
            Boolean isPDD = Double.parseDouble(headerPattern.split(reader.nextLine())[correctColumn]) == 1;

            // Skip 2 lines: blank and header.
            reader.nextLine();
            reader.nextLine();

            //Read Data
            ArrayList<MeasurementPoint> data = new ArrayList<>();
            while (reader.hasNextLine()) {
                String[] rawData = dataPattern.split(reader.nextLine());

                ArrayList<Double> numData = new ArrayList<>();
                for (String aRawData : rawData) {
                    try {
                        numData.add(Double.parseDouble(aRawData));
                    } catch (NumberFormatException exception) {
                        numData.add(Double.NaN);
                    }
                }

                MeasurementPoint point = new MeasurementPoint(
                        numData.get(0),
                        numData.get(1),
                        numData.get(2),
                        numData.get(3),
                        numData.get(4),
                        numData.get(5),
                        numData.get(6),
                        numData.get(7),
                        numData.get(8),
                        numData.get(9),
                        numData.get(10),
                        numData.get(11));

                data.add(point);
            }

            MeasurementFile measurement = new MeasurementFile(path.getFileName(), isLateral, channels, isPDD, data);

            String successString = String.format("Read profile successfully: %s", fullFilePath);
            System.out.println(successString);

            return measurement;
        } catch (Exception exception) {
            String errorString = String.format("Error during ReadInProfiles: %s", exception.toString());
            System.out.println(errorString);
        }

        // TODO cleanup error handling.
        return null;
    }
}

