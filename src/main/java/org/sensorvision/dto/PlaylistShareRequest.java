package org.sensorvision.dto;

import java.time.LocalDateTime;

public record PlaylistShareRequest(
    LocalDateTime expiresAt
) {
}
