package org.sensorvision.model;

import java.math.BigDecimal;

public enum RuleOperator {
    GT("Greater than"),
    GTE("Greater than or equal"),
    LT("Less than"),
    LTE("Less than or equal"),
    EQ("Equal to");

    private final String description;

    RuleOperator(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Evaluate the operator against two values
     */
    public boolean evaluate(BigDecimal actualValue, BigDecimal thresholdValue) {
        if (actualValue == null || thresholdValue == null) {
            return false;
        }

        int comparison = actualValue.compareTo(thresholdValue);

        return switch (this) {
            case GT -> comparison > 0;
            case GTE -> comparison >= 0;
            case LT -> comparison < 0;
            case LTE -> comparison <= 0;
            case EQ -> comparison == 0;
        };
    }
}