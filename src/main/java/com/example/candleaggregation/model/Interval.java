package com.example.candleaggregation.model;

public enum Interval {
    ONE_SECOND("1s", 1),
    FIVE_SECONDS("5s", 5),
    ONE_MINUTE("1m", 60),
    FIVE_MINUTES("5m", 300),
    FIFTEEN_MINUTES("15m", 900),
    ONE_HOUR("1h", 3600);

    private final String label;
    private final long seconds;

    Interval(String label, long seconds) {
        this.label = label;
        this.seconds = seconds;
    }

    public String getLabel() {
        return label;
    }

    public long getSeconds() {
        return seconds;
    }

    public static Interval fromString(String label) {
        for (Interval i : values()) {
            if (i.label.equalsIgnoreCase(label)) return i;
        }
        throw new IllegalArgumentException(
            "Unknown interval: '" + label + "'. Valid values: 1s, 5s, 1m, 5m, 15m, 1h");
    }
}
