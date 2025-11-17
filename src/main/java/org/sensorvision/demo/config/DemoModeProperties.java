package org.sensorvision.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Demo Mode.
 *
 * These properties control the behavior of the demo telemetry generator,
 * including device count, data generation frequency, and anomaly injection.
 *
 * Configure via application-demo.properties or environment variables.
 */
@Data
@Component
@ConfigurationProperties(prefix = "demo")
public class DemoModeProperties {

    /**
     * Enable/disable demo mode
     */
    private boolean modeEnabled = false;

    /**
     * Demo organization name (will be created if doesn't exist)
     */
    private String organizationName = "Demo Manufacturing Corp";

    /**
     * Number of demo devices to simulate
     */
    private int deviceCount = 3;

    /**
     * Device ID prefix (e.g., "demo-machine-" results in "demo-machine-01", "demo-machine-02", etc.)
     */
    private String devicePrefix = "demo-machine-";

    /**
     * Interval between telemetry generation (milliseconds)
     * Default: 500ms = 2 samples per second
     */
    private long generationIntervalMs = 500;

    /**
     * Probability of anomaly injection (0.0 to 1.0)
     * Default: 0.05 = 5% chance of anomaly
     */
    private double anomalyProbability = 0.05;

    /**
     * Temperature threshold for anomaly alerts (°C)
     */
    private double anomalyTemperatureThreshold = 85.0;

    /**
     * Vibration threshold for anomaly alerts (mm/s)
     */
    private double anomalyVibrationThreshold = 20.0;

    /**
     * Rolling cache window in minutes
     * Keeps the last N minutes of telemetry in memory for fast queries
     */
    private int rollingWindowMinutes = 5;

    // --- Baseline values for normal operation ---

    /**
     * Normal temperature range: min (°C)
     */
    private double temperatureMin = 58.0;

    /**
     * Normal temperature range: max (°C)
     */
    private double temperatureMax = 62.0;

    /**
     * Normal vibration range: min (mm/s)
     */
    private double vibrationMin = 4.0;

    /**
     * Normal vibration range: max (mm/s)
     */
    private double vibrationMax = 6.0;

    /**
     * Normal RPM range: min
     */
    private double rpmMin = 1450.0;

    /**
     * Normal RPM range: max
     */
    private double rpmMax = 1550.0;

    /**
     * Normal pressure range: min (bar)
     */
    private double pressureMin = 2.9;

    /**
     * Normal pressure range: max (bar)
     */
    private double pressureMax = 3.1;

    // --- Anomaly values ---

    /**
     * Anomalous temperature range: min (°C)
     */
    private double anomalyTemperatureMin = 85.0;

    /**
     * Anomalous temperature range: max (°C)
     */
    private double anomalyTemperatureMax = 95.0;

    /**
     * Anomalous vibration range: min (mm/s)
     */
    private double anomalyVibrationMin = 20.0;

    /**
     * Anomalous vibration range: max (mm/s)
     */
    private double anomalyVibrationMax = 30.0;

    /**
     * Get demo organization ID (computed from name)
     */
    public Long getDemoOrganizationId() {
        // This will be replaced by actual lookup in the service
        return null;
    }

    /**
     * Get formatted device ID for a given index
     */
    public String getDeviceId(int index) {
        return String.format("%s%02d", devicePrefix, index);
    }
}
