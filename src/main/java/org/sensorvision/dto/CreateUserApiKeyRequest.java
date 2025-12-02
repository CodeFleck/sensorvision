package org.sensorvision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new user API key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserApiKeyRequest {
    /**
     * Optional name for the API key. Defaults to "Default Token".
     */
    private String name;

    /**
     * Optional description for the API key.
     */
    private String description;
}
