package io.indcloud.dto;

import io.indcloud.model.DeviceType.TemplateCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating or updating a device type.
 */
public record DeviceTypeRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Size(max = 50, message = "Icon must be at most 50 characters")
        String icon,

        @Size(max = 7, message = "Color must be a hex code (e.g., #FF5733)")
        String color,

        TemplateCategory category,

        List<DeviceTypeVariableRequest> variables,

        List<DeviceTypeRuleTemplateRequest> ruleTemplates,

        List<DeviceTypeDashboardTemplateRequest> dashboardTemplates
) {}
