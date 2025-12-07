package org.sensorvision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Device;
import org.sensorvision.model.Organization;
import org.sensorvision.model.Variable;
import org.sensorvision.model.Variable.DataSource;
import org.sensorvision.model.VariableValue;
import org.sensorvision.repository.VariableRepository;
import org.sensorvision.repository.VariableValueRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing dynamic device-specific variables using the EAV pattern.
 * This enables Ubidots-like functionality where devices can send any variable
 * without requiring schema changes.
 *
 * Key features:
 * - Auto-provisioning of variables when new telemetry arrives
 * - Time-series storage for all variable values
 * - Quick access to latest values via Variable.lastValue
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DynamicVariableService {

    private final VariableRepository variableRepository;
    private final VariableValueRepository variableValueRepository;

    /**
     * Get or create a device-specific variable.
     * This is the core method for auto-provisioning variables.
     * Handles race conditions when multiple threads try to create the same variable.
     *
     * @param device The device this variable belongs to
     * @param variableName The variable name (e.g., "temperature", "custom_sensor_1")
     * @return The existing or newly created variable
     */
    public Variable getOrCreateVariable(Device device, String variableName) {
        return variableRepository.findByDeviceAndName(device, variableName)
                .orElseGet(() -> {
                    try {
                        log.info("Auto-provisioning new variable '{}' for device '{}'",
                                variableName, device.getExternalId());

                        Variable variable = Variable.builder()
                                .device(device)
                                .organization(device.getOrganization())
                                .name(variableName)
                                .displayName(humanizeVariableName(variableName))
                                .dataType(Variable.DataType.NUMBER)
                                .dataSource(DataSource.AUTO)
                                .isSystemVariable(false)
                                .decimalPlaces(2)
                                .build();

                        return variableRepository.save(variable);
                    } catch (DataIntegrityViolationException e) {
                        // Race condition - another thread created it first, fetch the existing one
                        log.debug("Variable '{}' for device '{}' was created by another thread",
                                variableName, device.getExternalId());
                        return variableRepository.findByDeviceAndName(device, variableName)
                                .orElseThrow(() -> new RuntimeException(
                                        "Failed to create or find variable: " + variableName, e));
                    }
                });
    }

    /**
     * Record a value for a variable.
     * This creates the variable if it doesn't exist (auto-provisioning).
     *
     * @param device The device sending the data
     * @param variableName The variable name
     * @param value The value to record
     * @param timestamp When the value was recorded
     * @param context Optional context data (location, quality flags, etc.)
     * @return The recorded variable value
     */
    public VariableValue recordValue(Device device, String variableName, BigDecimal value,
                                     Instant timestamp, Map<String, Object> context) {
        Variable variable = getOrCreateVariable(device, variableName);
        return recordValue(variable, value, timestamp, context);
    }

    /**
     * Record a value for an existing variable.
     *
     * @param variable The variable to record a value for
     * @param value The value to record
     * @param timestamp When the value was recorded
     * @param context Optional context data
     * @return The recorded variable value
     */
    public VariableValue recordValue(Variable variable, BigDecimal value,
                                     Instant timestamp, Map<String, Object> context) {
        // Create the time-series record
        VariableValue variableValue = VariableValue.builder()
                .variable(variable)
                .timestamp(timestamp)
                .value(value)
                .context(context)
                .build();

        VariableValue saved = variableValueRepository.save(variableValue);

        // Update the last value cache on the variable for quick access
        if (variable.getLastValueAt() == null || timestamp.isAfter(variable.getLastValueAt())) {
            variable.setLastValue(value);
            variable.setLastValueAt(timestamp);
            variableRepository.save(variable);
        }

        return saved;
    }

    /**
     * Process a batch of variable values from telemetry payload.
     * This is the main entry point from TelemetryIngestionService.
     *
     * @param device The device sending the telemetry
     * @param variables Map of variable names to values
     * @param timestamp When the telemetry was recorded
     * @param context Optional context data
     * @return Map of variable names to their recorded values
     */
    public Map<String, VariableValue> processTelemetry(Device device,
                                                        Map<String, BigDecimal> variables,
                                                        Instant timestamp,
                                                        Map<String, Object> context) {
        if (variables == null || variables.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, VariableValue> results = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry : variables.entrySet()) {
            String variableName = entry.getKey();
            BigDecimal value = entry.getValue();

            if (value != null) {
                VariableValue recorded = recordValue(device, variableName, value, timestamp, context);
                results.put(variableName, recorded);
            }
        }

        log.debug("Processed {} variables for device '{}'", results.size(), device.getExternalId());
        return results;
    }

    /**
     * Get all variables for a device.
     */
    @Transactional(readOnly = true)
    public List<Variable> getDeviceVariables(UUID deviceId) {
        return variableRepository.findByDeviceIdOrderByNameAsc(deviceId);
    }

    /**
     * Get latest values for all variables of a device.
     * Returns a map of variable name -> latest value.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getLatestValues(UUID deviceId) {
        return variableRepository.findByDeviceId(deviceId).stream()
                .filter(v -> v.getLastValue() != null)
                .collect(Collectors.toMap(
                        Variable::getName,
                        Variable::getLastValue
                ));
    }

    /**
     * Get time-series data for a variable within a time range.
     */
    @Transactional(readOnly = true)
    public List<VariableValue> getVariableHistory(Long variableId, Instant startTime, Instant endTime) {
        return variableValueRepository.findByVariableIdAndTimeRange(variableId, startTime, endTime);
    }

    /**
     * Get the latest N values for a variable.
     */
    @Transactional(readOnly = true)
    public List<VariableValue> getLatestValues(Long variableId, int count) {
        return variableValueRepository.findLatestByVariableId(variableId, PageRequest.of(0, count));
    }

    /**
     * Get aggregated statistics for a variable within a time range.
     */
    @Transactional(readOnly = true)
    public VariableStatistics getStatistics(Long variableId, Instant startTime, Instant endTime) {
        Double avg = variableValueRepository.calculateAverageByVariableIdAndTimeRange(variableId, startTime, endTime);
        Double min = variableValueRepository.calculateMinByVariableIdAndTimeRange(variableId, startTime, endTime);
        Double max = variableValueRepository.calculateMaxByVariableIdAndTimeRange(variableId, startTime, endTime);
        Double sum = variableValueRepository.calculateSumByVariableIdAndTimeRange(variableId, startTime, endTime);
        long count = variableValueRepository.countByVariableIdAndTimeRange(variableId, startTime, endTime);

        return new VariableStatistics(avg, min, max, sum, count);
    }

    /**
     * Convert snake_case or camelCase variable names to human-readable display names.
     * Examples:
     * - "temperature" -> "Temperature"
     * - "kw_consumption" -> "Kw Consumption"
     * - "powerFactor" -> "Power Factor"
     */
    private String humanizeVariableName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }

        // Replace underscores with spaces
        String result = name.replace("_", " ");

        // Insert space before uppercase letters (for camelCase)
        result = result.replaceAll("([a-z])([A-Z])", "$1 $2");

        // Capitalize first letter of each word
        String[] words = result.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append(" ");
            String word = words[i];
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }

        return sb.toString();
    }

    /**
     * Statistics record for variable aggregations.
     */
    public record VariableStatistics(
            Double average,
            Double min,
            Double max,
            Double sum,
            long count
    ) {}
}
