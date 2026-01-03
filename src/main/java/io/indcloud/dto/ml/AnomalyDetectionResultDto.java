package io.indcloud.dto.ml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AnomalyDetectionResultDto extends InferenceResponseDto {
    private BigDecimal anomalyScore;
    private boolean isAnomaly;
    private String severity;
    private List<String> affectedVariables;
    private Map<String, Double> expectedValues;
    private Map<String, Double> actualValues;
}
