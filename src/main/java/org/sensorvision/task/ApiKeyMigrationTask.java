package org.sensorvision.task;

import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.repository.UserApiKeyRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time migration task to hash existing plaintext API keys.
 * <p>
 * This task runs on application startup and migrates any legacy API keys
 * that still have plaintext values stored in the database.
 * <p>
 * After migration:
 * - key_prefix: First 8 characters of the original key
 * - key_hash: BCrypt hash of the full key
 * - key_value: NULL (cleared for security)
 * <p>
 * This is idempotent - running multiple times is safe.
 */
@Component
@Slf4j
public class ApiKeyMigrationTask {

    private static final int KEY_PREFIX_LENGTH = 8;

    private final UserApiKeyRepository userApiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor with @Lazy on PasswordEncoder to avoid circular dependency.
     */
    public ApiKeyMigrationTask(
            UserApiKeyRepository userApiKeyRepository,
            @Lazy PasswordEncoder passwordEncoder) {
        this.userApiKeyRepository = userApiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Run the migration after the application has fully started.
     * Uses ApplicationReadyEvent to ensure all beans are initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        migrateLegacyApiKeys();
    }

    /**
     * Migrate all legacy plaintext API keys to hashed storage.
     * <p>
     * This method is transactional and will roll back if any error occurs.
     * Each key is processed individually to allow partial progress.
     */
    @Transactional
    public void migrateLegacyApiKeys() {
        long count = userApiKeyRepository.countLegacyPlaintextKeys();

        if (count == 0) {
            log.debug("No legacy plaintext API keys found - migration not needed");
            return;
        }

        log.info("Starting migration of {} legacy plaintext API keys to hashed storage", count);

        List<UserApiKey> legacyKeys = userApiKeyRepository.findLegacyPlaintextKeys();
        int migrated = 0;
        int failed = 0;

        for (UserApiKey key : legacyKeys) {
            try {
                migrateKey(key);
                migrated++;
            } catch (Exception e) {
                log.error("Failed to migrate API key {}: {}", key.getId(), e.getMessage());
                failed++;
            }
        }

        if (failed == 0) {
            log.info("Successfully migrated {} API keys to hashed storage", migrated);
        } else {
            log.warn("API key migration completed: {} migrated, {} failed", migrated, failed);
        }
    }

    /**
     * Migrate a single API key from plaintext to hashed storage.
     *
     * @param key The API key to migrate
     */
    private void migrateKey(UserApiKey key) {
        String plaintextValue = key.getKeyValue();

        if (plaintextValue == null || plaintextValue.isBlank()) {
            log.warn("API key {} has null/blank keyValue but no hash - skipping", key.getId());
            return;
        }

        // Generate prefix and hash
        String keyPrefix = plaintextValue.length() >= KEY_PREFIX_LENGTH
                ? plaintextValue.substring(0, KEY_PREFIX_LENGTH)
                : plaintextValue;

        String keyHash = passwordEncoder.encode(plaintextValue);

        // Check for prefix collision with existing migrated keys
        if (key.getKeyPrefix() == null && userApiKeyRepository.existsByKeyPrefix(keyPrefix)) {
            // This shouldn't happen with UUIDs, but handle it gracefully
            log.warn("API key {} has prefix collision with existing key - using full prefix anyway", key.getId());
        }

        // Update the key
        key.setKeyPrefix(keyPrefix);
        key.setKeyHash(keyHash);
        key.setKeyValue(null); // Clear the plaintext value for security

        userApiKeyRepository.save(key);

        log.debug("Migrated API key {} (user: {}) to hashed storage",
                key.getId(),
                key.getUser() != null ? key.getUser().getUsername() : "unknown");
    }
}
