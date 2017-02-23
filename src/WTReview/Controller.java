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
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Collections;
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
    private VBox chartPane;
    @FXML
    private Button resetBtn;
    @FXML
    private TextField ui_coordinates;
    @FXML
    private TextArea ui_ResultsTable;
    @FXML
    private RadioButton ui_SyncLists;

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
        // Setup data tables.
        ui_mList.setItems(measuredData);
        ui_rList.setItems(referenceData);

        // Setup Main Graph.
        ui_ProfileGraph.setCreateSymbols(false);
        ui_ProfileGraph.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);

        // Setup Analysis Graph.
        ui_AnalysisGraph.setCreateSymbols(false);
        ui_AnalysisGraph.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        ui_AnalysisGraph.managedProperty().bind(ui_AnalysisGraph.visibleProperty());
        ui_AnalysisGraph.setVisible(false);

        // Setup Results Table.
        ui_ResultsTable.managedProperty().bind(ui_ResultsTable.visibleProperty());
        ui_ResultsTable.setVisible(false);

        // Sync selection for the 2 data list views.
        ui_rList.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) ->
        {
            if (ui_SyncLists.isSelected()) {
                ui_mList.getSelectionModel().clearAndSelect(newIndex.intValue());
            }
        });
        ui_mList.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) ->
        {
            if (ui_SyncLists.isSelected()) {
                ui_rList.getSelectionModel().clearAndSelect(newIndex.intValue());
            }
        });

        // Setup selection changed events for both data lists.
        ui_mList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);

            ui_ProfileGraph.getData().remove(measuredSeries);
            currentMeasuredProfile = newValue;

            if (newValue != null) {
                measuredSeries = new XYChart.Series();
                measuredSeries.setName("Measured");
                ui_ProfileGraph.getData().addAll(measuredSeries);

                Profile p = newValue.getProfile();
                for (int i = 0; i < p.getxValues().size(); i++) {
                    measuredSeries.getData().add(new XYChart.Data(p.getxValues().get(i), p.getyValues().get(i)));
                }
            }

            calcAnalysis();
        });

        ui_rList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);

            ui_ProfileGraph.getData().remove(referenceSeries);
            currentReferenceProfile = newValue;

            if (newValue != null) {
                referenceSeries = new XYChart.Series();
                referenceSeries.setName("Reference");
                ui_ProfileGraph.getData().addAll(referenceSeries);

                Profile p = newValue.getProfile();
                for (int i = 0; i < p.getxValues().size(); i++) {
                    referenceSeries.getData().add(new XYChart.Data(p.getxValues().get(i), p.getyValues().get(i)));
                }
            }

            calcAnalysis();
        });

        // Setup zooming for the main graph.
        final Rectangle zoomRect = new Rectangle();
        zoomRect.setManaged(false);
        zoomRect.setFill(Color.LIGHTSEAGREEN.deriveColor(0, 1, 1, 0.5));
        chartPane.getChildren().add(zoomRect);
        setUpZooming(zoomRect, ui_ProfileGraph);
        resetBtn.setOnAction(event -> doReset(ui_ProfileGraph));

        // TODO remove loading of test data.
        LoadTestData();
    }

    @FXML
    private void handleAddMeasuredButtonAction(ActionEvent event) {
        Node source = (Node) event.getSource();
        Window theStage = source.getScene().getWindow();

        List<File> list = fileChooser.showOpenMultipleDialog(theStage);
        if (list != null) {
            int filesAdded = 0;
            for (File file : list) {
                MeasurementFile fileToAdd = TemsReader.ReadInProfiles(file.getPath());
                if (fileToAdd != null) {
                    measuredData.add(fileToAdd);
                    filesAdded++;
                }
            }

            if (list.size() > filesAdded) {
                String errorString = String.format("%s files added from %s files selected", filesAdded, list.size());
                System.out.println(errorString);
            }
        }
    }

    @FXML
    private void handleAddReferenceButtonAction(ActionEvent event) {
        Node source = (Node) event.getSource();
        Window theStage = source.getScene().getWindow();

        List<File> list = fileChooser.showOpenMultipleDialog(theStage);
        if (list != null) {
            int filesAdded = 0;
            for (File file : list) {
                MeasurementFile fileToAdd = TemsReader.ReadInProfiles(file.getPath());
                if (fileToAdd != null) {
                    referenceData.add(fileToAdd);
                    filesAdded++;
                }
            }

            if (list.size() > filesAdded) {
                String errorString = String.format("%s files added from %s files selected", filesAdded, list.size());
                System.out.println(errorString);
            }
        }
    }

    @FXML
    private void handleReferenceMoveUp(ActionEvent event) {
        int selectedIndex = ui_rList.getSelectionModel().getSelectedIndex();
        if (selectedIndex > 0) {
            Collections.swap(referenceData, selectedIndex, selectedIndex - 1);
            ui_rList.getSelectionModel().clearAndSelect(selectedIndex - 1);
        }
    }

    @FXML
    private void handleReferenceMoveDown(ActionEvent event) {
        int selectedIndex = ui_rList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < referenceData.size() - 1) {
            Collections.swap(referenceData, selectedIndex, selectedIndex + 1);
            ui_rList.getSelectionModel().clearAndSelect(selectedIndex + 1);
        }
    }

    @FXML
    private void handleMeasuredMoveUp(ActionEvent event) {
        int selectedIndex = ui_mList.getSelectionModel().getSelectedIndex();
        if (selectedIndex > 0) {
            Collections.swap(measuredData, selectedIndex, selectedIndex - 1);
            ui_mList.getSelectionModel().clearAndSelect(selectedIndex - 1);
        }
    }

    @FXML
    private void handleMeasuredMoveDown(ActionEvent event) {
        int selectedIndex = ui_mList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < measuredData.size() - 1) {
            Collections.swap(measuredData, selectedIndex, selectedIndex + 1);
            ui_mList.getSelectionModel().clearAndSelect(selectedIndex + 1);
        }
    }

    @FXML
    private void handleReferenceDelete(ActionEvent event) {
        int selectedIndex = ui_rList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            referenceData.remove(selectedIndex);
        }
    }

    @FXML
    private void handleMeasuredDelete(ActionEvent event) {
        int selectedIndex = ui_mList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            measuredData.remove(selectedIndex);
        }
    }

    private void calcAnalysis() {
        ui_ProfileGraph.getData().remove(gammaSeries);
        ui_AnalysisGraph.getData().remove(ratioSeries);
        ui_ResultsTable.clear();

        // Check if profiles exist.
        if (currentReferenceProfile == null || currentMeasuredProfile == null) {
            ui_ResultsTable.setVisible(false);
            ui_AnalysisGraph.setVisible(false);
            return;
        }

        // Check if profile orientations match
        if (currentReferenceProfile.getOrientation() != currentMeasuredProfile.getOrientation()) {
            ui_ResultsTable.setVisible(false);
            ui_AnalysisGraph.setVisible(false);
            return;
        }

        // Do analysis for 2 PDDs.
        if (currentReferenceProfile.getOrientation() == ProfileOrientation.DepthDose && currentMeasuredProfile.getOrientation() == ProfileOrientation.DepthDose) {
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

            ui_AnalysisGraph.setVisible(true);
            return;
        }

        // Do analysis for 2 matching Horizontal Profiles.
        if (currentReferenceProfile.getOrientation() == currentMeasuredProfile.getOrientation()) {
            ui_AnalysisGraph.setVisible(false);

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

            ResultsFile results = new ResultsFile(currentReferenceProfile, currentMeasuredProfile, gamma);
            String resultsTxt = results.getResults();
            int numberOfLines = resultsTxt.split("\n").length;

            // TODO calculate minumum height of text from resultsFile instead of guessing.
            Text resultsText = (Text) ui_ResultsTable.lookup(".text");
            ui_ResultsTable.setMinHeight((numberOfLines + 1) * resultsText.getBoundsInLocal().getHeight());
            ui_ResultsTable.setText(resultsTxt);
            ui_ResultsTable.setVisible(true);
            return;
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
            Point2D pointInScene = new Point2D(event.getSceneX(), event.getSceneY());
            double x = chartPane.sceneToLocal(pointInScene).getX();
            double y = chartPane.sceneToLocal(pointInScene).getY();

            Point2D pointInChartPane = new Point2D(x, y);
            Bounds graphBoundsInChartPane = ui_ProfileGraph.getBoundsInParent();
            if (!graphBoundsInChartPane.contains(pointInChartPane)) {
                return;
            }

            mouseAnchor.set(new Point2D(x, y));
            rect.setWidth(0);
            rect.setHeight(0);
        });

        zoomingNode.setOnMouseDragged(event -> {
            Point2D pointInScene = new Point2D(event.getSceneX(), event.getSceneY());
            double x = chartPane.sceneToLocal(pointInScene).getX();
            double y = chartPane.sceneToLocal(pointInScene).getY();

            Point2D pointInChartPane = new Point2D(x, y);
            Bounds graphBoundsInChartPane = ui_ProfileGraph.getBoundsInParent();
            if (!graphBoundsInChartPane.contains(pointInChartPane)) {
                return;
            }

            double mouseX = mouseAnchor.get().getX();
            double mouseY = mouseAnchor.get().getY();

            double startX = Math.min(x, mouseX);
            double startY = Math.min(y, mouseY);

            rect.setX(startX);
            rect.setY(startY);

            double width = Math.abs(x - mouseAnchor.get().getX());
            double height = Math.abs(y - mouseAnchor.get().getY());

            rect.setWidth(width);
            rect.setHeight(height);
        });

//        zoomingNode.setOnMouseMoved(event -> {
//            Point2D pointInScene = new Point2D(event.getSceneX(), event.getSceneY());
//            double mouseX = ui_ProfileGraph.sceneToLocal(pointInScene).getX();
//            double mouseY = ui_ProfileGraph.sceneToLocal(pointInScene).getY();
//
//            double boundsX = ui_ProfileGraph.getBoundsInLocal().getMaxX();
//            double boundsY = ui_ProfileGraph.getBoundsInLocal().getMaxY();
//
//            double maxX = Math.max(boundsX, mouseX);
//            double maxY = Math.max(boundsY, mouseY);
//
//            ui_coordinates.setText(String.format("mx:%.2f, my:%.2f, bx:%.2f, by:%.2f", mouseX, mouseY, boundsX, boundsY));
//        });

//        zoomingNode.setOnMouseMoved(event -> {
//            Point2D pointInScene = new Point2D(event.getSceneX(), event.getSceneY());
//
//            NumberAxis xAxis = (NumberAxis) ui_ProfileGraph.getXAxis();
//            NumberAxis yAxis = (NumberAxis) ui_ProfileGraph.getYAxis();
//
//            double pointOnXAxis = xAxis.sceneToLocal(pointInScene).getX();
//            double pointOnYAxis = yAxis.sceneToLocal(pointInScene).getY();
//
//            double xPosition = xAxis.getValueForDisplay(pointOnXAxis).doubleValue();
//            double yPosition = yAxis.getValueForDisplay(pointOnYAxis).doubleValue();
//            ui_coordinates.setText(String.format("x:%.2f, y:%.2f", xPosition, yPosition));
//        });

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

    private void doReset(LineChart<Number, Number> chart) {
        final NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setAutoRanging(true);
        final NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        xAxis.setAutoRanging(true);
    }
}

