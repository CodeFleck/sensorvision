package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.WidgetAggregation;
import io.indcloud.model.WidgetType;

public record WidgetUpdateRequest(
    String name,
    WidgetType type,
    Integer positionX,
    Integer positionY,
    Integer width,
    Integer height,
    String deviceId,
    String secondDeviceId,
    String variableName,
    String secondVariableName,
    String deviceLabel,
    String secondDeviceLabel,
    WidgetAggregation aggregation,
    Integer timeRangeMinutes,
    JsonNode config
) {
}
