package io.sensorvision.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.sensorvision.sdk.exception.*;
import io.sensorvision.sdk.model.ClientConfig;
import io.sensorvision.sdk.model.IngestionResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main client for sending telemetry data to SensorVision platform.
 *
 * <p>Example usage:
 * <pre>{@code
 * SensorVisionClient client = new SensorVisionClient.Builder()
 *     .apiUrl("http://localhost:8080")
 *     .apiKey("your-device-token")
 *     .build();
 *
 * Map<String, Object> data = Map.of(
 *     "temperature", 23.5,
 *     "humidity", 65.2
 * );
 *
 * IngestionResponse response = client.sendData("device-001", data);
 * System.out.println(response.getMessage());
 * }</pre>
 *
 * @author SensorVision Team
 * @version 0.1.0
 */
public class SensorVisionClient {
    private static final Logger logger = LoggerFactory.getLogger(SensorVisionClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ClientConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private SensorVisionClient(ClientConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofMillis(config.getTimeout()))
            .readTimeout(Duration.ofMillis(config.getTimeout()))
            .writeTimeout(Duration.ofMillis(config.getTimeout()))
            .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Send telemetry data for a device.
     *
     * @param deviceId Unique identifier for the device
     * @param data Map of variable names to numeric/boolean values
     * @return IngestionResponse containing success message and metadata
     * @throws ValidationException if deviceId or data is invalid
     * @throws AuthenticationException if API key is invalid (401)
     * @throws NetworkException if network request fails
     * @throws RateLimitException if rate limit is exceeded (429)
     * @throws ServerException if server returns 5xx error
     */
    public IngestionResponse sendData(String deviceId, Map<String, Object> data)
            throws SensorVisionException {
        validateDeviceId(deviceId);
        validateTelemetryData(data);

        return sendDataWithRetry(deviceId, data, config.getRetryAttempts());
    }

    private IngestionResponse sendDataWithRetry(String deviceId, Map<String, Object> data, int attemptsLeft)
            throws SensorVisionException {
        try {
            String url = config.getApiUrl() + "/api/v1/ingest/" + deviceId;
            String jsonBody = objectMapper.writeValueAsString(data);

            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("X-API-Key", config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .build();

            logger.debug("Sending telemetry data to {} for device {}", url, deviceId);

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    logger.debug("Successfully sent data for device {}", deviceId);
                    return objectMapper.readValue(responseBody, IngestionResponse.class);
                }

                // Handle error responses
                switch (response.code()) {
                    case 400:
                        throw new ValidationException("Invalid data format: " + responseBody);
                    case 401:
                        throw new AuthenticationException("Invalid API key");
                    case 429:
                        throw new RateLimitException("Rate limit exceeded. Please slow down.");
                    case 500:
                    case 502:
                    case 503:
                    case 504:
                        if (attemptsLeft > 0) {
                            logger.warn("Server error ({}), retrying... ({} attempts left)",
                                response.code(), attemptsLeft);
                            Thread.sleep(config.getRetryDelay());
                            return sendDataWithRetry(deviceId, data, attemptsLeft - 1);
                        }
                        throw new ServerException("Server error: " + responseBody);
                    default:
                        throw new SensorVisionException("Unexpected error: " + responseBody);
                }
            }
        } catch (IOException e) {
            if (attemptsLeft > 0) {
                logger.warn("Network error, retrying... ({} attempts left)", attemptsLeft);
                try {
                    Thread.sleep(config.getRetryDelay());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new NetworkException("Request interrupted", ie);
                }
                return sendDataWithRetry(deviceId, data, attemptsLeft - 1);
            }
            throw new NetworkException("Network request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetworkException("Request interrupted", e);
        }
    }

    private void validateDeviceId(String deviceId) throws ValidationException {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new ValidationException("Device ID must be a non-empty string");
        }
        if (deviceId.length() > 255) {
            throw new ValidationException("Device ID must be less than 255 characters");
        }
    }

    private void validateTelemetryData(Map<String, Object> data) throws ValidationException {
        if (data == null || data.isEmpty()) {
            throw new ValidationException("Telemetry data cannot be empty");
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new ValidationException("Telemetry key must be a non-empty string");
            }

            Object value = entry.getValue();
            if (!(value instanceof Number || value instanceof Boolean)) {
                throw new ValidationException(
                    String.format("Telemetry value for '%s' must be numeric or boolean, got %s",
                        entry.getKey(), value.getClass().getSimpleName())
                );
            }
        }
    }

    /**
     * Builder for creating SensorVisionClient instances.
     */
    public static class Builder {
        private String apiUrl;
        private String apiKey;
        private long timeout = 30000; // 30 seconds default
        private int retryAttempts = 3;
        private long retryDelay = 1000; // 1 second default

        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder retryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
            return this;
        }

        public Builder retryDelay(long retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public SensorVisionClient build() {
            if (apiUrl == null || apiUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("API URL is required");
            }
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("API key is required");
            }

            ClientConfig config = new ClientConfig(apiUrl, apiKey, timeout, retryAttempts, retryDelay);
            return new SensorVisionClient(config);
        }
    }
}
