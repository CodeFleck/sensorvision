package io.indcloud.sdk.exception;

/**
 * Thrown when authentication fails (invalid API key).
 */
public class AuthenticationException extends IndCloudException {
    public AuthenticationException(String message) {
        super(message);
    }
}
