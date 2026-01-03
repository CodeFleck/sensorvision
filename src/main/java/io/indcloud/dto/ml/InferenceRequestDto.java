package io.indcloud.dto.ml;

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
    private UUID deviceId;
    private Long organizationId;
    private UUID modelId;
    private List<TelemetryPointDto> telemetry;
}
