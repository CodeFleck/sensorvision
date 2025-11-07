package org.sensorvision.expression.functions;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Mathematical functions for synthetic variable expressions.
 * All functions work with BigDecimal for precision.
 */
public class MathFunctions {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * Square root: sqrt(x)
     */
    public static BigDecimal sqrt(Object... args) {
        validateArgCount(args, 1, "sqrt");
        BigDecimal x = toBigDecimal(args[0]);
        if (x.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("sqrt: argument must be non-negative");
        }
        return BigDecimal.valueOf(Math.sqrt(x.doubleValue()));
    }

    /**
     * Power: pow(base, exponent)
     */
    public static BigDecimal pow(Object... args) {
        validateArgCount(args, 2, "pow");
        BigDecimal base = toBigDecimal(args[0]);
        BigDecimal exponent = toBigDecimal(args[1]);
        return BigDecimal.valueOf(Math.pow(base.doubleValue(), exponent.doubleValue()));
    }

    /**
     * Absolute value: abs(x)
     */
    public static BigDecimal abs(Object... args) {
        validateArgCount(args, 1, "abs");
        BigDecimal x = toBigDecimal(args[0]);
        return x.abs();
    }

    /**
     * Natural logarithm: log(x)
     */
    public static BigDecimal log(Object... args) {
        validateArgCount(args, 1, "log");
        BigDecimal x = toBigDecimal(args[0]);
        if (x.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("log: argument must be positive");
        }
        return BigDecimal.valueOf(Math.log(x.doubleValue()));
    }

    /**
     * Base-10 logarithm: log10(x)
     */
    public static BigDecimal log10(Object... args) {
        validateArgCount(args, 1, "log10");
        BigDecimal x = toBigDecimal(args[0]);
        if (x.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("log10: argument must be positive");
        }
        return BigDecimal.valueOf(Math.log10(x.doubleValue()));
    }

    /**
     * Exponential: exp(x) = e^x
     */
    public static BigDecimal exp(Object... args) {
        validateArgCount(args, 1, "exp");
        BigDecimal x = toBigDecimal(args[0]);
        return BigDecimal.valueOf(Math.exp(x.doubleValue()));
    }

    /**
     * Sine: sin(x) where x is in radians
     */
    public static BigDecimal sin(Object... args) {
        validateArgCount(args, 1, "sin");
        BigDecimal x = toBigDecimal(args[0]);
        return BigDecimal.valueOf(Math.sin(x.doubleValue()));
    }

    /**
     * Cosine: cos(x) where x is in radians
     */
    public static BigDecimal cos(Object... args) {
        validateArgCount(args, 1, "cos");
        BigDecimal x = toBigDecimal(args[0]);
        return BigDecimal.valueOf(Math.cos(x.doubleValue()));
    }

    /**
     * Tangent: tan(x) where x is in radians
     */
    public static BigDecimal tan(Object... args) {
        validateArgCount(args, 1, "tan");
        BigDecimal x = toBigDecimal(args[0]);
        return BigDecimal.valueOf(Math.tan(x.doubleValue()));
    }

    /**
     * Arc sine: asin(x) returns value in radians
     */
    public static BigDecimal asin(Object... args) {
        validateArgCount(args, 1, "asin");
        BigDecimal x = toBigDecimal(args[0]);
        if (x.abs().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("asin: argument must be in range [-1, 1]");
        }
        return BigDecimal.valueOf(Math.asin(x.doubleValue()));
    }

    /**
     * Arc cosine: acos(x) returns value in radians
     */
    public static BigDecimal acos(Object... args) {
        validateArgCount(args, 1, "acos");
        BigDecimal x = toBigDecimal(args[0]);
        if (x.abs().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("acos: argument must be in range [-1, 1]");
        }
        return BigDecimal.valueOf(Math.acos(x.doubleValue()));
    }

    /**
     * Arc tangent: atan(x) returns value in radians
     */
    public static BigDecimal atan(Object... args) {
        validateArgCount(args, 1, "atan");
        BigDecimal x = toBigDecimal(args[0]);
        return BigDecimal.valueOf(Math.atan(x.doubleValue()));
    }

    /**
     * Round to nearest integer: round(x)
     */
    public static BigDecimal round(Object... args) {
        validateArgCount(args, 1, "round");
        BigDecimal x = toBigDecimal(args[0]);
        return x.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Floor: largest integer <= x
     */
    public static BigDecimal floor(Object... args) {
        validateArgCount(args, 1, "floor");
        BigDecimal x = toBigDecimal(args[0]);
        return x.setScale(0, RoundingMode.FLOOR);
    }

    /**
     * Ceiling: smallest integer >= x
     */
    public static BigDecimal ceil(Object... args) {
        validateArgCount(args, 1, "ceil");
        BigDecimal x = toBigDecimal(args[0]);
        return x.setScale(0, RoundingMode.CEILING);
    }

    /**
     * Minimum value: min(x, y, ...)
     */
    public static BigDecimal min(Object... args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("min: requires at least one argument");
        }
        BigDecimal result = toBigDecimal(args[0]);
        for (int i = 1; i < args.length; i++) {
            BigDecimal current = toBigDecimal(args[i]);
            if (current.compareTo(result) < 0) {
                result = current;
            }
        }
        return result;
    }

    /**
     * Maximum value: max(x, y, ...)
     */
    public static BigDecimal max(Object... args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("max: requires at least one argument");
        }
        BigDecimal result = toBigDecimal(args[0]);
        for (int i = 1; i < args.length; i++) {
            BigDecimal current = toBigDecimal(args[i]);
            if (current.compareTo(result) > 0) {
                result = current;
            }
        }
        return result;
    }

    // Helper methods

    private static void validateArgCount(Object[] args, int expected, String functionName) {
        if (args.length != expected) {
            throw new IllegalArgumentException(
                String.format("%s: expected %d argument(s), got %d", functionName, expected, args.length)
            );
        }
    }

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
