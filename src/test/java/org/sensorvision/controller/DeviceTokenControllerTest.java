package org.sensorvision.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.DeviceTokenResponse;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.DeviceService;
import org.sensorvision.service.DeviceTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for DeviceTokenController.
 * Tests REST API endpoints for device token management.
 */
@ExtendWith(MockitoExtension.class)
class DeviceTokenControllerTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private DeviceTokenService deviceTokenService;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private DeviceTokenController deviceTokenController;

    private Organization testOrganization;
    private Device testDevice;
    private final String DEVICE_ID = "test-device-001";
    private final String TEST_TOKEN = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId(DEVICE_ID)
                .name("Test Device")
                .status(DeviceStatus.UNKNOWN)
                .organization(testOrganization)
                .build();

        // Mock SecurityUtils instance methods (lenient to avoid unnecessary stubbing exceptions)
        lenient().when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
    }

    @Test
    void generateToken_withDeviceWithoutToken_shouldSucceed() {
        // Given
        testDevice.setApiToken(null);
        when(deviceRepository.findByExternalId(DEVICE_ID)).thenReturn(Optional.of(testDevice));
        when(deviceTokenService.assignTokenToDevice(testDevice)).thenReturn(TEST_TOKEN);

        // When
        ResponseEntity<DeviceTokenResponse> response = deviceTokenController.generateToken(DEVICE_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getToken()).isEqualTo(TEST_TOKEN);
        assertThat(response.getBody().getDeviceId()).isEqualTo(DEVICE_ID);

        verify(deviceTokenService).assignTokenToDevice(testDevice);
    }

    @Test
    void generateToken_withDeviceAlreadyHavingToken_shouldReturnBadRequest() {
        // Given
        testDevice.setApiToken(TEST_TOKEN);
        when(deviceRepository.findByExternalId(DEVICE_ID)).thenReturn(Optional.of(testDevice));

        // When
        ResponseEntity<DeviceTokenResponse> response = deviceTokenController.generateToken(DEVICE_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("already has a token");

        verify(deviceTokenService, never()).assignTokenToDevice(any());
    }

    @Test
    void generateToken_withNonexistentDevice_shouldThrowException() {
        // Given
        when(deviceRepository.findByExternalId(DEVICE_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> deviceTokenController.generateToken(DEVICE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Device not found");
    }

    // Note: Organization access control tests are better suited for integration tests
    // with Spring Security configured. Unit tests focus on business logic.

    @Test
    void rotateToken_withValidDevice_shouldSucceed() {
        // Given
        String newToken = "650e8400-e29b-41d4-a716-446655440000";
        testDevice.setTokenCreatedAt(LocalDateTime.now());

        when(deviceService.rotateDeviceToken(DEVICE_ID)).thenReturn(newToken);
        when(deviceRepository.findByExternalId(DEVICE_ID)).thenReturn(Optional.of(testDevice));

        // When
        ResponseEntity<DeviceTokenResponse> response = deviceTokenController.rotateToken(DEVICE_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getToken()).isEqualTo(newToken);
        assertThat(response.getBody().getDeviceId()).isEqualTo(DEVICE_ID);
        assertThat(response.getBody().getMessage()).contains("rotated successfully");

        verify(deviceService).rotateDeviceToken(DEVICE_ID);
    }

    @Test
    void rotateToken_withNonexistentDevice_shouldThrowException() {
        // Given
        when(deviceService.rotateDeviceToken(DEVICE_ID))
                .thenThrow(new ResourceNotFoundException("Device not found: " + DEVICE_ID));

        // When/Then
        assertThatThrownBy(() -> deviceTokenController.rotateToken(DEVICE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Device not found");
    }

    @Test
    void getTokenInfo_withDeviceHavingToken_shouldReturnMaskedToken() {
        // Given
        testDevice.setApiToken(TEST_TOKEN);
        testDevice.setTokenCreatedAt(LocalDateTime.now().minusDays(1));
        testDevice.setTokenLastUsedAt(LocalDateTime.now().minusHours(1));

        when(deviceRepository.findByExternalId(DEVICE_ID)).thenReturn(Optional.of(testDevice));

        // When
        ResponseEntity<DeviceTokenResponse> response = deviceTokenController.getTokenInfo(DEVICE_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMaskedToken()).isNotNull();
        assertThat(response.getBody().getMaskedToken()).startsWith("550e8400");
        assertThat(response.getBody().getMaskedToken()).endsWith("0000");
        assertThat(response.getBody().getMaskedToken()).contains("...");
        assertThat(response.getBody().getToken()).isNull(); // Full token should not be exposed
        assertThat(response.getBody().getTokenCreatedAt()).isNotNull();
        assertThat(response.getBody().getTokenLastUsedAt()).isNotNull();
    }

    @Test
    void getTokenInfo_withDeviceWithoutToken_shouldReturnNoTokenMessage() {
        // Given
        testDevice.setApiToken(null);
        when(deviceRepository.findByExternalId(DEVICE_ID)).thenReturn(Optional.of(testDevice));

        // When
        ResponseEntity<DeviceTokenResponse> response = deviceTokenController.getTokenInfo(DEVICE_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("no API token");
        assertThat(response.getBody().getToken()).isNull();
        assertThat(response.getBody().getMaskedToken()).isNull();
    }

    @Test
    void getTokenInfo_withNonexistentDevice_shouldThrowException() {
        // Given
        when(deviceRepository.findByExternalId(DEVICE_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> deviceTokenController.getTokenInfo(DEVICE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Device not found");
    }


    @Test
    void revokeToken_withValidDevice_shouldSucceed() {
        // Given
        when(deviceRepository.findByExternalId(DEVICE_ID)).thenReturn(Optional.of(testDevice));
        doNothing().when(deviceTokenService).revokeToken(testDevice.getId());

        // When
        ResponseEntity<DeviceTokenResponse> response = deviceTokenController.revokeToken(DEVICE_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("revoked successfully");
        assertThat(response.getBody().getDeviceId()).isEqualTo(DEVICE_ID);

        verify(deviceTokenService).revokeToken(testDevice.getId());
    }

    @Test
    void revokeToken_withNonexistentDevice_shouldThrowException() {
        // Given
        when(deviceRepository.findByExternalId(DEVICE_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> deviceTokenController.revokeToken(DEVICE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Device not found");

        verify(deviceTokenService, never()).revokeToken(any());
    }

}
