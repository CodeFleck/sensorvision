package org.sensorvision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserPreferencesRequest {
    @NotBlank(message = "Theme preference is required")
    @Pattern(regexp = "^(light|dark|system)$", message = "Theme preference must be 'light', 'dark', or 'system'")
    private String themePreference;
}
