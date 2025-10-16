package org.sensorvision.dto;

import org.sensorvision.model.DashboardPermission;

import java.time.Instant;

/**
 * Response DTO for dashboard permissions
 */
public record DashboardPermissionResponse(
        Long id,
        Long userId,
        String username,
        String email,
        DashboardPermission.PermissionLevel permissionLevel,
        Instant grantedAt,
        Instant expiresAt
) {
}
