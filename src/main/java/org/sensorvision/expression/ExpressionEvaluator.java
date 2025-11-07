package org.sensorvision.expression;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced expression evaluator supporting:
 * - Basic arithmetic: +, -, *, /, ()
 * - Comparison operators: >, <, >=, <=, ==, !=
 * - Math functions: sqrt, pow, abs, log, exp, sin, cos, round, etc.
 * - Logic functions: if, and, or, not
 * - Variable substitution
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpressionEvaluator {

    private final FunctionRegistry functionRegistry;

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    // Patterns for tokenization
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /**
     * Evaluate an expression with variable substitution.
     *
     * @param expression The expression to evaluate
     * @param variables Map of variable names to values
     * @return The evaluated result
     */
    public BigDecimal evaluate(String expression, Map<String, BigDecimal> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        try {
            // Substitute variables with their values
            String processedExpression = substituteVariables(expression, variables);

            // Parse and evaluate the expression
            return evaluateExpression(processedExpression);

        } catch (Exception e) {
            log.error("Failed to evaluate expression '{}': {}", expression, e.getMessage());
            throw new ExpressionEvaluationException("Expression evaluation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Substitute variables in expression with their numeric values.
     */
    private String substituteVariables(String expression, Map<String, BigDecimal> variables) {
        String result = expression;

        // Sort variables by length (descending) to avoid partial replacements
        List<String> variableNames = new ArrayList<>(variables.keySet());
        variableNames.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String varName : variableNames) {
            BigDecimal value = variables.get(varName);
            if (value != null) {
                // Replace whole words only (use word boundaries)
                result = result.replaceAll("\\b" + Pattern.quote(varName) + "\\b",
                        Matcher.quoteReplacement(value.toPlainString()));
            }
        }

        return result;
    }

    /**
     * Parse and evaluate an expression (after variable substitution).
     */
    private BigDecimal evaluateExpression(String expression) {
        expression = expression.trim();

        // Handle function calls (only if there's an identifier before the parenthesis)
        if (FUNCTION_PATTERN.matcher(expression).find()) {
            return evaluateWithFunctions(expression);
        }

        // Handle comparison operators (must be before arithmetic to handle grouped comparisons)
        if (containsComparisonOperator(expression)) {
            return evaluateComparison(expression);
        }

        // Handle arithmetic (includes parentheses for grouping)
        return evaluateArithmetic(expression);
    }

    /**
     * Evaluate expressions containing function calls.
     */
    private BigDecimal evaluateWithFunctions(String expression) {
        // Process innermost function calls first
        while (true) {
            Matcher matcher = FUNCTION_PATTERN.matcher(expression);
            int lastFunctionStart = -1;
            int lastFunctionNameEnd = -1;
            String lastFunctionName = null;

            // Find the rightmost (innermost) function call
            while (matcher.find()) {
                lastFunctionStart = matcher.start();
                lastFunctionNameEnd = matcher.end() - 1; // Exclude the '('
                lastFunctionName = matcher.group(1);
            }

            if (lastFunctionStart == -1) {
                // No more functions, evaluate the rest
                break;
            }

            // Find matching closing parenthesis
            int parenDepth = 1;
            int closingParen = -1;
            for (int i = lastFunctionNameEnd + 1; i < expression.length(); i++) {
                if (expression.charAt(i) == '(') parenDepth++;
                else if (expression.charAt(i) == ')') {
                    parenDepth--;
                    if (parenDepth == 0) {
                        closingParen = i;
                        break;
                    }
                }
            }

            if (closingParen == -1) {
                throw new ExpressionEvaluationException("Mismatched parentheses in function call");
            }

            // Extract function arguments
            String argsString = expression.substring(lastFunctionNameEnd + 1, closingParen);
            List<BigDecimal> args = parseArguments(argsString);

            // Evaluate function
            BigDecimal result = evaluateFunction(lastFunctionName, args);

            // Replace function call with result
            expression = expression.substring(0, lastFunctionStart) +
                    result.toPlainString() +
                    expression.substring(closingParen + 1);
        }

        // Evaluate the final expression (should be arithmetic now)
        return evaluateArithmetic(expression);
    }

    /**
     * Parse comma-separated function arguments.
     */
    private List<BigDecimal> parseArguments(String argsString) {
        if (argsString.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<BigDecimal> args = new ArrayList<>();
        int depth = 0;
        StringBuilder currentArg = new StringBuilder();

        for (char c : argsString.toCharArray()) {
            if (c == ',' && depth == 0) {
                args.add(evaluateExpression(currentArg.toString().trim()));
                currentArg = new StringBuilder();
            } else {
                if (c == '(') depth++;
                else if (c == ')') depth--;
                currentArg.append(c);
            }
        }

        if (currentArg.length() > 0) {
            args.add(evaluateExpression(currentArg.toString().trim()));
        }

        return args;
    }

    /**
     * Evaluate a function call.
     */
    private BigDecimal evaluateFunction(String functionName, List<BigDecimal> args) {
        ExpressionFunction function = functionRegistry.getFunction(functionName);
        if (function == null) {
            throw new ExpressionEvaluationException("Unknown function: " + functionName);
        }

        return function.evaluate(args.toArray());
    }

    /**
     * Check if expression contains comparison operators.
     */
    private boolean containsComparisonOperator(String expression) {
        return expression.contains(">=") || expression.contains("<=") ||
               expression.contains("==") || expression.contains("!=") ||
               expression.contains(">") || expression.contains("<");
    }

    /**
     * Evaluate comparison operators (returns 1 for true, 0 for false).
     */
    private BigDecimal evaluateComparison(String expression) {
        // Handle operators in order of precedence
        String[] operators = {">=", "<=", "==", "!=", ">", "<"};

        for (String op : operators) {
            int index = expression.indexOf(op);
            if (index != -1) {
                String left = expression.substring(0, index).trim();
                String right = expression.substring(index + op.length()).trim();

                BigDecimal leftValue = evaluateArithmetic(left);
                BigDecimal rightValue = evaluateArithmetic(right);

                boolean result = switch (op) {
                    case ">" -> leftValue.compareTo(rightValue) > 0;
                    case "<" -> leftValue.compareTo(rightValue) < 0;
                    case ">=" -> leftValue.compareTo(rightValue) >= 0;
                    case "<=" -> leftValue.compareTo(rightValue) <= 0;
                    case "==" -> leftValue.compareTo(rightValue) == 0;
                    case "!=" -> leftValue.compareTo(rightValue) != 0;
                    default -> throw new ExpressionEvaluationException("Unknown operator: " + op);
                };

                return result ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        }

        return evaluateArithmetic(expression);
    }

    /**
     * Evaluate arithmetic expression (after all functions and comparisons are resolved).
     */
    private BigDecimal evaluateArithmetic(String expression) {
        expression = expression.trim();

        // Handle parentheses first
        while (expression.contains("(")) {
            int start = expression.lastIndexOf("(");
            int end = expression.indexOf(")", start);

            if (end == -1) {
                throw new ExpressionEvaluationException("Mismatched parentheses");
            }

            String subExpression = expression.substring(start + 1, end);
            BigDecimal result = evaluateArithmetic(subExpression);

            expression = expression.substring(0, start) + result.toPlainString() + expression.substring(end + 1);
        }

        // Handle multiplication and division (left to right)
        expression = handleArithmeticOperations(expression, "[*/]");

        // Handle addition and subtraction (left to right)
        expression = handleArithmeticOperations(expression, "[+\\-]");

        return new BigDecimal(expression.trim());
    }

    /**
     * Handle arithmetic operations with left-to-right precedence.
     */
    private String handleArithmeticOperations(String expression, String operatorRegex) {
        Pattern pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([" +
                operatorRegex.replace("[", "").replace("]", "") + "])\\s*(-?\\d+(?:\\.\\d+)?)");

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
                        throw new ArithmeticException("Division by zero");
                    }
                    yield left.divide(right, MC);
                }
                default -> throw new ExpressionEvaluationException("Unknown operator: " + operator);
            };

            expression = expression.substring(0, matcher.start()) +
                    result.toPlainString() +
                    expression.substring(matcher.end());
        }

        return expression;
    }

    /**
     * Custom exception for expression evaluation errors.
     */
    public static class ExpressionEvaluationException extends RuntimeException {
        public ExpressionEvaluationException(String message) {
            super(message);
        }

        public ExpressionEvaluationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
