package io.indcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SecretCreateRequest(
    @NotBlank(message = "Secret key is required")
    @Size(min = 2, max = 100, message = "Secret key must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
            message = "Secret key must start with uppercase letter and contain only uppercase letters, numbers, and underscores (e.g., API_KEY, DATABASE_URL)")
    String secretKey,

    @NotBlank(message = "Secret value is required")
    String secretValue
) {
}
