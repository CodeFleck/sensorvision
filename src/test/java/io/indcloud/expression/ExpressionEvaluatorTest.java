package io.indcloud.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.indcloud.expression.ExpressionEvaluator.ExpressionEvaluationException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for the advanced expression evaluator.
 */
class ExpressionEvaluatorTest {

    private ExpressionEvaluator evaluator;
    private Map<String, BigDecimal> variables;

    @BeforeEach
    void setUp() {
        evaluator = new ExpressionEvaluator(new FunctionRegistry());
        variables = new HashMap<>();
        variables.put("kwConsumption", new BigDecimal("100.0"));
        variables.put("voltage", new BigDecimal("220.0"));
        variables.put("current", new BigDecimal("5.0"));
        variables.put("temperature", new BigDecimal("75.5"));
    }

    // ===== BASIC ARITHMETIC TESTS =====

    @Test
    void shouldEvaluateSimpleAddition() {
        BigDecimal result = evaluator.evaluate("10 + 20", Map.of());
        assertThat(result).isEqualByComparingTo("30");
    }

    @Test
    void shouldEvaluateSimpleSubtraction() {
        BigDecimal result = evaluator.evaluate("100 - 30", Map.of());
        assertThat(result).isEqualByComparingTo("70");
    }

    @Test
    void shouldEvaluateMultiplication() {
        BigDecimal result = evaluator.evaluate("5 * 6", Map.of());
        assertThat(result).isEqualByComparingTo("30");
    }

    @Test
    void shouldEvaluateDivision() {
        BigDecimal result = evaluator.evaluate("20 / 4", Map.of());
        assertThat(result).isEqualByComparingTo("5");
    }

    @Test
    void shouldHandleParentheses() {
        BigDecimal result = evaluator.evaluate("(10 + 20) * 2", Map.of());
        assertThat(result).isEqualByComparingTo("60");
    }

    @Test
    void shouldRespectOperatorPrecedence() {
        BigDecimal result = evaluator.evaluate("10 + 20 * 2", Map.of());
        assertThat(result).isEqualByComparingTo("50");
    }

    // ===== VARIABLE SUBSTITUTION TESTS =====

    @Test
    void shouldSubstituteVariables() {
        BigDecimal result = evaluator.evaluate("voltage * current", variables);
        assertThat(result).isEqualByComparingTo("1100"); // 220 * 5
    }

    @Test
    void shouldHandleComplexExpression() {
        BigDecimal result = evaluator.evaluate("kwConsumption * voltage / current", variables);
        assertThat(result).isEqualByComparingTo("4400"); // 100 * 220 / 5
    }

    // ===== MATH FUNCTION TESTS =====

    @Test
    void shouldEvaluateSqrt() {
        BigDecimal result = evaluator.evaluate("sqrt(16)", Map.of());
        assertThat(result).isEqualByComparingTo("4");
    }

    @Test
    void shouldEvaluatePow() {
        BigDecimal result = evaluator.evaluate("pow(2, 3)", Map.of());
        assertThat(result).isEqualByComparingTo("8");
    }

    @Test
    void shouldEvaluateAbs() {
        BigDecimal result = evaluator.evaluate("abs(-42)", Map.of());
        assertThat(result).isEqualByComparingTo("42");
    }

    @Test
    void shouldEvaluateRound() {
        BigDecimal result = evaluator.evaluate("round(3.7)", Map.of());
        assertThat(result).isEqualByComparingTo("4");
    }

    @Test
    void shouldEvaluateFloor() {
        BigDecimal result = evaluator.evaluate("floor(3.9)", Map.of());
        assertThat(result).isEqualByComparingTo("3");
    }

    @Test
    void shouldEvaluateCeil() {
        BigDecimal result = evaluator.evaluate("ceil(3.1)", Map.of());
        assertThat(result).isEqualByComparingTo("4");
    }

    @Test
    void shouldEvaluateMin() {
        BigDecimal result = evaluator.evaluate("min(10, 5, 20)", Map.of());
        assertThat(result).isEqualByComparingTo("5");
    }

    @Test
    void shouldEvaluateMax() {
        BigDecimal result = evaluator.evaluate("max(10, 5, 20)", Map.of());
        assertThat(result).isEqualByComparingTo("20");
    }

    @Test
    void shouldEvaluateLog() {
        BigDecimal result = evaluator.evaluate("log(2.71828)", Map.of());
        assertThat(result.doubleValue()).isCloseTo(1.0, within(0.001));
    }

    @Test
    void shouldEvaluateExp() {
        BigDecimal result = evaluator.evaluate("exp(1)", Map.of());
        assertThat(result.doubleValue()).isCloseTo(2.71828, within(0.001));
    }

    // ===== TRIGONOMETRIC FUNCTION TESTS =====

    @Test
    void shouldEvaluateSin() {
        BigDecimal result = evaluator.evaluate("sin(0)", Map.of());
        assertThat(result.doubleValue()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void shouldEvaluateCos() {
        BigDecimal result = evaluator.evaluate("cos(0)", Map.of());
        assertThat(result.doubleValue()).isCloseTo(1.0, within(0.001));
    }

    // ===== COMPARISON OPERATOR TESTS =====

    @Test
    void shouldEvaluateGreaterThan() {
        BigDecimal result = evaluator.evaluate("10 > 5", Map.of());
        assertThat(result).isEqualByComparingTo("1"); // true = 1
    }

    @Test
    void shouldEvaluateLessThan() {
        BigDecimal result = evaluator.evaluate("3 < 10", Map.of());
        assertThat(result).isEqualByComparingTo("1"); // true = 1
    }

    @Test
    void shouldEvaluateGreaterThanOrEqual() {
        BigDecimal result = evaluator.evaluate("10 >= 10", Map.of());
        assertThat(result).isEqualByComparingTo("1"); // true = 1
    }

    @Test
    void shouldEvaluateLessThanOrEqual() {
        BigDecimal result = evaluator.evaluate("5 <= 10", Map.of());
        assertThat(result).isEqualByComparingTo("1"); // true = 1
    }

    @Test
    void shouldEvaluateEquals() {
        BigDecimal result = evaluator.evaluate("5 == 5", Map.of());
        assertThat(result).isEqualByComparingTo("1"); // true = 1
    }

    @Test
    void shouldEvaluateNotEquals() {
        BigDecimal result = evaluator.evaluate("5 != 10", Map.of());
        assertThat(result).isEqualByComparingTo("1"); // true = 1
    }

    // ===== CONDITIONAL LOGIC TESTS =====

    @Test
    void shouldEvaluateIfTrue() {
        BigDecimal result = evaluator.evaluate("if(1, 100, 200)", Map.of());
        assertThat(result).isEqualByComparingTo("100");
    }

    @Test
    void shouldEvaluateIfFalse() {
        BigDecimal result = evaluator.evaluate("if(0, 100, 200)", Map.of());
        assertThat(result).isEqualByComparingTo("200");
    }

    @Test
    void shouldEvaluateIfWithComparison() {
        BigDecimal result = evaluator.evaluate("if(10 > 5, 1, 0)", Map.of());
        assertThat(result).isEqualByComparingTo("1");
    }

    // ===== COMPLEX EXPRESSION TESTS =====

    @Test
    void shouldEvaluateNestedFunctions() {
        BigDecimal result = evaluator.evaluate("round(sqrt(pow(3, 2) + pow(4, 2)))", Map.of());
        assertThat(result).isEqualByComparingTo("5"); // round(sqrt(9 + 16)) = round(5) = 5
    }

    @Test
    void shouldEvaluateFunctionsWithVariables() {
        BigDecimal result = evaluator.evaluate("round(voltage / 10)", variables);
        assertThat(result).isEqualByComparingTo("22"); // round(220 / 10) = 22
    }

    @Test
    void shouldEvaluateComplexConditional() {
        // if temperature > 75, return 1, else 0
        BigDecimal result = evaluator.evaluate("if(temperature > 75, 1, 0)", variables);
        assertThat(result).isEqualByComparingTo("1"); // 75.5 > 75 = true
    }

    @Test
    void shouldEvaluatePowerCalculation() {
        // Apparent power = voltage * current
        BigDecimal result = evaluator.evaluate("round(voltage * current)", variables);
        assertThat(result).isEqualByComparingTo("1100");
    }

    @Test
    void shouldEvaluateSpikeDetection() {
        // Simplified spike detection: if(kwConsumption > 80, 1, 0)
        BigDecimal result = evaluator.evaluate("if(kwConsumption > 80, 1, 0)", variables);
        assertThat(result).isEqualByComparingTo("1"); // 100 > 80 = true
    }

    @Test
    void shouldEvaluateGroupedComparison() {
        // Bug fix test: parentheses for grouping should not break comparisons
        // (voltage - 210) > 5 should work (220 - 210 = 10 > 5 = true)
        BigDecimal result = evaluator.evaluate("(voltage - 210) > 5", variables);
        assertThat(result).isEqualByComparingTo("1");
    }

    @Test
    void shouldEvaluateIfWithGroupedComparison() {
        // Bug fix test: if with grouped comparison argument
        // if((kwConsumption - 80) > 10, 1, 0) should work (100 - 80 = 20 > 10 = true)
        BigDecimal result = evaluator.evaluate("if((kwConsumption - 80) > 10, 1, 0)", variables);
        assertThat(result).isEqualByComparingTo("1");
    }

    // ===== ERROR HANDLING TESTS =====

    @Test
    void shouldThrowExceptionForDivisionByZero() {
        assertThatThrownBy(() -> evaluator.evaluate("10 / 0", Map.of()))
                .isInstanceOf(ExpressionEvaluationException.class)
                .hasMessageContaining("Division by zero");
    }

    @Test
    void shouldThrowExceptionForUnknownFunction() {
        assertThatThrownBy(() -> evaluator.evaluate("unknownFunc(10)", Map.of()))
                .isInstanceOf(ExpressionEvaluationException.class)
                .hasMessageContaining("Unknown function");
    }

    @Test
    void shouldThrowExceptionForMismatchedParentheses() {
        assertThatThrownBy(() -> evaluator.evaluate("(10 + 20", Map.of()))
                .isInstanceOf(ExpressionEvaluationException.class)
                .hasMessageContaining("parentheses");
    }

    @Test
    void shouldThrowExceptionForNegativeSqrt() {
        assertThatThrownBy(() -> evaluator.evaluate("sqrt(-1)", Map.of()))
                .isInstanceOf(ExpressionEvaluationException.class)
                .hasMessageContaining("non-negative");
    }

    // ===== EDGE CASES =====

    @Test
    void shouldHandleWhitespace() {
        BigDecimal result = evaluator.evaluate("  10  +  20  ", Map.of());
        assertThat(result).isEqualByComparingTo("30");
    }

    @Test
    void shouldHandleNegativeNumbers() {
        BigDecimal result = evaluator.evaluate("-10 + 20", Map.of());
        assertThat(result).isEqualByComparingTo("10");
    }

    @Test
    void shouldHandleDecimalNumbers() {
        BigDecimal result = evaluator.evaluate("10.5 + 20.3", Map.of());
        assertThat(result.doubleValue()).isCloseTo(30.8, within(0.01));
    }

    @Test
    void shouldReturnNullForEmptyExpression() {
        BigDecimal result = evaluator.evaluate("", Map.of());
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForNullExpression() {
        BigDecimal result = evaluator.evaluate(null, Map.of());
        assertThat(result).isNull();
    }
}
