package io.indcloud.dto.ml;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for updating an existing ML model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLModelUpdateRequest {

    @Size(max = 255, message = "Model name must not exceed 255 characters")
    private String name;

    private Map<String, Object> hyperparameters;

    private List<String> featureColumns;

    @Size(max = 100, message = "Target column must not exceed 100 characters")
    private String targetColumn;

    @Size(max = 30, message = "Device scope must not exceed 30 characters")
    private String deviceScope;

    private List<UUID> deviceIds;

    private Long deviceGroupId;

    @Size(max = 100, message = "Inference schedule must not exceed 100 characters")
    private String inferenceSchedule;

    @DecimalMin(value = "0.0", message = "Confidence threshold must be >= 0")
    @DecimalMax(value = "1.0", message = "Confidence threshold must be <= 1")
    private BigDecimal confidenceThreshold;

    @DecimalMin(value = "0.0", message = "Anomaly threshold must be >= 0")
    @DecimalMax(value = "1.0", message = "Anomaly threshold must be <= 1")
    private BigDecimal anomalyThreshold;
}
