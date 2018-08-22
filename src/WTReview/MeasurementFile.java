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

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MeasurementFile implements Comparable<MeasurementFile> {
    private final List<Double> depths;
    private final Path fileName;
    private final ProfileOrientation orientation;
    private final Boolean[] enabledChannels;
    private final ArrayList<MeasurementPoint> data;
    private final int fieldWidth;


    MeasurementFile(Path fileName, boolean isLateral, Boolean[] channels, boolean isPDD, ArrayList<MeasurementPoint> data) {
        this.fileName = fileName;
        this.enabledChannels = channels;
        this.data = data;
        this.orientation = getProfileOrientation(isPDD, isLateral);
        this.depths = data.stream().map(MeasurementPoint::getVerticalPos).distinct().collect(Collectors.toList());
        this.fieldWidth = ProfileUtilities.estimateFieldWidth(fileName.getFileName().toString());

        //buildProfiles();
    }

    private ArrayList<Profile> buildProfiles(int primaryChannel) {

        ArrayList<Profile> profiles = new ArrayList<>();

        if (!enabledChannels[primaryChannel]) {
            return profiles;
        }

        if (orientation == ProfileOrientation.PDD) {
            Profile rawProfile = getRawProfile(-1, primaryChannel);
            if (rawProfile != null) {
                profiles.add(rawProfile);
            }
            return profiles;
        }

        for (Double depth : depths) {
            Profile rawProfile = getRawProfile(depth, primaryChannel);
            if (rawProfile != null) {
                profiles.add(rawProfile);
            }
        }

        return profiles;
    }

    ArrayList<Profile> getProfiles(int primaryChannel) {
        return buildProfiles(primaryChannel);
    }

    public ProfileOrientation getOrientation() {
        return orientation;
    }

    Profile getProfile() {
        int firstEnabledChannel = Arrays.asList(enabledChannels).indexOf(true);
        return getProfile(firstEnabledChannel);
    }

    Profile getProfile(int primaryChannel) {
        double depth = 15.0;
        double xSpacing = orientation == ProfileOrientation.Long ? 0.1 : 0.5;
        xSpacing = orientation == ProfileOrientation.PDD ? 1 : xSpacing;
        return getProfile(depth, primaryChannel, true, true, true, xSpacing);
    }

    private Profile getRawProfile(double depth, int primaryChannel) {
        ArrayList<Double> xValues = new ArrayList<>();
        ArrayList<Double> yValues = new ArrayList<>();

        if (orientation == ProfileOrientation.PDD) {
            xValues.addAll(data.stream().map(MeasurementPoint::getVerticalPos).collect(Collectors.toList()));
            switch (primaryChannel) {
                case 0:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel1).collect(Collectors.toList()));
                    break;
                case 1:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel2).collect(Collectors.toList()));
                    break;
                case 2:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel3).collect(Collectors.toList()));
                    break;
                case 3:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel4).collect(Collectors.toList()));
                    break;
                case 4:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel5).collect(Collectors.toList()));
                    break;
                case 5:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel6).collect(Collectors.toList()));
                    break;
                case 6:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel7).collect(Collectors.toList()));
                    break;
                case 7:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel8).collect(Collectors.toList()));
                    break;
            }
        } else {
            xValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getLateralPos).collect(Collectors.toList()));
            switch (primaryChannel) {
                case 0:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel1).collect(Collectors.toList()));
                    break;
                case 1:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel2).collect(Collectors.toList()));
                    break;
                case 2:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel3).collect(Collectors.toList()));
                    break;
                case 3:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel4).collect(Collectors.toList()));
                    break;
                case 4:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel5).collect(Collectors.toList()));
                    break;
                case 5:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel6).collect(Collectors.toList()));
                    break;
                case 6:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel7).collect(Collectors.toList()));
                    break;
                case 7:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel8).collect(Collectors.toList()));
                    break;
            }
        }

        try {
            return new Profile(xValues, yValues, fileName.toString(), orientation, depth);
        } catch (Exception e) {
            // TODO improve error handling with profile generation!
            return null;
        }
    }

    private Profile getProfile(double depth, int primaryChannel, boolean normalise, boolean centre, boolean resample, double newSpacing) {
        ArrayList<Double> xValues = new ArrayList<>();
        ArrayList<Double> yValues = new ArrayList<>();

        if (orientation == ProfileOrientation.PDD) {
            xValues.addAll(data.stream().map(MeasurementPoint::getVerticalPos).collect(Collectors.toList()));
            switch (primaryChannel) {
                case 0:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel1).collect(Collectors.toList()));
                    break;
                case 1:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel2).collect(Collectors.toList()));
                    break;
                case 2:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel3).collect(Collectors.toList()));
                    break;
                case 3:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel4).collect(Collectors.toList()));
                    break;
                case 4:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel5).collect(Collectors.toList()));
                    break;
                case 5:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel6).collect(Collectors.toList()));
                    break;
                case 6:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel7).collect(Collectors.toList()));
                    break;
                case 7:
                    yValues.addAll(data.stream().map(MeasurementPoint::getChannel8).collect(Collectors.toList()));
                    break;
            }
        } else {
            xValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getLateralPos).collect(Collectors.toList()));
            switch (primaryChannel) {
                case 0:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel1).collect(Collectors.toList()));
                    break;
                case 1:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel2).collect(Collectors.toList()));
                    break;
                case 2:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel3).collect(Collectors.toList()));
                    break;
                case 3:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel4).collect(Collectors.toList()));
                    break;
                case 4:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel5).collect(Collectors.toList()));
                    break;
                case 5:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel6).collect(Collectors.toList()));
                    break;
                case 6:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel7).collect(Collectors.toList()));
                    break;
                case 7:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(MeasurementPoint::getChannel8).collect(Collectors.toList()));
                    break;
            }
        }

        try {
            // Check the profile is orientated correctly, i.e. increasing x.
            if (xValues.get(0) > xValues.get(xValues.size() - 1)) {
                Collections.reverse(xValues);
                Collections.reverse(yValues);
            }

            if (normalise) {
                yValues = NormaliseProfile(yValues);
            }

            if (orientation != ProfileOrientation.PDD && centre) {

                double percentageHeight = orientation == ProfileOrientation.Long ? 0.5 : 0.25;
                double profileCentre = findProfileCentre(xValues, yValues, percentageHeight);

                // Check if the profile is an on-axis profile before centring
                if (profileCentre < 5 && profileCentre > -5) {
                    xValues = CentreProfile(xValues, yValues, percentageHeight);
                }
            }

            Profile profile = new Profile(xValues, yValues);

            if (resample) {
                profile = resampleProfile(profile, newSpacing);
            }

            return profile;
        } catch (Exception e) {
            // TODO improve error handling with profile generation!
            return null;
        }
    }

    private ProfileOrientation getProfileOrientation(Boolean isPDD, boolean isLateral) {
        if (isPDD) {
            return ProfileOrientation.PDD;
        } else if (isLateral) {
            return ProfileOrientation.Lat;
        } else {
            return ProfileOrientation.Long;
        }
    }

    private double findProfileEdge(ArrayList<Double> xValues, ArrayList<Double> yValues, int startIndex, double percentageHeight) {

        int i = Math.max(startIndex, 2);
        int lengthOfProfile = yValues.size();
        while (MathHelpers.hasSameSign(yValues.get(i) - percentageHeight, yValues.get(i - 1) - percentageHeight) && i < lengthOfProfile) {
            i++;
        }

        ArrayList<Double> horizontal = new ArrayList<>();
        horizontal.add(xValues.get(i - 1));
        horizontal.add(xValues.get(i));

        ArrayList<Double> vertical = new ArrayList<>();
        vertical.add(yValues.get(i - 1));
        vertical.add(yValues.get(i));

        // Check the profile is increasing in x otherwise reverse.
        if (vertical.get(0) > vertical.get(vertical.size() - 1)) {
            Collections.reverse(vertical);
            Collections.reverse(horizontal);
        }

        double[] v = vertical.stream().mapToDouble(x -> x).toArray();
        double[] h = horizontal.stream().mapToDouble(x -> x).toArray();

        LinearInterpolator interpolation = new LinearInterpolator();
        PolynomialSplineFunction interpolationFunction = interpolation.interpolate(v, h);

        return interpolationFunction.value(percentageHeight);
    }

    private int findProfileCenterIndex(ArrayList<Double> yValues, double percentageHeight) {

        if (yValues.get(0) < percentageHeight) {
            double maxValue = Collections.max(yValues);
            return yValues.indexOf(maxValue);
        }

        double minValue = Collections.min(yValues);
        return yValues.indexOf(minValue);
    }

    private ArrayList<Double> NormaliseProfile(ArrayList<Double> valuesToNormalise) {
        Double maxHeight = Collections.max(valuesToNormalise);
        ArrayList<Double> normalisedValues = new ArrayList<>();
        for (Double value : valuesToNormalise) {
            normalisedValues.add(value / maxHeight);
        }

        return normalisedValues;
    }

    private ArrayList<Double> CentreProfile(ArrayList<Double> xValues, ArrayList<Double> yValues, double percentageHeight) {
        double profileCentre = findProfileCentre(xValues, yValues, percentageHeight);
        ArrayList<Double> centredX = new ArrayList<>();
        for (Double value : xValues) {
            centredX.add(value - profileCentre);
        }
        return centredX;
    }

    private double findProfileCentre(ArrayList<Double> xValues, ArrayList<Double> yValues, double percentageHeight) {
        int centreIndex = findProfileCenterIndex(yValues, percentageHeight);
        double leadingEdge = findProfileEdge(xValues, yValues, 0, percentageHeight);
        double trailingEdge = findProfileEdge(xValues, yValues, centreIndex, percentageHeight);
        return 0.5 * (leadingEdge + trailingEdge);
    }

    private Profile resampleProfile(Profile profile, double desiredXSpacing) {
        double[] x = profile.getX().stream().mapToDouble(p -> p).toArray();
        double[] y = profile.getY().stream().mapToDouble(p -> p).toArray();
        LinearInterpolator interpolation = new LinearInterpolator();
        PolynomialSplineFunction interpolationFunction = interpolation.interpolate(x, y);

        double xStart = MathHelpers.findWholeNumberClosestToZero(x[0]);
        double xEnd = MathHelpers.findWholeNumberClosestToZero(x[x.length - 1]);

        int numberOfElements = (int) ((xEnd - xStart) / desiredXSpacing);
        ArrayList<Double> newX = new ArrayList<>();
        ArrayList<Double> newY = new ArrayList<>();
        for (int i = 0; i < numberOfElements; i++) {
            double currentX = xStart + (i * desiredXSpacing);
            newX.add(currentX);
            newY.add(interpolationFunction.value(currentX));
        }

        return new Profile(newX, newY);
    }

    private String findProfilePosition() {
        if (getProfile() == null) {
            return "";
        }

        if (getOrientation() == ProfileOrientation.PDD) {
            return "";
        }

        double referenceWidth = getOrientation() == ProfileOrientation.Lat ? 0.25 : 0.5;
        double profileCentre = findProfileCentre(getProfile().getX(), getProfile().getY(), referenceWidth);

        double allowableRange = 10;
        if (profileCentre < -allowableRange) {
            return "-IECy";
        } else if (profileCentre > allowableRange) {
            return "+IECy";
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        String field = fieldWidth > 0 ? Double.toString(fieldWidth) + "mm" : fileName.toString();
        String position = findProfilePosition();
        return String.format("%s - %s %s", orientation, field, position);
    }

    @Override
    public int compareTo(MeasurementFile other) {

        int thisSize = this.fieldWidth + this.orientation.getValue();
        int otherSize = other.fieldWidth + other.orientation.getValue();

        return Integer.compare(thisSize, otherSize);
    }
}