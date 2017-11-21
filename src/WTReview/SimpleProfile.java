package WTReview;

import java.util.ArrayList;

public class SimpleProfile {

    private final ArrayList<Double> xValues;
    private final ArrayList<Double> yValues;

    SimpleProfile(ArrayList<Double> xValues, ArrayList<Double> yValues) {

        if (xValues.size() != yValues.size()) {
            String error = String.format("Profile Data created with uneven dimensions: %s, %s", xValues.size(), yValues.size());
            System.out.println(error);
        }

        this.xValues = xValues;
        this.yValues = yValues;
    }

    public ArrayList<Double> getY() {
        return yValues;
    }

    public ArrayList<Double> getX() {
        return xValues;
    }
}
