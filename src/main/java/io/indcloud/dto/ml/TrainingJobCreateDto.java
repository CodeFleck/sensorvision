package io.indcloud.dto.ml;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingJobCreateDto {

    @NotNull(message = "Model ID is required")
    private UUID modelId;

    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    private String jobType;
    private Map<String, Object> trainingConfig;
    private Instant trainingDataStart;
    private Instant trainingDataEnd;
}
