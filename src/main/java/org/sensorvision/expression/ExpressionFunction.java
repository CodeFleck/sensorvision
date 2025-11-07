package org.sensorvision.expression;

import java.math.BigDecimal;

/**
 * Interface for functions that can be used in synthetic variable expressions.
 * Functions accept variable arguments and return a BigDecimal result.
 */
@FunctionalInterface
public interface ExpressionFunction {

    /**
     * Evaluate the function with the given arguments.
     *
     * @param args Function arguments (can be numbers, strings, etc.)
     * @return The calculated result
     * @throws IllegalArgumentException if arguments are invalid
     */
    BigDecimal evaluate(Object... args);
}
