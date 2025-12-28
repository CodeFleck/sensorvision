package io.indcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AggregationRequest(
        @NotBlank String deviceId,
        @NotBlank String variable,
        @NotBlank String aggregation, // MIN, MAX, AVG, SUM, COUNT
        @NotNull Instant from,
        @NotNull Instant to,
        String interval // Optional: 1m, 5m, 15m, 1h, 1d
) {
}