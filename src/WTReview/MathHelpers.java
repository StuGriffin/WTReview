package WTReview;

public class MathHelpers {
    public static Boolean hasSameSign(double a, double b) {
        return a * b >= 0.0d;
    }

    public static double findWholeNumberClosestToZero(double value) {
        if (value < 0) {
            return Math.ceil(value);
        } else {
            return Math.floor(value);
        }
    }
}
