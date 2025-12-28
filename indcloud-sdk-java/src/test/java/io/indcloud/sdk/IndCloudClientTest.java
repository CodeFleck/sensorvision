package io.indcloud.sdk;

import io.indcloud.sdk.exception.*;
import io.indcloud.sdk.model.IngestionResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndCloudClientTest {
    private MockWebServer mockWebServer;
    private IndCloudClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        client = new IndCloudClient.Builder()
            .apiUrl(mockWebServer.url("/").toString().replaceAll("/$", ""))
            .apiKey("test-api-key")
            .retryAttempts(0) // Disable retries for testing
            .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should successfully send telemetry data")
    void testSendDataSuccess() throws Exception {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"message\":\"Data ingested successfully\",\"deviceId\":\"test-001\",\"timestamp\":\"2024-01-01T12:00:00Z\"}")
            .addHeader("Content-Type", "application/json"));

        Map<String, Object> data = new HashMap<>();
        data.put("temperature", 23.5);
        data.put("humidity", 65.2);

        // When
        IngestionResponse response = client.sendData("test-001", data);

        // Then
        assertNotNull(response);
        assertEquals("Data ingested successfully", response.getMessage());
        assertEquals("test-001", response.getDeviceId());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/api/v1/ingest/test-001", request.getPath());
        assertEquals("test-api-key", request.getHeader("X-API-Key"));
        assertTrue(request.getBody().readUtf8().contains("temperature"));
    }

    @Test
    @DisplayName("Should throw ValidationException for empty device ID")
    void testValidateDeviceIdEmpty() {
        Map<String, Object> data = Map.of("temperature", 23.5);

        assertThrows(ValidationException.class, () -> client.sendData("", data));
        assertThrows(ValidationException.class, () -> client.sendData(null, data));
    }

    @Test
    @DisplayName("Should throw ValidationException for empty telemetry data")
    void testValidateTelemetryDataEmpty() {
        assertThrows(ValidationException.class, () -> client.sendData("test-001", null));
        assertThrows(ValidationException.class, () -> client.sendData("test-001", new HashMap<>()));
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid telemetry value types")
    void testValidateTelemetryDataInvalidTypes() {
        Map<String, Object> data = new HashMap<>();
        data.put("temperature", "not-a-number");

        assertThrows(ValidationException.class, () -> client.sendData("test-001", data));
    }

    @Test
    @DisplayName("Should throw AuthenticationException for 401 response")
    void testAuthenticationError() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody("Invalid API key"));

        Map<String, Object> data = Map.of("temperature", 23.5);

        assertThrows(AuthenticationException.class, () -> client.sendData("test-001", data));
    }

    @Test
    @DisplayName("Should throw RateLimitException for 429 response")
    void testRateLimitError() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(429)
            .setBody("Rate limit exceeded"));

        Map<String, Object> data = Map.of("temperature", 23.5);

        assertThrows(RateLimitException.class, () -> client.sendData("test-001", data));
    }

    @Test
    @DisplayName("Should throw ServerException for 500 response")
    void testServerError() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal server error"));

        Map<String, Object> data = Map.of("temperature", 23.5);

        assertThrows(ServerException.class, () -> client.sendData("test-001", data));
    }

    @Test
    @DisplayName("Should accept boolean values in telemetry data")
    void testBooleanValues() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"message\":\"Data ingested successfully\",\"deviceId\":\"test-001\",\"timestamp\":\"2024-01-01T12:00:00Z\"}")
            .addHeader("Content-Type", "application/json"));

        Map<String, Object> data = new HashMap<>();
        data.put("is_online", true);
        data.put("alarm_status", false);

        IngestionResponse response = client.sendData("test-001", data);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Builder should throw exception for missing API URL")
    void testBuilderMissingApiUrl() {
        assertThrows(IllegalArgumentException.class, () ->
            new IndCloudClient.Builder()
                .apiKey("test-key")
                .build()
        );
    }

    @Test
    @DisplayName("Builder should throw exception for missing API key")
    void testBuilderMissingApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
            new IndCloudClient.Builder()
                .apiUrl("http://localhost:8080")
                .build()
        );
    }
}
