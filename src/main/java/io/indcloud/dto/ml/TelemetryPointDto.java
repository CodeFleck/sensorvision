package io.indcloud.dto.ml;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryPointDto {

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    @NotEmpty(message = "Variables map cannot be empty")
    private Map<String, Double> variables;
}
