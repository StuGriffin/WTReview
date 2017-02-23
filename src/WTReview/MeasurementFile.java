package WTReview;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MeasurementFile {
    private final List<Double> depths;
    private Path fileName;
    private ProfileOrientation orientation;
    private Boolean[] enabledChannels;
    private ArrayList<MeasurementPoint> data;

    MeasurementFile(Path fileName, boolean isLateral, Boolean[] channels, boolean isPDD, ArrayList<MeasurementPoint> data) {
        this.fileName = fileName;
        this.enabledChannels = channels;
        this.data = data;
        this.orientation = getProfileOrientation(isPDD, isLateral);
        this.depths = data.stream().map(x -> x.getVerticalPos()).distinct().collect(Collectors.toList());
    }

    public ProfileOrientation getOrientation() {
        return orientation;
    }

    @Override
    public String toString() {
        return String.format("%s Profile - %s", orientation, fileName);
    }

    Profile getProfile() {
        double depth = 15.0;
        int firstEnabledChannel = Arrays.asList(enabledChannels).indexOf(true);
        double xSpacing = orientation == ProfileOrientation.Longitudinal ? 0.1 : 0.5;
        return getProfile(depth, firstEnabledChannel, true, true, true, xSpacing);
    }

    private Profile getProfile(double depth, int primaryChannel, boolean normalise, boolean centre, boolean resample, double newSpacing) {
        ArrayList<Double> xValues = new ArrayList<>();
        ArrayList<Double> yValues = new ArrayList<>();

        if (orientation == ProfileOrientation.DepthDose) {
            xValues.addAll(data.stream().map(x -> x.getVerticalPos()).collect(Collectors.toList()));
            switch (primaryChannel) {
                case 0:
                    yValues.addAll(data.stream().map(x -> x.getChannel1()).collect(Collectors.toList()));
                    break;
                case 1:
                    yValues.addAll(data.stream().map(x -> x.getChannel2()).collect(Collectors.toList()));
                    break;
                case 2:
                    yValues.addAll(data.stream().map(x -> x.getChannel3()).collect(Collectors.toList()));
                    break;
                case 3:
                    yValues.addAll(data.stream().map(x -> x.getChannel4()).collect(Collectors.toList()));
                    break;
                case 4:
                    yValues.addAll(data.stream().map(x -> x.getChannel5()).collect(Collectors.toList()));
                    break;
                case 5:
                    yValues.addAll(data.stream().map(x -> x.getChannel6()).collect(Collectors.toList()));
                    break;
                case 6:
                    yValues.addAll(data.stream().map(x -> x.getChannel7()).collect(Collectors.toList()));
                    break;
                case 7:
                    yValues.addAll(data.stream().map(x -> x.getChannel8()).collect(Collectors.toList()));
                    break;
            }
        } else {
            xValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(x -> x.getLateralPos()).collect(Collectors.toList()));
            switch (primaryChannel) {
                case 0:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(x -> x.getChannel1()).collect(Collectors.toList()));
                    break;
                case 1:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(x -> x.getChannel2()).collect(Collectors.toList()));
                    break;
                case 2:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(x -> x.getChannel3()).collect(Collectors.toList()));
                    break;
                case 3:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(x -> x.getChannel4()).collect(Collectors.toList()));
                    break;
                case 4:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(x -> x.getChannel5()).collect(Collectors.toList()));
                    break;
                case 5:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(x -> x.getChannel6()).collect(Collectors.toList()));
                    break;
                case 6:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(x -> x.getChannel7()).collect(Collectors.toList()));
                    break;
                case 7:
                    yValues.addAll(data.stream().filter(y -> y.getVerticalPos() == depth).map(x -> x.getChannel8()).collect(Collectors.toList()));
                    break;
            }
        }

        // Check the profile is orientated correctly, i.e. increasing x.
        if (xValues.get(0) > xValues.get(xValues.size() - 1)) {
            Collections.reverse(xValues);
            Collections.reverse(yValues);
        }

        if (normalise) {
            yValues = NormaliseProfile(yValues);
        }

        if (orientation != ProfileOrientation.DepthDose && centre) {
            double percentageHeight = orientation == ProfileOrientation.Longitudinal ? 0.5 : 0.25;
            xValues = CentreProfile(xValues, yValues, percentageHeight);
        }

        Profile profile = new Profile(xValues, yValues);

        if (resample) {
            profile = resampleProfile(profile, newSpacing);
        }

        return profile;
    }

    private ProfileOrientation getProfileOrientation(Boolean isPDD, boolean isLateral) {
        if (isPDD) {
            return ProfileOrientation.DepthDose;
        } else if (isLateral) {
            return ProfileOrientation.Lateral;
        } else {
            return ProfileOrientation.Longitudinal;
        }
    }

    private double findProfileEdge(ArrayList<Double> xValues, ArrayList<Double> yValues, int startIndex, double percentageHeight) {

        int i = Math.max(startIndex, 2);
        int lengthOfProfile = yValues.size();
        while (hasSameSign(yValues.get(i) - percentageHeight, yValues.get(i - 1) - percentageHeight) && i < lengthOfProfile) {
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

    private double findProfileCentre(Profile profile, double percentageHeight) {
        return findProfileCentre(profile.getxValues(), profile.getyValues(), percentageHeight);
    }

    private Profile resampleProfile(Profile profile, double desiredXSpacing) {
        double[] x = profile.getxValues().stream().mapToDouble(p -> p).toArray();
        double[] y = profile.getyValues().stream().mapToDouble(p -> p).toArray();
        LinearInterpolator interpolation = new LinearInterpolator();
        PolynomialSplineFunction interpolationFunction = interpolation.interpolate(x, y);

        double xStart = findWholeNumberClosestToZero(x[0]);
        double xEnd = findWholeNumberClosestToZero(x[x.length - 1]);

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

    private double findWholeNumberClosestToZero(double value) {
        if (value < 0) {
            return Math.ceil(value);
        } else {
            return Math.floor(value);
        }
    }

    public double findProfileWidth() {
        Profile profile = getProfile();
        double percentageHeight = orientation == ProfileOrientation.Longitudinal ? 0.5 : 0.25;
        int centerIndex = findProfileCenterIndex(profile.getyValues(), percentageHeight);
        double leadingEdge = findProfileEdge(profile.getxValues(), profile.getyValues(), 0, percentageHeight);
        double trailingEdge = findProfileEdge(profile.getxValues(), profile.getyValues(), centerIndex, percentageHeight);
        return Math.abs(trailingEdge - leadingEdge);
    }

    // TODO: move to a math library.
    private Boolean hasSameSign(double a, double b) {
        return a * b >= 0.0d;
    }
}