package org.sensorvision.service;

import org.sensorvision.config.PilotConfiguration;
import org.sensorvision.exception.QuotaExceededException;
import org.sensorvision.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Service to enforce quotas and limits for the SensorVision Pilot Program
 */
@Service
@ConditionalOnProperty(name = "pilot.mode", havingValue = "true")
public class PilotQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(PilotQuotaService.class);

    @Autowired
    private PilotConfiguration pilotConfig;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private TelemetryRecordRepository telemetryRecordRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Validate organization creation against pilot limits
     */
    public void validateOrganizationCreation() {
        long currentOrgCount = organizationRepository.count();
        
        if (currentOrgCount >= pilotConfig.getMaxOrganizations()) {
            logger.warn("Organization creation blocked: pilot limit reached ({}/{})", 
                currentOrgCount, pilotConfig.getMaxOrganizations());
            throw new QuotaExceededException(
                String.format("Pilot program organization limit reached (%d/%d). " +
                    "Please contact pilot-support@sensorvision.io for assistance.",
                    currentOrgCount, pilotConfig.getMaxOrganizations())
            );
        }
        
        logger.info("Organization creation validated: {}/{} organizations", 
            currentOrgCount + 1, pilotConfig.getMaxOrganizations());
    }

    /**
     * Validate user creation against pilot limits
     */
    public void validateUserCreation(String organizationId) {
        long userCount = userRepository.countByOrganizationId(organizationId);
        
        if (userCount >= pilotConfig.getMaxUsersPerOrg()) {
            logger.warn("User creation blocked for org {}: limit reached ({}/{})", 
                organizationId, userCount, pilotConfig.getMaxUsersPerOrg());
            throw new QuotaExceededException(
                String.format("Organization user limit reached (%d/%d). " +
                    "Please contact your organization admin or pilot support.",
                    userCount, pilotConfig.getMaxUsersPerOrg())
            );
        }
        
        logger.debug("User creation validated for org {}: {}/{} users", 
            organizationId, userCount + 1, pilotConfig.getMaxUsersPerOrg());
    }

    /**
     * Validate device creation against pilot limits
     */
    public void validateDeviceCreation(String organizationId) {
        long deviceCount = deviceRepository.countByOrganizationId(organizationId);
        
        if (deviceCount >= pilotConfig.getMaxDevicesPerOrg()) {
            logger.warn("Device creation blocked for org {}: limit reached ({}/{})", 
                organizationId, deviceCount, pilotConfig.getMaxDevicesPerOrg());
            throw new QuotaExceededException(
                String.format("Organization device limit reached (%d/%d). " +
                    "Please contact pilot support to discuss increasing your limit.",
                    deviceCount, pilotConfig.getMaxDevicesPerOrg())
            );
        }
        
        logger.debug("Device creation validated for org {}: {}/{} devices", 
            organizationId, deviceCount + 1, pilotConfig.getMaxDevicesPerOrg());
    }

    /**
     * Validate dashboard creation against pilot limits
     */
    public void validateDashboardCreation(String organizationId) {
        long dashboardCount = dashboardRepository.countByOrganizationId(organizationId);
        
        if (dashboardCount >= pilotConfig.getMaxDashboardsPerOrg()) {
            logger.warn("Dashboard creation blocked for org {}: limit reached ({}/{})", 
                organizationId, dashboardCount, pilotConfig.getMaxDashboardsPerOrg());
            throw new QuotaExceededException(
                String.format("Organization dashboard limit reached (%d/%d). " +
                    "Consider deleting unused dashboards or contact pilot support.",
                    dashboardCount, pilotConfig.getMaxDashboardsPerOrg())
            );
        }
        
        logger.debug("Dashboard creation validated for org {}: {}/{} dashboards", 
            organizationId, dashboardCount + 1, pilotConfig.getMaxDashboardsPerOrg());
    }

    /**
     * Validate rule creation against pilot limits
     */
    public void validateRuleCreation(String organizationId) {
        long ruleCount = ruleRepository.countByOrganizationId(organizationId);
        
        if (ruleCount >= pilotConfig.getMaxRulesPerOrg()) {
            logger.warn("Rule creation blocked for org {}: limit reached ({}/{})", 
                organizationId, ruleCount, pilotConfig.getMaxRulesPerOrg());
            throw new QuotaExceededException(
                String.format("Organization rule limit reached (%d/%d). " +
                    "Consider consolidating rules or contact pilot support.",
                    ruleCount, pilotConfig.getMaxRulesPerOrg())
            );
        }
        
        logger.debug("Rule creation validated for org {}: {}/{} rules", 
            organizationId, ruleCount + 1, pilotConfig.getMaxRulesPerOrg());
    }

    /**
     * Validate API call against daily limits
     */
    public void validateApiCall(String organizationId, String endpoint) {
        String key = String.format("api_calls:%s:%s", organizationId, LocalDate.now());
        
        try {
            Long callCount = redisTemplate.opsForValue().increment(key);
            
            if (callCount == 1) {
                // Set expiration for the key (24 hours)
                redisTemplate.expire(key, Duration.ofDays(1));
            }
            
            if (callCount > pilotConfig.getMaxApiCallsPerDay()) {
                logger.warn("API call blocked for org {}: daily limit exceeded ({}/{})", 
                    organizationId, callCount, pilotConfig.getMaxApiCallsPerDay());
                throw new QuotaExceededException(
                    String.format("Daily API call limit exceeded (%d/%d). " +
                        "Limit resets at midnight UTC. Contact pilot support if you need a higher limit.",
                        callCount, pilotConfig.getMaxApiCallsPerDay())
                );
            }
            
            // Log warning when approaching limit (90%)
            if (callCount > pilotConfig.getMaxApiCallsPerDay() * 0.9) {
                logger.warn("API call limit warning for org {}: approaching daily limit ({}/{})", 
                    organizationId, callCount, pilotConfig.getMaxApiCallsPerDay());
            }
            
        } catch (Exception e) {
            logger.error("Error validating API call for org {}: {}", organizationId, e.getMessage());
            // Don't block on Redis errors - fail open for availability
        }
    }

    /**
     * Validate telemetry ingestion against daily limits
     */
    public void validateTelemetryIngestion(String organizationId, int pointCount) {
        String key = String.format("telemetry_points:%s:%s", organizationId, LocalDate.now());
        
        try {
            Long totalPoints = redisTemplate.opsForValue().increment(key, pointCount);
            
            if (totalPoints <= pointCount) {
                // First increment of the day - set expiration
                redisTemplate.expire(key, Duration.ofDays(1));
            }
            
            if (totalPoints > pilotConfig.getMaxTelemetryPointsPerDay()) {
                logger.warn("Telemetry ingestion blocked for org {}: daily limit exceeded ({}/{})", 
                    organizationId, totalPoints, pilotConfig.getMaxTelemetryPointsPerDay());
                throw new QuotaExceededException(
                    String.format("Daily telemetry point limit exceeded (%d/%d). " +
                        "Limit resets at midnight UTC. Consider reducing data frequency or contact pilot support.",
                        totalPoints, pilotConfig.getMaxTelemetryPointsPerDay())
                );
            }
            
            // Log warning when approaching limit (90%)
            if (totalPoints > pilotConfig.getMaxTelemetryPointsPerDay() * 0.9) {
                logger.warn("Telemetry limit warning for org {}: approaching daily limit ({}/{})", 
                    organizationId, totalPoints, pilotConfig.getMaxTelemetryPointsPerDay());
            }
            
        } catch (Exception e) {
            logger.error("Error validating telemetry ingestion for org {}: {}", organizationId, e.getMessage());
            // Don't block on Redis errors - fail open for availability
        }
    }

    /**
     * Check if a feature is enabled for the pilot program
     */
    public void validateFeatureAccess(String feature) {
        if (!pilotConfig.isFeatureEnabled(feature)) {
            logger.warn("Feature access blocked: {} is not enabled in pilot program", feature);
            throw new QuotaExceededException(
                String.format("Feature '%s' is not available in the pilot program. " +
                    "Contact pilot support for more information.", feature)
            );
        }
    }

    /**
     * Get current usage statistics for an organization
     */
    public PilotUsageStats getUsageStats(String organizationId) {
        PilotUsageStats stats = new PilotUsageStats();
        
        // Current counts
        stats.setUserCount(userRepository.countByOrganizationId(organizationId));
        stats.setDeviceCount(deviceRepository.countByOrganizationId(organizationId));
        stats.setDashboardCount(dashboardRepository.countByOrganizationId(organizationId));
        stats.setRuleCount(ruleRepository.countByOrganizationId(organizationId));
        
        // Daily usage from Redis
        String apiKey = String.format("api_calls:%s:%s", organizationId, LocalDate.now());
        String telemetryKey = String.format("telemetry_points:%s:%s", organizationId, LocalDate.now());
        
        try {
            Object apiCalls = redisTemplate.opsForValue().get(apiKey);
            Object telemetryPoints = redisTemplate.opsForValue().get(telemetryKey);
            
            stats.setApiCallsToday(apiCalls != null ? Long.parseLong(apiCalls.toString()) : 0L);
            stats.setTelemetryPointsToday(telemetryPoints != null ? Long.parseLong(telemetryPoints.toString()) : 0L);
        } catch (Exception e) {
            logger.error("Error retrieving usage stats for org {}: {}", organizationId, e.getMessage());
            stats.setApiCallsToday(0L);
            stats.setTelemetryPointsToday(0L);
        }
        
        // Limits
        stats.setMaxUsers(pilotConfig.getMaxUsersPerOrg());
        stats.setMaxDevices(pilotConfig.getMaxDevicesPerOrg());
        stats.setMaxDashboards(pilotConfig.getMaxDashboardsPerOrg());
        stats.setMaxRules(pilotConfig.getMaxRulesPerOrg());
        stats.setMaxApiCallsPerDay(pilotConfig.getMaxApiCallsPerDay());
        stats.setMaxTelemetryPointsPerDay(pilotConfig.getMaxTelemetryPointsPerDay());
        
        return stats;
    }

    /**
     * Usage statistics for pilot organizations
     */
    public static class PilotUsageStats {
        private long userCount;
        private long deviceCount;
        private long dashboardCount;
        private long ruleCount;
        private long apiCallsToday;
        private long telemetryPointsToday;
        
        private int maxUsers;
        private int maxDevices;
        private int maxDashboards;
        private int maxRules;
        private long maxApiCallsPerDay;
        private long maxTelemetryPointsPerDay;

        // Getters and Setters
        public long getUserCount() { return userCount; }
        public void setUserCount(long userCount) { this.userCount = userCount; }

        public long getDeviceCount() { return deviceCount; }
        public void setDeviceCount(long deviceCount) { this.deviceCount = deviceCount; }

        public long getDashboardCount() { return dashboardCount; }
        public void setDashboardCount(long dashboardCount) { this.dashboardCount = dashboardCount; }

        public long getRuleCount() { return ruleCount; }
        public void setRuleCount(long ruleCount) { this.ruleCount = ruleCount; }

        public long getApiCallsToday() { return apiCallsToday; }
        public void setApiCallsToday(long apiCallsToday) { this.apiCallsToday = apiCallsToday; }

        public long getTelemetryPointsToday() { return telemetryPointsToday; }
        public void setTelemetryPointsToday(long telemetryPointsToday) { this.telemetryPointsToday = telemetryPointsToday; }

        public int getMaxUsers() { return maxUsers; }
        public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

        public int getMaxDevices() { return maxDevices; }
        public void setMaxDevices(int maxDevices) { this.maxDevices = maxDevices; }

        public int getMaxDashboards() { return maxDashboards; }
        public void setMaxDashboards(int maxDashboards) { this.maxDashboards = maxDashboards; }

        public int getMaxRules() { return maxRules; }
        public void setMaxRules(int maxRules) { this.maxRules = maxRules; }

        public long getMaxApiCallsPerDay() { return maxApiCallsPerDay; }
        public void setMaxApiCallsPerDay(long maxApiCallsPerDay) { this.maxApiCallsPerDay = maxApiCallsPerDay; }

        public long getMaxTelemetryPointsPerDay() { return maxTelemetryPointsPerDay; }
        public void setMaxTelemetryPointsPerDay(long maxTelemetryPointsPerDay) { this.maxTelemetryPointsPerDay = maxTelemetryPointsPerDay; }

        // Utility methods
        public double getUserUtilization() {
            return maxUsers > 0 ? (double) userCount / maxUsers * 100 : 0;
        }

        public double getDeviceUtilization() {
            return maxDevices > 0 ? (double) deviceCount / maxDevices * 100 : 0;
        }

        public double getApiCallUtilization() {
            return maxApiCallsPerDay > 0 ? (double) apiCallsToday / maxApiCallsPerDay * 100 : 0;
        }

        public double getTelemetryUtilization() {
            return maxTelemetryPointsPerDay > 0 ? (double) telemetryPointsToday / maxTelemetryPointsPerDay * 100 : 0;
        }
    }
}