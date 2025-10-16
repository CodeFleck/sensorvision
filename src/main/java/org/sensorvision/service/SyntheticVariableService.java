package org.sensorvision.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.SyntheticVariable;
import org.sensorvision.model.SyntheticVariableValue;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.repository.SyntheticVariableRepository;
import org.sensorvision.repository.SyntheticVariableValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SyntheticVariableService {

    private final SyntheticVariableRepository syntheticVariableRepository;
    private final SyntheticVariableValueRepository syntheticVariableValueRepository;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\b(kwConsumption|voltage|current|powerFactor|frequency)\\b");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
    private static final Pattern OPERATOR_PATTERN = Pattern.compile("[+\\-*/()]");

    /**
     * Calculate synthetic variables for a telemetry record
     */
    public void calculateSyntheticVariables(TelemetryRecord telemetryRecord) {
        List<SyntheticVariable> enabledVariables = syntheticVariableRepository
                .findByDeviceExternalIdAndEnabledTrue(telemetryRecord.getDevice().getExternalId());

        if (enabledVariables.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> telemetryValues = extractTelemetryValues(telemetryRecord);

        for (SyntheticVariable syntheticVariable : enabledVariables) {
            try {
                BigDecimal calculatedValue = evaluateExpression(syntheticVariable.getExpression(), telemetryValues);

                if (calculatedValue != null) {
                    SyntheticVariableValue value = SyntheticVariableValue.builder()
                            .syntheticVariable(syntheticVariable)
                            .telemetryRecord(telemetryRecord)
                            .calculatedValue(calculatedValue)
                            .timestamp(telemetryRecord.getTimestamp())
                            .build();

                    syntheticVariableValueRepository.save(value);
                    log.debug("Calculated synthetic variable '{}' = {} for device {}",
                            syntheticVariable.getName(),
                            calculatedValue,
                            telemetryRecord.getDevice().getExternalId());
                }
            } catch (Exception e) {
                log.error("Failed to calculate synthetic variable '{}' for device {}: {}",
                        syntheticVariable.getName(),
                        telemetryRecord.getDevice().getExternalId(),
                        e.getMessage());
            }
        }
    }

    /**
     * Evaluate a mathematical expression with telemetry variables
     */
    private BigDecimal evaluateExpression(String expression, Map<String, BigDecimal> values) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        // Replace variables with their values
        String evaluatedExpression = expression;
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(expression);

        while (variableMatcher.find()) {
            String variable = variableMatcher.group();
            BigDecimal value = values.get(variable);

            if (value == null) {
                log.debug("Variable '{}' not found in telemetry data", variable);
                return null;
            }

            // Use word boundary regex to replace only whole words and escape variable name
            evaluatedExpression = evaluatedExpression.replaceAll(
                    "\\b" + Pattern.quote(variable) + "\\b",
                    Matcher.quoteReplacement(value.toString())
            );
        }

        // Evaluate the mathematical expression
        return evaluateSimpleExpression(evaluatedExpression);
    }

    /**
     * Simple expression evaluator for basic arithmetic operations
     * Supports +, -, *, /, and parentheses
     */
    private BigDecimal evaluateSimpleExpression(String expression) {
        try {
            // Remove whitespace
            expression = expression.replaceAll("\\s+", "");

            // Validate expression contains only numbers, operators, and parentheses
            if (!expression.matches("^[0-9+\\-*/.()]+$")) {
                throw new IllegalArgumentException("Invalid characters in expression");
            }

            // This is a simplified evaluator - in production, you might want to use
            // a proper expression parser like ANTLR or a library like exp4j
            return evaluateWithPrecedence(expression);
        } catch (Exception e) {
            log.error("Failed to evaluate expression '{}': {}", expression, e.getMessage());
            return null;
        }
    }

    private BigDecimal evaluateWithPrecedence(String expression) {
        // Handle parentheses first
        while (expression.contains("(")) {
            int start = expression.lastIndexOf("(");
            int end = expression.indexOf(")", start);

            if (end == -1) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }

            String subExpression = expression.substring(start + 1, end);
            BigDecimal result = evaluateWithPrecedence(subExpression);

            expression = expression.substring(0, start) + result.toString() + expression.substring(end + 1);
        }

        // Handle multiplication and division first (left to right)
        expression = handleOperations(expression, "[*/]");

        // Handle addition and subtraction (left to right)
        expression = handleOperations(expression, "[+\\-]");

        return new BigDecimal(expression);
    }

    private String handleOperations(String expression, String operatorRegex) {
        Pattern pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([" + operatorRegex.replace("[", "").replace("]", "") + "])\\s*(-?\\d+(?:\\.\\d+)?)");

        while (true) {
            Matcher matcher = pattern.matcher(expression);
            if (!matcher.find()) {
                break;
            }

            BigDecimal left = new BigDecimal(matcher.group(1));
            String operator = matcher.group(2);
            BigDecimal right = new BigDecimal(matcher.group(3));

            BigDecimal result = switch (operator) {
                case "+" -> left.add(right);
                case "-" -> left.subtract(right);
                case "*" -> left.multiply(right);
                case "/" -> {
                    if (right.compareTo(BigDecimal.ZERO) == 0) {
                        log.error("Division by zero in synthetic variable calculation");
                        throw new ArithmeticException("Division by zero in expression");
                    }
                    yield left.divide(right, new MathContext(10, RoundingMode.HALF_UP));
                }
                default -> throw new IllegalArgumentException("Unknown operator: " + operator);
            };

            expression = expression.substring(0, matcher.start()) + result.toString() + expression.substring(matcher.end());
        }

        return expression;
    }

    private Map<String, BigDecimal> extractTelemetryValues(TelemetryRecord record) {
        return Map.of(
                "kwConsumption", record.getKwConsumption() != null ? record.getKwConsumption() : BigDecimal.ZERO,
                "voltage", record.getVoltage() != null ? record.getVoltage() : BigDecimal.ZERO,
                "current", record.getCurrent() != null ? record.getCurrent() : BigDecimal.ZERO,
                "powerFactor", record.getPowerFactor() != null ? record.getPowerFactor() : BigDecimal.ZERO,
                "frequency", record.getFrequency() != null ? record.getFrequency() : BigDecimal.ZERO
        );
    }
}