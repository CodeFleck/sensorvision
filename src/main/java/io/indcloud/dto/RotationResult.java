package io.indcloud.dto;

import io.indcloud.model.UserApiKey;

import java.time.LocalDateTime;

/**
 * Result of an API key rotation operation.
 * Contains the new key and optionally the time when the old key will be revoked.
 */
public record RotationResult(
        UserApiKey newKey,
        LocalDateTime oldKeyValidUntil
) {
    /**
     * Create a result for immediate revocation (no grace period).
     */
    public static RotationResult immediate(UserApiKey newKey) {
        return new RotationResult(newKey, null);
    }

    /**
     * Create a result with a scheduled revocation.
     */
    public static RotationResult withGracePeriod(UserApiKey newKey, LocalDateTime oldKeyValidUntil) {
        return new RotationResult(newKey, oldKeyValidUntil);
    }
}
