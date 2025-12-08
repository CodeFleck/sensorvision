package org.sensorvision.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.repository.UserApiKeyRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to process API keys with expired grace periods.
 * <p>
 * When an API key is rotated with a grace period, the old key is scheduled for
 * future revocation rather than being immediately revoked. This task runs
 * periodically to revoke keys whose grace period has expired.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiKeyRevocationTask {

    private final UserApiKeyRepository userApiKeyRepository;

    /**
     * Process expired grace periods every minute.
     * Revokes API keys whose scheduled revocation time has passed.
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void processExpiredGracePeriods() {
        LocalDateTime now = LocalDateTime.now();
        long count = userApiKeyRepository.countKeysWithExpiredGracePeriod(now);

        if (count == 0) {
            return; // No keys to process
        }

        log.info("Processing {} API keys with expired grace periods", count);

        List<UserApiKey> expiredKeys = userApiKeyRepository.findKeysWithExpiredGracePeriod(now);
        int revoked = 0;
        int failed = 0;

        for (UserApiKey key : expiredKeys) {
            try {
                revokeKey(key);
                revoked++;
            } catch (Exception e) {
                log.error("Failed to revoke API key {} (user: {}): {}",
                        key.getId(),
                        key.getUser() != null ? key.getUser().getUsername() : "unknown",
                        e.getMessage());
                failed++;
            }
        }

        if (failed == 0) {
            log.info("Successfully revoked {} API keys after grace period expiration", revoked);
        } else {
            log.warn("API key revocation completed: {} revoked, {} failed", revoked, failed);
        }
    }

    /**
     * Revoke a single API key.
     *
     * @param key The API key to revoke
     */
    private void revokeKey(UserApiKey key) {
        key.setRevokedAt(LocalDateTime.now());
        // Clear the scheduled revocation since it's now been processed
        key.setScheduledRevocationAt(null);
        userApiKeyRepository.save(key);

        log.debug("Revoked API key '{}' (id: {}) for user {} after grace period expiration",
                key.getName(),
                key.getId(),
                key.getUser() != null ? key.getUser().getUsername() : "unknown");
    }
}
