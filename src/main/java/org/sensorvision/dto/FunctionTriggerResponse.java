package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.sensorvision.model.FunctionTrigger;
import org.sensorvision.model.FunctionTriggerType;

import java.time.Instant;

public record FunctionTriggerResponse(
    Long id,
    FunctionTriggerType triggerType,
    JsonNode triggerConfig,
    Boolean enabled,
    Instant createdAt
) {
    public static FunctionTriggerResponse fromEntity(FunctionTrigger trigger) {
        return new FunctionTriggerResponse(
            trigger.getId(),
            trigger.getTriggerType(),
            trigger.getTriggerConfig(),
            trigger.getEnabled(),
            trigger.getCreatedAt()
        );
    }
}
