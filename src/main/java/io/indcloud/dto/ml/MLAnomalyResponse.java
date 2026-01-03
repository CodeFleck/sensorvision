package io.indcloud.dto.ml;

import io.indcloud.model.MLAnomalySeverity;
import io.indcloud.model.MLAnomalyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for ML anomaly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLAnomalyResponse {

    private UUID id;
    private UUID predictionId;
    private UUID deviceId;
    private String deviceName;
    private Long organizationId;
    private BigDecimal anomalyScore;
    private MLAnomalySeverity severity;
    private MLAnomalyStatus status;
    private String anomalyType;
    private List<String> affectedVariables;
    private Map<String, Object> expectedValues;
    private Map<String, Object> actualValues;
    private String contextWindow;
    private Instant detectedAt;
    private Long acknowledgedBy;
    private Instant acknowledgedAt;
    private Long resolvedBy;
    private Instant resolvedAt;
    private String resolutionNote;
    private UUID globalAlertId;
    private Instant createdAt;

    // Prediction details
    private String predictionType;
    private BigDecimal predictionConfidence;
}
