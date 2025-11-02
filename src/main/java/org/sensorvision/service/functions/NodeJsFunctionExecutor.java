package org.sensorvision.service.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sensorvision.model.FunctionRuntime;
import org.sensorvision.model.ServerlessFunction;
import org.sensorvision.service.FunctionSecretsService;
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
 * Executor for Node.js 18+ functions.
 * Executes JavaScript code in a separate process with resource limits.
 */
@Component
public class NodeJsFunctionExecutor implements FunctionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(NodeJsFunctionExecutor.class);
    private final ObjectMapper objectMapper;
    private final FunctionSecretsService secretsService;
    private static Boolean nodeAvailable = null;

    public NodeJsFunctionExecutor(ObjectMapper objectMapper, FunctionSecretsService secretsService) {
        this.objectMapper = objectMapper;
        this.secretsService = secretsService;
    }

    @Override
    public boolean supports(FunctionRuntime runtime) {
        return runtime == FunctionRuntime.NODEJS_18;
    }

    @Override
    public FunctionExecutionResult execute(ServerlessFunction function, JsonNode input) throws FunctionExecutionException {
        long startTime = System.currentTimeMillis();

        // Check if Node.js is available
        if (!isNodeJsAvailable()) {
            return FunctionExecutionResult.builder()
                .success(false)
                .errorMessage("Node.js is not installed or not available in PATH. " +
                    "Please install Node.js 18 or later to use NODEJS_18 runtime.")
                .durationMs(System.currentTimeMillis() - startTime)
                .memoryUsedMb(0)
                .build();
        }
        Path tempDir = null;

        try {
            // Create temporary directory for function execution
            tempDir = Files.createTempDirectory("sensorvision-func-" + UUID.randomUUID());
            Path functionPath = tempDir.resolve("function.js");
            Path wrapperPath = tempDir.resolve("wrapper.js");
            Path inputPath = tempDir.resolve("input.json");
            Path outputPath = tempDir.resolve("output.json");

            // Write user function code to file
            try (FileWriter writer = new FileWriter(functionPath.toFile())) {
                writer.write(createUserFunction(function.getCode(), function.getHandler()));
            }

            // Write wrapper code to file
            try (FileWriter writer = new FileWriter(wrapperPath.toFile())) {
                writer.write(createNodeWrapper(functionPath.toString(), inputPath.toString(), outputPath.toString()));
            }

            // Write input data to file
            try (FileWriter writer = new FileWriter(inputPath.toFile())) {
                objectMapper.writeValue(writer, input);
            }

            // Build Node.js command
            List<String> command = new ArrayList<>();
            command.add(getNodeCommand());
            command.add(wrapperPath.toString());

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

            Process process = processBuilder.start();

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
                String errorMessage = "Node.js function failed with exit code " + exitCode;
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
            logger.error("Error executing Node.js function: {}", e.getMessage(), e);
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
     * Create user function module.
     */
    private String createUserFunction(String userCode, String handlerName) {
        return String.format("""
            // User code
            %s

            // Export handler
            module.exports = { handler: %s };
            """, userCode, handlerName);
    }

    /**
     * Create Node.js wrapper that handles input/output and calls user function.
     */
    private String createNodeWrapper(String functionPath, String inputPath, String outputPath) {
        return String.format("""
            const fs = require('fs');

            async function __sensorvision_wrapper() {
                try {
                    // Read input
                    const event = JSON.parse(fs.readFileSync('%s', 'utf8'));

                    // Load user function
                    const userModule = require('%s');

                    // Call handler (support both sync and async)
                    const result = await Promise.resolve(userModule.handler(event));

                    // Write output
                    fs.writeFileSync('%s', JSON.stringify(result));

                    process.exit(0);
                } catch (error) {
                    console.error('Error:', error.message);
                    if (error.stack) {
                        console.error(error.stack);
                    }
                    process.exit(1);
                }
            }

            __sensorvision_wrapper();
            """,
            inputPath.replace("\\", "\\\\"),
            functionPath.replace("\\", "\\\\"),
            outputPath.replace("\\", "\\\\")
        );
    }

    /**
     * Check if Node.js is available in the system PATH.
     */
    private boolean isNodeJsAvailable() {
        if (nodeAvailable != null) {
            return nodeAvailable;
        }

        try {
            Process process = new ProcessBuilder(getNodeCommand(), "--version")
                .redirectErrorStream(true)
                .start();

            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (completed && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String version = reader.readLine();
                    logger.info("Node.js detected: {}", version);
                    nodeAvailable = true;
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("Node.js not found in PATH: {}", e.getMessage());
        }

        nodeAvailable = false;
        return false;
    }

    /**
     * Get the Node.js command for the current platform.
     */
    private String getNodeCommand() {
        // Try 'node' first (common on all platforms)
        return "node";
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
