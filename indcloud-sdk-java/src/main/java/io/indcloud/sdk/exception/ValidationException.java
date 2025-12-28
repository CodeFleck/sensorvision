package io.indcloud.sdk.exception;

/**
 * Thrown when input validation fails.
 */
public class ValidationException extends IndCloudException {
    public ValidationException(String message) {
        super(message);
    }
}
