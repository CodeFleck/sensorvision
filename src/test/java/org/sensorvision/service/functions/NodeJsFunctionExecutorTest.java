package org.sensorvision.service.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.FunctionRuntime;
import org.sensorvision.model.ServerlessFunction;
import org.sensorvision.service.FunctionSecretsService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NodeJsFunctionExecutor.
 *
 * Note: These tests require Node.js 18+ to be installed on the system.
 * Tests will be skipped if Node.js is not available.
 */
@ExtendWith(MockitoExtension.class)
class NodeJsFunctionExecutorTest {

    private NodeJsFunctionExecutor executor;
    private ObjectMapper objectMapper;

    @Mock
    private FunctionSecretsService secretsService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(secretsService.getDecryptedSecrets(anyLong())).thenReturn(Collections.emptyMap());
        executor = new NodeJsFunctionExecutor(objectMapper, secretsService);
    }

    @Test
    void testSupportsNodeJs18Runtime() {
        assertThat(executor.supports(FunctionRuntime.NODEJS_18)).isTrue();
        assertThat(executor.supports(FunctionRuntime.PYTHON_3_11)).isFalse();
    }

    @Test
    @EnabledIf("isNodeJsAvailable")
    void testExecuteSimpleFunction() throws Exception {
        // Given
        String code = """
            function handler(event) {
                return {
                    message: 'Hello, ' + event.name + '!',
                    input: event
                };
            }
            """;

        ServerlessFunction function = createTestFunction(code, "handler", 30);
        ObjectNode input = objectMapper.createObjectNode();
        input.put("name", "World");

        // When
        FunctionExecutionResult result = executor.execute(function, input);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getOutput()).isNotNull();
        assertThat(result.getOutput().get("message").asText()).isEqualTo("Hello, World!");
        assertThat(result.getDurationMs()).isGreaterThan(0);
    }

    @Test
    @EnabledIf("isNodeJsAvailable")
    void testExecuteAsyncFunction() throws Exception {
        // Given
        String code = """
            async function handler(event) {
                // Simulate async operation
                await new Promise(resolve => setTimeout(resolve, 100));

                return {
                    message: 'Async result',
                    value: event.value * 2
                };
            }
            """;

        ServerlessFunction function = createTestFunction(code, "handler", 30);
        ObjectNode input = objectMapper.createObjectNode();
        input.put("value", 21);

        // When
        FunctionExecutionResult result = executor.execute(function, input);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isNotNull();
        assertThat(result.getOutput().get("message").asText()).isEqualTo("Async result");
        assertThat(result.getOutput().get("value").asInt()).isEqualTo(42);
    }

    @Test
    @EnabledIf("isNodeJsAvailable")
    void testExecuteFunctionWithComplexInput() throws Exception {
        // Given
        String code = """
            function handler(event) {
                const sum = event.numbers.reduce((a, b) => a + b, 0);
                const avg = sum / event.numbers.length;

                return {
                    sum: sum,
                    average: avg,
                    count: event.numbers.length
                };
            }
            """;

        ServerlessFunction function = createTestFunction(code, "handler", 30);
        ObjectNode input = objectMapper.createObjectNode();
        input.putArray("numbers").add(10).add(20).add(30).add(40).add(50);

        // When
        FunctionExecutionResult result = executor.execute(function, input);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isNotNull();
        assertThat(result.getOutput().get("sum").asInt()).isEqualTo(150);
        assertThat(result.getOutput().get("average").asDouble()).isEqualTo(30.0);
        assertThat(result.getOutput().get("count").asInt()).isEqualTo(5);
    }

    @Test
    @EnabledIf("isNodeJsAvailable")
    void testExecuteFunctionWithError() throws Exception {
        // Given
        String code = """
            function handler(event) {
                throw new Error('Intentional error for testing');
            }
            """;

        ServerlessFunction function = createTestFunction(code, "handler", 30);
        ObjectNode input = objectMapper.createObjectNode();

        // When
        FunctionExecutionResult result = executor.execute(function, input);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("exit code 1");
        assertThat(result.getErrorStack()).contains("Intentional error for testing");
    }

    @Test
    @EnabledIf("isNodeJsAvailable")
    void testExecuteFunctionWithTimeout() throws Exception {
        // Given
        String code = """
            function handler(event) {
                // Infinite loop
                while(true) {
                    // Do nothing
                }
                return { message: 'Should not reach here' };
            }
            """;

        ServerlessFunction function = createTestFunction(code, "handler", 2); // 2 second timeout
        ObjectNode input = objectMapper.createObjectNode();

        // When
        FunctionExecutionResult result = executor.execute(function, input);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("timeout");
    }

    @Test
    @EnabledIf("isNodeJsAvailable")
    void testExecuteFunctionWithSyntaxError() throws Exception {
        // Given
        String code = """
            function handler(event) {
                return {
                    invalid syntax here!!!
                };
            }
            """;

        ServerlessFunction function = createTestFunction(code, "handler", 30);
        ObjectNode input = objectMapper.createObjectNode();

        // When
        FunctionExecutionResult result = executor.execute(function, input);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("exit code 1");
    }

    @Test
    void testNodeJsNotAvailableThrowsException() throws Exception {
        // Skip if Node.js is actually available
        if (isNodeJsAvailable()) {
            return;
        }

        // Given
        String code = "function handler(event) { return event; }";
        ServerlessFunction function = createTestFunction(code, "handler", 30);
        ObjectNode input = objectMapper.createObjectNode();

        // When
        FunctionExecutionResult result = executor.execute(function, input);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Node.js is not installed");
    }

    // Helper methods

    private ServerlessFunction createTestFunction(String code, String handler, int timeoutSeconds) {
        ServerlessFunction function = new ServerlessFunction();
        function.setId(1L);
        function.setName("test-function");
        function.setCode(code);
        function.setHandler(handler);
        function.setRuntime(FunctionRuntime.NODEJS_18);
        function.setTimeoutSeconds(timeoutSeconds);
        function.setMemoryLimitMb(512);
        return function;
    }

    private static boolean isNodeJsAvailable() {
        try {
            Process process = new ProcessBuilder("node", "--version")
                .redirectErrorStream(true)
                .start();
            boolean completed = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
