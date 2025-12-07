package org.sensorvision.service;

import org.sensorvision.config.PilotConfiguration;
import org.sensorvision.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for collecting and analyzing pilot program metrics and analytics
 */
@Service
@ConditionalOnProperty(name = "pilot.mode", havingValue = "true")
public class PilotAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(PilotAnalyticsService.class);

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
    private AlertRepository alertRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Get comprehensive pilot program analytics
     */
    public Map<String, Object> getPilotAnalytics() {
        logger.info("Generating pilot program analytics");

        Map<String, Object> analytics = new HashMap<>();
        
        // Overall program metrics
        analytics.put("programOverview", getProgramOverview());
        
        // Organization metrics
        analytics.put("organizationMetrics", getOrganizationMetrics());
        
        // User engagement metrics
        analytics.put("userEngagement", getUserEngagementMetrics());
        
        // Device and data metrics
        analytics.put("deviceMetrics", getDeviceMetrics());
        
        // Feature adoption metrics
        analytics.put("featureAdoption", getFeatureAdoptionMetrics());
        
        // Performance metrics
        analytics.put("performanceMetrics", getPerformanceMetrics());
        
        // Daily trends (last 30 days)
        analytics.put("dailyTrends", getDailyTrends(30));
        
        return analytics;
    }

    /**
     * Get analytics for a specific organization
     */
    public Map<String, Object> getOrganizationAnalytics(String organizationId, int days) {
        logger.info("Generating analytics for organization {} over {} days", organizationId, days);

        Map<String, Object> analytics = new HashMap<>();
        
        // Basic organization info
        analytics.put("organizationId", organizationId);
        analytics.put("period", days + " days");
        
        // User metrics
        analytics.put("userCount", userRepository.countByOrganizationId(organizationId));
        analytics.put("activeUsers", getActiveUsersCount(organizationId, days));
        
        // Device metrics
        analytics.put("deviceCount", deviceRepository.countByOrganizationId(organizationId));
        analytics.put("activeDevices", getActiveDevicesCount(organizationId, days));
        
        // Dashboard and rule metrics
        analytics.put("dashboardCount", dashboardRepository.countByOrganizationId(organizationId));
        analytics.put("ruleCount", ruleRepository.countByOrganizationId(organizationId));
        
        // Data volume metrics
        analytics.put("telemetryPoints", getTelemetryPointsCount(organizationId, days));
        analytics.put("alertsGenerated", getAlertsCount(organizationId, days));
        
        // Usage patterns
        analytics.put("usagePatterns", getUsagePatterns(organizationId, days));
        
        // Quota utilization
        analytics.put("quotaUtilization", getQuotaUtilization(organizationId));
        
        return analytics;
    }

    /**
     * Get analytics for all organizations
     */
    public List<Map<String, Object>> getAllOrganizationsAnalytics(int days) {
        logger.info("Generating analytics for all organizations over {} days", days);

        return organizationRepository.findAll().stream()
            .map(org -> getOrganizationAnalytics(org.getId(), days))
            .collect(Collectors.toList());
    }

    /**
     * Track a pilot event for analytics
     */
    public void trackEvent(String organizationId, String userId, String eventType, Map<String, Object> properties) {
        if (!pilotConfig.getAnalytics().getEnabled()) {
            return;
        }

        try {
            String key = String.format("pilot_events:%s:%s", LocalDate.now(), eventType);
            
            Map<String, Object> event = new HashMap<>();
            event.put("timestamp", System.currentTimeMillis());
            event.put("organizationId", organizationId);
            event.put("userId", userId);
            event.put("eventType", eventType);
            event.put("properties", properties);
            
            redisTemplate.opsForList().leftPush(key, event);
            redisTemplate.expire(key, java.time.Duration.ofDays(90)); // Keep for 90 days
            
            logger.debug("Tracked pilot event: {} for user {} in org {}", eventType, userId, organizationId);
            
        } catch (Exception e) {
            logger.error("Error tracking pilot event: {}", e.getMessage());
        }
    }

    /**
     * Get program overview metrics
     */
    private Map<String, Object> getProgramOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        long totalOrganizations = organizationRepository.count();
        long totalUsers = userRepository.count();
        long totalDevices = deviceRepository.count();
        
        overview.put("totalOrganizations", totalOrganizations);
        overview.put("totalUsers", totalUsers);
        overview.put("totalDevices", totalDevices);
        overview.put("organizationUtilization", (double) totalOrganizations / pilotConfig.getMaxOrganizations() * 100);
        
        // Program health score (0-100)
        double healthScore = calculateProgramHealthScore();
        overview.put("healthScore", healthScore);
        
        return overview;
    }

    /**
     * Get organization-level metrics
     */
    private Map<String, Object> getOrganizationMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Organization distribution
        List<Map<String, Object>> orgStats = organizationRepository.findAll().stream()
            .map(org -> {
                Map<String, Object> stat = new HashMap<>();
                stat.put("id", org.getId());
                stat.put("name", org.getName());
                stat.put("userCount", userRepository.countByOrganizationId(org.getId()));
                stat.put("deviceCount", deviceRepository.countByOrganizationId(org.getId()));
                stat.put("createdAt", org.getCreatedAt());
                return stat;
            })
            .collect(Collectors.toList());
        
        metrics.put("organizations", orgStats);
        
        // Average metrics per organization
        double avgUsersPerOrg = orgStats.stream()
            .mapToLong(stat -> (Long) stat.get("userCount"))
            .average()
            .orElse(0.0);
        
        double avgDevicesPerOrg = orgStats.stream()
            .mapToLong(stat -> (Long) stat.get("deviceCount"))
            .average()
            .orElse(0.0);
        
        metrics.put("averageUsersPerOrganization", avgUsersPerOrg);
        metrics.put("averageDevicesPerOrganization", avgDevicesPerOrg);
        
        return metrics;
    }

    /**
     * Get user engagement metrics
     */
    private Map<String, Object> getUserEngagementMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Active users (last 7 days)
        long activeUsers7d = getActiveUsersCount(null, 7);
        long activeUsers30d = getActiveUsersCount(null, 30);
        long totalUsers = userRepository.count();
        
        metrics.put("activeUsers7d", activeUsers7d);
        metrics.put("activeUsers30d", activeUsers30d);
        metrics.put("totalUsers", totalUsers);
        metrics.put("weeklyActiveUserRate", totalUsers > 0 ? (double) activeUsers7d / totalUsers * 100 : 0);
        metrics.put("monthlyActiveUserRate", totalUsers > 0 ? (double) activeUsers30d / totalUsers * 100 : 0);
        
        // User retention (users who logged in both this week and last week)
        // This would require tracking login events - placeholder for now
        metrics.put("userRetentionRate", 75.0); // Placeholder
        
        return metrics;
    }

    /**
     * Get device-related metrics
     */
    private Map<String, Object> getDeviceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long totalDevices = deviceRepository.count();
        long activeDevices = getActiveDevicesCount(null, 7);
        
        metrics.put("totalDevices", totalDevices);
        metrics.put("activeDevices", activeDevices);
        metrics.put("deviceActivityRate", totalDevices > 0 ? (double) activeDevices / totalDevices * 100 : 0);
        
        // Telemetry volume (last 24 hours)
        long telemetryPoints24h = getTelemetryPointsCount(null, 1);
        metrics.put("telemetryPoints24h", telemetryPoints24h);
        metrics.put("avgTelemetryPerDevice", activeDevices > 0 ? (double) telemetryPoints24h / activeDevices : 0);
        
        return metrics;
    }

    /**
     * Get feature adoption metrics
     */
    private Map<String, Object> getFeatureAdoptionMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long totalOrgs = organizationRepository.count();
        
        // Dashboard usage
        long orgsWithDashboards = dashboardRepository.countDistinctOrganizations();
        metrics.put("dashboardAdoptionRate", totalOrgs > 0 ? (double) orgsWithDashboards / totalOrgs * 100 : 0);
        
        // Rules usage
        long orgsWithRules = ruleRepository.countDistinctOrganizations();
        metrics.put("rulesAdoptionRate", totalOrgs > 0 ? (double) orgsWithRules / totalOrgs * 100 : 0);
        
        // Feature-specific metrics would be tracked via events
        metrics.put("pluginInstallations", getEventCount("plugin_installed", 30));
        metrics.put("syntheticVariablesCreated", getEventCount("synthetic_variable_created", 30));
        
        return metrics;
    }

    /**
     * Get performance metrics
     */
    private Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // These would typically come from application metrics
        // Placeholder values for now
        metrics.put("avgApiResponseTime", 245.0); // ms
        metrics.put("avgTelemetryProcessingTime", 12.0); // ms
        metrics.put("systemUptime", 99.8); // percentage
        metrics.put("errorRate", 0.02); // percentage
        
        return metrics;
    }

    /**
     * Get daily trends over specified period
     */
    private Map<String, Object> getDailyTrends(int days) {
        Map<String, Object> trends = new HashMap<>();
        
        List<String> dates = new ArrayList<>();
        List<Long> userLogins = new ArrayList<>();
        List<Long> deviceActivity = new ArrayList<>();
        List<Long> telemetryVolume = new ArrayList<>();
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dates.add(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            // These would come from tracked events or database queries
            userLogins.add(getEventCountForDate("user_login", date));
            deviceActivity.add(getActiveDevicesCountForDate(date));
            telemetryVolume.add(getTelemetryVolumeForDate(date));
        }
        
        trends.put("dates", dates);
        trends.put("userLogins", userLogins);
        trends.put("deviceActivity", deviceActivity);
        trends.put("telemetryVolume", telemetryVolume);
        
        return trends;
    }

    /**
     * Calculate overall program health score
     */
    private double calculateProgramHealthScore() {
        double score = 0.0;
        
        // Organization utilization (20%)
        double orgUtilization = (double) organizationRepository.count() / pilotConfig.getMaxOrganizations();
        score += Math.min(orgUtilization * 100, 100) * 0.2;
        
        // User engagement (30%)
        long totalUsers = userRepository.count();
        long activeUsers = getActiveUsersCount(null, 7);
        double userEngagement = totalUsers > 0 ? (double) activeUsers / totalUsers * 100 : 0;
        score += userEngagement * 0.3;
        
        // Device activity (25%)
        long totalDevices = deviceRepository.count();
        long activeDevices = getActiveDevicesCount(null, 7);
        double deviceActivity = totalDevices > 0 ? (double) activeDevices / totalDevices * 100 : 0;
        score += deviceActivity * 0.25;
        
        // Feature adoption (25%)
        long orgsWithDashboards = dashboardRepository.countDistinctOrganizations();
        long orgsWithRules = ruleRepository.countDistinctOrganizations();
        long totalOrgs = organizationRepository.count();
        double featureAdoption = totalOrgs > 0 ? 
            ((double) orgsWithDashboards + orgsWithRules) / (totalOrgs * 2) * 100 : 0;
        score += featureAdoption * 0.25;
        
        return Math.min(score, 100.0);
    }

    // Helper methods (these would need actual implementations based on your data model)
    
    private long getActiveUsersCount(String organizationId, int days) {
        // This would query user login events or last_login timestamps
        return 0; // Placeholder
    }
    
    private long getActiveDevicesCount(String organizationId, int days) {
        // This would query devices that sent telemetry in the last N days
        return 0; // Placeholder
    }
    
    private long getTelemetryPointsCount(String organizationId, int days) {
        // This would query telemetry records for the period
        return 0; // Placeholder
    }
    
    private long getAlertsCount(String organizationId, int days) {
        // This would query alerts generated in the period
        return 0; // Placeholder
    }
    
    private Map<String, Object> getUsagePatterns(String organizationId, int days) {
        // This would analyze usage patterns (peak hours, etc.)
        return new HashMap<>();
    }
    
    private Map<String, Object> getQuotaUtilization(String organizationId) {
        // This would calculate quota utilization percentages
        return new HashMap<>();
    }
    
    private long getEventCount(String eventType, int days) {
        // This would count events from Redis
        return 0; // Placeholder
    }
    
    private long getEventCountForDate(String eventType, LocalDate date) {
        // This would count events for a specific date
        return 0; // Placeholder
    }
    
    private long getActiveDevicesCountForDate(LocalDate date) {
        // This would count active devices for a specific date
        return 0; // Placeholder
    }
    
    private long getTelemetryVolumeForDate(LocalDate date) {
        // This would get telemetry volume for a specific date
        return 0; // Placeholder
    }
}