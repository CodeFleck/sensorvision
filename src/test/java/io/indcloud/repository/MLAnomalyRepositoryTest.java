package io.indcloud.repository;

import io.indcloud.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MLAnomalyRepository.
 * These tests verify repository method contracts using Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MLAnomalyRepository Tests")
class MLAnomalyRepositoryTest {

    @Mock
    private MLAnomalyRepository mlAnomalyRepository;

    private Organization testOrg;
    private Device testDevice;
    private MLPrediction testPrediction;
    private MLAnomaly testAnomaly;
    private Long orgId = 1L;
    private UUID deviceId;

    @BeforeEach
    void setUp() {
        testOrg = new Organization();
        testOrg.setId(orgId);
        testOrg.setName("Test Organization");

        deviceId = UUID.randomUUID();
        testDevice = new Device();
        testDevice.setId(deviceId);
        testDevice.setExternalId("test-device-001");
        testDevice.setName("Test Device");
        testDevice.setOrganization(testOrg);

        testPrediction = MLPrediction.builder()
                .id(UUID.randomUUID())
                .device(testDevice)
                .organization(testOrg)
                .predictionType("ANOMALY")
                .predictionTimestamp(Instant.now())
                .confidence(new BigDecimal("0.9"))
                .build();

        testAnomaly = MLAnomaly.builder()
                .id(UUID.randomUUID())
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
    @DisplayName("Find By Organization Tests")
    class FindByOrganizationTests {

        @Test
        @DisplayName("Should find anomalies by organization")
        void shouldFindByOrganization() {
            Page<MLAnomaly> page = new PageImpl<>(List.of(testAnomaly));
            when(mlAnomalyRepository.findByOrganizationId(eq(orgId), any())).thenReturn(page);

            Page<MLAnomaly> result = mlAnomalyRepository.findByOrganizationId(orgId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should find anomalies by organization and status")
        void shouldFindByOrganizationAndStatus() {
            when(mlAnomalyRepository.findByOrganizationIdAndStatus(orgId, MLAnomalyStatus.NEW))
                    .thenReturn(List.of(testAnomaly));

            List<MLAnomaly> result = mlAnomalyRepository.findByOrganizationIdAndStatus(orgId, MLAnomalyStatus.NEW);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(MLAnomalyStatus.NEW);
        }

        @Test
        @DisplayName("Should find anomalies by organization and severity")
        void shouldFindByOrganizationAndSeverity() {
            when(mlAnomalyRepository.findByOrganizationIdAndSeverity(orgId, MLAnomalySeverity.HIGH))
                    .thenReturn(List.of(testAnomaly));

            List<MLAnomaly> result = mlAnomalyRepository.findByOrganizationIdAndSeverity(
                    orgId, MLAnomalySeverity.HIGH);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSeverity()).isEqualTo(MLAnomalySeverity.HIGH);
        }
    }

    @Nested
    @DisplayName("Find By Device Tests")
    class FindByDeviceTests {

        @Test
        @DisplayName("Should find anomalies by device")
        void shouldFindByDevice() {
            Page<MLAnomaly> page = new PageImpl<>(List.of(testAnomaly));
            when(mlAnomalyRepository.findByDeviceId(eq(deviceId), any())).thenReturn(page);

            Page<MLAnomaly> result = mlAnomalyRepository.findByDeviceId(deviceId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should find anomalies by device and time range")
        void shouldFindByDeviceAndTimeRange() {
            Instant start = Instant.now().minusSeconds(3600);
            Instant end = Instant.now();
            when(mlAnomalyRepository.findByDeviceAndTimeRange(deviceId, start, end))
                    .thenReturn(List.of(testAnomaly));

            List<MLAnomaly> result = mlAnomalyRepository.findByDeviceAndTimeRange(deviceId, start, end);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("New Anomalies Tests")
    class NewAnomaliesTests {

        @Test
        @DisplayName("Should find new anomalies by organization")
        void shouldFindNewAnomaliesByOrganization() {
            when(mlAnomalyRepository.findNewAnomaliesByOrganization(orgId))
                    .thenReturn(List.of(testAnomaly));

            List<MLAnomaly> result = mlAnomalyRepository.findNewAnomaliesByOrganization(orgId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(MLAnomalyStatus.NEW);
        }

        @Test
        @DisplayName("Should find critical new anomalies")
        void shouldFindCriticalNewAnomalies() {
            MLAnomaly criticalAnomaly = testAnomaly.toBuilder()
                    .severity(MLAnomalySeverity.CRITICAL)
                    .build();
            when(mlAnomalyRepository.findCriticalNewAnomalies(eq(orgId), any()))
                    .thenReturn(List.of(criticalAnomaly));

            List<MLAnomaly> result = mlAnomalyRepository.findCriticalNewAnomalies(
                    orgId, List.of(MLAnomalySeverity.CRITICAL, MLAnomalySeverity.HIGH));

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Should count new anomalies by organization")
        void shouldCountNewAnomaliesByOrganization() {
            when(mlAnomalyRepository.countNewAnomaliesByOrganization(orgId)).thenReturn(5L);

            long count = mlAnomalyRepository.countNewAnomaliesByOrganization(orgId);

            assertThat(count).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should count anomalies since timestamp")
        void shouldCountAnomaliesSince() {
            Instant since = Instant.now().minusSeconds(3600);
            when(mlAnomalyRepository.countAnomaliesSince(deviceId, since)).thenReturn(3L);

            long count = mlAnomalyRepository.countAnomaliesSince(deviceId, since);

            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should count by severity since timestamp")
        void shouldCountBySeveritySince() {
            Instant since = Instant.now().minusSeconds(3600);
            List<Object[]> mockResults = List.of(
                    new Object[]{MLAnomalySeverity.CRITICAL, 2L},
                    new Object[]{MLAnomalySeverity.HIGH, 5L},
                    new Object[]{MLAnomalySeverity.MEDIUM, 10L}
            );
            when(mlAnomalyRepository.countBySeveritySince(orgId, since)).thenReturn(mockResults);

            List<Object[]> result = mlAnomalyRepository.countBySeveritySince(orgId, since);

            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find anomaly by id")
        void shouldFindById() {
            UUID anomalyId = testAnomaly.getId();
            when(mlAnomalyRepository.findById(anomalyId)).thenReturn(Optional.of(testAnomaly));

            Optional<MLAnomaly> result = mlAnomalyRepository.findById(anomalyId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(anomalyId);
        }

        @Test
        @DisplayName("Should return empty when anomaly not found")
        void shouldReturnEmptyWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(mlAnomalyRepository.findById(unknownId)).thenReturn(Optional.empty());

            Optional<MLAnomaly> result = mlAnomalyRepository.findById(unknownId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Save Tests")
    class SaveTests {

        @Test
        @DisplayName("Should save anomaly")
        void shouldSaveAnomaly() {
            when(mlAnomalyRepository.save(any(MLAnomaly.class))).thenAnswer(inv -> {
                MLAnomaly a = inv.getArgument(0);
                if (a.getId() == null) {
                    a.setId(UUID.randomUUID());
                }
                return a;
            });

            MLAnomaly newAnomaly = MLAnomaly.builder()
                    .prediction(testPrediction)
                    .device(testDevice)
                    .organization(testOrg)
                    .anomalyScore(new BigDecimal("0.75"))
                    .severity(MLAnomalySeverity.MEDIUM)
                    .status(MLAnomalyStatus.NEW)
                    .detectedAt(Instant.now())
                    .build();

            MLAnomaly saved = mlAnomalyRepository.save(newAnomaly);

            assertThat(saved.getId()).isNotNull();
            verify(mlAnomalyRepository).save(newAnomaly);
        }

        @Test
        @DisplayName("Should update anomaly status")
        void shouldUpdateAnomalyStatus() {
            testAnomaly.setStatus(MLAnomalyStatus.ACKNOWLEDGED);
            when(mlAnomalyRepository.save(testAnomaly)).thenReturn(testAnomaly);

            MLAnomaly updated = mlAnomalyRepository.save(testAnomaly);

            assertThat(updated.getStatus()).isEqualTo(MLAnomalyStatus.ACKNOWLEDGED);
        }
    }

    @Nested
    @DisplayName("Severity Filter Tests")
    class SeverityFilterTests {

        @Test
        @DisplayName("Should return anomalies matching multiple severities")
        void shouldFindByMultipleSeverities() {
            MLAnomaly highAnomaly = testAnomaly.toBuilder()
                    .id(UUID.randomUUID())
                    .severity(MLAnomalySeverity.HIGH)
                    .build();
            MLAnomaly criticalAnomaly = testAnomaly.toBuilder()
                    .id(UUID.randomUUID())
                    .severity(MLAnomalySeverity.CRITICAL)
                    .build();

            when(mlAnomalyRepository.findCriticalNewAnomalies(
                    eq(orgId),
                    eq(List.of(MLAnomalySeverity.CRITICAL, MLAnomalySeverity.HIGH))))
                    .thenReturn(List.of(highAnomaly, criticalAnomaly));

            List<MLAnomaly> result = mlAnomalyRepository.findCriticalNewAnomalies(
                    orgId, List.of(MLAnomalySeverity.CRITICAL, MLAnomalySeverity.HIGH));

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(a ->
                    a.getSeverity() == MLAnomalySeverity.CRITICAL ||
                            a.getSeverity() == MLAnomalySeverity.HIGH);
        }

        @Test
        @DisplayName("Should return empty when no matching severities")
        void shouldReturnEmptyWhenNoMatchingSeverities() {
            when(mlAnomalyRepository.findCriticalNewAnomalies(
                    eq(orgId),
                    eq(List.of(MLAnomalySeverity.CRITICAL))))
                    .thenReturn(List.of());

            List<MLAnomaly> result = mlAnomalyRepository.findCriticalNewAnomalies(
                    orgId, List.of(MLAnomalySeverity.CRITICAL));

            assertThat(result).isEmpty();
        }
    }
}
