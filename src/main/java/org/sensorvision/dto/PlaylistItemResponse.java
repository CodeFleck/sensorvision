package org.sensorvision.dto;

import org.sensorvision.model.DashboardPlaylistItem;

import java.time.Instant;

public record PlaylistItemResponse(
    Long id,
    Long dashboardId,
    String dashboardName,
    Integer position,
    Integer displayDurationSeconds,
    Instant createdAt
) {
    public static PlaylistItemResponse fromEntity(DashboardPlaylistItem item) {
        return new PlaylistItemResponse(
            item.getId(),
            item.getDashboard().getId(),
            item.getDashboard().getName(),
            item.getPosition(),
            item.getDisplayDurationSeconds(),
            item.getCreatedAt()
        );
    }
}
