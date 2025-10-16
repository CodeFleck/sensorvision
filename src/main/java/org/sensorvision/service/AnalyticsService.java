package org.sensorvision.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.AggregationResponse;
import org.sensorvision.exception.BadRequestException;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.Organization;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TelemetryRecordRepository telemetryRecordRepository;
    private final DeviceRepository deviceRepository;

    private static final Set<String> VALID_VARIABLES = Set.of(
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
        if (!VALID_VARIABLES.contains(variable)) {
            throw new BadRequestException("Invalid variable: " + variable +
                    ". Valid variables are: " + VALID_VARIABLES);
        }

        // Verify device ownership
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + deviceId);
        }

        // Get telemetry records for the time range
        List<TelemetryRecord> records = telemetryRecordRepository
                .findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(deviceId, from, to);

        if (records.isEmpty()) {
            return List.of();
        }

        // If no interval specified, return single aggregation
        if (interval == null || interval.isEmpty()) {
            AggregationResponse result = calculateAggregation(records, deviceId, variable, aggregation, from);
            return result != null ? List.of(result) : List.of();
        }

        // Calculate interval-based aggregation
        return calculateIntervalAggregation(records, deviceId, variable, aggregation, from, to, interval);
    }

    private List<AggregationResponse> calculateIntervalAggregation(List<TelemetryRecord> records,
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
                AggregationResponse result = calculateAggregation(intervalRecords, deviceId, variable, aggregation, current);
                if (result != null) {
                    results.add(result);
                }
            }

            current = intervalEnd;
        }

        return results;
    }

    private AggregationResponse calculateAggregation(List<TelemetryRecord> records,
                                                    String deviceId,
                                                    String variable,
                                                    String aggregation,
                                                    Instant timestamp) {

        List<BigDecimal> values = extractVariableValues(records, variable);
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

    private List<BigDecimal> extractVariableValues(List<TelemetryRecord> records, String variable) {
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