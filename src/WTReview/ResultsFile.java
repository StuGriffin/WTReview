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

import java.util.Collections;

class ResultsFile {
    private final Profile measuredProfile;
    private final Profile referenceProfile;
    private final SimpleProfile gammaProfile;

    public ResultsFile(Profile referenceProfile, Profile measuredProfile, SimpleProfile gamma) {
        this.referenceProfile = referenceProfile;
        this.measuredProfile = measuredProfile;
        this.gammaProfile = gamma;
    }

    public String getResults() {
        if (referenceProfile.getOrientation() != measuredProfile.getOrientation()) {
            // TODO handle null better.
            return null;
        }

        if (referenceProfile.getOrientation() != ProfileOrientation.PDD) {
            double referenceWidth = ProfileUtilities.findProfileWidth(referenceProfile);
            double measuredWidth = ProfileUtilities.findProfileWidth(measuredProfile);
            double widthDifference = measuredWidth - referenceWidth;
            double widthDifferencePercentage = (widthDifference / referenceWidth) * 100;
            double maxGamma = Collections.max(gammaProfile.getY());
            String widthType = referenceProfile.getOrientation() == ProfileOrientation.Lat ? "FWQM" : "FWHM";

            return String.format("Reference %s: %.2fmm\nMeasured %s: %.2fmm\nDifference: %.2fmm\nDifference: %.2f%%\nMax Gamma: %.2f", widthType, referenceWidth, widthType, measuredWidth, widthDifference, widthDifferencePercentage, maxGamma);
        }

        return null;
    }
}
