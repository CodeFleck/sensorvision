package org.sensorvision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.DashboardResponse;
import org.sensorvision.dto.DashboardTemplateResponse;
import org.sensorvision.dto.InstantiateDashboardTemplateRequest;
import org.sensorvision.model.*;
import org.sensorvision.repository.DashboardRepository;
import org.sensorvision.repository.DashboardTemplateRepository;
import org.sensorvision.repository.WidgetRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing dashboard templates
 */
@Service
@Slf4j
public class DashboardTemplateService {

    private final DashboardTemplateRepository templateRepository;
    private final DashboardRepository dashboardRepository;
    private final WidgetRepository widgetRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    @Autowired
    public DashboardTemplateService(DashboardTemplateRepository templateRepository,
                                   DashboardRepository dashboardRepository,
                                   WidgetRepository widgetRepository,
                                   SecurityUtils securityUtils,
                                   ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.dashboardRepository = dashboardRepository;
        this.widgetRepository = widgetRepository;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all available templates
     */
    @Transactional(readOnly = true)
    public List<DashboardTemplateResponse> getAllTemplates() {
        return templateRepository.findAll().stream()
            .map(DashboardTemplateResponse::fromEntitySummary)
            .collect(Collectors.toList());
    }

    /**
     * Get templates by category
     */
    @Transactional(readOnly = true)
    public List<DashboardTemplateResponse> getTemplatesByCategory(DashboardTemplateCategory category) {
        return templateRepository.findByCategory(category).stream()
            .map(DashboardTemplateResponse::fromEntitySummary)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific template with full configuration
     */
    @Transactional(readOnly = true)
    public DashboardTemplateResponse getTemplate(Long templateId) {
        DashboardTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));

        return DashboardTemplateResponse.fromEntity(template);
    }

    /**
     * Instantiate a dashboard from a template
     */
    @Transactional
    public DashboardResponse instantiateTemplate(Long templateId, InstantiateDashboardTemplateRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        // Fetch template
        DashboardTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));

        log.info("Instantiating dashboard from template {} for organization {}", template.getName(), userOrg.getId());

        // Create dashboard from template config
        Dashboard dashboard = new Dashboard();
        dashboard.setOrganization(userOrg);
        dashboard.setName(request.dashboardName());
        dashboard.setDescription(request.dashboardDescription() != null
            ? request.dashboardDescription()
            : template.getDescription());
        dashboard.setIsDefault(false);

        // Copy layout config if present
        JsonNode dashboardConfig = template.getDashboardConfig();
        if (dashboardConfig != null && dashboardConfig.has("layoutConfig")) {
            dashboard.setLayoutConfig(dashboardConfig.get("layoutConfig"));
        }

        // Save dashboard first to get ID
        Dashboard savedDashboard = dashboardRepository.save(dashboard);
        log.info("Created dashboard with id: {}", savedDashboard.getId());

        // Create widgets from template
        List<Widget> widgets = createWidgetsFromTemplate(
            savedDashboard,
            template.getWidgetsConfig(),
            request.deviceId()
        );

        // Save all widgets
        widgetRepository.saveAll(widgets);
        log.info("Created {} widgets for dashboard {}", widgets.size(), savedDashboard.getId());

        // Increment template usage count
        template.incrementUsageCount();
        templateRepository.save(template);

        // Reload dashboard with widgets
        Dashboard reloadedDashboard = dashboardRepository.findByIdWithWidgets(savedDashboard.getId())
            .orElseThrow(() -> new RuntimeException("Dashboard not found after creation"));

        return DashboardResponse.fromEntity(reloadedDashboard);
    }

    /**
     * Create widgets from template configuration
     */
    private List<Widget> createWidgetsFromTemplate(Dashboard dashboard, JsonNode widgetsConfig, String deviceId) {
        List<Widget> widgets = new ArrayList<>();

        if (widgetsConfig == null || !widgetsConfig.isArray()) {
            log.warn("No widgets configuration found in template");
            return widgets;
        }

        ArrayNode widgetsArray = (ArrayNode) widgetsConfig;

        for (JsonNode widgetConfig : widgetsArray) {
            try {
                Widget widget = new Widget();
                widget.setDashboard(dashboard);

                // Basic properties
                widget.setName(widgetConfig.get("name").asText());
                widget.setType(WidgetType.valueOf(widgetConfig.get("type").asText()));

                // Position and size
                widget.setPositionX(widgetConfig.has("positionX") ? widgetConfig.get("positionX").asInt() : 0);
                widget.setPositionY(widgetConfig.has("positionY") ? widgetConfig.get("positionY").asInt() : 0);
                widget.setWidth(widgetConfig.has("width") ? widgetConfig.get("width").asInt() : 4);
                widget.setHeight(widgetConfig.has("height") ? widgetConfig.get("height").asInt() : 4);

                // Device binding - use provided deviceId or placeholder
                if (deviceId != null && !deviceId.isEmpty()) {
                    widget.setDeviceId(deviceId);
                } else if (widgetConfig.has("deviceId") && !widgetConfig.get("deviceId").isNull()) {
                    widget.setDeviceId(widgetConfig.get("deviceId").asText());
                }

                // Variable name
                if (widgetConfig.has("variableName") && !widgetConfig.get("variableName").isNull()) {
                    widget.setVariableName(widgetConfig.get("variableName").asText());
                }

                // Aggregation
                if (widgetConfig.has("aggregation") && !widgetConfig.get("aggregation").isNull()) {
                    widget.setAggregation(WidgetAggregation.valueOf(widgetConfig.get("aggregation").asText()));
                } else {
                    widget.setAggregation(WidgetAggregation.NONE);
                }

                // Time range
                if (widgetConfig.has("timeRangeMinutes") && !widgetConfig.get("timeRangeMinutes").isNull()) {
                    widget.setTimeRangeMinutes(widgetConfig.get("timeRangeMinutes").asInt());
                }

                // Widget-specific configuration
                if (widgetConfig.has("config") && widgetConfig.get("config").isObject()) {
                    widget.setConfig(widgetConfig.get("config"));
                } else {
                    widget.setConfig(objectMapper.createObjectNode());
                }

                widgets.add(widget);
            } catch (Exception e) {
                log.error("Error creating widget from template config: {}", widgetConfig, e);
                // Continue with other widgets even if one fails
            }
        }

        return widgets;
    }

    /**
     * Delete a template (only non-system templates)
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        DashboardTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));

        if (template.getIsSystem()) {
            throw new AccessDeniedException("Cannot delete system templates");
        }

        templateRepository.delete(template);
        log.info("Deleted template: {}", templateId);
    }
}
