package io.sensorvision.sdk.exception;

/**
 * Thrown when server returns 5xx error.
 */
public class ServerException extends SensorVisionException {
    public ServerException(String message) {
        super(message);
    }
}
