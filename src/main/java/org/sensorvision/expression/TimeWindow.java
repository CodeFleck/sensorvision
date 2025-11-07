package org.sensorvision.expression;

import java.time.Duration;
import java.time.Instant;

/**
 * Time window specifications for statistical aggregations.
 * Supports common time ranges: 5m, 15m, 1h, 24h, 7d, 30d.
 */
public enum TimeWindow {
    FIVE_MINUTES("5m", Duration.ofMinutes(5)),
    FIFTEEN_MINUTES("15m", Duration.ofMinutes(15)),
    ONE_HOUR("1h", Duration.ofHours(1)),
    SIX_HOURS("6h", Duration.ofHours(6)),
    TWELVE_HOURS("12h", Duration.ofHours(12)),
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
     * Get the start time for this window relative to a reference time.
     */
    public Instant getStartTime(Instant referenceTime) {
        return referenceTime.minus(duration);
    }

    /**
     * Parse a time window string (case-insensitive).
     *
     * @param windowStr Time window string (e.g., "5m", "1h", "7d")
     * @return The corresponding TimeWindow enum
     * @throws IllegalArgumentException if the window string is invalid
     */
    public static TimeWindow parse(String windowStr) {
        if (windowStr == null || windowStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Time window cannot be null or empty");
        }

        String normalized = windowStr.trim().toLowerCase();
        for (TimeWindow window : values()) {
            if (window.code.equals(normalized)) {
                return window;
            }
        }

        throw new IllegalArgumentException(
            "Invalid time window: " + windowStr +
            ". Supported: 5m, 15m, 1h, 6h, 12h, 24h, 7d, 30d"
        );
    }

    @Override
    public String toString() {
        return code;
    }
}
