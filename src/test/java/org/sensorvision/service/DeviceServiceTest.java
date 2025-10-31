package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.DeviceCreateRequest;
import org.sensorvision.dto.DeviceResponse;
import org.sensorvision.dto.DeviceUpdateRequest;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.security.SecurityUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeviceService.
 * Tests device creation with auto-token generation, validation, and CRUD operations.
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceTokenService deviceTokenService;

    @Mock
    private EventService eventService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private DeviceHealthService deviceHealthService;

    @InjectMocks
    private DeviceService deviceService;

    @Captor
    private ArgumentCaptor<Device> deviceCaptor;

    private Organization testOrganization;
    private Device testDevice;
    private String testToken;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("test-device-001")
                .name("Test Device")
                .status(DeviceStatus.UNKNOWN)
                .organization(testOrganization)
                .build();

        testToken = "test-api-token-" + UUID.randomUUID();

        // Mock security context to return test organization
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);

        // Mock health service to return a default health status (lenient for tests that don't use it)
        lenient().when(deviceHealthService.getHealthStatus(anyInt())).thenReturn("EXCELLENT");
    }

    @Test
    void createDevice_shouldAutoGenerateApiToken() {
        // Given
        DeviceCreateRequest request = new DeviceCreateRequest(
                "new-device-001",
                "New Device",
                null,  // description
                null,  // active (will default to true)
                null,  // location
                null,  // sensorType
                null   // firmwareVersion
        );

        Device savedDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId(request.externalId())
                .name(request.name())
                .status(DeviceStatus.UNKNOWN)
                .organization(testOrganization)
                .build();

        when(deviceRepository.findByExternalId(request.externalId())).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);
        when(deviceTokenService.assignTokenToDevice(any(Device.class))).thenReturn(testToken);

        // When
        DeviceResponse response = deviceService.createDevice(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.externalId()).isEqualTo(request.externalId());

        // Verify token was auto-generated
        verify(deviceTokenService, times(1)).assignTokenToDevice(savedDevice);

        // Verify device was saved
        verify(deviceRepository, times(1)).save(any(Device.class));

        // Verify event was emitted
        verify(eventService, times(1)).emitDeviceLifecycleEvent(
                eq(testOrganization),
                eq(request.externalId()),
                eq(request.name()),
                any()
        );
    }

    @Test
    void createDevice_shouldThrowException_whenDeviceAlreadyExists() {
        // Given
        DeviceCreateRequest request = new DeviceCreateRequest(
                "existing-device",
                "Existing Device",
                null,  // description
                null,  // active (will default to true)
                null,  // location
                null,  // sensorType
                null   // firmwareVersion
        );

        when(deviceRepository.findByExternalId(request.externalId()))
                .thenReturn(Optional.of(testDevice));

        // When/Then
        assertThatThrownBy(() -> deviceService.createDevice(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device already exists: existing-device");

        // Verify device was NOT saved
        verify(deviceRepository, never()).save(any(Device.class));

        // Verify token was NOT generated
        verify(deviceTokenService, never()).assignTokenToDevice(any(Device.class));
    }

    @Test
    void createDevice_shouldAssignCurrentUserOrganization() {
        // Given
        DeviceCreateRequest request = new DeviceCreateRequest(
                "org-test-device",
                "Organization Test Device",
                null,  // description
                null,  // active (will default to true)
                null,  // location
                null,  // sensorType
                null   // firmwareVersion
        );

        when(deviceRepository.findByExternalId(request.externalId())).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceTokenService.assignTokenToDevice(any(Device.class))).thenReturn(testToken);

        // When
        deviceService.createDevice(request);

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();

        assertThat(savedDevice.getOrganization()).isEqualTo(testOrganization);
    }

    @Test
    void createDevice_shouldSetInitialStatusToUnknown() {
        // Given
        DeviceCreateRequest request = new DeviceCreateRequest(
                "status-test-device",
                "Status Test Device",
                null,  // description
                null,  // active (will default to true)
                null,  // location
                null,  // sensorType
                null   // firmwareVersion
        );

        when(deviceRepository.findByExternalId(request.externalId())).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceTokenService.assignTokenToDevice(any(Device.class))).thenReturn(testToken);

        // When
        deviceService.createDevice(request);

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();

        assertThat(savedDevice.getStatus()).isEqualTo(DeviceStatus.UNKNOWN);
    }

    @Test
    void getDevice_shouldReturnDevice_whenExists() {
        // Given
        when(deviceRepository.findByExternalId("test-device-001"))
                .thenReturn(Optional.of(testDevice));

        // When
        DeviceResponse response = deviceService.getDevice("test-device-001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.externalId()).isEqualTo("test-device-001");
        assertThat(response.name()).isEqualTo("Test Device");
    }

    @Test
    void getDevice_shouldThrowException_whenNotFound() {
        // Given
        when(deviceRepository.findByExternalId("non-existent"))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> deviceService.getDevice("non-existent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Device not found: non-existent");
    }

    @Test
    void updateDevice_shouldUpdateDeviceFields() {
        // Given
        DeviceUpdateRequest updateRequest = new DeviceUpdateRequest(
                "Updated Device Name",
                null,  // description
                null,  // active
                "New Location",
                "NewSensorType",
                "v2.0.0"
        );

        when(deviceRepository.findByExternalId("test-device-001"))
                .thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DeviceResponse response = deviceService.updateDevice("test-device-001", updateRequest);

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device updatedDevice = deviceCaptor.getValue();

        assertThat(updatedDevice.getName()).isEqualTo("Updated Device Name");
        assertThat(updatedDevice.getLocation()).isEqualTo("New Location");
        assertThat(updatedDevice.getSensorType()).isEqualTo("NewSensorType");
        assertThat(updatedDevice.getFirmwareVersion()).isEqualTo("v2.0.0");
    }

    @Test
    void deleteDevice_shouldDeleteDevice_whenExists() {
        // Given
        when(deviceRepository.findByExternalId("test-device-001"))
                .thenReturn(Optional.of(testDevice));

        // When
        deviceService.deleteDevice("test-device-001");

        // Then
        verify(deviceRepository, times(1)).delete(testDevice);
    }

    @Test
    void deleteDevice_shouldThrowException_whenNotFound() {
        // Given
        when(deviceRepository.findByExternalId("non-existent"))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> deviceService.deleteDevice("non-existent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Device not found: non-existent");

        verify(deviceRepository, never()).delete(any(Device.class));
    }

    /**
     * CRITICAL TEST: Validates the auto-token generation behavior that caused the 400 error bug.
     *
     * Background:
     * - The frontend Integration Wizard was calling createDevice() then generateDeviceToken()
     * - But createDevice() automatically generates a token via assignTokenToDevice()
     * - The second call to generateDeviceToken() failed with 400 "already has token"
     *
     * This test ensures the auto-generation behavior is documented and tested.
     */
    @Test
    void createDevice_autoTokenGeneration_preventsDuplicateTokenCalls() {
        // Given
        DeviceCreateRequest request = new DeviceCreateRequest(
                "auto-token-device",
                "Auto Token Device",
                null,  // description
                null,  // active (will default to true)
                null,  // location
                null,  // sensorType
                null   // firmwareVersion
        );

        Device savedDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId(request.externalId())
                .name(request.name())
                .status(DeviceStatus.UNKNOWN)
                .organization(testOrganization)
                .apiToken(testToken)  // Token is set during creation
                .build();

        when(deviceRepository.findByExternalId(request.externalId())).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);
        when(deviceTokenService.assignTokenToDevice(any(Device.class))).thenReturn(testToken);

        // When
        DeviceResponse response = deviceService.createDevice(request);

        // Then
        // 1. Verify token was auto-generated ONCE during device creation
        verify(deviceTokenService, times(1)).assignTokenToDevice(any(Device.class));

        // 2. Frontend should NOT call generateDeviceToken() after createDevice()
        // 3. Frontend should use rotateDeviceToken() to get the actual token value
        // 4. This prevents the "device already has token" 400 error

        assertThat(response).isNotNull();
    }

    /**
     * Tests for active and description fields (regression test for toggle bug fix)
     */
    @Test
    void createDevice_shouldDefaultActiveToTrue_whenNotProvided() {
        // Given
        DeviceCreateRequest request = new DeviceCreateRequest(
                "default-active-device",
                "Default Active Device",
                null,  // description
                null,  // active (should default to true)
                null,  // location
                null,  // sensorType
                null   // firmwareVersion
        );

        when(deviceRepository.findByExternalId(request.externalId())).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceTokenService.assignTokenToDevice(any(Device.class))).thenReturn(testToken);

        // When
        deviceService.createDevice(request);

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();

        assertThat(savedDevice.getActive()).isTrue();
    }

    @Test
    void createDevice_shouldSetActiveToFalse_whenExplicitlyProvided() {
        // Given
        DeviceCreateRequest request = new DeviceCreateRequest(
                "inactive-device",
                "Inactive Device",
                "Test description",
                false,  // explicitly set to false
                null,
                null,
                null
        );

        when(deviceRepository.findByExternalId(request.externalId())).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceTokenService.assignTokenToDevice(any(Device.class))).thenReturn(testToken);

        // When
        deviceService.createDevice(request);

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();

        assertThat(savedDevice.getActive()).isFalse();
        assertThat(savedDevice.getDescription()).isEqualTo("Test description");
    }

    @Test
    void updateDevice_shouldToggleActiveStatus() {
        // Given - device initially active
        testDevice.setActive(true);

        DeviceUpdateRequest updateRequest = new DeviceUpdateRequest(
                "Test Device",
                null,    // description
                false,   // toggle to inactive
                null,
                null,
                null
        );

        when(deviceRepository.findByExternalId("test-device-001"))
                .thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        deviceService.updateDevice("test-device-001", updateRequest);

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device updatedDevice = deviceCaptor.getValue();

        assertThat(updatedDevice.getActive()).isFalse();
    }

    @Test
    void updateDevice_shouldUpdateDescription() {
        // Given
        String newDescription = "Updated device description";
        DeviceUpdateRequest updateRequest = new DeviceUpdateRequest(
                "Test Device",
                newDescription,
                null,  // don't change active
                null,
                null,
                null
        );

        when(deviceRepository.findByExternalId("test-device-001"))
                .thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        deviceService.updateDevice("test-device-001", updateRequest);

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device updatedDevice = deviceCaptor.getValue();

        assertThat(updatedDevice.getDescription()).isEqualTo(newDescription);
    }

    @Test
    void updateDevice_shouldNotChangeActive_whenNull() {
        // Given - device initially active
        testDevice.setActive(true);

        DeviceUpdateRequest updateRequest = new DeviceUpdateRequest(
                "Test Device",
                null,
                null,  // null means don't change
                null,
                null,
                null
        );

        when(deviceRepository.findByExternalId("test-device-001"))
                .thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        deviceService.updateDevice("test-device-001", updateRequest);

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device updatedDevice = deviceCaptor.getValue();

        // Active should remain true (unchanged)
        assertThat(updatedDevice.getActive()).isTrue();
    }

    @Test
    void toResponse_shouldIncludeDescriptionAndActive() {
        // Given
        testDevice.setDescription("Test description");
        testDevice.setActive(false);

        when(deviceRepository.findByExternalId("test-device-001"))
                .thenReturn(Optional.of(testDevice));

        // When
        DeviceResponse response = deviceService.getDevice("test-device-001");

        // Then
        assertThat(response.description()).isEqualTo("Test description");
        assertThat(response.active()).isFalse();
    }
}
