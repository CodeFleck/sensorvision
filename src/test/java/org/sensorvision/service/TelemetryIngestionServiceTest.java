package org.sensorvision.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.config.TelemetryConfigurationProperties;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.Organization;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.mqtt.TelemetryPayload;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.OrganizationRepository;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.sensorvision.security.DeviceTokenAuthenticationFilter.DeviceTokenAuthentication;
import org.sensorvision.websocket.TelemetryWebSocketHandler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}
