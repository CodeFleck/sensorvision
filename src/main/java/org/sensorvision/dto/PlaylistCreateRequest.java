package org.sensorvision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCreateRequest(
    @NotBlank(message = "Playlist name is required")
    @Size(max = 255, message = "Playlist name must not exceed 255 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    Boolean loopEnabled,

    @Size(max = 50, message = "Transition effect must not exceed 50 characters")
    String transitionEffect
) {
}
