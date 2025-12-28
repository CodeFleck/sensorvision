package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
@ToString(exclude = {"organization", "roles"})
public class User extends AuditableEntity implements SoftDeletable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "email_verification_token_expiry")
    private LocalDateTime emailVerificationTokenExpiry;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "avatar_version")
    @Builder.Default
    private Long avatarVersion = 0L;

    @Column(name = "email_notifications_enabled")
    @Builder.Default
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "theme_preference", length = 20)
    @Builder.Default
    private String themePreference = "light-luxury";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // Soft delete fields
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;

    // SoftDeletable implementation
    @Override
    public String getEntityId() {
        return id != null ? id.toString() : null;
    }

    @Override
    public String getEntityName() {
        String name = "";
        if (firstName != null) name += firstName;
        if (lastName != null) name += (name.isEmpty() ? "" : " ") + lastName;
        if (name.isEmpty()) name = username;
        return name;
    }

    @Override
    public String getEntityType() {
        return "USER";
    }
}
