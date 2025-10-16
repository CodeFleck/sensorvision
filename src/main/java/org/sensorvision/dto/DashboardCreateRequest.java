package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record DashboardCreateRequest(
    String name,
    String description,
    Boolean isDefault,
    JsonNode layoutConfig
) {
}
