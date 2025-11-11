package org.sensorvision.model;

/**
 * Enum for fleet aggregation functions in global rules
 */
public enum FleetAggregationFunction {
    // Count-based operations
    COUNT_DEVICES("countDevices"),
    COUNT_ONLINE("countOnline"),
    COUNT_OFFLINE("countOffline"),
    COUNT_ALERTING("countAlerting"),
    PERCENT_ONLINE("percentOnline"),
    PERCENT_OFFLINE("percentOffline"),

    // Metric-based operations
    SUM("sum"),
    AVG("avg"),
    MIN("min"),
    MAX("max"),
    STDDEV("stddev"),
    PERCENTILE("percentile"),

    // Time-window operations
    COUNT_DEVICES_WHERE("countDevicesWhere"),
    AVG_UPTIME_HOURS("avgUptimeHours");

    private final String functionName;

    FleetAggregationFunction(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public static FleetAggregationFunction fromString(String functionName) {
        for (FleetAggregationFunction func : values()) {
            if (func.functionName.equalsIgnoreCase(functionName)) {
                return func;
            }
        }
        throw new IllegalArgumentException("Unknown aggregation function: " + functionName);
    }

    public boolean requiresVariable() {
        return this == SUM || this == AVG || this == MIN || this == MAX ||
               this == STDDEV || this == PERCENTILE;
    }
}
