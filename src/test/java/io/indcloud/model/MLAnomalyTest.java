package io.indcloud.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MLAnomaly Entity Tests")
class MLAnomalyTest {

    @Test
    @DisplayName("Should create MLAnomaly with defaults")
    void shouldCreateWithDefaults() {
        Instant detectedAt = Instant.now();

        MLAnomaly anomaly = MLAnomaly.builder()
                .anomalyScore(new BigDecimal("0.75"))
                .detectedAt(detectedAt)
                .build();

        assertThat(anomaly.getAnomalyScore()).isEqualByComparingTo(new BigDecimal("0.75"));
        assertThat(anomaly.getDetectedAt()).isEqualTo(detectedAt);
        assertThat(anomaly.getAnomalyType()).isEqualTo("POINT_ANOMALY");
        assertThat(anomaly.getSeverity()).isEqualTo(MLAnomalySeverity.MEDIUM);
        assertThat(anomaly.getStatus()).isEqualTo(MLAnomalyStatus.NEW);
        assertThat(anomaly.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create MLAnomaly with all fields")
    void shouldCreateWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID acknowledgedBy = UUID.randomUUID();
        UUID resolvedBy = UUID.randomUUID();
        UUID globalAlertId = UUID.randomUUID();
        Instant now = Instant.now();

        MLPrediction prediction = MLPrediction.builder()
                .predictionType("ANOMALY")
                .predictionTimestamp(now)
                .build();

        Device device = new Device();
        device.setId(UUID.randomUUID());

        Organization org = new Organization();
        org.setId(1L);

        List<String> affectedVars = List.of("temperature", "pressure");
        Map<String, Object> expectedVals = Map.of("temperature", 25.0, "pressure", 101.3);
        Map<String, Object> actualVals = Map.of("temperature", 85.0, "pressure", 150.2);

        MLAnomaly anomaly = MLAnomaly.builder()
                .id(id)
                .prediction(prediction)
                .device(device)
                .organization(org)
                .anomalyScore(new BigDecimal("0.92"))
                .anomalyType("CONTEXTUAL")
                .severity(MLAnomalySeverity.CRITICAL)
                .affectedVariables(affectedVars)
                .expectedValues(expectedVals)
                .actualValues(actualVals)
                .status(MLAnomalyStatus.RESOLVED)
                .acknowledgedAt(now)
                .acknowledgedBy(acknowledgedBy)
                .resolvedAt(now.plusSeconds(3600))
                .resolvedBy(resolvedBy)
                .resolutionNote("Sensor recalibrated")
                .globalAlertId(globalAlertId)
                .detectedAt(now.minusSeconds(7200))
                .build();

        assertThat(anomaly.getId()).isEqualTo(id);
        assertThat(anomaly.getPrediction()).isEqualTo(prediction);
        assertThat(anomaly.getDevice()).isEqualTo(device);
        assertThat(anomaly.getAnomalyScore()).isEqualByComparingTo(new BigDecimal("0.92"));
        assertThat(anomaly.getAnomalyType()).isEqualTo("CONTEXTUAL");
        assertThat(anomaly.getSeverity()).isEqualTo(MLAnomalySeverity.CRITICAL);
        assertThat(anomaly.getAffectedVariables()).containsExactly("temperature", "pressure");
        assertThat(anomaly.getExpectedValues()).containsKey("temperature");
        assertThat(anomaly.getActualValues()).containsKey("pressure");
        assertThat(anomaly.getStatus()).isEqualTo(MLAnomalyStatus.RESOLVED);
        assertThat(anomaly.getResolutionNote()).isEqualTo("Sensor recalibrated");
        assertThat(anomaly.getGlobalAlertId()).isEqualTo(globalAlertId);
    }

    @Test
    @DisplayName("Should support all severity levels")
    void shouldSupportAllSeverityLevels() {
        for (MLAnomalySeverity severity : MLAnomalySeverity.values()) {
            MLAnomaly anomaly = MLAnomaly.builder()
                    .anomalyScore(new BigDecimal("0.5"))
                    .detectedAt(Instant.now())
                    .severity(severity)
                    .build();
            assertThat(anomaly.getSeverity()).isEqualTo(severity);
        }
    }

    @Test
    @DisplayName("Should support all anomaly statuses")
    void shouldSupportAllStatuses() {
        for (MLAnomalyStatus status : MLAnomalyStatus.values()) {
            MLAnomaly anomaly = MLAnomaly.builder()
                    .anomalyScore(new BigDecimal("0.5"))
                    .detectedAt(Instant.now())
                    .status(status)
                    .build();
            assertThat(anomaly.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("Should support different anomaly types")
    void shouldSupportDifferentAnomalyTypes() {
        String[] types = {"POINT_ANOMALY", "CONTEXTUAL", "COLLECTIVE"};

        for (String type : types) {
            MLAnomaly anomaly = MLAnomaly.builder()
                    .anomalyScore(new BigDecimal("0.5"))
                    .detectedAt(Instant.now())
                    .anomalyType(type)
                    .build();
            assertThat(anomaly.getAnomalyType()).isEqualTo(type);
        }
    }
}
