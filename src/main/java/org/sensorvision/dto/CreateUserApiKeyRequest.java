package org.sensorvision.dto;

import jakarta.validation.constraints.Size;
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
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    /**
     * Optional description for the API key.
     */
    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;
}
