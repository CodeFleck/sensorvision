package io.indcloud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user API key responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserApiKeyDto {
    private Long id;
    private String name;
    private String description;

    /**
     * The full API key value. Only included when generating a new key.
     */
    private String keyValue;

    /**
     * Masked version of the key for display (e.g., "abc12345...xyz9").
     */
    private String maskedKeyValue;

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
    private LocalDateTime scheduledRevocationAt;
    private boolean active;

    private String message;
    private boolean success;

    /**
     * Create a DTO for a newly generated key (includes full key value).
     */
    public static UserApiKeyDto newKey(Long id, String name, String keyValue, LocalDateTime createdAt) {
        return UserApiKeyDto.builder()
                .id(id)
                .name(name)
                .keyValue(keyValue)
                .maskedKeyValue(maskKey(keyValue))
                .createdAt(createdAt)
                .active(true)
                .success(true)
                .message("API key generated successfully. Save this key securely - it won't be shown again!")
                .build();
    }

    /**
     * Create a DTO for an existing key (masked, no full value).
     */
    public static UserApiKeyDto existing(Long id, String name, String description,
                                         String maskedKeyValue, LocalDateTime createdAt,
                                         LocalDateTime lastUsedAt, boolean active) {
        return UserApiKeyDto.builder()
                .id(id)
                .name(name)
                .description(description)
                .maskedKeyValue(maskedKeyValue)
                .createdAt(createdAt)
                .lastUsedAt(lastUsedAt)
                .active(active)
                .success(true)
                .build();
    }

    /**
     * Create an error response.
     */
    public static UserApiKeyDto error(String message) {
        return UserApiKeyDto.builder()
                .success(false)
                .message(message)
                .build();
    }

    private static String maskKey(String keyValue) {
        if (keyValue == null || keyValue.length() < 12) {
            return "****";
        }
        return keyValue.substring(0, 8) + "..." + keyValue.substring(keyValue.length() - 4);
    }
}
