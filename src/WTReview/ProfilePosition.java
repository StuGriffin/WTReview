package WTReview;

public enum ProfilePosition {
    OnAxis(0, ""),
    Positive(1, "+IECy"),
    Negative(2, "-IECy");

    private final int value;
    private final String description;

    ProfilePosition(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
