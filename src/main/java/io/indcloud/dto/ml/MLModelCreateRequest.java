package io.indcloud.dto.ml;

import io.indcloud.model.MLModelType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * Request DTO for creating a new ML model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLModelCreateRequest {

    @NotBlank(message = "Model name is required")
    @Size(max = 255, message = "Model name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Model type is required")
    private MLModelType modelType;

    @NotBlank(message = "Algorithm is required")
    @Size(max = 100, message = "Algorithm name must not exceed 100 characters")
    private String algorithm;

    @Size(max = 50, message = "Version must not exceed 50 characters")
    @Builder.Default
    private String version = "1.0.0";

    private Map<String, Object> hyperparameters;

    private List<String> featureColumns;

    @Size(max = 100, message = "Target column must not exceed 100 characters")
    private String targetColumn;

    @Size(max = 30, message = "Device scope must not exceed 30 characters")
    @Builder.Default
    private String deviceScope = "ALL";

    private List<UUID> deviceIds;

    private Long deviceGroupId;

    @Size(max = 100, message = "Inference schedule must not exceed 100 characters")
    private String inferenceSchedule;

    @DecimalMin(value = "0.0", message = "Confidence threshold must be >= 0")
    @DecimalMax(value = "1.0", message = "Confidence threshold must be <= 1")
    @Builder.Default
    private BigDecimal confidenceThreshold = new BigDecimal("0.8");

    @DecimalMin(value = "0.0", message = "Anomaly threshold must be >= 0")
    @DecimalMax(value = "1.0", message = "Anomaly threshold must be <= 1")
    @Builder.Default
    private BigDecimal anomalyThreshold = new BigDecimal("0.5");
}
