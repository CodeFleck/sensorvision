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
import org.sensorvision.model.Organization;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.mqtt.TelemetryPayload;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.OrganizationRepository;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.sensorvision.dto.TelemetryPointDto;
import org.sensorvision.websocket.TelemetryWebSocketHandler;
import org.sensorvision.config.TelemetryConfigurationProperties;
import org.sensorvision.security.DeviceTokenAuthenticationFilter.DeviceTokenAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TelemetryIngestionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;
    private final OrganizationRepository organizationRepository;
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final TelemetryWebSocketHandler webSocketHandler;
    private final RuleEngineService ruleEngineService;
    private final SyntheticVariableService syntheticVariableService;
    private final MeterRegistry meterRegistry;
    private final TelemetryConfigurationProperties telemetryConfig;
    private final Counter mqttMessagesCounter;
    private final ConcurrentHashMap<String, AtomicInteger> deviceStatusGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> kwGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> voltageGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> currentGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> powerFactorGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> frequencyGauges = new ConcurrentHashMap<>();

    public TelemetryIngestionService(DeviceRepository deviceRepository,
                                     DeviceService deviceService,
                                     OrganizationRepository organizationRepository,
                                     TelemetryRecordRepository telemetryRecordRepository,
                                     TelemetryWebSocketHandler webSocketHandler,
                                     RuleEngineService ruleEngineService,
                                     SyntheticVariableService syntheticVariableService,
                                     MeterRegistry meterRegistry,
                                     TelemetryConfigurationProperties telemetryConfig) {
        this.deviceRepository = deviceRepository;
        this.deviceService = deviceService;
        this.organizationRepository = organizationRepository;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.webSocketHandler = webSocketHandler;
        this.ruleEngineService = ruleEngineService;
        this.syntheticVariableService = syntheticVariableService;
        this.meterRegistry = meterRegistry;
        this.telemetryConfig = telemetryConfig;
        this.mqttMessagesCounter = meterRegistry.counter("mqtt_messages_total");
    }

    /**
     * Ingest telemetry data with auto-provisioning based on configuration
     * @param payload The telemetry data
     */
    public void ingest(TelemetryPayload payload) {
        Device device = deviceRepository.findByExternalId(payload.deviceId())
                .orElseGet(() -> {
                    if (!telemetryConfig.getAutoProvision().isEnabled()) {
                        throw new IllegalArgumentException(
                            "Device not found and auto-provisioning is disabled: " + payload.deviceId());
                    }

                    // Get organization from authenticated device token
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    Organization targetOrg = null;

                    if (auth instanceof DeviceTokenAuthentication) {
                        DeviceTokenAuthentication deviceAuth = (DeviceTokenAuthentication) auth;
                        Device authenticatedDevice = deviceAuth.getDevice();
                        targetOrg = authenticatedDevice.getOrganization();
                    }

                    // Fallback to default organization if no device token authentication
                    if (targetOrg == null) {
                        targetOrg = organizationRepository
                                .findByName("Default Organization")
                                .orElseGet(() -> organizationRepository.save(
                                        Organization.builder()
                                                .name("Default Organization")
                                                .build()
                                ));
                    }

                    return deviceService.getOrCreateDevice(payload.deviceId(), targetOrg);
                });

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
                .organization(device.getOrganization())
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

        // Broadcast telemetry data to WebSocket clients (only to same organization)
        TelemetryPointDto telemetryPoint = new TelemetryPointDto(
                device.getExternalId(),
                payload.timestamp(),
                kw != null ? kw.doubleValue() : null,
                voltage != null ? voltage.doubleValue() : null,
                current != null ? current.doubleValue() : null,
                powerFactor != null ? powerFactor.doubleValue() : null,
                frequency != null ? frequency.doubleValue() : null
        );
        webSocketHandler.broadcastTelemetryData(telemetryPoint, device.getOrganization().getId());

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
