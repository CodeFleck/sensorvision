package io.sensorvision.sdk.exception;

/**
 * Thrown when authentication fails (invalid API key).
 */
public class AuthenticationException extends SensorVisionException {
    public AuthenticationException(String message) {
        super(message);
    }
}
