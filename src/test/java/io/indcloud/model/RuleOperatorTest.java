package io.indcloud.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleOperator Tests")
class RuleOperatorTest {

    @Nested
    @DisplayName("getSymbol() Tests")
    class GetSymbolTests {

        @Test
        @DisplayName("GT should return '>' symbol")
        void gtShouldReturnGreaterThanSymbol() {
            assertThat(RuleOperator.GT.getSymbol()).isEqualTo(">");
        }

        @Test
        @DisplayName("GTE should return '>=' symbol")
        void gteShouldReturnGreaterThanOrEqualSymbol() {
            assertThat(RuleOperator.GTE.getSymbol()).isEqualTo(">=");
        }

        @Test
        @DisplayName("LT should return '<' symbol")
        void ltShouldReturnLessThanSymbol() {
            assertThat(RuleOperator.LT.getSymbol()).isEqualTo("<");
        }

        @Test
        @DisplayName("LTE should return '<=' symbol")
        void lteShouldReturnLessThanOrEqualSymbol() {
            assertThat(RuleOperator.LTE.getSymbol()).isEqualTo("<=");
        }

        @Test
        @DisplayName("EQ should return '=' symbol")
        void eqShouldReturnEqualSymbol() {
            assertThat(RuleOperator.EQ.getSymbol()).isEqualTo("=");
        }

        @Test
        @DisplayName("All operators should have non-null symbols")
        void allOperatorsShouldHaveNonNullSymbols() {
            for (RuleOperator operator : RuleOperator.values()) {
                assertThat(operator.getSymbol())
                    .as("Symbol for %s should not be null", operator.name())
                    .isNotNull()
                    .isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("getDescription() Tests")
    class GetDescriptionTests {

        @Test
        @DisplayName("All operators should have non-null descriptions")
        void allOperatorsShouldHaveNonNullDescriptions() {
            for (RuleOperator operator : RuleOperator.values()) {
                assertThat(operator.getDescription())
                    .as("Description for %s should not be null", operator.name())
                    .isNotNull()
                    .isNotEmpty();
            }
        }

        @ParameterizedTest
        @CsvSource({
            "GT, Greater than",
            "GTE, Greater than or equal",
            "LT, Less than",
            "LTE, Less than or equal",
            "EQ, Equal to"
        })
        @DisplayName("Operators should have correct descriptions")
        void operatorsShouldHaveCorrectDescriptions(String operatorName, String expectedDescription) {
            RuleOperator operator = RuleOperator.valueOf(operatorName);
            assertThat(operator.getDescription()).isEqualTo(expectedDescription);
        }
    }

    @Nested
    @DisplayName("evaluate() Tests")
    class EvaluateTests {

        @ParameterizedTest
        @CsvSource({
            "GT, 10, 5, true",
            "GT, 5, 10, false",
            "GT, 5, 5, false",
            "GTE, 10, 5, true",
            "GTE, 5, 5, true",
            "GTE, 5, 10, false",
            "LT, 5, 10, true",
            "LT, 10, 5, false",
            "LT, 5, 5, false",
            "LTE, 5, 10, true",
            "LTE, 5, 5, true",
            "LTE, 10, 5, false",
            "EQ, 5, 5, true",
            "EQ, 5, 10, false"
        })
        @DisplayName("evaluate() should correctly compare values")
        void evaluateShouldCorrectlyCompareValues(String operatorName, String actual, String threshold, boolean expected) {
            RuleOperator operator = RuleOperator.valueOf(operatorName);
            BigDecimal actualValue = new BigDecimal(actual);
            BigDecimal thresholdValue = new BigDecimal(threshold);

            assertThat(operator.evaluate(actualValue, thresholdValue))
                .as("%s.evaluate(%s, %s) should be %s", operatorName, actual, threshold, expected)
                .isEqualTo(expected);
        }

        @Test
        @DisplayName("evaluate() should return false when actual value is null")
        void evaluateShouldReturnFalseWhenActualIsNull() {
            for (RuleOperator operator : RuleOperator.values()) {
                assertThat(operator.evaluate(null, BigDecimal.TEN))
                    .as("%s.evaluate(null, 10) should be false", operator.name())
                    .isFalse();
            }
        }

        @Test
        @DisplayName("evaluate() should return false when threshold is null")
        void evaluateShouldReturnFalseWhenThresholdIsNull() {
            for (RuleOperator operator : RuleOperator.values()) {
                assertThat(operator.evaluate(BigDecimal.TEN, null))
                    .as("%s.evaluate(10, null) should be false", operator.name())
                    .isFalse();
            }
        }

        @Test
        @DisplayName("evaluate() should handle decimal precision correctly")
        void evaluateShouldHandleDecimalPrecision() {
            BigDecimal value1 = new BigDecimal("175.500000");
            BigDecimal value2 = new BigDecimal("175.5");
            BigDecimal threshold = new BigDecimal("200");

            // Both should evaluate the same way
            assertThat(RuleOperator.LT.evaluate(value1, threshold)).isTrue();
            assertThat(RuleOperator.LT.evaluate(value2, threshold)).isTrue();

            // They should be equal to each other
            assertThat(RuleOperator.EQ.evaluate(value1, value2)).isTrue();
        }
    }
}
