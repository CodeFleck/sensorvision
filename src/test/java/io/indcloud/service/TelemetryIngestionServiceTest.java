package io.indcloud.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.config.TelemetryConfigurationProperties;
import io.indcloud.model.Device;
import io.indcloud.model.DeviceStatus;
import io.indcloud.model.Event;
import io.indcloud.model.Organization;
import io.indcloud.model.TelemetryRecord;
import io.indcloud.mqtt.TelemetryPayload;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.OrganizationRepository;
import io.indcloud.repository.TelemetryRecordRepository;
import io.indcloud.security.DeviceTokenAuthenticationFilter.DeviceTokenAuthentication;
import io.indcloud.websocket.TelemetryWebSocketHandler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryIngestionServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TelemetryRecordRepository telemetryRecordRepository;

    @Mock
    private TelemetryWebSocketHandler webSocketHandler;

    @Mock
    private RuleEngineService ruleEngineService;

    @Mock
    private SyntheticVariableService syntheticVariableService;

    @Mock
    private DynamicVariableService dynamicVariableService;

    @Mock
    private EventService eventService;

    @Mock
    private AutoWidgetGeneratorService autoWidgetGeneratorService;

    private MeterRegistry meterRegistry;

    private TelemetryConfigurationProperties telemetryConfig;

    private TelemetryIngestionService telemetryIngestionService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        telemetryConfig = new TelemetryConfigurationProperties();
        telemetryConfig.getAutoProvision().setEnabled(true);

        telemetryIngestionService = new TelemetryIngestionService(
                deviceRepository,
                deviceService,
                organizationRepository,
                telemetryRecordRepository,
                webSocketHandler,
                ruleEngineService,
                syntheticVariableService,
                dynamicVariableService,
                eventService,
                autoWidgetGeneratorService,
                meterRegistry,
                telemetryConfig
        );

        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    @Test
    void ingest_withExistingDevice_shouldUpdateAndSaveTelemetry() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device existingDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("test-device-001")
                .name("Test Device")
                .status(DeviceStatus.UNKNOWN)
                .organization(org)
                .build();

        TelemetryPayload payload = new TelemetryPayload(
                "test-device-001",
                Instant.now(),
                Map.of("kw_consumption", new BigDecimal("50.5")),
                Map.of("location", "Building A")
        );

        when(deviceRepository.findByExternalIdWithOrganization("test-device-001")).thenReturn(Optional.of(existingDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(existingDevice);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert
        verify(deviceRepository).findByExternalIdWithOrganization("test-device-001");
        verify(deviceRepository).save(existingDevice);
        verify(telemetryRecordRepository).save(any(TelemetryRecord.class));
        verify(ruleEngineService).evaluateRules(any(TelemetryRecord.class));
        verify(syntheticVariableService).calculateSyntheticVariables(any(TelemetryRecord.class));

        assertEquals(DeviceStatus.ONLINE, existingDevice.getStatus());
        assertEquals("Building A", existingDevice.getLocation());
    }

    @Test
    void ingest_withNewDevice_andDeviceTokenAuth_shouldAutoCreateInSameOrganization() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device authenticatedDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("authenticated-device")
                .name("Authenticated Device")
                .organization(org)
                .apiToken("test-token")
                .build();

        Device newDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("new-device-001")
                .name("new-device-001")
                .status(DeviceStatus.UNKNOWN)
                .organization(org)
                .build();

        TelemetryPayload payload = new TelemetryPayload(
                "new-device-001",
                Instant.now(),
                Map.of("kw_consumption", new BigDecimal("75.3")),
                Map.of()
        );

        // Set up device token authentication in security context
        DeviceTokenAuthentication auth = new DeviceTokenAuthentication(
                authenticatedDevice.getExternalId(),
                authenticatedDevice,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(deviceRepository.findByExternalIdWithOrganization("new-device-001")).thenReturn(Optional.empty());
        when(deviceService.getOrCreateDevice(eq("new-device-001"), eq(org))).thenReturn(newDevice);
        when(deviceRepository.save(any(Device.class))).thenReturn(newDevice);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert
        verify(deviceService).getOrCreateDevice("new-device-001", org);
        verify(telemetryRecordRepository).save(any(TelemetryRecord.class));

        ArgumentCaptor<TelemetryRecord> recordCaptor = ArgumentCaptor.forClass(TelemetryRecord.class);
        verify(telemetryRecordRepository).save(recordCaptor.capture());
        TelemetryRecord savedRecord = recordCaptor.getValue();

        assertEquals("new-device-001", savedRecord.getDevice().getExternalId());
        assertEquals(org.getId(), savedRecord.getDevice().getOrganization().getId());
    }

    @Test
    void ingest_withNewDevice_noDeviceTokenAuth_shouldUseDefaultOrganization() {
        // Arrange
        Organization defaultOrg = Organization.builder().id(999L).name("Default Organization").build();
        Device newDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("new-device-002")
                .name("new-device-002")
                .status(DeviceStatus.UNKNOWN)
                .organization(defaultOrg)
                .build();

        TelemetryPayload payload = new TelemetryPayload(
                "new-device-002",
                Instant.now(),
                Map.of("voltage", new BigDecimal("220.5")),
                Map.of()
        );

        // No authentication in security context
        SecurityContextHolder.clearContext();

        when(deviceRepository.findByExternalIdWithOrganization("new-device-002")).thenReturn(Optional.empty());
        when(organizationRepository.findByName("Default Organization")).thenReturn(Optional.of(defaultOrg));
        when(deviceService.getOrCreateDevice(eq("new-device-002"), eq(defaultOrg))).thenReturn(newDevice);
        when(deviceRepository.save(any(Device.class))).thenReturn(newDevice);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert
        verify(organizationRepository).findByName("Default Organization");
        verify(deviceService).getOrCreateDevice("new-device-002", defaultOrg);
        verify(telemetryRecordRepository).save(any(TelemetryRecord.class));
    }

    @Test
    void ingest_withNewDevice_autoProvisionDisabled_shouldThrowException() {
        // Arrange
        telemetryConfig.getAutoProvision().setEnabled(false);

        TelemetryPayload payload = new TelemetryPayload(
                "non-existent-device",
                Instant.now(),
                Map.of("kw_consumption", new BigDecimal("100.0")),
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("non-existent-device")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> telemetryIngestionService.ingest(payload)
        );

        assertTrue(exception.getMessage().contains("auto-provisioning is disabled"));
        assertTrue(exception.getMessage().contains("non-existent-device"));

        verify(deviceRepository).findByExternalIdWithOrganization("non-existent-device");
        verify(deviceService, never()).getOrCreateDevice(any(), any());
        verify(telemetryRecordRepository, never()).save(any());
    }

    @Test
    void ingest_shouldUpdateMetadataFromPayload() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device device = Device.builder()
                .id(UUID.randomUUID())
                .externalId("device-with-metadata")
                .name("Device")
                .organization(org)
                .build();

        Map<String, Object> metadata = Map.of(
                "location", "Floor 3",
                "sensor_type", "temperature_sensor",
                "firmware_version", "v2.1.0"
        );

        TelemetryPayload payload = new TelemetryPayload(
                "device-with-metadata",
                Instant.now(),
                Map.of("kw_consumption", new BigDecimal("25.0")),
                metadata
        );

        when(deviceRepository.findByExternalIdWithOrganization("device-with-metadata")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert
        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository, atLeastOnce()).save(deviceCaptor.capture());

        Device savedDevice = deviceCaptor.getValue();
        assertEquals("Floor 3", savedDevice.getLocation());
        assertEquals("temperature_sensor", savedDevice.getSensorType());
        assertEquals("v2.1.0", savedDevice.getFirmwareVersion());
    }

    @Test
    void ingest_shouldPopulateOrganizationInTelemetryRecord() {
        // Regression test for organization_id null constraint violation
        // This test ensures that the organization field is properly populated
        // when creating telemetry records to prevent batch insert failures

        // Arrange
        Organization org = Organization.builder().id(42L).name("Test Organization").build();
        Device device = Device.builder()
                .id(UUID.randomUUID())
                .externalId("test-device-org")
                .name("Test Device with Org")
                .organization(org)
                .build();

        TelemetryPayload payload = new TelemetryPayload(
                "test-device-org",
                Instant.now(),
                Map.of("kw_consumption", new BigDecimal("100.5")),
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("test-device-org"))
                .thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert
        ArgumentCaptor<TelemetryRecord> recordCaptor = ArgumentCaptor.forClass(TelemetryRecord.class);
        verify(telemetryRecordRepository).save(recordCaptor.capture());
        TelemetryRecord savedRecord = recordCaptor.getValue();

        // Verify organization is NOT null (regression check)
        assertNotNull(savedRecord.getOrganization(), "Organization must not be null in telemetry record");
        assertEquals(42L, savedRecord.getOrganization().getId(), "Organization ID must match device's organization");
        assertEquals("Test Organization", savedRecord.getOrganization().getName());
        assertEquals(device.getId(), savedRecord.getDevice().getId());
    }

    // ==================== Dynamic Variable (EAV) Tests ====================

    @Test
    void ingest_shouldCallDynamicVariableServiceToProcessTelemetry() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device device = Device.builder()
                .id(UUID.randomUUID())
                .externalId("device-with-dynamic-vars")
                .name("Dynamic Vars Device")
                .organization(org)
                .build();

        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put("temperature", new BigDecimal("23.5"));
        variables.put("humidity", new BigDecimal("65.0"));
        variables.put("custom_sensor", new BigDecimal("100.0"));

        TelemetryPayload payload = new TelemetryPayload(
                "device-with-dynamic-vars",
                Instant.now(),
                variables,
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("device-with-dynamic-vars"))
                .thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(dynamicVariableService.processTelemetry(any(), any(), any(), any()))
                .thenReturn(Collections.emptyMap());

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert - verify DynamicVariableService.processTelemetry was called with correct parameters
        verify(dynamicVariableService).processTelemetry(
                eq(device),
                eq(variables),
                eq(payload.timestamp()),
                any()
        );
    }

    @Test
    void ingest_withEmptyVariables_shouldNotCallDynamicVariableService() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device device = Device.builder()
                .id(UUID.randomUUID())
                .externalId("device-empty-vars")
                .name("Empty Vars Device")
                .organization(org)
                .build();

        TelemetryPayload payload = new TelemetryPayload(
                "device-empty-vars",
                Instant.now(),
                Collections.emptyMap(),
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("device-empty-vars"))
                .thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert - verify DynamicVariableService.processTelemetry was NOT called
        verify(dynamicVariableService, never()).processTelemetry(any(), any(), any(), any());
    }

    @Test
    void ingest_shouldCreateDynamicPrometheusGaugesForVariables() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device device = Device.builder()
                .id(UUID.randomUUID())
                .externalId("device-metrics")
                .name("Metrics Device")
                .organization(org)
                .build();

        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put("custom_temp", new BigDecimal("25.5"));
        variables.put("custom_pressure", new BigDecimal("1013.25"));

        TelemetryPayload payload = new TelemetryPayload(
                "device-metrics",
                Instant.now(),
                variables,
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("device-metrics"))
                .thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(dynamicVariableService.processTelemetry(any(), any(), any(), any()))
                .thenReturn(Collections.emptyMap());

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert - verify Prometheus gauges were created
        assertNotNull(meterRegistry.find("iot_dynamic_custom_temp").gauge());
        assertNotNull(meterRegistry.find("iot_dynamic_custom_pressure").gauge());

        // Verify gauge values
        assertEquals(25.5, meterRegistry.find("iot_dynamic_custom_temp").gauge().value(), 0.01);
        assertEquals(1013.25, meterRegistry.find("iot_dynamic_custom_pressure").gauge().value(), 0.01);
    }

    @Test
    void ingest_shouldReuseDynamicGaugesForSameDeviceAndVariable() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device device = Device.builder()
                .id(UUID.randomUUID())
                .externalId("device-reuse-gauges")
                .name("Reuse Gauges Device")
                .organization(org)
                .build();

        when(deviceRepository.findByExternalIdWithOrganization("device-reuse-gauges"))
                .thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(dynamicVariableService.processTelemetry(any(), any(), any(), any()))
                .thenReturn(Collections.emptyMap());

        // First ingestion
        TelemetryPayload payload1 = new TelemetryPayload(
                "device-reuse-gauges",
                Instant.now(),
                Map.of("sensor_value", new BigDecimal("100.0")),
                Map.of()
        );
        telemetryIngestionService.ingest(payload1);

        // Verify first value
        assertEquals(100.0, meterRegistry.find("iot_dynamic_sensor_value").gauge().value(), 0.01);

        // Second ingestion with same variable but different value
        TelemetryPayload payload2 = new TelemetryPayload(
                "device-reuse-gauges",
                Instant.now(),
                Map.of("sensor_value", new BigDecimal("200.0")),
                Map.of()
        );
        telemetryIngestionService.ingest(payload2);

        // Assert - gauge should be reused and value updated
        assertEquals(200.0, meterRegistry.find("iot_dynamic_sensor_value").gauge().value(), 0.01);

        // Verify only one gauge was created (not multiple)
        assertEquals(1, meterRegistry.find("iot_dynamic_sensor_value").gauges().size());
    }

    @Test
    void ingest_dynamicGauges_shouldSanitizeVariableNamesForPrometheus() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device device = Device.builder()
                .id(UUID.randomUUID())
                .externalId("device-sanitize")
                .name("Sanitize Device")
                .organization(org)
                .build();

        // Variable names with special characters that need sanitization
        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put("sensor-reading", new BigDecimal("50.0"));  // Contains hyphen
        variables.put("Temperature.Value", new BigDecimal("22.5")); // Contains dot

        TelemetryPayload payload = new TelemetryPayload(
                "device-sanitize",
                Instant.now(),
                variables,
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("device-sanitize"))
                .thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(dynamicVariableService.processTelemetry(any(), any(), any(), any()))
                .thenReturn(Collections.emptyMap());

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert - variable names should be sanitized (hyphens/dots replaced with underscores)
        assertNotNull(meterRegistry.find("iot_dynamic_sensor_reading").gauge());
        assertNotNull(meterRegistry.find("iot_dynamic_temperature_value").gauge());
    }

    @Test
    void ingest_dynamicGauges_shouldHandleNullVariableValues() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device device = Device.builder()
                .id(UUID.randomUUID())
                .externalId("device-null-values")
                .name("Null Values Device")
                .organization(org)
                .build();

        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put("valid_sensor", new BigDecimal("75.0"));
        variables.put("null_sensor", null);  // Null value

        TelemetryPayload payload = new TelemetryPayload(
                "device-null-values",
                Instant.now(),
                variables,
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("device-null-values"))
                .thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(dynamicVariableService.processTelemetry(any(), any(), any(), any()))
                .thenReturn(Collections.emptyMap());

        // Act - should not throw exception
        assertDoesNotThrow(() -> telemetryIngestionService.ingest(payload));

        // Assert - valid sensor gauge should be created, null sensor should be skipped
        assertNotNull(meterRegistry.find("iot_dynamic_valid_sensor").gauge());
        assertEquals(75.0, meterRegistry.find("iot_dynamic_valid_sensor").gauge().value(), 0.01);
        // Null value sensor should NOT create a gauge
        assertNull(meterRegistry.find("iot_dynamic_null_sensor").gauge());
    }

    // ==================== Device Connected Event Tests ====================

    @Test
    void ingest_withOfflineDevice_shouldCreateDeviceConnectedEvent() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device offlineDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("offline-device-001")
                .name("Offline Device")
                .status(DeviceStatus.OFFLINE)
                .organization(org)
                .build();

        TelemetryPayload payload = new TelemetryPayload(
                "offline-device-001",
                Instant.now(),
                Map.of("kw_consumption", new BigDecimal("50.0")),
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("offline-device-001"))
                .thenReturn(Optional.of(offlineDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(offlineDevice);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert - DEVICE_CONNECTED event should be created
        verify(eventService).createDeviceEvent(
                eq(org),
                eq("offline-device-001"),
                eq(Event.EventType.DEVICE_CONNECTED),
                eq(Event.EventSeverity.INFO),
                eq("Device connected"),
                contains("is now online")
        );
    }

    @Test
    void ingest_withUnknownDevice_shouldCreateDeviceConnectedEvent() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device unknownDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("unknown-device-001")
                .name("Unknown Device")
                .status(DeviceStatus.UNKNOWN)
                .organization(org)
                .build();

        TelemetryPayload payload = new TelemetryPayload(
                "unknown-device-001",
                Instant.now(),
                Map.of("voltage", new BigDecimal("220.0")),
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("unknown-device-001"))
                .thenReturn(Optional.of(unknownDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(unknownDevice);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert - DEVICE_CONNECTED event should be created for UNKNOWN status
        verify(eventService).createDeviceEvent(
                eq(org),
                eq("unknown-device-001"),
                eq(Event.EventType.DEVICE_CONNECTED),
                eq(Event.EventSeverity.INFO),
                eq("Device connected"),
                contains("is now online")
        );
    }

    @Test
    void ingest_withOnlineDevice_shouldNotCreateDeviceConnectedEvent() {
        // Arrange
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device onlineDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("online-device-001")
                .name("Online Device")
                .status(DeviceStatus.ONLINE)  // Already online
                .organization(org)
                .build();

        TelemetryPayload payload = new TelemetryPayload(
                "online-device-001",
                Instant.now(),
                Map.of("current", new BigDecimal("10.5")),
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("online-device-001"))
                .thenReturn(Optional.of(onlineDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(onlineDevice);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert - NO event should be created for already online device
        verify(eventService, never()).createDeviceEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ingest_withNullPreviousStatus_shouldCreateDeviceConnectedEvent() {
        // Arrange - device with null status (can happen for legacy devices)
        Organization org = Organization.builder().id(1L).name("Test Org").build();
        Device deviceWithNullStatus = Device.builder()
                .id(UUID.randomUUID())
                .externalId("null-status-device")
                .name("Device With Null Status")
                .status(null)  // Null status
                .organization(org)
                .build();

        TelemetryPayload payload = new TelemetryPayload(
                "null-status-device",
                Instant.now(),
                Map.of("temperature", new BigDecimal("25.0")),
                Map.of()
        );

        when(deviceRepository.findByExternalIdWithOrganization("null-status-device"))
                .thenReturn(Optional.of(deviceWithNullStatus));
        when(deviceRepository.save(any(Device.class))).thenReturn(deviceWithNullStatus);
        when(telemetryRecordRepository.save(any(TelemetryRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        telemetryIngestionService.ingest(payload);

        // Assert - DEVICE_CONNECTED event SHOULD be created for null status (treated as offline)
        verify(eventService).createDeviceEvent(
                eq(org),
                eq("null-status-device"),
                eq(Event.EventType.DEVICE_CONNECTED),
                eq(Event.EventSeverity.INFO),
                eq("Device connected"),
                contains("is now online")
        );
    }
}
