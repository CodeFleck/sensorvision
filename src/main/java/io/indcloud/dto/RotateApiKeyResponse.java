package io.indcloud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for API key rotation with optional grace period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RotateApiKeyResponse {

    /**
     * The newly generated API key.
     */
    private UserApiKeyDto newKey;

    /**
     * When the old key will expire (if grace period was used).
     * Null if the old key was revoked immediately.
     */
    private LocalDateTime oldKeyValidUntil;

    /**
     * The grace period in minutes that was applied.
     * 0 or null if immediate revocation.
     */
    private Integer gracePeriodMinutes;
}
