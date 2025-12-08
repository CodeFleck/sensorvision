package org.sensorvision.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User-level API key for programmatic access.
 * One key works for all devices in the user's organization (like Ubidots Default Token).
 */
@Entity
@Table(name = "user_api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id", "keyPrefix"})
@ToString(exclude = {"user", "keyHash"})
public class UserApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * DEPRECATED: Will be NULL for new keys after hashing migration.
     * Only used temporarily to return the key value on creation.
     */
    @Column(name = "key_value", unique = true, length = 64)
    private String keyValue;

    /**
     * First 8 characters of the API key (for fast prefix-based lookups).
     */
    @Column(name = "key_prefix", length = 8)
    private String keyPrefix;

    /**
     * BCrypt hash of the full API key (for secure validation).
     */
    @Column(name = "key_hash", length = 255)
    private String keyHash;

    /**
     * Transient field to hold the plaintext key value after generation.
     * This is only available immediately after key creation and is never persisted.
     */
    @Transient
    private String plaintextKeyValue;

    @Column(nullable = false, length = 255)
    @Builder.Default
    private String name = "Default Token";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * Future timestamp when the key will be automatically revoked.
     * Used for grace period during key rotation to enable zero-downtime updates.
     */
    @Column(name = "scheduled_revocation_at")
    private LocalDateTime scheduledRevocationAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this API key is currently active.
     * A key is active if:
     * - It has not been revoked (revokedAt is null)
     * - AND either has no scheduled revocation OR the scheduled time has not passed yet
     */
    public boolean isActive() {
        if (revokedAt != null) {
            return false;
        }
        // If scheduled for revocation, check if the time has passed
        if (scheduledRevocationAt != null && LocalDateTime.now().isAfter(scheduledRevocationAt)) {
            return false;
        }
        return true;
    }

    /**
     * Check if this key has a pending scheduled revocation.
     */
    public boolean hasPendingRevocation() {
        return scheduledRevocationAt != null && revokedAt == null;
    }

    /**
     * Get a masked version of the key for display purposes.
     * Uses the key prefix if available, otherwise falls back to keyValue.
     */
    public String getMaskedKeyValue() {
        // Prefer using the key prefix for masking
        if (keyPrefix != null && !keyPrefix.isBlank()) {
            return keyPrefix + "...****";
        }
        // Fallback to keyValue for backwards compatibility with existing keys
        if (keyValue != null && keyValue.length() >= 12) {
            return keyValue.substring(0, 8) + "..." + keyValue.substring(keyValue.length() - 4);
        }
        return "****";
    }

    /**
     * Get the key value to return to the user.
     * Returns the transient plaintextKeyValue if available (for new keys),
     * otherwise falls back to keyValue for backwards compatibility.
     */
    public String getDisplayKeyValue() {
        if (plaintextKeyValue != null) {
            return plaintextKeyValue;
        }
        return keyValue;
    }
}
