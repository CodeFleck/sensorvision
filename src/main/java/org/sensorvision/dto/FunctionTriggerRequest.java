package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import org.sensorvision.model.FunctionTriggerType;

public record FunctionTriggerRequest(
    @NotNull(message = "Trigger type is required")
    FunctionTriggerType triggerType,

    @NotNull(message = "Trigger configuration is required")
    JsonNode triggerConfig,

    Boolean enabled
) {
}
