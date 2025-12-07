package org.sensorvision.service;

import org.sensorvision.dto.LatestTelemetryResponse;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Performance-optimized telemetry service for pilot program
 * Implements caching, batching, and async processing for high-throughput scenarios
 */
@Service
@ConditionalOnProperty(name = "pilot.mode", havingValue = "true")
public class PilotTelemetryService {

    private static final Logger logger = LoggerFactory.getLogger(PilotTelemetryService.class);

    @Autowired
    private TelemetryRecordRepository telemetryRecordRepository;

    @Autowired
    private TelemetryService telemetryService;

    @Autowired(required = false)
    private PilotQuotaService pilotQuotaService;

    /**
     * Get latest telemetry with caching for improved dashboard performance
     */
    @Cacheable(value = "telemetry-latest", key = "#deviceId")
    public LatestTelemetryResponse getCachedLatestTelemetry(String deviceId) {
        logger.debug("Fetching latest telemetry from database (cache miss): {}", deviceId);
        return telemetryService.getLatestTelemetry(deviceId);
    }

    /**
     * Get latest telemetry for multiple devices with batch optimization
     */
    @Cacheable(value = "telemetry-latest", key = "'batch:' + #deviceIds.hashCode()")
    public Map<String, LatestTelemetryResponse> getCachedLatestTelemetryBatch(List<String> deviceIds) {
        logger.debug("Fetching latest telemetry batch from database (cache miss): {} devices", deviceIds.size());
        
        Map<String, LatestTelemetryResponse> results = new HashMap<>();
        
        // Batch query for better performance
        List<TelemetryRecord> latestRecords = telemetryRecordRepository.findLatestByDeviceIds(deviceIds);
        
        // Group by device ID and convert to response
        Map<String, List<TelemetryRecord>> groupedByDevice = latestRecords.stream()
                .collect(Collectors.groupingBy(TelemetryRecord::getDeviceId));
        
        for (String deviceId : deviceIds) {
            List<TelemetryRecord> deviceRecords = groupedByDevice.getOrDefault(deviceId, Collections.emptyList());
            if (!deviceRecords.isEmpty()) {
                results.put(deviceId, convertToLatestTelemetryResponse(deviceRecords));
            }
        }
        
        return results;
    }

    /**
     * Get telemetry data with time range and caching
     */
    @Cacheable(value = "analytics", key = "#deviceId + ':' + #hours + 'h'")
    public List<TelemetryRecord> getCachedTelemetryByTimeRange(String deviceId, int hours) {
        logger.debug("Fetching telemetry time range from database (cache miss): {} ({}h)", deviceId, hours);
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return telemetryRecordRepository.findByDeviceIdAndTimestampAfterOrderByTimestampDesc(deviceId, since);
    }

    /**
     * Process telemetry ingestion with quota validation and async processing
     */
    @Async("telemetryExecutor")
    public CompletableFuture<Void> processIngestedTelemetryAsync(
            String organizationId, String deviceId, Map<String, Object> variables) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Validate telemetry quota if pilot quota service is available
                if (pilotQuotaService != null) {
                    pilotQuotaService.validateTelemetryIngestion(organizationId, variables.size());
                }
                
                // Process telemetry through main service
                telemetryService.processTelemetryData(deviceId, variables);
                
                // Evict relevant caches
                evictTelemetryCaches(deviceId);
                
                logger.debug("Async telemetry processing completed for device: {}", deviceId);
                
            } catch (Exception e) {
                logger.error("Error in async telemetry processing for device {}: {}", deviceId, e.getMessage(), e);
            }
        });
    }

    /**
     * Get aggregated telemetry statistics for pilot monitoring
     */
    @Cacheable(value = "analytics", key = "'telemetry-stats:' + #organizationId + ':' + #hours")
    public Map<String, Object> getTelemetryStatistics(String organizationId, int hours) {
        logger.debug("Calculating telemetry statistics (cache miss): {} ({}h)", organizationId, hours);
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        
        // Get telemetry volume statistics
        List<Object[]> volumeStats = telemetryRecordRepository.getTelemetryVolumeByOrganization(organizationId, since);
        
        Map<String, Object> stats = new HashMap<>();
        
        if (!volumeStats.isEmpty()) {
            Object[] row = volumeStats.get(0);
            stats.put("totalMessages", row[0]);
            stats.put("uniqueDevices", row[1]);
            stats.put("uniqueVariables", row[2]);
            stats.put("avgMessagesPerDevice", row[3]);
            stats.put("timeRange", hours + " hours");
        } else {
            stats.put("totalMessages", 0);
            stats.put("uniqueDevices", 0);
            stats.put("uniqueVariables", 0);
            stats.put("avgMessagesPerDevice", 0);
            stats.put("timeRange", hours + " hours");
        }
        
        // Get top variables by volume
        List<Object[]> topVariables = telemetryRecordRepository.getTopVariablesByVolume(organizationId, since, 10);
        List<Map<String, Object>> topVariablesList = topVariables.stream()
                .map(row -> Map.of(
                        "variableName", row[0],
                        "messageCount", row[1],
                        "avgValue", row[2] != null ? row[2] : 0
                ))
                .collect(Collectors.toList());
        stats.put("topVariables", topVariablesList);
        
        stats.put("lastCalculated", LocalDateTime.now());
        
        return stats;
    }

    /**
     * Get telemetry ingestion rate for performance monitoring
     */
    @Cacheable(value = "analytics", key = "'ingestion-rate:' + #minutes")
    public Map<String, Object> getTelemetryIngestionRate(int minutes) {
        logger.debug("Calculating telemetry ingestion rate (cache miss): {} minutes", minutes);
        
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        
        // Get ingestion rate by minute
        List<Object[]> rateData = telemetryRecordRepository.getTelemetryIngestionRate(since);
        
        List<Map<String, Object>> rateByMinute = rateData.stream()
                .map(row -> Map.of(
                        "minute", row[0],
                        "messageCount", row[1],
                        "deviceCount", row[2]
                ))
                .collect(Collectors.toList());
        
        // Calculate average rate
        double avgRate = rateData.stream()
                .mapToLong(row -> (Long) row[1])
                .average()
                .orElse(0.0);
        
        // Calculate peak rate
        long peakRate = rateData.stream()
                .mapToLong(row -> (Long) row[1])
                .max()
                .orElse(0L);
        
        Map<String, Object> result = Map.of(
                "timeRangeMinutes", minutes,
                "averageRatePerMinute", Math.round(avgRate * 100.0) / 100.0,
                "peakRatePerMinute", peakRate,
                "rateByMinute", rateByMinute,
                "lastCalculated", LocalDateTime.now()
        );
        
        return result;
    }

    /**
     * Batch process multiple telemetry records for better performance
     */
    @Async("telemetryExecutor")
    public CompletableFuture<Void> processTelemetryBatch(List<Map<String, Object>> telemetryBatch) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("Processing telemetry batch of {} records", telemetryBatch.size());
                
                // Group by organization for quota validation
                Map<String, List<Map<String, Object>>> byOrganization = telemetryBatch.stream()
                        .collect(Collectors.groupingBy(record -> (String) record.get("organizationId")));
                
                // Validate quotas for each organization
                if (pilotQuotaService != null) {
                    for (Map.Entry<String, List<Map<String, Object>>> entry : byOrganization.entrySet()) {
                        String orgId = entry.getKey();
                        int pointCount = entry.getValue().stream()
                                .mapToInt(record -> ((Map<String, Object>) record.get("variables")).size())
                                .sum();
                        
                        pilotQuotaService.validateTelemetryIngestion(orgId, pointCount);
                    }
                }
                
                // Process each record
                for (Map<String, Object> record : telemetryBatch) {
                    String deviceId = (String) record.get("deviceId");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> variables = (Map<String, Object>) record.get("variables");
                    
                    telemetryService.processTelemetryData(deviceId, variables);
                }
                
                // Evict caches for affected devices
                Set<String> affectedDevices = telemetryBatch.stream()
                        .map(record -> (String) record.get("deviceId"))
                        .collect(Collectors.toSet());
                
                for (String deviceId : affectedDevices) {
                    evictTelemetryCaches(deviceId);
                }
                
                logger.info("Successfully processed telemetry batch of {} records", telemetryBatch.size());
                
            } catch (Exception e) {
                logger.error("Error processing telemetry batch: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Get telemetry performance metrics for monitoring
     */
    @Cacheable(value = "analytics", key = "'telemetry-performance'")
    public Map<String, Object> getTelemetryPerformanceMetrics() {
        logger.debug("Calculating telemetry performance metrics (cache miss)");
        
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Ingestion volume (last 24 hours)
            Integer volume24h = telemetryRecordRepository.countByTimestampAfter(
                    LocalDateTime.now().minusHours(24));
            metrics.put("ingestionsLast24h", volume24h);
            
            // Average ingestion rate per hour
            double avgPerHour = volume24h != null ? volume24h / 24.0 : 0;
            metrics.put("avgIngestionsPerHour", Math.round(avgPerHour * 100.0) / 100.0);
            
            // Peak ingestion hour (last 24 hours)
            List<Object[]> hourlyStats = telemetryRecordRepository.getHourlyIngestionStats(
                    LocalDateTime.now().minusHours(24));
            
            if (!hourlyStats.isEmpty()) {
                Object[] peakHour = hourlyStats.stream()
                        .max(Comparator.comparing(row -> (Long) row[1]))
                        .orElse(new Object[]{null, 0L});
                
                metrics.put("peakIngestionHour", peakHour[0]);
                metrics.put("peakIngestionCount", peakHour[1]);
            }
            
            // Data distribution by variable type
            List<Object[]> variableStats = telemetryRecordRepository.getVariableDistribution(
                    LocalDateTime.now().minusHours(24));
            
            Map<String, Long> variableDistribution = variableStats.stream()
                    .collect(Collectors.toMap(
                            row -> (String) row[0],
                            row -> (Long) row[1]
                    ));
            metrics.put("variableDistribution", variableDistribution);
            
            // Processing latency (average time from timestamp to created_at)
            Double avgLatency = telemetryRecordRepository.getAverageProcessingLatency(
                    LocalDateTime.now().minusHours(1));
            metrics.put("avgProcessingLatencySeconds", 
                    avgLatency != null ? Math.round(avgLatency * 1000.0) / 1000.0 : 0);
            
        } catch (Exception e) {
            logger.error("Error calculating telemetry performance metrics: {}", e.getMessage());
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }

    /**
     * Evict telemetry-related caches for a device
     */
    @CacheEvict(value = {"telemetry-latest", "analytics"}, allEntries = true)
    public void evictTelemetryCaches(String deviceId) {
        logger.debug("Evicting telemetry caches for device: {}", deviceId);
    }

    /**
     * Warm up telemetry cache for frequently accessed devices
     */
    public void warmUpTelemetryCache(List<String> deviceIds) {
        logger.info("Warming up telemetry cache for {} devices", deviceIds.size());
        
        // Warm up latest telemetry cache
        for (String deviceId : deviceIds) {
            try {
                getCachedLatestTelemetry(deviceId);
            } catch (Exception e) {
                logger.warn("Failed to warm up cache for device {}: {}", deviceId, e.getMessage());
            }
        }
        
        // Warm up batch cache
        try {
            getCachedLatestTelemetryBatch(deviceIds);
        } catch (Exception e) {
            logger.warn("Failed to warm up batch cache: {}", e.getMessage());
        }
    }

    /**
     * Convert telemetry records to latest telemetry response
     */
    private LatestTelemetryResponse convertToLatestTelemetryResponse(List<TelemetryRecord> records) {
        if (records.isEmpty()) {
            return null;
        }
        
        // Group by variable name and get the latest value for each
        Map<String, Object> latestValues = new HashMap<>();
        LocalDateTime latestTimestamp = null;
        
        for (TelemetryRecord record : records) {
            if (latestTimestamp == null || record.getTimestamp().isAfter(latestTimestamp)) {
                latestTimestamp = record.getTimestamp();
            }
            
            // Use the most recent value for each variable
            if (!latestValues.containsKey(record.getVariableName()) ||
                record.getTimestamp().isAfter(getTimestampForVariable(records, record.getVariableName()))) {
                
                Object value = record.getNumericValue() != null ? 
                        record.getNumericValue() : record.getStringValue();
                latestValues.put(record.getVariableName(), value);
            }
        }
        
        return LatestTelemetryResponse.builder()
                .deviceId(records.get(0).getDeviceId())
                .timestamp(latestTimestamp)
                .variables(latestValues)
                .build();
    }

    /**
     * Get timestamp for a specific variable from telemetry records
     */
    private LocalDateTime getTimestampForVariable(List<TelemetryRecord> records, String variableName) {
        return records.stream()
                .filter(r -> variableName.equals(r.getVariableName()))
                .map(TelemetryRecord::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.MIN);
    }

    /**
     * Get telemetry statistics for pilot program monitoring
     */
    @Cacheable(value = "analytics", key = "'pilot-telemetry-overview'")
    public Map<String, Object> getPilotTelemetryOverview() {
        logger.debug("Calculating pilot telemetry overview (cache miss)");
        
        Map<String, Object> overview = new HashMap<>();
        
        try {
            // Total telemetry records
            long totalRecords = telemetryRecordRepository.count();
            overview.put("totalRecords", totalRecords);
            
            // Records in last 24 hours
            Integer records24h = telemetryRecordRepository.countByTimestampAfter(
                    LocalDateTime.now().minusHours(24));
            overview.put("recordsLast24h", records24h);
            
            // Unique devices sending data (last 24 hours)
            Integer uniqueDevices24h = telemetryRecordRepository.countUniqueDevicesAfter(
                    LocalDateTime.now().minusHours(24));
            overview.put("uniqueDevicesLast24h", uniqueDevices24h);
            
            // Data volume by organization (last 24 hours)
            List<Object[]> orgVolume = telemetryRecordRepository.getTelemetryVolumeByOrganizationLast24h();
            Map<String, Long> volumeByOrg = orgVolume.stream()
                    .collect(Collectors.toMap(
                            row -> (String) row[0],
                            row -> (Long) row[1]
                    ));
            overview.put("volumeByOrganization", volumeByOrg);
            
            // Average messages per device
            double avgPerDevice = uniqueDevices24h != null && uniqueDevices24h > 0 ? 
                    (double) records24h / uniqueDevices24h : 0;
            overview.put("avgMessagesPerDevice", Math.round(avgPerDevice * 100.0) / 100.0);
            
            overview.put("lastCalculated", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error calculating pilot telemetry overview: {}", e.getMessage());
            overview.put("error", e.getMessage());
        }
        
        return overview;
    }

    /**
     * Preload frequently accessed telemetry data for better performance
     */
    @Async("telemetryExecutor")
    public CompletableFuture<Void> preloadFrequentlyAccessedData(String organizationId) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Preloading frequently accessed telemetry data for org: {}", organizationId);
                
                // Get most active devices (sent data in last hour)
                List<String> activeDeviceIds = telemetryRecordRepository.getMostActiveDevices(
                        organizationId, LocalDateTime.now().minusHours(1), 20);
                
                // Warm up cache for these devices
                warmUpTelemetryCache(activeDeviceIds);
                
                // Preload common analytics
                getTelemetryStatistics(organizationId, 24);
                getTelemetryIngestionRate(60);
                
                logger.info("Completed preloading telemetry data for {} active devices", activeDeviceIds.size());
                
            } catch (Exception e) {
                logger.error("Error preloading telemetry data for org {}: {}", organizationId, e.getMessage());
            }
        });
    }
}