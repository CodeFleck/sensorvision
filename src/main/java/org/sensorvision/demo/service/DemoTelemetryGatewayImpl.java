package org.sensorvision.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.demo.cache.RollingTelemetryCache;
import org.sensorvision.demo.cache.TelemetryPoint;
import org.sensorvision.demo.config.DemoModeProperties;
import org.sensorvision.model.*;
import org.sensorvision.repository.OrganizationRepository;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.sensorvision.service.DeviceService;
import org.sensorvision.websocket.TelemetryWebSocketHandler;
import org.sensorvision.dto.TelemetryPointDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Implementation of DemoTelemetryGateway that integrates with SensorVision core services.
 *
 * This adapter connects the demo data generator to the existing application infrastructure:
 * - DeviceService: For device creation/lookup
 * - TelemetryRecordRepository: For data persistence
 * - RollingTelemetryCache: For fast in-memory queries
 * - TelemetryWebSocketHandler: For real-time broadcasts
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo", name = "mode-enabled", havingValue = "true")
public class DemoTelemetryGatewayImpl implements DemoTelemetryGateway {

    private final DemoModeProperties properties;
    private final RollingTelemetryCache cache;
    private final DeviceService deviceService;
    private final OrganizationRepository organizationRepository;
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final TelemetryWebSocketHandler webSocketHandler;

    @Override
    @Transactional
    public String ensureDemoDevice(String externalId) {
        // Get or create demo organization
        Organization demoOrg = organizationRepository
                .findByName(properties.getOrganizationName())
                .orElseGet(() -> {
                    log.info("Creating demo organization: {}", properties.getOrganizationName());
                    return organizationRepository.save(
                            Organization.builder()
                                    .name(properties.getOrganizationName())
                                    .description("Demo organization for manufacturing sensor demonstrations")
                                    .enabled(true)
                                    .build()
                    );
                });

        // Use DeviceService's existing auto-provisioning logic
        Device device = deviceService.getOrCreateDevice(externalId, demoOrg);

        // Update device metadata for demo
        device.setSensorType("manufacturing_sensor");
        device.setLocation("Demo Facility - Production Floor");
        device.setName("Demo Machine " + externalId.substring(externalId.lastIndexOf('-') + 1));

        log.debug("Demo device ensured: {} (ID: {})", externalId, device.getId());
        return device.getId().toString();
    }

    @Override
    @Transactional
    public void ingestDemoTelemetry(String deviceExternalId, Map<String, Object> payload, boolean anomaly) {
        try {
            long timestamp = (Long) payload.getOrDefault("timestamp", System.currentTimeMillis());
            Instant instant = Instant.ofEpochMilli(timestamp);

            // 1. Add to rolling cache for fast queries
            TelemetryPoint point = new TelemetryPoint(instant, payload);
            cache.addPoint(deviceExternalId, point);

            // 2. Persist to database using TelemetryRecord with custom_variables
            Organization demoOrg = organizationRepository
                    .findByName(properties.getOrganizationName())
                    .orElseThrow(() -> new IllegalStateException("Demo organization not found"));

            Device device = deviceService.getOrCreateDevice(deviceExternalId, demoOrg);

            // Update device status
            device.setStatus(DeviceStatus.ONLINE);
            device.setLastSeenAt(instant);

            // Create telemetry record with demo data in custom_variables
            TelemetryRecord record = TelemetryRecord.builder()
                    .device(device)
                    .organization(demoOrg)
                    .timestamp(instant)
                    .customVariables(payload)  // Store demo metrics here
                    .build();

            telemetryRecordRepository.save(record);

            // 3. Broadcast via WebSocket for real-time dashboard updates
            TelemetryPointDto telemetryDto = buildTelemetryDto(deviceExternalId, instant, payload);
            webSocketHandler.broadcastTelemetryData(telemetryDto, demoOrg.getId());

            // 4. Log anomalies (simplified alert handling for demo)
            if (anomaly) {
                handleAnomaly(deviceExternalId, payload);
            }

        } catch (Exception e) {
            log.error("Failed to ingest demo telemetry for device: {}", deviceExternalId, e);
        }
    }

    /**
     * Build TelemetryPointDto for WebSocket broadcast.
     * Maps demo variables to DTO structure.
     */
    private TelemetryPointDto buildTelemetryDto(String deviceExternalId, Instant timestamp, Map<String, Object> payload) {
        // For demo mode, we'll use the existing DTO but populate with demo values
        // The frontend can be enhanced to display custom_variables
        Double temperature = getDoubleValue(payload, "temperature");
        Double vibration = getDoubleValue(payload, "vibration");
        Double rpm = getDoubleValue(payload, "rpm");
        Double pressure = getDoubleValue(payload, "pressure");

        return new TelemetryPointDto(
                deviceExternalId,
                timestamp,
                temperature,  // Map to kwConsumption for now
                vibration,    // Map to voltage
                rpm,          // Map to current
                pressure,     // Map to powerFactor
                null          // frequency
        );
    }

    /**
     * Handle anomaly detection for demo mode.
     * Logs anomalies - can be extended to create actual alerts.
     */
    private void handleAnomaly(String deviceExternalId, Map<String, Object> payload) {
        Double temperature = getDoubleValue(payload, "temperature");
        Double vibration = getDoubleValue(payload, "vibration");

        String message = buildAnomalyMessage(temperature, vibration);

        log.warn("🚨 ANOMALY DETECTED - Device: {} - {}", deviceExternalId, message);

        // TODO: Create actual Alert entity if needed
        // This requires creating a demo Rule or modifying AlertService to support rule-less alerts
        // For now, logging provides visibility during demos
    }

    /**
     * Build human-readable anomaly message
     */
    private String buildAnomalyMessage(Double temperature, Double vibration) {
        StringBuilder msg = new StringBuilder();

        if (temperature != null && temperature >= properties.getAnomalyTemperatureThreshold()) {
            msg.append(String.format("High temperature: %.1f°C", temperature));
        }

        if (vibration != null && vibration >= properties.getAnomalyVibrationThreshold()) {
            if (msg.length() > 0) {
                msg.append(", ");
            }
            msg.append(String.format("High vibration: %.1f mm/s", vibration));
        }

        if (msg.length() == 0) {
            msg.append("Anomalous pattern detected");
        }

        return msg.toString();
    }

    /**
     * Extract Double value from payload map
     */
    private Double getDoubleValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
