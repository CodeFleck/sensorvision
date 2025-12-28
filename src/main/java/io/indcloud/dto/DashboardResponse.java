package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.Dashboard;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public record DashboardResponse(
    Long id,
    String name,
    String description,
    Boolean isDefault,
    String defaultDeviceId,
    JsonNode layoutConfig,
    List<WidgetResponse> widgets,
    Instant createdAt,
    Instant updatedAt
) {
    public static DashboardResponse fromEntity(Dashboard dashboard) {
        return new DashboardResponse(
            dashboard.getId(),
            dashboard.getName(),
            dashboard.getDescription(),
            dashboard.getIsDefault(),
            dashboard.getDefaultDeviceId(),
            dashboard.getLayoutConfig(),
            dashboard.getWidgets().stream()
                .map(WidgetResponse::fromEntity)
                .collect(Collectors.toList()),
            dashboard.getCreatedAt(),
            dashboard.getUpdatedAt()
        );
    }

    public static DashboardResponse fromEntityWithoutWidgets(Dashboard dashboard) {
        return new DashboardResponse(
            dashboard.getId(),
            dashboard.getName(),
            dashboard.getDescription(),
            dashboard.getIsDefault(),
            dashboard.getDefaultDeviceId(),
            dashboard.getLayoutConfig(),
            null,
            dashboard.getCreatedAt(),
            dashboard.getUpdatedAt()
        );
    }
}
