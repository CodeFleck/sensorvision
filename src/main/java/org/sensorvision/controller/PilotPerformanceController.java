package org.sensorvision.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.service.PilotPerformanceService;
import org.sensorvision.service.PilotTelemetryBatchProcessor;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for pilot program performance monitoring and optimization.
 * Provides endpoints for performance metrics, cache management, and system health.
 */
@RestController
@RequestMapping("/api/v1/pilot/performance")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('PILOT_ADMIN')")
public class PilotPerformanceController {

    private final PilotPerformanceService performanceService;
    private final PilotTelemetryBatchProcessor batchProcessor;
    private final CacheManager cacheManager;

    /**
     * Get comprehensive performance metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        log.debug("Retrieving pilot performance metrics");
        
        Map<String, Object> metrics = new HashMap<>();
        
        // Database performance metrics
        metrics.put("database", performanceService.getDatabaseMetrics());
        
        // Cache performance metrics
        metrics.put("cache", performanceService.getCacheMetrics());
        
        // JVM performance metrics
        metrics.put("jvm", performanceService.getJvmMetrics());
        
        // Telemetry processing metrics
        metrics.put("telemetry", performanceService.getTelemetryMetrics());
        
        // Batch processing statistics
        metrics.put("batchProcessing", batchProcessor.getBatchStatistics());
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get real-time system health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        log.debug("Checking pilot system health");
        
        Map<String, Object> health = performanceService.getSystemHealth();
        
        // Determine overall health status
        boolean isHealthy = (Boolean) health.getOrDefault("healthy", false);
        
        if (isHealthy) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Get cache statistics and hit ratios
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        log.debug("Retrieving cache statistics");
        
        Map<String, Object> cacheStats = new HashMap<>();
        
        // Get cache names and their statistics
        cacheManager.getCacheNames().forEach(cacheName -> {
            Map<String, Object> stats = performanceService.getCacheStatistics(cacheName);
            cacheStats.put(cacheName, stats);
        });
        
        return ResponseEntity.ok(cacheStats);
    }

    /**
     * Clear specific cache
     */
    @DeleteMapping("/cache/{cacheName}")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                performanceService.recordCacheClear(cacheName);
                
                Map<String, String> response = Map.of(
                    "status", "success",
                    "message", "Cache '" + cacheName + "' cleared successfully"
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> response = Map.of(
                    "status", "error",
                    "message", "Cache '" + cacheName + "' not found"
                );
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error clearing cache: {}", cacheName, e);
            Map<String, String> response = Map.of(
                "status", "error",
                "message", "Failed to clear cache: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Clear all caches
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        log.info("Clearing all caches");
        
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
            
            performanceService.recordCacheClear("all");
            
            Map<String, String> response = Map.of(
                "status", "success",
                "message", "All caches cleared successfully"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing all caches", e);
            Map<String, String> response = Map.of(
                "status", "error",
                "message", "Failed to clear caches: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Force flush pending telemetry batches
     */
    @PostMapping("/telemetry/flush")
    public ResponseEntity<Map<String, Object>> flushTelemetryBatches() {
        log.info("Flushing pending telemetry batches");
        
        try {
            var batchStats = batchProcessor.getBatchStatistics();
            batchProcessor.flushPendingRecords();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Telemetry batches flushed successfully");
            response.put("pendingRecords", batchStats.getPendingRecords());
            response.put("activeBatches", batchStats.getActiveBatches());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error flushing telemetry batches", e);
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to flush telemetry batches: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get performance optimization recommendations
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getPerformanceRecommendations() {
        log.debug("Generating performance recommendations");
        
        Map<String, Object> recommendations = performanceService.getPerformanceRecommendations();
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get slow query analysis
     */
    @GetMapping("/slow-queries")
    public ResponseEntity<Map<String, Object>> getSlowQueryAnalysis(
            @RequestParam(defaultValue = "24") int hours) {
        log.debug("Retrieving slow query analysis for last {} hours", hours);
        
        Map<String, Object> analysis = performanceService.getSlowQueryAnalysis(hours);
        
        return ResponseEntity.ok(analysis);
    }

    /**
     * Get performance trends over time
     */
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getPerformanceTrends(
            @RequestParam(defaultValue = "24") int hours) {
        log.debug("Retrieving performance trends for last {} hours", hours);
        
        Map<String, Object> trends = performanceService.getPerformanceTrends(hours);
        
        return ResponseEntity.ok(trends);
    }
}