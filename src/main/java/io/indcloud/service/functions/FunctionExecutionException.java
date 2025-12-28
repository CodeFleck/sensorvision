package io.indcloud.service.functions;

/**
 * Exception thrown when function execution fails.
 */
public class FunctionExecutionException extends Exception {

    private final String errorStack;

    public FunctionExecutionException(String message) {
        super(message);
        this.errorStack = null;
    }

    public FunctionExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.errorStack = getStackTraceAsString(cause);
    }

    public FunctionExecutionException(String message, String errorStack) {
        super(message);
        this.errorStack = errorStack;
    }

    public String getErrorStack() {
        return errorStack;
    }

    private static String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) return null;
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
