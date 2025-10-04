package org.sensorvision.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import com.google.common.util.concurrent.AtomicDouble;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.mqtt.TelemetryPayload;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.sensorvision.dto.TelemetryPointDto;
import org.sensorvision.websocket.TelemetryWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TelemetryIngestionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final DeviceRepository deviceRepository;
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final TelemetryWebSocketHandler webSocketHandler;
    private final RuleEngineService ruleEngineService;
    private final SyntheticVariableService syntheticVariableService;
    private final MeterRegistry meterRegistry;
    private final Counter mqttMessagesCounter;
    private final ConcurrentHashMap<String, AtomicInteger> deviceStatusGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> kwGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> voltageGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> currentGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> powerFactorGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> frequencyGauges = new ConcurrentHashMap<>();

    public TelemetryIngestionService(DeviceRepository deviceRepository,
                                     TelemetryRecordRepository telemetryRecordRepository,
                                     TelemetryWebSocketHandler webSocketHandler,
                                     RuleEngineService ruleEngineService,
                                     SyntheticVariableService syntheticVariableService,
                                     MeterRegistry meterRegistry) {
        this.deviceRepository = deviceRepository;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.webSocketHandler = webSocketHandler;
        this.ruleEngineService = ruleEngineService;
        this.syntheticVariableService = syntheticVariableService;
        this.meterRegistry = meterRegistry;
        this.mqttMessagesCounter = meterRegistry.counter("mqtt_messages_total");
    }

    public void ingest(TelemetryPayload payload) {
        Device device = deviceRepository.findByExternalId(payload.deviceId())
                .orElseGet(() -> deviceRepository.save(Device.builder()
                        .externalId(payload.deviceId())
                        .name(payload.deviceId())
                        .status(DeviceStatus.UNKNOWN)
                        .build()));

        Map<String, Object> metadata = payload.metadata();
        if (metadata != null) {
            device.setLocation((String) metadata.getOrDefault("location", device.getLocation()));
            device.setSensorType((String) metadata.getOrDefault("sensor_type", device.getSensorType()));
            device.setFirmwareVersion((String) metadata.getOrDefault("firmware_version", device.getFirmwareVersion()));
        }
        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeenAt(payload.timestamp());
        deviceRepository.save(device);
        updateDeviceGauge(device.getExternalId(), true);

        BigDecimal kw = value(payload.variables(), "kw_consumption");
        BigDecimal voltage = value(payload.variables(), "voltage");
        BigDecimal current = value(payload.variables(), "current");
        BigDecimal powerFactor = value(payload.variables(), "power_factor");
        BigDecimal frequency = value(payload.variables(), "frequency");

        TelemetryRecord record = TelemetryRecord.builder()
                .device(device)
                .timestamp(payload.timestamp())
                .kwConsumption(kw)
                .voltage(voltage)
                .current(current)
                .powerFactor(powerFactor)
                .frequency(frequency)
                .metadata(metadata)
                .build();

        telemetryRecordRepository.save(record);
        mqttMessagesCounter.increment();
        updateGauge("iot_kw_consumption", kwGauges, device.getExternalId(), kw);
        updateGauge("iot_voltage", voltageGauges, device.getExternalId(), voltage);
        updateGauge("iot_current", currentGauges, device.getExternalId(), current);
        updateGauge("iot_power_factor", powerFactorGauges, device.getExternalId(), powerFactor);
        updateGauge("iot_frequency", frequencyGauges, device.getExternalId(), frequency);

        // Broadcast telemetry data to WebSocket clients
        TelemetryPointDto telemetryPoint = new TelemetryPointDto(
                device.getExternalId(),
                payload.timestamp(),
                kw != null ? kw.doubleValue() : null,
                voltage != null ? voltage.doubleValue() : null,
                current != null ? current.doubleValue() : null,
                powerFactor != null ? powerFactor.doubleValue() : null,
                frequency != null ? frequency.doubleValue() : null
        );
        webSocketHandler.broadcastTelemetryData(telemetryPoint);

        // Evaluate rules and trigger alerts if conditions are met
        ruleEngineService.evaluateRules(record);

        // Calculate synthetic variables (derived metrics)
        syntheticVariableService.calculateSyntheticVariables(record);
    }

    private void updateDeviceGauge(String deviceId, boolean online) {
        AtomicInteger gauge = deviceStatusGauges.computeIfAbsent(deviceId, id -> {
            AtomicInteger newGauge = new AtomicInteger(0);
            meterRegistry.gauge("iot_device_status", Tags.of("deviceId", id, "status", "ONLINE"), newGauge);
            return newGauge;
        });
        gauge.set(online ? 1 : 0);
    }

    private void updateGauge(String metricName,
                             ConcurrentHashMap<String, AtomicDouble> gauges,
                             String deviceId,
                             BigDecimal value) {
        AtomicDouble gauge = gauges.computeIfAbsent(deviceId, id ->
                meterRegistry.gauge(metricName, Tags.of("deviceId", id), new AtomicDouble(0.0)));
        gauge.set(value != null ? value.doubleValue() : 0.0);
    }

    private BigDecimal value(Map<String, BigDecimal> variables, String key) {
        if (variables == null) {
            return ZERO;
        }
        return variables.getOrDefault(key, ZERO);
    }
}
