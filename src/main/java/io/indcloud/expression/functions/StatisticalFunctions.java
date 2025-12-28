package io.indcloud.expression.functions;

import lombok.extern.slf4j.Slf4j;
import io.indcloud.expression.ContextualExpressionFunction;
import io.indcloud.expression.StatisticalFunctionContext;
import io.indcloud.expression.TimeWindow;
import io.indcloud.model.TelemetryRecord;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Statistical functions for time-series analysis in synthetic variables.
 * These functions query historical telemetry data over specified time windows.
 *
 * Supported functions:
 * - avg(variable, timeWindow): Average value over time window
 * - stddev(variable, timeWindow): Standard deviation over time window
 * - sum(variable, timeWindow): Sum of values over time window
 * - count(variable, timeWindow): Count of data points over time window
 * - minTime(variable, timeWindow): Minimum value over time window
 * - maxTime(variable, timeWindow): Maximum value over time window
 * - rate(variable, timeWindow): Rate of change per hour
 * - movingAvg(variable, timeWindow): Moving average
 */
@Slf4j
public class StatisticalFunctions {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * Calculate average value over time window.
     * Usage: avg("kwConsumption", "5m")
     */
    public static ContextualExpressionFunction avg() {
        return (context, args) -> {
            validateContext(context, "avg");
            validateArgCount(args, 2, "avg");

            String variableName = toString(args[0]);
            TimeWindow window = TimeWindow.fromCode(toString(args[1]));

            List<BigDecimal> values = fetchValues(context, variableName, window);

            if (values.isEmpty()) {
                log.debug("avg({}): No data found in time window {}", variableName, window);
                return BigDecimal.ZERO;
            }

            BigDecimal sum = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            return sum.divide(BigDecimal.valueOf(values.size()), MC);
        };
    }

    /**
     * Calculate standard deviation over time window.
     * Usage: stddev("voltage", "1h")
     */
    public static ContextualExpressionFunction stddev() {
        return (context, args) -> {
            validateContext(context, "stddev");
            validateArgCount(args, 2, "stddev");

            String variableName = toString(args[0]);
            TimeWindow window = TimeWindow.fromCode(toString(args[1]));

            List<BigDecimal> values = fetchValues(context, variableName, window);

            if (values.isEmpty() || values.size() == 1) {
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

            // Return standard deviation (square root of variance)
            return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        };
    }

    /**
     * Calculate sum over time window.
     * Usage: sum("kwConsumption", "24h")
     */
    public static ContextualExpressionFunction sum() {
        return (context, args) -> {
            validateContext(context, "sum");
            validateArgCount(args, 2, "sum");

            String variableName = toString(args[0]);
            TimeWindow window = TimeWindow.fromCode(toString(args[1]));

            List<BigDecimal> values = fetchValues(context, variableName, window);

            if (values.isEmpty()) {
                return BigDecimal.ZERO;
            }

            return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        };
    }

    /**
     * Count data points over time window.
     * Usage: count("voltage", "1h")
     */
    public static ContextualExpressionFunction count() {
        return (context, args) -> {
            validateContext(context, "count");
            validateArgCount(args, 2, "count");

            String variableName = toString(args[0]);
            TimeWindow window = TimeWindow.fromCode(toString(args[1]));

            List<BigDecimal> values = fetchValues(context, variableName, window);

            return BigDecimal.valueOf(values.size());
        };
    }

    /**
     * Find minimum value over time window.
     * Usage: minTime("voltage", "24h")
     */
    public static ContextualExpressionFunction minTime() {
        return (context, args) -> {
            validateContext(context, "minTime");
            validateArgCount(args, 2, "minTime");

            String variableName = toString(args[0]);
            TimeWindow window = TimeWindow.fromCode(toString(args[1]));

            List<BigDecimal> values = fetchValues(context, variableName, window);

            if (values.isEmpty()) {
                return BigDecimal.ZERO;
            }

            return values.stream()
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        };
    }

    /**
     * Find maximum value over time window.
     * Usage: maxTime("current", "7d")
     */
    public static ContextualExpressionFunction maxTime() {
        return (context, args) -> {
            validateContext(context, "maxTime");
            validateArgCount(args, 2, "maxTime");

            String variableName = toString(args[0]);
            TimeWindow window = TimeWindow.fromCode(toString(args[1]));

            List<BigDecimal> values = fetchValues(context, variableName, window);

            if (values.isEmpty()) {
                return BigDecimal.ZERO;
            }

            return values.stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        };
    }

    /**
     * Calculate rate of change per hour.
     * Usage: rate("kwConsumption", "1h")
     * Returns: (latest - oldest) / hours
     */
    public static ContextualExpressionFunction rate() {
        return (context, args) -> {
            validateContext(context, "rate");
            validateArgCount(args, 2, "rate");

            String variableName = toString(args[0]);
            TimeWindow window = TimeWindow.fromCode(toString(args[1]));

            List<BigDecimal> values = fetchValues(context, variableName, window);

            if (values.size() < 2) {
                return BigDecimal.ZERO;
            }

            // Calculate change
            BigDecimal first = values.get(0);
            BigDecimal last = values.get(values.size() - 1);
            BigDecimal change = last.subtract(first);

            // Calculate hours in time window
            long hours = window.getDuration().toHours();
            if (hours == 0) {
                hours = 1; // For windows less than 1 hour, normalize to per-hour rate
            }

            return change.divide(BigDecimal.valueOf(hours), MC);
        };
    }

    /**
     * Calculate moving average over time window.
     * Same as avg() but with clearer semantic meaning.
     * Usage: movingAvg("voltage", "15m")
     */
    public static ContextualExpressionFunction movingAvg() {
        return avg(); // Alias for avg with clearer naming
    }

    /**
     * Calculate percentage change over time window.
     * Usage: percentChange("kwConsumption", "1h")
     * Returns: ((latest - oldest) / oldest) * 100
     */
    public static ContextualExpressionFunction percentChange() {
        return (context, args) -> {
            validateContext(context, "percentChange");
            validateArgCount(args, 2, "percentChange");

            String variableName = toString(args[0]);
            TimeWindow window = TimeWindow.fromCode(toString(args[1]));

            List<BigDecimal> values = fetchValues(context, variableName, window);

            if (values.size() < 2) {
                return BigDecimal.ZERO;
            }

            BigDecimal first = values.get(0);
            BigDecimal last = values.get(values.size() - 1);

            if (first.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO; // Avoid division by zero
            }

            BigDecimal change = last.subtract(first);
            return change.divide(first, MC).multiply(BigDecimal.valueOf(100));
        };
    }

    /**
     * Calculate median value over time window.
     * Usage: median("voltage", "1h")
     */
    public static ContextualExpressionFunction median() {
        return (context, args) -> {
            validateContext(context, "median");
            validateArgCount(args, 2, "median");

            String variableName = toString(args[0]);
            TimeWindow window = TimeWindow.fromCode(toString(args[1]));

            List<BigDecimal> values = new java.util.ArrayList<>(fetchValues(context, variableName, window));

            if (values.isEmpty()) {
                return BigDecimal.ZERO;
            }

            values.sort(BigDecimal::compareTo);

            int size = values.size();
            if (size % 2 == 0) {
                // Even number: average of two middle values
                BigDecimal mid1 = values.get(size / 2 - 1);
                BigDecimal mid2 = values.get(size / 2);
                return mid1.add(mid2).divide(BigDecimal.valueOf(2), MC);
            } else {
                // Odd number: middle value
                return values.get(size / 2);
            }
        };
    }

    // Helper methods

    /**
     * Fetch telemetry values for a specific variable over a time window.
     * Includes all records up to and including the current timestamp.
     */
    private static List<BigDecimal> fetchValues(
            StatisticalFunctionContext context,
            String variableName,
            TimeWindow window) {

        Instant startTime = window.calculateStartTime(context.getCurrentTimestamp());
        // Include records up to and including the current timestamp
        // Add a small buffer to ensure the current record is included
        Instant endTime = context.getCurrentTimestamp().plusMillis(1);

        List<TelemetryRecord> records = context.getTelemetryRepository()
            .findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                context.getDeviceExternalId(),
                startTime,
                endTime
            );

        log.debug("fetchValues({}): startTime={}, endTime={}, found {} records",
                  variableName, startTime, endTime, records.size());

        // Map variable name to record field
        Function<TelemetryRecord, BigDecimal> extractor = getFieldExtractor(variableName);

        return records.stream()
            .map(extractor)
            .filter(value -> value != null && value.compareTo(BigDecimal.ZERO) != 0)
            .toList();
    }

    /**
     * Fetch telemetry values for a specific variable over a time window,
     * EXCLUDING the current record. Used for comparison-based functions
     * like anomaly detection where you want to compare current vs historical.
     */
    private static List<BigDecimal> fetchHistoricalValues(
            StatisticalFunctionContext context,
            String variableName,
            TimeWindow window) {

        Instant startTime = window.calculateStartTime(context.getCurrentTimestamp());
        // Exclude the current record by using a slightly earlier end time
        Instant endTime = context.getCurrentTimestamp().minusMillis(1);

        List<TelemetryRecord> records = context.getTelemetryRepository()
            .findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                context.getDeviceExternalId(),
                startTime,
                endTime
            );

        log.debug("fetchHistoricalValues({}): startTime={}, endTime={}, found {} records",
                  variableName, startTime, endTime, records.size());

        // Map variable name to record field
        Function<TelemetryRecord, BigDecimal> extractor = getFieldExtractor(variableName);

        return records.stream()
            .map(extractor)
            .filter(value -> value != null && value.compareTo(BigDecimal.ZERO) != 0)
            .toList();
    }

    /**
     * Get field extractor function for a variable name.
     */
    private static Function<TelemetryRecord, BigDecimal> getFieldExtractor(String variableName) {
        return switch (variableName.toLowerCase()) {
            case "kwconsumption", "kw_consumption" -> TelemetryRecord::getKwConsumption;
            case "voltage" -> TelemetryRecord::getVoltage;
            case "current" -> TelemetryRecord::getCurrent;
            case "powerfactor", "power_factor" -> TelemetryRecord::getPowerFactor;
            case "frequency" -> TelemetryRecord::getFrequency;
            default -> throw new IllegalArgumentException(
                "Unknown variable: " + variableName +
                ". Valid options: kwConsumption, voltage, current, powerFactor, frequency"
            );
        };
    }

    private static void validateContext(StatisticalFunctionContext context, String functionName) {
        if (context == null || !context.isAvailable()) {
            throw new IllegalStateException(
                functionName + ": Statistical context is required but not available. " +
                "This function can only be used in synthetic variables with device context."
            );
        }
    }

    private static void validateArgCount(Object[] args, int expected, String functionName) {
        if (args.length != expected) {
            throw new IllegalArgumentException(
                String.format("%s: expected %d argument(s), got %d",
                    functionName, expected, args.length)
            );
        }
    }

    private static String toString(Object arg) {
        if (arg == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }
        return arg.toString().trim();
    }
}
