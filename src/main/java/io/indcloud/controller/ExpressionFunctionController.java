package io.indcloud.controller;

import lombok.RequiredArgsConstructor;
import io.indcloud.expression.FunctionRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for expression functions metadata.
 * Provides available functions for UI autocomplete and documentation.
 */
@RestController
@RequestMapping("/api/v1/expression-functions")
@RequiredArgsConstructor
public class ExpressionFunctionController {

    private final FunctionRegistry functionRegistry;

    /**
     * Get all available expression functions organized by category.
     *
     * Response format:
     * {
     *   "Math": [
     *     { "name": "sqrt", "description": "Square root: sqrt(x)", "category": "Math" },
     *     ...
     *   ],
     *   "Logic": [
     *     { "name": "if", "description": "Conditional: if(condition, trueValue, falseValue)", "category": "Logic" },
     *     ...
     *   ]
     * }
     */
    @GetMapping
    public ResponseEntity<Map<String, List<FunctionInfo>>> getAllFunctions() {
        Map<String, List<FunctionInfo>> categorizedFunctions = new LinkedHashMap<>();

        // Group functions by category
        for (String functionName : functionRegistry.getFunctionNames()) {
            String category = functionRegistry.getCategory(functionName);
            String description = functionRegistry.getDescription(functionName);

            FunctionInfo info = new FunctionInfo(functionName, description, category);

            categorizedFunctions
                    .computeIfAbsent(category, k -> new ArrayList<>())
                    .add(info);
        }

        // Sort functions within each category
        categorizedFunctions.values().forEach(list ->
                list.sort(Comparator.comparing(FunctionInfo::name))
        );

        return ResponseEntity.ok(categorizedFunctions);
    }

    /**
     * Get a flat list of all available functions (useful for autocomplete).
     *
     * Response format:
     * [
     *   { "name": "sqrt", "description": "Square root: sqrt(x)", "category": "Math" },
     *   { "name": "if", "description": "Conditional: if(condition, trueValue, falseValue)", "category": "Logic" },
     *   ...
     * ]
     */
    @GetMapping("/flat")
    public ResponseEntity<List<FunctionInfo>> getAllFunctionsFlat() {
        List<FunctionInfo> functions = functionRegistry.getFunctionNames().stream()
                .map(name -> new FunctionInfo(
                        name,
                        functionRegistry.getDescription(name),
                        functionRegistry.getCategory(name)
                ))
                .sorted(Comparator.comparing(FunctionInfo::name))
                .collect(Collectors.toList());

        return ResponseEntity.ok(functions);
    }

    /**
     * Function metadata DTO.
     */
    public record FunctionInfo(
            String name,
            String description,
            String category
    ) {}
}
