package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.repository.UserApiKeyRepository;
import org.sensorvision.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserApiKeyService.
 * Tests key generation, validation, rotation, and revocation.
 */
@ExtendWith(MockitoExtension.class)
class UserApiKeyServiceTest {

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserApiKeyService userApiKeyService;

    @Captor
    private ArgumentCaptor<UserApiKey> apiKeyCaptor;

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

    // ==================== generateApiKey() Tests ====================

    @Test
    void generateApiKey_withValidUser_shouldCreateNewKey() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            key.setId(1L);
            return key;
        });

        // When
        UserApiKey result = userApiKeyService.generateApiKey(1L, "My API Key");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("My API Key");
        assertThat(result.getKeyValue()).isNotNull();
        assertThat(result.getKeyValue()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.isActive()).isTrue();

        verify(userApiKeyRepository).save(apiKeyCaptor.capture());
        UserApiKey savedKey = apiKeyCaptor.getValue();
        assertThat(savedKey.getName()).isEqualTo("My API Key");
    }

    @Test
    void generateApiKey_withDefaultName_shouldUseDefaultToken() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            key.setId(1L);
            return key;
        });

        // When
        UserApiKey result = userApiKeyService.generateApiKey(1L, null);

        // Then
        verify(userApiKeyRepository).save(apiKeyCaptor.capture());
        assertThat(apiKeyCaptor.getValue().getName()).isEqualTo("Default Token");
    }

    @Test
    void generateApiKey_withNonexistentUser_shouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userApiKeyService.generateApiKey(999L, "Test Key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void generateApiKey_shouldHandleKeyCollision() {
        // Given: First key value already exists, second is unique
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.existsByKeyValue(anyString()))
                .thenReturn(true)   // First attempt - collision
                .thenReturn(false); // Second attempt - unique
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            key.setId(1L);
            return key;
        });

        // When
        UserApiKey result = userApiKeyService.generateApiKey(1L, "Test Key");

        // Then
        assertThat(result).isNotNull();
        verify(userApiKeyRepository, times(2)).existsByKeyValue(anyString());
    }

    // ==================== generateDefaultTokenIfNeeded() Tests ====================

    @Test
    void generateDefaultTokenIfNeeded_whenNoKeysExist_shouldGenerateNew() {
        // Given
        when(userApiKeyRepository.hasActiveKeys(1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            key.setId(1L);
            return key;
        });

        // When
        Optional<UserApiKey> result = userApiKeyService.generateDefaultTokenIfNeeded(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Default Token");
    }

    @Test
    void generateDefaultTokenIfNeeded_whenKeysExist_shouldReturnEmpty() {
        // Given
        when(userApiKeyRepository.hasActiveKeys(1L)).thenReturn(true);

        // When
        Optional<UserApiKey> result = userApiKeyService.generateDefaultTokenIfNeeded(1L);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).findById(any());
        verify(userApiKeyRepository, never()).save(any());
    }

    // ==================== validateApiKey() Tests ====================

    @Test
    void validateApiKey_withValidKey_shouldReturnApiKey() {
        // Given
        String keyValue = "550e8400-e29b-41d4-a716-446655440000";
        when(userApiKeyRepository.findActiveByKeyValue(keyValue)).thenReturn(Optional.of(testApiKey));

        // When
        Optional<UserApiKey> result = userApiKeyService.validateApiKey(keyValue);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testApiKey);
    }

    @Test
    void validateApiKey_withInvalidKey_shouldReturnEmpty() {
        // Given
        String keyValue = "invalid-key";
        when(userApiKeyRepository.findActiveByKeyValue(keyValue)).thenReturn(Optional.empty());

        // When
        Optional<UserApiKey> result = userApiKeyService.validateApiKey(keyValue);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void validateApiKey_withNullKey_shouldReturnEmpty() {
        // When
        Optional<UserApiKey> result = userApiKeyService.validateApiKey(null);

        // Then
        assertThat(result).isEmpty();
        verify(userApiKeyRepository, never()).findActiveByKeyValue(any());
    }

    @Test
    void validateApiKey_withBlankKey_shouldReturnEmpty() {
        // When
        Optional<UserApiKey> result = userApiKeyService.validateApiKey("   ");

        // Then
        assertThat(result).isEmpty();
        verify(userApiKeyRepository, never()).findActiveByKeyValue(any());
    }

    // ==================== rotateApiKey() Tests ====================

    @Test
    void rotateApiKey_withActiveKey_shouldRevokeAndCreateNew() {
        // Given
        String oldKeyValue = testApiKey.getKeyValue();
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser)); // Need this for generateApiKey
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            if (key.getId() == null) {
                key.setId(2L); // New key
            }
            return key;
        });

        // When
        UserApiKey newKey = userApiKeyService.rotateApiKey(1L);

        // Then
        assertThat(newKey).isNotNull();
        assertThat(newKey.getKeyValue()).isNotEqualTo(oldKeyValue);
        assertThat(newKey.getName()).isEqualTo("Default Token");
        assertThat(testApiKey.getRevokedAt()).isNotNull(); // Old key revoked

        verify(userApiKeyRepository, times(2)).save(any(UserApiKey.class));
    }

    @Test
    void rotateApiKey_withRevokedKey_shouldThrowException() {
        // Given
        testApiKey.setRevokedAt(LocalDateTime.now());
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));

        // When/Then
        assertThatThrownBy(() -> userApiKeyService.rotateApiKey(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot rotate a revoked API key");
    }

    @Test
    void rotateApiKey_withNonexistentKey_shouldThrowException() {
        // Given
        when(userApiKeyRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userApiKeyService.rotateApiKey(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key not found");
    }

    // ==================== revokeApiKey() Tests ====================

    @Test
    void revokeApiKey_withActiveKey_shouldSetRevokedAt() {
        // Given
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenReturn(testApiKey);

        // When
        userApiKeyService.revokeApiKey(1L);

        // Then
        verify(userApiKeyRepository).save(apiKeyCaptor.capture());
        UserApiKey savedKey = apiKeyCaptor.getValue();
        assertThat(savedKey.getRevokedAt()).isNotNull();
        assertThat(savedKey.isActive()).isFalse();
    }

    @Test
    void revokeApiKey_withAlreadyRevokedKey_shouldThrowException() {
        // Given
        testApiKey.setRevokedAt(LocalDateTime.now());
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));

        // When/Then
        assertThatThrownBy(() -> userApiKeyService.revokeApiKey(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API key is already revoked");
    }

    @Test
    void revokeApiKey_withNonexistentKey_shouldThrowException() {
        // Given
        when(userApiKeyRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userApiKeyService.revokeApiKey(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key not found");
    }

    // ==================== getApiKeysForUser() Tests ====================

    @Test
    void getApiKeysForUser_shouldReturnAllUserKeys() {
        // Given
        UserApiKey key2 = UserApiKey.builder()
                .id(2L)
                .user(testUser)
                .keyValue("660e8400-e29b-41d4-a716-446655440001")
                .name("Second Token")
                .build();
        List<UserApiKey> keys = List.of(testApiKey, key2);
        when(userApiKeyRepository.findAllByUserId(1L)).thenReturn(keys);

        // When
        List<UserApiKey> result = userApiKeyService.getApiKeysForUser(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testApiKey, key2);
    }

    @Test
    void getApiKeysForUser_whenNoKeys_shouldReturnEmptyList() {
        // Given
        when(userApiKeyRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());

        // When
        List<UserApiKey> result = userApiKeyService.getApiKeysForUser(1L);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== updateLastUsedAt() Tests ====================

    @Test
    void updateLastUsedAt_shouldUpdateTimestamp() {
        // Given
        LocalDateTime beforeUpdate = LocalDateTime.now();

        // When
        userApiKeyService.updateLastUsedAt(1L);

        // Then
        verify(userApiKeyRepository).updateLastUsedAt(eq(1L), any(LocalDateTime.class));
    }

    // ==================== Edge Cases ====================

    @Test
    void multipleKeyGeneration_shouldProduceUniqueKeys() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            key.setId(System.nanoTime()); // Unique ID
            return key;
        });

        // When
        UserApiKey key1 = userApiKeyService.generateApiKey(1L, "Key 1");
        UserApiKey key2 = userApiKeyService.generateApiKey(1L, "Key 2");
        UserApiKey key3 = userApiKeyService.generateApiKey(1L, "Key 3");

        // Then
        assertThat(key1.getKeyValue()).isNotEqualTo(key2.getKeyValue());
        assertThat(key2.getKeyValue()).isNotEqualTo(key3.getKeyValue());
        assertThat(key1.getKeyValue()).isNotEqualTo(key3.getKeyValue());
    }
}
