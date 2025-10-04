package org.sensorvision.simulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.config.SimulatorProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "simulator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SmartMeterSimulator {

    private final SimulatorProperties properties;
    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${simulator.interval-seconds:30}000", initialDelay = 5000)
    public void publishTelemetry() {
        if (!properties.isEnabled()) {
            return;
        }
        int deviceCount = Math.max(properties.getDeviceCount(), 1);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        for (int i = 1; i <= deviceCount; i++) {
            String deviceId = "meter-" + String.format("%03d", i);
            Map<String, Object> payload = buildPayload(deviceId, now);
            try {
                String topic = String.format(properties.getTopicPattern(), deviceId);
                String json = objectMapper.writeValueAsString(payload);
                mqttOutboundChannel.send(MessageBuilder.withPayload(json)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .build());
            } catch (JsonProcessingException ex) {
                log.error("Failed to serialize simulator payload for {}", deviceId, ex);
            }
        }
    }

    private Map<String, Object> buildPayload(String deviceId, LocalDateTime timestamp) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double baseLoad = baseLoad(timestamp.getHour());
        double kw = round(baseLoad + random.nextDouble(-2.5, 2.5));
        double voltage = round(220 + random.nextDouble(-5, 5));
        double current = round(kw / voltage * 1000); // convert to amps estimate
        double powerFactor = round(0.85 + random.nextDouble(-0.05, 0.05));
        double frequency = round(50 + random.nextDouble(-0.1, 0.1));

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", deviceId);
        payload.put("timestamp", Instant.now().toString());

        Map<String, Object> variables = new HashMap<>();
        variables.put("kw_consumption", kw);
        variables.put("voltage", voltage);
        variables.put("current", current);
        variables.put("power_factor", powerFactor);
        variables.put("frequency", frequency);
        payload.put("variables", variables);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("location", "Building " + (char) ('A' + ((deviceId.hashCode() & 0x7fffffff) % 3)) + " - Floor " + (1 + ((deviceId.hashCode() >> 3) & 0x03)));
        metadata.put("sensor_type", "smart_meter");
        metadata.put("firmware_version", "2.1." + ((deviceId.hashCode() & 0x7fffffff) % 5));
        payload.put("metadata", metadata);
        return payload;
    }

    private double baseLoad(int hour) {
        if (hour >= 6 && hour < 9) {
            return 110;
        }
        if (hour >= 9 && hour < 17) {
            return 140;
        }
        if (hour >= 17 && hour < 22) {
            return 180;
        }
        return 90;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}
