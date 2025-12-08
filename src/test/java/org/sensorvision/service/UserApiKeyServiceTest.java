package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.repository.UserApiKeyRepository;
import org.sensorvision.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserApiKeyService userApiKeyService;

    @Captor
    private ArgumentCaptor<UserApiKey> apiKeyCaptor;

    private Organization testOrganization;
    private User testUser;
    private UserApiKey testApiKey;

    @BeforeEach
    void setUp() {
        userApiKeyService = new UserApiKeyService(userApiKeyRepository, userRepository, passwordEncoder);

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
                .keyPrefix("550e8400")
                .keyHash("$2a$10$hashedvalue")
                .keyValue("550e8400-e29b-41d4-a716-446655440000") // Legacy for backwards compatibility
                .name("Default Token")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== generateApiKey() Tests ====================

    @Test
    void generateApiKey_withValidUser_shouldCreateNewKeyWithHash() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.countActiveByUserId(1L)).thenReturn(0L);
        when(userApiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedvalue");
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
        assertThat(result.getKeyHash()).isEqualTo("$2a$10$hashedvalue");
        assertThat(result.getKeyPrefix()).isNotNull();
        assertThat(result.getKeyPrefix()).hasSize(8);
        assertThat(result.getPlaintextKeyValue()).isNotNull();
        assertThat(result.getPlaintextKeyValue()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.isActive()).isTrue();

        verify(passwordEncoder).encode(anyString());
        verify(userApiKeyRepository).save(apiKeyCaptor.capture());
        UserApiKey savedKey = apiKeyCaptor.getValue();
        assertThat(savedKey.getName()).isEqualTo("My API Key");
        assertThat(savedKey.getKeyValue()).isNull(); // Plaintext not stored
    }

    @Test
    void generateApiKey_withDescription_shouldSaveDescription() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.countActiveByUserId(1L)).thenReturn(0L);
        when(userApiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedvalue");
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            key.setId(1L);
            return key;
        });

        // When
        UserApiKey result = userApiKeyService.generateApiKey(1L, "My API Key", "Test description");

        // Then
        verify(userApiKeyRepository).save(apiKeyCaptor.capture());
        UserApiKey savedKey = apiKeyCaptor.getValue();
        assertThat(savedKey.getDescription()).isEqualTo("Test description");
    }

    @Test
    void generateApiKey_withDefaultName_shouldUseDefaultToken() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.countActiveByUserId(1L)).thenReturn(0L);
        when(userApiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedvalue");
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
    void generateApiKey_whenMaxKeysReached_shouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.countActiveByUserId(1L)).thenReturn(10L); // Max is 10

        // When/Then
        assertThatThrownBy(() -> userApiKeyService.generateApiKey(1L, "Test Key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maximum number of API keys");
    }

    @Test
    void generateApiKey_shouldHandlePrefixCollision() {
        // Given: First prefix already exists, second is unique
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.countActiveByUserId(1L)).thenReturn(0L);
        when(userApiKeyRepository.existsByKeyPrefix(anyString()))
                .thenReturn(true)   // First attempt - collision
                .thenReturn(false); // Second attempt - unique
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedvalue");
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            key.setId(1L);
            return key;
        });

        // When
        UserApiKey result = userApiKeyService.generateApiKey(1L, "Test Key");

        // Then
        assertThat(result).isNotNull();
        verify(userApiKeyRepository, times(2)).existsByKeyPrefix(anyString());
    }

    // ==================== generateDefaultTokenIfNeeded() Tests ====================

    @Test
    void generateDefaultTokenIfNeeded_whenNoKeysExist_shouldGenerateNew() {
        // Given
        when(userApiKeyRepository.hasActiveKeys(1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.countActiveByUserId(1L)).thenReturn(0L);
        when(userApiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedvalue");
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
    void validateApiKey_withValidHashedKey_shouldReturnApiKey() {
        // Given
        String keyValue = "550e8400-e29b-41d4-a716-446655440000";
        String keyPrefix = keyValue.substring(0, 8);
        when(userApiKeyRepository.findActiveByKeyPrefix(keyPrefix)).thenReturn(List.of(testApiKey));
        when(passwordEncoder.matches(keyValue, testApiKey.getKeyHash())).thenReturn(true);

        // When
        Optional<UserApiKey> result = userApiKeyService.validateApiKey(keyValue);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testApiKey);
    }

    @Test
    void validateApiKey_withLegacyPlaintextKey_shouldReturnApiKey() {
        // Given - legacy key without hash
        UserApiKey legacyKey = UserApiKey.builder()
                .id(2L)
                .user(testUser)
                .keyValue("660e8400-e29b-41d4-a716-446655440001")
                .keyHash(null) // No hash - legacy key
                .keyPrefix("660e8400")
                .name("Legacy Token")
                .build();

        String keyValue = "660e8400-e29b-41d4-a716-446655440001";
        String keyPrefix = keyValue.substring(0, 8);
        when(userApiKeyRepository.findActiveByKeyPrefix(keyPrefix)).thenReturn(List.of(legacyKey));
        // Note: passwordEncoder.matches is NOT called because keyHash is null (condition short-circuits)

        // When
        Optional<UserApiKey> result = userApiKeyService.validateApiKey(keyValue);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(legacyKey);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void validateApiKey_withInvalidKey_shouldReturnEmpty() {
        // Given - key that doesn't match hash and has no keyValue match
        UserApiKey keyWithOnlyHash = UserApiKey.builder()
                .id(3L)
                .user(testUser)
                .keyPrefix("550e8400")
                .keyHash("$2a$10$differenthash")
                .keyValue(null) // No legacy value
                .name("Hashed Only Token")
                .build();

        String keyValue = "550e8400-e29b-41d4-a716-446655440000";
        String keyPrefix = keyValue.substring(0, 8);
        when(userApiKeyRepository.findActiveByKeyPrefix(keyPrefix)).thenReturn(List.of(keyWithOnlyHash));
        when(passwordEncoder.matches(keyValue, keyWithOnlyHash.getKeyHash())).thenReturn(false);
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
        verify(userApiKeyRepository, never()).findActiveByKeyPrefix(any());
    }

    @Test
    void validateApiKey_withBlankKey_shouldReturnEmpty() {
        // When
        Optional<UserApiKey> result = userApiKeyService.validateApiKey("   ");

        // Then
        assertThat(result).isEmpty();
        verify(userApiKeyRepository, never()).findActiveByKeyPrefix(any());
    }

    @Test
    void validateApiKey_withShortKey_shouldReturnEmpty() {
        // When - key too short for prefix
        Optional<UserApiKey> result = userApiKeyService.validateApiKey("abc");

        // Then
        assertThat(result).isEmpty();
        verify(userApiKeyRepository, never()).findActiveByKeyPrefix(any());
    }

    // ==================== rotateApiKey() Tests ====================

    @Test
    void rotateApiKey_withActiveKey_shouldRevokeAndCreateNew() {
        // Given
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.countActiveByUserId(1L)).thenReturn(0L); // After revoke
        when(userApiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$newhashedvalue");
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
        assertThat(newKey.getName()).isEqualTo("Default Token");
        assertThat(newKey.getKeyHash()).isEqualTo("$2a$10$newhashedvalue");
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
                .keyPrefix("660e8400")
                .keyHash("$2a$10$anotherhash")
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
        // When
        userApiKeyService.updateLastUsedAt(1L);

        // Then
        verify(userApiKeyRepository).updateLastUsedAt(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void markKeyUsed_shouldTrackForBatchUpdate() {
        // When
        userApiKeyService.markKeyUsed(1L);
        userApiKeyService.markKeyUsed(2L);

        // Then - no immediate database call
        verify(userApiKeyRepository, never()).updateLastUsedAt(any(), any());
    }

    @Test
    void flushLastUsedUpdates_shouldPersistPendingUpdates() {
        // Given
        userApiKeyService.markKeyUsed(1L);
        userApiKeyService.markKeyUsed(2L);

        // When
        userApiKeyService.flushLastUsedUpdates();

        // Then
        verify(userApiKeyRepository, times(2)).updateLastUsedAt(any(), any());
    }

    // ==================== rotateApiKey() with Grace Period Tests ====================

    @Test
    void rotateApiKey_withGracePeriod_shouldScheduleRevocationInsteadOfImmediateRevoke() {
        // Given
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.countActiveByUserId(1L)).thenReturn(0L);
        when(userApiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$newhashedvalue");
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            if (key.getId() == null) {
                key.setId(2L);
            }
            return key;
        });

        // When
        UserApiKey newKey = userApiKeyService.rotateApiKey(1L, Duration.ofMinutes(30));

        // Then
        assertThat(newKey).isNotNull();
        assertThat(newKey.getId()).isEqualTo(2L);
        assertThat(testApiKey.getRevokedAt()).isNull(); // Not immediately revoked
        assertThat(testApiKey.getScheduledRevocationAt()).isNotNull();
        assertThat(testApiKey.getScheduledRevocationAt()).isAfter(LocalDateTime.now());
        assertThat(testApiKey.getScheduledRevocationAt()).isBefore(LocalDateTime.now().plusMinutes(31));
    }

    @Test
    void rotateApiKey_withZeroGracePeriod_shouldRevokeImmediately() {
        // Given
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.countActiveByUserId(1L)).thenReturn(0L);
        when(userApiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$newhashedvalue");
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            if (key.getId() == null) {
                key.setId(2L);
            }
            return key;
        });

        // When
        UserApiKey newKey = userApiKeyService.rotateApiKey(1L, Duration.ZERO);

        // Then
        assertThat(testApiKey.getRevokedAt()).isNotNull();
        assertThat(testApiKey.getScheduledRevocationAt()).isNull();
    }

    @Test
    void rotateApiKey_withNullGracePeriod_shouldRevokeImmediately() {
        // Given
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userApiKeyRepository.countActiveByUserId(1L)).thenReturn(0L);
        when(userApiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
        when(userApiKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$newhashedvalue");
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> {
            UserApiKey key = invocation.getArgument(0);
            if (key.getId() == null) {
                key.setId(2L);
            }
            return key;
        });

        // When
        UserApiKey newKey = userApiKeyService.rotateApiKey(1L, null);

        // Then
        assertThat(testApiKey.getRevokedAt()).isNotNull();
        assertThat(testApiKey.getScheduledRevocationAt()).isNull();
    }

    @Test
    void rotateApiKey_withExcessiveGracePeriod_shouldThrowException() {
        // Given
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));

        // When/Then - 8 days exceeds 7 day max
        assertThatThrownBy(() -> userApiKeyService.rotateApiKey(1L, Duration.ofDays(8)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Grace period cannot exceed 7 days");
    }

    @Test
    void rotateApiKey_withPendingRotation_shouldThrowException() {
        // Given - key already has a pending revocation
        testApiKey.setScheduledRevocationAt(LocalDateTime.now().plusMinutes(30));
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));

        // When/Then
        assertThatThrownBy(() -> userApiKeyService.rotateApiKey(1L, Duration.ofMinutes(30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has a pending rotation");
    }

    // ==================== cancelScheduledRevocation() Tests ====================

    @Test
    void cancelScheduledRevocation_withPendingRevocation_shouldClearSchedule() {
        // Given
        testApiKey.setScheduledRevocationAt(LocalDateTime.now().plusMinutes(30));
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));
        when(userApiKeyRepository.save(any())).thenReturn(testApiKey);

        // When
        userApiKeyService.cancelScheduledRevocation(1L);

        // Then
        verify(userApiKeyRepository).save(apiKeyCaptor.capture());
        assertThat(apiKeyCaptor.getValue().getScheduledRevocationAt()).isNull();
    }

    @Test
    void cancelScheduledRevocation_withNoSchedule_shouldThrowException() {
        // Given
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));

        // When/Then
        assertThatThrownBy(() -> userApiKeyService.cancelScheduledRevocation(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has no scheduled revocation");
    }

    @Test
    void cancelScheduledRevocation_withRevokedKey_shouldThrowException() {
        // Given
        testApiKey.setRevokedAt(LocalDateTime.now());
        testApiKey.setScheduledRevocationAt(LocalDateTime.now().plusMinutes(30));
        when(userApiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));

        // When/Then
        assertThatThrownBy(() -> userApiKeyService.cancelScheduledRevocation(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already revoked");
    }

    // ==================== UserApiKey.isActive() with Scheduled Revocation Tests ====================

    @Test
    void isActive_withScheduledRevocationInFuture_shouldReturnTrue() {
        // Given
        testApiKey.setScheduledRevocationAt(LocalDateTime.now().plusMinutes(30));

        // Then
        assertThat(testApiKey.isActive()).isTrue();
    }

    @Test
    void isActive_withScheduledRevocationInPast_shouldReturnFalse() {
        // Given
        testApiKey.setScheduledRevocationAt(LocalDateTime.now().minusMinutes(1));

        // Then
        assertThat(testApiKey.isActive()).isFalse();
    }

    @Test
    void hasPendingRevocation_withScheduledRevocation_shouldReturnTrue() {
        // Given
        testApiKey.setScheduledRevocationAt(LocalDateTime.now().plusMinutes(30));

        // Then
        assertThat(testApiKey.hasPendingRevocation()).isTrue();
    }

    @Test
    void hasPendingRevocation_withoutScheduledRevocation_shouldReturnFalse() {
        // Then
        assertThat(testApiKey.hasPendingRevocation()).isFalse();
    }

    @Test
    void hasPendingRevocation_whenRevoked_shouldReturnFalse() {
        // Given
        testApiKey.setScheduledRevocationAt(LocalDateTime.now().plusMinutes(30));
        testApiKey.setRevokedAt(LocalDateTime.now());

        // Then
        assertThat(testApiKey.hasPendingRevocation()).isFalse();
    }
}
