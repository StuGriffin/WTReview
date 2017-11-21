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

import static WTReview.ProfileUtilities.*;

public class Profile implements Comparable<Profile> {

    private final ArrayList<Double> xValues;
    private final ArrayList<Double> yValues;
    private String fileName;
    private Double depth;
    private int nominalFieldWidth;
    private ProfileOrientation orientation;
    private ProfilePosition position;

    Profile(ArrayList<Double> xValues, ArrayList<Double> yValues) {

        if (xValues.size() != yValues.size()) {
            String error = String.format("Profile Data created with uneven dimensions: %s, %s", xValues.size(), yValues.size());
            System.out.println(error);
        }

        this.xValues = xValues;
        this.yValues = yValues;
    }

    Profile(ArrayList<Double> xValues, ArrayList<Double> yValues, String fileName, ProfileOrientation orientation, Double depth) {
        if (xValues.size() != yValues.size()) {
            throw new IllegalArgumentException(String.format("The input dimensions are not equal, x: %s, y: %s", xValues.size(), yValues.size()));
        }

        // Check the profile is increasing in x and if not reverse it.
        if (xValues.get(0) > xValues.get(xValues.size() - 1)) {
            Collections.reverse(xValues);
            Collections.reverse(yValues);
        }

        this.fileName = fileName;
        this.orientation = orientation;
        this.depth = depth;
        this.nominalFieldWidth = estimateFieldWidth(fileName);
        this.position = estimateProfilePosition(orientation, xValues, yValues);
        this.xValues = getCentredX(orientation, xValues, yValues);
        this.yValues = normaliseProfile(yValues);
    }

    public ArrayList<Double> getY() {
        return yValues;
    }

    public ArrayList<Double> getX() {
        return xValues;
    }

    public ProfileOrientation getOrientation() {
        return orientation;
    }

    ArrayList<Double> getResampleY(ArrayList<Double> xValues) {
        double[] x = this.getX().stream().mapToDouble(p -> p).toArray();
        double[] y = this.getY().stream().mapToDouble(p -> p).toArray();

        LinearInterpolator interpolation = new LinearInterpolator();
        PolynomialSplineFunction interpolationFunction = interpolation.interpolate(x, y);

        ArrayList<Double> newY = new ArrayList<>();

        for (Double xValue : xValues) {
            newY.add(interpolationFunction.value(xValue));
        }

        return newY;
    }

    @Override
    public String toString() {
        if (nominalFieldWidth <= 0) {
            return fileName;
        }

        if (orientation == ProfileOrientation.PDD) {
            return String.format("%s - %smm", orientation, Double.toString(nominalFieldWidth));
        }

        return String.format("%s - %smm @ d%smm %s", orientation, Double.toString(nominalFieldWidth), Double.toString(depth), position.getDescription());
    }

    @Override
    public int compareTo(Profile other) {
        int thisSize = this.nominalFieldWidth + this.orientation.getValue() + this.position.getValue();
        int otherSize = other.nominalFieldWidth + other.orientation.getValue() + other.position.getValue();
        return Integer.compare(thisSize, otherSize);
    }

    public Double getDepth() {
        return depth;
    }
}

