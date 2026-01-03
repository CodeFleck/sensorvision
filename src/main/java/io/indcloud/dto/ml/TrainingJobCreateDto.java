package io.indcloud.dto.ml;

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
    private UUID modelId;
    private Long organizationId;
    private String jobType;
    private Map<String, Object> trainingConfig;
    private Instant trainingDataStart;
    private Instant trainingDataEnd;
}
