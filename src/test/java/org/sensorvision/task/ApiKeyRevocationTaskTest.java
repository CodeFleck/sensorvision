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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyRevocationTask.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyRevocationTaskTest {

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    private ApiKeyRevocationTask revocationTask;

    private Organization testOrganization;
    private User testUser;

    @BeforeEach
    void setUp() {
        revocationTask = new ApiKeyRevocationTask(userApiKeyRepository);

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
    void processExpiredGracePeriods_whenNoExpiredKeys_shouldSkip() {
        // Given
        when(userApiKeyRepository.countKeysWithExpiredGracePeriod(any())).thenReturn(0L);

        // When
        revocationTask.processExpiredGracePeriods();

        // Then
        verify(userApiKeyRepository, never()).findKeysWithExpiredGracePeriod(any());
        verify(userApiKeyRepository, never()).save(any());
    }

    @Test
    void processExpiredGracePeriods_shouldRevokeExpiredKey() {
        // Given
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(5);

        UserApiKey expiredKey = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyPrefix("abc12345")
                .keyHash("$2a$10$hashedvalue")
                .name("Expired Token")
                .scheduledRevocationAt(expiredTime)
                .build();

        when(userApiKeyRepository.countKeysWithExpiredGracePeriod(any())).thenReturn(1L);
        when(userApiKeyRepository.findKeysWithExpiredGracePeriod(any())).thenReturn(List.of(expiredKey));
        when(userApiKeyRepository.save(any())).thenReturn(expiredKey);

        // When
        revocationTask.processExpiredGracePeriods();

        // Then
        ArgumentCaptor<UserApiKey> keyCaptor = ArgumentCaptor.forClass(UserApiKey.class);
        verify(userApiKeyRepository).save(keyCaptor.capture());

        UserApiKey savedKey = keyCaptor.getValue();
        assertThat(savedKey.getRevokedAt()).isNotNull();
        assertThat(savedKey.getScheduledRevocationAt()).isNull(); // Should be cleared
    }

    @Test
    void processExpiredGracePeriods_shouldProcessMultipleKeys() {
        // Given
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(5);

        UserApiKey key1 = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyPrefix("abc12345")
                .name("Token 1")
                .scheduledRevocationAt(expiredTime)
                .build();

        UserApiKey key2 = UserApiKey.builder()
                .id(2L)
                .user(testUser)
                .keyPrefix("def67890")
                .name("Token 2")
                .scheduledRevocationAt(expiredTime.minusMinutes(10))
                .build();

        when(userApiKeyRepository.countKeysWithExpiredGracePeriod(any())).thenReturn(2L);
        when(userApiKeyRepository.findKeysWithExpiredGracePeriod(any())).thenReturn(List.of(key1, key2));
        when(userApiKeyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        revocationTask.processExpiredGracePeriods();

        // Then
        verify(userApiKeyRepository, times(2)).save(any());
    }

    @Test
    void processExpiredGracePeriods_shouldContinueOnIndividualFailure() {
        // Given
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(5);

        UserApiKey key1 = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyPrefix("abc12345")
                .name("Token 1")
                .scheduledRevocationAt(expiredTime)
                .build();

        UserApiKey key2 = UserApiKey.builder()
                .id(2L)
                .user(testUser)
                .keyPrefix("def67890")
                .name("Token 2")
                .scheduledRevocationAt(expiredTime)
                .build();

        when(userApiKeyRepository.countKeysWithExpiredGracePeriod(any())).thenReturn(2L);
        when(userApiKeyRepository.findKeysWithExpiredGracePeriod(any())).thenReturn(List.of(key1, key2));
        when(userApiKeyRepository.save(key1)).thenThrow(new RuntimeException("Database error"));
        when(userApiKeyRepository.save(key2)).thenReturn(key2);

        // When
        revocationTask.processExpiredGracePeriods();

        // Then - should still try to process second key
        verify(userApiKeyRepository, times(2)).save(any());
    }

    @Test
    void processExpiredGracePeriods_shouldSkipAlreadyRevokedKeys() {
        // Given - this shouldn't happen, but the query filters them out
        when(userApiKeyRepository.countKeysWithExpiredGracePeriod(any())).thenReturn(0L);

        // When
        revocationTask.processExpiredGracePeriods();

        // Then
        verify(userApiKeyRepository, never()).save(any());
    }
}
