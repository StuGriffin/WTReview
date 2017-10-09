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
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.*;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Controller {


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
    @FXML
    private ProgressIndicator ui_Progress;
    @FXML
    private Button ui_reportBtn;

    private MeasurementFile currentReferenceProfile;
    private MeasurementFile currentMeasuredProfile;

    @FXML
    private void initialize() {
        // Setup Drag & Drop
        ViewHelpers.setupDragDrop(ui_mList);
        ViewHelpers.setupDragDrop(ui_rList);

        // Setup data tables.
        ui_mList.setItems(measuredData);
        ui_rList.setItems(referenceData);

        // Setup Main Graph.
        ui_ProfileGraph.setCreateSymbols(false);
        ui_ProfileGraph.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        ui_ProfileGraph.setAnimated(false);

        // Setup Analysis Graph.
        ui_AnalysisGraph.setCreateSymbols(false);
        ui_AnalysisGraph.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        ui_AnalysisGraph.managedProperty().bind(ui_AnalysisGraph.visibleProperty());
        ui_AnalysisGraph.setVisible(false);
        ui_AnalysisGraph.setAnimated(false);

        // Setup Results Table.
        ui_ResultsTable.managedProperty().bind(ui_ResultsTable.visibleProperty());
        ui_ResultsTable.setVisible(false);

        // Setup Progress Bar.
        ui_Progress.managedProperty().bind(ui_Progress.visibleProperty());
        ui_Progress.setVisible(false);

        // Sync selection for the 2 data list views.
        ui_rList.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (ui_SyncLists.isSelected() && newIndex.intValue() < measuredData.size()) {
                ui_mList.getSelectionModel().clearAndSelect(newIndex.intValue());
            }
        });
        ui_mList.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (ui_SyncLists.isSelected() && newIndex.intValue() < referenceData.size()) {
                ui_rList.getSelectionModel().clearAndSelect(newIndex.intValue());
            }
        });

        // Setup selection changed events for both data lists.
        ui_mList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);
            currentMeasuredProfile = newValue;
            updateOnScreenGraphs();
        });
        ui_rList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ListView selection changed from oldValue = " + oldValue + " to newValue = " + newValue);
            currentReferenceProfile = newValue;
            updateOnScreenGraphs();
        });

        // Setup zooming for the main graph.
        final Rectangle zoomRect = new Rectangle();
        zoomRect.setManaged(false);
        zoomRect.setFill(Color.LIGHTSEAGREEN.deriveColor(0, 1, 1, 0.5));
        ui_chartPane.getChildren().add(zoomRect);
        setUpZooming(zoomRect, ui_ProfileGraph);
        ui_resetBtn.setOnAction(event -> doReset(ui_ProfileGraph));

        //loadTestData();
    }

    private void loadTestData() {
        File testDirectory = new File("TestData/");
        FileFilter filter = new WildcardFileFilter("*.csv");
        File[] files = testDirectory.listFiles(filter);

        if (files != null && files.length > 0) {
            loadData(Arrays.asList(files), referenceData);
            loadData(Arrays.asList(files), measuredData);
        }
    }

    @FXML
    private void handleAddMeasuredButtonAction(ActionEvent event) {
        Node source = (Node) event.getSource();
        final Window theStage = source.getScene().getWindow();
        final List<File> list = fileChooser.showOpenMultipleDialog(theStage);
        loadData(list, measuredData);
    }

    @FXML
    private void handleAddReferenceButtonAction(ActionEvent event) {
        Node source = (Node) event.getSource();
        final Window theStage = source.getScene().getWindow();
        final List<File> list = fileChooser.showOpenMultipleDialog(theStage);
        loadData(list, referenceData);
    }

    @FXML
    private void handleReportButtonAction(ActionEvent event) {
        generateReport();
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

    private void loadData(List<File> source, ObservableList<MeasurementFile> target) {
        if (source != null) {
            Task<ArrayList<MeasurementFile>> readDataTask = new Task<ArrayList<MeasurementFile>>() {
                @Override
                protected ArrayList<MeasurementFile> call() {
                    ArrayList<MeasurementFile> results = new ArrayList<>();
                    for (int i = 0; i < source.size(); i++) {
                        if (isCancelled()) {
                            break;
                        }

                        MeasurementFile fileToAdd = TemsReader.ReadInProfiles(source.get(i).getPath());
                        if (fileToAdd != null) {
                            results.add(fileToAdd);
                        }

                        updateProgress(i, source.size());
                    }
                    return results;
                }
            };

            ui_Progress.visibleProperty().bind(readDataTask.runningProperty());
            ui_Progress.progressProperty().bind(readDataTask.progressProperty());
            readDataTask.setOnSucceeded(stateEvent -> {
                ArrayList<MeasurementFile> result = readDataTask.getValue();
                target.addAll(result);
                FXCollections.sort(target);
            });

            Thread tread = new Thread(readDataTask);
            tread.start();
        }
    }

    private void updateOnScreenGraphs() {
        // Cleanup the existing analysis.
        ui_ProfileGraph.getData().clear();
        ui_AnalysisGraph.getData().clear();
        ui_ResultsTable.clear();

        // Check if profiles exist.
        if (currentReferenceProfile == null || currentMeasuredProfile == null) {
            ui_ResultsTable.setVisible(false);
            ui_AnalysisGraph.setVisible(false);
            return;
        }

        Profile analysisProfile = buildGraphs(currentReferenceProfile, currentMeasuredProfile, ui_ProfileGraph, ui_AnalysisGraph);

        if (analysisProfile == null) {
            ui_ResultsTable.setVisible(false);
            ui_AnalysisGraph.setVisible(false);
            return;
        }

        // Setup view for PDD analysis
        if (currentReferenceProfile.getOrientation() == ProfileOrientation.PDD && currentMeasuredProfile.getOrientation() == ProfileOrientation.PDD) {
            ui_AnalysisGraph.setVisible(true);
            return;
        }

        // Setup view for Profile Analysis and build text information.
        if (currentReferenceProfile.getOrientation() == currentMeasuredProfile.getOrientation()) {
            ui_AnalysisGraph.setVisible(false);

            ResultsFile results = new ResultsFile(currentReferenceProfile, currentMeasuredProfile, analysisProfile);
            String resultsTxt = results.getResults();
            int numberOfLines = resultsTxt.split("\n").length;

            // TODO calculate minimum height of text from resultsFile instead of guessing.
            Text resultsText = (Text) ui_ResultsTable.lookup(".text");
            ui_ResultsTable.setMinHeight((numberOfLines + 1) * resultsText.getBoundsInLocal().getHeight());
            ui_ResultsTable.setText(resultsTxt);
            ui_ResultsTable.setVisible(true);
        }
    }

    /**
     * Takes the two input MeasurementFiles and builds the input graphs depending on the type of profile.
     *
     * @param referenceProfile
     * @param measuredProfile
     * @param profileGraph
     * @param analysisGraph
     * @return returns the analysis profile built from the two input measurementFiles. If PDD the analysis is a ratio otherwise its a Gamma profile.
     */
    private Profile buildGraphs(MeasurementFile referenceProfile, MeasurementFile measuredProfile, LineChart<Number, Number> profileGraph, LineChart<Number, Number> analysisGraph) {

        // Check if profile orientations match and return with null if not.
        if (referenceProfile.getOrientation() != measuredProfile.getOrientation()) {
            return null;
        }

        // Build measured data series
        LineChart.Series<Number, Number> measuredSeries = new XYChart.Series<>();
        measuredSeries.setName("Measured");
        profileGraph.getData().add(measuredSeries);

        Profile m = measuredProfile.getProfile();
        for (int i = 0; i < m.getX().size(); i++) {
            measuredSeries.getData().add(new XYChart.Data<>(m.getX().get(i), m.getY().get(i)));
        }

        // Build reference data series
        LineChart.Series<Number, Number> referenceSeries = new XYChart.Series<>();
        referenceSeries.setName("Reference");
        profileGraph.getData().add(referenceSeries);

        Profile r = referenceProfile.getProfile();
        for (int i = 0; i < r.getX().size(); i++) {
            referenceSeries.getData().add(new XYChart.Data<>(r.getX().get(i), r.getY().get(i)));
        }

        LineChart.Series<Number, Number> analysisSeries = new XYChart.Series<>();
        Profile analysisProfile = null;

        // Do analysis for 2 PDDs.
        if (referenceProfile.getOrientation() == ProfileOrientation.PDD && measuredProfile.getOrientation() == ProfileOrientation.PDD) {
            analysisProfile = ProfileUtilities.calcRatio(r, m);

            analysisSeries.setName("Ratio");
            analysisGraph.getData().add(analysisSeries);
        } else if (referenceProfile.getOrientation() == measuredProfile.getOrientation()) {
            double distanceToAgreement = referenceProfile.getOrientation() == ProfileOrientation.Lat ? 1.0 : 0.1;
            analysisProfile = ProfileUtilities.calcGamma(r, m, distanceToAgreement, 0.02);

            analysisSeries.setName("Gamma");
            profileGraph.getData().add(analysisSeries);
        } else {
            // TODO better null handling.
            return null;
        }

        for (int i = 0; i < analysisProfile.getX().size(); i++) {
            analysisSeries.getData().add(new XYChart.Data<>(analysisProfile.getX().get(i), analysisProfile.getY().get(i)));
        }

        return analysisProfile;
    }

    private void generateReportWithProgressBar() {
        ProgressForm progressForm = new ProgressForm();


        PDDocument report = new PDDocument();
        String path = "/Users/griffo/Desktop/output.pdf";
        int sizeOfArray = Math.min(measuredData.size(), referenceData.size());

        // Create the main Profile graphic surface.
        LineChart<Number, Number> tProfiles = new LineChart<>(new NumberAxis(), new NumberAxis());
        tProfiles.getXAxis().setLabel("Position (mm)");
        tProfiles.getYAxis().setLabel("Relative Height / Gamma Value");
        tProfiles.setCreateSymbols(false);
        tProfiles.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        tProfiles.setAnimated(false);


        // Create the analysis graphic surface.
        LineChart<Number, Number> aProfiles = new LineChart<>(new NumberAxis(), new NumberAxis(0.95, 1.05, 0.01));
        aProfiles.getXAxis().setLabel("Depth (mm)");
        aProfiles.getYAxis().setLabel("Ratio (Measured / Reference)");
        aProfiles.setCreateSymbols(false);
        aProfiles.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        aProfiles.setAnimated(false);

        float paperWidth = PDRectangle.A4.getHeight();
        float paperHeight = PDRectangle.A4.getWidth();


        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < sizeOfArray; i++) {
                    tProfiles.getData().clear();
                    aProfiles.getData().clear();

                    MeasurementFile referenceProfile = referenceData.get(i);
                    MeasurementFile measuredProfile = measuredData.get(i);
                    Profile analysisProfile = buildGraphs(referenceProfile, measuredProfile, tProfiles, aProfiles);

                    if (analysisProfile == null) {
                        continue;
                    }

                    String chartTitle = referenceProfile.toString();
                    aProfiles.setTitle(chartTitle);
                    tProfiles.setTitle(chartTitle);

                    try {
                        Scene profileScene = new Scene(tProfiles, 1400, 1000);
                        WritableImage profileSnapshot = tProfiles.snapshot(null, null);

                        PDPage graphPage = new PDPage(new PDRectangle(paperWidth, paperHeight));
                        report.addPage(graphPage);
                        saveImageToPDFPage(profileSnapshot, report, graphPage);

                        if (referenceProfile.getOrientation() == ProfileOrientation.PDD) {
                            PDPage analysisPage = new PDPage(new PDRectangle(paperWidth, paperHeight));
                            report.addPage(analysisPage);

                            Scene analysisScene = new Scene(aProfiles, 1400, 1000);
                            WritableImage analysisSnapshot = aProfiles.snapshot(null, null);

                            saveImageToPDFPage(analysisSnapshot, report, analysisPage);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    updateProgress(i, sizeOfArray);
                }

                try {
                    report.save(path);
                    report.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        progressForm.activateProgressBar(task);
        task.setOnSucceeded(event -> {
            progressForm.getDialogStage().close();
        });

        progressForm.getDialogStage().show();

        Thread thread = new Thread(task);
        thread.start();
    }

    private void generateReport() {
        PDDocument report = new PDDocument();
        String path = "/Users/griffo/Desktop/output.pdf";
        int sizeOfArray = Math.min(measuredData.size(), referenceData.size());

        // Create the main Profile graphic surface.
        LineChart<Number, Number> tProfiles = new LineChart<>(new NumberAxis(), new NumberAxis());
        tProfiles.getXAxis().setLabel("Position (mm)");
        tProfiles.getYAxis().setLabel("Relative Height / Gamma Value");
        tProfiles.setCreateSymbols(false);
        tProfiles.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        tProfiles.setAnimated(false);
        Scene profileScene = new Scene(tProfiles, 1400, 1000);


        // Create the analysis graphic surface.
        LineChart<Number, Number> aProfiles = new LineChart<>(new NumberAxis(), new NumberAxis(0.95, 1.05, 0.01));
        aProfiles.getXAxis().setLabel("Depth (mm)");
        aProfiles.getYAxis().setLabel("Ratio (Measured / Reference)");
        aProfiles.setCreateSymbols(false);
        aProfiles.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        aProfiles.setAnimated(false);
        Scene analysisScene = new Scene(aProfiles, 1400, 1000);

        float paperWidth = PDRectangle.A4.getHeight();
        float paperHeight = PDRectangle.A4.getWidth();
        PDRectangle paperSize = new PDRectangle(paperWidth, paperHeight);

        for (int i = 0; i < sizeOfArray; i++) {
            tProfiles.getData().clear();
            aProfiles.getData().clear();

            MeasurementFile referenceProfile = referenceData.get(i);
            MeasurementFile measuredProfile = measuredData.get(i);
            Profile analysisProfile = buildGraphs(referenceProfile, measuredProfile, tProfiles, aProfiles);

            if (analysisProfile == null) {
                continue;
            }

            String chartTitle = referenceProfile.toString();
            aProfiles.setTitle(chartTitle);
            tProfiles.setTitle(chartTitle);

            try {
                WritableImage profileSnapshot = tProfiles.snapshot(null, null);
                PDPage graphPage = new PDPage(paperSize);
                report.addPage(graphPage);
                saveImageToPDFPage(profileSnapshot, report, graphPage);

                if (referenceProfile.getOrientation() == ProfileOrientation.PDD) {
                    PDPage analysisPage = new PDPage(paperSize);
                    report.addPage(analysisPage);

                    WritableImage analysisSnapshot = aProfiles.snapshot(null, null);
                    saveImageToPDFPage(analysisSnapshot, report, analysisPage);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            report.save(path);
            report.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveImageToPDFPage(WritableImage image, PDDocument document, PDPage page) throws IOException {
        // Create our PDF image object.
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", stream);
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, stream.toByteArray(), "image-1");

        // Write the image object to the PDF document.
        PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);

        int boarder = 20;
        double pageWidth = PDRectangle.A4.getHeight() - (2.0 * boarder);
        double pageHeight = PDRectangle.A4.getWidth() - (2.0 * boarder);
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        double scale = Math.min(pageHeight / imageHeight, pageWidth / imageWidth);
        int printWidth = (int) (imageWidth * scale);
        int printHeight = (int) (imageHeight * scale);

        contentStream.drawImage(pdImage, boarder, boarder, printWidth, printHeight);
        contentStream.close();
        stream.close();
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

    public static class ProgressForm {
        private final Stage dialogStage;
        private final ProgressBar pb = new ProgressBar();
        private final ProgressIndicator pin = new ProgressIndicator();

        public ProgressForm() {
            dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.UTILITY);
            dialogStage.setResizable(false);
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            final Label label = new Label();
            label.setText("alerto");

            pb.setProgress(-1F);
            pin.setProgress(-1F);

            final HBox hb = new HBox();
            hb.setSpacing(5);
            hb.setAlignment(Pos.CENTER);
            hb.getChildren().addAll(pb, pin);

            Scene scene = new Scene(hb);
            dialogStage.setScene(scene);
        }

        public void activateProgressBar(final Task<?> task) {
            pb.progressProperty().bind(task.progressProperty());
            pin.progressProperty().bind(task.progressProperty());
            dialogStage.show();
        }

        public Stage getDialogStage() {
            return dialogStage;
        }
    }
}