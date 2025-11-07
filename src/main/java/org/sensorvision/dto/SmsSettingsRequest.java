package org.sensorvision.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for updating SMS settings
 */
public record SmsSettingsRequest(
    @NotNull(message = "Enabled flag is required")
    Boolean enabled,

    @NotNull(message = "Daily limit is required")
    @Min(value = 1, message = "Daily limit must be at least 1")
    @Max(value = 10000, message = "Daily limit cannot exceed 10,000")
    Integer dailyLimit,

    @NotNull(message = "Monthly budget is required")
    @Min(value = 0, message = "Monthly budget must be positive")
    BigDecimal monthlyBudget,

    @NotNull(message = "Alert on budget threshold flag is required")
    Boolean alertOnBudgetThreshold,

    @NotNull(message = "Budget threshold percentage is required")
    @Min(value = 1, message = "Budget threshold must be at least 1%")
    @Max(value = 100, message = "Budget threshold cannot exceed 100%")
    Integer budgetThresholdPercentage
) {}
