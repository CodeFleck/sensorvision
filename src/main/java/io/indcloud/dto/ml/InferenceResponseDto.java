package io.indcloud.dto.ml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InferenceResponseDto {
    private UUID deviceId;
    private UUID modelId;
    private String predictionType;
    private BigDecimal predictionValue;
    private String predictionLabel;
    private BigDecimal confidence;
    private Map<String, Object> predictionDetails;
    private Instant predictionTimestamp;
    private String predictionHorizon;
    private Instant validUntil;
}
