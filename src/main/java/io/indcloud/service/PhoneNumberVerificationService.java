package io.indcloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.User;
import io.indcloud.model.UserPhoneNumber;
import io.indcloud.repository.UserPhoneNumberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user phone numbers and verification.
 * Handles OTP generation, verification, and phone number management.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PhoneNumberVerificationService {

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_EXPIRY_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserPhoneNumberRepository phoneNumberRepository;
    private final SmsNotificationService smsNotificationService;

    /**
     * Add a new phone number for a user and send verification code
     */
    @Transactional
    public UserPhoneNumber addPhoneNumber(User user, String phoneNumber, String countryCode) {
        // Check if phone number already exists for this user
        Optional<UserPhoneNumber> existing = phoneNumberRepository
            .findByUserIdAndPhoneNumber(user.getId(), phoneNumber);

        if (existing.isPresent()) {
            throw new IllegalArgumentException("Phone number already exists for this user");
        }

        // Generate verification code
        String verificationCode = generateVerificationCode();
        Instant expiresAt = Instant.now().plus(VERIFICATION_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        // Determine if this should be primary (first phone number)
        List<UserPhoneNumber> existingPhones = phoneNumberRepository.findByUserId(user.getId());
        boolean isPrimary = existingPhones.isEmpty();

        // Create phone number record
        UserPhoneNumber userPhoneNumber = UserPhoneNumber.builder()
            .user(user)
            .phoneNumber(phoneNumber)
            .countryCode(countryCode)
            .verified(false)
            .verificationCode(verificationCode)
            .verificationExpiresAt(expiresAt)
            .isPrimary(isPrimary)
            .enabled(true)
            .build();

        userPhoneNumber = phoneNumberRepository.save(userPhoneNumber);

        // Send verification SMS
        sendVerificationSms(phoneNumber, verificationCode);

        log.info("Phone number {} added for user {} (verified: false, primary: {})",
            phoneNumber, user.getId(), isPrimary);

        return userPhoneNumber;
    }

    /**
     * Verify a phone number with OTP code
     */
    @Transactional
    public boolean verifyPhoneNumber(User user, String phoneNumber, String code) {
        Optional<UserPhoneNumber> phoneOpt = phoneNumberRepository
            .findByUserIdAndPhoneNumber(user.getId(), phoneNumber);

        if (phoneOpt.isEmpty()) {
            log.warn("Phone number {} not found for user {}", phoneNumber, user.getId());
            return false;
        }

        UserPhoneNumber phone = phoneOpt.get();

        if (!phone.isVerificationCodeValid(code)) {
            log.warn("Invalid or expired verification code for phone {} (user {})",
                phoneNumber, user.getId());
            return false;
        }

        // Mark as verified
        phone.setVerified(true);
        phone.setVerificationCode(null);
        phone.setVerificationExpiresAt(null);
        phoneNumberRepository.save(phone);

        log.info("Phone number {} verified for user {}", phoneNumber, user.getId());
        return true;
    }

    /**
     * Resend verification code
     */
    @Transactional
    public void resendVerificationCode(User user, String phoneNumber) {
        Optional<UserPhoneNumber> phoneOpt = phoneNumberRepository
            .findByUserIdAndPhoneNumber(user.getId(), phoneNumber);

        if (phoneOpt.isEmpty()) {
            throw new IllegalArgumentException("Phone number not found");
        }

        UserPhoneNumber phone = phoneOpt.get();

        if (phone.getVerified()) {
            throw new IllegalStateException("Phone number already verified");
        }

        // Generate new code
        String verificationCode = generateVerificationCode();
        Instant expiresAt = Instant.now().plus(VERIFICATION_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        phone.setVerificationCode(verificationCode);
        phone.setVerificationExpiresAt(expiresAt);
        phoneNumberRepository.save(phone);

        // Send SMS
        sendVerificationSms(phoneNumber, verificationCode);

        log.info("Verification code resent to {} for user {}", phoneNumber, user.getId());
    }

    /**
     * Set a phone number as primary
     */
    @Transactional
    public void setPrimaryPhoneNumber(User user, UUID phoneId) {
        // Find the phone number
        Optional<UserPhoneNumber> phoneOpt = phoneNumberRepository.findById(phoneId);
        if (phoneOpt.isEmpty() || !phoneOpt.get().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Phone number not found");
        }

        UserPhoneNumber phone = phoneOpt.get();

        if (!phone.getVerified()) {
            throw new IllegalStateException("Cannot set unverified phone as primary");
        }

        // Unset current primary
        List<UserPhoneNumber> allPhones = phoneNumberRepository.findByUserId(user.getId());
        for (UserPhoneNumber p : allPhones) {
            if (p.getIsPrimary()) {
                p.setIsPrimary(false);
                phoneNumberRepository.save(p);
            }
        }

        // Set new primary
        phone.setIsPrimary(true);
        phoneNumberRepository.save(phone);

        log.info("Phone number {} set as primary for user {}", phone.getPhoneNumber(), user.getId());
    }

    /**
     * Remove a phone number
     */
    @Transactional
    public void removePhoneNumber(User user, UUID phoneId) {
        Optional<UserPhoneNumber> phoneOpt = phoneNumberRepository.findById(phoneId);
        if (phoneOpt.isEmpty() || !phoneOpt.get().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Phone number not found");
        }

        UserPhoneNumber phone = phoneOpt.get();

        // Don't allow removing primary if other phones exist
        if (phone.getIsPrimary()) {
            List<UserPhoneNumber> otherPhones = phoneNumberRepository.findByUserId(user.getId());
            if (otherPhones.size() > 1) {
                throw new IllegalStateException("Cannot remove primary phone number. Set another phone as primary first.");
            }
        }

        phoneNumberRepository.delete(phone);
        log.info("Phone number {} removed for user {}", phone.getPhoneNumber(), user.getId());
    }

    /**
     * Get all phone numbers for a user
     */
    public List<UserPhoneNumber> getUserPhoneNumbers(User user) {
        return phoneNumberRepository.findByUserId(user.getId());
    }

    /**
     * Get verified phone numbers for a user
     */
    public List<UserPhoneNumber> getVerifiedPhoneNumbers(User user) {
        return phoneNumberRepository.findByUserIdAndVerifiedTrue(user.getId());
    }

    /**
     * Get primary phone number for a user
     */
    public Optional<UserPhoneNumber> getPrimaryPhoneNumber(User user) {
        return phoneNumberRepository.findByUserIdAndIsPrimaryTrue(user.getId());
    }

    /**
     * Toggle phone number enabled status
     */
    @Transactional
    public UserPhoneNumber togglePhoneNumberEnabled(User user, UUID phoneId) {
        Optional<UserPhoneNumber> phoneOpt = phoneNumberRepository.findById(phoneId);
        if (phoneOpt.isEmpty() || !phoneOpt.get().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Phone number not found");
        }

        UserPhoneNumber phone = phoneOpt.get();
        phone.setEnabled(!phone.getEnabled());
        UserPhoneNumber saved = phoneNumberRepository.save(phone);

        log.info("Phone number {} toggled for user {}: enabled={}",
            phone.getMaskedPhoneNumber(), user.getUsername(), phone.getEnabled());

        return saved;
    }

    /**
     * Generate 6-digit verification code
     */
    private String generateVerificationCode() {
        int code = RANDOM.nextInt(900000) + 100000; // 100000 to 999999
        return String.valueOf(code);
    }

    /**
     * Send verification SMS
     * @return true if SMS was sent successfully, false otherwise
     */
    private boolean sendVerificationSms(String phoneNumber, String code) {
        String message = String.format(
            "Your SensorVision verification code is: %s. Valid for %d minutes.",
            code,
            VERIFICATION_EXPIRY_MINUTES
        );

        boolean sent = smsNotificationService.sendVerificationSms(phoneNumber, message);

        if (!sent) {
            log.warn("Failed to send verification SMS to {} - code: {}", phoneNumber, code);
        }
        return sent;
    }

    /**
     * Check if SMS verification is available (Twilio configured)
     */
    public boolean isSmsVerificationAvailable() {
        return smsNotificationService.isSmsEnabled();
    }
}
