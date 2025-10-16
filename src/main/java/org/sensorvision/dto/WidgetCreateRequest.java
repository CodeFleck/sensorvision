package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.sensorvision.model.WidgetAggregation;
import org.sensorvision.model.WidgetType;

public record WidgetCreateRequest(
    String name,
    WidgetType type,
    Integer positionX,
    Integer positionY,
    Integer width,
    Integer height,
    String deviceId,
    String variableName,
    WidgetAggregation aggregation,
    Integer timeRangeMinutes,
    JsonNode config
) {
}
