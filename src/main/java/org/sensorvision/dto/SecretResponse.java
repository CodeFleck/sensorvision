package org.sensorvision.dto;

import java.time.Instant;

public record SecretResponse(
    Long id,
    String secretKey,
    Instant createdAt,
    Instant updatedAt
) {
}
