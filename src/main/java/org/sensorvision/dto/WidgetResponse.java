package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.sensorvision.model.Widget;
import org.sensorvision.model.WidgetAggregation;
import org.sensorvision.model.WidgetType;

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
    String variableName,
    Boolean useContextDevice,
    String deviceLabel,
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
            widget.getVariableName(),
            widget.getUseContextDevice(),
            widget.getDeviceLabel(),
            widget.getAggregation(),
            widget.getTimeRangeMinutes(),
            widget.getConfig(),
            widget.getCreatedAt(),
            widget.getUpdatedAt()
        );
    }
}
