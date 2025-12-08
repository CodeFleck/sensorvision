package org.sensorvision.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.repository.UserApiKeyRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyMigrationTask.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyMigrationTaskTest {

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ApiKeyMigrationTask migrationTask;

    private Organization testOrganization;
    private User testUser;

    @BeforeEach
    void setUp() {
        migrationTask = new ApiKeyMigrationTask(userApiKeyRepository, passwordEncoder);

        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .organization(testOrganization)
                .roles(Set.of())
                .build();
    }

    @Test
    void migrateLegacyApiKeys_whenNoLegacyKeys_shouldSkipMigration() {
        // Given
        when(userApiKeyRepository.countLegacyPlaintextKeys()).thenReturn(0L);

        // When
        migrationTask.migrateLegacyApiKeys();

        // Then
        verify(userApiKeyRepository, never()).findLegacyPlaintextKeys();
        verify(userApiKeyRepository, never()).save(any());
    }

    @Test
    void migrateLegacyApiKeys_shouldHashAndClearPlaintextValue() {
        // Given
        String plaintextKey = "550e8400-e29b-41d4-a716-446655440000";
        String expectedPrefix = "550e8400";
        String hashedValue = "$2a$10$hashedvalue";

        UserApiKey legacyKey = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyValue(plaintextKey)
                .keyPrefix(null)
                .keyHash(null)
                .name("Legacy Token")
                .build();

        when(userApiKeyRepository.countLegacyPlaintextKeys()).thenReturn(1L);
        when(userApiKeyRepository.findLegacyPlaintextKeys()).thenReturn(List.of(legacyKey));
        when(passwordEncoder.encode(plaintextKey)).thenReturn(hashedValue);
        when(userApiKeyRepository.existsByKeyPrefix(expectedPrefix)).thenReturn(false);
        when(userApiKeyRepository.save(any())).thenReturn(legacyKey);

        // When
        migrationTask.migrateLegacyApiKeys();

        // Then
        ArgumentCaptor<UserApiKey> keyCaptor = ArgumentCaptor.forClass(UserApiKey.class);
        verify(userApiKeyRepository).save(keyCaptor.capture());

        UserApiKey savedKey = keyCaptor.getValue();
        assertThat(savedKey.getKeyPrefix()).isEqualTo(expectedPrefix);
        assertThat(savedKey.getKeyHash()).isEqualTo(hashedValue);
        assertThat(savedKey.getKeyValue()).isNull(); // Plaintext should be cleared
    }

    @Test
    void migrateLegacyApiKeys_shouldMigrateMultipleKeys() {
        // Given
        UserApiKey key1 = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyValue("11111111-0000-0000-0000-000000000001")
                .name("Token 1")
                .build();

        UserApiKey key2 = UserApiKey.builder()
                .id(2L)
                .user(testUser)
                .keyValue("22222222-0000-0000-0000-000000000002")
                .name("Token 2")
                .build();

        when(userApiKeyRepository.countLegacyPlaintextKeys()).thenReturn(2L);
        when(userApiKeyRepository.findLegacyPlaintextKeys()).thenReturn(List.of(key1, key2));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(userApiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
        when(userApiKeyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        migrationTask.migrateLegacyApiKeys();

        // Then
        verify(userApiKeyRepository, times(2)).save(any());
        verify(passwordEncoder, times(2)).encode(anyString());
    }

    @Test
    void migrateLegacyApiKeys_shouldHandlePrefixCollision() {
        // Given
        String plaintextKey = "550e8400-e29b-41d4-a716-446655440000";

        UserApiKey legacyKey = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyValue(plaintextKey)
                .keyPrefix(null)
                .keyHash(null)
                .name("Legacy Token")
                .build();

        when(userApiKeyRepository.countLegacyPlaintextKeys()).thenReturn(1L);
        when(userApiKeyRepository.findLegacyPlaintextKeys()).thenReturn(List.of(legacyKey));
        when(passwordEncoder.encode(plaintextKey)).thenReturn("$2a$10$hashed");
        when(userApiKeyRepository.existsByKeyPrefix("550e8400")).thenReturn(true); // Collision!
        when(userApiKeyRepository.save(any())).thenReturn(legacyKey);

        // When
        migrationTask.migrateLegacyApiKeys();

        // Then - should still migrate despite collision (logged as warning)
        verify(userApiKeyRepository).save(any());
    }

    @Test
    void migrateLegacyApiKeys_shouldSkipKeysWithNullValue() {
        // Given
        UserApiKey keyWithNullValue = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyValue(null) // This shouldn't happen but handle it
                .keyPrefix(null)
                .keyHash(null)
                .name("Broken Token")
                .build();

        when(userApiKeyRepository.countLegacyPlaintextKeys()).thenReturn(1L);
        when(userApiKeyRepository.findLegacyPlaintextKeys()).thenReturn(List.of(keyWithNullValue));

        // When
        migrationTask.migrateLegacyApiKeys();

        // Then
        verify(userApiKeyRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void migrateLegacyApiKeys_shouldContinueOnIndividualFailure() {
        // Given
        UserApiKey key1 = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyValue("11111111-0000-0000-0000-000000000001")
                .name("Token 1")
                .build();

        UserApiKey key2 = UserApiKey.builder()
                .id(2L)
                .user(testUser)
                .keyValue("22222222-0000-0000-0000-000000000002")
                .name("Token 2")
                .build();

        when(userApiKeyRepository.countLegacyPlaintextKeys()).thenReturn(2L);
        when(userApiKeyRepository.findLegacyPlaintextKeys()).thenReturn(List.of(key1, key2));
        when(passwordEncoder.encode("11111111-0000-0000-0000-000000000001"))
                .thenThrow(new RuntimeException("Encoding failed"));
        when(passwordEncoder.encode("22222222-0000-0000-0000-000000000002"))
                .thenReturn("$2a$10$hashed");
        when(userApiKeyRepository.existsByKeyPrefix(anyString())).thenReturn(false);
        when(userApiKeyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        migrationTask.migrateLegacyApiKeys();

        // Then - second key should still be saved
        verify(userApiKeyRepository, times(1)).save(any());
    }

    @Test
    void migrateLegacyApiKeys_shouldHandleShortKeyValue() {
        // Given - key value shorter than prefix length (edge case)
        String shortKey = "abc123";

        UserApiKey legacyKey = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyValue(shortKey)
                .keyPrefix(null)
                .keyHash(null)
                .name("Short Token")
                .build();

        when(userApiKeyRepository.countLegacyPlaintextKeys()).thenReturn(1L);
        when(userApiKeyRepository.findLegacyPlaintextKeys()).thenReturn(List.of(legacyKey));
        when(passwordEncoder.encode(shortKey)).thenReturn("$2a$10$hashed");
        when(userApiKeyRepository.existsByKeyPrefix(shortKey)).thenReturn(false);
        when(userApiKeyRepository.save(any())).thenReturn(legacyKey);

        // When
        migrationTask.migrateLegacyApiKeys();

        // Then
        ArgumentCaptor<UserApiKey> keyCaptor = ArgumentCaptor.forClass(UserApiKey.class);
        verify(userApiKeyRepository).save(keyCaptor.capture());

        UserApiKey savedKey = keyCaptor.getValue();
        assertThat(savedKey.getKeyPrefix()).isEqualTo(shortKey); // Uses full value as prefix
    }
}
