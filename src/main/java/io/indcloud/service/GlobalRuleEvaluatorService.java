package io.indcloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.*;
import io.indcloud.repository.GlobalAlertRepository;
import io.indcloud.repository.GlobalRuleRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for evaluating global/fleet-wide rules on a scheduled basis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalRuleEvaluatorService {

    private final GlobalRuleRepository globalRuleRepository;
    private final GlobalAlertRepository globalAlertRepository;
    private final FleetAggregatorService fleetAggregatorService;
    private final EventService eventService;
    private final AlertService alertService;

    /**
     * Scheduled task to evaluate all enabled global rules
     * Runs every minute
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    @Transactional
    public void evaluateGlobalRules() {
        log.debug("Starting scheduled global rules evaluation");

        try {
            List<GlobalRule> allEnabledRules = globalRuleRepository.findRulesDueForEvaluation(Instant.now());

            if (allEnabledRules.isEmpty()) {
                log.debug("No global rules due for evaluation");
                return;
            }

            log.info("Evaluating {} global rules", allEnabledRules.size());

            for (GlobalRule rule : allEnabledRules) {
                try {
                    evaluateRule(rule);
                } catch (Exception e) {
                    log.error("Error evaluating global rule {}: {}", rule.getId(), e.getMessage(), e);
                }
            }

            log.debug("Completed global rules evaluation");
        } catch (Exception e) {
            log.error("Error in scheduled global rules evaluation: {}", e.getMessage(), e);
        }
    }

    /**
     * Evaluate a specific global rule
     */
    @Transactional
    public void evaluateRule(GlobalRule rule) {
        if (!rule.getEnabled()) {
            log.debug("Global rule {} is disabled, skipping", rule.getId());
            return;
        }

        // Check if rule should be evaluated based on interval
        if (!shouldEvaluate(rule)) {
            log.debug("Global rule {} not due for evaluation yet", rule.getId());
            return;
        }

        log.debug("Evaluating global rule: {} ({})", rule.getName(), rule.getId());

        try {
            // Calculate aggregation
            FleetAggregatorService.AggregationResult result = fleetAggregatorService.calculateAggregation(rule);

            // Update last evaluated timestamp
            rule.setLastEvaluatedAt(Instant.now());
            globalRuleRepository.save(rule);

            // Evaluate condition
            boolean conditionMet = rule.getOperator().evaluate(result.getValue(), rule.getThreshold());

            if (conditionMet) {
                // Check cooldown period to prevent alert spam
                if (isInCooldown(rule)) {
                    log.debug("Global rule {} is in cooldown period, skipping alert", rule.getName());
                    return;
                }

                triggerGlobalAlert(rule, result);
            } else {
                log.debug("Global rule {} condition not met: {} {} {} (actual: {})",
                        rule.getName(),
                        rule.getAggregationFunction(),
                        getOperatorSymbol(rule.getOperator().name()),
                        rule.getThreshold(),
                        result.getValue());
            }
        } catch (Exception e) {
            log.error("Error evaluating global rule {}: {}", rule.getId(), e.getMessage(), e);
        }
    }

    /**
     * Check if rule should be evaluated based on its interval
     */
    private boolean shouldEvaluate(GlobalRule rule) {
        if (rule.getLastEvaluatedAt() == null) {
            return true; // Never evaluated, should evaluate
        }

        long intervalMinutes = parseIntervalToMinutes(rule.getEvaluationInterval());
        Instant nextEvaluation = rule.getLastEvaluatedAt().plus(intervalMinutes, ChronoUnit.MINUTES);

        return Instant.now().isAfter(nextEvaluation);
    }

    /**
     * Check if rule is in cooldown period
     */
    private boolean isInCooldown(GlobalRule rule) {
        if (rule.getLastTriggeredAt() == null) {
            return false;
        }

        Instant cooldownEnd = rule.getLastTriggeredAt().plus(rule.getCooldownMinutes(), ChronoUnit.MINUTES);
        return Instant.now().isBefore(cooldownEnd);
    }

    /**
     * Trigger a global alert
     */
    @Transactional
    protected void triggerGlobalAlert(GlobalRule rule, FleetAggregatorService.AggregationResult result) {
        AlertSeverity severity = determineSeverity(rule, result.getValue());

        String message = String.format(
                "Global rule '%s' triggered: %s(%s) %s %s (actual: %s, %d devices in scope)",
                rule.getName(),
                rule.getAggregationFunction(),
                rule.getAggregationVariable() != null ? rule.getAggregationVariable() : "",
                getOperatorSymbol(rule.getOperator().name()),
                rule.getThreshold(),
                result.getValue(),
                result.getDeviceCount()
        );

        GlobalAlert alert = GlobalAlert.builder()
                .globalRule(rule)
                .organization(rule.getOrganization())
                .message(message)
                .severity(severity)
                .triggeredValue(result.getValue())
                .deviceCount(result.getDeviceCount())
                .affectedDevices(result.getAffectedDevices())
                .triggeredAt(Instant.now())
                .build();

        alert = globalAlertRepository.save(alert);

        // Update rule's last triggered timestamp
        rule.setLastTriggeredAt(Instant.now());
        globalRuleRepository.save(rule);

        log.info("Global alert triggered: {} for rule {}", message, rule.getName());

        // Emit events
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

            eventService.emitAlertEvent(
                    rule.getOrganization(),
                    alert.getId(),
                    "FLEET-WIDE",
                    Event.EventType.ALERT_CREATED,
                    eventSeverity,
                    message
            );
        }

        // Send notifications if configured
        if (rule.getSendSms() && rule.getSmsRecipients() != null && rule.getSmsRecipients().length > 0) {
            sendGlobalAlertNotification(alert, rule);
        }
    }

    /**
     * Send notification for global alert
     */
    private void sendGlobalAlertNotification(GlobalAlert alert, GlobalRule rule) {
        try {
            // Use existing AlertService SMS capability
            for (String phoneNumber : rule.getSmsRecipients()) {
                alertService.sendSmsNotification(
                        phoneNumber,
                        alert.getMessage(),
                        rule.getOrganization().getId()
                );
            }
        } catch (Exception e) {
            log.error("Error sending global alert notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Determine alert severity based on deviation from threshold
     */
    private AlertSeverity determineSeverity(GlobalRule rule, BigDecimal triggeredValue) {
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

    /**
     * Parse evaluation interval string to minutes
     * Supports formats: "5m", "1h", "30s", "15", "every_15_minutes", "every_1_hour"
     */
    private long parseIntervalToMinutes(String interval) {
        if (interval == null || interval.isBlank()) {
            return 5; // Default to 5 minutes
        }

        try {
            interval = interval.toLowerCase().trim();

            // Handle "every_X_minutes" and "every_X_hours" format
            if (interval.startsWith("every_")) {
                String[] parts = interval.split("_");
                if (parts.length >= 3) {
                    long value = Long.parseLong(parts[1]);
                    String unit = parts[2];
                    if (unit.startsWith("minute")) {
                        return value;
                    } else if (unit.startsWith("hour")) {
                        return value * 60;
                    } else if (unit.startsWith("second")) {
                        return Math.max(1, value / 60);
                    }
                }
            }

            // Handle standard unit suffixes: "5m", "1h", "30s"
            if (interval.endsWith("m")) {
                return Long.parseLong(interval.substring(0, interval.length() - 1));
            } else if (interval.endsWith("h")) {
                return Long.parseLong(interval.substring(0, interval.length() - 1)) * 60;
            } else if (interval.endsWith("s")) {
                return Math.max(1, Long.parseLong(interval.substring(0, interval.length() - 1)) / 60);
            } else {
                return Long.parseLong(interval); // Assume minutes if no unit
            }
        } catch (Exception e) {
            log.error("Error parsing interval '{}', defaulting to 5 minutes", interval, e);
            return 5;
        }
    }

    private Event.EventSeverity mapAlertSeverityToEventSeverity(AlertSeverity alertSeverity) {
        return switch (alertSeverity) {
            case CRITICAL -> Event.EventSeverity.CRITICAL;
            case HIGH -> Event.EventSeverity.ERROR;
            case MEDIUM -> Event.EventSeverity.WARNING;
            case LOW -> Event.EventSeverity.INFO;
        };
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
