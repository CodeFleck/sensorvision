package org.sensorvision.controller;

import org.sensorvision.config.PilotConfiguration;
import org.sensorvision.service.PilotQuotaService;
import org.sensorvision.service.PilotAnalyticsService;
import org.sensorvision.service.PilotFeedbackService;
import org.sensorvision.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.util.Map;

/**
 * REST controller for SensorVision Pilot Program specific endpoints
 */
@RestController
@RequestMapping("/api/v1/pilot")
@ConditionalOnProperty(name = "pilot.mode", havingValue = "true")
public class PilotController {

    private static final Logger logger = LoggerFactory.getLogger(PilotController.class);

    @Autowired
    private PilotConfiguration pilotConfig;

    @Autowired
    private PilotQuotaService pilotQuotaService;

    @Autowired
    private PilotAnalyticsService pilotAnalyticsService;

    @Autowired
    private PilotFeedbackService pilotFeedbackService;

    /**
     * Get pilot program configuration and status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPilotStatus(Authentication auth) {
        logger.info("Pilot status requested by user: {}", auth.getName());
        
        Map<String, Object> status = Map.of(
            "pilotMode", pilotConfig.getMode(),
            "maxOrganizations", pilotConfig.getMaxOrganizations(),
            "featuresEnabled", pilotConfig.getFeaturesEnabled(),
            "supportEmail", pilotConfig.getSupport().getEmail(),
            "feedbackEnabled", pilotConfig.getSupport().getFeedbackEnabled()
        );
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Get usage statistics for the current organization
     */
    @GetMapping("/usage")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PilotQuotaService.PilotUsageStats>> getUsageStats(Authentication auth) {
        String organizationId = extractOrganizationId(auth);
        logger.info("Usage stats requested for organization: {}", organizationId);
        
        PilotQuotaService.PilotUsageStats stats = pilotQuotaService.getUsageStats(organizationId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Submit feedback for the pilot program
     */
    @PostMapping("/feedback")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<String>> submitFeedback(
            @Valid @RequestBody PilotFeedbackRequest request,
            Authentication auth) {
        
        String userId = auth.getName();
        String organizationId = extractOrganizationId(auth);
        
        logger.info("Feedback submitted by user {} from organization {}", userId, organizationId);
        
        pilotFeedbackService.submitFeedback(userId, organizationId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted successfully. Thank you for helping improve SensorVision!"));
    }

    /**
     * Get pilot analytics dashboard (admin only)
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PILOT_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPilotAnalytics() {
        logger.info("Pilot analytics requested");
        
        Map<String, Object> analytics = pilotAnalyticsService.getPilotAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    /**
     * Get detailed organization analytics (admin only)
     */
    @GetMapping("/analytics/organizations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PILOT_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getOrganizationAnalytics(
            @RequestParam(required = false) String organizationId,
            @RequestParam(defaultValue = "7") int days) {
        
        logger.info("Organization analytics requested for {} days", days);
        
        Object analytics = organizationId != null 
            ? pilotAnalyticsService.getOrganizationAnalytics(organizationId, days)
            : pilotAnalyticsService.getAllOrganizationsAnalytics(days);
            
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    /**
     * Get pilot feedback summary (admin only)
     */
    @GetMapping("/feedback/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PILOT_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getFeedbackSummary(
            @RequestParam(defaultValue = "30") int days) {
        
        logger.info("Feedback summary requested for {} days", days);
        
        Object summary = pilotFeedbackService.getFeedbackSummary(days);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Check if a feature is available in the pilot program
     */
    @GetMapping("/features/{feature}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkFeature(
            @PathVariable String feature) {
        
        boolean enabled = pilotConfig.isFeatureEnabled(feature);
        
        Map<String, Object> result = Map.of(
            "feature", feature,
            "enabled", enabled,
            "message", enabled 
                ? "Feature is available in the pilot program"
                : "Feature is not available in the pilot program"
        );
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get pilot program information and resources
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPilotInfo() {
        Map<String, Object> info = Map.of(
            "programName", "SensorVision Pilot Program",
            "version", "1.0",
            "duration", "90 days",
            "supportEmail", pilotConfig.getSupport().getEmail(),
            "documentationUrl", "https://docs.sensorvision.io/pilot",
            "feedbackEnabled", pilotConfig.getSupport().getFeedbackEnabled(),
            "features", pilotConfig.getFeaturesEnabled()
        );
        
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    /**
     * Health check endpoint for pilot-specific monitoring
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPilotHealth() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "pilotMode", pilotConfig.getMode(),
            "timestamp", System.currentTimeMillis(),
            "quotaServiceActive", pilotQuotaService != null,
            "analyticsServiceActive", pilotAnalyticsService != null,
            "feedbackServiceActive", pilotFeedbackService != null
        );
        
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    /**
     * Extract organization ID from authentication context
     */
    private String extractOrganizationId(Authentication auth) {
        // This would be implemented based on your authentication system
        // For now, returning a placeholder
        return "org-" + auth.getName().hashCode();
    }

    /**
     * Request DTO for pilot feedback
     */
    public static class PilotFeedbackRequest {
        private String category;
        private int rating;
        private String title;
        private String message;
        private String email;
        private Map<String, Object> metadata;

        // Getters and Setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}