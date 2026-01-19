package io.indcloud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.indcloud.model.*;
import io.indcloud.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for auto-provisioning device resources based on device type templates.
 * When a device is assigned to a device type, this service can automatically create:
 * - Variables defined in the device type
 * - Alert rules from rule templates
 * - Dashboard with widgets from dashboard templates
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceAutoProvisioningService {

    private final DeviceRepository deviceRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final DeviceTemplateApplicationRepository templateApplicationRepository;
    private final VariableRepository variableRepository;
    private final RuleRepository ruleRepository;
    private final DashboardRepository dashboardRepository;
    private final WidgetRepository widgetRepository;
    private final EventService eventService;

    /**
     * Apply a device type template to a device.
     * Creates variables, rules, and dashboard based on the template configuration.
     *
     * @param device The device to apply the template to
     * @param deviceType The device type template to apply
     * @param appliedBy Username of the person applying the template
     * @return The template application record
     */
    public DeviceTemplateApplication applyTemplate(Device device, DeviceType deviceType, String appliedBy) {
        // Check if template was already applied
        if (templateApplicationRepository.existsByDeviceIdAndDeviceType(device.getId(), deviceType)) {
            log.info("Template {} already applied to device {}", deviceType.getName(), device.getExternalId());
            return templateApplicationRepository.findByDeviceIdAndDeviceType(device.getId(), deviceType)
                    .orElseThrow();
        }

        log.info("Applying template {} to device {}", deviceType.getName(), device.getExternalId());

        int variablesCreated = createVariablesFromTemplate(device, deviceType);
        int rulesCreated = createRulesFromTemplate(device, deviceType);
        boolean dashboardCreated = createDashboardFromTemplate(device, deviceType);

        // Record the template application
        DeviceTemplateApplication application = DeviceTemplateApplication.builder()
                .deviceId(device.getId())
                .deviceType(deviceType)
                .appliedAt(Instant.now())
                .appliedBy(appliedBy)
                .variablesCreated(variablesCreated)
                .rulesCreated(rulesCreated)
                .dashboardCreated(dashboardCreated)
                .build();

        application = templateApplicationRepository.save(application);

        // Update device with the device type
        device.setDeviceType(deviceType);
        deviceRepository.save(device);

        // Emit event
        eventService.createEvent(
                device.getOrganization(),
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Device Template Applied",
                String.format("Template '%s' applied to device '%s': %d variables, %d rules, dashboard: %s",
                        deviceType.getName(), device.getName(),
                        variablesCreated, rulesCreated, dashboardCreated ? "created" : "not created")
        );

        log.info("Template application complete: {} variables, {} rules, dashboard: {}",
                variablesCreated, rulesCreated, dashboardCreated);

        return application;
    }

    /**
     * Create variables from device type variable definitions.
     */
    private int createVariablesFromTemplate(Device device, DeviceType deviceType) {
        var templateVars = deviceType.getVariables();
        int created = 0;

        for (DeviceTypeVariable templateVar : templateVars) {
            // Check if variable already exists for this device
            if (variableRepository.findByDeviceAndName(device, templateVar.getName()).isPresent()) {
                log.debug("Variable {} already exists for device {}", templateVar.getName(), device.getExternalId());
                continue;
            }

            Variable variable = Variable.builder()
                    .organization(device.getOrganization())
                    .device(device)
                    .name(templateVar.getName())
                    .displayName(templateVar.getLabel())
                    .description(templateVar.getDescription())
                    .unit(templateVar.getUnit())
                    .dataType(mapDataType(templateVar.getDataType()))
                    .minValue(templateVar.getMinValue() != null ? templateVar.getMinValue().doubleValue() : null)
                    .maxValue(templateVar.getMaxValue() != null ? templateVar.getMaxValue().doubleValue() : null)
                    .dataSource(Variable.DataSource.MANUAL)
                    .isSystemVariable(false)
                    .build();

            variableRepository.save(variable);
            created++;
            log.debug("Created variable {} for device {}", templateVar.getName(), device.getExternalId());
        }

        return created;
    }

    /**
     * Create alert rules from device type rule templates.
     */
    private int createRulesFromTemplate(Device device, DeviceType deviceType) {
        var ruleTemplates = deviceType.getRuleTemplates();
        int created = 0;

        for (DeviceTypeRuleTemplate template : ruleTemplates) {
            if (!template.getEnabled()) {
                continue;
            }

            // Check if a rule with same name already exists for this device
            if (ruleRepository.existsByDeviceAndName(device, template.getName())) {
                log.debug("Rule {} already exists for device {}", template.getName(), device.getExternalId());
                continue;
            }

            Rule rule = Rule.builder()
                    .name(template.getName())
                    .description(template.getDescription())
                    .device(device)
                    .organization(device.getOrganization())
                    .variable(template.getVariableName())
                    .operator(mapRuleOperator(template.getOperator()))
                    .threshold(template.getThresholdValue())
                    .enabled(true)
                    .build();

            ruleRepository.save(rule);
            created++;
            log.debug("Created rule {} for device {}", template.getName(), device.getExternalId());
        }

        return created;
    }

    /**
     * Create a dashboard with widgets from device type dashboard templates.
     */
    private boolean createDashboardFromTemplate(Device device, DeviceType deviceType) {
        var widgetTemplates = deviceType.getDashboardTemplates();

        if (widgetTemplates.isEmpty()) {
            return false;
        }

        // Create the dashboard
        String dashboardName = String.format("%s - %s", device.getName(), deviceType.getName());

        // Check if dashboard already exists
        if (dashboardRepository.existsByOrganizationAndName(device.getOrganization(), dashboardName)) {
            log.debug("Dashboard {} already exists", dashboardName);
            return false;
        }

        Dashboard dashboard = new Dashboard();
        dashboard.setName(dashboardName);
        dashboard.setDescription(String.format("Auto-generated dashboard for %s (%s)", device.getName(), deviceType.getName()));
        dashboard.setOrganization(device.getOrganization());
        dashboard.setDefaultDeviceId(device.getExternalId());
        dashboard.setIsPublic(false);
        dashboard.setIsDefault(false);

        dashboard = dashboardRepository.save(dashboard);

        // Create widgets
        for (DeviceTypeDashboardTemplate template : widgetTemplates) {
            Widget widget = new Widget();
            widget.setDashboard(dashboard);
            widget.setType(mapWidgetType(template.getWidgetType()));
            widget.setName(template.getTitle());
            widget.setPositionX(template.getGridX());
            widget.setPositionY(template.getGridY());
            widget.setWidth(template.getGridWidth());
            widget.setHeight(template.getGridHeight());
            widget.setConfig(template.getConfig() != null ? template.getConfig() : JsonNodeFactory.instance.objectNode());

            // If widget is for a specific variable, configure it
            if (template.getVariableName() != null) {
                widget.setVariableName(template.getVariableName());
                widget.setDeviceId(device.getExternalId());
            }

            widgetRepository.save(widget);
        }

        log.debug("Created dashboard {} with {} widgets for device {}",
                dashboardName, widgetTemplates.size(), device.getExternalId());

        return true;
    }

    /**
     * Map DeviceTypeVariable data type to Variable data type.
     */
    private Variable.DataType mapDataType(DeviceTypeVariable.VariableDataType type) {
        return switch (type) {
            case NUMBER -> Variable.DataType.NUMBER;
            case BOOLEAN -> Variable.DataType.BOOLEAN;
            case STRING -> Variable.DataType.STRING;
            case JSON, LOCATION, DATETIME -> Variable.DataType.JSON;
        };
    }

    /**
     * Map DeviceTypeRuleTemplate operator to Rule operator.
     */
    private RuleOperator mapRuleOperator(DeviceTypeRuleTemplate.RuleOperator op) {
        return switch (op) {
            case GT -> RuleOperator.GT;
            case GTE -> RuleOperator.GTE;
            case LT -> RuleOperator.LT;
            case LTE -> RuleOperator.LTE;
            case EQ -> RuleOperator.EQ;
        };
    }

    /**
     * Map DeviceTypeDashboardTemplate widget type to standalone WidgetType enum.
     */
    private WidgetType mapWidgetType(DeviceTypeDashboardTemplate.WidgetType type) {
        return switch (type) {
            case LINE_CHART -> WidgetType.LINE_CHART;
            case GAUGE -> WidgetType.GAUGE;
            case METRIC -> WidgetType.METRIC_CARD;
            case TABLE -> WidgetType.TABLE;
            case MAP -> WidgetType.MAP;
            case STATUS_INDICATOR -> WidgetType.INDICATOR;
            case CONTROL_BUTTON -> WidgetType.CONTROL_BUTTON;
            case THERMOMETER, TANK, IMAGE, TEXT -> WidgetType.INDICATOR;
        };
    }

    /**
     * Get all template applications for a device.
     */
    @Transactional(readOnly = true)
    public List<DeviceTemplateApplication> getTemplateApplications(UUID deviceId) {
        return templateApplicationRepository.findByDeviceId(deviceId);
    }

    /**
     * Get the count of devices using a specific template.
     */
    @Transactional(readOnly = true)
    public long getTemplateUsageCount(DeviceType deviceType) {
        return templateApplicationRepository.countByDeviceType(deviceType);
    }
}
