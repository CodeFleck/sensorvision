package org.sensorvision.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for updating user preferences
 * Supports theme preference and email notification settings
 */
public record UpdateUserPreferencesRequest(
    @Pattern(regexp = "^(light|dark|system)$", message = "Theme preference must be 'light', 'dark', or 'system'")
    String themePreference,

    Boolean emailNotificationsEnabled
) {
    // Compact constructor for validation
    public UpdateUserPreferencesRequest {
        // At least one preference must be provided
        if (themePreference == null && emailNotificationsEnabled == null) {
            throw new IllegalArgumentException("At least one preference must be provided");
        }
    }
}
