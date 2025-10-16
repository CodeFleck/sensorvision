package org.sensorvision.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.HttpTelemetryRequest;
import org.sensorvision.dto.HttpTelemetryResponse;
import org.sensorvision.exception.BadRequestException;
import org.sensorvision.mqtt.TelemetryPayload;
import org.sensorvision.service.TelemetryIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@Slf4j
@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataIngestionController {

    private final TelemetryIngestionService telemetryIngestionService;

    /**
     * Ingest telemetry data via HTTP REST API
     *
     * Example request:
     * POST /api/v1/data/ingest
     * {
     *   "deviceId": "sensor-001",
     *   "timestamp": "2024-01-15T10:30:00Z",  // Optional
     *   "variables": {
     *     "kw_consumption": 125.5,
     *     "voltage": 220.1,
     *     "current": 0.57
     *   },
     *   "metadata": {
     *     "location": "Building A",
     *     "sensor_type": "smart_meter"
     *   }
     * }
     */
    @PostMapping("/ingest")
    public ResponseEntity<HttpTelemetryResponse> ingestTelemetry(@RequestBody HttpTelemetryRequest request) {
        try {
            // Validate request
            if (request.deviceId() == null || request.deviceId().trim().isEmpty()) {
                throw new BadRequestException("deviceId is required");
            }

            if (request.variables() == null || request.variables().isEmpty()) {
                throw new BadRequestException("variables are required");
            }

            // Parse timestamp or use current time
            Instant timestamp;
            if (request.timestamp() != null && !request.timestamp().trim().isEmpty()) {
                try {
                    timestamp = Instant.parse(request.timestamp());
                } catch (DateTimeParseException e) {
                    throw new BadRequestException("Invalid timestamp format. Use ISO-8601 format (e.g., 2024-01-15T10:30:00Z)");
                }
            } else {
                timestamp = Instant.now();
            }

            // Create telemetry payload
            TelemetryPayload payload = new TelemetryPayload(
                    request.deviceId(),
                    timestamp,
                    request.variables(),
                    request.metadata() != null ? request.metadata() : java.util.Map.of()
            );

            // Ingest the data (allow auto-provision for HTTP endpoints - user is authenticated)
            telemetryIngestionService.ingest(payload, true);

            log.info("HTTP telemetry data ingested for device: {}", request.deviceId());

            return ResponseEntity.ok(HttpTelemetryResponse.success(
                    request.deviceId(),
                    timestamp,
                    request.variables().size()
            ));

        } catch (BadRequestException e) {
            log.warn("Bad request for telemetry ingestion: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    HttpTelemetryResponse.error(request.deviceId(), e.getMessage())
            );
        } catch (Exception e) {
            log.error("Failed to ingest telemetry data for device {}: {}", request.deviceId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    HttpTelemetryResponse.error(request.deviceId(), "Internal server error: " + e.getMessage())
            );
        }
    }

    /**
     * Simplified endpoint for single variable ingestion
     * POST /api/v1/data/{deviceId}/{variable}
     * Body: { "value": 125.5 }
     */
    @PostMapping("/{deviceId}/{variableName}")
    public ResponseEntity<HttpTelemetryResponse> ingestSingleVariable(
            @PathVariable String deviceId,
            @PathVariable String variableName,
            @RequestBody java.math.BigDecimal value) {

        try {
            if (deviceId == null || deviceId.trim().isEmpty()) {
                throw new BadRequestException("deviceId is required");
            }

            if (variableName == null || variableName.trim().isEmpty()) {
                throw new BadRequestException("variableName is required");
            }

            if (value == null) {
                throw new BadRequestException("value is required");
            }

            Instant timestamp = Instant.now();

            // Create telemetry payload with single variable
            TelemetryPayload payload = new TelemetryPayload(
                    deviceId,
                    timestamp,
                    java.util.Map.of(variableName, value),
                    java.util.Map.of()
            );

            // Allow auto-provision for HTTP endpoints - user is authenticated
            telemetryIngestionService.ingest(payload, true);

            log.info("HTTP single variable '{}' ingested for device: {}", variableName, deviceId);

            return ResponseEntity.ok(HttpTelemetryResponse.success(deviceId, timestamp, 1));

        } catch (BadRequestException e) {
            log.warn("Bad request for single variable ingestion: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    HttpTelemetryResponse.error(deviceId, e.getMessage())
            );
        } catch (Exception e) {
            log.error("Failed to ingest single variable for device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    HttpTelemetryResponse.error(deviceId, "Internal server error: " + e.getMessage())
            );
        }
    }

    /**
     * Bulk ingestion endpoint for multiple devices
     * POST /api/v1/data/bulk
     * Body: [ { "deviceId": "...", "variables": {...} }, ... ]
     */
    @PostMapping("/bulk")
    public ResponseEntity<java.util.List<HttpTelemetryResponse>> bulkIngest(
            @RequestBody java.util.List<HttpTelemetryRequest> requests) {

        java.util.List<HttpTelemetryResponse> responses = new java.util.ArrayList<>();

        for (HttpTelemetryRequest request : requests) {
            try {
                if (request.deviceId() == null || request.variables() == null) {
                    responses.add(HttpTelemetryResponse.error(
                            request.deviceId(),
                            "deviceId and variables are required"
                    ));
                    continue;
                }

                Instant timestamp = request.timestamp() != null
                        ? Instant.parse(request.timestamp())
                        : Instant.now();

                TelemetryPayload payload = new TelemetryPayload(
                        request.deviceId(),
                        timestamp,
                        request.variables(),
                        request.metadata() != null ? request.metadata() : java.util.Map.of()
                );

                // Allow auto-provision for HTTP endpoints - user is authenticated
                telemetryIngestionService.ingest(payload, true);

                responses.add(HttpTelemetryResponse.success(
                        request.deviceId(),
                        timestamp,
                        request.variables().size()
                ));

            } catch (Exception e) {
                log.error("Failed to ingest data for device {}: {}", request.deviceId(), e.getMessage());
                responses.add(HttpTelemetryResponse.error(
                        request.deviceId(),
                        e.getMessage()
                ));
            }
        }

        log.info("Bulk ingestion completed: {} devices", requests.size());

        return ResponseEntity.ok(responses);
    }
}
