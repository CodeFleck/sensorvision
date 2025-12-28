package io.indcloud.sdk.exception;

/**
 * Base exception for all Industrial Cloud SDK errors.
 */
public class IndCloudException extends Exception {
    public IndCloudException(String message) {
        super(message);
    }

    public IndCloudException(String message, Throwable cause) {
        super(message, cause);
    }
}
