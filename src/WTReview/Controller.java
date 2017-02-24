/*
 * Copyright (c) 2017 S. Griffin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
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

    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    private final FileChooser fileChooser = new FileChooser();
    private final ObservableList<MeasurementFile> measuredData = FXCollections.observableArrayList();
    private final ObservableList<MeasurementFile> referenceData = FXCollections.observableArrayList();
    @FXML
    private ListView<MeasurementFile> ui_mList;
    @FXML
    private ListView<MeasurementFile> ui_rList;
    @FXML
    private LineChart<Number, Number> ui_ProfileGraph;
    @FXML
    private LineChart<Number, Number> ui_AnalysisGraph;
    @FXML
    private VBox ui_chartPane;
    @FXML
    private Button ui_resetBtn;
    @FXML
    private TextArea ui_ResultsTable;
    @FXML
    private RadioButton ui_SyncLists;
    private LineChart.Series<Number, Number> measuredSeries;
    private LineChart.Series<Number, Number> referenceSeries;
    private LineChart.Series<Number, Number> gammaSeries;
    private LineChart.Series<Number, Number> ratioSeries;
    private MeasurementFile currentReferenceProfile;
    private MeasurementFile currentMeasuredProfile;

    @FXML
    private void initialize() {
        // Setup Drag & Drop
        setupDragDrop(ui_mList);
        setupDragDrop(ui_rList);

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
            if (ui_SyncLists.isSelected() && newIndex.intValue() < measuredData.size()) {
                ui_mList.getSelectionModel().clearAndSelect(newIndex.intValue());
            }
        });
        ui_mList.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) ->
        {
            if (ui_SyncLists.isSelected() && newIndex.intValue() < referenceData.size()) {
                ui_rList.getSelectionModel().clearAndSelect(newIndex.intValue());
            }
        });

        // Setup selection changed events for both data lists.
        ui_mList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);

            ui_ProfileGraph.getData().remove(measuredSeries);
            currentMeasuredProfile = newValue;

            if (newValue != null) {
                measuredSeries = new XYChart.Series<>();
                measuredSeries.setName("Measured");
                ui_ProfileGraph.getData().add(measuredSeries);

                Profile p = newValue.getProfile();
                for (int i = 0; i < p.getX().size(); i++) {
                    measuredSeries.getData().add(new XYChart.Data<>(p.getX().get(i), p.getY().get(i)));
                }
            }

            calcAnalysis();
        });

        ui_rList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);

            ui_ProfileGraph.getData().remove(referenceSeries);
            currentReferenceProfile = newValue;

            if (newValue != null) {
                referenceSeries = new XYChart.Series<>();
                referenceSeries.setName("Reference");
                ui_ProfileGraph.getData().add(referenceSeries);

                Profile p = newValue.getProfile();
                for (int i = 0; i < p.getX().size(); i++) {
                    referenceSeries.getData().add(new XYChart.Data<>(p.getX().get(i), p.getY().get(i)));
                }
            }

            calcAnalysis();
        });

        // Setup zooming for the main graph.
        final Rectangle zoomRect = new Rectangle();
        zoomRect.setManaged(false);
        zoomRect.setFill(Color.LIGHTSEAGREEN.deriveColor(0, 1, 1, 0.5));
        ui_chartPane.getChildren().add(zoomRect);
        setUpZooming(zoomRect, ui_ProfileGraph);
        ui_resetBtn.setOnAction(event -> doReset(ui_ProfileGraph));

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
    private void handleReferenceMoveUp() {
        int selectedIndex = ui_rList.getSelectionModel().getSelectedIndex();
        if (selectedIndex > 0) {
            Collections.swap(referenceData, selectedIndex, selectedIndex - 1);
            ui_rList.getSelectionModel().clearAndSelect(selectedIndex - 1);
        }
    }

    @FXML
    private void handleReferenceMoveDown() {
        int selectedIndex = ui_rList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < referenceData.size() - 1) {
            Collections.swap(referenceData, selectedIndex, selectedIndex + 1);
            ui_rList.getSelectionModel().clearAndSelect(selectedIndex + 1);
        }
    }

    @FXML
    private void handleMeasuredMoveUp() {
        int selectedIndex = ui_mList.getSelectionModel().getSelectedIndex();
        if (selectedIndex > 0) {
            Collections.swap(measuredData, selectedIndex, selectedIndex - 1);
            ui_mList.getSelectionModel().clearAndSelect(selectedIndex - 1);
        }
    }

    @FXML
    private void handleMeasuredMoveDown() {
        int selectedIndex = ui_mList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < measuredData.size() - 1) {
            Collections.swap(measuredData, selectedIndex, selectedIndex + 1);
            ui_mList.getSelectionModel().clearAndSelect(selectedIndex + 1);
        }
    }

    @FXML
    private void handleReferenceDelete() {
        int selectedIndex = ui_rList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            referenceData.remove(selectedIndex);
        }
    }

    @FXML
    private void handleMeasuredDelete() {
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

            ratioSeries = new XYChart.Series<>();
            ratioSeries.setName("Ratio");
            ui_AnalysisGraph.getData().add(ratioSeries);

            for (int i = 0; i < ratio.getX().size(); i++) {
                ratioSeries.getData().add(new XYChart.Data<>(ratio.getX().get(i), ratio.getY().get(i)));
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

            gammaSeries = new XYChart.Series<>();
            gammaSeries.setName("Gamma");
            ui_ProfileGraph.getData().add(gammaSeries);

            for (int i = 0; i < gamma.getX().size(); i++) {
                gammaSeries.getData().add(new XYChart.Data<>(gamma.getX().get(i), gamma.getY().get(i)));
            }

            ResultsFile results = new ResultsFile(currentReferenceProfile, currentMeasuredProfile, gamma);
            String resultsTxt = results.getResults();
            int numberOfLines = resultsTxt.split("\n").length;

            // TODO calculate minimum height of text from resultsFile instead of guessing.
            Text resultsText = (Text) ui_ResultsTable.lookup(".text");
            ui_ResultsTable.setMinHeight((numberOfLines + 1) * resultsText.getBoundsInLocal().getHeight());
            ui_ResultsTable.setText(resultsTxt);
            ui_ResultsTable.setVisible(true);
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
            double x = ui_chartPane.sceneToLocal(pointInScene).getX();
            double y = ui_chartPane.sceneToLocal(pointInScene).getY();

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
            double x = ui_chartPane.sceneToLocal(pointInScene).getX();
            double y = ui_chartPane.sceneToLocal(pointInScene).getY();

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

    private <T> void setupDragDrop(ListView<T> listView) {
        listView.setCellFactory(tv -> {
            ListCell<T> cell = new TextFieldListCell<>();

            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Integer index = cell.getIndex();
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(cell.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            cell.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    if (cell.getIndex() != (Integer) db.getContent(SERIALIZED_MIME_TYPE)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        event.consume();
                    }
                }
            });

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                    T draggedItem = listView.getItems().remove(draggedIndex);

                    int dropIndex;

                    if (cell.isEmpty()) {
                        dropIndex = ui_rList.getItems().size();
                    } else {
                        dropIndex = cell.getIndex();
                    }

                    listView.getItems().add(dropIndex, draggedItem);

                    event.setDropCompleted(true);
                    listView.getSelectionModel().select(dropIndex);
                    event.consume();
                }
            });

            return cell;
        });
    }
}

