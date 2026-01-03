package io.indcloud.dto.ml;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InferenceRequestDto {

    @NotNull(message = "Device ID is required")
    private UUID deviceId;

    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    private UUID modelId;

    @NotEmpty(message = "Telemetry data is required")
    @Valid
    private List<TelemetryPointDto> telemetry;
}
