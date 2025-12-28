package io.indcloud.controller;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.LatestTelemetryResponse;
import io.indcloud.dto.TelemetryPointDto;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.Device;
import io.indcloud.model.Organization;
import io.indcloud.mqtt.TelemetryPayload;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.DeviceService;
import io.indcloud.service.DeviceTokenService;
import io.indcloud.service.TelemetryIngestionService;
import io.indcloud.service.TelemetryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;
    private final TelemetryIngestionService telemetryIngestionService;
    private final DeviceService deviceService;
    private final DeviceTokenService deviceTokenService;
    private final DeviceRepository deviceRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/query")
    public List<TelemetryPointDto> queryTelemetry(@RequestParam String deviceId,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return telemetryService.queryTelemetry(deviceId, from, to);
    }

    @GetMapping("/latest")
    public List<LatestTelemetryResponse> latestForDevices(@RequestParam(name = "deviceIds") List<String> deviceIds) {
        List<String> ids = deviceIds.stream()
                .flatMap(id -> Arrays.stream(id.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return telemetryService.getLatest(ids);
    }

    @GetMapping("/latest/{deviceId}")
    public TelemetryPointDto latestForDevice(@PathVariable String deviceId) {
        return telemetryService.getLatest(deviceId);
    }

    /**
     * HTTP POST endpoint for telemetry ingestion
     * Supports device authentication via X-Device-Token header
     *
     * POST /api/v1/data/ingest/{deviceId}
     * Headers: X-Device-Token: {device-api-token} (optional if using JWT)
     * Body: { "timestamp": "2025-01-15T10:30:00Z", "variables": {...}, "metadata": {...} }
     */
    @PostMapping("/ingest/{deviceId}")
    @Transactional
    public ResponseEntity<TelemetryIngestResponse> ingestTelemetry(
            @PathVariable String deviceId,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @Valid @RequestBody TelemetryIngestRequest request) {

        log.debug("Received HTTP telemetry for device: {}", deviceId);

        // Authenticate device if token is provided
        if (deviceToken != null && !deviceToken.isEmpty()) {
            Device authenticatedDevice = deviceTokenService.getDeviceByToken(deviceToken).orElse(null);
            if (authenticatedDevice == null) {
                log.warn("Invalid device token provided for device: {}", deviceId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new TelemetryIngestResponse(false, "Invalid device token"));
            }

            // Verify device ID matches token
            if (!authenticatedDevice.getExternalId().equals(deviceId)) {
                log.warn("Device ID mismatch: token for {}, requested {}",
                        authenticatedDevice.getExternalId(), deviceId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new TelemetryIngestResponse(false, "Device ID mismatch"));
            }

            // Update last used timestamp
            deviceTokenService.updateTokenLastUsed(deviceToken);
        } else {
            // If using JWT auth (no device token), verify organization ownership
            try {
                Organization userOrg = securityUtils.getCurrentUserOrganization();
                Device device = deviceRepository.findByExternalId(deviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

                if (!device.getOrganization().getId().equals(userOrg.getId())) {
                    log.warn("Organization mismatch: user org {}, device org {}",
                            userOrg.getId(), device.getOrganization().getId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new TelemetryIngestResponse(false, "Access denied to device"));
                }
            } catch (AccessDeniedException e) {
                log.warn("Access denied for device: {}", deviceId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new TelemetryIngestResponse(false, "Access denied"));
            }
        }

        try {
            // Convert HTTP request to TelemetryPayload
            TelemetryPayload payload = new TelemetryPayload(
                    deviceId,
                    request.timestamp() != null ? request.timestamp() : Instant.now(),
                    request.variables(),
                    request.metadata()
            );

            // Ingest via existing service (auto-provision setting controlled by configuration)
            telemetryIngestionService.ingest(payload);

            log.info("Successfully ingested HTTP telemetry for device: {}", deviceId);

            return ResponseEntity.ok(new TelemetryIngestResponse(true, "Telemetry data ingested successfully"));

        } catch (Exception e) {
            log.error("Failed to ingest telemetry for device: {}", deviceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TelemetryIngestResponse(false, "Failed to ingest telemetry: " + e.getMessage()));
        }
    }

    /**
     * Bulk ingest telemetry data for multiple devices
     * POST /api/v1/data/ingest/bulk
     */
    @PostMapping("/ingest/bulk")
    @Transactional
    public ResponseEntity<BulkIngestResponse> bulkIngestTelemetry(
            @Valid @RequestBody BulkTelemetryIngestRequest request) {

        log.debug("Received bulk HTTP telemetry for {} devices", request.telemetry().size());

        // For bulk ingestion via JWT, verify all devices belong to user's organization
        try {
            Organization userOrg = securityUtils.getCurrentUserOrganization();

            // Pre-validate all devices belong to user's organization
            for (SingleDeviceTelemetry telemetry : request.telemetry()) {
                Device device = deviceRepository.findByExternalId(telemetry.deviceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + telemetry.deviceId()));

                if (!device.getOrganization().getId().equals(userOrg.getId())) {
                    log.warn("Access denied to device {} for organization {}",
                            telemetry.deviceId(), userOrg.getId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new BulkIngestResponse(0, request.telemetry().size()));
                }
            }
        } catch (AccessDeniedException e) {
            log.warn("Access denied for bulk ingestion");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new BulkIngestResponse(0, request.telemetry().size()));
        }

        int successCount = 0;
        int failureCount = 0;

        for (SingleDeviceTelemetry telemetry : request.telemetry()) {
            try {
                TelemetryPayload payload = new TelemetryPayload(
                        telemetry.deviceId(),
                        telemetry.timestamp() != null ? telemetry.timestamp() : Instant.now(),
                        telemetry.variables(),
                        telemetry.metadata()
                );

                // Auto-provision setting controlled by configuration
                telemetryIngestionService.ingest(payload);
                successCount++;

            } catch (Exception e) {
                log.error("Failed to ingest telemetry for device: {}", telemetry.deviceId(), e);
                failureCount++;
            }
        }

        log.info("Bulk ingestion complete: {} succeeded, {} failed", successCount, failureCount);

        return ResponseEntity.ok(new BulkIngestResponse(successCount, failureCount));
    }

    // DTOs for ingestion endpoints
    public record TelemetryIngestRequest(
            Instant timestamp,
            Map<String, BigDecimal> variables,
            Map<String, Object> metadata
    ) {}

    public record TelemetryIngestResponse(
            boolean success,
            String message
    ) {}

    public record BulkTelemetryIngestRequest(
            List<SingleDeviceTelemetry> telemetry
    ) {}

    public record SingleDeviceTelemetry(
            String deviceId,
            Instant timestamp,
            Map<String, BigDecimal> variables,
            Map<String, Object> metadata
    ) {}

    public record BulkIngestResponse(
            int successCount,
            int failureCount
    ) {}
}
