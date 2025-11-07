package org.sensorvision.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.expression.ExpressionEvaluator;
import org.sensorvision.model.SyntheticVariable;
import org.sensorvision.model.SyntheticVariableValue;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.repository.SyntheticVariableRepository;
import org.sensorvision.repository.SyntheticVariableValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SyntheticVariableService {

    private final SyntheticVariableRepository syntheticVariableRepository;
    private final SyntheticVariableValueRepository syntheticVariableValueRepository;
    private final ExpressionEvaluator expressionEvaluator;

    /**
     * Calculate synthetic variables for a telemetry record.
     * Now supports advanced expressions with math functions, logic, and comparisons.
     */
    public void calculateSyntheticVariables(TelemetryRecord telemetryRecord) {
        List<SyntheticVariable> enabledVariables = syntheticVariableRepository
                .findByDeviceExternalIdAndEnabledTrue(telemetryRecord.getDevice().getExternalId());

        if (enabledVariables.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> telemetryValues = extractTelemetryValues(telemetryRecord);

        for (SyntheticVariable syntheticVariable : enabledVariables) {
            try {
                BigDecimal calculatedValue = expressionEvaluator.evaluate(
                        syntheticVariable.getExpression(),
                        telemetryValues
                );

                if (calculatedValue != null) {
                    SyntheticVariableValue value = SyntheticVariableValue.builder()
                            .syntheticVariable(syntheticVariable)
                            .telemetryRecord(telemetryRecord)
                            .calculatedValue(calculatedValue)
                            .timestamp(telemetryRecord.getTimestamp())
                            .build();

                    syntheticVariableValueRepository.save(value);
                    log.debug("Calculated synthetic variable '{}' = {} for device {}",
                            syntheticVariable.getName(),
                            calculatedValue,
                            telemetryRecord.getDevice().getExternalId());
                }
            } catch (Exception e) {
                log.error("Failed to calculate synthetic variable '{}' for device {}: {}",
                        syntheticVariable.getName(),
                        telemetryRecord.getDevice().getExternalId(),
                        e.getMessage());
            }
        }
    }

    /**
     * Extract telemetry values from a record into a map for expression evaluation.
     */
    private Map<String, BigDecimal> extractTelemetryValues(TelemetryRecord record) {
        return Map.of(
                "kwConsumption", record.getKwConsumption() != null ? record.getKwConsumption() : BigDecimal.ZERO,
                "voltage", record.getVoltage() != null ? record.getVoltage() : BigDecimal.ZERO,
                "current", record.getCurrent() != null ? record.getCurrent() : BigDecimal.ZERO,
                "powerFactor", record.getPowerFactor() != null ? record.getPowerFactor() : BigDecimal.ZERO,
                "frequency", record.getFrequency() != null ? record.getFrequency() : BigDecimal.ZERO
        );
    }
}
