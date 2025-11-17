package org.sensorvision.demo.service;

import java.util.Map;

/**
 * Gateway interface for demo telemetry integration with SensorVision core services.
 *
 * This interface abstracts the integration points, allowing the demo mode
 * to remain decoupled from the main application's service implementations.
 *
 * The gateway handles:
 * - Device provisioning (create/lookup demo devices)
 * - Telemetry ingestion (persist and broadcast demo data)
 * - Alert generation (create anomaly alerts)
 */
public interface DemoTelemetryGateway {

    /**
     * Ensure a demo device exists, creating it if necessary.
     *
     * @param externalId The device external ID (e.g., "demo-machine-01")
     * @return The internal device ID (UUID as string)
     */
    String ensureDemoDevice(String externalId);

    /**
     * Ingest demo telemetry data into the system.
     *
     * This should:
     * 1. Add data to rolling cache
     * 2. Persist to database
     * 3. Broadcast via WebSocket
     * 4. Evaluate rules (if applicable)
     * 5. Generate alerts if anomaly flag is set
     *
     * @param deviceId  The internal device ID
     * @param payload   Telemetry data (temperature, vibration, RPM, pressure)
     * @param anomaly   Whether this is an anomalous reading
     */
    void ingestDemoTelemetry(String deviceId, Map<String, Object> payload, boolean anomaly);
}
