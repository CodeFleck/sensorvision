package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.indcloud.dto.*;
import io.indcloud.model.Dashboard;
import io.indcloud.model.Organization;
import io.indcloud.model.Widget;
import io.indcloud.repository.DashboardRepository;
import io.indcloud.repository.WidgetRepository;
import io.indcloud.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private final DashboardRepository dashboardRepository;
    private final WidgetRepository widgetRepository;
    private final ObjectMapper objectMapper;
    private final DefaultDashboardInitializer defaultDashboardInitializer;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;

    public DashboardService(DashboardRepository dashboardRepository,
                          WidgetRepository widgetRepository,
                          ObjectMapper objectMapper,
                          DefaultDashboardInitializer defaultDashboardInitializer,
                          SecurityUtils securityUtils,
                          PasswordEncoder passwordEncoder) {
        this.dashboardRepository = dashboardRepository;
        this.widgetRepository = widgetRepository;
        this.objectMapper = objectMapper;
        this.defaultDashboardInitializer = defaultDashboardInitializer;
        this.securityUtils = securityUtils;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Get all dashboards (without widgets for performance)
     */
    @Transactional(readOnly = true)
    public List<DashboardResponse> getAllDashboards() {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        return dashboardRepository.findByOrganization(userOrg).stream()
            .map(DashboardResponse::fromEntityWithoutWidgets)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific dashboard with all its widgets
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardById(Long id) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Dashboard dashboard = dashboardRepository.findByIdWithWidgets(id)
            .orElseThrow(() -> new RuntimeException("Dashboard not found with id: " + id));

        // Verify dashboard belongs to user's organization
        if (!dashboard.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to dashboard: " + id);
        }

        return DashboardResponse.fromEntity(dashboard);
    }

    /**
     * Get the default dashboard
     * If no default dashboard exists, creates one automatically
     */
    @Transactional
    public DashboardResponse getDefaultDashboard() {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        defaultDashboardInitializer.ensureDefaultDashboardExists(userOrg);

        Dashboard dashboard = dashboardRepository.findByOrganizationAndIsDefaultTrueWithWidgets(userOrg)
            .orElseThrow(() -> new RuntimeException("Default dashboard could not be loaded after initialization"));

        dashboard.getWidgets().size();
        dashboard.getPermissions().size();

        return DashboardResponse.fromEntity(dashboard);
    }

    /**
     * Create a new dashboard
     */
    @Transactional
    public DashboardResponse createDashboard(DashboardCreateRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        logger.info("Creating new dashboard: {} for organization: {}", request.name(), userOrg.getId());

        // If this dashboard is set as default, unset any existing default for this organization
        if (Boolean.TRUE.equals(request.isDefault())) {
            dashboardRepository.findByOrganizationAndIsDefaultTrue(userOrg).ifPresent(existingDefault -> {
                // CRITICAL: Initialize collections to prevent orphan removal errors
                existingDefault.getWidgets().size();
                existingDefault.getPermissions().size();
                existingDefault.setIsDefault(false);
                dashboardRepository.save(existingDefault);
            });
        }

        Dashboard dashboard = new Dashboard();
        dashboard.setName(request.name());
        dashboard.setDescription(request.description());
        dashboard.setIsDefault(request.isDefault() != null ? request.isDefault() : false);
        dashboard.setLayoutConfig(request.layoutConfig());
        dashboard.setOrganization(userOrg);  // Set organization from current user

        Dashboard saved = dashboardRepository.save(dashboard);
        logger.info("Dashboard created with id: {}", saved.getId());

        return DashboardResponse.fromEntity(saved);
    }

    /**
     * Update an existing dashboard
     */
    @Transactional
    public DashboardResponse updateDashboard(Long id, DashboardUpdateRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        logger.info("Updating dashboard: {}", id);

        Dashboard dashboard = dashboardRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dashboard not found with id: " + id));

        // CRITICAL: Initialize collections to prevent orphan removal errors
        dashboard.getWidgets().size();
        dashboard.getPermissions().size();

        // Verify dashboard belongs to user's organization
        if (!dashboard.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to dashboard: " + id);
        }

        if (request.name() != null) {
            dashboard.setName(request.name());
        }
        if (request.description() != null) {
            dashboard.setDescription(request.description());
        }
        if (request.defaultDeviceId() != null) {
            dashboard.setDefaultDeviceId(request.defaultDeviceId());
        }
        if (request.isDefault() != null) {
            // If setting this as default, unset any existing default for this organization
            if (Boolean.TRUE.equals(request.isDefault())) {
                dashboardRepository.findByOrganizationAndIsDefaultTrue(userOrg).ifPresent(existingDefault -> {
                    if (!existingDefault.getId().equals(id)) {
                        // CRITICAL: Initialize collections to prevent orphan removal errors
                        existingDefault.getWidgets().size();
                        existingDefault.getPermissions().size();
                        existingDefault.setIsDefault(false);
                        dashboardRepository.save(existingDefault);
                    }
                });
            }
            dashboard.setIsDefault(request.isDefault());
        }
        if (request.layoutConfig() != null) {
            dashboard.setLayoutConfig(request.layoutConfig());
        }

        Dashboard updated = dashboardRepository.save(dashboard);

        // Trigger lazy loading of widgets within the transaction
        updated.getWidgets().size();

        return DashboardResponse.fromEntity(updated);
    }

    /**
     * Delete a dashboard and all its widgets
     */
    @Transactional
    public void deleteDashboard(Long id) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        logger.info("Deleting dashboard: {}", id);

        Dashboard dashboard = dashboardRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dashboard not found with id: " + id));

        // Verify dashboard belongs to user's organization
        if (!dashboard.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to dashboard: " + id);
        }

        // Prevent deletion of the default dashboard if it's the only one
        if (Boolean.TRUE.equals(dashboard.getIsDefault()) && dashboardRepository.count() == 1) {
            throw new RuntimeException("Cannot delete the last remaining default dashboard");
        }

        dashboardRepository.delete(dashboard);
        logger.info("Dashboard deleted: {}", id);
    }

    /**
     * Add a widget to a dashboard
     */
    @Transactional
    public WidgetResponse addWidget(Long dashboardId, WidgetCreateRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        logger.info("Adding widget to dashboard {}: {}", dashboardId, request.name());

        Dashboard dashboard = dashboardRepository.findById(dashboardId)
            .orElseThrow(() -> new RuntimeException("Dashboard not found with id: " + dashboardId));

        // CRITICAL: Initialize collections to prevent orphan removal errors
        dashboard.getWidgets().size();
        dashboard.getPermissions().size();

        // Verify dashboard belongs to user's organization
        if (!dashboard.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to dashboard: " + dashboardId);
        }

        Widget widget = new Widget();
        widget.setDashboard(dashboard);
        widget.setName(request.name());
        widget.setType(request.type());
        widget.setPositionX(request.positionX() != null ? request.positionX() : 0);
        widget.setPositionY(request.positionY() != null ? request.positionY() : 0);
        widget.setWidth(request.width() != null ? request.width() : 4);
        widget.setHeight(request.height() != null ? request.height() : 4);
        widget.setDeviceId(request.deviceId());
        widget.setSecondDeviceId(request.secondDeviceId());
        widget.setVariableName(request.variableName());
        widget.setSecondVariableName(request.secondVariableName());
        widget.setDeviceLabel(request.deviceLabel());
        widget.setSecondDeviceLabel(request.secondDeviceLabel());
        widget.setAggregation(request.aggregation() != null ? request.aggregation() : io.indcloud.model.WidgetAggregation.NONE);
        widget.setTimeRangeMinutes(request.timeRangeMinutes());
        widget.setConfig(request.config() != null ? request.config() : objectMapper.createObjectNode());

        Widget saved = widgetRepository.save(widget);
        logger.info("Widget created with id: {}", saved.getId());

        return WidgetResponse.fromEntity(saved);
    }

    /**
     * Update a widget
     */
    @Transactional
    public WidgetResponse updateWidget(Long widgetId, WidgetUpdateRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        logger.info("Updating widget: {}", widgetId);

        Widget widget = widgetRepository.findById(widgetId)
            .orElseThrow(() -> new RuntimeException("Widget not found with id: " + widgetId));

        // Verify widget's dashboard belongs to user's organization
        if (!widget.getDashboard().getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to widget: " + widgetId);
        }

        if (request.name() != null) {
            widget.setName(request.name());
        }
        if (request.type() != null) {
            widget.setType(request.type());
        }
        if (request.positionX() != null) {
            widget.setPositionX(request.positionX());
        }
        if (request.positionY() != null) {
            widget.setPositionY(request.positionY());
        }
        if (request.width() != null) {
            widget.setWidth(request.width());
        }
        if (request.height() != null) {
            widget.setHeight(request.height());
        }
        if (request.deviceId() != null) {
            widget.setDeviceId(request.deviceId());
        }
        if (request.secondDeviceId() != null) {
            widget.setSecondDeviceId(request.secondDeviceId());
        }
        if (request.variableName() != null) {
            widget.setVariableName(request.variableName());
        }
        if (request.secondVariableName() != null) {
            widget.setSecondVariableName(request.secondVariableName());
        }
        if (request.deviceLabel() != null) {
            widget.setDeviceLabel(request.deviceLabel());
        }
        if (request.secondDeviceLabel() != null) {
            widget.setSecondDeviceLabel(request.secondDeviceLabel());
        }
        if (request.aggregation() != null) {
            widget.setAggregation(request.aggregation());
        }
        if (request.timeRangeMinutes() != null) {
            widget.setTimeRangeMinutes(request.timeRangeMinutes());
        }
        if (request.config() != null) {
            widget.setConfig(request.config());
        }

        Widget updated = widgetRepository.save(widget);
        return WidgetResponse.fromEntity(updated);
    }

    /**
     * Delete a widget
     */
    @Transactional
    public void deleteWidget(Long widgetId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        logger.info("Deleting widget: {}", widgetId);

        Widget widget = widgetRepository.findById(widgetId)
            .orElseThrow(() -> new RuntimeException("Widget not found with id: " + widgetId));

        // Verify widget's dashboard belongs to user's organization
        if (!widget.getDashboard().getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to widget: " + widgetId);
        }

        widgetRepository.delete(widget);
        logger.info("Widget deleted: {}", widgetId);
    }

    /**
     * Get all widgets for a dashboard
     */
    @Transactional(readOnly = true)
    public List<WidgetResponse> getWidgetsByDashboard(Long dashboardId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        // Verify dashboard belongs to user's organization
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
            .orElseThrow(() -> new RuntimeException("Dashboard not found with id: " + dashboardId));

        if (!dashboard.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to dashboard: " + dashboardId);
        }

        return widgetRepository.findByDashboardId(dashboardId).stream()
            .map(WidgetResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Enable or update dashboard sharing
     */
    @Transactional
    public DashboardShareResponse configureDashboardSharing(Long dashboardId, DashboardShareRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
            .orElseThrow(() -> new RuntimeException("Dashboard not found with id: " + dashboardId));

        // Verify dashboard belongs to user's organization
        if (!dashboard.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to dashboard: " + dashboardId);
        }

        dashboard.setIsPublic(request.getIsPublic());
        dashboard.setAllowAnonymousView(request.getAllowAnonymousView());
        dashboard.setShareExpiresAt(request.getExpiresAt());

        if (request.getIsPublic()) {
            // Generate share token if enabling sharing
            if (dashboard.getPublicShareToken() == null) {
                dashboard.setPublicShareToken(UUID.randomUUID().toString());
            }

            // Set password hash if password provided
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                dashboard.setSharePasswordHash(passwordEncoder.encode(request.getPassword()));
            } else {
                dashboard.setSharePasswordHash(null); // Remove password if not provided
            }
        } else {
            // Disable sharing
            dashboard.setPublicShareToken(null);
            dashboard.setSharePasswordHash(null);
            dashboard.setShareExpiresAt(null);
        }

        Dashboard saved = dashboardRepository.save(dashboard);

        String shareUrl = saved.getPublicShareToken() != null
            ? "/public/dashboards/" + saved.getPublicShareToken()
            : null;

        return DashboardShareResponse.builder()
            .dashboardId(saved.getId())
            .dashboardName(saved.getName())
            .isPublic(saved.getIsPublic())
            .shareUrl(shareUrl)
            .shareToken(saved.getPublicShareToken())
            .expiresAt(saved.getShareExpiresAt())
            .isPasswordProtected(saved.getSharePasswordHash() != null)
            .allowAnonymousView(saved.getAllowAnonymousView())
            .message(saved.getIsPublic() ? "Dashboard sharing enabled" : "Dashboard sharing disabled")
            .build();
    }

    /**
     * Get public dashboard by share token (for anonymous access)
     */
    @Transactional(readOnly = true)
    public DashboardResponse getPublicDashboard(String shareToken, String password) {
        Dashboard dashboard = dashboardRepository.findByPublicShareToken(shareToken)
            .orElseThrow(() -> new RuntimeException("Dashboard not found or not shared"));

        // Check if dashboard is actually public
        if (!dashboard.getIsPublic()) {
            throw new AccessDeniedException("Dashboard is not publicly shared");
        }

        // Check if share has expired
        if (dashboard.getShareExpiresAt() != null && LocalDateTime.now().isAfter(dashboard.getShareExpiresAt())) {
            throw new AccessDeniedException("Share link has expired");
        }

        // Check password if required
        if (dashboard.getSharePasswordHash() != null) {
            if (password == null || !passwordEncoder.matches(password, dashboard.getSharePasswordHash())) {
                throw new AccessDeniedException("Invalid password");
            }
        }

        return DashboardResponse.fromEntity(dashboard);
    }

    /**
     * Disable dashboard sharing
     */
    @Transactional
    public void disableDashboardSharing(Long dashboardId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
            .orElseThrow(() -> new RuntimeException("Dashboard not found with id: " + dashboardId));

        if (!dashboard.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to dashboard: " + dashboardId);
        }

        dashboard.setIsPublic(false);
        dashboard.setPublicShareToken(null);
        dashboard.setSharePasswordHash(null);
        dashboard.setShareExpiresAt(null);

        dashboardRepository.save(dashboard);
    }

}
























