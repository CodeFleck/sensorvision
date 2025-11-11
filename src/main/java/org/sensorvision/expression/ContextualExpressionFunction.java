package org.sensorvision.expression;

import java.math.BigDecimal;

/**
 * Enhanced expression function interface that supports contextual information.
 * Used for statistical functions that need access to historical telemetry data.
 */
@FunctionalInterface
public interface ContextualExpressionFunction extends ExpressionFunction {

    /**
     * Evaluate the function with context and arguments.
     *
     * @param context Statistical context (device, time, repository access)
     * @param args Function arguments
     * @return The calculated result
     * @throws IllegalArgumentException if arguments are invalid
     */
    BigDecimal evaluateWithContext(StatisticalFunctionContext context, Object... args);

    /**
     * Default implementation that throws exception if called without context.
     * Statistical functions require context to work.
     */
    @Override
    default BigDecimal evaluate(Object... args) {
        throw new UnsupportedOperationException(
            "This function requires statistical context. Use evaluateWithContext() instead."
        );
    }
}
