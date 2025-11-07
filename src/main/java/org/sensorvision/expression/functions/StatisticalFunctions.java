package org.sensorvision.expression.functions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.expression.TimeWindow;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Statistical functions for synthetic variable expressions.
 * These functions operate on historical telemetry data within time windows.
 *
 * Context requirements:
 * - deviceId: The device external ID for historical queries
 * - timestamp: The reference timestamp for time window calculations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticalFunctions {

    private final TelemetryRecordRepository telemetryRepository;
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * Calculate average (mean) of a variable over a time window.
     * Usage: avg(variableName, timeWindow)
     * Example: avg("kwConsumption", "1h") - average consumption in last hour
     *
     * @param args [0] = variable name (String), [1] = time window (String), [2] = deviceId (String), [3] = timestamp (Instant)
     */
    public BigDecimal avg(Object... args) {
        validateTimeWindowArgs(args, "avg");
        String variableName = (String) args[0];
        TimeWindow window = TimeWindow.parse((String) args[1]);
        String deviceId = (String) args[2];
        Instant timestamp = (Instant) args[3];

        List<BigDecimal> values = getHistoricalValues(deviceId, timestamp, window, variableName);
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), MC);
    }

    /**
     * Calculate standard deviation of a variable over a time window.
     * Usage: stddev(variableName, timeWindow)
     * Example: stddev("temperature", "24h") - temperature variability in last 24 hours
     */
    public BigDecimal stddev(Object... args) {
        validateTimeWindowArgs(args, "stddev");
        String variableName = (String) args[0];
        TimeWindow window = TimeWindow.parse((String) args[1]);
        String deviceId = (String) args[2];
        Instant timestamp = (Instant) args[3];

        List<BigDecimal> values = getHistoricalValues(deviceId, timestamp, window, variableName);
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }

        // Calculate mean
        BigDecimal mean = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), MC);

        // Calculate variance
        BigDecimal variance = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), MC);

        // Standard deviation is sqrt of variance
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    /**
     * Calculate percentile of a variable over a time window.
     * Usage: percentile(variableName, percentile, timeWindow)
     * Example: percentile("kwConsumption", 95, "7d") - 95th percentile consumption in last 7 days
     */
    public BigDecimal percentile(Object... args) {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                "percentile: expected 5 arguments (variable, percentile, timeWindow, deviceId, timestamp), got " + args.length
            );
        }

        String variableName = (String) args[0];
        double percentile = toBigDecimal(args[1]).doubleValue();
        TimeWindow window = TimeWindow.parse((String) args[2]);
        String deviceId = (String) args[3];
        Instant timestamp = (Instant) args[4];

        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException("percentile: value must be between 0 and 100");
        }

        List<BigDecimal> values = getHistoricalValues(deviceId, timestamp, window, variableName);
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Collections.sort(values);
        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        index = Math.max(0, Math.min(index, values.size() - 1));
        return values.get(index);
    }

    /**
     * Calculate rate of change (derivative) of a variable over a time window.
     * Usage: rateOfChange(variableName, timeWindow)
     * Example: rateOfChange("temperature", "15m") - temperature change per minute in last 15 minutes
     */
    public BigDecimal rateOfChange(Object... args) {
        validateTimeWindowArgs(args, "rateOfChange");
        String variableName = (String) args[0];
        TimeWindow window = TimeWindow.parse((String) args[1]);
        String deviceId = (String) args[2];
        Instant timestamp = (Instant) args[3];

        List<TelemetryRecord> records = getHistoricalRecords(deviceId, timestamp, window);
        if (records.size() < 2) {
            return BigDecimal.ZERO;
        }

        TelemetryRecord first = records.get(0);
        TelemetryRecord last = records.get(records.size() - 1);

        BigDecimal firstValue = extractValue(first, variableName);
        BigDecimal lastValue = extractValue(last, variableName);

        long timeDiffSeconds = last.getTimestamp().getEpochSecond() - first.getTimestamp().getEpochSecond();
        if (timeDiffSeconds == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal valueDiff = lastValue.subtract(firstValue);
        return valueDiff.divide(BigDecimal.valueOf(timeDiffSeconds), MC);
    }

    /**
     * Calculate sum of a variable over a time window.
     * Usage: sum(variableName, timeWindow)
     * Example: sum("kwConsumption", "24h") - total consumption in last 24 hours
     */
    public BigDecimal sum(Object... args) {
        validateTimeWindowArgs(args, "sum");
        String variableName = (String) args[0];
        TimeWindow window = TimeWindow.parse((String) args[1]);
        String deviceId = (String) args[2];
        Instant timestamp = (Instant) args[3];

        List<BigDecimal> values = getHistoricalValues(deviceId, timestamp, window, variableName);
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Count number of data points in a time window.
     * Usage: count(variableName, timeWindow)
     * Example: count("kwConsumption", "1h") - number of readings in last hour
     */
    public BigDecimal count(Object... args) {
        validateTimeWindowArgs(args, "count");
        String variableName = (String) args[0];
        TimeWindow window = TimeWindow.parse((String) args[1]);
        String deviceId = (String) args[2];
        Instant timestamp = (Instant) args[3];

        List<BigDecimal> values = getHistoricalValues(deviceId, timestamp, window, variableName);
        return BigDecimal.valueOf(values.size());
    }

    /**
     * Get minimum value in a time window.
     * Usage: minWindow(variableName, timeWindow)
     * Example: minWindow("voltage", "24h") - minimum voltage in last 24 hours
     */
    public BigDecimal minWindow(Object... args) {
        validateTimeWindowArgs(args, "minWindow");
        String variableName = (String) args[0];
        TimeWindow window = TimeWindow.parse((String) args[1]);
        String deviceId = (String) args[2];
        Instant timestamp = (Instant) args[3];

        List<BigDecimal> values = getHistoricalValues(deviceId, timestamp, window, variableName);
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return Collections.min(values);
    }

    /**
     * Get maximum value in a time window.
     * Usage: maxWindow(variableName, timeWindow)
     * Example: maxWindow("temperature", "7d") - maximum temperature in last 7 days
     */
    public BigDecimal maxWindow(Object... args) {
        validateTimeWindowArgs(args, "maxWindow");
        String variableName = (String) args[0];
        TimeWindow window = TimeWindow.parse((String) args[1]);
        String deviceId = (String) args[2];
        Instant timestamp = (Instant) args[3];

        List<BigDecimal> values = getHistoricalValues(deviceId, timestamp, window, variableName);
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return Collections.max(values);
    }

    /**
     * Get the difference (delta) from the previous value.
     * Usage: delta(variableName)
     * Example: delta("kwConsumption") - change in consumption from last reading
     */
    public BigDecimal delta(Object... args) {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                "delta: expected 3 arguments (variable, deviceId, timestamp), got " + args.length
            );
        }

        String variableName = (String) args[0];
        String deviceId = (String) args[1];
        Instant timestamp = (Instant) args[2];

        // Get last 2 records
        List<TelemetryRecord> records = telemetryRepository
                .findByDeviceExternalIdAndTimestampBeforeOrderByTimestampDesc(
                        deviceId,
                        timestamp,
                        PageRequest.of(0, 2)
                );

        if (records.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentValue = extractValue(records.get(0), variableName);
        BigDecimal previousValue = extractValue(records.get(1), variableName);

        return currentValue.subtract(previousValue);
    }

    // Helper methods

    private void validateTimeWindowArgs(Object[] args, String functionName) {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                functionName + ": expected 4 arguments (variable, timeWindow, deviceId, timestamp), got " + args.length
            );
        }
    }

    private List<TelemetryRecord> getHistoricalRecords(String deviceId, Instant timestamp, TimeWindow window) {
        Instant startTime = window.getStartTime(timestamp);
        return telemetryRepository.findByDeviceExternalIdAndTimestampBetween(deviceId, startTime, timestamp);
    }

    private List<BigDecimal> getHistoricalValues(String deviceId, Instant timestamp, TimeWindow window, String variableName) {
        List<TelemetryRecord> records = getHistoricalRecords(deviceId, timestamp, window);
        List<BigDecimal> values = new ArrayList<>();

        for (TelemetryRecord record : records) {
            BigDecimal value = extractValue(record, variableName);
            if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
                values.add(value);
            }
        }

        return values;
    }

    private BigDecimal extractValue(TelemetryRecord record, String variableName) {
        return switch (variableName.toLowerCase()) {
            case "kwconsumption" -> record.getKwConsumption();
            case "voltage" -> record.getVoltage();
            case "current" -> record.getCurrent();
            case "powerfactor" -> record.getPowerFactor();
            case "frequency" -> record.getFrequency();
            default -> {
                log.warn("Unknown variable name for statistical function: {}", variableName);
                yield BigDecimal.ZERO;
            }
        };
    }

    private BigDecimal toBigDecimal(Object arg) {
        if (arg instanceof BigDecimal) {
            return (BigDecimal) arg;
        } else if (arg instanceof Number) {
            return BigDecimal.valueOf(((Number) arg).doubleValue());
        } else if (arg instanceof String) {
            try {
                return new BigDecimal((String) arg);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + arg + "' to number");
            }
        } else {
            throw new IllegalArgumentException("Cannot convert " + arg.getClass().getSimpleName() + " to BigDecimal");
        }
    }
}
