package io.indcloud.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * User phone number for SMS notifications.
 * Stores phone numbers in E.164 format with verification status.
 */
@Entity
@Table(name = "user_phone_numbers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "phone_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPhoneNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Phone number in E.164 format (e.g., +15551234567)
     */
    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format (e.g., +15551234567)")
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /**
     * Country code (e.g., "US", "BR", "IN")
     */
    @NotBlank
    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode;

    /**
     * Phone number verified via OTP
     */
    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    /**
     * OTP code for verification (6 digits)
     */
    @Column(name = "verification_code", length = 6)
    private String verificationCode;

    /**
     * Verification code expiration timestamp
     */
    @Column(name = "verification_expires_at")
    private Instant verificationExpiresAt;

    /**
     * Primary phone number for user (only one per user)
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Phone number enabled for notifications
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Check if verification code is valid and not expired
     */
    public boolean isVerificationCodeValid(String code) {
        if (verificationCode == null || verificationExpiresAt == null) {
            return false;
        }
        return verificationCode.equals(code) && Instant.now().isBefore(verificationExpiresAt);
    }

    /**
     * Get masked phone number for display (e.g., +1555***4567)
     */
    public String getMaskedPhoneNumber() {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }
        int visibleDigits = 4;
        String prefix = phoneNumber.substring(0, phoneNumber.length() - visibleDigits - 3);
        String suffix = phoneNumber.substring(phoneNumber.length() - visibleDigits);
        return prefix + "***" + suffix;
    }
}
