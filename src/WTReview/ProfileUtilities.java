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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ProfileUtilities {
    static SimpleProfile calcGamma(Profile referenceProfile, Profile measuredProfile, double distanceToAgreement, double doseDifferenceTolerance) {

        double sampling = referenceProfile.getOrientation() == ProfileOrientation.Lat ? 0.5 : 0.1;
        ArrayList<Double> baseX = resampleX(referenceProfile, measuredProfile, sampling);
        ArrayList<Double> refY = referenceProfile.getResampleY(baseX);
        ArrayList<Double> measY = measuredProfile.getResampleY(baseX);
        ArrayList<Double> gamma = new ArrayList<>();

        double spacing = baseX.get(1) - baseX.get(0);
        int searchDistance = (int) ((2 * distanceToAgreement) / spacing);

        double dtaSquared = distanceToAgreement * distanceToAgreement;
        double ddtSquared = doseDifferenceTolerance * doseDifferenceTolerance;

        for (int i = 0; i < baseX.size(); i++) {
            ArrayList<Double> gammaValues = new ArrayList<>();

            int startIndex = Math.max(0, i - searchDistance);
            int endIndex = Math.min(baseX.size(), i + 1 + searchDistance);

            double refDose = refY.get(i);
            double refPos = baseX.get(i);

            for (int j = startIndex; j < endIndex; j++) {
                double deltaDose = measY.get(j) - refDose;
                double deltaPos = Math.abs(refPos - baseX.get(j));

                double doseSquared = (deltaDose * deltaDose) / ddtSquared;
                double posSquared = (deltaPos * deltaPos) / dtaSquared;

                gammaValues.add(doseSquared + posSquared);
            }

            gamma.add(Collections.min(gammaValues));
        }

        for (int i = 0; i < gamma.size(); i++) {
            double squareRoot = Math.sqrt(gamma.get(i));
            gamma.set(i, squareRoot);
        }

        ArrayList<Double> gammaX = new ArrayList<>();
        gammaX.addAll(baseX);

        return new SimpleProfile(gammaX, gamma);
    }

    static SimpleProfile calcRatio(Profile referenceProfile, Profile measuredProfile) {

        double sampling = 1;
        ArrayList<Double> baseX = resampleX(referenceProfile, measuredProfile, sampling);
        ArrayList<Double> refY = referenceProfile.getResampleY(baseX);
        ArrayList<Double> measY = measuredProfile.getResampleY(baseX);
        ArrayList<Double> ratio = new ArrayList<>();

        for (int i = 0; i < baseX.size(); i++) {
            ratio.add(measY.get(i) / refY.get(i));
        }

        ArrayList<Double> ratioX = new ArrayList<>();
        ratioX.addAll(baseX);

        return new SimpleProfile(ratioX, ratio);
    }

    static double findProfileWidth(Profile profile) {
        return findProfileWidth(profile.getOrientation(), profile.getX(), profile.getY());
    }

    static ArrayList<Double> getCentredX(ProfileOrientation orientation, ArrayList<Double> x, ArrayList<Double> y) {
        if (orientation == ProfileOrientation.PDD) {
            return x;
        }

        y = normaliseProfile(y);
        double percentageHeight = getPercentageHeight(orientation);
        double profileCentre = findProfileCentre(x, y, percentageHeight);

        // Test to see if its an off axis profile.
        if (profileCentre > 5 || profileCentre < -5) {
            //return x;
        }

        ArrayList<Double> centredX = new ArrayList<>();
        for (Double value : x) {
            centredX.add(value - profileCentre);
        }
        return centredX;
    }

    static ProfilePosition estimateProfilePosition(ProfileOrientation orientation, ArrayList<Double> x, ArrayList<Double> y) {

        if (orientation == ProfileOrientation.PDD) {
            return ProfilePosition.OnAxis;
        }

        y = normaliseProfile(y);
        double profileCentre = findProfileCentre(x, y, getPercentageHeight(orientation));
        double allowableRange = 10;

        if (profileCentre < -allowableRange) {
            return ProfilePosition.Negative;
        } else if (profileCentre > allowableRange) {
            return ProfilePosition.Positive;
        } else {
            return ProfilePosition.OnAxis;
        }
    }

    static ArrayList<Double> normaliseProfile(ArrayList<Double> valuesToNormalise) {
        ArrayList<Double> normalisedValues = new ArrayList<>();
        Double maxHeight = Collections.max(valuesToNormalise);
        for (Double value : valuesToNormalise) {
            normalisedValues.add(value / maxHeight);
        }

        return normalisedValues;
    }

    static int estimateFieldWidth(String fileName) {
        // TODO add filter strings to properties file for easy customisation.
        if (fileName.contains("1cm") || fileName.contains("10mm") || fileName.contains("J07") || fileName.contains("J7")) {
            return 10;
        } else if (fileName.contains("1.8cm") || fileName.contains("18mm") || fileName.contains("J14")) {
            return 18;
        } else if (fileName.contains("2.5cm") || fileName.contains("25mm") || fileName.contains("J20")) {
            return 25;
        } else if (fileName.contains("5cm") || fileName.contains("50mm") || fileName.contains("J42")) {
            return 50;
        } else return -1;
    }


    private static double getPercentageHeight(ProfileOrientation orientation) {
        return orientation == ProfileOrientation.Lat ? 0.25 : 0.5;
    }

    private static List<Double> GetCommonX(Profile referenceProfile, Profile measuredProfile) {
        ArrayList<Double> refX = referenceProfile.getX();
        ArrayList<Double> measX = measuredProfile.getX();

        double startValue = Math.max(refX.get(0), measX.get(0));
        double endValue = Math.min(refX.get(refX.size() - 1), measX.get(measX.size() - 1));

        int refLowerIndex = refX.indexOf(startValue);
        int refUpperIndex = refX.indexOf(endValue);
        int refSamples = refUpperIndex - refLowerIndex;

        int measLowerIndex = measX.indexOf(startValue);
        int measUpperIndex = measX.indexOf(endValue);
        int measSamples = measUpperIndex - measLowerIndex;

        // TODO investigate java double comparison best practices.
        if (refSamples != measSamples) {
            String errorString = String.format("Spacing is not equivalent, ref samples:%s, meas samples:%s", refSamples, measSamples);
            System.out.println(errorString);
            return null;
        }

        return refX.subList(refLowerIndex, refUpperIndex + 1);
    }

    private static ArrayList<Double> resampleX(Profile reference, Profile measured, double spacing) {
        ArrayList<Double> refX = reference.getX();
        ArrayList<Double> measX = measured.getX();

        double startValue = Math.max(refX.get(0), measX.get(0));
        double endValue = Math.min(refX.get(refX.size() - 1), measX.get(measX.size() - 1));
        int numberOfSamples = (int) ((endValue - startValue) / spacing);


        ArrayList<Double> newX = new ArrayList<>();

        for (int i = 0; i < numberOfSamples; i++) {
            double currentX = startValue + (i * spacing);
            newX.add(currentX);
        }

        return newX;
    }

    private static List<Double> GetSubsetY(Profile profile, List<Double> subsetX) {
        int startIndex = profile.getX().indexOf(subsetX.get(0));
        int endIndex = profile.getX().indexOf(subsetX.get(subsetX.size() - 1)) + 1;
        return profile.getY().subList(startIndex, endIndex);
    }

    private static double findProfileCentre(ArrayList<Double> xValues, ArrayList<Double> yValues, double percentageHeight) {
        int centreIndex = findProfileCenterIndex(yValues, percentageHeight);
        double leadingEdge = findProfileEdge(xValues, yValues, 0, percentageHeight);
        double trailingEdge = findProfileEdge(xValues, yValues, centreIndex, percentageHeight);
        return 0.5 * (leadingEdge + trailingEdge);
    }

    private static int findProfileCenterIndex(ArrayList<Double> yValues, double percentageHeight) {

        if (yValues.get(0) < percentageHeight) {
            double maxValue = Collections.max(yValues);
            return yValues.indexOf(maxValue);
        }

        double minValue = Collections.min(yValues);
        return yValues.indexOf(minValue);
    }

    private static double findProfileWidth(ProfileOrientation orientation, ArrayList<Double> x, ArrayList<Double> y) {
        double percentageHeight = getPercentageHeight(orientation);
        int centerIndex = findProfileCenterIndex(y, percentageHeight);
        double leadingEdge = findProfileEdge(x, y, 0, percentageHeight);
        double trailingEdge = findProfileEdge(x, y, centerIndex, percentageHeight);
        return Math.abs(trailingEdge - leadingEdge);
    }

    private static double findProfileEdge(ArrayList<Double> xValues, ArrayList<Double> yValues, int startIndex, double percentageHeight) {

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
}