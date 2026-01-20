package io.indcloud.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import io.indcloud.model.Device;
import io.indcloud.model.DeviceStatus;
import io.indcloud.model.Event;
import io.indcloud.model.Organization;
import io.indcloud.model.TelemetryRecord;
import io.indcloud.model.VariableValue;
import io.indcloud.mqtt.TelemetryPayload;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.OrganizationRepository;
import io.indcloud.repository.TelemetryRecordRepository;
import io.indcloud.dto.TelemetryPointDto;
import io.indcloud.dto.DynamicTelemetryPointDto;
import io.indcloud.websocket.TelemetryWebSocketHandler;
import io.indcloud.config.TelemetryConfigurationProperties;
import io.indcloud.logging.LogContext;
import io.indcloud.security.DeviceTokenAuthenticationFilter.DeviceTokenAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
    private final DynamicVariableService dynamicVariableService;
    private final EventService eventService;
    private final AutoWidgetGeneratorService autoWidgetGeneratorService;
    private final MeterRegistry meterRegistry;
    private final TelemetryConfigurationProperties telemetryConfig;
    private final Counter mqttMessagesCounter;
    private final ConcurrentHashMap<String, AtomicInteger> deviceStatusGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> kwGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> voltageGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> currentGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> powerFactorGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicDouble> frequencyGauges = new ConcurrentHashMap<>();
    // Dynamic gauges for any variable (EAV pattern support)
    // Limited to prevent unbounded metric cardinality
    private static final int MAX_DYNAMIC_GAUGES = 1000;
    private final ConcurrentHashMap<String, AtomicDouble> dynamicGauges = new ConcurrentHashMap<>();

    public TelemetryIngestionService(DeviceRepository deviceRepository,
                                     DeviceService deviceService,
                                     OrganizationRepository organizationRepository,
                                     TelemetryRecordRepository telemetryRecordRepository,
                                     TelemetryWebSocketHandler webSocketHandler,
                                     RuleEngineService ruleEngineService,
                                     SyntheticVariableService syntheticVariableService,
                                     DynamicVariableService dynamicVariableService,
                                     EventService eventService,
                                     AutoWidgetGeneratorService autoWidgetGeneratorService,
                                     MeterRegistry meterRegistry,
                                     TelemetryConfigurationProperties telemetryConfig) {
        this.deviceRepository = deviceRepository;
        this.deviceService = deviceService;
        this.organizationRepository = organizationRepository;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.webSocketHandler = webSocketHandler;
        this.ruleEngineService = ruleEngineService;
        this.syntheticVariableService = syntheticVariableService;
        this.dynamicVariableService = dynamicVariableService;
        this.eventService = eventService;
        this.autoWidgetGeneratorService = autoWidgetGeneratorService;
        this.meterRegistry = meterRegistry;
        this.telemetryConfig = telemetryConfig;
        this.mqttMessagesCounter = meterRegistry.counter("mqtt_messages_total");
    }

    /**
     * Ingest telemetry data with auto-provisioning based on configuration
     * @param payload The telemetry data
     */
    public void ingest(TelemetryPayload payload) {
        Device device = deviceRepository.findByExternalIdWithOrganization(payload.deviceId())
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

        // Set logging context for this telemetry processing
        LogContext.setDevice(device.getId(), device.getExternalId());
        if (device.getOrganization() != null) {
            LogContext.setOrganization(device.getOrganization().getId(), device.getOrganization().getName());
        }
        LogContext.setOperation("telemetry_ingestion");

        try {
            processTelemetry(device, payload);
        } finally {
            LogContext.clearDevice();
            LogContext.clearOrganization();
            LogContext.clearOperation();
        }
    }

    /**
     * Internal method to process telemetry after logging context is set.
     */
    private void processTelemetry(Device device, TelemetryPayload payload) {
        Map<String, Object> metadata = payload.metadata();
        if (metadata != null) {
            device.setLocation((String) metadata.getOrDefault("location", device.getLocation()));
            device.setSensorType((String) metadata.getOrDefault("sensor_type", device.getSensorType()));
            device.setFirmwareVersion((String) metadata.getOrDefault("firmware_version", device.getFirmwareVersion()));
        }

        // Track previous status to detect status changes
        DeviceStatus previousStatus = device.getStatus();
        boolean wasOffline = previousStatus == null ||
                             previousStatus == DeviceStatus.OFFLINE ||
                             previousStatus == DeviceStatus.UNKNOWN;

        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeenAt(payload.timestamp());
        deviceRepository.save(device);
        updateDeviceGauge(device.getExternalId(), true);

        // Create DEVICE_CONNECTED event when device comes online from offline/unknown state
        if (wasOffline && device.getOrganization() != null) {
            eventService.createDeviceEvent(
                    device.getOrganization(),
                    device.getExternalId(),
                    Event.EventType.DEVICE_CONNECTED,
                    Event.EventSeverity.INFO,
                    "Device connected",
                    String.format("Device %s (%s) is now online", device.getName(), device.getExternalId())
            );
            log.info("Device {} came online, created DEVICE_CONNECTED event", device.getExternalId());
        }

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

        // ========== EAV Pattern: Store ALL variables dynamically ==========
        // This enables Ubidots-like functionality where any variable is accepted
        Map<String, VariableValue> storedVariables = null;
        if (payload.variables() != null && !payload.variables().isEmpty()) {
            storedVariables = dynamicVariableService.processTelemetry(
                    device,
                    payload.variables(),
                    payload.timestamp(),
                    metadata
            );

            // Update dynamic Prometheus gauges for all variables
            for (Map.Entry<String, BigDecimal> entry : payload.variables().entrySet()) {
                if (entry.getValue() != null) {
                    updateDynamicGauge(device.getExternalId(), entry.getKey(), entry.getValue());
                }
            }

            log.debug("Stored {} dynamic variables for device '{}'",
                    storedVariables.size(), device.getExternalId());
        }

        // Broadcast telemetry data to WebSocket clients (only to same organization)
        // Legacy format for backward compatibility
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

        // NEW: Also broadcast dynamic telemetry with ALL variables
        if (payload.variables() != null && !payload.variables().isEmpty()) {
            Map<String, Double> allVariables = new HashMap<>();
            for (Map.Entry<String, BigDecimal> entry : payload.variables().entrySet()) {
                if (entry.getValue() != null) {
                    allVariables.put(entry.getKey(), entry.getValue().doubleValue());
                }
            }
            DynamicTelemetryPointDto dynamicPoint = new DynamicTelemetryPointDto(
                    device.getExternalId(),
                    payload.timestamp(),
                    allVariables
            );
            webSocketHandler.broadcastDynamicTelemetryData(dynamicPoint, device.getOrganization().getId());
        }

        // Evaluate rules and trigger alerts if conditions are met
        ruleEngineService.evaluateRules(record);

        // Calculate synthetic variables (derived metrics)
        syntheticVariableService.calculateSyntheticVariables(record);

        // Auto-generate widgets for first telemetry from a device
        if (payload.variables() != null && !payload.variables().isEmpty()) {
            boolean isFirstTelemetry = device.getInitialWidgetsCreated() == null
                                       || !device.getInitialWidgetsCreated();
            if (isFirstTelemetry) {
                log.info("First telemetry from device {}, triggering auto-widget generation",
                        device.getExternalId());
                autoWidgetGeneratorService.generateInitialWidgets(device, payload.variables());
            }
        }
    }

    // Lock object for synchronizing gauge creation to prevent cardinality overflow
    private final Object gaugeLock = new Object();

    /**
     * Update a dynamic gauge for any variable name (EAV pattern support).
     * Limited to MAX_DYNAMIC_GAUGES to prevent unbounded metric cardinality.
     * Uses synchronization to prevent race conditions when creating new gauges.
     */
    private void updateDynamicGauge(String deviceId, String variableName, BigDecimal value) {
        String gaugeKey = deviceId + ":" + variableName;

        // Fast path: check if gauge already exists (no lock needed for reads)
        AtomicDouble existingGauge = dynamicGauges.get(gaugeKey);
        if (existingGauge != null) {
            if (value != null) {
                existingGauge.set(value.doubleValue());
            }
            return;
        }

        // Slow path: need to create a new gauge - synchronize to prevent race condition
        synchronized (gaugeLock) {
            // Double-check after acquiring lock (another thread may have created it)
            existingGauge = dynamicGauges.get(gaugeKey);
            if (existingGauge != null) {
                if (value != null) {
                    existingGauge.set(value.doubleValue());
                }
                return;
            }

            // Check cardinality limit before creating new gauge
            if (dynamicGauges.size() >= MAX_DYNAMIC_GAUGES) {
                log.warn("Dynamic gauge limit ({}) reached. Skipping gauge for device '{}', variable '{}'",
                        MAX_DYNAMIC_GAUGES, deviceId, variableName);
                return;
            }

            // Safe to create new gauge now
            String metricName = "iot_dynamic_" + sanitizeMetricName(variableName);
            AtomicDouble gauge = new AtomicDouble(value != null ? value.doubleValue() : 0.0);
            meterRegistry.gauge(metricName,
                    Tags.of("deviceId", deviceId, "variable", variableName),
                    gauge);
            dynamicGauges.put(gaugeKey, gauge);
        }
    }

    /**
     * Sanitize variable name for use as Prometheus metric name.
     */
    private String sanitizeMetricName(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
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
