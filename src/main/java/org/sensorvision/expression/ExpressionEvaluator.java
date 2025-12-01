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

    // Thread-local context for statistical functions
    private static final ThreadLocal<StatisticalFunctionContext> contextHolder = new ThreadLocal<>();

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
     * Evaluate an expression with variable substitution and statistical context.
     * This overload supports time-series statistical functions.
     *
     * @param expression The expression to evaluate
     * @param variables Map of variable names to values
     * @param context Statistical function context (device, time, repository)
     * @return The evaluated result
     */
    public BigDecimal evaluate(String expression, Map<String, BigDecimal> variables, StatisticalFunctionContext context) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        try {
            // Set context in thread-local for function access
            contextHolder.set(context);

            // Substitute variables with their values
            String processedExpression = substituteVariables(expression, variables);

            // Parse and evaluate the expression
            return evaluateExpression(processedExpression);

        } catch (Exception e) {
            log.error("Failed to evaluate expression '{}': {}", expression, e.getMessage());
            throw new ExpressionEvaluationException("Expression evaluation failed: " + e.getMessage(), e);
        } finally {
            // Always clear context to prevent leaks
            contextHolder.remove();
        }
    }

    /**
     * Substitute variables in expression with their numeric values.
     * Preserves string literals (content inside quotes) to avoid replacing
     * variable names that are meant to be passed as strings to functions.
     */
    private String substituteVariables(String expression, Map<String, BigDecimal> variables) {
        // Extract string literals first and replace with placeholders
        List<String> stringLiterals = new ArrayList<>();
        StringBuilder withPlaceholders = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        StringBuilder currentLiteral = new StringBuilder();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == '"' && !inSingleQuote) {
                if (inDoubleQuote) {
                    // End of double-quoted string
                    currentLiteral.append(c);
                    stringLiterals.add(currentLiteral.toString());
                    withPlaceholders.append("__STRING_LITERAL_").append(stringLiterals.size() - 1).append("__");
                    currentLiteral = new StringBuilder();
                    inDoubleQuote = false;
                } else {
                    // Start of double-quoted string
                    inDoubleQuote = true;
                    currentLiteral.append(c);
                }
            } else if (c == '\'' && !inDoubleQuote) {
                if (inSingleQuote) {
                    // End of single-quoted string
                    currentLiteral.append(c);
                    stringLiterals.add(currentLiteral.toString());
                    withPlaceholders.append("__STRING_LITERAL_").append(stringLiterals.size() - 1).append("__");
                    currentLiteral = new StringBuilder();
                    inSingleQuote = false;
                } else {
                    // Start of single-quoted string
                    inSingleQuote = true;
                    currentLiteral.append(c);
                }
            } else if (inSingleQuote || inDoubleQuote) {
                currentLiteral.append(c);
            } else {
                withPlaceholders.append(c);
            }
        }

        // Perform variable substitution on the expression without string literals
        String result = withPlaceholders.toString();

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

        // Restore string literals
        for (int i = 0; i < stringLiterals.size(); i++) {
            result = result.replace("__STRING_LITERAL_" + i + "__", stringLiterals.get(i));
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
            List<Object> args = parseArguments(argsString);

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
     * Now supports both numeric expressions and string literals (for statistical functions).
     */
    private List<Object> parseArguments(String argsString) {
        if (argsString.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> args = new ArrayList<>();
        int depth = 0;
        StringBuilder currentArg = new StringBuilder();

        for (char c : argsString.toCharArray()) {
            if (c == ',' && depth == 0) {
                args.add(parseArgument(currentArg.toString().trim()));
                currentArg = new StringBuilder();
            } else {
                if (c == '(') depth++;
                else if (c == ')') depth--;
                currentArg.append(c);
            }
        }

        if (currentArg.length() > 0) {
            args.add(parseArgument(currentArg.toString().trim()));
        }

        return args;
    }

    /**
     * Parse a single argument - can be a string literal or numeric expression.
     */
    private Object parseArgument(String arg) {
        arg = arg.trim();

        // Check if it's a string literal (enclosed in quotes)
        if ((arg.startsWith("\"") && arg.endsWith("\"")) ||
            (arg.startsWith("'") && arg.endsWith("'"))) {
            // Remove quotes and return as string
            return arg.substring(1, arg.length() - 1);
        }

        // Otherwise, evaluate as expression
        return evaluateExpression(arg);
    }

    /**
     * Evaluate a function call.
     */
    private BigDecimal evaluateFunction(String functionName, List<Object> args) {
        ExpressionFunction function = functionRegistry.getFunction(functionName);
        if (function == null) {
            throw new ExpressionEvaluationException("Unknown function: " + functionName);
        }

        // Check if function is context-aware (statistical function)
        if (function instanceof ContextualExpressionFunction) {
            StatisticalFunctionContext context = contextHolder.get();
            return ((ContextualExpressionFunction) function).evaluateWithContext(context, args.toArray());
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
