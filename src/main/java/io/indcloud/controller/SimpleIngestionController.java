package io.indcloud.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.Device;
import io.indcloud.model.Organization;
import io.indcloud.mqtt.TelemetryPayload;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.service.DeviceTokenService;
import io.indcloud.service.TelemetryIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Simplified telemetry ingestion endpoint for developers who want the simplest possible integration.
 *
 * This controller provides a single, ultra-simple endpoint that:
 * - Accepts device API key via X-API-Key header
 * - Accepts direct variable key-value pairs (no nesting required)
 * - Auto-creates devices on first data send
 * - Requires minimal code from IoT devices
 *
 * Example usage:
 * <pre>
 * curl -X POST http://localhost:8080/api/v1/ingest/sensor-001 \
 *   -H "X-API-Key: 550e8400-e29b-41d4-a716-446655440000" \
 *   -H "Content-Type: application/json" \
 *   -d '{"temperature": 23.5, "humidity": 65.2}'
 * </pre>
 *
 * This is the recommended endpoint for:
 * - Beginners and non-technical users
 * - Quick prototypes and demos
 * - Embedded devices with limited memory
 * - Simple sensor scripts
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class SimpleIngestionController {

    private final TelemetryIngestionService telemetryIngestionService;
    private final DeviceTokenService deviceTokenService;
    private final DeviceRepository deviceRepository;

    /**
     * Ultra-simple telemetry ingestion endpoint.
     *
     * POST /api/v1/ingest/{deviceId}
     * Header: X-API-Key: {device-api-token}
     * Body: { "temperature": 23.5, "humidity": 65.2, "voltage": 220.1 }
     *
     * Features:
     * - No complex JSON nesting required
     * - Auto-creates device if not exists (controlled by telemetry.auto-provision.enabled)
     * - Simple authentication via API key
     * - Timestamp automatically added
     *
     * @param deviceId The device identifier (external ID)
     * @param apiKey Device API key (UUID token)
     * @param variables Direct key-value pairs of sensor variables
     * @return Response indicating success or failure
     */
    @PostMapping("/{deviceId}")
    @Transactional
    public ResponseEntity<SimpleIngestResponse> ingest(
            @PathVariable String deviceId,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody Map<String, Object> variables) {

        log.debug("Received simple HTTP telemetry for device: {} with {} variables", deviceId, variables.size());

        // Validate API key is provided
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Missing X-API-Key header for device: {}", deviceId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SimpleIngestResponse(false, "X-API-Key header is required"));
        }

        // Validate API key format (UUID)
        if (!deviceTokenService.isDeviceToken(apiKey)) {
            log.warn("Invalid API key format for device: {}", deviceId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SimpleIngestResponse(false, "Invalid API key format. Expected UUID."));
        }

        // Authenticate device using API key
        Device authenticatedDevice = deviceTokenService.getDeviceByToken(apiKey).orElse(null);
        if (authenticatedDevice == null) {
            log.warn("Invalid API key provided for device: {}", deviceId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SimpleIngestResponse(false, "Invalid API key"));
        }

        // Get the organization from the authenticated device
        Organization authenticatedOrganization = authenticatedDevice.getOrganization();
        if (authenticatedOrganization == null) {
            log.error("Authenticated device {} has no organization", authenticatedDevice.getExternalId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SimpleIngestResponse(false, "Device configuration error"));
        }

        // Check if the target device exists and verify organization ownership
        Optional<Device> targetDevice = deviceRepository.findByExternalId(deviceId);
        if (targetDevice.isPresent()) {
            // Device exists - verify it belongs to the same organization
            if (!targetDevice.get().getOrganization().getId().equals(authenticatedOrganization.getId())) {
                log.warn("Organization mismatch: token org={}, target device org={}",
                        authenticatedOrganization.getId(),
                        targetDevice.get().getOrganization().getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new SimpleIngestResponse(false,
                            "Cannot send data to device in different organization"));
            }
            log.debug("Target device exists and belongs to same organization");
        } else {
            // Device doesn't exist - will be auto-created by ingestion service
            // in the authenticated organization (if auto-provision is enabled)
            log.debug("Target device {} will be auto-created in organization {}",
                    deviceId, authenticatedOrganization.getName());
        }

        // Update last used timestamp
        deviceTokenService.updateTokenLastUsed(apiKey);

        // Convert all values to BigDecimal (telemetry service expects BigDecimal)
        // Fail fast on invalid data types or conversion errors
        Map<String, BigDecimal> telemetryVariables;
        try {
            telemetryVariables = variables.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> convertToBigDecimal(entry.getKey(), entry.getValue())
                    ));
        } catch (IllegalArgumentException e) {
            // Invalid data type or conversion failure
            log.error("Invalid data format for device {}: {}", deviceId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new SimpleIngestResponse(false, e.getMessage()));
        }

        if (telemetryVariables.isEmpty()) {
            log.warn("No valid numeric variables provided for device: {}", deviceId);
            return ResponseEntity.badRequest()
                    .body(new SimpleIngestResponse(false, "At least one numeric variable is required"));
        }

        try {
            // Create telemetry payload
            TelemetryPayload payload = new TelemetryPayload(
                    deviceId,
                    Instant.now(), // Auto-add timestamp
                    telemetryVariables,
                    null // No metadata in simple mode
            );

            // Ingest telemetry (auto-provision controlled by configuration)
            telemetryIngestionService.ingest(payload);

            log.info("Successfully ingested simple HTTP telemetry for device: {} ({} variables)",
                    deviceId, telemetryVariables.size());

            return ResponseEntity.ok(new SimpleIngestResponse(true, "Data received successfully"));

        } catch (Exception e) {
            log.error("Failed to ingest simple telemetry for device: {}", deviceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SimpleIngestResponse(false, "Failed to process data: " + e.getMessage()));
        }
    }

    /**
     * Convert various number types to BigDecimal.
     * Throws IllegalArgumentException for invalid data instead of silently returning zero.
     *
     * @param fieldName The name of the field being converted (for error messages)
     * @param value The value to convert
     * @return BigDecimal representation of the value
     * @throws IllegalArgumentException if value cannot be converted to a number
     */
    private BigDecimal convertToBigDecimal(String fieldName, Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    String.format("Invalid numeric value for field '%s': '%s' is not a valid number",
                        fieldName, value));
            }
        } else {
            throw new IllegalArgumentException(
                String.format("Unsupported data type for field '%s': %s (expected number or numeric string)",
                    fieldName, value.getClass().getSimpleName()));
        }
    }

    /**
     * Simple response DTO
     */
    public record SimpleIngestResponse(
            boolean success,
            String message
    ) {}
}
