package org.sensorvision.expression;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TimeWindowTest {

    @Test
    void testFromCode_ValidCodes() {
        assertEquals(TimeWindow.FIVE_MINUTES, TimeWindow.fromCode("5m"));
        assertEquals(TimeWindow.FIFTEEN_MINUTES, TimeWindow.fromCode("15m"));
        assertEquals(TimeWindow.ONE_HOUR, TimeWindow.fromCode("1h"));
        assertEquals(TimeWindow.TWENTY_FOUR_HOURS, TimeWindow.fromCode("24h"));
        assertEquals(TimeWindow.SEVEN_DAYS, TimeWindow.fromCode("7d"));
        assertEquals(TimeWindow.THIRTY_DAYS, TimeWindow.fromCode("30d"));
    }

    @Test
    void testFromCode_CaseInsensitive() {
        assertEquals(TimeWindow.FIVE_MINUTES, TimeWindow.fromCode("5M"));
        assertEquals(TimeWindow.ONE_HOUR, TimeWindow.fromCode("1H"));
        assertEquals(TimeWindow.SEVEN_DAYS, TimeWindow.fromCode("7D"));
    }

    @Test
    void testFromCode_WithWhitespace() {
        assertEquals(TimeWindow.FIFTEEN_MINUTES, TimeWindow.fromCode("  15m  "));
        assertEquals(TimeWindow.TWENTY_FOUR_HOURS, TimeWindow.fromCode(" 24h "));
    }

    @Test
    void testFromCode_InvalidCode() {
        assertThrows(IllegalArgumentException.class, () -> TimeWindow.fromCode("10m"));
        assertThrows(IllegalArgumentException.class, () -> TimeWindow.fromCode("2h"));
        assertThrows(IllegalArgumentException.class, () -> TimeWindow.fromCode("invalid"));
    }

    @Test
    void testFromCode_NullCode() {
        assertThrows(IllegalArgumentException.class, () -> TimeWindow.fromCode(null));
    }

    @Test
    void testGetDuration() {
        assertEquals(Duration.ofMinutes(5), TimeWindow.FIVE_MINUTES.getDuration());
        assertEquals(Duration.ofMinutes(15), TimeWindow.FIFTEEN_MINUTES.getDuration());
        assertEquals(Duration.ofHours(1), TimeWindow.ONE_HOUR.getDuration());
        assertEquals(Duration.ofHours(24), TimeWindow.TWENTY_FOUR_HOURS.getDuration());
        assertEquals(Duration.ofDays(7), TimeWindow.SEVEN_DAYS.getDuration());
        assertEquals(Duration.ofDays(30), TimeWindow.THIRTY_DAYS.getDuration());
    }

    @Test
    void testCalculateStartTime() {
        Instant now = Instant.parse("2025-11-11T12:00:00Z");

        // Test 5 minutes
        Instant fiveMinAgo = TimeWindow.FIVE_MINUTES.calculateStartTime(now);
        assertEquals(Instant.parse("2025-11-11T11:55:00Z"), fiveMinAgo);

        // Test 1 hour
        Instant oneHourAgo = TimeWindow.ONE_HOUR.calculateStartTime(now);
        assertEquals(Instant.parse("2025-11-11T11:00:00Z"), oneHourAgo);

        // Test 24 hours
        Instant dayAgo = TimeWindow.TWENTY_FOUR_HOURS.calculateStartTime(now);
        assertEquals(Instant.parse("2025-11-10T12:00:00Z"), dayAgo);

        // Test 7 days
        Instant weekAgo = TimeWindow.SEVEN_DAYS.calculateStartTime(now);
        assertEquals(Instant.parse("2025-11-04T12:00:00Z"), weekAgo);
    }

    @Test
    void testToString() {
        assertEquals("5m", TimeWindow.FIVE_MINUTES.toString());
        assertEquals("15m", TimeWindow.FIFTEEN_MINUTES.toString());
        assertEquals("1h", TimeWindow.ONE_HOUR.toString());
        assertEquals("24h", TimeWindow.TWENTY_FOUR_HOURS.toString());
        assertEquals("7d", TimeWindow.SEVEN_DAYS.toString());
        assertEquals("30d", TimeWindow.THIRTY_DAYS.toString());
    }

    @Test
    void testGetCode() {
        assertEquals("5m", TimeWindow.FIVE_MINUTES.getCode());
        assertEquals("15m", TimeWindow.FIFTEEN_MINUTES.getCode());
        assertEquals("1h", TimeWindow.ONE_HOUR.getCode());
        assertEquals("24h", TimeWindow.TWENTY_FOUR_HOURS.getCode());
        assertEquals("7d", TimeWindow.SEVEN_DAYS.getCode());
        assertEquals("30d", TimeWindow.THIRTY_DAYS.getCode());
    }
}
