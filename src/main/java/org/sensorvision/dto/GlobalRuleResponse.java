package org.sensorvision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sensorvision.model.DeviceSelectorType;
import org.sensorvision.model.RuleOperator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalRuleResponse {

    private UUID id;
    private String name;
    private String description;
    private Long organizationId;

    private DeviceSelectorType selectorType;
    private String selectorValue;

    private String aggregationFunction;
    private String aggregationVariable;
    private Map<String, Object> aggregationParams;

    private RuleOperator operator;
    private BigDecimal threshold;

    private Boolean enabled;
    private String evaluationInterval;
    private Integer cooldownMinutes;

    private Instant lastEvaluatedAt;
    private Instant lastTriggeredAt;

    private Boolean sendSms;
    private String[] smsRecipients;

    private Instant createdAt;
    private Instant updatedAt;
}
