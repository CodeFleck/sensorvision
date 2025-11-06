package io.sensorvision.sdk.model;

/**
 * Configuration for SensorVisionClient.
 */
public class ClientConfig {
    private final String apiUrl;
    private final String apiKey;
    private final long timeout;
    private final int retryAttempts;
    private final long retryDelay;

    public ClientConfig(String apiUrl, String apiKey, long timeout, int retryAttempts, long retryDelay) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.retryAttempts = retryAttempts;
        this.retryDelay = retryDelay;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public long getTimeout() {
        return timeout;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public long getRetryDelay() {
        return retryDelay;
    }
}
