package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record DashboardUpdateRequest(
    String name,
    String description,
    Boolean isDefault,
    JsonNode layoutConfig
) {
}
