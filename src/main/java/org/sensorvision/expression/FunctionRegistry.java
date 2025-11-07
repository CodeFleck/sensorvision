package org.sensorvision.expression;

import lombok.extern.slf4j.Slf4j;
import org.sensorvision.expression.functions.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of all available expression functions.
 * Functions are organized by category: math, statistics, time, logic.
 */
@Slf4j
@Component
public class FunctionRegistry {

    private final Map<String, ExpressionFunction> functions = new HashMap<>();
    private final Map<String, String> functionDescriptions = new HashMap<>();
    private final Map<String, String> functionCategories = new HashMap<>();

    public FunctionRegistry() {
        registerAllFunctions();
    }

    private void registerAllFunctions() {
        // Math functions
        registerMath("sqrt", MathFunctions::sqrt, "Square root: sqrt(x)");
        registerMath("pow", MathFunctions::pow, "Power: pow(base, exponent)");
        registerMath("abs", MathFunctions::abs, "Absolute value: abs(x)");
        registerMath("log", MathFunctions::log, "Natural logarithm: log(x)");
        registerMath("log10", MathFunctions::log10, "Base-10 logarithm: log10(x)");
        registerMath("exp", MathFunctions::exp, "Exponential: exp(x) = e^x");
        registerMath("sin", MathFunctions::sin, "Sine: sin(x) in radians");
        registerMath("cos", MathFunctions::cos, "Cosine: cos(x) in radians");
        registerMath("tan", MathFunctions::tan, "Tangent: tan(x) in radians");
        registerMath("asin", MathFunctions::asin, "Arc sine: asin(x) in radians");
        registerMath("acos", MathFunctions::acos, "Arc cosine: acos(x) in radians");
        registerMath("atan", MathFunctions::atan, "Arc tangent: atan(x) in radians");
        registerMath("round", MathFunctions::round, "Round to nearest integer: round(x)");
        registerMath("floor", MathFunctions::floor, "Floor: floor(x)");
        registerMath("ceil", MathFunctions::ceil, "Ceiling: ceil(x)");
        registerMath("min", MathFunctions::min, "Minimum value: min(x, y, ...)");
        registerMath("max", MathFunctions::max, "Maximum value: max(x, y, ...)");

        // Conditional logic
        registerLogic("if", LogicFunctions::ifThenElse, "Conditional: if(condition, trueValue, falseValue)");
        registerLogic("and", LogicFunctions::and, "Logical AND: and(x, y, ...) - returns 1 if all non-zero, else 0");
        registerLogic("or", LogicFunctions::or, "Logical OR: or(x, y, ...) - returns 1 if any non-zero, else 0");
        registerLogic("not", LogicFunctions::not, "Logical NOT: not(x) - returns 1 if x is 0, else 0");

        log.info("Registered {} expression functions across {} categories",
                functions.size(), Set.copyOf(functionCategories.values()).size());
    }

    private void registerMath(String name, ExpressionFunction function, String description) {
        register(name, function, description, "Math");
    }

    private void registerLogic(String name, ExpressionFunction function, String description) {
        register(name, function, description, "Logic");
    }

    private void register(String name, ExpressionFunction function, String description, String category) {
        functions.put(name.toLowerCase(), function);
        functionDescriptions.put(name.toLowerCase(), description);
        functionCategories.put(name.toLowerCase(), category);
    }

    /**
     * Get a function by name (case-insensitive).
     */
    public ExpressionFunction getFunction(String name) {
        return functions.get(name.toLowerCase());
    }

    /**
     * Check if a function exists.
     */
    public boolean hasFunction(String name) {
        return functions.containsKey(name.toLowerCase());
    }

    /**
     * Get all registered function names.
     */
    public Set<String> getFunctionNames() {
        return functions.keySet();
    }

    /**
     * Get function description.
     */
    public String getDescription(String name) {
        return functionDescriptions.get(name.toLowerCase());
    }

    /**
     * Get function category.
     */
    public String getCategory(String name) {
        return functionCategories.get(name.toLowerCase());
    }

    /**
     * Get all functions by category.
     */
    public Map<String, Set<String>> getFunctionsByCategory() {
        Map<String, Set<String>> result = new HashMap<>();
        functionCategories.forEach((func, cat) ->
            result.computeIfAbsent(cat, k -> new java.util.HashSet<>()).add(func)
        );
        return result;
    }
}
