package org.sensorvision.demo.cache;

import lombok.extern.slf4j.Slf4j;
import org.sensorvision.demo.config.DemoModeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of RollingTelemetryCache.
 *
 * Stores telemetry data points in memory with automatic eviction of old data.
 * Thread-safe and optimized for high-frequency writes and reads.
 *
 * Memory usage: ~1KB per telemetry point
 * For 3 devices at 2 samples/sec with 5-min window: ~1.8MB total
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "demo", name = "mode-enabled", havingValue = "true")
public class InMemoryRollingTelemetryCache implements RollingTelemetryCache {

    private final DemoModeProperties properties;
    private final Map<String, List<TelemetryPoint>> deviceCache;

    public InMemoryRollingTelemetryCache(DemoModeProperties properties) {
        this.properties = properties;
        this.deviceCache = new ConcurrentHashMap<>();
        log.info("Initialized in-memory rolling telemetry cache (window: {} minutes)",
                properties.getRollingWindowMinutes());
    }

    @Override
    public void addPoint(String deviceId, TelemetryPoint point) {
        deviceCache.compute(deviceId, (key, points) -> {
            if (points == null) {
                points = Collections.synchronizedList(new ArrayList<>());
            }

            // Add new point
            points.add(point);

            // Evict old points outside the rolling window
            Instant cutoff = Instant.now().minus(properties.getRollingWindowMinutes(), ChronoUnit.MINUTES);
            points.removeIf(p -> p.timestamp().isBefore(cutoff));

            return points;
        });
    }

    @Override
    public List<TelemetryPoint> getAllRecent(String deviceId) {
        List<TelemetryPoint> points = deviceCache.get(deviceId);
        if (points == null || points.isEmpty()) {
            return Collections.emptyList();
        }

        // Return defensive copy, sorted by timestamp
        synchronized (points) {
            return points.stream()
                    .sorted(Comparator.comparing(TelemetryPoint::timestamp))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public TelemetryPoint getLatest(String deviceId) {
        List<TelemetryPoint> points = deviceCache.get(deviceId);
        if (points == null || points.isEmpty()) {
            return null;
        }

        synchronized (points) {
            return points.stream()
                    .max(Comparator.comparing(TelemetryPoint::timestamp))
                    .orElse(null);
        }
    }

    @Override
    public void clear(String deviceId) {
        deviceCache.remove(deviceId);
        log.debug("Cleared cache for device: {}", deviceId);
    }

    @Override
    public void clearAll() {
        int deviceCount = deviceCache.size();
        int totalPoints = deviceCache.values().stream()
                .mapToInt(List::size)
                .sum();

        deviceCache.clear();
        log.info("Cleared cache: {} devices, {} total points", deviceCount, totalPoints);
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        int totalPoints = deviceCache.values().stream()
                .mapToInt(List::size)
                .sum();

        double avgPointsPerDevice = deviceCache.isEmpty() ? 0 :
                (double) totalPoints / deviceCache.size();

        stats.put("total_points", totalPoints);
        stats.put("device_count", deviceCache.size());
        stats.put("avg_points_per_device", Math.round(avgPointsPerDevice * 100.0) / 100.0);
        stats.put("window_minutes", properties.getRollingWindowMinutes());

        // Estimate memory usage (rough approximation: 1KB per point)
        long estimatedMemoryKB = totalPoints;
        stats.put("estimated_memory_kb", estimatedMemoryKB);
        stats.put("estimated_memory_mb", Math.round(estimatedMemoryKB / 1024.0 * 100.0) / 100.0);

        return stats;
    }
}
