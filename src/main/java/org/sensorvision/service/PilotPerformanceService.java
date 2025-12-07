package org.sensorvision.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for monitoring and optimizing performance during the pilot program
 */
@Service
@ConditionalOnProperty(name = "pilot.mode", havingValue = "true")
public class PilotPerformanceService {

    private static final Logger logger = LoggerFactory.getLogger(PilotPerformanceService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Get comprehensive performance metrics for pilot monitoring
     */
    @Cacheable(value = "analytics", key = "'performance-metrics'")
    public Map<String, Object> getPerformanceMetrics() {
        logger.debug("Collecting performance metrics for pilot program");
        
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Database performance metrics
            metrics.put("database", getDatabaseMetrics());
            
            // Redis performance metrics
            metrics.put("redis", getRedisMetrics());
            
            // Application performance metrics
            metrics.put("application", getApplicationMetrics());
            
            // Pilot-specific metrics
            metrics.put("pilot", getPilotSpecificMetrics());
            
            // System resource metrics
            metrics.put("system", getSystemMetrics());
            
        } catch (Exception e) {
            logger.error("Error collecting performance metrics: {}", e.getMessage(), e);
            metrics.put("error", "Failed to collect metrics: " + e.getMessage());
        }
        
        return metrics;
    }

    /**
     * Get database performance metrics
     */
    private Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> dbMetrics = new HashMap<>();
        
        try {
            // Connection pool metrics
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                dbMetrics.put("databaseProductName", metaData.getDatabaseProductName());
                dbMetrics.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            }
            
            // Active connections
            Integer activeConnections = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'", Integer.class);
            dbMetrics.put("activeConnections", activeConnections);
            
            // Database size
            Long databaseSize = jdbcTemplate.queryForObject(
                "SELECT pg_database_size(current_database())", Long.class);
            dbMetrics.put("databaseSizeMB", databaseSize != null ? databaseSize / 1024 / 1024 : 0);
            
            // Cache hit ratio
            Double cacheHitRatio = jdbcTemplate.queryForObject(
                "SELECT ROUND(100.0 * sum(blks_hit) / (sum(blks_hit) + sum(blks_read)), 2) " +
                "FROM pg_stat_database WHERE datname = current_database()", Double.class);
            dbMetrics.put("cacheHitRatio", cacheHitRatio);
            
            // Table sizes for key pilot tables
            Map<String, Long> tableSizes = new HashMap<>();
            List<Map<String, Object>> tableStats = jdbcTemplate.queryForList(
                "SELECT schemaname, tablename, pg_total_relation_size(schemaname||'.'||tablename) as size " +
                "FROM pg_tables WHERE schemaname = 'public' AND tablename IN " +
                "('telemetry_records', 'devices', 'users', 'organizations', 'pilot_feedback') " +
                "ORDER BY size DESC");
            
            for (Map<String, Object> row : tableStats) {
                String tableName = (String) row.get("tablename");
                Long size = (Long) row.get("size");
                tableSizes.put(tableName, size / 1024 / 1024); // Convert to MB
            }
            dbMetrics.put("tableSizesMB", tableSizes);
            
            // Query performance from pg_stat_statements (if available)
            try {
                List<Map<String, Object>> slowQueries = jdbcTemplate.queryForList(
                    "SELECT query, calls, total_exec_time, mean_exec_time " +
                    "FROM pg_stat_statements " +
                    "WHERE mean_exec_time > 100 " +
                    "ORDER BY mean_exec_time DESC LIMIT 5");
                dbMetrics.put("slowQueries", slowQueries);
            } catch (Exception e) {
                logger.debug("pg_stat_statements not available: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error collecting database metrics: {}", e.getMessage());
            dbMetrics.put("error", e.getMessage());
        }
        
        return dbMetrics;
    }

    /**
     * Get Redis performance metrics
     */
    private Map<String, Object> getRedisMetrics() {
        Map<String, Object> redisMetrics = new HashMap<>();
        
        try {
            // Redis info
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            
            // Memory usage
            String usedMemory = info.getProperty("used_memory_human");
            String maxMemory = info.getProperty("maxmemory_human");
            redisMetrics.put("usedMemory", usedMemory);
            redisMetrics.put("maxMemory", maxMemory);
            
            // Connected clients
            String connectedClients = info.getProperty("connected_clients");
            redisMetrics.put("connectedClients", connectedClients);
            
            // Operations per second
            String opsPerSec = info.getProperty("instantaneous_ops_per_sec");
            redisMetrics.put("operationsPerSecond", opsPerSec);
            
            // Keyspace info
            String keyspaceInfo = info.getProperty("db0");
            redisMetrics.put("keyspaceInfo", keyspaceInfo);
            
            // Cache hit ratio (approximation)
            String keyspaceHits = info.getProperty("keyspace_hits");
            String keyspaceMisses = info.getProperty("keyspace_misses");
            if (keyspaceHits != null && keyspaceMisses != null) {
                long hits = Long.parseLong(keyspaceHits);
                long misses = Long.parseLong(keyspaceMisses);
                double hitRatio = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;
                redisMetrics.put("cacheHitRatio", Math.round(hitRatio * 100.0) / 100.0);
            }
            
        } catch (Exception e) {
            logger.error("Error collecting Redis metrics: {}", e.getMessage());
            redisMetrics.put("error", e.getMessage());
        }
        
        return redisMetrics;
    }

    /**
     * Get application performance metrics
     */
    private Map<String, Object> getApplicationMetrics() {
        Map<String, Object> appMetrics = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // JVM memory metrics
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            appMetrics.put("jvmMemoryUsedMB", usedMemory / 1024 / 1024);
            appMetrics.put("jvmMemoryTotalMB", totalMemory / 1024 / 1024);
            appMetrics.put("jvmMemoryMaxMB", maxMemory / 1024 / 1024);
            appMetrics.put("jvmMemoryUsagePercent", 
                Math.round((double) usedMemory / maxMemory * 100 * 100.0) / 100.0);
            
            // Thread metrics
            appMetrics.put("activeThreads", Thread.activeCount());
            
            // Garbage collection metrics (simplified)
            appMetrics.put("availableProcessors", runtime.availableProcessors());
            
            // Application uptime (approximate)
            long uptimeMs = System.currentTimeMillis() - getApplicationStartTime();
            appMetrics.put("uptimeHours", uptimeMs / (1000 * 60 * 60));
            
        } catch (Exception e) {
            logger.error("Error collecting application metrics: {}", e.getMessage());
            appMetrics.put("error", e.getMessage());
        }
        
        return appMetrics;
    }

    /**
     * Get pilot-specific performance metrics
     */
    private Map<String, Object> getPilotSpecificMetrics() {
        Map<String, Object> pilotMetrics = new HashMap<>();
        
        try {
            // Telemetry ingestion rate (last hour)
            Integer telemetryLastHour = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM telemetry_records WHERE timestamp > NOW() - INTERVAL '1 hour'",
                Integer.class);
            pilotMetrics.put("telemetryIngestionsLastHour", telemetryLastHour);
            
            // Average telemetry processing time (from recent data)
            Double avgProcessingTime = jdbcTemplate.queryForObject(
                "SELECT AVG(EXTRACT(EPOCH FROM (created_at - timestamp))) " +
                "FROM telemetry_records WHERE created_at > NOW() - INTERVAL '1 hour'",
                Double.class);
            pilotMetrics.put("avgTelemetryProcessingTimeSeconds", 
                avgProcessingTime != null ? Math.round(avgProcessingTime * 1000.0) / 1000.0 : 0);
            
            // Active devices (sent data in last 24 hours)
            Integer activeDevices = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT device_id) FROM telemetry_records " +
                "WHERE timestamp > NOW() - INTERVAL '24 hours'", Integer.class);
            pilotMetrics.put("activeDevicesLast24h", activeDevices);
            
            // API calls per minute (from Redis if available)
            try {
                Set<String> apiCallKeys = redisTemplate.keys("api_calls:*");
                long totalApiCalls = 0;
                for (String key : apiCallKeys) {
                    Object value = redisTemplate.opsForValue().get(key);
                    if (value instanceof Number) {
                        totalApiCalls += ((Number) value).longValue();
                    }
                }
                pilotMetrics.put("totalApiCallsToday", totalApiCalls);
            } catch (Exception e) {
                logger.debug("Could not retrieve API call metrics from Redis: {}", e.getMessage());
            }
            
            // Recent alerts count
            Integer recentAlerts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE created_at > NOW() - INTERVAL '24 hours'",
                Integer.class);
            pilotMetrics.put("alertsLast24h", recentAlerts);
            
            // Dashboard load performance (average widgets per dashboard)
            Double avgWidgetsPerDashboard = jdbcTemplate.queryForObject(
                "SELECT AVG(widget_count) FROM (" +
                "SELECT dashboard_id, COUNT(*) as widget_count FROM widgets GROUP BY dashboard_id" +
                ") as widget_stats", Double.class);
            pilotMetrics.put("avgWidgetsPerDashboard", 
                avgWidgetsPerDashboard != null ? Math.round(avgWidgetsPerDashboard * 100.0) / 100.0 : 0);
            
        } catch (Exception e) {
            logger.error("Error collecting pilot-specific metrics: {}", e.getMessage());
            pilotMetrics.put("error", e.getMessage());
        }
        
        return pilotMetrics;
    }

    /**
     * Get system resource metrics
     */
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();
        
        try {
            // System load and CPU
            systemMetrics.put("availableProcessors", Runtime.getRuntime().availableProcessors());
            
            // System properties
            systemMetrics.put("javaVersion", System.getProperty("java.version"));
            systemMetrics.put("osName", System.getProperty("os.name"));
            systemMetrics.put("osVersion", System.getProperty("os.version"));
            
            // Current timestamp for metrics collection
            systemMetrics.put("metricsCollectedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error collecting system metrics: {}", e.getMessage());
            systemMetrics.put("error", e.getMessage());
        }
        
        return systemMetrics;
    }

    /**
     * Scheduled task to refresh materialized views for performance
     */
    @Scheduled(fixedRate = 900000) // Every 15 minutes
    public void refreshMaterializedViews() {
        try {
            logger.info("Refreshing materialized views for pilot performance optimization");
            
            // Refresh pilot program statistics
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY pilot_program_stats");
            
            // Refresh telemetry volume statistics
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY telemetry_volume_stats");
            
            logger.info("Successfully refreshed materialized views");
            
        } catch (Exception e) {
            logger.error("Error refreshing materialized views: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled task to cleanup old performance data
     */
    @Scheduled(cron = "0 2 * * *") // Daily at 2 AM
    public void cleanupOldData() {
        try {
            logger.info("Starting scheduled cleanup of old performance data");
            
            // Cleanup old telemetry data (respects 90-day pilot retention)
            Integer deletedRecords = jdbcTemplate.queryForObject(
                "SELECT cleanup_old_telemetry()", Integer.class);
            
            logger.info("Cleaned up {} old telemetry records", deletedRecords);
            
            // Cleanup old Redis keys
            cleanupOldRedisKeys();
            
        } catch (Exception e) {
            logger.error("Error during scheduled cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Get performance recommendations based on current metrics
     */
    public List<String> getPerformanceRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        try {
            Map<String, Object> metrics = getPerformanceMetrics();
            
            // Database recommendations
            @SuppressWarnings("unchecked")
            Map<String, Object> dbMetrics = (Map<String, Object>) metrics.get("database");
            if (dbMetrics != null) {
                Double cacheHitRatio = (Double) dbMetrics.get("cacheHitRatio");
                if (cacheHitRatio != null && cacheHitRatio < 95.0) {
                    recommendations.add("Database cache hit ratio is " + cacheHitRatio + 
                        "%. Consider increasing shared_buffers or adding more RAM.");
                }
                
                Integer activeConnections = (Integer) dbMetrics.get("activeConnections");
                if (activeConnections != null && activeConnections > 50) {
                    recommendations.add("High number of active database connections (" + 
                        activeConnections + "). Consider connection pooling optimization.");
                }
            }
            
            // Application recommendations
            @SuppressWarnings("unchecked")
            Map<String, Object> appMetrics = (Map<String, Object>) metrics.get("application");
            if (appMetrics != null) {
                Double memoryUsage = (Double) appMetrics.get("jvmMemoryUsagePercent");
                if (memoryUsage != null && memoryUsage > 85.0) {
                    recommendations.add("JVM memory usage is " + memoryUsage + 
                        "%. Consider increasing heap size or optimizing memory usage.");
                }
            }
            
            // Pilot-specific recommendations
            @SuppressWarnings("unchecked")
            Map<String, Object> pilotMetrics = (Map<String, Object>) metrics.get("pilot");
            if (pilotMetrics != null) {
                Integer telemetryRate = (Integer) pilotMetrics.get("telemetryIngestionsLastHour");
                if (telemetryRate != null && telemetryRate > 50000) {
                    recommendations.add("High telemetry ingestion rate (" + telemetryRate + 
                        "/hour). Consider implementing batching or rate limiting.");
                }
            }
            
            if (recommendations.isEmpty()) {
                recommendations.add("System performance is within optimal ranges.");
            }
            
        } catch (Exception e) {
            logger.error("Error generating performance recommendations: {}", e.getMessage());
            recommendations.add("Unable to generate recommendations due to metrics collection error.");
        }
        
        return recommendations;
    }

    /**
     * Cleanup old Redis keys to prevent memory bloat
     */
    private void cleanupOldRedisKeys() {
        try {
            // Cleanup old API call tracking keys (older than 2 days)
            Set<String> apiCallKeys = redisTemplate.keys("api_calls:*");
            for (String key : apiCallKeys) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (ttl != null && ttl < 0) { // Key without expiration
                    redisTemplate.delete(key);
                }
            }
            
            // Cleanup old telemetry point tracking keys
            Set<String> telemetryKeys = redisTemplate.keys("telemetry_points:*");
            for (String key : telemetryKeys) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (ttl != null && ttl < 0) {
                    redisTemplate.delete(key);
                }
            }
            
            logger.debug("Completed Redis key cleanup");
            
        } catch (Exception e) {
            logger.error("Error cleaning up Redis keys: {}", e.getMessage());
        }
    }

    /**
     * Get approximate application start time (for uptime calculation)
     */
    private long getApplicationStartTime() {
        // This is a simplified implementation
        // In a real application, you might store this in a static variable during startup
        return System.currentTimeMillis() - (Runtime.getRuntime().totalMemory() / 1024 / 1024);
    }
}