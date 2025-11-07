package org.sensorvision.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for SMS settings
 */
public record SmsSettingsResponse(
    UUID id,
    Boolean enabled,
    Integer dailyLimit,
    BigDecimal monthlyBudget,
    Integer currentMonthCount,
    BigDecimal currentMonthCost,
    Boolean alertOnBudgetThreshold,
    Integer budgetThresholdPercentage,
    Instant createdAt,
    Instant updatedAt
) {}
