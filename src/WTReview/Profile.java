package WTReview;

import java.util.ArrayList;

public class Profile {

    private ArrayList<Double> xValues;
    private ArrayList<Double> yValues;

    public Profile(ArrayList<Double> xValues, ArrayList<Double> yValues) {

        if (xValues.size() != yValues.size()) {
            String error = String.format("Profile Data created with uneven dimensions: %s, %s", xValues.size(), yValues.size());
            System.out.println(error);
        }

        this.xValues = xValues;
        this.yValues = yValues;
    }

    public ArrayList<Double> getxValues() {
        return xValues;
    }

    public ArrayList<Double> getyValues() {
        return yValues;
    }
}
