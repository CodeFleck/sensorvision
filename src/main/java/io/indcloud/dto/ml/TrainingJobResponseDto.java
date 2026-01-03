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
public class TrainingJobResponseDto {
    private UUID id;
    private UUID modelId;
    private Long organizationId;
    private String jobType;
    private String status;
    private Map<String, Object> trainingConfig;
    private Instant trainingDataStart;
    private Instant trainingDataEnd;
    private Long recordCount;
    private Integer deviceCount;
    private Integer progressPercent;
    private String currentStep;
    private Map<String, Object> resultMetrics;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;
    private Integer durationSeconds;
    private UUID triggeredBy;
    private Instant createdAt;
}
