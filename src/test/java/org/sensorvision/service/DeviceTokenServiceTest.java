package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.DeviceRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeviceTokenService.
 * Tests token generation, validation, rotation, and revocation.
 */
@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    @Captor
    private ArgumentCaptor<Device> deviceCaptor;

    private Organization testOrganization;
    private Device testDevice;

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
    }

    @Test
    void generateToken_shouldReturnValidUUID() {
        // When
        String token = deviceTokenService.generateToken();

        // Then
        assertThat(token).isNotNull();
        assertThat(token).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    void validateToken_withValidToken_shouldReturnTrue() {
        // Given
        String token = UUID.randomUUID().toString();
        testDevice.setApiToken(token);
        when(deviceRepository.findByApiToken(token)).thenReturn(Optional.of(testDevice));

        // When
        boolean isValid = deviceTokenService.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
        verify(deviceRepository).findByApiToken(token);
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() {
        // Given
        String token = "invalid-token";
        when(deviceRepository.findByApiToken(token)).thenReturn(Optional.empty());

        // When
        boolean isValid = deviceTokenService.validateToken(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_withNullToken_shouldReturnFalse() {
        // When
        boolean isValid = deviceTokenService.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
        verify(deviceRepository, never()).findByApiToken(any());
    }

    @Test
    void validateToken_withEmptyToken_shouldReturnFalse() {
        // When
        boolean isValid = deviceTokenService.validateToken("");

        // Then
        assertThat(isValid).isFalse();
        verify(deviceRepository, never()).findByApiToken(any());
    }

    @Test
    void getDeviceByToken_withValidToken_shouldReturnDevice() {
        // Given
        String token = UUID.randomUUID().toString();
        testDevice.setApiToken(token);
        when(deviceRepository.findByApiTokenWithOrganization(token)).thenReturn(Optional.of(testDevice));

        // When
        Optional<Device> result = deviceTokenService.getDeviceByToken(token);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getExternalId()).isEqualTo("test-device-001");
        verify(deviceRepository).findByApiTokenWithOrganization(token);
    }

    @Test
    void getDeviceByToken_withInvalidToken_shouldReturnEmpty() {
        // Given
        String token = "invalid-token";
        when(deviceRepository.findByApiTokenWithOrganization(token)).thenReturn(Optional.empty());

        // When
        Optional<Device> result = deviceTokenService.getDeviceByToken(token);

        // Then
        assertThat(result).isEmpty();
        verify(deviceRepository).findByApiTokenWithOrganization(token);
    }

    @Test
    void assignTokenToDevice_shouldGenerateAndSaveToken() {
        // Given
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        String token = deviceTokenService.assignTokenToDevice(testDevice);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertThat(savedDevice.getApiToken()).isEqualTo(token);
        assertThat(savedDevice.getTokenCreatedAt()).isNotNull();
    }

    @Test
    void rotateToken_withValidDevice_shouldGenerateNewToken() {
        // Given
        UUID deviceId = testDevice.getId();
        String oldToken = UUID.randomUUID().toString();
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
        testDevice.setApiToken(oldToken);
        testDevice.setTokenCreatedAt(originalCreatedAt);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        String newToken = deviceTokenService.rotateToken(deviceId);

        // Then
        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(newToken).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertThat(savedDevice.getApiToken()).isEqualTo(newToken);
        assertThat(savedDevice.getTokenCreatedAt()).isAfter(originalCreatedAt);
    }

    @Test
    void rotateToken_withNonexistentDevice_shouldThrowException() {
        // Given
        UUID deviceId = UUID.randomUUID();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> deviceTokenService.rotateToken(deviceId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device not found");
    }

    @Test
    void revokeToken_withValidDevice_shouldRemoveToken() {
        // Given
        UUID deviceId = testDevice.getId();
        testDevice.setApiToken(UUID.randomUUID().toString());
        testDevice.setTokenCreatedAt(LocalDateTime.now());
        testDevice.setTokenLastUsedAt(LocalDateTime.now());

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        deviceTokenService.revokeToken(deviceId);

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertThat(savedDevice.getApiToken()).isNull();
        assertThat(savedDevice.getTokenCreatedAt()).isNull();
        assertThat(savedDevice.getTokenLastUsedAt()).isNull();
    }

    @Test
    void revokeToken_withNonexistentDevice_shouldThrowException() {
        // Given
        UUID deviceId = UUID.randomUUID();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> deviceTokenService.revokeToken(deviceId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device not found");
    }

    @Test
    void updateTokenLastUsed_withValidToken_shouldUpdateTimestamp() {
        // Given
        String token = UUID.randomUUID().toString();
        LocalDateTime beforeUpdate = LocalDateTime.now();
        testDevice.setApiToken(token);
        testDevice.setTokenLastUsedAt(beforeUpdate.minusHours(1));

        when(deviceRepository.findByApiToken(token)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // When
        deviceTokenService.updateTokenLastUsed(token);

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertThat(savedDevice.getTokenLastUsedAt()).isAfter(beforeUpdate.minusHours(1));
    }

    @Test
    void updateTokenLastUsed_withInvalidToken_shouldNotUpdate() {
        // Given
        String token = "invalid-token";
        when(deviceRepository.findByApiToken(token)).thenReturn(Optional.empty());

        // When
        deviceTokenService.updateTokenLastUsed(token);

        // Then
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void isDeviceToken_withValidUUID_shouldReturnTrue() {
        // Given
        String validUUID = "550e8400-e29b-41d4-a716-446655440000";

        // When
        boolean result = deviceTokenService.isDeviceToken(validUUID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isDeviceToken_withInvalidFormat_shouldReturnFalse() {
        // Given
        String[] invalidTokens = {
                "not-a-uuid",
                "550e8400-e29b-41d4-a716",  // Too short
                "550e8400-e29b-41d4-a716-446655440000-extra",  // Too long
                "",
                null
        };

        // When/Then
        for (String invalidToken : invalidTokens) {
            boolean result = deviceTokenService.isDeviceToken(invalidToken);
            assertThat(result).isFalse();
        }
    }

    @Test
    void isDeviceToken_withJWT_shouldReturnFalse() {
        // Given
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";

        // When
        boolean result = deviceTokenService.isDeviceToken(jwtToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void multipleTokenGeneration_shouldProduceUniqueTokens() {
        // When
        String token1 = deviceTokenService.generateToken();
        String token2 = deviceTokenService.generateToken();
        String token3 = deviceTokenService.generateToken();

        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(token2).isNotEqualTo(token3);
        assertThat(token1).isNotEqualTo(token3);
    }
}
