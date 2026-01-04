package io.indcloud.dto;

import io.indcloud.model.DeviceTypeDashboardTemplate.WidgetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request DTO for device type dashboard template widget.
 */
public record DeviceTypeDashboardTemplateRequest(
        @NotNull(message = "Widget type is required")
        WidgetType widgetType,

        @NotBlank(message = "Title is required")
        @Size(max = 255)
        String title,

        @Size(max = 100)
        String variableName,

        Map<String, Object> config,

        Integer gridX,

        Integer gridY,

        Integer gridWidth,

        Integer gridHeight,

        Integer displayOrder
) {}
