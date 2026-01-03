package io.indcloud.controller;

import io.indcloud.dto.ml.MLAnomalyResolveRequest;
import io.indcloud.dto.ml.MLAnomalyResponse;
import io.indcloud.model.*;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.ml.MLAnomalyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MLAnomalyController.
 * Tests REST API endpoints for ML anomaly management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MLAnomalyController Tests")
class MLAnomalyControllerTest {

    @Mock
    private MLAnomalyService mlAnomalyService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private MLAnomalyController mlAnomalyController;

    private Organization testOrganization;
    private User testUser;
    private Device testDevice;
    private MLAnomaly testAnomaly;
    private UUID anomalyId;
    private UUID deviceId;
    private Long orgId = 1L;
    private Long userId = 100L;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(orgId)
                .name("Test Organization")
                .build();

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setOrganization(testOrganization);

        deviceId = UUID.randomUUID();
        testDevice = new Device();
        testDevice.setId(deviceId);
        testDevice.setName("Test Device");
        testDevice.setOrganization(testOrganization);

        anomalyId = UUID.randomUUID();
        testAnomaly = MLAnomaly.builder()
                .id(anomalyId)
                .device(testDevice)
                .organization(testOrganization)
                .anomalyScore(new BigDecimal("0.85"))
                .severity(MLAnomalySeverity.HIGH)
                .status(MLAnomalyStatus.NEW)
                .anomalyType("POINT_ANOMALY")
                .affectedVariables(List.of("temperature", "pressure"))
                .detectedAt(Instant.now())
                .build();

        lenient().when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        lenient().when(securityUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("GET /api/v1/ml/anomalies")
    class GetAnomaliesTests {

        @Test
        @DisplayName("Should return paginated list of anomalies")
        void shouldReturnPaginatedAnomalies() {
            // Given
            Page<MLAnomaly> anomalyPage = new PageImpl<>(List.of(testAnomaly));
            Pageable pageable = PageRequest.of(0, 20);

            when(mlAnomalyService.getAnomalies(orgId, pageable)).thenReturn(anomalyPage);

            // When
            Page<MLAnomalyResponse> response = mlAnomalyController.getAnomalies(pageable, null, null);

            // Then
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent().get(0).getSeverity()).isEqualTo(MLAnomalySeverity.HIGH);
            verify(mlAnomalyService).getAnomalies(orgId, pageable);
        }

        @Test
        @DisplayName("Should filter by status")
        void shouldFilterByStatus() {
            // Given
            Page<MLAnomaly> anomalyPage = new PageImpl<>(List.of(testAnomaly));
            Pageable pageable = PageRequest.of(0, 20);
            when(mlAnomalyService.getAnomaliesByStatus(orgId, MLAnomalyStatus.NEW, pageable))
                    .thenReturn(anomalyPage);

            // When
            Page<MLAnomalyResponse> response = mlAnomalyController.getAnomalies(pageable, MLAnomalyStatus.NEW, null);

            // Then
            assertThat(response.getTotalElements()).isEqualTo(1);
            verify(mlAnomalyService).getAnomaliesByStatus(orgId, MLAnomalyStatus.NEW, pageable);
        }

        @Test
        @DisplayName("Should filter by severity")
        void shouldFilterBySeverity() {
            // Given
            Page<MLAnomaly> anomalyPage = new PageImpl<>(List.of(testAnomaly));
            Pageable pageable = PageRequest.of(0, 20);
            when(mlAnomalyService.getAnomaliesBySeverity(orgId, MLAnomalySeverity.HIGH, pageable))
                    .thenReturn(anomalyPage);

            // When
            Page<MLAnomalyResponse> response = mlAnomalyController.getAnomalies(pageable, null, MLAnomalySeverity.HIGH);

            // Then
            assertThat(response.getTotalElements()).isEqualTo(1);
            verify(mlAnomalyService).getAnomaliesBySeverity(orgId, MLAnomalySeverity.HIGH, pageable);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/anomalies/device/{deviceId}")
    class GetDeviceAnomaliesTests {

        @Test
        @DisplayName("Should return anomalies for device")
        void shouldReturnDeviceAnomalies() {
            // Given
            Page<MLAnomaly> anomalyPage = new PageImpl<>(List.of(testAnomaly));
            Pageable pageable = PageRequest.of(0, 20);

            when(mlAnomalyService.getAnomaliesByDevice(deviceId, orgId, pageable)).thenReturn(anomalyPage);

            // When
            Page<MLAnomalyResponse> response = mlAnomalyController.getDeviceAnomalies(deviceId, pageable);

            // Then
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent().get(0).getDeviceId()).isEqualTo(deviceId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/anomalies/{id}")
    class GetAnomalyByIdTests {

        @Test
        @DisplayName("Should return anomaly by ID")
        void shouldReturnAnomalyById() {
            // Given
            when(mlAnomalyService.getAnomaly(anomalyId, orgId)).thenReturn(Optional.of(testAnomaly));

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.getAnomaly(anomalyId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(anomalyId);
        }

        @Test
        @DisplayName("Should return 404 when anomaly not found")
        void shouldReturn404WhenNotFound() {
            // Given
            when(mlAnomalyService.getAnomaly(anomalyId, orgId)).thenReturn(Optional.empty());

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.getAnomaly(anomalyId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/anomalies/new")
    class GetNewAnomaliesTests {

        @Test
        @DisplayName("Should return new anomalies")
        void shouldReturnNewAnomalies() {
            // Given
            when(mlAnomalyService.getNewAnomalies(orgId)).thenReturn(List.of(testAnomaly));

            // When
            List<MLAnomalyResponse> response = mlAnomalyController.getNewAnomalies();

            // Then
            assertThat(response).hasSize(1);
            assertThat(response.get(0).getStatus()).isEqualTo(MLAnomalyStatus.NEW);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/anomalies/critical")
    class GetCriticalAnomaliesTests {

        @Test
        @DisplayName("Should return critical anomalies")
        void shouldReturnCriticalAnomalies() {
            // Given
            testAnomaly.setSeverity(MLAnomalySeverity.CRITICAL);
            when(mlAnomalyService.getCriticalAnomalies(orgId)).thenReturn(List.of(testAnomaly));

            // When
            List<MLAnomalyResponse> response = mlAnomalyController.getCriticalAnomalies();

            // Then
            assertThat(response).hasSize(1);
            assertThat(response.get(0).getSeverity()).isEqualTo(MLAnomalySeverity.CRITICAL);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/anomalies/{id}/acknowledge")
    class AcknowledgeAnomalyTests {

        @Test
        @DisplayName("Should acknowledge anomaly")
        void shouldAcknowledgeAnomaly() {
            // Given
            testAnomaly.setStatus(MLAnomalyStatus.ACKNOWLEDGED);
            testAnomaly.setAcknowledgedBy(userId);
            testAnomaly.setAcknowledgedAt(Instant.now());

            when(mlAnomalyService.acknowledgeAnomaly(anomalyId, orgId, userId)).thenReturn(testAnomaly);

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.acknowledgeAnomaly(anomalyId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo(MLAnomalyStatus.ACKNOWLEDGED);
            assertThat(response.getBody().getAcknowledgedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should return 404 when anomaly not found")
        void shouldReturn404WhenNotFound() {
            // Given
            when(mlAnomalyService.acknowledgeAnomaly(anomalyId, orgId, userId))
                    .thenThrow(new IllegalArgumentException("Anomaly not found"));

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.acknowledgeAnomaly(anomalyId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should return 400 when acknowledging non-new anomaly")
        void shouldReturn400WhenNotNew() {
            // Given
            when(mlAnomalyService.acknowledgeAnomaly(anomalyId, orgId, userId))
                    .thenThrow(new IllegalStateException("Can only acknowledge NEW anomalies"));

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.acknowledgeAnomaly(anomalyId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/anomalies/{id}/investigate")
    class StartInvestigationTests {

        @Test
        @DisplayName("Should start investigation")
        void shouldStartInvestigation() {
            // Given
            testAnomaly.setStatus(MLAnomalyStatus.INVESTIGATING);
            when(mlAnomalyService.startInvestigation(anomalyId, orgId)).thenReturn(testAnomaly);

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.startInvestigation(anomalyId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo(MLAnomalyStatus.INVESTIGATING);
        }

        @Test
        @DisplayName("Should return 400 when investigating closed anomaly")
        void shouldReturn400WhenClosed() {
            // Given
            when(mlAnomalyService.startInvestigation(anomalyId, orgId))
                    .thenThrow(new IllegalStateException("Cannot investigate closed anomaly"));

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.startInvestigation(anomalyId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/anomalies/{id}/resolve")
    class ResolveAnomalyTests {

        @Test
        @DisplayName("Should resolve anomaly")
        void shouldResolveAnomaly() {
            // Given
            testAnomaly.setStatus(MLAnomalyStatus.RESOLVED);
            testAnomaly.setResolvedBy(userId);
            testAnomaly.setResolvedAt(Instant.now());
            testAnomaly.setResolutionNote("Fixed the sensor");

            MLAnomalyResolveRequest request = new MLAnomalyResolveRequest();
            request.setResolutionNote("Fixed the sensor");

            when(mlAnomalyService.resolveAnomaly(anomalyId, orgId, userId, "Fixed the sensor"))
                    .thenReturn(testAnomaly);

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.resolveAnomaly(anomalyId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo(MLAnomalyStatus.RESOLVED);
            assertThat(response.getBody().getResolutionNote()).isEqualTo("Fixed the sensor");
        }

        @Test
        @DisplayName("Should resolve anomaly without note")
        void shouldResolveAnomalyWithoutNote() {
            // Given
            testAnomaly.setStatus(MLAnomalyStatus.RESOLVED);
            testAnomaly.setResolvedBy(userId);

            when(mlAnomalyService.resolveAnomaly(anomalyId, orgId, userId, null)).thenReturn(testAnomaly);

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.resolveAnomaly(anomalyId, null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should return 400 when already resolved")
        void shouldReturn400WhenAlreadyResolved() {
            // Given
            when(mlAnomalyService.resolveAnomaly(anomalyId, orgId, userId, null))
                    .thenThrow(new IllegalStateException("Anomaly is already closed"));

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.resolveAnomaly(anomalyId, null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/anomalies/{id}/false-positive")
    class MarkFalsePositiveTests {

        @Test
        @DisplayName("Should mark as false positive")
        void shouldMarkFalsePositive() {
            // Given
            testAnomaly.setStatus(MLAnomalyStatus.FALSE_POSITIVE);
            testAnomaly.setResolvedBy(userId);
            testAnomaly.setResolutionNote("Sensor noise");

            MLAnomalyResolveRequest request = new MLAnomalyResolveRequest();
            request.setResolutionNote("Sensor noise");

            when(mlAnomalyService.markFalsePositive(anomalyId, orgId, userId, "Sensor noise"))
                    .thenReturn(testAnomaly);

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.markFalsePositive(anomalyId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo(MLAnomalyStatus.FALSE_POSITIVE);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/anomalies/{id}/link-alert/{alertId}")
    class LinkToAlertTests {

        @Test
        @DisplayName("Should link anomaly to alert")
        void shouldLinkToAlert() {
            // Given
            UUID alertId = UUID.randomUUID();
            testAnomaly.setGlobalAlertId(alertId);

            when(mlAnomalyService.linkToAlert(anomalyId, orgId, alertId)).thenReturn(testAnomaly);

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.linkToAlert(anomalyId, alertId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getGlobalAlertId()).isEqualTo(alertId);
        }

        @Test
        @DisplayName("Should return 404 when anomaly not found")
        void shouldReturn404WhenNotFound() {
            // Given
            UUID alertId = UUID.randomUUID();
            when(mlAnomalyService.linkToAlert(anomalyId, orgId, alertId))
                    .thenThrow(new IllegalArgumentException("Anomaly not found"));

            // When
            ResponseEntity<MLAnomalyResponse> response = mlAnomalyController.linkToAlert(anomalyId, alertId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/anomalies/stats")
    class GetAnomalyStatsTests {

        @Test
        @DisplayName("Should return anomaly statistics")
        void shouldReturnAnomalyStats() {
            // Given
            when(mlAnomalyService.countNewAnomalies(orgId)).thenReturn(5L);
            when(mlAnomalyService.getAnomalyCountsBySeverity(eq(orgId), any()))
                    .thenReturn(Map.of(
                            MLAnomalySeverity.CRITICAL, 2L,
                            MLAnomalySeverity.HIGH, 3L,
                            MLAnomalySeverity.MEDIUM, 10L
                    ));

            // When
            MLAnomalyController.AnomalyStatsResponse response = mlAnomalyController.getAnomalyStats(24);

            // Then
            assertThat(response.newCount()).isEqualTo(5L);
            assertThat(response.bySeverity()).containsEntry(MLAnomalySeverity.CRITICAL, 2L);
            assertThat(response.periodHours()).isEqualTo(24);
        }
    }
}
