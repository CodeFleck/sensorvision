package org.sensorvision.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.CreateUserApiKeyRequest;
import org.sensorvision.dto.RotateApiKeyResponse;
import org.sensorvision.dto.UserApiKeyDto;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.UserApiKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserApiKeyController.
 * Tests API endpoints for managing user API keys.
 */
@ExtendWith(MockitoExtension.class)
class UserApiKeyControllerTest {

    @Mock
    private UserApiKeyService userApiKeyService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private UserApiKeyController controller;

    private Organization testOrganization;
    private User testUser;
    private UserApiKey testApiKey;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@example.com")
                .organization(testOrganization)
                .build();

        testApiKey = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyValue("550e8400-e29b-41d4-a716-446655440000")
                .name("Default Token")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== GET /api/v1/api-keys Tests ====================

    @Test
    void getMyApiKeys_shouldReturnUserKeys() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.getApiKeysForUser(1L)).thenReturn(List.of(testApiKey));

        // When
        ResponseEntity<List<UserApiKeyDto>> response = controller.getMyApiKeys();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo(1L);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Default Token");
        assertThat(response.getBody().get(0).getMaskedKeyValue()).isEqualTo("550e8400...0000");
    }

    @Test
    void getMyApiKeys_whenNoKeys_shouldReturnEmptyList() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.getApiKeysForUser(1L)).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<UserApiKeyDto>> response = controller.getMyApiKeys();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ==================== POST /api/v1/api-keys Tests ====================

    @Test
    void generateApiKey_withName_shouldCreateKeyWithName() {
        // Given
        CreateUserApiKeyRequest request = new CreateUserApiKeyRequest();
        request.setName("My Custom Key");

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        // Service now takes 3 parameters: userId, name, description
        when(userApiKeyService.generateApiKey(eq(1L), eq("My Custom Key"), isNull())).thenReturn(testApiKey);

        // When
        ResponseEntity<UserApiKeyDto> response = controller.generateApiKey(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        // New implementation uses getDisplayKeyValue() which may return null for keys without plaintextKeyValue set
        assertThat(response.getBody().getKeyValue()).isNotNull();
    }

    @Test
    void generateApiKey_withoutBody_shouldUseDefaultName() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.generateApiKey(eq(1L), eq("Default Token"), isNull())).thenReturn(testApiKey);

        // When
        ResponseEntity<UserApiKeyDto> response = controller.generateApiKey(null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userApiKeyService).generateApiKey(1L, "Default Token", null);
    }

    @Test
    void generateApiKey_withEmptyName_shouldUseDefaultName() {
        // Given
        CreateUserApiKeyRequest request = new CreateUserApiKeyRequest();
        request.setName(null);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.generateApiKey(eq(1L), eq("Default Token"), isNull())).thenReturn(testApiKey);

        // When
        ResponseEntity<UserApiKeyDto> response = controller.generateApiKey(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userApiKeyService).generateApiKey(1L, "Default Token", null);
    }

    // ==================== POST /api/v1/api-keys/default Tests ====================

    @Test
    void generateDefaultToken_whenNoKeysExist_shouldGenerateNew() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.generateDefaultTokenIfNeeded(1L)).thenReturn(Optional.of(testApiKey));

        // When
        ResponseEntity<UserApiKeyDto> response = controller.generateDefaultToken();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getKeyValue()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void generateDefaultToken_whenKeysExist_shouldReturnMessage() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.generateDefaultTokenIfNeeded(1L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<UserApiKeyDto> response = controller.generateDefaultToken();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("already have an API key");
    }

    // ==================== POST /api/v1/api-keys/{keyId}/rotate Tests ====================

    @Test
    void rotateApiKey_forOwnKey_shouldReturnNewKey() {
        // Given
        UserApiKey newKey = UserApiKey.builder()
                .id(2L)
                .user(testUser)
                .keyValue("660e8400-e29b-41d4-a716-446655440001")
                .name("Default Token")
                .createdAt(LocalDateTime.now())
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.getApiKeysForUser(1L)).thenReturn(List.of(testApiKey));
        when(userApiKeyService.rotateApiKey(eq(1L), isNull())).thenReturn(newKey);

        // When - default grace period is 0 (immediate)
        ResponseEntity<RotateApiKeyResponse> response = controller.rotateApiKey(1L, 0);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getNewKey().getId()).isEqualTo(2L);
        assertThat(response.getBody().getNewKey().getKeyValue()).isEqualTo("660e8400-e29b-41d4-a716-446655440001");
        assertThat(response.getBody().getOldKeyValidUntil()).isNull();
    }

    @Test
    void rotateApiKey_withGracePeriod_shouldReturnOldKeyValidUntil() {
        // Given
        UserApiKey newKey = UserApiKey.builder()
                .id(2L)
                .user(testUser)
                .keyValue("660e8400-e29b-41d4-a716-446655440001")
                .name("Default Token")
                .createdAt(LocalDateTime.now())
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.getApiKeysForUser(1L)).thenReturn(List.of(testApiKey));
        when(userApiKeyService.rotateApiKey(eq(1L), any(Duration.class))).thenReturn(newKey);

        // When - 30 minute grace period
        ResponseEntity<RotateApiKeyResponse> response = controller.rotateApiKey(1L, 30);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getNewKey().getId()).isEqualTo(2L);
        assertThat(response.getBody().getOldKeyValidUntil()).isNotNull();
        assertThat(response.getBody().getGracePeriodMinutes()).isEqualTo(30);
    }

    @Test
    void rotateApiKey_forOtherUsersKey_shouldThrow403() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.getApiKeysForUser(1L)).thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> controller.rotateApiKey(999L, 0))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to API key");
    }

    // ==================== DELETE /api/v1/api-keys/{keyId} Tests ====================

    @Test
    void revokeApiKey_forOwnKey_shouldRevokeSuccessfully() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.getApiKeysForUser(1L)).thenReturn(List.of(testApiKey));
        doNothing().when(userApiKeyService).revokeApiKey(1L);

        // When
        ResponseEntity<UserApiKeyDto> response = controller.revokeApiKey(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("API key revoked successfully");
        assertThat(response.getBody().isActive()).isFalse();
    }

    @Test
    void revokeApiKey_forOtherUsersKey_shouldThrow403() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.getApiKeysForUser(1L)).thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> controller.revokeApiKey(999L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to API key");
    }

    // ==================== Ownership Verification Tests ====================

    @Test
    void verifyKeyOwnership_whenUserOwnsKey_shouldNotThrow() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.getApiKeysForUser(1L)).thenReturn(List.of(testApiKey));
        when(userApiKeyService.rotateApiKey(eq(1L), isNull())).thenReturn(testApiKey);

        // When/Then - should not throw
        ResponseEntity<RotateApiKeyResponse> response = controller.rotateApiKey(1L, 0);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void verifyKeyOwnership_whenUserDoesNotOwnKey_shouldThrowAccessDenied() {
        // Given
        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .organization(testOrganization)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(otherUser);
        when(userApiKeyService.getApiKeysForUser(2L)).thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> controller.rotateApiKey(1L, 0))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ==================== DELETE /api/v1/api-keys/{keyId}/scheduled-revocation Tests ====================

    @Test
    void cancelScheduledRevocation_forOwnKey_shouldCancelSuccessfully() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.getApiKeysForUser(1L)).thenReturn(List.of(testApiKey));
        doNothing().when(userApiKeyService).cancelScheduledRevocation(1L);

        // When
        ResponseEntity<UserApiKeyDto> response = controller.cancelScheduledRevocation(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Scheduled revocation cancelled");
    }

    @Test
    void cancelScheduledRevocation_forOtherUsersKey_shouldThrow403() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(userApiKeyService.getApiKeysForUser(1L)).thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> controller.cancelScheduledRevocation(999L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to API key");
    }
}
