package io.indcloud.sdk.exception;

/**
 * Thrown when network request fails.
 */
public class NetworkException extends IndCloudException {
    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
