package io.indcloud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import io.indcloud.dto.AggregationResponse;
import io.indcloud.exception.BadRequestException;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.Device;
import io.indcloud.model.Organization;
import io.indcloud.model.TelemetryRecord;
import io.indcloud.model.Variable;
import io.indcloud.model.VariableValue;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.TelemetryRecordRepository;
import io.indcloud.repository.VariableRepository;
import io.indcloud.repository.VariableValueRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TelemetryRecordRepository telemetryRecordRepository;
    private final DeviceRepository deviceRepository;
    private final VariableRepository variableRepository;
    private final VariableValueRepository variableValueRepository;
    private final SecurityUtils securityUtils;

    // Legacy variables stored in TelemetryRecord table
    private static final Set<String> LEGACY_VARIABLES = Set.of(
            "kwConsumption", "voltage", "current", "powerFactor", "frequency"
    );

    private static final Set<String> VALID_AGGREGATIONS = Set.of(
            "MIN", "MAX", "AVG", "SUM", "COUNT"
    );

    /**
     * Perform aggregation on telemetry data
     */
    public List<AggregationResponse> aggregateData(String deviceId,
                                                  String variable,
                                                  String aggregation,
                                                  Instant from,
                                                  Instant to,
                                                  String interval) {

        // Validate inputs
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new BadRequestException("Device ID is required");
        }
        if (from == null || to == null) {
            throw new BadRequestException("Time range (from and to) is required");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("End time must be after start time");
        }
        if (!VALID_AGGREGATIONS.contains(aggregation.toUpperCase())) {
            throw new BadRequestException("Invalid aggregation type: " + aggregation +
                    ". Valid types are: " + VALID_AGGREGATIONS);
        }
        if (variable == null || variable.trim().isEmpty()) {
            throw new BadRequestException("Variable name is required");
        }

        // Verify device ownership
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + deviceId);
        }

        // Check if this is a legacy variable or a dynamic variable
        if (LEGACY_VARIABLES.contains(variable)) {
            // Use legacy TelemetryRecord-based aggregation
            return aggregateLegacyVariable(device, deviceId, variable, aggregation, from, to, interval);
        } else {
            // Use dynamic variable (EAV pattern) aggregation
            return aggregateDynamicVariable(device, variable, aggregation, from, to, interval);
        }
    }

    /**
     * Aggregate data from legacy TelemetryRecord table
     */
    private List<AggregationResponse> aggregateLegacyVariable(Device device, String deviceId, String variable,
                                                              String aggregation, Instant from, Instant to,
                                                              String interval) {
        // Get telemetry records for the time range
        List<TelemetryRecord> records = telemetryRecordRepository
                .findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(deviceId, from, to);

        if (records.isEmpty()) {
            return List.of();
        }

        // If no interval specified, return single aggregation
        if (interval == null || interval.isEmpty()) {
            AggregationResponse result = calculateLegacyAggregation(records, deviceId, variable, aggregation, from);
            return result != null ? List.of(result) : List.of();
        }

        // Calculate interval-based aggregation
        return calculateLegacyIntervalAggregation(records, deviceId, variable, aggregation, from, to, interval);
    }

    /**
     * Aggregate data from dynamic VariableValue table (EAV pattern)
     */
    private List<AggregationResponse> aggregateDynamicVariable(Device device, String variableName,
                                                               String aggregation, Instant from, Instant to,
                                                               String interval) {
        // Find the dynamic variable for this device
        Optional<Variable> variableOpt = variableRepository.findByDeviceIdAndName(device.getId(), variableName);
        if (variableOpt.isEmpty()) {
            throw new BadRequestException("Variable '" + variableName + "' not found for device. " +
                    "Send telemetry data with this variable first.");
        }
        Variable variable = variableOpt.get();

        // Get variable values for the time range
        List<VariableValue> values = variableValueRepository.findByVariableIdAndTimeRange(
                variable.getId(), from, to);

        if (values.isEmpty()) {
            return List.of();
        }

        // If no interval specified, return single aggregation
        if (interval == null || interval.isEmpty()) {
            AggregationResponse result = calculateDynamicAggregation(values, device.getExternalId(),
                    variableName, aggregation, from);
            return result != null ? List.of(result) : List.of();
        }

        // Calculate interval-based aggregation
        return calculateDynamicIntervalAggregation(values, device.getExternalId(), variableName,
                aggregation, from, to, interval);
    }

    // ========== Legacy TelemetryRecord-based methods ==========

    private List<AggregationResponse> calculateLegacyIntervalAggregation(List<TelemetryRecord> records,
                                                                         String deviceId,
                                                                         String variable,
                                                                         String aggregation,
                                                                         Instant from,
                                                                         Instant to,
                                                                         String interval) {

        long intervalMinutes = parseInterval(interval);
        List<AggregationResponse> results = new ArrayList<>();

        Instant current = from;
        while (current.isBefore(to)) {
            final Instant intervalStart = current;
            final Instant intervalEnd = current.plus(intervalMinutes, ChronoUnit.MINUTES).isAfter(to) ? to : current.plus(intervalMinutes, ChronoUnit.MINUTES);

            // Filter records for this interval
            List<TelemetryRecord> intervalRecords = records.stream()
                    .filter(r -> !r.getTimestamp().isBefore(intervalStart) && r.getTimestamp().isBefore(intervalEnd))
                    .toList();

            if (!intervalRecords.isEmpty()) {
                AggregationResponse result = calculateLegacyAggregation(intervalRecords, deviceId, variable, aggregation, current);
                if (result != null) {
                    results.add(result);
                }
            }

            current = intervalEnd;
        }

        return results;
    }

    private AggregationResponse calculateLegacyAggregation(List<TelemetryRecord> records,
                                                           String deviceId,
                                                           String variable,
                                                           String aggregation,
                                                           Instant timestamp) {

        List<BigDecimal> values = extractLegacyVariableValues(records, variable);
        if (values.isEmpty()) {
            return null;
        }

        BigDecimal result = switch (aggregation.toUpperCase()) {
            case "MIN" -> values.stream().min(BigDecimal::compareTo).orElse(null);
            case "MAX" -> values.stream().max(BigDecimal::compareTo).orElse(null);
            case "AVG" -> calculateAverage(values);
            case "SUM" -> values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case "COUNT" -> BigDecimal.valueOf(values.size());
            default -> throw new IllegalArgumentException("Unsupported aggregation: " + aggregation);
        };

        return new AggregationResponse(
                deviceId,
                variable,
                aggregation,
                timestamp,
                result,
                (long) values.size()
        );
    }

    private List<BigDecimal> extractLegacyVariableValues(List<TelemetryRecord> records, String variable) {
        return records.stream()
                .map(record -> switch (variable) {
                    case "kwConsumption" -> record.getKwConsumption();
                    case "voltage" -> record.getVoltage();
                    case "current" -> record.getCurrent();
                    case "powerFactor" -> record.getPowerFactor();
                    case "frequency" -> record.getFrequency();
                    default -> null;
                })
                .filter(value -> value != null && value.compareTo(BigDecimal.ZERO) != 0)
                .toList();
    }

    // ========== Dynamic VariableValue-based methods (EAV pattern) ==========

    private List<AggregationResponse> calculateDynamicIntervalAggregation(List<VariableValue> values,
                                                                          String deviceId,
                                                                          String variableName,
                                                                          String aggregation,
                                                                          Instant from,
                                                                          Instant to,
                                                                          String interval) {

        long intervalMinutes = parseInterval(interval);
        List<AggregationResponse> results = new ArrayList<>();

        Instant current = from;
        while (current.isBefore(to)) {
            final Instant intervalStart = current;
            final Instant intervalEnd = current.plus(intervalMinutes, ChronoUnit.MINUTES).isAfter(to) ? to : current.plus(intervalMinutes, ChronoUnit.MINUTES);

            // Filter values for this interval
            List<VariableValue> intervalValues = values.stream()
                    .filter(v -> !v.getTimestamp().isBefore(intervalStart) && v.getTimestamp().isBefore(intervalEnd))
                    .toList();

            if (!intervalValues.isEmpty()) {
                AggregationResponse result = calculateDynamicAggregation(intervalValues, deviceId, variableName, aggregation, current);
                if (result != null) {
                    results.add(result);
                }
            }

            current = intervalEnd;
        }

        return results;
    }

    private AggregationResponse calculateDynamicAggregation(List<VariableValue> values,
                                                            String deviceId,
                                                            String variableName,
                                                            String aggregation,
                                                            Instant timestamp) {

        List<BigDecimal> numericValues = values.stream()
                .map(VariableValue::getValue)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) != 0)
                .toList();

        if (numericValues.isEmpty()) {
            return null;
        }

        BigDecimal result = switch (aggregation.toUpperCase()) {
            case "MIN" -> numericValues.stream().min(BigDecimal::compareTo).orElse(null);
            case "MAX" -> numericValues.stream().max(BigDecimal::compareTo).orElse(null);
            case "AVG" -> calculateAverage(numericValues);
            case "SUM" -> numericValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case "COUNT" -> BigDecimal.valueOf(numericValues.size());
            default -> throw new IllegalArgumentException("Unsupported aggregation: " + aggregation);
        };

        return new AggregationResponse(
                deviceId,
                variableName,
                aggregation,
                timestamp,
                result,
                (long) numericValues.size()
        );
    }

    private BigDecimal calculateAverage(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    private long parseInterval(String interval) {
        if (interval == null || interval.isEmpty()) {
            return 60; // Default 1 hour
        }

        // Parse interval string like "5m", "1h", "1d"
        String numPart = interval.replaceAll("[a-zA-Z]", "");
        String unitPart = interval.replaceAll("[0-9]", "");

        long num = numPart.isEmpty() ? 1 : Long.parseLong(numPart);

        return switch (unitPart.toLowerCase()) {
            case "s" -> num / 60; // seconds to minutes
            case "m" -> num; // minutes
            case "h" -> num * 60; // hours to minutes
            case "d" -> num * 24 * 60; // days to minutes
            default -> 60; // default 1 hour
        };
    }
}