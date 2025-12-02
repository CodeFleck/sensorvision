package org.sensorvision.service;

import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.repository.UserApiKeyRepository;
import org.sensorvision.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for managing user-level API keys.
 * These keys allow users to access all devices in their organization
 * with a single token (like Ubidots Default Token).
 * <p>
 * Security: API keys are stored using BCrypt hashing with a prefix for lookups.
 */
@Service
@Slf4j
public class UserApiKeyService {

    private static final int MAX_KEYS_PER_USER = 10;
    private static final int KEY_PREFIX_LENGTH = 8;

    private final UserApiKeyRepository userApiKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor with @Lazy on PasswordEncoder to break circular dependency.
     * The circular dependency occurs because:
     * SecurityConfig -> passwordEncoder -> UserApiKeyService -> PasswordEncoder -> SecurityConfig
     */
    public UserApiKeyService(
            UserApiKeyRepository userApiKeyRepository,
            UserRepository userRepository,
            @Lazy PasswordEncoder passwordEncoder) {
        this.userApiKeyRepository = userApiKeyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Batch last-used updates to reduce database writes
    private final ConcurrentMap<Long, LocalDateTime> pendingLastUsedUpdates = new ConcurrentHashMap<>();

    /**
     * Generate a new API key for a user.
     *
     * @param userId      The user ID
     * @param name        Optional name for the key (defaults to "Default Token")
     * @param description Optional description for the key
     * @return The newly created API key with the full key value (only shown once)
     * @throws IllegalArgumentException if user not found
     * @throws IllegalStateException    if user has reached maximum key limit
     */
    @Transactional
    public UserApiKey generateApiKey(Long userId, String name, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Enforce maximum keys per user
        long activeKeyCount = userApiKeyRepository.countActiveByUserId(userId);
        if (activeKeyCount >= MAX_KEYS_PER_USER) {
            throw new IllegalStateException("Maximum number of API keys (" + MAX_KEYS_PER_USER + ") reached");
        }

        // Generate a unique key value
        String plaintextKey = generateUniqueKeyValue();
        String keyPrefix = plaintextKey.substring(0, KEY_PREFIX_LENGTH);
        String keyHash = passwordEncoder.encode(plaintextKey);

        UserApiKey apiKey = UserApiKey.builder()
                .user(user)
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .keyValue(null) // Don't store plaintext - will be returned via transient field
                .name(name != null && !name.isBlank() ? name : "Default Token")
                .description(description)
                .build();

        apiKey = userApiKeyRepository.save(apiKey);

        // Set the transient field for one-time display
        apiKey.setPlaintextKeyValue(plaintextKey);

        log.info("Generated new API key '{}' for user {} (org: {})",
                apiKey.getName(), user.getUsername(),
                user.getOrganization() != null ? user.getOrganization().getName() : "N/A");

        return apiKey;
    }

    /**
     * Generate a new API key for a user (without description).
     *
     * @param userId The user ID
     * @param name   Optional name for the key (defaults to "Default Token")
     * @return The newly created API key with the full key value (only shown once)
     */
    @Transactional
    public UserApiKey generateApiKey(Long userId, String name) {
        return generateApiKey(userId, name, null);
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
     * Uses prefix-based lookup with BCrypt hash validation for security.
     *
     * @param keyValue The API key value
     * @return The API key entity if valid, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<UserApiKey> validateApiKey(String keyValue) {
        if (keyValue == null || keyValue.isBlank()) {
            return Optional.empty();
        }

        // Extract prefix for database lookup
        if (keyValue.length() < KEY_PREFIX_LENGTH) {
            return Optional.empty();
        }
        String keyPrefix = keyValue.substring(0, KEY_PREFIX_LENGTH);

        // Find candidates by prefix
        List<UserApiKey> candidates = userApiKeyRepository.findActiveByKeyPrefix(keyPrefix);

        // Validate hash for each candidate (BCrypt provides constant-time comparison)
        for (UserApiKey candidate : candidates) {
            if (candidate.getKeyHash() != null && passwordEncoder.matches(keyValue, candidate.getKeyHash())) {
                return Optional.of(candidate);
            }
            // Fallback for legacy keys that still have plaintext keyValue
            if (candidate.getKeyValue() != null && candidate.getKeyValue().equals(keyValue)) {
                log.warn("API key {} is using legacy plaintext storage - should be migrated", candidate.getId());
                return Optional.of(candidate);
            }
        }

        // Also check legacy plaintext keys for backwards compatibility
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
     * @throws IllegalArgumentException if key not found or access denied
     * @throws IllegalStateException    if key is already revoked
     */
    @Transactional
    public UserApiKey rotateApiKey(Long keyId) {
        UserApiKey oldKey = userApiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found or access denied"));

        if (!oldKey.isActive()) {
            throw new IllegalStateException("Cannot rotate a revoked API key");
        }

        // Revoke the old key
        oldKey.setRevokedAt(LocalDateTime.now());
        userApiKeyRepository.save(oldKey);

        // Generate a new key with the same name (doesn't count against limit since we just revoked one)
        UserApiKey newKey = generateApiKey(oldKey.getUser().getId(), oldKey.getName(), oldKey.getDescription());

        log.info("Rotated API key '{}' for user {}",
                oldKey.getName(), oldKey.getUser().getUsername());

        return newKey;
    }

    /**
     * Revoke an API key.
     *
     * @param keyId The ID of the key to revoke
     * @throws IllegalArgumentException if key not found or access denied
     * @throws IllegalStateException    if key is already revoked
     */
    @Transactional
    public void revokeApiKey(Long keyId) {
        UserApiKey apiKey = userApiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found or access denied"));

        if (!apiKey.isActive()) {
            throw new IllegalStateException("API key is already revoked");
        }

        apiKey.setRevokedAt(LocalDateTime.now());
        userApiKeyRepository.save(apiKey);

        log.info("Revoked API key '{}' for user {}",
                apiKey.getName(), apiKey.getUser().getUsername());
    }

    /**
     * Mark an API key as used. Updates are batched to reduce database load.
     * The actual update happens asynchronously.
     *
     * @param keyId The API key ID
     */
    public void markKeyUsed(Long keyId) {
        pendingLastUsedUpdates.put(keyId, LocalDateTime.now());
    }

    /**
     * Update the last used timestamp for an API key.
     * Called asynchronously to avoid blocking the request.
     *
     * @param keyId The API key ID
     */
    @Async
    @Transactional
    public void updateLastUsedAtAsync(Long keyId) {
        userApiKeyRepository.updateLastUsedAt(keyId, LocalDateTime.now());
    }

    /**
     * Legacy synchronous update method.
     * @deprecated Use markKeyUsed() or updateLastUsedAtAsync() instead
     */
    @Deprecated
    @Transactional
    public void updateLastUsedAt(Long keyId) {
        userApiKeyRepository.updateLastUsedAt(keyId, LocalDateTime.now());
    }

    /**
     * Flush pending last-used updates to the database.
     * Can be called periodically by a scheduled task.
     */
    @Transactional
    public void flushLastUsedUpdates() {
        if (pendingLastUsedUpdates.isEmpty()) {
            return;
        }

        ConcurrentMap<Long, LocalDateTime> updates = new ConcurrentHashMap<>(pendingLastUsedUpdates);
        pendingLastUsedUpdates.clear();

        updates.forEach((keyId, timestamp) -> {
            try {
                userApiKeyRepository.updateLastUsedAt(keyId, timestamp);
            } catch (Exception e) {
                log.warn("Failed to update lastUsedAt for API key {}: {}", keyId, e.getMessage());
            }
        });

        if (!updates.isEmpty()) {
            log.debug("Flushed {} last-used updates", updates.size());
        }
    }

    /**
     * Generate a unique key value with a unique prefix.
     */
    private String generateUniqueKeyValue() {
        String keyValue;
        String keyPrefix;
        do {
            keyValue = UUID.randomUUID().toString();
            keyPrefix = keyValue.substring(0, KEY_PREFIX_LENGTH);
            // Check both prefix and full value for uniqueness
        } while (userApiKeyRepository.existsByKeyPrefix(keyPrefix) ||
                 userApiKeyRepository.existsByKeyValue(keyValue));
        return keyValue;
    }
}
