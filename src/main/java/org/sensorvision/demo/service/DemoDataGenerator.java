package org.sensorvision.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.demo.config.DemoModeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Demo telemetry data generator for manufacturing sensors.
 *
 * Generates realistic telemetry data for demo devices with the following characteristics:
 * - Temperature: 58-62°C (normal) or 85-95°C (anomaly)
 * - Vibration: 4-6 mm/s (normal) or 20-30 mm/s (anomaly)
 * - RPM: 1450-1550 (normal, with random walk)
 * - Pressure: 2.9-3.1 bar (normal, stable)
 *
 * Anomalies are injected probabilistically (default: 5% of samples).
 * When an anomaly occurs, both temperature and vibration spike together
 * to simulate realistic bearing failure patterns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo", name = "mode-enabled", havingValue = "true")
public class DemoDataGenerator {

    private final DemoModeProperties properties;
    private final DemoTelemetryGateway gateway;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, Double> deviceRpmState = new HashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (running.compareAndSet(false, true)) {
            // Ensure all demo devices exist
            for (int i = 1; i <= properties.getDeviceCount(); i++) {
                String externalId = properties.getDeviceId(i);
                gateway.ensureDemoDevice(externalId);
                // Initialize RPM state for random walk
                deviceRpmState.put(externalId, (properties.getRpmMin() + properties.getRpmMax()) / 2);
            }

            log.info("✅ Demo telemetry generator started successfully");
            log.info("   Devices: {}", properties.getDeviceCount());
            log.info("   Interval: {}ms", properties.getGenerationIntervalMs());
            log.info("   Anomaly rate: {}%", properties.getAnomalyProbability() * 100);
        }
    }

    @PreDestroy
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Demo telemetry generator stopped");
        }
    }

    /**
     * Scheduled task to generate telemetry for all demo devices
     */
    @Scheduled(fixedDelayString = "${demo.generation-interval-ms:500}")
    public void generateTelemetry() {
        if (!running.get()) {
            return;
        }

        for (int i = 1; i <= properties.getDeviceCount(); i++) {
            String externalId = properties.getDeviceId(i);
            generateTelemetryForDevice(externalId);
        }
    }

    /**
     * Generate a single telemetry sample for a device
     */
    private void generateTelemetryForDevice(String deviceExternalId) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Decide if this should be an anomaly
        boolean isAnomaly = random.nextDouble() < properties.getAnomalyProbability();

        // Generate telemetry values
        double temperature;
        double vibration;

        if (isAnomaly) {
            // Anomalous readings: temperature and vibration spike together
            temperature = round(random.nextDouble(
                    properties.getAnomalyTemperatureMin(),
                    properties.getAnomalyTemperatureMax()
            ));
            vibration = round(random.nextDouble(
                    properties.getAnomalyVibrationMin(),
                    properties.getAnomalyVibrationMax()
            ));

            log.debug("🚨 ANOMALY INJECTED - Device: {} - Temp: {}°C, Vibration: {} mm/s",
                    deviceExternalId, temperature, vibration);
        } else {
            // Normal readings
            temperature = round(random.nextDouble(
                    properties.getTemperatureMin(),
                    properties.getTemperatureMax()
            ));
            vibration = round(random.nextDouble(
                    properties.getVibrationMin(),
                    properties.getVibrationMax()
            ));
        }

        // RPM: Random walk within normal range (independent of anomaly)
        double currentRpm = deviceRpmState.getOrDefault(deviceExternalId,
                (properties.getRpmMin() + properties.getRpmMax()) / 2);
        double rpmChange = random.nextDouble(-20, 20);
        double rpm = Math.max(properties.getRpmMin(),
                Math.min(properties.getRpmMax(), currentRpm + rpmChange));
        deviceRpmState.put(deviceExternalId, rpm);
        rpm = round(rpm);

        // Pressure: Stable with minimal variation (independent of anomaly)
        double pressure = round(random.nextDouble(
                properties.getPressureMin(),
                properties.getPressureMax()
        ));

        // Build payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("temperature", temperature);
        payload.put("vibration", vibration);
        payload.put("rpm", rpm);
        payload.put("pressure", pressure);

        // Ingest via gateway
        gateway.ingestDemoTelemetry(deviceExternalId, payload, isAnomaly);
    }

    /**
     * Round to 2 decimal places
     */
    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
