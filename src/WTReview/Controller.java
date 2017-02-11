package WTReview;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
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
    private LineChart<Number, Number> ui_ProfileGraph;
    @FXML
    private LineChart<Number, Number> ui_AnalysisGraph;
    @FXML
    private AnchorPane chartPane;
    @FXML
    private Button resetBtn;
    @FXML
    private TextField ui_coordinates;
    @FXML
    private SplitPane ui_GraphDivider;

    private XYChart.Series measuredSeries;
    private XYChart.Series referenceSeries;
    private XYChart.Series gammaSeries;
    private XYChart.Series ratioSeries;
    private ObservableList<MeasurementFile> measuredData = FXCollections.observableArrayList();
    private ObservableList<MeasurementFile> referenceData = FXCollections.observableArrayList();
    private MeasurementFile currentReferenceProfile;
    private MeasurementFile currentMeasuredProfile;

    @FXML
    private void initialize() {
        ui_mList.setItems(measuredData);
        ui_rList.setItems(referenceData);

        ui_ProfileGraph.setCreateSymbols(false);
        ui_ProfileGraph.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);

        ui_AnalysisGraph.setCreateSymbols(false);
        ui_AnalysisGraph.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);

        ui_mList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);

            if (newValue != null) {
                ui_ProfileGraph.getData().remove(measuredSeries);
                measuredSeries = new XYChart.Series();
                measuredSeries.setName("Measured");
                ui_ProfileGraph.getData().addAll(measuredSeries);

                Profile p = newValue.getProfile();
                currentMeasuredProfile = newValue;
                for (int i = 0; i < p.getxValues().size(); i++) {
                    measuredSeries.getData().add(new XYChart.Data(p.getxValues().get(i), p.getyValues().get(i)));
                }

                calcAnalysis();
            }
        });

        ui_rList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);

            if (newValue != null) {
                ui_ProfileGraph.getData().remove(referenceSeries);
                referenceSeries = new XYChart.Series();
                referenceSeries.setName("Reference");
                ui_ProfileGraph.getData().addAll(referenceSeries);

                Profile p = newValue.getProfile();
                currentReferenceProfile = newValue;
                for (int i = 0; i < p.getxValues().size(); i++) {
                    referenceSeries.getData().add(new XYChart.Data(p.getxValues().get(i), p.getyValues().get(i)));
                }

                calcAnalysis();
            }
        });

        final Rectangle zoomRect = new Rectangle();
        zoomRect.setManaged(false);
        zoomRect.setFill(Color.LIGHTSEAGREEN.deriveColor(0, 1, 1, 0.5));
        chartPane.getChildren().add(zoomRect);

        setUpZooming(zoomRect, ui_ProfileGraph);
        resetBtn.setOnAction(event -> doReset(ui_ProfileGraph));

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

    private void calcAnalysis() {
        ui_ProfileGraph.getData().remove(gammaSeries);
        ui_AnalysisGraph.getData().remove(ratioSeries);

        if (currentReferenceProfile == null || currentMeasuredProfile == null) {
            return;
        }

        // Do analysis for 2 PDDs.
        if (currentReferenceProfile.getOrientation() == ProfileOrientation.DepthDose && currentMeasuredProfile.getOrientation() == ProfileOrientation.DepthDose) {
            ui_AnalysisGraph.setVisible(true);
            ui_GraphDivider.setDividerPosition(0, 0.5);

            Profile ratio = ProfileUtilities.calcRatio(currentReferenceProfile.getProfile(), currentMeasuredProfile.getProfile());

            if (ratio == null) {
                return;
            }

            ratioSeries = new XYChart.Series();
            ratioSeries.setName("Ratio");
            ui_AnalysisGraph.getData().addAll(ratioSeries);

            for (int i = 0; i < ratio.getxValues().size(); i++) {
                ratioSeries.getData().add(new XYChart.Data(ratio.getxValues().get(i), ratio.getyValues().get(i)));
            }

            return;
        }

        // Do analysis for 2 matching Horizontal Profiles.
        if (currentReferenceProfile.getOrientation() == currentMeasuredProfile.getOrientation()) {
            ui_AnalysisGraph.setVisible(false);
            ui_GraphDivider.setDividerPosition(0, 1.0);

            double distanceToAgreement = currentReferenceProfile.getOrientation() == ProfileOrientation.Lateral ? 1.0 : 0.1;
            Profile gamma = ProfileUtilities.calcGamma(currentReferenceProfile.getProfile(), currentMeasuredProfile.getProfile(), distanceToAgreement, 0.02);

            if (gamma == null) {
                return;
            }

            gammaSeries = new XYChart.Series();
            gammaSeries.setName("Gamma");
            ui_ProfileGraph.getData().addAll(gammaSeries);

            for (int i = 0; i < gamma.getxValues().size(); i++) {
                gammaSeries.getData().add(new XYChart.Data(gamma.getxValues().get(i), gamma.getyValues().get(i)));
            }
            return;
        }

        ui_AnalysisGraph.setVisible(false);
        ui_GraphDivider.setDividerPosition(0, 1.0);
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
            Point2D pointInScene = new Point2D(event.getSceneX(), event.getSceneY());
            double x = chartPane.sceneToLocal(pointInScene).getX();
            double y = chartPane.sceneToLocal(pointInScene).getY();

            mouseAnchor.set(new Point2D(x, y));
            rect.setWidth(0);
            rect.setHeight(0);
        });

        zoomingNode.setOnMouseDragged(event -> {
            Point2D pointInScene = new Point2D(event.getSceneX(), event.getSceneY());
            double x = chartPane.sceneToLocal(pointInScene).getX();
            double y = chartPane.sceneToLocal(pointInScene).getY();

            rect.setX(Math.min(x, mouseAnchor.get().getX()));
            rect.setY(Math.min(y, mouseAnchor.get().getY()));

            rect.setWidth(Math.abs(x - mouseAnchor.get().getX()));
            rect.setHeight(Math.abs(y - mouseAnchor.get().getY()));
        });

        zoomingNode.setOnMouseMoved(event -> {
            Point2D pointInScene = new Point2D(event.getSceneX(), event.getSceneY());

            NumberAxis xAxis = (NumberAxis) ui_ProfileGraph.getXAxis();
            NumberAxis yAxis = (NumberAxis) ui_ProfileGraph.getYAxis();

            double pointOnXAxis = xAxis.sceneToLocal(pointInScene).getX();
            double pointOnYAxis = yAxis.sceneToLocal(pointInScene).getY();

            double xPosition = xAxis.getValueForDisplay(pointOnXAxis).doubleValue();
            double yPosition = yAxis.getValueForDisplay(pointOnYAxis).doubleValue();
            ui_coordinates.setText(String.format("x:%.2f, y:%.2f", xPosition, yPosition));
        });

        zoomingNode.setOnMouseReleased(event -> doZoom(rect, ui_ProfileGraph));
    }

    private void doZoom(Rectangle zoomRect, LineChart<Number, Number> chart) {
        final NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setAutoRanging(false);

        final NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        xAxis.setAutoRanging(false);

        double yAxisScale = yAxis.getTickUnit();
        double xAxisScale = xAxis.getTickUnit();

        Bounds zoomBoundsInScene = zoomRect.localToScene(zoomRect.getBoundsInLocal());
        Bounds zoomBoundsInXAxis = xAxis.sceneToLocal(zoomBoundsInScene);
        Bounds zoomBoundsInYAxis = yAxis.sceneToLocal(zoomBoundsInScene);

        double newYMax = yAxis.getValueForDisplay(zoomBoundsInYAxis.getMinY()).doubleValue();
        double newYMin = yAxis.getValueForDisplay(zoomBoundsInYAxis.getMaxY()).doubleValue();
        double newXMax = xAxis.getValueForDisplay(zoomBoundsInXAxis.getMaxX()).doubleValue();
        double newXMin = xAxis.getValueForDisplay(zoomBoundsInXAxis.getMinX()).doubleValue();

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

