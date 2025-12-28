package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.Widget;
import io.indcloud.model.WidgetAggregation;
import io.indcloud.model.WidgetType;

import java.time.Instant;

public record WidgetResponse(
    Long id,
    Long dashboardId,
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
    JsonNode config,
    Instant createdAt,
    Instant updatedAt
) {
    public static WidgetResponse fromEntity(Widget widget) {
        return new WidgetResponse(
            widget.getId(),
            widget.getDashboard() != null ? widget.getDashboard().getId() : null,
            widget.getName(),
            widget.getType(),
            widget.getPositionX(),
            widget.getPositionY(),
            widget.getWidth(),
            widget.getHeight(),
            widget.getDeviceId(),
            widget.getSecondDeviceId(),
            widget.getVariableName(),
            widget.getSecondVariableName(),
            widget.getDeviceLabel(),
            widget.getSecondDeviceLabel(),
            widget.getAggregation(),
            widget.getTimeRangeMinutes(),
            widget.getConfig(),
            widget.getCreatedAt(),
            widget.getUpdatedAt()
        );
    }
}
