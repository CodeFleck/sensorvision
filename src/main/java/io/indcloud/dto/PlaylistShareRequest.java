package io.indcloud.dto;

import java.time.LocalDateTime;

public record PlaylistShareRequest(
    LocalDateTime expiresAt
) {
}
