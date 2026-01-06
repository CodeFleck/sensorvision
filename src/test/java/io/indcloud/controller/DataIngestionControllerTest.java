package io.indcloud.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.dto.HttpTelemetryRequest;
import io.indcloud.dto.HttpTelemetryResponse;
import io.indcloud.service.TelemetryIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataIngestionController.
 * Tests HTTP telemetry ingestion endpoints including validation and error handling.
 */
@ExtendWith(MockitoExtension.class)
class DataIngestionControllerTest {

    @Mock
    private TelemetryIngestionService telemetryIngestionService;

    @InjectMocks
    private DataIngestionController dataIngestionController;

    private HttpTelemetryRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new HttpTelemetryRequest(
                "device-001",
                "2024-01-15T10:30:00Z",
                Map.of("temperature", new BigDecimal("23.5")),
                Map.of("location", "Building A")
        );
    }

    // ===== SINGLE INGESTION TESTS =====

    @Test
    void ingestTelemetry_withValidRequest_shouldSucceed() {
        // Given
        doNothing().when(telemetryIngestionService).ingest(any());

        // When
        ResponseEntity<HttpTelemetryResponse> response = dataIngestionController.ingestTelemetry(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("success");
        assertThat(response.getBody().deviceId()).isEqualTo("device-001");
        verify(telemetryIngestionService).ingest(any());
    }

    @Test
    void ingestTelemetry_withNullDeviceId_shouldReturnBadRequest() {
        // Given
        HttpTelemetryRequest invalidRequest = new HttpTelemetryRequest(
                null, // null deviceId
                "2024-01-15T10:30:00Z",
                Map.of("temperature", new BigDecimal("23.5")),
                null
        );

        // When
        ResponseEntity<HttpTelemetryResponse> response = dataIngestionController.ingestTelemetry(invalidRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).contains("deviceId is required");
        verify(telemetryIngestionService, never()).ingest(any());
    }

    @Test
    void ingestTelemetry_withEmptyVariables_shouldReturnBadRequest() {
        // Given
        HttpTelemetryRequest invalidRequest = new HttpTelemetryRequest(
                "device-001",
                "2024-01-15T10:30:00Z",
                Map.of(), // empty variables
                null
        );

        // When
        ResponseEntity<HttpTelemetryResponse> response = dataIngestionController.ingestTelemetry(invalidRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).contains("variables are required");
    }

    @Test
    void ingestTelemetry_withInvalidTimestamp_shouldReturnBadRequest() {
        // Given
        HttpTelemetryRequest invalidRequest = new HttpTelemetryRequest(
                "device-001",
                "not-a-valid-timestamp",
                Map.of("temperature", new BigDecimal("23.5")),
                null
        );

        // When
        ResponseEntity<HttpTelemetryResponse> response = dataIngestionController.ingestTelemetry(invalidRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).contains("Invalid timestamp format");
    }

    @Test
    void ingestTelemetry_withNoTimestamp_shouldUseCurrentTime() {
        // Given
        HttpTelemetryRequest requestWithoutTimestamp = new HttpTelemetryRequest(
                "device-001",
                null, // no timestamp
                Map.of("temperature", new BigDecimal("23.5")),
                null
        );
        doNothing().when(telemetryIngestionService).ingest(any());

        // When
        ResponseEntity<HttpTelemetryResponse> response = dataIngestionController.ingestTelemetry(requestWithoutTimestamp);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("success");
        assertThat(response.getBody().timestamp()).isNotNull();
        verify(telemetryIngestionService).ingest(any());
    }

    // ===== BULK INGESTION TESTS =====

    @Test
    void bulkIngest_withNullRequestBody_shouldReturnBadRequest() {
        // Given - Test for bug fix: NPE when request body is null

        // When
        ResponseEntity<List<HttpTelemetryResponse>> response = dataIngestionController.bulkIngest(null);

        // Then - Should not throw NPE, should return error response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).status()).isEqualTo("error");
        assertThat(response.getBody().get(0).message()).contains("cannot be null");
    }

    @Test
    void bulkIngest_withValidRequests_shouldSucceed() {
        // Given
        HttpTelemetryRequest request1 = new HttpTelemetryRequest(
                "device-001",
                "2024-01-15T10:30:00Z",
                Map.of("temperature", new BigDecimal("23.5")),
                null
        );
        HttpTelemetryRequest request2 = new HttpTelemetryRequest(
                "device-002",
                "2024-01-15T10:31:00Z",
                Map.of("humidity", new BigDecimal("65.0")),
                null
        );

        doNothing().when(telemetryIngestionService).ingest(any());

        // When
        ResponseEntity<List<HttpTelemetryResponse>> response = dataIngestionController.bulkIngest(
                List.of(request1, request2));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).allMatch(r -> "success".equals(r.status()));
        verify(telemetryIngestionService, times(2)).ingest(any());
    }

    @Test
    void bulkIngest_withMixedValidAndInvalidRequests_shouldProcessAll() {
        // Given
        HttpTelemetryRequest validReq = new HttpTelemetryRequest(
                "device-001",
                "2024-01-15T10:30:00Z",
                Map.of("temperature", new BigDecimal("23.5")),
                null
        );
        HttpTelemetryRequest invalidReq = new HttpTelemetryRequest(
                null, // invalid - no deviceId
                "2024-01-15T10:31:00Z",
                null, // invalid - no variables
                null
        );

        doNothing().when(telemetryIngestionService).ingest(any());

        // When
        ResponseEntity<List<HttpTelemetryResponse>> response = dataIngestionController.bulkIngest(
                List.of(validReq, invalidReq));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);

        // First request should succeed
        assertThat(response.getBody().get(0).status()).isEqualTo("success");

        // Second request should fail
        assertThat(response.getBody().get(1).status()).isEqualTo("error");

        // Only valid request should call ingestion service
        verify(telemetryIngestionService, times(1)).ingest(any());
    }

    @Test
    void bulkIngest_withEmptyList_shouldReturnEmptyResponse() {
        // When
        ResponseEntity<List<HttpTelemetryResponse>> response = dataIngestionController.bulkIngest(List.of());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
        verify(telemetryIngestionService, never()).ingest(any());
    }

    // ===== SINGLE VARIABLE INGESTION TESTS =====

    @Test
    void ingestSingleVariable_withValidInput_shouldSucceed() {
        // Given
        doNothing().when(telemetryIngestionService).ingest(any());

        // When
        ResponseEntity<HttpTelemetryResponse> response = dataIngestionController.ingestSingleVariable(
                "device-001", "temperature", new BigDecimal("23.5"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("success");
        assertThat(response.getBody().deviceId()).isEqualTo("device-001");
        verify(telemetryIngestionService).ingest(any());
    }

    @Test
    void ingestSingleVariable_withNullValue_shouldReturnBadRequest() {
        // When
        ResponseEntity<HttpTelemetryResponse> response = dataIngestionController.ingestSingleVariable(
                "device-001", "temperature", null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).contains("value is required");
    }
}
