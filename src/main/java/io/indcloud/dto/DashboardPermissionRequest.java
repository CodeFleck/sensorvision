package io.indcloud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.indcloud.model.DashboardPermission;

import java.time.Instant;

/**
 * Request DTO for creating/updating dashboard permissions
 */
public record DashboardPermissionRequest(
        @NotBlank(message = "User email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Permission level is required")
        DashboardPermission.PermissionLevel permissionLevel,

        Instant expiresAt
) {
}
