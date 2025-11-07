package org.sensorvision.expression.functions;

import java.math.BigDecimal;

/**
 * Logic and conditional functions for synthetic variable expressions.
 */
public class LogicFunctions {

    /**
     * Conditional: if(condition, trueValue, falseValue)
     * If condition != 0, returns trueValue, otherwise returns falseValue
     */
    public static BigDecimal ifThenElse(Object... args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("if: expected 3 arguments (condition, trueValue, falseValue), got " + args.length);
        }

        BigDecimal condition = toBigDecimal(args[0]);
        BigDecimal trueValue = toBigDecimal(args[1]);
        BigDecimal falseValue = toBigDecimal(args[2]);

        // Condition is true if != 0
        return condition.compareTo(BigDecimal.ZERO) != 0 ? trueValue : falseValue;
    }

    /**
     * Logical AND: Returns 1 if both arguments are non-zero, otherwise 0
     */
    public static BigDecimal and(Object... args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("and: requires at least 2 arguments");
        }

        for (Object arg : args) {
            BigDecimal value = toBigDecimal(arg);
            if (value.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ONE;
    }

    /**
     * Logical OR: Returns 1 if any argument is non-zero, otherwise 0
     */
    public static BigDecimal or(Object... args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("or: requires at least 2 arguments");
        }

        for (Object arg : args) {
            BigDecimal value = toBigDecimal(arg);
            if (value.compareTo(BigDecimal.ZERO) != 0) {
                return BigDecimal.ONE;
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Logical NOT: Returns 1 if argument is 0, otherwise 0
     */
    public static BigDecimal not(Object... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("not: expected 1 argument, got " + args.length);
        }

        BigDecimal value = toBigDecimal(args[0]);
        return value.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    // Helper method
    private static BigDecimal toBigDecimal(Object arg) {
        if (arg instanceof BigDecimal) {
            return (BigDecimal) arg;
        } else if (arg instanceof Number) {
            return BigDecimal.valueOf(((Number) arg).doubleValue());
        } else if (arg instanceof String) {
            try {
                return new BigDecimal((String) arg);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + arg + "' to number");
            }
        } else {
            throw new IllegalArgumentException("Cannot convert " + arg.getClass().getSimpleName() + " to BigDecimal");
        }
    }
}
