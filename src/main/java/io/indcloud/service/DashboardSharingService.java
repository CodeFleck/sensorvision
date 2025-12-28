package io.indcloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.DashboardPermissionRequest;
import io.indcloud.dto.DashboardPermissionResponse;
import io.indcloud.dto.DashboardSharingRequest;
import io.indcloud.exception.BadRequestException;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.*;
import io.indcloud.repository.DashboardPermissionRepository;
import io.indcloud.repository.DashboardRepository;
import io.indcloud.repository.UserRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DashboardSharingService {

    private final DashboardRepository dashboardRepository;
    private final DashboardPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final SecurityUtils securityUtils;

    public Dashboard updateSharingSettings(Long dashboardId, DashboardSharingRequest request) {
        Dashboard dashboard = findDashboardById(dashboardId);

        // CRITICAL: Initialize the widgets collection to prevent orphan removal errors
        // This ensures Hibernate doesn't try to replace the collection reference during save
        dashboard.getWidgets().size();
        dashboard.getPermissions().size();

        boolean wasPublic = dashboard.getIsPublic();
        dashboard.setIsPublic(request.isPublic());

        // Generate or remove public share token
        if (request.isPublic() && dashboard.getPublicShareToken() == null) {
            dashboard.setPublicShareToken(UUID.randomUUID().toString());
        } else if (!request.isPublic()) {
            dashboard.setPublicShareToken(null);
        }

        Dashboard updated = dashboardRepository.save(dashboard);

        // Emit event
        String action = request.isPublic() ? "enabled" : "disabled";
        eventService.createEvent(
                dashboard.getOrganization(),
                Event.EventType.DASHBOARD_UPDATED,
                Event.EventSeverity.INFO,
                "Public sharing " + action + " for dashboard '" + dashboard.getName() + "'",
                null
        );

        log.info("Updated sharing settings for dashboard: {} to public={}", dashboardId, request.isPublic());
        return updated;
    }

    @Transactional(readOnly = true)
    public List<DashboardPermissionResponse> getPermissions(Long dashboardId) {
        Dashboard dashboard = findDashboardById(dashboardId);
        return permissionRepository.findByDashboardId(dashboardId).stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());
    }

    public DashboardPermissionResponse addPermission(Long dashboardId, DashboardPermissionRequest request) {
        Dashboard dashboard = findDashboardById(dashboardId);
        // Initialize collections to prevent orphan removal errors
        dashboard.getWidgets().size();
        dashboard.getPermissions().size();

        Organization org = securityUtils.getCurrentUserOrganization();

        // Find user by email within the same organization
        User targetUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        // Verify user belongs to same organization
        if (!targetUser.getOrganization().getId().equals(org.getId())) {
            throw new BadRequestException("Cannot share with user from different organization");
        }

        // Check if permission already exists
        permissionRepository.findByDashboardIdAndUserId(dashboardId, targetUser.getId())
                .ifPresent(existing -> {
                    throw new BadRequestException("Permission already exists for this user");
                });

        DashboardPermission permission = DashboardPermission.builder()
                .dashboard(dashboard)
                .user(targetUser)
                .permissionLevel(request.permissionLevel())
                .expiresAt(request.expiresAt())
                .build();

        DashboardPermission saved = permissionRepository.save(permission);

        // Emit event
        eventService.createEvent(
                org,
                Event.EventType.DASHBOARD_UPDATED,
                Event.EventSeverity.INFO,
                "Granted " + request.permissionLevel() + " permission to " + targetUser.getEmail() +
                " for dashboard '" + dashboard.getName() + "'",
                null
        );

        log.info("Added permission for user {} to dashboard {}", targetUser.getEmail(), dashboardId);
        return toPermissionResponse(saved);
    }

    public DashboardPermissionResponse updatePermission(Long dashboardId, Long permissionId,
                                                        DashboardPermission.PermissionLevel permissionLevel) {
        Dashboard dashboard = findDashboardById(dashboardId);
        // Initialize collections to prevent orphan removal errors
        dashboard.getWidgets().size();
        dashboard.getPermissions().size();

        DashboardPermission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));

        // Verify permission belongs to this dashboard
        if (!permission.getDashboard().getId().equals(dashboardId)) {
            throw new BadRequestException("Permission does not belong to this dashboard");
        }

        permission.setPermissionLevel(permissionLevel);
        DashboardPermission updated = permissionRepository.save(permission);

        // Emit event
        eventService.createEvent(
                dashboard.getOrganization(),
                Event.EventType.DASHBOARD_UPDATED,
                Event.EventSeverity.INFO,
                "Updated permission level to " + permissionLevel + " for dashboard '" + dashboard.getName() + "'",
                null
        );

        log.info("Updated permission {} to level {}", permissionId, permissionLevel);
        return toPermissionResponse(updated);
    }

    public void revokePermission(Long dashboardId, Long permissionId) {
        Dashboard dashboard = findDashboardById(dashboardId);
        // Initialize collections to prevent orphan removal errors
        dashboard.getWidgets().size();
        dashboard.getPermissions().size();

        DashboardPermission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));

        // Verify permission belongs to this dashboard
        if (!permission.getDashboard().getId().equals(dashboardId)) {
            throw new BadRequestException("Permission does not belong to this dashboard");
        }

        String userEmail = permission.getUser().getEmail();
        permissionRepository.delete(permission);

        // Emit event
        eventService.createEvent(
                dashboard.getOrganization(),
                Event.EventType.DASHBOARD_UPDATED,
                Event.EventSeverity.INFO,
                "Revoked permission for " + userEmail + " from dashboard '" + dashboard.getName() + "'",
                null
        );

        log.info("Revoked permission {} from dashboard {}", permissionId, dashboardId);
    }

    private Dashboard findDashboardById(Long id) {
        Dashboard dashboard = dashboardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard not found: " + id));

        // Verify organization access
        Organization currentOrg = securityUtils.getCurrentUserOrganization();
        if (!dashboard.getOrganization().getId().equals(currentOrg.getId())) {
            throw new ResourceNotFoundException("Dashboard not found: " + id);
        }

        return dashboard;
    }

    private DashboardPermissionResponse toPermissionResponse(DashboardPermission permission) {
        User user = permission.getUser();
        return new DashboardPermissionResponse(
                permission.getId(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                permission.getPermissionLevel(),
                permission.getGrantedAt(),
                permission.getExpiresAt()
        );
    }
}
