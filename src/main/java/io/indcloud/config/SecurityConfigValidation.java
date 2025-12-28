package io.indcloud.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Validates security configuration at application startup
 * Ensures sensitive values are not using insecure defaults
 */
@Slf4j
@Configuration
public class SecurityConfigValidation {

    @Value("${spring.datasource.username:}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${mqtt.broker.username:}")
    private String mqttUsername;

    @Value("${mqtt.broker.password:}")
    private String mqttPassword;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecurityConfiguration() {
        log.info("Validating security configuration for profile: {}", activeProfile);

        boolean hasErrors = false;

        // Skip validation for test profile
        if ("test".equals(activeProfile)) {
            log.info("Skipping security validation for test profile");
            return;
        }

        // Validate database credentials
        if (dbUsername == null || dbUsername.isEmpty()) {
            log.error("SECURITY WARNING: DB_USERNAME environment variable is not set");
            hasErrors = true;
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            log.error("SECURITY WARNING: DB_PASSWORD environment variable is not set");
            hasErrors = true;
        }

        // Validate JWT secret
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            log.error("SECURITY ERROR: JWT_SECRET environment variable is REQUIRED but not set");
            hasErrors = true;
        } else if (jwtSecret.length() < 32) {
            log.warn("SECURITY WARNING: JWT_SECRET is too short (minimum 32 characters recommended)");
        }

        // Validate MQTT credentials
        if (mqttUsername == null || mqttUsername.isEmpty()) {
            log.warn("SECURITY WARNING: MQTT_USERNAME environment variable is not set");
        }
        if (mqttPassword == null || mqttPassword.isEmpty()) {
            log.warn("SECURITY WARNING: MQTT_PASSWORD environment variable is not set");
        }

        if (hasErrors) {
            log.error("====================================================================");
            log.error("SECURITY CONFIGURATION ERRORS DETECTED");
            log.error("Please set required environment variables before running in production");
            log.error("====================================================================");
            // In production, you might want to throw an exception to prevent startup
            // throw new IllegalStateException("Required security configuration is missing");
        } else {
            log.info("Security configuration validation passed");
        }
    }
}
