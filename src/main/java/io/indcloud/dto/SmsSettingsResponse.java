package io.indcloud.dto;

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
    Integer currentDayCount,
    Instant lastResetDate,
    Boolean alertOnBudgetThreshold,
    Integer budgetThresholdPercentage,
    Instant createdAt,
    Instant updatedAt
) {}
