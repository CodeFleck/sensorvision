package io.indcloud.dto;

import io.indcloud.model.DeviceTypeRuleTemplate.RuleOperator;
import io.indcloud.model.DeviceTypeRuleTemplate.RuleSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for device type rule template.
 */
public record DeviceTypeRuleTemplateRequest(
        @NotBlank(message = "Rule name is required")
        @Size(max = 255)
        String name,

        String description,

        @NotBlank(message = "Variable name is required")
        @Size(max = 100)
        String variableName,

        @NotNull(message = "Operator is required")
        RuleOperator operator,

        @NotNull(message = "Threshold value is required")
        BigDecimal thresholdValue,

        RuleSeverity severity,

        String notificationMessage,

        Boolean enabled,

        Integer displayOrder
) {}
