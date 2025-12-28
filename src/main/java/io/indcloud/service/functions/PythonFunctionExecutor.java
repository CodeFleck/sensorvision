package io.indcloud.service.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.indcloud.model.FunctionRuntime;
import io.indcloud.model.ServerlessFunction;
import io.indcloud.service.FunctionSecretsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Executor for Python 3.11+ functions.
 * Executes Python code in a separate process with resource limits.
 */
@Component
public class PythonFunctionExecutor implements FunctionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(PythonFunctionExecutor.class);
    private final ObjectMapper objectMapper;
    private final FunctionSecretsService secretsService;

    public PythonFunctionExecutor(ObjectMapper objectMapper, FunctionSecretsService secretsService) {
        this.objectMapper = objectMapper;
        this.secretsService = secretsService;
    }

    @Override
    public boolean supports(FunctionRuntime runtime) {
        return runtime == FunctionRuntime.PYTHON_3_11;
    }

    @Override
    public FunctionExecutionResult execute(ServerlessFunction function, JsonNode input) throws FunctionExecutionException {
        long startTime = System.currentTimeMillis();
        Path tempDir = null;

        try {
            // Create temporary directory for function execution
            tempDir = Files.createTempDirectory("sensorvision-func-" + UUID.randomUUID());
            Path scriptPath = tempDir.resolve("function.py");
            Path inputPath = tempDir.resolve("input.json");
            Path outputPath = tempDir.resolve("output.json");

            // Write function code to file
            try (FileWriter writer = new FileWriter(scriptPath.toFile())) {
                writer.write(createPythonWrapper(function.getCode(), function.getHandler()));
            }

            // Write input data to file
            try (FileWriter writer = new FileWriter(inputPath.toFile())) {
                objectMapper.writeValue(writer, input);
            }

            // Build Python command (OS-aware: uses "python" on Windows, "python3" elsewhere)
            List<String> command = new ArrayList<>();
            command.add(FunctionRuntime.PYTHON_3_11.getCommand());
            command.add(scriptPath.toString());
            command.add(inputPath.toString());
            command.add(outputPath.toString());

            // Get decrypted secrets and prepare environment variables
            Map<String, String> secrets = secretsService.getDecryptedSecrets(function.getId());

            // Execute with timeout and inject secrets as environment variables
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(tempDir.toFile())
                .redirectErrorStream(false);

            // Inject secrets as environment variables
            if (!secrets.isEmpty()) {
                Map<String, String> env = processBuilder.environment();
                env.putAll(secrets);
                logger.debug("Injected {} secrets as environment variables for function {}",
                    secrets.size(), function.getId());
            }

            logger.debug("Executing Python function with command: {}", command);

            Process process;
            try {
                process = processBuilder.start();
            } catch (java.io.IOException e) {
                String pythonCmd = command.get(0);
                if (e.getMessage().contains("CreateProcess error=3") ||
                    e.getMessage().contains("Cannot run program") ||
                    e.getMessage().contains("No such file")) {
                    throw new FunctionExecutionException(
                        "Python interpreter not found. Please ensure Python is installed and available in PATH. " +
                        "Attempted command: " + pythonCmd,
                        "To fix this issue:\n" +
                        "1. Install Python 3.10+ from https://python.org\n" +
                        "2. During installation, check 'Add Python to PATH'\n" +
                        "3. Or set PYTHON_HOME environment variable\n" +
                        "4. Restart the application after installation"
                    );
                }
                throw e;
            }

            // Capture output and errors
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Future<String> stdoutFuture = executor.submit(() -> readStream(process.getInputStream()));
            Future<String> stderrFuture = executor.submit(() -> readStream(process.getErrorStream()));

            boolean completed;
            try {
                completed = process.waitFor(function.getTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                process.destroyForcibly();
                throw new FunctionExecutionException("Function execution interrupted", e);
            }

            if (!completed) {
                process.destroyForcibly();
                throw new FunctionExecutionException("Function execution timeout after " + function.getTimeoutSeconds() + " seconds");
            }

            String stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(1, TimeUnit.SECONDS);
            executor.shutdown();

            int exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;

            if (exitCode != 0) {
                String errorMessage = "Python function failed with exit code " + exitCode;
                String errorDetails = stderr.isEmpty() ? stdout : stderr;
                throw new FunctionExecutionException(errorMessage, errorDetails);
            }

            // Read output
            JsonNode output = null;
            if (Files.exists(outputPath)) {
                output = objectMapper.readTree(outputPath.toFile());
            }

            return FunctionExecutionResult.builder()
                .success(true)
                .output(output)
                .durationMs(duration)
                .memoryUsedMb(estimateMemoryUsage())
                .build();

        } catch (FunctionExecutionException e) {
            long duration = System.currentTimeMillis() - startTime;
            return FunctionExecutionResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .errorStack(e.getErrorStack())
                .durationMs(duration)
                .memoryUsedMb(estimateMemoryUsage())
                .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error executing Python function: {}", e.getMessage(), e);
            return FunctionExecutionResult.builder()
                .success(false)
                .errorMessage("Execution error: " + e.getMessage())
                .errorStack(getStackTrace(e))
                .durationMs(duration)
                .memoryUsedMb(estimateMemoryUsage())
                .build();
        } finally {
            // Cleanup temp directory
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir.toFile());
                } catch (Exception e) {
                    logger.warn("Failed to cleanup temp directory: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Create Python wrapper that handles input/output and calls user function.
     */
    private String createPythonWrapper(String userCode, String handlerName) {
        return String.format("""
            import json
            import sys
            import traceback

            # User code
            %s

            def __sensorvision_wrapper():
                try:
                    # Read input
                    with open(sys.argv[1], 'r') as f:
                        event = json.load(f)

                    # Call user function
                    result = %s(event)

                    # Write output
                    with open(sys.argv[2], 'w') as f:
                        json.dump(result, f)

                    sys.exit(0)
                except Exception as e:
                    print(f"Error: {str(e)}", file=sys.stderr)
                    traceback.print_exc(file=sys.stderr)
                    sys.exit(1)

            if __name__ == '__main__':
                __sensorvision_wrapper()
            """, userCode, handlerName);
    }

    private String readStream(java.io.InputStream inputStream) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    private int estimateMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        return (int) usedMemory;
    }

    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
