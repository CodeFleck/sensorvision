package io.indcloud.service.functions;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Result of a function execution.
 */
public class FunctionExecutionResult {
    private final boolean success;
    private final JsonNode output;
    private final String errorMessage;
    private final String errorStack;
    private final long durationMs;
    private final int memoryUsedMb;

    private FunctionExecutionResult(Builder builder) {
        this.success = builder.success;
        this.output = builder.output;
        this.errorMessage = builder.errorMessage;
        this.errorStack = builder.errorStack;
        this.durationMs = builder.durationMs;
        this.memoryUsedMb = builder.memoryUsedMb;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() {
        return success;
    }

    public JsonNode getOutput() {
        return output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorStack() {
        return errorStack;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getMemoryUsedMb() {
        return memoryUsedMb;
    }

    public static class Builder {
        private boolean success;
        private JsonNode output;
        private String errorMessage;
        private String errorStack;
        private long durationMs;
        private int memoryUsedMb;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder output(JsonNode output) {
            this.output = output;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder errorStack(String errorStack) {
            this.errorStack = errorStack;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder memoryUsedMb(int memoryUsedMb) {
            this.memoryUsedMb = memoryUsedMb;
            return this;
        }

        public FunctionExecutionResult build() {
            return new FunctionExecutionResult(this);
        }
    }
}
