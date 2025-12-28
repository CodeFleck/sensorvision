package io.indcloud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for device token API responses.
 * Provides different response formats for various token operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceTokenResponse {

    private String deviceId;
    private String token;          // Full token (only shown once on generation/rotation)
    private String maskedToken;    // Masked token (e.g., "550e8400...0000")
    private String message;
    private boolean success;
    private LocalDateTime tokenCreatedAt;
    private LocalDateTime tokenLastUsedAt;

    /**
     * Success response with full token (for generate/rotate operations)
     */
    public static DeviceTokenResponse success(String deviceId, String token, String message, LocalDateTime createdAt) {
        return DeviceTokenResponse.builder()
                .success(true)
                .deviceId(deviceId)
                .token(token)
                .message(message)
                .tokenCreatedAt(createdAt)
                .build();
    }

    /**
     * Response with masked token (for get token info)
     */
    public static DeviceTokenResponse masked(String deviceId, String maskedToken,
                                              LocalDateTime createdAt, LocalDateTime lastUsedAt) {
        return DeviceTokenResponse.builder()
                .success(true)
                .deviceId(deviceId)
                .maskedToken(maskedToken)
                .message("Token exists (showing masked version)")
                .tokenCreatedAt(createdAt)
                .tokenLastUsedAt(lastUsedAt)
                .build();
    }

    /**
     * Response when device has no token
     */
    public static DeviceTokenResponse noToken(String deviceId) {
        return DeviceTokenResponse.builder()
                .success(true)
                .deviceId(deviceId)
                .message("Device has no API token. Use /generate to create one.")
                .build();
    }

    /**
     * Response when token is revoked
     */
    public static DeviceTokenResponse revoked(String deviceId) {
        return DeviceTokenResponse.builder()
                .success(true)
                .deviceId(deviceId)
                .message("API token revoked successfully")
                .build();
    }

    /**
     * Error response
     */
    public static DeviceTokenResponse error(String message) {
        return DeviceTokenResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
