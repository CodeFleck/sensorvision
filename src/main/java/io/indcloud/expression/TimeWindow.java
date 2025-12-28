package io.indcloud.expression;

import java.time.Duration;
import java.time.Instant;

/**
 * Enumeration of supported time windows for statistical functions.
 * Used in functions like avg(variable, "5m"), stddev(variable, "1h"), etc.
 */
public enum TimeWindow {
    FIVE_MINUTES("5m", Duration.ofMinutes(5)),
    FIFTEEN_MINUTES("15m", Duration.ofMinutes(15)),
    ONE_HOUR("1h", Duration.ofHours(1)),
    TWENTY_FOUR_HOURS("24h", Duration.ofHours(24)),
    SEVEN_DAYS("7d", Duration.ofDays(7)),
    THIRTY_DAYS("30d", Duration.ofDays(30));

    private final String code;
    private final Duration duration;

    TimeWindow(String code, Duration duration) {
        this.code = code;
        this.duration = duration;
    }

    public String getCode() {
        return code;
    }

    public Duration getDuration() {
        return duration;
    }

    /**
     * Calculate the start time for this time window from a given timestamp.
     */
    public Instant calculateStartTime(Instant endTime) {
        return endTime.minus(duration);
    }

    /**
     * Parse a time window code (case-insensitive).
     *
     * @param code The time window code (e.g., "5m", "1h", "7d")
     * @return The corresponding TimeWindow enum
     * @throws IllegalArgumentException if the code is invalid
     */
    public static TimeWindow fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Time window code cannot be null");
        }

        String normalized = code.trim().toLowerCase();
        for (TimeWindow window : values()) {
            if (window.code.equals(normalized)) {
                return window;
            }
        }

        throw new IllegalArgumentException(
            "Invalid time window code: " + code +
            ". Valid options: 5m, 15m, 1h, 24h, 7d, 30d"
        );
    }

    @Override
    public String toString() {
        return code;
    }
}