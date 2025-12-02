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
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user"})
public class UserApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "key_value", nullable = false, unique = true, length = 64)
    private String keyValue;

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

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this API key is active (not revoked).
     */
    public boolean isActive() {
        return revokedAt == null;
    }

    /**
     * Get a masked version of the key for display purposes.
     * Shows first 8 and last 4 characters.
     */
    public String getMaskedKeyValue() {
        if (keyValue == null || keyValue.length() < 12) {
            return "****";
        }
        return keyValue.substring(0, 8) + "..." + keyValue.substring(keyValue.length() - 4);
    }
}
