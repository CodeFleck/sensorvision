package io.indcloud.service.ml;

import io.indcloud.model.*;
import io.indcloud.repository.MLAnomalyRepository;
import io.indcloud.repository.MLPredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MLAnomalyService Tests")
class MLAnomalyServiceTest {

    @Mock
    private MLAnomalyRepository mlAnomalyRepository;

    @Mock
    private MLPredictionRepository mlPredictionRepository;

    @InjectMocks
    private MLAnomalyService mlAnomalyService;

    private Organization testOrg;
    private Device testDevice;
    private MLPrediction testPrediction;
    private MLAnomaly testAnomaly;
    private UUID anomalyId;
    private UUID deviceId;
    private Long orgId = 1L;
    private Long userId = 100L;

    @BeforeEach
    void setUp() {
        testOrg = new Organization();
        testOrg.setId(orgId);

        deviceId = UUID.randomUUID();
        testDevice = new Device();
        testDevice.setId(deviceId);
        testDevice.setOrganization(testOrg);

        testPrediction = MLPrediction.builder()
                .id(UUID.randomUUID())
                .device(testDevice)
                .organization(testOrg)
                .predictionType("ANOMALY")
                .predictionTimestamp(Instant.now())
                .confidence(new BigDecimal("0.9"))
                .build();

        anomalyId = UUID.randomUUID();
        testAnomaly = MLAnomaly.builder()
                .id(anomalyId)
                .prediction(testPrediction)
                .device(testDevice)
                .organization(testOrg)
                .anomalyScore(new BigDecimal("0.85"))
                .severity(MLAnomalySeverity.HIGH)
                .status(MLAnomalyStatus.NEW)
                .detectedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Get Anomalies Tests")
    class GetAnomaliesTests {

        @Test
        @DisplayName("Should get anomalies by organization")
        void shouldGetByOrganization() {
            Page<MLAnomaly> page = new PageImpl<>(List.of(testAnomaly));
            when(mlAnomalyRepository.findByOrganizationId(eq(orgId), any())).thenReturn(page);

            Page<MLAnomaly> result = mlAnomalyService.getAnomalies(orgId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get anomalies by device")
        void shouldGetByDevice() {
            Page<MLAnomaly> page = new PageImpl<>(List.of(testAnomaly));
            when(mlAnomalyRepository.findByDeviceId(eq(deviceId), any())).thenReturn(page);

            Page<MLAnomaly> result = mlAnomalyService.getAnomaliesByDevice(deviceId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get anomaly by ID")
        void shouldGetById() {
            when(mlAnomalyRepository.findById(anomalyId)).thenReturn(Optional.of(testAnomaly));

            Optional<MLAnomaly> result = mlAnomalyService.getAnomaly(anomalyId);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Should get new anomalies")
        void shouldGetNewAnomalies() {
            when(mlAnomalyRepository.findNewAnomaliesByOrganization(orgId))
                    .thenReturn(List.of(testAnomaly));

            List<MLAnomaly> result = mlAnomalyService.getNewAnomalies(orgId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get critical anomalies")
        void shouldGetCriticalAnomalies() {
            testAnomaly.setSeverity(MLAnomalySeverity.CRITICAL);
            when(mlAnomalyRepository.findCriticalNewAnomalies(eq(orgId), any()))
                    .thenReturn(List.of(testAnomaly));

            List<MLAnomaly> result = mlAnomalyService.getCriticalAnomalies(orgId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get anomalies in time range")
        void shouldGetInTimeRange() {
            Instant start = Instant.now().minusSeconds(3600);
            Instant end = Instant.now();
            when(mlAnomalyRepository.findByDeviceAndTimeRange(deviceId, start, end))
                    .thenReturn(List.of(testAnomaly));

            List<MLAnomaly> result = mlAnomalyService.getAnomaliesInTimeRange(deviceId, start, end);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Create Anomaly Tests")
    class CreateAnomalyTests {

        @Test
        @DisplayName("Should create anomaly from prediction")
        void shouldCreateAnomaly() {
            when(mlAnomalyRepository.save(any())).thenAnswer(inv -> {
                MLAnomaly a = inv.getArgument(0);
                a.setId(UUID.randomUUID());
                return a;
            });

            MLAnomaly result = mlAnomalyService.createAnomaly(
                    testPrediction,
                    new BigDecimal("0.85"),
                    "POINT_ANOMALY",
                    MLAnomalySeverity.HIGH,
                    List.of("temperature", "pressure"),
                    Map.of("temperature", 25.0),
                    Map.of("temperature", 85.0));

            assertThat(result.getId()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(MLAnomalyStatus.NEW);
            assertThat(result.getSeverity()).isEqualTo(MLAnomalySeverity.HIGH);
            assertThat(result.getAffectedVariables()).containsExactly("temperature", "pressure");

            ArgumentCaptor<MLAnomaly> captor = ArgumentCaptor.forClass(MLAnomaly.class);
            verify(mlAnomalyRepository).save(captor.capture());
            assertThat(captor.getValue().getDevice()).isEqualTo(testDevice);
            assertThat(captor.getValue().getOrganization()).isEqualTo(testOrg);
        }
    }

    @Nested
    @DisplayName("Acknowledge Anomaly Tests")
    class AcknowledgeAnomalyTests {

        @Test
        @DisplayName("Should acknowledge new anomaly")
        void shouldAcknowledgeNewAnomaly() {
            when(mlAnomalyRepository.findById(anomalyId)).thenReturn(Optional.of(testAnomaly));
            when(mlAnomalyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLAnomaly result = mlAnomalyService.acknowledgeAnomaly(anomalyId, userId);

            assertThat(result.getStatus()).isEqualTo(MLAnomalyStatus.ACKNOWLEDGED);
            assertThat(result.getAcknowledgedAt()).isNotNull();
            assertThat(result.getAcknowledgedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw when acknowledging non-new anomaly")
        void shouldThrowWhenNotNew() {
            testAnomaly.setStatus(MLAnomalyStatus.RESOLVED);
            when(mlAnomalyRepository.findById(anomalyId)).thenReturn(Optional.of(testAnomaly));

            assertThatThrownBy(() -> mlAnomalyService.acknowledgeAnomaly(anomalyId, userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("only acknowledge NEW");
        }

        @Test
        @DisplayName("Should throw when anomaly not found")
        void shouldThrowWhenNotFound() {
            when(mlAnomalyRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mlAnomalyService.acknowledgeAnomaly(anomalyId, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Investigation Tests")
    class InvestigationTests {

        @Test
        @DisplayName("Should start investigation")
        void shouldStartInvestigation() {
            testAnomaly.setStatus(MLAnomalyStatus.ACKNOWLEDGED);
            when(mlAnomalyRepository.findById(anomalyId)).thenReturn(Optional.of(testAnomaly));
            when(mlAnomalyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLAnomaly result = mlAnomalyService.startInvestigation(anomalyId);

            assertThat(result.getStatus()).isEqualTo(MLAnomalyStatus.INVESTIGATING);
        }

        @Test
        @DisplayName("Should throw when investigating closed anomaly")
        void shouldThrowWhenClosed() {
            testAnomaly.setStatus(MLAnomalyStatus.RESOLVED);
            when(mlAnomalyRepository.findById(anomalyId)).thenReturn(Optional.of(testAnomaly));

            assertThatThrownBy(() -> mlAnomalyService.startInvestigation(anomalyId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("closed");
        }
    }

    @Nested
    @DisplayName("Resolve Anomaly Tests")
    class ResolveAnomalyTests {

        @Test
        @DisplayName("Should resolve anomaly")
        void shouldResolveAnomaly() {
            testAnomaly.setStatus(MLAnomalyStatus.INVESTIGATING);
            when(mlAnomalyRepository.findById(anomalyId)).thenReturn(Optional.of(testAnomaly));
            when(mlAnomalyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLAnomaly result = mlAnomalyService.resolveAnomaly(anomalyId, userId, "Fixed sensor");

            assertThat(result.getStatus()).isEqualTo(MLAnomalyStatus.RESOLVED);
            assertThat(result.getResolvedAt()).isNotNull();
            assertThat(result.getResolvedBy()).isEqualTo(userId);
            assertThat(result.getResolutionNote()).isEqualTo("Fixed sensor");
        }

        @Test
        @DisplayName("Should throw when already resolved")
        void shouldThrowWhenAlreadyResolved() {
            testAnomaly.setStatus(MLAnomalyStatus.RESOLVED);
            when(mlAnomalyRepository.findById(anomalyId)).thenReturn(Optional.of(testAnomaly));

            assertThatThrownBy(() -> mlAnomalyService.resolveAnomaly(anomalyId, userId, "note"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already closed");
        }
    }

    @Nested
    @DisplayName("False Positive Tests")
    class FalsePositiveTests {

        @Test
        @DisplayName("Should mark as false positive")
        void shouldMarkFalsePositive() {
            when(mlAnomalyRepository.findById(anomalyId)).thenReturn(Optional.of(testAnomaly));
            when(mlAnomalyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mlPredictionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLAnomaly result = mlAnomalyService.markFalsePositive(anomalyId, userId, "Sensor noise");

            assertThat(result.getStatus()).isEqualTo(MLAnomalyStatus.FALSE_POSITIVE);
            assertThat(result.getResolvedBy()).isEqualTo(userId);
            assertThat(result.getResolutionNote()).isEqualTo("Sensor noise");

            // Verify prediction feedback was updated
            ArgumentCaptor<MLPrediction> captor = ArgumentCaptor.forClass(MLPrediction.class);
            verify(mlPredictionRepository).save(captor.capture());
            assertThat(captor.getValue().getFeedbackLabel()).isEqualTo("FALSE_POSITIVE");
        }
    }

    @Nested
    @DisplayName("Link to Alert Tests")
    class LinkToAlertTests {

        @Test
        @DisplayName("Should link anomaly to alert")
        void shouldLinkToAlert() {
            UUID alertId = UUID.randomUUID();
            when(mlAnomalyRepository.findById(anomalyId)).thenReturn(Optional.of(testAnomaly));
            when(mlAnomalyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLAnomaly result = mlAnomalyService.linkToAlert(anomalyId, alertId);

            assertThat(result.getGlobalAlertId()).isEqualTo(alertId);
        }
    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Should count new anomalies")
        void shouldCountNewAnomalies() {
            when(mlAnomalyRepository.countNewAnomaliesByOrganization(orgId)).thenReturn(5L);

            long count = mlAnomalyService.countNewAnomalies(orgId);

            assertThat(count).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should count anomalies since timestamp")
        void shouldCountSince() {
            Instant since = Instant.now().minusSeconds(3600);
            when(mlAnomalyRepository.countAnomaliesSince(deviceId, since)).thenReturn(3L);

            long count = mlAnomalyService.countAnomaliesSince(deviceId, since);

            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should get counts by severity")
        void shouldGetCountsBySeverity() {
            Instant since = Instant.now().minusSeconds(3600);
            List<Object[]> mockResults = List.of(
                    new Object[]{MLAnomalySeverity.CRITICAL, 2L},
                    new Object[]{MLAnomalySeverity.HIGH, 5L},
                    new Object[]{MLAnomalySeverity.MEDIUM, 10L}
            );
            when(mlAnomalyRepository.countBySeveritySince(orgId, since)).thenReturn(mockResults);

            Map<MLAnomalySeverity, Long> counts = mlAnomalyService.getAnomalyCountsBySeverity(orgId, since);

            assertThat(counts).hasSize(3);
            assertThat(counts.get(MLAnomalySeverity.CRITICAL)).isEqualTo(2L);
            assertThat(counts.get(MLAnomalySeverity.HIGH)).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Severity Determination Tests")
    class SeverityDeterminationTests {

        @Test
        @DisplayName("Should determine CRITICAL severity")
        void shouldDetermineCritical() {
            MLAnomalySeverity severity = mlAnomalyService.determineSeverity(
                    new BigDecimal("0.90"), new BigDecimal("0.5"));
            assertThat(severity).isEqualTo(MLAnomalySeverity.CRITICAL);
        }

        @Test
        @DisplayName("Should determine HIGH severity")
        void shouldDetermineHigh() {
            MLAnomalySeverity severity = mlAnomalyService.determineSeverity(
                    new BigDecimal("0.75"), new BigDecimal("0.5"));
            assertThat(severity).isEqualTo(MLAnomalySeverity.HIGH);
        }

        @Test
        @DisplayName("Should determine MEDIUM severity")
        void shouldDetermineMedium() {
            MLAnomalySeverity severity = mlAnomalyService.determineSeverity(
                    new BigDecimal("0.55"), new BigDecimal("0.5"));
            assertThat(severity).isEqualTo(MLAnomalySeverity.MEDIUM);
        }

        @Test
        @DisplayName("Should determine LOW severity")
        void shouldDetermineLow() {
            MLAnomalySeverity severity = mlAnomalyService.determineSeverity(
                    new BigDecimal("0.40"), new BigDecimal("0.5"));
            assertThat(severity).isEqualTo(MLAnomalySeverity.LOW);
        }
    }
}
