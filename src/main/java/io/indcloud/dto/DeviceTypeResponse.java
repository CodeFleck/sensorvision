package io.indcloud.dto;

import io.indcloud.model.DeviceType;
import io.indcloud.model.DeviceType.TemplateCategory;
import io.indcloud.model.DeviceTypeDashboardTemplate;
import io.indcloud.model.DeviceTypeRuleTemplate;
import io.indcloud.model.DeviceTypeVariable;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for device type.
 */
public record DeviceTypeResponse(
        Long id,
        String name,
        String description,
        String icon,
        String color,
        TemplateCategory category,
        Boolean isActive,
        Boolean isSystemTemplate,
        List<DeviceTypeVariableResponse> variables,
        List<DeviceTypeRuleTemplateResponse> ruleTemplates,
        List<DeviceTypeDashboardTemplateResponse> dashboardTemplates,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Create response from entity.
     */
    public static DeviceTypeResponse from(DeviceType entity) {
        return new DeviceTypeResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getIcon(),
                entity.getColor(),
                entity.getTemplateCategory(),
                entity.getIsActive(),
                entity.getIsSystemTemplate(),
                entity.getVariables().stream()
                        .map(DeviceTypeVariableResponse::from)
                        .toList(),
                entity.getRuleTemplates().stream()
                        .map(DeviceTypeRuleTemplateResponse::from)
                        .toList(),
                entity.getDashboardTemplates().stream()
                        .map(DeviceTypeDashboardTemplateResponse::from)
                        .toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Response for variable definition.
     */
    public record DeviceTypeVariableResponse(
            Long id,
            String name,
            String label,
            String unit,
            String dataType,
            java.math.BigDecimal minValue,
            java.math.BigDecimal maxValue,
            Boolean required,
            String defaultValue,
            String description,
            Integer displayOrder
    ) {
        public static DeviceTypeVariableResponse from(DeviceTypeVariable v) {
            return new DeviceTypeVariableResponse(
                    v.getId(),
                    v.getName(),
                    v.getLabel(),
                    v.getUnit(),
                    v.getDataType() != null ? v.getDataType().name() : null,
                    v.getMinValue(),
                    v.getMaxValue(),
                    v.getRequired(),
                    v.getDefaultValue(),
                    v.getDescription(),
                    v.getDisplayOrder()
            );
        }
    }

    /**
     * Response for rule template.
     */
    public record DeviceTypeRuleTemplateResponse(
            Long id,
            String name,
            String description,
            String variableName,
            String operator,
            java.math.BigDecimal thresholdValue,
            String severity,
            String notificationMessage,
            Boolean enabled,
            Integer displayOrder
    ) {
        public static DeviceTypeRuleTemplateResponse from(DeviceTypeRuleTemplate r) {
            return new DeviceTypeRuleTemplateResponse(
                    r.getId(),
                    r.getName(),
                    r.getDescription(),
                    r.getVariableName(),
                    r.getOperator() != null ? r.getOperator().name() : null,
                    r.getThresholdValue(),
                    r.getSeverity() != null ? r.getSeverity().name() : null,
                    r.getNotificationMessage(),
                    r.getEnabled(),
                    r.getDisplayOrder()
            );
        }
    }

    /**
     * Response for dashboard template widget.
     */
    public record DeviceTypeDashboardTemplateResponse(
            Long id,
            String widgetType,
            String title,
            String variableName,
            Object config,
            Integer gridX,
            Integer gridY,
            Integer gridWidth,
            Integer gridHeight,
            Integer displayOrder
    ) {
        public static DeviceTypeDashboardTemplateResponse from(DeviceTypeDashboardTemplate d) {
            return new DeviceTypeDashboardTemplateResponse(
                    d.getId(),
                    d.getWidgetType() != null ? d.getWidgetType().name() : null,
                    d.getTitle(),
                    d.getVariableName(),
                    d.getConfig(),
                    d.getGridX(),
                    d.getGridY(),
                    d.getGridWidth(),
                    d.getGridHeight(),
                    d.getDisplayOrder()
            );
        }
    }
}
