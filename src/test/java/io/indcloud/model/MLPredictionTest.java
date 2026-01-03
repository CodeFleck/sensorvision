package io.indcloud.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MLPrediction Entity Tests")
class MLPredictionTest {

    @Test
    @DisplayName("Should create MLPrediction with required fields")
    void shouldCreateWithRequiredFields() {
        Instant now = Instant.now();

        MLPrediction prediction = MLPrediction.builder()
                .predictionType("ANOMALY")
                .predictionTimestamp(now)
                .build();

        assertThat(prediction.getPredictionType()).isEqualTo("ANOMALY");
        assertThat(prediction.getPredictionTimestamp()).isEqualTo(now);
        assertThat(prediction.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create MLPrediction with all fields")
    void shouldCreateWithAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Instant validUntil = now.plusSeconds(3600);

        MLModel model = MLModel.builder()
                .name("Test Model")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .algorithm("isolation_forest")
                .build();

        Device device = new Device();
        device.setId(UUID.randomUUID());

        Organization org = new Organization();
        org.setId(1L);

        Map<String, Object> details = Map.of(
                "anomaly_score", 0.85,
                "affected_variables", List.of("temperature", "pressure")
        );

        MLPrediction prediction = MLPrediction.builder()
                .id(id)
                .model(model)
                .device(device)
                .organization(org)
                .predictionType("ANOMALY")
                .predictionValue(new BigDecimal("0.85"))
                .predictionLabel("HIGH_RISK")
                .confidence(new BigDecimal("0.9234"))
                .predictionDetails(details)
                .predictionTimestamp(now)
                .predictionHorizon("1h")
                .validUntil(validUntil)
                .build();

        assertThat(prediction.getId()).isEqualTo(id);
        assertThat(prediction.getModel()).isEqualTo(model);
        assertThat(prediction.getDevice()).isEqualTo(device);
        assertThat(prediction.getOrganization()).isEqualTo(org);
        assertThat(prediction.getPredictionValue()).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(prediction.getPredictionLabel()).isEqualTo("HIGH_RISK");
        assertThat(prediction.getConfidence()).isEqualByComparingTo(new BigDecimal("0.9234"));
        assertThat(prediction.getPredictionDetails()).containsKey("anomaly_score");
        assertThat(prediction.getPredictionHorizon()).isEqualTo("1h");
        assertThat(prediction.getValidUntil()).isEqualTo(validUntil);
    }

    @Test
    @DisplayName("Should support feedback fields")
    void shouldSupportFeedbackFields() {
        UUID feedbackBy = UUID.randomUUID();
        Instant feedbackAt = Instant.now();

        MLPrediction prediction = MLPrediction.builder()
                .predictionType("ANOMALY")
                .predictionTimestamp(Instant.now())
                .feedbackLabel("FALSE_POSITIVE")
                .feedbackAt(feedbackAt)
                .feedbackBy(feedbackBy)
                .build();

        assertThat(prediction.getFeedbackLabel()).isEqualTo("FALSE_POSITIVE");
        assertThat(prediction.getFeedbackAt()).isEqualTo(feedbackAt);
        assertThat(prediction.getFeedbackBy()).isEqualTo(feedbackBy);
    }

    @Test
    @DisplayName("Should handle different prediction types")
    void shouldHandleDifferentPredictionTypes() {
        String[] types = {"ANOMALY", "MAINTENANCE", "ENERGY", "RUL"};

        for (String type : types) {
            MLPrediction prediction = MLPrediction.builder()
                    .predictionType(type)
                    .predictionTimestamp(Instant.now())
                    .build();
            assertThat(prediction.getPredictionType()).isEqualTo(type);
        }
    }
}
