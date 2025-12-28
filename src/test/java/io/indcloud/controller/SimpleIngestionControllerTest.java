package io.indcloud.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.model.Device;
import io.indcloud.model.DeviceStatus;
import io.indcloud.model.Organization;
import io.indcloud.mqtt.TelemetryPayload;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.service.DeviceTokenService;
import io.indcloud.service.TelemetryIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SimpleIngestionController
 */
@ExtendWith(MockitoExtension.class)
class SimpleIngestionControllerTest {

    @Mock
    private TelemetryIngestionService telemetryIngestionService;

    @Mock
    private DeviceTokenService deviceTokenService;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private SimpleIngestionController controller;

    private Organization testOrganization;
    private Device authenticatedDevice;
    private Device targetDevice;
    private String validApiKey;

    @BeforeEach
    void setUp() {
        // Create test organization
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Organization");

        // Create authenticated device (the one that owns the token)
        authenticatedDevice = new Device();
        authenticatedDevice.setId(UUID.randomUUID());
        authenticatedDevice.setExternalId("auth-device-001");
        authenticatedDevice.setName("Authenticated Device");
        authenticatedDevice.setOrganization(testOrganization);
        authenticatedDevice.setStatus(DeviceStatus.ONLINE);

        // Create target device (the one receiving data)
        targetDevice = new Device();
        targetDevice.setId(UUID.randomUUID());
        targetDevice.setExternalId("target-device-001");
        targetDevice.setName("Target Device");
        targetDevice.setOrganization(testOrganization);
        targetDevice.setStatus(DeviceStatus.ONLINE);

                validApiKey = "00000000-0000-0000-0000-000000000001";
    }

    @Test
    void testIngest_Success_ExistingDeviceInSameOrganization() {
        // Given
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);
        variables.put("humidity", 65.2);

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));
        doNothing().when(deviceTokenService).updateTokenLastUsed(validApiKey);
        doNothing().when(telemetryIngestionService).ingest(any(TelemetryPayload.class));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("Data received successfully");

        // Verify interactions
        verify(deviceTokenService).isDeviceToken(validApiKey);
        verify(deviceTokenService).getDeviceByToken(validApiKey);
        verify(deviceRepository).findByExternalId(deviceId);
        verify(deviceTokenService).updateTokenLastUsed(validApiKey);
        verify(telemetryIngestionService).ingest(any(TelemetryPayload.class));
    }

    @Test
    void testIngest_Success_NewDeviceAutoCreation() {
        // Given - device doesn't exist yet, will be auto-created
        String newDeviceId = "new-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(newDeviceId)).thenReturn(Optional.empty());
        doNothing().when(deviceTokenService).updateTokenLastUsed(validApiKey);
        doNothing().when(telemetryIngestionService).ingest(any(TelemetryPayload.class));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(newDeviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();

        // Verify device lookup happened
        verify(deviceRepository).findByExternalId(newDeviceId);
        // Verify ingestion was called (which will handle auto-creation)
        verify(telemetryIngestionService).ingest(any(TelemetryPayload.class));
    }

    @Test
    void testIngest_Success_MultipleVariables() {
        // Given
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);
        variables.put("humidity", 65.2);
        variables.put("pressure", 1013.25);
        variables.put("light", 450);

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));
        doNothing().when(telemetryIngestionService).ingest(any(TelemetryPayload.class));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Capture the telemetry payload
        ArgumentCaptor<TelemetryPayload> payloadCaptor = ArgumentCaptor.forClass(TelemetryPayload.class);
        verify(telemetryIngestionService).ingest(payloadCaptor.capture());

        TelemetryPayload payload = payloadCaptor.getValue();
        assertThat(payload.deviceId()).isEqualTo(deviceId);
        assertThat(payload.variables()).hasSize(4);
        assertThat(payload.variables().get("temperature")).isEqualByComparingTo(BigDecimal.valueOf(23.5));
        assertThat(payload.variables().get("humidity")).isEqualByComparingTo(BigDecimal.valueOf(65.2));
        assertThat(payload.variables().get("pressure")).isEqualByComparingTo(BigDecimal.valueOf(1013.25));
        assertThat(payload.variables().get("light")).isEqualByComparingTo(BigDecimal.valueOf(450));
    }

    @Test
    void testIngest_Failure_MissingApiKey() {
        // Given
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, null, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("X-API-Key header is required");

        // Verify no service calls were made
        verify(deviceTokenService, never()).getDeviceByToken(any());
        verify(telemetryIngestionService, never()).ingest(any());
    }

    @Test
    void testIngest_Failure_EmptyApiKey() {
        // Given
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, "   ", variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("X-API-Key header is required");
    }

    @Test
    void testIngest_Failure_InvalidApiKeyFormat() {
        // Given
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);
        String invalidKey = "not-a-uuid-format";

        when(deviceTokenService.isDeviceToken(invalidKey)).thenReturn(false);

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, invalidKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Invalid API key format. Expected UUID.");

        verify(deviceTokenService).isDeviceToken(invalidKey);
        verify(deviceTokenService, never()).getDeviceByToken(any());
    }

    @Test
    void testIngest_Failure_InvalidApiKey() {
        // Given
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.empty());

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Invalid API key");

        verify(deviceTokenService).getDeviceByToken(validApiKey);
        verify(telemetryIngestionService, never()).ingest(any());
    }

    @Test
    void testIngest_Failure_OrganizationMismatch() {
        // Given - target device belongs to different organization
        Organization differentOrganization = new Organization();
        differentOrganization.setId(2L);
        differentOrganization.setName("Different Organization");

        Device differentOrgDevice = new Device();
        differentOrgDevice.setId(UUID.randomUUID());
        differentOrgDevice.setExternalId("other-device-001");
        differentOrgDevice.setOrganization(differentOrganization);

        String deviceId = "other-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(differentOrgDevice));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Cannot send data to device in different organization");

        // Verify token was NOT updated
        verify(deviceTokenService, never()).updateTokenLastUsed(any());
        // Verify ingestion was NOT called
        verify(telemetryIngestionService, never()).ingest(any());
    }

    @Test
    void testIngest_Failure_DeviceWithoutOrganization() {
        // Given - authenticated device has no organization (should never happen, but test defensive code)
        Device deviceWithoutOrg = new Device();
        deviceWithoutOrg.setId(UUID.randomUUID());
        deviceWithoutOrg.setExternalId("orphan-device");
        deviceWithoutOrg.setOrganization(null);  // No organization!

        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(deviceWithoutOrg));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Device configuration error");

        verify(telemetryIngestionService, never()).ingest(any());
    }

    @Test
    void testIngest_Failure_NoNumericVariables_EmptyMap() {
        // Given - completely empty variables map
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        // No variables provided at all

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("At least one numeric variable is required");

        verify(telemetryIngestionService, never()).ingest(any());
    }

    @Test
    void testIngest_Failure_NoNumericVariables_AllNullValues() {
        // Given - non-empty map but ALL values are null (edge case: becomes empty after filtering)
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", null);  // Null value
        variables.put("humidity", null);     // Null value
        variables.put("pressure", null);     // Null value
        // All values are null, so after filtering the map becomes empty

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("At least one numeric variable is required");

        // Should NOT call ingestion service - no valid data to ingest
        verify(telemetryIngestionService, never()).ingest(any());
    }

    @Test
    void testIngest_Failure_IngestionServiceException() {
        // Given
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));
        doThrow(new RuntimeException("Database error")).when(telemetryIngestionService).ingest(any(TelemetryPayload.class));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("Failed to process data");
        assertThat(response.getBody().message()).contains("Database error");
    }

    @Test
    void testIngest_Success_StringNumberConversion() {
        // Given - variables come as strings (common in JSON)
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", "23.5");  // String instead of number
        variables.put("humidity", "65.2");

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));
        doNothing().when(telemetryIngestionService).ingest(any(TelemetryPayload.class));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<TelemetryPayload> payloadCaptor = ArgumentCaptor.forClass(TelemetryPayload.class);
        verify(telemetryIngestionService).ingest(payloadCaptor.capture());

        TelemetryPayload payload = payloadCaptor.getValue();
        assertThat(payload.variables().get("temperature")).isEqualByComparingTo(BigDecimal.valueOf(23.5));
        assertThat(payload.variables().get("humidity")).isEqualByComparingTo(BigDecimal.valueOf(65.2));
    }

    @Test
    void testIngest_Success_MixedNumberTypes() {
        // Given - mix of Integer, Double, BigDecimal
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);           // Double
        variables.put("humidity", 65);                // Integer
        variables.put("pressure", new BigDecimal("1013.25"));  // BigDecimal

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));
        doNothing().when(telemetryIngestionService).ingest(any(TelemetryPayload.class));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<TelemetryPayload> payloadCaptor = ArgumentCaptor.forClass(TelemetryPayload.class);
        verify(telemetryIngestionService).ingest(payloadCaptor.capture());

        TelemetryPayload payload = payloadCaptor.getValue();
        assertThat(payload.variables()).hasSize(3);
        assertThat(payload.variables().get("temperature")).isEqualByComparingTo(BigDecimal.valueOf(23.5));
        assertThat(payload.variables().get("humidity")).isEqualByComparingTo(BigDecimal.valueOf(65));
        assertThat(payload.variables().get("pressure")).isEqualByComparingTo(new BigDecimal("1013.25"));
    }

    @Test
    void testIngest_Success_NullValuesFiltered() {
        // Given - some null values that should be filtered out
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);
        variables.put("humidity", null);  // Null value
        variables.put("pressure", 1013.25);

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));
        doNothing().when(telemetryIngestionService).ingest(any(TelemetryPayload.class));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<TelemetryPayload> payloadCaptor = ArgumentCaptor.forClass(TelemetryPayload.class);
        verify(telemetryIngestionService).ingest(payloadCaptor.capture());

        TelemetryPayload payload = payloadCaptor.getValue();
        // Only 2 variables (null one filtered out)
        assertThat(payload.variables()).hasSize(2);
        assertThat(payload.variables()).containsKeys("temperature", "pressure");
        assertThat(payload.variables()).doesNotContainKey("humidity");
    }

    @Test
    void testIngest_Failure_InvalidStringConversion() {
        // Given - invalid string that cannot be converted to number
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", "25.abc");  // Invalid number string

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("Invalid numeric value");
        assertThat(response.getBody().message()).contains("temperature");
        assertThat(response.getBody().message()).contains("25.abc");

        // Should NOT call ingestion service for bad data
        verify(telemetryIngestionService, never()).ingest(any(TelemetryPayload.class));
    }

    @Test
    void testIngest_Failure_UnsupportedDataType_Boolean() {
        // Given - boolean value (unsupported type)
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("sensor_active", true);  // Boolean - unsupported

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("Unsupported data type");
        assertThat(response.getBody().message()).contains("sensor_active");
        assertThat(response.getBody().message()).contains("Boolean");

        // Should NOT call ingestion service for bad data
        verify(telemetryIngestionService, never()).ingest(any(TelemetryPayload.class));
    }

    @Test
    void testIngest_Failure_UnsupportedDataType_List() {
        // Given - List/Array value (unsupported type)
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("readings", java.util.Arrays.asList(1, 2, 3));  // List - unsupported

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("Unsupported data type");
        assertThat(response.getBody().message()).contains("readings");

        // Should NOT call ingestion service for bad data
        verify(telemetryIngestionService, never()).ingest(any(TelemetryPayload.class));
    }

    @Test
    void testIngest_Failure_PartiallyInvalidData() {
        // Given - mix of valid and invalid data (should fail on first invalid)
        String deviceId = "target-device-001";
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", 23.5);      // Valid
        variables.put("humidity", "not-a-number");  // Invalid
        variables.put("pressure", 1013.25);      // Valid

        when(deviceTokenService.isDeviceToken(validApiKey)).thenReturn(true);
        when(deviceTokenService.getDeviceByToken(validApiKey)).thenReturn(Optional.of(authenticatedDevice));
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(targetDevice));

        // When
        ResponseEntity<SimpleIngestionController.SimpleIngestResponse> response =
                controller.ingest(deviceId, validApiKey, variables);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("Invalid numeric value");
        assertThat(response.getBody().message()).contains("humidity");

        // Should NOT call ingestion service - fail fast on invalid data
        verify(telemetryIngestionService, never()).ingest(any(TelemetryPayload.class));
    }
}
