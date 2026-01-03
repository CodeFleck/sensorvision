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
public class PredictiveMaintenanceResultDto extends InferenceResponseDto {
    private BigDecimal maintenanceProbability;
    private Integer daysToMaintenance;
    private List<String> recommendedActions;
    private Map<String, Double> riskFactors;
}
