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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ProfileUtilities {

    // Assumptions:
    //  - Both channels are normalised. Therefor doseDifferenceTolerance can remain as a percentage.
    //
    // Conditions:
    // - Gamma is calculated based on a 'global' tolerance.
    public static Profile calcGamma(Profile referenceProfile, Profile measuredProfile, double distanceToAgreement, double doseDifferenceTolerance) {
        ArrayList<Double> gamma = new ArrayList<>();

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

        double refSpacing = refX.get(1) - refX.get(0);

        // TODO investigate java double comparison best practices.
        if (refSamples != measSamples) {
            String errorString = String.format("Spacing is not equivalent, ref samples:%s, meas samples:%s", refSamples, measSamples);
            System.out.println(errorString);
            return null;
        }

        List<Double> baseX = refX.subList(refLowerIndex, refUpperIndex);
        List<Double> refY = referenceProfile.getY().subList(refLowerIndex, refUpperIndex);
        List<Double> measY = measuredProfile.getY().subList(measLowerIndex, measUpperIndex);

        int searchDistance = (int) ((2 * distanceToAgreement) / refSpacing);

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

        return new Profile(gammaX, gamma);
    }

    public static Profile calcRatio(Profile referenceProfile, Profile measuredProfile) {

        // TODO generify duplicate code (with calcGamma) that finds common range for ref/meas profiles.
        ArrayList<Double> ratio = new ArrayList<>();

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

        List<Double> baseX = refX.subList(refLowerIndex, refUpperIndex);
        List<Double> refY = referenceProfile.getY().subList(refLowerIndex, refUpperIndex);
        List<Double> measY = measuredProfile.getY().subList(measLowerIndex, measUpperIndex);

        for (int i = 0; i < baseX.size(); i++) {
            ratio.add(measY.get(i) / refY.get(i));
        }

        ArrayList<Double> ratioX = new ArrayList<>();
        ratioX.addAll(baseX);

        return new Profile(ratioX, ratio);
    }
}
