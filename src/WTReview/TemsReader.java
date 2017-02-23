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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

class TemsReader {
    public static MeasurementFile ReadInProfiles(String fullFilePath) {

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
