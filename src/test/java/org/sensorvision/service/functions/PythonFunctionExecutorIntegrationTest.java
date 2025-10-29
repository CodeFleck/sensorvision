package org.sensorvision.service.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.sensorvision.model.FunctionRuntime;
import org.sensorvision.model.Organization;
import org.sensorvision.model.ServerlessFunction;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Python function execution.
 * Requires Python 3 to be installed on the system.
 *
 * Set PYTHON_AVAILABLE=true environment variable to enable these tests.
 */
@EnabledIfEnvironmentVariable(named = "PYTHON_AVAILABLE", matches = "true")
class PythonFunctionExecutorIntegrationTest {

    private PythonFunctionExecutor executor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executor = new PythonFunctionExecutor(objectMapper);
    }

    @Test
    void execute_shouldRunSimplePythonFunction() throws Exception {
        // Arrange
        String pythonCode = """
            def main(event):
                return {"result": "Hello from Python", "input": event}
            """;

        ServerlessFunction function = createFunction("test-function", pythonCode);
        JsonNode input = objectMapper.readTree("{\"value\": 42}");

        // Act
        FunctionExecutionResult result = executor.execute(function, input);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isNotNull();
        assertThat(result.getOutput().get("result").asText()).isEqualTo("Hello from Python");
        assertThat(result.getOutput().get("input").get("value").asInt()).isEqualTo(42);
        assertThat(result.getDurationMs()).isGreaterThan(0);
    }

    @Test
    void execute_shouldHandleMathOperations() throws Exception {
        // Arrange
        String pythonCode = """
            def main(event):
                a = event.get('a', 0)
                b = event.get('b', 0)
                return {
                    'sum': a + b,
                    'product': a * b,
                    'difference': a - b
                }
            """;

        ServerlessFunction function = createFunction("math-function", pythonCode);
        JsonNode input = objectMapper.readTree("{\"a\": 10, \"b\": 5}");

        // Act
        FunctionExecutionResult result = executor.execute(function, input);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("sum").asInt()).isEqualTo(15);
        assertThat(result.getOutput().get("product").asInt()).isEqualTo(50);
        assertThat(result.getOutput().get("difference").asInt()).isEqualTo(5);
    }

    @Test
    void execute_shouldCaptureErrorsGracefully() throws Exception {
        // Arrange - code that will raise an exception
        String pythonCode = """
            def main(event):
                x = event['missing_key']  # This will raise KeyError
                return {"result": x}
            """;

        ServerlessFunction function = createFunction("error-function", pythonCode);
        JsonNode input = objectMapper.readTree("{}");

        // Act
        FunctionExecutionResult result = executor.execute(function, input);

        // Assert
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("failed with exit code");
        assertThat(result.getErrorStack()).isNotNull();
    }

    @Test
    void execute_shouldEnforceTimeout() throws Exception {
        // Arrange - function that sleeps longer than timeout
        String pythonCode = """
            import time

            def main(event):
                time.sleep(10)  # Sleep for 10 seconds
                return {"result": "Should not reach here"}
            """;

        ServerlessFunction function = createFunction("timeout-function", pythonCode);
        function.setTimeoutSeconds(2);  // 2 second timeout
        JsonNode input = objectMapper.readTree("{}");

        // Act
        FunctionExecutionResult result = executor.execute(function, input);

        // Assert
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("timeout");
        assertThat(result.getDurationMs()).isGreaterThanOrEqualTo(2000);
        assertThat(result.getDurationMs()).isLessThan(15000);  // Should not wait full 10 seconds
    }

    @Test
    void execute_shouldHandleComplexDataStructures() throws Exception {
        // Arrange
        String pythonCode = """
            def main(event):
                devices = event.get('devices', [])
                total = sum(d.get('value', 0) for d in devices)
                avg = total / len(devices) if devices else 0

                return {
                    'total': total,
                    'average': avg,
                    'count': len(devices)
                }
            """;

        ServerlessFunction function = createFunction("aggregate-function", pythonCode);

        Map<String, Object> inputData = Map.of(
            "devices", java.util.List.of(
                Map.of("id", "dev-1", "value", 100),
                Map.of("id", "dev-2", "value", 200),
                Map.of("id", "dev-3", "value", 300)
            )
        );
        JsonNode input = objectMapper.valueToTree(inputData);

        // Act
        FunctionExecutionResult result = executor.execute(function, input);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("total").asInt()).isEqualTo(600);
        assertThat(result.getOutput().get("average").asDouble()).isEqualTo(200.0);
        assertThat(result.getOutput().get("count").asInt()).isEqualTo(3);
    }

    @Test
    void execute_shouldUseCustomHandler() throws Exception {
        // Arrange - using a different handler name
        String pythonCode = """
            def custom_handler(event):
                return {"handler": "custom", "value": event.get("x", 0) * 2}
            """;

        ServerlessFunction function = createFunction("custom-handler", pythonCode);
        function.setHandler("custom_handler");
        JsonNode input = objectMapper.readTree("{\"x\": 21}");

        // Act
        FunctionExecutionResult result = executor.execute(function, input);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("handler").asText()).isEqualTo("custom");
        assertThat(result.getOutput().get("value").asInt()).isEqualTo(42);
    }

    private ServerlessFunction createFunction(String name, String code) {
        Organization org = new Organization();
        org.setId(1L);
        org.setName("Test Org");

        ServerlessFunction function = new ServerlessFunction();
        function.setId(1L);
        function.setOrganization(org);
        function.setName(name);
        function.setRuntime(FunctionRuntime.PYTHON_3_11);
        function.setCode(code);
        function.setHandler("main");
        function.setTimeoutSeconds(30);
        function.setMemoryLimitMb(512);
        function.setEnabled(true);

        return function;
    }
}
