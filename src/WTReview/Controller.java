package WTReview;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

public class Controller {

    private final FileChooser fileChooser = new FileChooser();
    @FXML
    private ListView<MeasurementFile> ui_mList;
    @FXML
    private ListView<MeasurementFile> ui_rList;
    @FXML
    private LineChart<Number, Number> ui_Graph;
    @FXML
    private AnchorPane chartPane;
    @FXML
    private Button zoomBtn;
    @FXML
    private Button resetBtn;
    private XYChart.Series measuredSeries;
    private XYChart.Series referenceSeries;
    private XYChart.Series gammaSeries;
    private ObservableList<MeasurementFile> measuredData = FXCollections.observableArrayList();
    private ObservableList<MeasurementFile> referenceData = FXCollections.observableArrayList();
    private MeasurementFile currentReferenceProfile;
    private MeasurementFile currentMeasuredProfile;

    @FXML
    private void initialize() {
        ui_mList.setItems(measuredData);
        ui_rList.setItems(referenceData);

        ui_Graph.setCreateSymbols(false);
        ui_Graph.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);

        ui_mList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);

            if (newValue != null) {
                ui_Graph.getData().remove(measuredSeries);
                measuredSeries = new XYChart.Series();
                measuredSeries.setName("Measured");
                ui_Graph.getData().addAll(measuredSeries);

                Profile p = newValue.getProfile();
                currentMeasuredProfile = newValue;
                for (int i = 0; i < p.getxValues().size(); i++) {
                    measuredSeries.getData().add(new XYChart.Data(p.getxValues().get(i), p.getyValues().get(i)));
                }

                calcGamma();
            }
        });

        ui_rList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);

            if (newValue != null) {
                ui_Graph.getData().remove(referenceSeries);
                referenceSeries = new XYChart.Series();
                referenceSeries.setName("Reference");
                ui_Graph.getData().addAll(referenceSeries);

                Profile p = newValue.getProfile();
                currentReferenceProfile = newValue;
                for (int i = 0; i < p.getxValues().size(); i++) {
                    referenceSeries.getData().add(new XYChart.Data(p.getxValues().get(i), p.getyValues().get(i)));
                }

                calcGamma();
            }
        });

        final Rectangle zoomRect = new Rectangle();
        zoomRect.setManaged(false);
        zoomRect.setFill(Color.LIGHTSEAGREEN.deriveColor(0, 1, 1, 0.5));
        chartPane.getChildren().add(zoomRect);

        setUpZooming(zoomRect, ui_Graph);

        zoomBtn.setOnAction(event -> doZoom(zoomRect, ui_Graph));
        resetBtn.setOnAction(event -> doReset(ui_Graph));

        // TODO remove loading of test data.
        LoadTestData();
    }

    private void doReset(LineChart<Number, Number> chart) {
        final NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setAutoRanging(true);
        final NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        xAxis.setAutoRanging(true);
    }

    @FXML
    private void handleAddMeasuredButtonAction(ActionEvent event) {
        Node source = (Node) event.getSource();
        Window theStage = source.getScene().getWindow();

        List<File> list = fileChooser.showOpenMultipleDialog(theStage);
        if (list != null) {
            for (File file : list) {
                measuredData.add(TemsReader.ReadInProfiles(file.getPath()));
            }
        }
    }

    @FXML
    private void handleAddReferenceButtonAction(ActionEvent event) {
        Node source = (Node) event.getSource();
        Window theStage = source.getScene().getWindow();

        List<File> list = fileChooser.showOpenMultipleDialog(theStage);
        if (list != null) {
            for (File file : list) {
                referenceData.add(TemsReader.ReadInProfiles(file.getPath()));
            }
        }
    }

    private void calcGamma() {
        ui_Graph.getData().remove(gammaSeries);

        if (currentReferenceProfile == null) {
            return;
        }

        if (currentMeasuredProfile == null) {
            return;
        }

        if (currentReferenceProfile.getOrientation() == ProfileOrientation.DepthDose || currentMeasuredProfile.getOrientation() == ProfileOrientation.DepthDose) {
            return;
        }

        if (currentReferenceProfile.getOrientation() != currentMeasuredProfile.getOrientation()) {
            return;
        }

        double distanceToAgreement = currentReferenceProfile.getOrientation() == ProfileOrientation.Lateral ? 1.0 : 0.1;

        Profile gamma = GammaFunction.calcGamma(currentReferenceProfile.getProfile(), currentMeasuredProfile.getProfile(), distanceToAgreement, 0.02);

        if (gamma == null) {
            return;
        }

        gammaSeries = new XYChart.Series();
        gammaSeries.setName("Gamma");
        ui_Graph.getData().addAll(gammaSeries);

        for (int i = 0; i < gamma.getxValues().size(); i++) {
            gammaSeries.getData().add(new XYChart.Data(gamma.getxValues().get(i), gamma.getyValues().get(i)));
        }
    }

    private void LoadTestData() {
        String rLong = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/rlong.csv";
        String rLat = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/rLat.csv";
        String rPDD = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/rPDD.csv";

        referenceData.add(TemsReader.ReadInProfiles(rLong));
        referenceData.add(TemsReader.ReadInProfiles(rLat));
        referenceData.add(TemsReader.ReadInProfiles(rPDD));

        String mLong = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/mlong.csv";
        String mLat = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/mLat.csv";
        String mPDD = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/mPDD.csv";

        measuredData.add(TemsReader.ReadInProfiles(mLong));
        measuredData.add(TemsReader.ReadInProfiles(mLat));
        measuredData.add(TemsReader.ReadInProfiles(mPDD));
    }

    private void setUpZooming(final Rectangle rect, final Node zoomingNode) {
        final ObjectProperty<Point2D> mouseAnchor = new SimpleObjectProperty<>();

        zoomingNode.setOnMousePressed(event -> {
            mouseAnchor.set(new Point2D(event.getX(), event.getY()));
            rect.setWidth(0);
            rect.setHeight(0);
        });

        zoomingNode.setOnMouseDragged(event -> {
            double x = event.getX();
            double y = event.getY();

            rect.setX(Math.min(x, mouseAnchor.get().getX()));
            rect.setY(Math.min(y, mouseAnchor.get().getY()));

            rect.setWidth(Math.abs(x - mouseAnchor.get().getX()));
            rect.setHeight(Math.abs(y - mouseAnchor.get().getY()));
        });
    }

    private void doZoom(Rectangle zoomRect, LineChart<Number, Number> chart) {
        final NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setAutoRanging(false);

        final NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        xAxis.setAutoRanging(false);

        double yAxisScale = yAxis.getTickUnit();
        double xAxisScale = xAxis.getTickUnit();

        double newYMax = yAxis.getValueForDisplay(zoomRect.getY()).doubleValue();
        double newYMin = yAxis.getValueForDisplay(zoomRect.getY() + zoomRect.getHeight()).doubleValue();
        double newXMax = xAxis.getValueForDisplay(zoomRect.getX() + zoomRect.getWidth()).doubleValue();
        double newXMin = xAxis.getValueForDisplay(zoomRect.getX()).doubleValue();

        newYMax = yAxisScale * Math.round(newYMax / yAxisScale);
        newYMin = yAxisScale * Math.round(newYMin / yAxisScale);
        newXMax = xAxisScale * Math.round(newXMax / xAxisScale);
        newXMin = xAxisScale * Math.round(newXMin / xAxisScale);

        yAxis.setLowerBound(newYMin);
        yAxis.setUpperBound(newYMax);
        xAxis.setLowerBound(newXMin);
        xAxis.setUpperBound(newXMax);

        zoomRect.setWidth(0);
        zoomRect.setHeight(0);
    }
}

