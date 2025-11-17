package org.sensorvision.demo.cache;

import java.util.List;
import java.util.Map;

/**
 * Rolling cache for storing recent telemetry data points in memory.
 *
 * This cache maintains a time-windowed view of telemetry data (e.g., last 5 minutes)
 * to provide fast access for real-time dashboard queries without hitting the database.
 *
 * Implementations should:
 * - Automatically evict old data outside the rolling window
 * - Be thread-safe for concurrent access
 * - Provide O(1) or near-O(1) write performance
 * - Support per-device data isolation
 */
public interface RollingTelemetryCache {

    /**
     * Add a telemetry point to the cache for a specific device.
     *
     * Old data points outside the rolling window will be automatically evicted.
     *
     * @param deviceId The device identifier
     * @param point    The telemetry data point
     */
    void addPoint(String deviceId, TelemetryPoint point);

    /**
     * Get all recent telemetry points for a device within the rolling window.
     *
     * @param deviceId The device identifier
     * @return List of telemetry points, ordered by timestamp (oldest first)
     */
    List<TelemetryPoint> getAllRecent(String deviceId);

    /**
     * Get the most recent telemetry point for a device.
     *
     * @param deviceId The device identifier
     * @return The latest telemetry point, or null if none exists
     */
    TelemetryPoint getLatest(String deviceId);

    /**
     * Clear all cached data for a specific device.
     *
     * @param deviceId The device identifier
     */
    void clear(String deviceId);

    /**
     * Clear all cached data for all devices.
     */
    void clearAll();

    /**
     * Get cache statistics (useful for monitoring and debugging).
     *
     * @return Map of statistic names to values
     */
    Map<String, Object> getStatistics();
}
