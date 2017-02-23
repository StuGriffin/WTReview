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

public class ProfileUtilities {

    // Assumptions:
    //  - Both channels are normalised. Therefor doseDifferenceTolerance can remain as a percentage.
    //
    // Conditions:
    // - Gamma is calculated based on a 'global' tolerance.
    public static Profile calcGamma(Profile referenceProfile, Profile measuredProfile, double distanceToAgreement, double doseDifferenceTolerance) {
        ArrayList<Double> gamma = new ArrayList<>();

        ArrayList<Double> refx = referenceProfile.getxValues();
        ArrayList<Double> measx = measuredProfile.getxValues();

        double startValue = Math.max(refx.get(0), measx.get(0));
        double endValue = Math.min(refx.get(refx.size() - 1), measx.get(measx.size() - 1));

        int refLowerIndex = refx.indexOf(startValue);
        int refUpperIndex = refx.indexOf(endValue);
        int refSamples = refUpperIndex - refLowerIndex;

        int measLowerIndex = measx.indexOf(startValue);
        int measUpperIndex = measx.indexOf(endValue);
        int measSamples = measUpperIndex - measLowerIndex;

        double refSpacing = refx.get(1) - refx.get(0);

        // TODO investigate java double comparison best practices.
        if (refSamples != measSamples) {
            String errorString = String.format("Spacing is not equivalent, ref samples:%s, meas samples:%s", refSamples, measSamples);
            System.out.println(errorString);
            return null;
        }

        List<Double> baseX = refx.subList(refLowerIndex, refUpperIndex);
        List<Double> refy = referenceProfile.getyValues().subList(refLowerIndex, refUpperIndex);
        List<Double> measy = measuredProfile.getyValues().subList(measLowerIndex, measUpperIndex);

        int searchDistance = (int) ((2 * distanceToAgreement) / refSpacing);

        double dtaSqrd = distanceToAgreement * distanceToAgreement;
        double ddtSqrd = doseDifferenceTolerance * doseDifferenceTolerance;

        for (int i = 0; i < baseX.size(); i++) {
            ArrayList<Double> gammaValues = new ArrayList<>();

            int startIndex = Math.max(0, i - searchDistance);
            int endIndex = Math.min(baseX.size(), i + 1 + searchDistance);

            double refDose = refy.get(i);
            double refPos = baseX.get(i);

            for (int j = startIndex; j < endIndex; j++) {
                double deltaDose = measy.get(j) - refDose;
                double deltaPos = Math.abs(refPos - baseX.get(j));

                double doseSquared = (deltaDose * deltaDose) / ddtSqrd;
                double posSquared = (deltaPos * deltaPos) / dtaSqrd;

                gammaValues.add(doseSquared + posSquared);
            }

            gamma.add(Collections.min(gammaValues));
        }

        for (int i = 0; i < gamma.size(); i++) {
            double sqrt = Math.sqrt(gamma.get(i));
            gamma.set(i, sqrt);
        }

        ArrayList<Double> gammaX = new ArrayList<>();
        gammaX.addAll(baseX);

        return new Profile(gammaX, gamma);
    }

    public static Profile calcRatio(Profile referenceProfile, Profile measuredProfile) {

        // TODO generify duplicate code (with calcGamma) that finds common range for ref/meas profiles.
        ArrayList<Double> ratio = new ArrayList<>();

        ArrayList<Double> refx = referenceProfile.getxValues();
        ArrayList<Double> measx = measuredProfile.getxValues();

        double startValue = Math.max(refx.get(0), measx.get(0));
        double endValue = Math.min(refx.get(refx.size() - 1), measx.get(measx.size() - 1));

        int refLowerIndex = refx.indexOf(startValue);
        int refUpperIndex = refx.indexOf(endValue);
        int refSamples = refUpperIndex - refLowerIndex;

        int measLowerIndex = measx.indexOf(startValue);
        int measUpperIndex = measx.indexOf(endValue);
        int measSamples = measUpperIndex - measLowerIndex;

        double refSpacing = refx.get(1) - refx.get(0);

        // TODO investigate java double comparison best practices.
        if (refSamples != measSamples) {
            String errorString = String.format("Spacing is not equivalent, ref samples:%s, meas samples:%s", refSamples, measSamples);
            System.out.println(errorString);
            return null;
        }

        List<Double> baseX = refx.subList(refLowerIndex, refUpperIndex);
        List<Double> refy = referenceProfile.getyValues().subList(refLowerIndex, refUpperIndex);
        List<Double> measy = measuredProfile.getyValues().subList(measLowerIndex, measUpperIndex);

        for (int i = 0; i < baseX.size(); i++) {
            ratio.add(measy.get(i) / refy.get(i));
        }

        ArrayList<Double> ratioX = new ArrayList<>();
        ratioX.addAll(baseX);

        return new Profile(ratioX, ratio);
    }
}
