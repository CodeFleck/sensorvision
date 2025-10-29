package org.sensorvision.service.functions;

import com.fasterxml.jackson.databind.JsonNode;
import org.sensorvision.model.FunctionRuntime;
import org.sensorvision.model.ServerlessFunction;

/**
 * Interface for executing serverless functions in different runtimes.
 */
public interface FunctionExecutor {

    /**
     * Check if this executor supports the given runtime.
     */
    boolean supports(FunctionRuntime runtime);

    /**
     * Execute a function with the given input data.
     *
     * @param function The function to execute
     * @param input The input data
     * @return The execution result
     * @throws FunctionExecutionException if execution fails
     */
    FunctionExecutionResult execute(ServerlessFunction function, JsonNode input) throws FunctionExecutionException;
}
