package io.indcloud.dto.ml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLModelResponseDto {
    private UUID id;
    private Long organizationId;
    private String name;
    private String modelType;
    private String version;
    private String algorithm;
    private Map<String, Object> hyperparameters;
    private List<String> featureColumns;
    private String targetColumn;
    private String status;
    private String modelPath;
    private Long modelSizeBytes;
    private Map<String, Object> trainingMetrics;
    private Map<String, Object> validationMetrics;
    private String deviceScope;
    private List<UUID> deviceIds;
    private Long deviceGroupId;
    private String inferenceSchedule;
    private Instant lastInferenceAt;
    private Instant nextInferenceAt;
    private BigDecimal confidenceThreshold;
    private BigDecimal anomalyThreshold;
    private UUID createdBy;
    private UUID trainedBy;
    private Instant trainedAt;
    private Instant deployedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
