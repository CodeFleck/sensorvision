package org.sensorvision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.repository.UserApiKeyRepository;
import org.sensorvision.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user-level API keys.
 * These keys allow users to access all devices in their organization
 * with a single token (like Ubidots Default Token).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserApiKeyService {

    private final UserApiKeyRepository userApiKeyRepository;
    private final UserRepository userRepository;

    /**
     * Generate a new API key for a user.
     *
     * @param userId The user ID
     * @param name   Optional name for the key (defaults to "Default Token")
     * @return The newly created API key with the full key value (only shown once)
     */
    @Transactional
    public UserApiKey generateApiKey(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String keyValue = generateUniqueKeyValue();

        UserApiKey apiKey = UserApiKey.builder()
                .user(user)
                .keyValue(keyValue)
                .name(name != null && !name.isBlank() ? name : "Default Token")
                .build();

        apiKey = userApiKeyRepository.save(apiKey);

        log.info("Generated new API key '{}' for user {} (org: {})",
                apiKey.getName(), user.getUsername(), user.getOrganization().getName());

        return apiKey;
    }

    /**
     * Generate the first "Default Token" for a user if they don't have any.
     * This is typically called after user registration.
     *
     * @param userId The user ID
     * @return The API key, or empty if user already has keys
     */
    @Transactional
    public Optional<UserApiKey> generateDefaultTokenIfNeeded(Long userId) {
        if (userApiKeyRepository.hasActiveKeys(userId)) {
            return Optional.empty();
        }
        return Optional.of(generateApiKey(userId, "Default Token"));
    }

    /**
     * Validate an API key and return the associated key entity if valid.
     *
     * @param keyValue The API key value
     * @return The API key entity if valid, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<UserApiKey> validateApiKey(String keyValue) {
        if (keyValue == null || keyValue.isBlank()) {
            return Optional.empty();
        }
        return userApiKeyRepository.findActiveByKeyValue(keyValue);
    }

    /**
     * Get all API keys for a user.
     *
     * @param userId The user ID
     * @return List of API keys (active and revoked)
     */
    @Transactional(readOnly = true)
    public List<UserApiKey> getApiKeysForUser(Long userId) {
        return userApiKeyRepository.findAllByUserId(userId);
    }

    /**
     * Get only active API keys for a user.
     *
     * @param userId The user ID
     * @return List of active API keys
     */
    @Transactional(readOnly = true)
    public List<UserApiKey> getActiveApiKeysForUser(Long userId) {
        return userApiKeyRepository.findActiveByUserId(userId);
    }

    /**
     * Rotate an API key (revoke old, create new with same name).
     *
     * @param keyId The ID of the key to rotate
     * @return The new API key
     */
    @Transactional
    public UserApiKey rotateApiKey(Long keyId) {
        UserApiKey oldKey = userApiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        if (!oldKey.isActive()) {
            throw new IllegalStateException("Cannot rotate a revoked API key");
        }

        // Revoke the old key
        oldKey.setRevokedAt(LocalDateTime.now());
        userApiKeyRepository.save(oldKey);

        // Generate a new key with the same name
        UserApiKey newKey = generateApiKey(oldKey.getUser().getId(), oldKey.getName());

        log.info("Rotated API key '{}' for user {}",
                oldKey.getName(), oldKey.getUser().getUsername());

        return newKey;
    }

    /**
     * Revoke an API key.
     *
     * @param keyId The ID of the key to revoke
     */
    @Transactional
    public void revokeApiKey(Long keyId) {
        UserApiKey apiKey = userApiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        if (!apiKey.isActive()) {
            throw new IllegalStateException("API key is already revoked");
        }

        apiKey.setRevokedAt(LocalDateTime.now());
        userApiKeyRepository.save(apiKey);

        log.info("Revoked API key '{}' for user {}",
                apiKey.getName(), apiKey.getUser().getUsername());
    }

    /**
     * Update the last used timestamp for an API key.
     * Called by the authentication filter after successful authentication.
     *
     * @param keyId The API key ID
     */
    @Transactional
    public void updateLastUsedAt(Long keyId) {
        userApiKeyRepository.updateLastUsedAt(keyId, LocalDateTime.now());
    }

    /**
     * Generate a unique key value.
     */
    private String generateUniqueKeyValue() {
        String keyValue;
        do {
            keyValue = UUID.randomUUID().toString();
        } while (userApiKeyRepository.existsByKeyValue(keyValue));
        return keyValue;
    }
}
