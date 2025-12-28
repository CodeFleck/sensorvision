package io.indcloud.task;

import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.UserApiKey;
import io.indcloud.repository.UserApiKeyRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * Each key is migrated in its own transaction for partial progress support.
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
     * Each key is processed in its own transaction to allow partial progress.
     * If the application crashes mid-migration, already-migrated keys are preserved.
     */
    public void migrateLegacyApiKeys() {
        long count = userApiKeyRepository.countLegacyPlaintextKeys();

        if (count == 0) {
            log.debug("No legacy plaintext API keys found - migration not needed");
            return;
        }

        log.info("Starting migration of {} legacy plaintext API keys to hashed storage", count);

        List<UserApiKey> legacyKeys = userApiKeyRepository.findLegacyPlaintextKeys();
        int migrated = 0;
        int skipped = 0;
        int failed = 0;

        // Track prefixes used in this batch to detect collisions within the migration
        Set<String> prefixesInBatch = new HashSet<>();

        for (UserApiKey key : legacyKeys) {
            try {
                String plaintextValue = key.getKeyValue();

                // Skip keys with null/blank values
                if (plaintextValue == null || plaintextValue.isBlank()) {
                    log.warn("API key {} has null/blank keyValue but no hash - skipping", key.getId());
                    skipped++;
                    continue;
                }

                // Generate prefix
                String keyPrefix = plaintextValue.length() >= KEY_PREFIX_LENGTH
                        ? plaintextValue.substring(0, KEY_PREFIX_LENGTH)
                        : plaintextValue;

                // Check for collision within current migration batch
                if (prefixesInBatch.contains(keyPrefix)) {
                    log.warn("API key {} has prefix collision within migration batch - skipping", key.getId());
                    skipped++;
                    continue;
                }

                // Check for collision with already-migrated keys in database
                if (userApiKeyRepository.existsByKeyPrefix(keyPrefix)) {
                    log.warn("API key {} has prefix collision with existing key - skipping", key.getId());
                    skipped++;
                    continue;
                }

                // Track this prefix to prevent collisions within the batch
                prefixesInBatch.add(keyPrefix);

                // Migrate the key in its own transaction
                migrateKeyInTransaction(key, keyPrefix);
                migrated++;

            } catch (Exception e) {
                log.error("Failed to migrate API key {}: {}", key.getId(), e.getMessage());
                failed++;
            }
        }

        if (failed == 0 && skipped == 0) {
            log.info("Successfully migrated {} API keys to hashed storage", migrated);
        } else {
            log.warn("API key migration completed: {} migrated, {} skipped, {} failed", migrated, skipped, failed);
        }
    }

    /**
     * Migrate a single API key in its own transaction.
     * Using REQUIRES_NEW ensures each key migration is committed independently.
     *
     * @param key       The API key to migrate
     * @param keyPrefix The pre-computed prefix (collision-checked)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void migrateKeyInTransaction(UserApiKey key, String keyPrefix) {
        // Re-fetch the key to ensure we have the latest state within this transaction
        UserApiKey freshKey = userApiKeyRepository.findById(key.getId())
                .orElseThrow(() -> new IllegalStateException("API key " + key.getId() + " not found"));

        // Double-check it still needs migration (could have been migrated by another instance)
        if (freshKey.getKeyHash() != null) {
            log.debug("API key {} already migrated by another process - skipping", key.getId());
            return;
        }

        String plaintextValue = freshKey.getKeyValue();
        if (plaintextValue == null || plaintextValue.isBlank()) {
            log.warn("API key {} has null/blank keyValue - skipping", key.getId());
            return;
        }

        // Generate hash
        String keyHash = passwordEncoder.encode(plaintextValue);

        // Update the key
        freshKey.setKeyPrefix(keyPrefix);
        freshKey.setKeyHash(keyHash);
        freshKey.setKeyValue(null); // Clear the plaintext value for security

        userApiKeyRepository.save(freshKey);

        log.debug("Migrated API key {} (user: {}) to hashed storage",
                freshKey.getId(),
                freshKey.getUser() != null ? freshKey.getUser().getUsername() : "unknown");
    }
}
