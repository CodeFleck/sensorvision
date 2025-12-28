package io.indcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.indcloud.model.DeviceSelectorType;
import io.indcloud.model.RuleOperator;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalRuleCreateRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotNull(message = "Selector type is required")
    private DeviceSelectorType selectorType;

    private String selectorValue;

    @NotBlank(message = "Aggregation function is required")
    private String aggregationFunction;

    private String aggregationVariable;

    private Map<String, Object> aggregationParams;

    @NotNull(message = "Operator is required")
    private RuleOperator operator;

    @NotNull(message = "Threshold is required")
    private BigDecimal threshold;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private String evaluationInterval = "5m";

    @Builder.Default
    private Integer cooldownMinutes = 5;

    @Builder.Default
    private Boolean sendSms = false;

    private String[] smsRecipients;
}
