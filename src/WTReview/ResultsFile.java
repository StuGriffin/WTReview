package WTReview;

import java.util.Collections;

public class ResultsFile {
    private MeasurementFile measuredProfile;
    private MeasurementFile refenceProfile;
    private Profile gammaProfile;

    public ResultsFile(MeasurementFile referenceProfile, MeasurementFile measuredProfile, Profile gamma) {
        this.refenceProfile = referenceProfile;
        this.measuredProfile = measuredProfile;

        gammaProfile = gamma;
    }

    public String getResults() {
        if (refenceProfile.getOrientation() != measuredProfile.getOrientation()) {
            // TODO handle null better.
            return null;
        }

        if (refenceProfile.getOrientation() != ProfileOrientation.DepthDose) {
            double referenceWidth = refenceProfile.findProfileWidth();
            double measuredWidth = measuredProfile.findProfileWidth();
            double widthDifference = measuredWidth - referenceWidth;
            double widthDifferencePercentage = (widthDifference / referenceWidth) * 100;
            double maxGamma = Collections.max(gammaProfile.getyValues());
            String widthType = refenceProfile.getOrientation() == ProfileOrientation.Lateral ? "FWQM" : "FWHM";

            return String.format("Reference %s: %.2fmm\nMeasured %s: %.2fmm\nDifference: %.2fmm\nDifference: %.2f%%\nMax Gamma: %.2f", widthType, referenceWidth, widthType, measuredWidth, widthDifference, widthDifferencePercentage, maxGamma);
        }

        return null;
    }
}
