package io.indcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import io.indcloud.model.RuleOperator;

public record RuleCreateRequest(
        @NotBlank String name,
        String description,
        @NotBlank String deviceId,
        @NotBlank String variable,
        @NotNull RuleOperator operator,
        @NotNull BigDecimal threshold,
        Boolean enabled
) {
}