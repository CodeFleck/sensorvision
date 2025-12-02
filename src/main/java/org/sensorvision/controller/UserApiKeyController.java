package org.sensorvision.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.CreateUserApiKeyRequest;
import org.sensorvision.dto.UserApiKeyDto;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.UserApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

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

        UserApiKey apiKey = userApiKeyService.generateApiKey(currentUser.getId(), name);

        // If description was provided, we'd need to add it after creation
        // For now, the description can be added in a future update

        log.info("User {} generated new API key '{}'", currentUser.getUsername(), name);

        return ResponseEntity.ok(UserApiKeyDto.newKey(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyValue(),  // Full key value - only shown on creation
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
                        apiKey.getKeyValue(),
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
     *
     * @param keyId The API key ID to rotate
     * @return The new API key (full key value shown only once)
     */
    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<UserApiKeyDto> rotateApiKey(@PathVariable Long keyId) {
        User currentUser = securityUtils.getCurrentUser();

        // Verify the key belongs to the current user
        verifyKeyOwnership(keyId, currentUser.getId());

        UserApiKey newKey = userApiKeyService.rotateApiKey(keyId);

        log.info("User {} rotated API key '{}'", currentUser.getUsername(), newKey.getName());

        return ResponseEntity.ok(UserApiKeyDto.newKey(
                newKey.getId(),
                newKey.getName(),
                newKey.getKeyValue(),
                newKey.getCreatedAt()
        ));
    }

    /**
     * Revoke an API key.
     * DELETE /api/v1/api-keys/{keyId}
     *
     * @param keyId The API key ID to revoke
     * @return Success message
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<UserApiKeyDto> revokeApiKey(@PathVariable Long keyId) {
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
            throw new AccessDeniedException("Access denied to API key: " + keyId);
        }
    }

    /**
     * Convert UserApiKey entity to DTO (masked, for listing).
     */
    private UserApiKeyDto toDto(UserApiKey apiKey) {
        return UserApiKeyDto.existing(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getDescription(),
                apiKey.getMaskedKeyValue(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.isActive()
        );
    }
}
