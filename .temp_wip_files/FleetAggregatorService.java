package org.sensorvision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.*;
import org.sensorvision.repository.AlertRepository;
import org.sensorvision.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for aggregating fleet-wide metrics across multiple devices
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FleetAggregatorService {

    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final EntityManager entityManager;

    private static final int ONLINE_THRESHOLD_MINUTES = 5;

    /**
     * Select devices based on global rule selector criteria
     */
    public List<Device> selectDevices(GlobalRule rule) {
        return selectDevices(rule.getOrganization().getId(), rule.getSelectorType(), rule.getSelectorValue());
    }

    /**
     * Select devices based on selector criteria
     */
    public List<Device> selectDevices(Long organizationId, DeviceSelectorType selectorType, String selectorValue) {
        return switch (selectorType) {
            case ORGANIZATION -> selectByOrganization(organizationId);
            case TAG -> selectByTag(organizationId, selectorValue);
            case GROUP -> selectByGroup(organizationId, selectorValue);
            case CUSTOM_FILTER -> selectByCustomFilter(organizationId, selectorValue);
        };
    }

    /**
     * Calculate aggregation value for a global rule
     */
    public AggregationResult calculateAggregation(GlobalRule rule) {
        List<Device> devices = selectDevices(rule);

        if (devices.isEmpty()) {
            return AggregationResult.builder()
                    .value(BigDecimal.ZERO)
                    .deviceCount(0)
                    .affectedDevices(Collections.emptyList())
                    .build();
        }

        FleetAggregationFunction function = FleetAggregationFunction.fromString(rule.getAggregationFunction());

        return switch (function) {
            case COUNT_DEVICES -> countDevices(devices);
            case COUNT_ONLINE -> countOnline(devices);
            case COUNT_OFFLINE -> countOffline(devices);
            case COUNT_ALERTING -> countAlerting(devices);
            case PERCENT_ONLINE -> percentOnline(devices);
            case PERCENT_OFFLINE -> percentOffline(devices);
            case SUM -> sum(devices, rule.getAggregationVariable());
            case AVG -> avg(devices, rule.getAggregationVariable());
            case MIN -> min(devices, rule.getAggregationVariable());
            case MAX -> max(devices, rule.getAggregationVariable());
            case STDDEV -> stddev(devices, rule.getAggregationVariable());
            case PERCENTILE -> percentile(devices, rule.getAggregationVariable(), rule.getAggregationParams());
            case COUNT_DEVICES_WHERE -> countDevicesWhere(devices, rule.getAggregationParams());
            case AVG_UPTIME_HOURS -> avgUptimeHours(devices, rule.getAggregationParams());
        };
    }

    // === Device Selection Methods ===

    private List<Device> selectByOrganization(Long organizationId) {
        return deviceRepository.findByOrganizationIdAndActiveTrue(organizationId);
    }

    private List<Device> selectByTag(Long organizationId, String tagName) {
        if (tagName == null || tagName.isBlank()) {
            log.warn("Tag name is empty, returning no devices");
            return Collections.emptyList();
        }

        String jpql = "SELECT DISTINCT d FROM Device d " +
                     "JOIN d.tags t " +
                     "WHERE d.organization.id = :orgId " +
                     "AND t.name = :tagName " +
                     "AND d.active = true";

        return entityManager.createQuery(jpql, Device.class)
                .setParameter("orgId", organizationId)
                .setParameter("tagName", tagName)
                .getResultList();
    }

    private List<Device> selectByGroup(Long organizationId, String groupId) {
        if (groupId == null || groupId.isBlank()) {
            log.warn("Group ID is empty, returning no devices");
            return Collections.emptyList();
        }

        try {
            Long groupIdLong = Long.parseLong(groupId);
            String jpql = "SELECT DISTINCT d FROM Device d " +
                         "JOIN d.groups g " +
                         "WHERE d.organization.id = :orgId " +
                         "AND g.id = :groupId " +
                         "AND d.active = true";

            return entityManager.createQuery(jpql, Device.class)
                    .setParameter("orgId", organizationId)
                    .setParameter("groupId", groupIdLong)
                    .getResultList();
        } catch (NumberFormatException e) {
            log.error("Invalid group ID format: {}", groupId);
            return Collections.emptyList();
        }
    }

    private List<Device> selectByCustomFilter(Long organizationId, String filterExpression) {
        if (filterExpression == null || filterExpression.isBlank()) {
            log.warn("Custom filter is empty, returning all devices");
            return selectByOrganization(organizationId);
        }

        // Parse simple custom filter expressions like "location=warehouse-1" or "status=ONLINE"
        // For production, this should use a proper query parser
        try {
            String[] parts = filterExpression.split("=", 2);
            if (parts.length != 2) {
                log.error("Invalid filter format: {}", filterExpression);
                return Collections.emptyList();
            }

            String field = parts[0].trim();
            String value = parts[1].trim();

            String jpql = switch (field.toLowerCase()) {
                case "location" -> "SELECT d FROM Device d WHERE d.organization.id = :orgId " +
                                   "AND d.location = :value AND d.active = true";
                case "status" -> "SELECT d FROM Device d WHERE d.organization.id = :orgId " +
                                "AND d.status = :value AND d.active = true";
                case "sensortype" -> "SELECT d FROM Device d WHERE d.organization.id = :orgId " +
                                     "AND d.sensorType = :value AND d.active = true";
                default -> {
                    log.error("Unsupported filter field: {}", field);
                    yield null;
                }
            };

            if (jpql == null) {
                return Collections.emptyList();
            }

            Query query = entityManager.createQuery(jpql, Device.class)
                    .setParameter("orgId", organizationId);

            if (field.equalsIgnoreCase("status")) {
                query.setParameter("value", DeviceStatus.valueOf(value.toUpperCase()));
            } else {
                query.setParameter("value", value);
            }

            return query.getResultList();
        } catch (Exception e) {
            log.error("Error parsing custom filter: {}", filterExpression, e);
            return Collections.emptyList();
        }
    }

    // === Count-Based Aggregation Methods ===

    private AggregationResult countDevices(List<Device> devices) {
        return AggregationResult.builder()
                .value(BigDecimal.valueOf(devices.size()))
                .deviceCount(devices.size())
                .affectedDevices(extractDeviceIds(devices))
                .build();
    }

    private AggregationResult countOnline(List<Device> devices) {
        Instant threshold = Instant.now().minus(ONLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        List<Device> onlineDevices = devices.stream()
                .filter(d -> d.getLastSeenAt() != null && d.getLastSeenAt().isAfter(threshold))
                .toList();

        return AggregationResult.builder()
                .value(BigDecimal.valueOf(onlineDevices.size()))
                .deviceCount(devices.size())
                .affectedDevices(extractDeviceIds(onlineDevices))
                .build();
    }

    private AggregationResult countOffline(List<Device> devices) {
        Instant threshold = Instant.now().minus(ONLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        List<Device> offlineDevices = devices.stream()
                .filter(d -> d.getLastSeenAt() == null || d.getLastSeenAt().isBefore(threshold))
                .toList();

        return AggregationResult.builder()
                .value(BigDecimal.valueOf(offlineDevices.size()))
                .deviceCount(devices.size())
                .affectedDevices(extractDeviceIds(offlineDevices))
                .build();
    }

    private AggregationResult countAlerting(List<Device> devices) {
        List<UUID> deviceIds = extractDeviceIds(devices);

        // Count devices with unacknowledged alerts
        String jpql = "SELECT DISTINCT a.device.id FROM Alert a " +
                     "WHERE a.device.id IN :deviceIds " +
                     "AND a.acknowledged = false";

        @SuppressWarnings("unchecked")
        List<UUID> alertingDeviceIds = entityManager.createQuery(jpql)
                .setParameter("deviceIds", deviceIds)
                .getResultList();

        return AggregationResult.builder()
                .value(BigDecimal.valueOf(alertingDeviceIds.size()))
                .deviceCount(devices.size())
                .affectedDevices(alertingDeviceIds)
                .build();
    }

    private AggregationResult percentOnline(List<Device> devices) {
        if (devices.isEmpty()) {
            return AggregationResult.builder()
                    .value(BigDecimal.ZERO)
                    .deviceCount(0)
                    .affectedDevices(Collections.emptyList())
                    .build();
        }

        AggregationResult onlineResult = countOnline(devices);
        BigDecimal percentage = onlineResult.getValue()
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(devices.size()), 2, RoundingMode.HALF_UP);

        return AggregationResult.builder()
                .value(percentage)
                .deviceCount(devices.size())
                .affectedDevices(onlineResult.getAffectedDevices())
                .build();
    }

    private AggregationResult percentOffline(List<Device> devices) {
        if (devices.isEmpty()) {
            return AggregationResult.builder()
                    .value(BigDecimal.ZERO)
                    .deviceCount(0)
                    .affectedDevices(Collections.emptyList())
                    .build();
        }

        AggregationResult offlineResult = countOffline(devices);
        BigDecimal percentage = offlineResult.getValue()
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(devices.size()), 2, RoundingMode.HALF_UP);

        return AggregationResult.builder()
                .value(percentage)
                .deviceCount(devices.size())
                .affectedDevices(offlineResult.getAffectedDevices())
                .build();
    }

    // === Metric-Based Aggregation Methods ===

    private AggregationResult sum(List<Device> devices, String variable) {
        Map<UUID, BigDecimal> latestValues = getLatestTelemetryValues(devices, variable);
        BigDecimal sum = latestValues.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AggregationResult.builder()
                .value(sum)
                .deviceCount(devices.size())
                .affectedDevices(new ArrayList<>(latestValues.keySet()))
                .build();
    }

    private AggregationResult avg(List<Device> devices, String variable) {
        Map<UUID, BigDecimal> latestValues = getLatestTelemetryValues(devices, variable);

        if (latestValues.isEmpty()) {
            return AggregationResult.builder()
                    .value(BigDecimal.ZERO)
                    .deviceCount(devices.size())
                    .affectedDevices(Collections.emptyList())
                    .build();
        }

        BigDecimal sum = latestValues.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(latestValues.size()), 6, RoundingMode.HALF_UP);

        return AggregationResult.builder()
                .value(avg)
                .deviceCount(devices.size())
                .affectedDevices(new ArrayList<>(latestValues.keySet()))
                .build();
    }

    private AggregationResult min(List<Device> devices, String variable) {
        Map<UUID, BigDecimal> latestValues = getLatestTelemetryValues(devices, variable);

        Optional<BigDecimal> min = latestValues.values().stream().min(BigDecimal::compareTo);

        return AggregationResult.builder()
                .value(min.orElse(BigDecimal.ZERO))
                .deviceCount(devices.size())
                .affectedDevices(new ArrayList<>(latestValues.keySet()))
                .build();
    }

    private AggregationResult max(List<Device> devices, String variable) {
        Map<UUID, BigDecimal> latestValues = getLatestTelemetryValues(devices, variable);

        Optional<BigDecimal> max = latestValues.values().stream().max(BigDecimal::compareTo);

        return AggregationResult.builder()
                .value(max.orElse(BigDecimal.ZERO))
                .deviceCount(devices.size())
                .affectedDevices(new ArrayList<>(latestValues.keySet()))
                .build();
    }

    private AggregationResult stddev(List<Device> devices, String variable) {
        Map<UUID, BigDecimal> latestValues = getLatestTelemetryValues(devices, variable);

        if (latestValues.size() < 2) {
            return AggregationResult.builder()
                    .value(BigDecimal.ZERO)
                    .deviceCount(devices.size())
                    .affectedDevices(new ArrayList<>(latestValues.keySet()))
                    .build();
        }

        // Calculate mean
        BigDecimal sum = latestValues.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(latestValues.size()), 6, RoundingMode.HALF_UP);

        // Calculate variance
        BigDecimal variance = latestValues.values().stream()
                .map(val -> val.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(latestValues.size()), 6, RoundingMode.HALF_UP);

        // Calculate standard deviation
        double stddevDouble = Math.sqrt(variance.doubleValue());
        BigDecimal stddev = BigDecimal.valueOf(stddevDouble).setScale(6, RoundingMode.HALF_UP);

        return AggregationResult.builder()
                .value(stddev)
                .deviceCount(devices.size())
                .affectedDevices(new ArrayList<>(latestValues.keySet()))
                .build();
    }

    private AggregationResult percentile(List<Device> devices, String variable, Map<String, Object> params) {
        Map<UUID, BigDecimal> latestValues = getLatestTelemetryValues(devices, variable);

        if (latestValues.isEmpty()) {
            return AggregationResult.builder()
                    .value(BigDecimal.ZERO)
                    .deviceCount(devices.size())
                    .affectedDevices(Collections.emptyList())
                    .build();
        }

        // Get percentile parameter (default to 95th percentile)
        int percentileValue = params != null && params.containsKey("percentile") ?
                ((Number) params.get("percentile")).intValue() : 95;

        List<BigDecimal> sortedValues = latestValues.values().stream()
                .sorted()
                .toList();

        int index = (int) Math.ceil(percentileValue / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));

        return AggregationResult.builder()
                .value(sortedValues.get(index))
                .deviceCount(devices.size())
                .affectedDevices(new ArrayList<>(latestValues.keySet()))
                .build();
    }

    // === Time-Window Aggregation Methods ===

    private AggregationResult countDevicesWhere(List<Device> devices, Map<String, Object> params) {
        // Simplified implementation - in production, parse condition from params
        // For now, just return count of all devices
        return countDevices(devices);
    }

    private AggregationResult avgUptimeHours(List<Device> devices, Map<String, Object> params) {
        // Simplified implementation - calculate based on last seen
        Instant now = Instant.now();

        List<Long> uptimeHours = devices.stream()
                .filter(d -> d.getLastSeenAt() != null)
                .map(d -> ChronoUnit.HOURS.between(d.getCreatedAt(), now))
                .toList();

        if (uptimeHours.isEmpty()) {
            return AggregationResult.builder()
                    .value(BigDecimal.ZERO)
                    .deviceCount(devices.size())
                    .affectedDevices(Collections.emptyList())
                    .build();
        }

        double avg = uptimeHours.stream().mapToLong(Long::longValue).average().orElse(0.0);

        return AggregationResult.builder()
                .value(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP))
                .deviceCount(devices.size())
                .affectedDevices(extractDeviceIds(devices))
                .build();
    }

    // === Helper Methods ===

    private Map<UUID, BigDecimal> getLatestTelemetryValues(List<Device> devices, String variable) {
        if (variable == null || variable.isBlank()) {
            return Collections.emptyMap();
        }

        List<UUID> deviceIds = extractDeviceIds(devices);

        if (deviceIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Map variable name to column name
        String columnName = mapVariableToColumn(variable);
        if (columnName == null) {
            log.error("Unknown variable: {}", variable);
            return Collections.emptyMap();
        }

        // Get latest telemetry value for each device
        String sql = "SELECT DISTINCT ON (device_id) device_id, " + columnName + " " +
                    "FROM telemetry_records " +
                    "WHERE device_id = ANY(:deviceIds) " +
                    "AND " + columnName + " IS NOT NULL " +
                    "ORDER BY device_id, measurement_timestamp DESC";

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("deviceIds", deviceIds.toArray(new UUID[0]))
                .getResultList();

        Map<UUID, BigDecimal> values = new HashMap<>();
        for (Object[] row : results) {
            UUID deviceId = (UUID) row[0];
            BigDecimal value = (BigDecimal) row[1];
            values.put(deviceId, value);
        }

        return values;
    }

    private String mapVariableToColumn(String variable) {
        return switch (variable.toLowerCase()) {
            case "kwconsumption" -> "kw_consumption";
            case "voltage" -> "voltage";
            case "current" -> "current";
            case "powerfactor" -> "power_factor";
            case "frequency" -> "frequency";
            default -> null;
        };
    }

    private List<UUID> extractDeviceIds(List<Device> devices) {
        return devices.stream()
                .map(Device::getId)
                .collect(Collectors.toList());
    }

    /**
     * Result object for aggregation calculations
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AggregationResult {
        private BigDecimal value;
        private int deviceCount;
        private List<UUID> affectedDevices;
    }
}
