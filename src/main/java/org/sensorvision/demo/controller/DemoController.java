package org.sensorvision.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.demo.cache.RollingTelemetryCache;
import org.sensorvision.demo.config.DemoModeProperties;
import org.sensorvision.demo.service.DemoResetService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for Demo Mode operations.
 *
 * Provides endpoints for:
 * - Resetting demo data (POST /api/demo/reset)
 * - Checking demo status (GET /api/demo/status)
 * - Viewing cache statistics (GET /api/demo/cache/stats)
 * - Health check (GET /api/demo/health)
 *
 * All endpoints are only available when demo mode is enabled.
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo", name = "mode-enabled", havingValue = "true")
public class DemoController {

    private final DemoResetService resetService;
    private final DemoModeProperties properties;
    private final RollingTelemetryCache cache;

    /**
     * Reset all demo data to prepare for a new demonstration.
     *
     * DELETE /api/demo/reset
     *
     * Response:
     * {
     *   "telemetry_records_deleted": 1234,
     *   "alerts_deleted": 5,
     *   "devices_retained": 3,
     *   "message": "Demo reset successful"
     * }
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetDemo() {
        log.info("Demo reset requested via API");

        DemoResetService.DemoResetResult result = resetService.reset();

        Map<String, Object> response = new HashMap<>();
        response.put("telemetry_records_deleted", result.telemetryRecordsDeleted());
        response.put("alerts_deleted", result.alertsDeleted());
        response.put("devices_retained", result.devicesRetained());
        response.put("message", result.message());

        return ResponseEntity.ok(response);
    }

    /**
     * Get current demo mode status and configuration.
     *
     * GET /api/demo/status
     *
     * Response:
     * {
     *   "demo_mode_enabled": true,
     *   "organization_name": "Demo Manufacturing Corp",
     *   "device_count": 3,
     *   "generation_interval_ms": 500,
     *   "anomaly_probability": 0.05,
     *   "rolling_window_minutes": 5
     * }
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("demo_mode_enabled", properties.isModeEnabled());
        status.put("organization_name", properties.getOrganizationName());
        status.put("device_count", properties.getDeviceCount());
        status.put("device_prefix", properties.getDevicePrefix());
        status.put("generation_interval_ms", properties.getGenerationIntervalMs());
        status.put("samples_per_second", 1000.0 / properties.getGenerationIntervalMs());
        status.put("anomaly_probability", properties.getAnomalyProbability());
        status.put("anomaly_rate_percent", properties.getAnomalyProbability() * 100);
        status.put("rolling_window_minutes", properties.getRollingWindowMinutes());

        // Threshold values
        Map<String, Object> thresholds = new HashMap<>();
        thresholds.put("temperature_threshold", properties.getAnomalyTemperatureThreshold());
        thresholds.put("vibration_threshold", properties.getAnomalyVibrationThreshold());
        status.put("thresholds", thresholds);

        // Normal ranges
        Map<String, Object> normalRanges = new HashMap<>();
        normalRanges.put("temperature", Map.of("min", properties.getTemperatureMin(), "max", properties.getTemperatureMax()));
        normalRanges.put("vibration", Map.of("min", properties.getVibrationMin(), "max", properties.getVibrationMax()));
        normalRanges.put("rpm", Map.of("min", properties.getRpmMin(), "max", properties.getRpmMax()));
        normalRanges.put("pressure", Map.of("min", properties.getPressureMin(), "max", properties.getPressureMax()));
        status.put("normal_ranges", normalRanges);

        return ResponseEntity.ok(status);
    }

    /**
     * Get rolling cache statistics.
     *
     * GET /api/demo/cache/stats
     *
     * Response:
     * {
     *   "total_points": 1800,
     *   "device_count": 3,
     *   "avg_points_per_device": 600,
     *   "window_minutes": 5,
     *   "estimated_memory_kb": 1800,
     *   "estimated_memory_mb": 1.76
     * }
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = cache.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint for demo mode.
     *
     * GET /api/demo/health
     *
     * Response:
     * {
     *   "status": "healthy",
     *   "demo_mode": "active"
     * }
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("demo_mode", "active");
        health.put("organization", properties.getOrganizationName());
        return ResponseEntity.ok(health);
    }
}
