package io.indcloud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.Alert;
import io.indcloud.model.AlertSeverity;
import io.indcloud.model.Device;
import io.indcloud.model.Event;
import io.indcloud.model.Rule;
import io.indcloud.model.TelemetryRecord;
import io.indcloud.repository.AlertRepository;
import io.indcloud.repository.RuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RuleEngineService {

    private final RuleRepository ruleRepository;
    private final AlertRepository alertRepository;
    private final AlertService alertService;
    private final EventService eventService;

    /**
     * Evaluate all rules for a telemetry record and trigger alerts if conditions are met
     */
    public void evaluateRules(TelemetryRecord telemetryRecord) {
        Device device = telemetryRecord.getDevice();
        List<Rule> enabledRules = ruleRepository.findByDeviceExternalIdAndEnabledTrue(device.getExternalId());

        if (enabledRules.isEmpty()) {
            return;
        }

        // Extract telemetry values into a map for easy access
        Map<String, BigDecimal> telemetryValues = Map.of(
                "kwConsumption", telemetryRecord.getKwConsumption() != null ? telemetryRecord.getKwConsumption() : BigDecimal.ZERO,
                "voltage", telemetryRecord.getVoltage() != null ? telemetryRecord.getVoltage() : BigDecimal.ZERO,
                "current", telemetryRecord.getCurrent() != null ? telemetryRecord.getCurrent() : BigDecimal.ZERO,
                "powerFactor", telemetryRecord.getPowerFactor() != null ? telemetryRecord.getPowerFactor() : BigDecimal.ZERO,
                "frequency", telemetryRecord.getFrequency() != null ? telemetryRecord.getFrequency() : BigDecimal.ZERO
        );

        for (Rule rule : enabledRules) {
            evaluateRule(rule, telemetryValues, telemetryRecord);
        }
    }

    private void evaluateRule(Rule rule, Map<String, BigDecimal> telemetryValues, TelemetryRecord telemetryRecord) {
        // Guard against null variable name in rule configuration
        String variableName = rule.getVariable();
        if (variableName == null || variableName.isEmpty()) {
            log.warn("Rule '{}' has null or empty variable name, skipping evaluation", rule.getName());
            return;
        }

        BigDecimal actualValue = telemetryValues.get(variableName);
        if (actualValue == null) {
            log.debug("No value found for variable '{}' in telemetry record", variableName);
            return;
        }

        boolean conditionMet = rule.getOperator().evaluate(actualValue, rule.getThreshold());

        if (conditionMet) {
            // Check if we've already triggered this rule recently (within 5 minutes) to prevent spam
            Instant recentThreshold = Instant.now().minus(5, ChronoUnit.MINUTES);
            List<Alert> recentAlerts = alertRepository.findRecentAlertsForRule(rule.getId(), recentThreshold);

            if (recentAlerts.isEmpty()) {
                triggerAlert(rule, actualValue, telemetryRecord);
            } else {
                log.debug("Rule '{}' recently triggered, skipping to prevent spam", rule.getName());
            }
        }
    }

    private void triggerAlert(Rule rule, BigDecimal triggeredValue, TelemetryRecord telemetryRecord) {
        AlertSeverity severity = determineSeverity(rule, triggeredValue);

        String message = String.format(
                "Rule '%s' triggered: %s %s %s (actual: %s)",
                rule.getName(),
                rule.getVariable(),
                getOperatorSymbol(rule.getOperator().name()),
                rule.getThreshold(),
                triggeredValue
        );

        Alert alert = Alert.builder()
                .rule(rule)
                .device(telemetryRecord.getDevice())
                .message(message)
                .severity(severity)
                .triggeredValue(triggeredValue)
                .build();

        alert = alertRepository.save(alert);
        log.info("Alert triggered: {} for device {}", message, telemetryRecord.getDevice().getExternalId());

        // Emit rule triggered event
        if (rule.getOrganization() != null) {
            Event.EventSeverity eventSeverity = mapAlertSeverityToEventSeverity(severity);
            eventService.emitRuleEvent(
                rule.getOrganization(),
                rule.getId(),
                rule.getName(),
                Event.EventType.RULE_TRIGGERED,
                eventSeverity,
                message
            );

            // Also emit alert created event
            eventService.emitAlertEvent(
                rule.getOrganization(),
                alert.getId(),
                telemetryRecord.getDevice().getExternalId(),
                Event.EventType.ALERT_CREATED,
                eventSeverity,
                message
            );
        }

        // Send notification via AlertService
        alertService.sendAlertNotification(alert);
    }

    private Event.EventSeverity mapAlertSeverityToEventSeverity(AlertSeverity alertSeverity) {
        return switch (alertSeverity) {
            case CRITICAL -> Event.EventSeverity.CRITICAL;
            case HIGH -> Event.EventSeverity.ERROR;
            case MEDIUM -> Event.EventSeverity.WARNING;
            case LOW -> Event.EventSeverity.INFO;
        };
    }

    private AlertSeverity determineSeverity(Rule rule, BigDecimal triggeredValue) {
        // Simple severity determination based on how far the value deviates from threshold
        BigDecimal deviation = triggeredValue.subtract(rule.getThreshold()).abs();
        BigDecimal threshold = rule.getThreshold().abs();

        if (threshold.compareTo(BigDecimal.ZERO) == 0) {
            return AlertSeverity.MEDIUM;
        }

        // Calculate percentage deviation
        BigDecimal percentageDeviation = deviation.divide(threshold, 4, RoundingMode.HALF_UP);

        if (percentageDeviation.compareTo(BigDecimal.valueOf(2.0)) > 0) {
            return AlertSeverity.CRITICAL;
        } else if (percentageDeviation.compareTo(BigDecimal.valueOf(1.0)) > 0) {
            return AlertSeverity.HIGH;
        } else if (percentageDeviation.compareTo(BigDecimal.valueOf(0.5)) > 0) {
            return AlertSeverity.MEDIUM;
        } else {
            return AlertSeverity.LOW;
        }
    }

    private String getOperatorSymbol(String operator) {
        return switch (operator) {
            case "GT" -> ">";
            case "GTE" -> "≥";
            case "LT" -> "<";
            case "LTE" -> "≤";
            case "EQ" -> "=";
            default -> operator;
        };
    }
}