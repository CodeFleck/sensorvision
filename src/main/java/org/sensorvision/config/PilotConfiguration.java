package org.sensorvision.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * Configuration properties for SensorVision Pilot Program
 * Manages quotas, limits, and pilot-specific settings
 */
@Configuration
@ConfigurationProperties(prefix = "pilot")
@Validated
public class PilotConfiguration {

    /**
     * Whether pilot mode is enabled
     */
    @NotNull
    private Boolean mode = false;

    /**
     * Maximum number of organizations in the pilot program
     */
    @Positive
    private Integer maxOrganizations = 10;

    /**
     * Maximum number of users per organization
     */
    @Positive
    private Integer maxUsersPerOrg = 50;

    /**
     * Maximum number of devices per organization
     */
    @Positive
    private Integer maxDevicesPerOrg = 100;

    /**
     * Maximum number of dashboards per organization
     */
    @Positive
    private Integer maxDashboardsPerOrg = 20;

    /**
     * Maximum number of rules per organization
     */
    @Positive
    private Integer maxRulesPerOrg = 50;

    /**
     * Maximum API calls per day per organization
     */
    @Positive
    private Long maxApiCallsPerDay = 100000L;

    /**
     * Maximum telemetry points per day per organization
     */
    @Positive
    private Long maxTelemetryPointsPerDay = 1000000L;

    /**
     * Data retention period in days
     */
    @Min(1)
    private Integer dataRetentionDays = 90;

    /**
     * List of enabled features for pilot program
     */
    private List<String> featuresEnabled = List.of(
        "plugin_marketplace",
        "advanced_analytics", 
        "fleet_monitoring",
        "synthetic_variables"
    );

    /**
     * List of disabled features for pilot program
     */
    private List<String> featuresDisabled = List.of(
        "white_labeling",
        "custom_branding",
        "unlimited_retention"
    );

    /**
     * Pilot support configuration
     */
    private Support support = new Support();

    /**
     * Pilot analytics configuration
     */
    private Analytics analytics = new Analytics();

    /**
     * Pilot notification configuration
     */
    private Notifications notifications = new Notifications();

    // Getters and Setters
    public Boolean getMode() {
        return mode;
    }

    public void setMode(Boolean mode) {
        this.mode = mode;
    }

    public Integer getMaxOrganizations() {
        return maxOrganizations;
    }

    public void setMaxOrganizations(Integer maxOrganizations) {
        this.maxOrganizations = maxOrganizations;
    }

    public Integer getMaxUsersPerOrg() {
        return maxUsersPerOrg;
    }

    public void setMaxUsersPerOrg(Integer maxUsersPerOrg) {
        this.maxUsersPerOrg = maxUsersPerOrg;
    }

    public Integer getMaxDevicesPerOrg() {
        return maxDevicesPerOrg;
    }

    public void setMaxDevicesPerOrg(Integer maxDevicesPerOrg) {
        this.maxDevicesPerOrg = maxDevicesPerOrg;
    }

    public Integer getMaxDashboardsPerOrg() {
        return maxDashboardsPerOrg;
    }

    public void setMaxDashboardsPerOrg(Integer maxDashboardsPerOrg) {
        this.maxDashboardsPerOrg = maxDashboardsPerOrg;
    }

    public Integer getMaxRulesPerOrg() {
        return maxRulesPerOrg;
    }

    public void setMaxRulesPerOrg(Integer maxRulesPerOrg) {
        this.maxRulesPerOrg = maxRulesPerOrg;
    }

    public Long getMaxApiCallsPerDay() {
        return maxApiCallsPerDay;
    }

    public void setMaxApiCallsPerDay(Long maxApiCallsPerDay) {
        this.maxApiCallsPerDay = maxApiCallsPerDay;
    }

    public Long getMaxTelemetryPointsPerDay() {
        return maxTelemetryPointsPerDay;
    }

    public void setMaxTelemetryPointsPerDay(Long maxTelemetryPointsPerDay) {
        this.maxTelemetryPointsPerDay = maxTelemetryPointsPerDay;
    }

    public Integer getDataRetentionDays() {
        return dataRetentionDays;
    }

    public void setDataRetentionDays(Integer dataRetentionDays) {
        this.dataRetentionDays = dataRetentionDays;
    }

    public List<String> getFeaturesEnabled() {
        return featuresEnabled;
    }

    public void setFeaturesEnabled(List<String> featuresEnabled) {
        this.featuresEnabled = featuresEnabled;
    }

    public List<String> getFeaturesDisabled() {
        return featuresDisabled;
    }

    public void setFeaturesDisabled(List<String> featuresDisabled) {
        this.featuresDisabled = featuresDisabled;
    }

    public Support getSupport() {
        return support;
    }

    public void setSupport(Support support) {
        this.support = support;
    }

    public Analytics getAnalytics() {
        return analytics;
    }

    public void setAnalytics(Analytics analytics) {
        this.analytics = analytics;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    /**
     * Check if a feature is enabled in the pilot program
     */
    public boolean isFeatureEnabled(String feature) {
        return featuresEnabled.contains(feature) && !featuresDisabled.contains(feature);
    }

    /**
     * Support configuration
     */
    public static class Support {
        private String email = "pilot-support@sensorvision.io";
        private String slackWebhook;
        private String slackChannel = "#sensorvision-pilot";
        private Boolean feedbackEnabled = true;
        private Boolean chatEnabled = true;

        // Getters and Setters
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSlackWebhook() {
            return slackWebhook;
        }

        public void setSlackWebhook(String slackWebhook) {
            this.slackWebhook = slackWebhook;
        }

        public String getSlackChannel() {
            return slackChannel;
        }

        public void setSlackChannel(String slackChannel) {
            this.slackChannel = slackChannel;
        }

        public Boolean getFeedbackEnabled() {
            return feedbackEnabled;
        }

        public void setFeedbackEnabled(Boolean feedbackEnabled) {
            this.feedbackEnabled = feedbackEnabled;
        }

        public Boolean getChatEnabled() {
            return chatEnabled;
        }

        public void setChatEnabled(Boolean chatEnabled) {
            this.chatEnabled = chatEnabled;
        }
    }

    /**
     * Analytics configuration
     */
    public static class Analytics {
        private Boolean enabled = true;
        private Boolean detailedTracking = true;
        private Integer reportingIntervalHours = 24;
        private List<String> trackedEvents = List.of(
            "user_login",
            "device_created",
            "dashboard_viewed",
            "rule_created",
            "alert_triggered",
            "plugin_installed"
        );

        // Getters and Setters
        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Boolean getDetailedTracking() {
            return detailedTracking;
        }

        public void setDetailedTracking(Boolean detailedTracking) {
            this.detailedTracking = detailedTracking;
        }

        public Integer getReportingIntervalHours() {
            return reportingIntervalHours;
        }

        public void setReportingIntervalHours(Integer reportingIntervalHours) {
            this.reportingIntervalHours = reportingIntervalHours;
        }

        public List<String> getTrackedEvents() {
            return trackedEvents;
        }

        public void setTrackedEvents(List<String> trackedEvents) {
            this.trackedEvents = trackedEvents;
        }
    }

    /**
     * Notifications configuration
     */
    public static class Notifications {
        private Boolean welcomeEmailEnabled = true;
        private Boolean weeklyDigestEnabled = true;
        private Boolean feedbackReminderEnabled = true;
        private Integer feedbackReminderDays = 7;
        private Boolean slackNotificationsEnabled = true;

        // Getters and Setters
        public Boolean getWelcomeEmailEnabled() {
            return welcomeEmailEnabled;
        }

        public void setWelcomeEmailEnabled(Boolean welcomeEmailEnabled) {
            this.welcomeEmailEnabled = welcomeEmailEnabled;
        }

        public Boolean getWeeklyDigestEnabled() {
            return weeklyDigestEnabled;
        }

        public void setWeeklyDigestEnabled(Boolean weeklyDigestEnabled) {
            this.weeklyDigestEnabled = weeklyDigestEnabled;
        }

        public Boolean getFeedbackReminderEnabled() {
            return feedbackReminderEnabled;
        }

        public void setFeedbackReminderEnabled(Boolean feedbackReminderEnabled) {
            this.feedbackReminderEnabled = feedbackReminderEnabled;
        }

        public Integer getFeedbackReminderDays() {
            return feedbackReminderDays;
        }

        public void setFeedbackReminderDays(Integer feedbackReminderDays) {
            this.feedbackReminderDays = feedbackReminderDays;
        }

        public Boolean getSlackNotificationsEnabled() {
            return slackNotificationsEnabled;
        }

        public void setSlackNotificationsEnabled(Boolean slackNotificationsEnabled) {
            this.slackNotificationsEnabled = slackNotificationsEnabled;
        }
    }
}