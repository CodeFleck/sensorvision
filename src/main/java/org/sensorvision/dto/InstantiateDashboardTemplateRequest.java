package org.sensorvision.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to instantiate a dashboard from a template
 */
public record InstantiateDashboardTemplateRequest(
    @NotBlank(message = "Dashboard name is required")
    String dashboardName,

    String dashboardDescription,

    String deviceId  // Optional: bind widgets to specific device
) {
}
