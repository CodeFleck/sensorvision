package org.sensorvision.dto;

import java.time.Instant;

/**
 * Response for HTTP telemetry ingestion
 */
public record HttpTelemetryResponse(
        String deviceId,
        Instant timestamp,
        int variablesReceived,
        String status,
        String message
) {
    public static HttpTelemetryResponse success(String deviceId, Instant timestamp, int variablesReceived) {
        return new HttpTelemetryResponse(
                deviceId,
                timestamp,
                variablesReceived,
                "success",
                "Telemetry data ingested successfully"
        );
    }

    public static HttpTelemetryResponse error(String deviceId, String message) {
        return new HttpTelemetryResponse(
                deviceId,
                Instant.now(),
                0,
                "error",
                message
        );
    }
}
