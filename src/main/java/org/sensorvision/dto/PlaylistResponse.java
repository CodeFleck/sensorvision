package org.sensorvision.dto;

import org.sensorvision.model.DashboardPlaylist;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PlaylistResponse(
    Long id,
    String name,
    String description,
    String createdByEmail,
    Boolean isPublic,
    String publicShareToken,
    LocalDateTime shareExpiresAt,
    Boolean loopEnabled,
    String transitionEffect,
    List<PlaylistItemResponse> items,
    Instant createdAt,
    Instant updatedAt
) {
    public static PlaylistResponse fromEntity(DashboardPlaylist playlist) {
        return new PlaylistResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getCreatedBy() != null ? playlist.getCreatedBy().getEmail() : null,
            playlist.getIsPublic(),
            playlist.getPublicShareToken(),
            playlist.getShareExpiresAt(),
            playlist.getLoopEnabled(),
            playlist.getTransitionEffect(),
            playlist.getItems().stream()
                .map(PlaylistItemResponse::fromEntity)
                .collect(Collectors.toList()),
            playlist.getCreatedAt(),
            playlist.getUpdatedAt()
        );
    }

    public static PlaylistResponse fromEntityWithoutItems(DashboardPlaylist playlist) {
        return new PlaylistResponse(
            playlist.getId(),
            playlist.getName(),
            playlist.getDescription(),
            playlist.getCreatedBy() != null ? playlist.getCreatedBy().getEmail() : null,
            playlist.getIsPublic(),
            playlist.getPublicShareToken(),
            playlist.getShareExpiresAt(),
            playlist.getLoopEnabled(),
            playlist.getTransitionEffect(),
            null,
            playlist.getCreatedAt(),
            playlist.getUpdatedAt()
        );
    }
}
