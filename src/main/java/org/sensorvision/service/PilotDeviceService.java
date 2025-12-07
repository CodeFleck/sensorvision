package org.sensorvision.service;

import org.sensorvision.dto.DeviceResponse;
import org.sensorvision.model.Device;
import org.sensorvision.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced device service with caching optimizations for pilot program performance
 */
@Service
@ConditionalOnProperty(name = "pilot.mode", havingValue = "true")
public class PilotDeviceService {

    private static final Logger logger = LoggerFactory.getLogger(PilotDeviceService.class);

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceService deviceService;

    @Autowired(required = false)
    private PilotQuotaService pilotQuotaService;

    /**
     * Get device with caching for improved performance
     */
    @Cacheable(value = "devices", key = "#deviceId")
    public DeviceResponse getCachedDevice(String deviceId) {
        logger.debug("Fetching device from database (cache miss): {}", deviceId);
        return deviceService.getDevice(deviceId);
    }

    /**
     * Get devices by organization with caching
     */
    @Cacheable(value = "devices", key = "'org:' + #organizationId")
    public List<DeviceResponse> getCachedDevicesByOrganization(String organizationId) {
        logger.debug("Fetching devices by organization from database (cache miss): {}", organizationId);
        return deviceService.getDevicesByOrganization(organizationId);
    }

    /**
     * Get active devices with caching (frequently accessed for pilot monitoring)
     */
    @Cacheable(value = "devices", key = "'active:' + #organizationId")
    public List<DeviceResponse> getCachedActiveDevices(String organizationId) {
        logger.debug("Fetching active devices from database (cache miss): {}", organizationId);
        
        List<Device> activeDevices = deviceRepository.findByOrganizationIdAndActiveTrue(organizationId);
        return activeDevices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get device count by organization with caching (for quota enforcement)
     */
    @Cacheable(value = "pilot-quotas", key = "'device-count:' + #organizationId")
    public long getCachedDeviceCount(String organizationId) {
        logger.debug("Fetching device count from database (cache miss): {}", organizationId);
        return deviceRepository.countByOrganizationId(organizationId);
    }

    /**
     * Get recently active devices (sent data in last 24 hours)
     */
    @Cacheable(value = "devices", key = "'recent-active:' + #organizationId")
    public List<DeviceResponse> getRecentlyActiveDevices(String organizationId) {
        logger.debug("Fetching recently active devices from database (cache miss): {}", organizationId);
        
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Device> recentDevices = deviceRepository.findRecentlyActiveDevices(organizationId, since);
        
        return recentDevices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get device health statistics with caching
     */
    @Cacheable(value = "analytics", key = "'device-health:' + #organizationId")
    public Map<String, Object> getDeviceHealthStats(String organizationId) {
        logger.debug("Calculating device health statistics (cache miss): {}", organizationId);
        
        List<Device> allDevices = deviceRepository.findByOrganizationId(organizationId);
        
        long totalDevices = allDevices.size();
        long onlineDevices = allDevices.stream()
                .filter(d -> "ONLINE".equals(d.getStatus()))
                .count();
        long offlineDevices = allDevices.stream()
                .filter(d -> "OFFLINE".equals(d.getStatus()))
                .count();
        long activeDevices = allDevices.stream()
                .filter(Device::getActive)
                .count();
        
        // Calculate average health score
        double avgHealthScore = allDevices.stream()
                .filter(d -> d.getHealthScore() != null)
                .mapToDouble(Device::getHealthScore)
                .average()
                .orElse(0.0);
        
        Map<String, Object> stats = Map.of(
                "totalDevices", totalDevices,
                "onlineDevices", onlineDevices,
                "offlineDevices", offlineDevices,
                "activeDevices", activeDevices,
                "avgHealthScore", Math.round(avgHealthScore * 100.0) / 100.0,
                "onlinePercentage", totalDevices > 0 ? Math.round((double) onlineDevices / totalDevices * 100 * 100.0) / 100.0 : 0,
                "lastUpdated", LocalDateTime.now()
        );
        
        return stats;
    }

    /**
     * Update device and evict cache
     */
    @CacheEvict(value = {"devices", "pilot-quotas"}, allEntries = true)
    public DeviceResponse updateDeviceAndEvictCache(String deviceId, DeviceResponse updatedDevice) {
        logger.debug("Updating device and evicting cache: {}", deviceId);
        
        // Update the device through the main service
        DeviceResponse result = deviceService.updateDevice(deviceId, convertToUpdateRequest(updatedDevice));
        
        // Log cache eviction
        logger.debug("Cache evicted for device update: {}", deviceId);
        
        return result;
    }

    /**
     * Create device with quota validation and cache management
     */
    @CacheEvict(value = {"devices", "pilot-quotas"}, allEntries = true)
    public DeviceResponse createDeviceWithQuotaCheck(String organizationId, DeviceResponse deviceRequest) {
        logger.debug("Creating device with quota validation: {}", deviceRequest.getExternalId());
        
        // Validate quota if pilot quota service is available
        if (pilotQuotaService != null) {
            pilotQuotaService.validateDeviceCreation(organizationId);
        }
        
        // Create device through main service
        DeviceResponse result = deviceService.createDevice(convertToCreateRequest(deviceRequest));
        
        logger.info("Device created successfully with quota validation: {}", result.getExternalId());
        return result;
    }

    /**
     * Delete device and evict cache
     */
    @CacheEvict(value = {"devices", "pilot-quotas"}, allEntries = true)
    public void deleteDeviceAndEvictCache(String deviceId) {
        logger.debug("Deleting device and evicting cache: {}", deviceId);
        
        deviceService.deleteDevice(deviceId);
        
        logger.debug("Cache evicted for device deletion: {}", deviceId);
    }

    /**
     * Refresh device cache for specific organization
     */
    @CacheEvict(value = "devices", key = "'org:' + #organizationId")
    public void refreshOrganizationDeviceCache(String organizationId) {
        logger.debug("Refreshing device cache for organization: {}", organizationId);
    }

    /**
     * Warm up cache with frequently accessed devices
     */
    @CachePut(value = "devices", key = "#device.externalId")
    public DeviceResponse warmUpDeviceCache(Device device) {
        logger.debug("Warming up cache for device: {}", device.getExternalId());
        return convertToResponse(device);
    }

    /**
     * Get pilot program device statistics
     */
    @Cacheable(value = "analytics", key = "'pilot-device-stats'")
    public Map<String, Object> getPilotDeviceStatistics() {
        logger.debug("Calculating pilot program device statistics (cache miss)");
        
        // Get statistics across all pilot organizations
        long totalDevices = deviceRepository.count();
        long activeDevices = deviceRepository.countByActiveTrue();
        long onlineDevices = deviceRepository.countByStatus("ONLINE");
        
        // Get device distribution by organization
        List<Object[]> devicesByOrg = deviceRepository.getDeviceCountByOrganization();
        Map<String, Long> orgDistribution = devicesByOrg.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // organization_id
                        row -> (Long) row[1]    // device_count
                ));
        
        // Calculate utilization against pilot quotas
        double avgDevicesPerOrg = orgDistribution.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        Map<String, Object> stats = Map.of(
                "totalDevices", totalDevices,
                "activeDevices", activeDevices,
                "onlineDevices", onlineDevices,
                "deviceUtilizationPercent", totalDevices > 0 ? Math.round((double) activeDevices / totalDevices * 100 * 100.0) / 100.0 : 0,
                "avgDevicesPerOrganization", Math.round(avgDevicesPerOrg * 100.0) / 100.0,
                "organizationDistribution", orgDistribution,
                "lastCalculated", LocalDateTime.now()
        );
        
        return stats;
    }

    /**
     * Convert Device entity to DeviceResponse DTO
     */
    private DeviceResponse convertToResponse(Device device) {
        return DeviceResponse.builder()
                .externalId(device.getExternalId())
                .name(device.getName())
                .status(device.getStatus())
                .location(device.getLocation())
                .sensorType(device.getSensorType())
                .description(device.getDescription())
                .active(device.getActive())
                .healthScore(device.getHealthScore())
                .lastSeen(device.getLastSeen())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }

    /**
     * Convert DeviceResponse to DeviceCreateRequest
     */
    private org.sensorvision.dto.DeviceCreateRequest convertToCreateRequest(DeviceResponse response) {
        return org.sensorvision.dto.DeviceCreateRequest.builder()
                .externalId(response.getExternalId())
                .name(response.getName())
                .location(response.getLocation())
                .sensorType(response.getSensorType())
                .description(response.getDescription())
                .build();
    }

    /**
     * Convert DeviceResponse to DeviceUpdateRequest
     */
    private org.sensorvision.dto.DeviceUpdateRequest convertToUpdateRequest(DeviceResponse response) {
        return org.sensorvision.dto.DeviceUpdateRequest.builder()
                .name(response.getName())
                .location(response.getLocation())
                .sensorType(response.getSensorType())
                .description(response.getDescription())
                .active(response.getActive())
                .build();
    }
}