package WTReview;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Controller {
    @FXML
    private void handleButtonAction(ActionEvent event) {
        String mLong = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/mlong.csv";
        String mLat = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/mLat.csv";
        String mPDD = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/mPDD.csv";

        ReadInProfiles(mLong);
        ReadInProfiles(mLat);
        ReadInProfiles(mPDD);

        String rLong = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/rlong.csv";
        String rLat = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/rLat.csv";
        String rPDD = "/Users/stgriffin/Library/Mobile Documents/com~apple~CloudDocs/Projects/Coding/WTReview/TestData/rPDD.csv";

        ReadInProfiles(rLong);
        ReadInProfiles(rLat);
        ReadInProfiles(rPDD);
    }

    private void ReadInProfiles(String fullFilePath) {

        Path path = Paths.get(fullFilePath);

        try (Scanner reader = new Scanner(path)) {

            //reader.useDelimiter("\r|\n|\\*");

            Pattern headerPattern = Pattern.compile("\\*");
            Pattern dataPattern = Pattern.compile(",");
            int correctColumn = 2;

            // Skip first line which should contain data information.
            String date = reader.nextLine();

            // Check version number.
            double version = Double.parseDouble(headerPattern.split(reader.nextLine())[correctColumn]);

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
            String blankLine = reader.nextLine();
            String headerLine = reader.nextLine();

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

            String successString = String.format("Read profile successfully: %s", fullFilePath);
            System.out.println(successString);
        } catch (Exception exception) {
            String errorString = String.format("Error during ReadInProfiles: %s", exception.toString());
            System.out.println(errorString);
        }
    }
}

