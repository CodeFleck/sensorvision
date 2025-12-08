package org.sensorvision.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.CreateUserApiKeyRequest;
import org.sensorvision.dto.RotateApiKeyResponse;
import org.sensorvision.dto.UserApiKeyDto;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.UserApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for managing user API keys.
 * <p>
 * User API keys allow programmatic access to all devices in the user's organization
 * with a single token (similar to Ubidots Default Token).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
@Validated
public class UserApiKeyController {

    private final UserApiKeyService userApiKeyService;
    private final SecurityUtils securityUtils;

    /**
     * Get all API keys for the current user.
     * GET /api/v1/api-keys
     *
     * @return List of API keys (masked)
     */
    @GetMapping
    public ResponseEntity<List<UserApiKeyDto>> getMyApiKeys() {
        User currentUser = securityUtils.getCurrentUser();

        List<UserApiKeyDto> keys = userApiKeyService.getApiKeysForUser(currentUser.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(keys);
    }

    /**
     * Generate a new API key for the current user.
     * POST /api/v1/api-keys
     *
     * @param request Optional name and description for the key
     * @return The newly generated API key (full key value shown only once)
     */
    @PostMapping
    public ResponseEntity<UserApiKeyDto> generateApiKey(@Valid @RequestBody(required = false) CreateUserApiKeyRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        String name = (request != null && request.getName() != null) ? request.getName() : "Default Token";
        String description = (request != null) ? request.getDescription() : null;

        UserApiKey apiKey = userApiKeyService.generateApiKey(currentUser.getId(), name, description);

        log.info("User {} generated new API key '{}'", currentUser.getUsername(), name);

        return ResponseEntity.ok(UserApiKeyDto.newKey(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getDisplayKeyValue(),  // Full key value - only shown on creation
                apiKey.getCreatedAt()
        ));
    }

    /**
     * Generate a default token if the user doesn't have any keys.
     * POST /api/v1/api-keys/default
     *
     * @return The default token or message if one already exists
     */
    @PostMapping("/default")
    public ResponseEntity<UserApiKeyDto> generateDefaultToken() {
        User currentUser = securityUtils.getCurrentUser();

        return userApiKeyService.generateDefaultTokenIfNeeded(currentUser.getId())
                .map(apiKey -> ResponseEntity.ok(UserApiKeyDto.newKey(
                        apiKey.getId(),
                        apiKey.getName(),
                        apiKey.getDisplayKeyValue(),
                        apiKey.getCreatedAt()
                )))
                .orElseGet(() -> ResponseEntity.ok(UserApiKeyDto.builder()
                        .success(true)
                        .message("You already have an API key. Use the existing key or generate a new one.")
                        .build()));
    }

    /**
     * Rotate an existing API key (revoke old, create new with same name).
     * POST /api/v1/api-keys/{keyId}/rotate
     * <p>
     * Supports optional grace period for zero-downtime rotation in distributed systems.
     * When gracePeriodMinutes is provided, the old key remains valid until the grace period expires.
     *
     * @param keyId              The API key ID to rotate
     * @param gracePeriodMinutes Optional grace period in minutes (0-10080, i.e., max 7 days).
     *                           If not provided or 0, the old key is revoked immediately.
     * @return The new API key with optional old key expiration time
     */
    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<RotateApiKeyResponse> rotateApiKey(
            @PathVariable @Positive Long keyId,
            @RequestParam(required = false, defaultValue = "0")
            @Min(0) @Max(10080) Integer gracePeriodMinutes) {

        User currentUser = securityUtils.getCurrentUser();

        // Verify the key belongs to the current user
        verifyKeyOwnership(keyId, currentUser.getId());

        // Get the old key's scheduled revocation time for the response
        LocalDateTime oldKeyValidUntil = null;
        Duration gracePeriod = null;

        if (gracePeriodMinutes != null && gracePeriodMinutes > 0) {
            gracePeriod = Duration.ofMinutes(gracePeriodMinutes);
            oldKeyValidUntil = LocalDateTime.now().plus(gracePeriod);
        }

        UserApiKey newKey = userApiKeyService.rotateApiKey(keyId, gracePeriod);

        log.info("User {} rotated API key '{}' {}",
                currentUser.getUsername(),
                newKey.getName(),
                gracePeriodMinutes > 0 ? "(grace period: " + gracePeriodMinutes + " min)" : "(immediate)");

        return ResponseEntity.ok(RotateApiKeyResponse.builder()
                .newKey(UserApiKeyDto.newKey(
                        newKey.getId(),
                        newKey.getName(),
                        newKey.getDisplayKeyValue(),
                        newKey.getCreatedAt()))
                .oldKeyValidUntil(oldKeyValidUntil)
                .gracePeriodMinutes(gracePeriodMinutes)
                .build());
    }

    /**
     * Cancel a scheduled revocation for an API key.
     * DELETE /api/v1/api-keys/{keyId}/scheduled-revocation
     *
     * @param keyId The API key ID
     * @return Success message
     */
    @DeleteMapping("/{keyId}/scheduled-revocation")
    public ResponseEntity<UserApiKeyDto> cancelScheduledRevocation(@PathVariable @Positive Long keyId) {
        User currentUser = securityUtils.getCurrentUser();

        // Verify the key belongs to the current user
        verifyKeyOwnership(keyId, currentUser.getId());

        userApiKeyService.cancelScheduledRevocation(keyId);

        log.info("User {} cancelled scheduled revocation for API key {}", currentUser.getUsername(), keyId);

        return ResponseEntity.ok(UserApiKeyDto.builder()
                .id(keyId)
                .success(true)
                .message("Scheduled revocation cancelled")
                .active(true)
                .build());
    }

    /**
     * Revoke an API key.
     * DELETE /api/v1/api-keys/{keyId}
     *
     * @param keyId The API key ID to revoke
     * @return Success message
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<UserApiKeyDto> revokeApiKey(@PathVariable @Positive Long keyId) {
        User currentUser = securityUtils.getCurrentUser();

        // Verify the key belongs to the current user
        verifyKeyOwnership(keyId, currentUser.getId());

        userApiKeyService.revokeApiKey(keyId);

        log.info("User {} revoked API key {}", currentUser.getUsername(), keyId);

        return ResponseEntity.ok(UserApiKeyDto.builder()
                .id(keyId)
                .success(true)
                .message("API key revoked successfully")
                .active(false)
                .build());
    }

    /**
     * Verify that an API key belongs to the specified user.
     */
    private void verifyKeyOwnership(Long keyId, Long userId) {
        List<UserApiKey> userKeys = userApiKeyService.getApiKeysForUser(userId);
        boolean ownsKey = userKeys.stream().anyMatch(k -> k.getId().equals(keyId));

        if (!ownsKey) {
            throw new AccessDeniedException("Access denied to API key");
        }
    }

    /**
     * Convert UserApiKey entity to DTO (masked, for listing).
     */
    private UserApiKeyDto toDto(UserApiKey apiKey) {
        return UserApiKeyDto.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .description(apiKey.getDescription())
                .maskedKeyValue(apiKey.getMaskedKeyValue())
                .createdAt(apiKey.getCreatedAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .scheduledRevocationAt(apiKey.getScheduledRevocationAt())
                .active(apiKey.isActive())
                .success(true)
                .build();
    }
}
