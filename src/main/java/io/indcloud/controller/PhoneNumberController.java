package io.indcloud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.ApiResponse;
import io.indcloud.dto.PhoneNumberRequest;
import io.indcloud.dto.PhoneNumberResponse;
import io.indcloud.dto.VerifyPhoneRequest;
import io.indcloud.model.User;
import io.indcloud.model.UserPhoneNumber;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.PhoneNumberVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing user phone numbers
 */
@RestController
@RequestMapping("/api/v1/phone-numbers")
@RequiredArgsConstructor
@Slf4j
public class PhoneNumberController {

    private final PhoneNumberVerificationService phoneNumberService;
    private final SecurityUtils securityUtils;

    /**
     * Get all phone numbers for current user
     */
    @GetMapping
    public ResponseEntity<List<PhoneNumberResponse>> getUserPhoneNumbers() {
        User currentUser = securityUtils.getCurrentUser();
        List<UserPhoneNumber> phoneNumbers = phoneNumberService.getUserPhoneNumbers(currentUser);

        List<PhoneNumberResponse> response = phoneNumbers.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Add a new phone number
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PhoneNumberResponse>> addPhoneNumber(
            @Valid @RequestBody PhoneNumberRequest request) {
        try {
            User currentUser = securityUtils.getCurrentUser();

            UserPhoneNumber phoneNumber = phoneNumberService.addPhoneNumber(
                currentUser,
                request.phoneNumber(),
                request.countryCode()
            );

            log.info("Phone number added for user {}: {}", currentUser.getUsername(),
                phoneNumber.getMaskedPhoneNumber());

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                    toResponse(phoneNumber),
                    "Phone number added. Verification code sent via SMS."
                ));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to add phone number: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Verify a phone number with OTP code
     */
    @PostMapping("/{phoneId}/verify")
    public ResponseEntity<ApiResponse<String>> verifyPhoneNumber(
            @PathVariable UUID phoneId,
            @Valid @RequestBody VerifyPhoneRequest request) {
        try {
            User currentUser = securityUtils.getCurrentUser();

            // Find the phone number
            List<UserPhoneNumber> userPhones = phoneNumberService.getUserPhoneNumbers(currentUser);
            UserPhoneNumber phone = userPhones.stream()
                .filter(p -> p.getId().equals(phoneId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Phone number not found"));

            boolean verified = phoneNumberService.verifyPhoneNumber(
                currentUser,
                phone.getPhoneNumber(),
                request.code()
            );

            if (verified) {
                log.info("Phone number verified for user {}: {}",
                    currentUser.getUsername(), phone.getMaskedPhoneNumber());
                return ResponseEntity.ok(ApiResponse.success(
                    "verified",
                    "Phone number verified successfully"
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired verification code"));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Resend verification code
     */
    @PostMapping("/{phoneId}/resend-code")
    public ResponseEntity<ApiResponse<String>> resendVerificationCode(@PathVariable UUID phoneId) {
        try {
            User currentUser = securityUtils.getCurrentUser();

            // Find the phone number
            List<UserPhoneNumber> userPhones = phoneNumberService.getUserPhoneNumbers(currentUser);
            UserPhoneNumber phone = userPhones.stream()
                .filter(p -> p.getId().equals(phoneId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Phone number not found"));

            phoneNumberService.resendVerificationCode(currentUser, phone.getPhoneNumber());

            log.info("Verification code resent for user {}: {}",
                currentUser.getUsername(), phone.getMaskedPhoneNumber());

            return ResponseEntity.ok(ApiResponse.success(
                "sent",
                "Verification code sent via SMS"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Failed to resend code: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Set a phone number as primary
     */
    @PutMapping("/{phoneId}/set-primary")
    public ResponseEntity<ApiResponse<String>> setPrimaryPhoneNumber(@PathVariable UUID phoneId) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            phoneNumberService.setPrimaryPhoneNumber(currentUser, phoneId);

            log.info("Primary phone updated for user {}", currentUser.getUsername());

            return ResponseEntity.ok(ApiResponse.success(
                "updated",
                "Primary phone number updated"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Failed to set primary phone: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Toggle phone number enabled status
     */
    @PutMapping("/{phoneId}/toggle")
    public ResponseEntity<ApiResponse<PhoneNumberResponse>> togglePhoneNumber(@PathVariable UUID phoneId) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            UserPhoneNumber phone = phoneNumberService.togglePhoneNumberEnabled(currentUser, phoneId);

            return ResponseEntity.ok(ApiResponse.success(
                toResponse(phone),
                "Phone number " + (phone.getEnabled() ? "enabled" : "disabled")
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to toggle phone: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete a phone number
     */
    @DeleteMapping("/{phoneId}")
    public ResponseEntity<ApiResponse<String>> deletePhoneNumber(@PathVariable UUID phoneId) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            phoneNumberService.removePhoneNumber(currentUser, phoneId);

            log.info("Phone number removed for user {}", currentUser.getUsername());

            return ResponseEntity.ok(ApiResponse.success(
                "deleted",
                "Phone number removed"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Failed to delete phone: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Convert entity to response DTO
     */
    private PhoneNumberResponse toResponse(UserPhoneNumber phone) {
        return new PhoneNumberResponse(
            phone.getId(),
            phone.getMaskedPhoneNumber(),
            phone.getCountryCode(),
            phone.getVerified(),
            phone.getIsPrimary(),
            phone.getEnabled(),
            phone.getCreatedAt()
        );
    }
}
