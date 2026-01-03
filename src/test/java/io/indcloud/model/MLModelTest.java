package io.indcloud.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MLModel Entity Tests")
class MLModelTest {

    @Test
    @DisplayName("Should create MLModel with builder defaults")
    void shouldCreateWithDefaults() {
        MLModel model = MLModel.builder()
                .name("Test Model")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .algorithm("isolation_forest")
                .build();

        assertThat(model.getName()).isEqualTo("Test Model");
        assertThat(model.getModelType()).isEqualTo(MLModelType.ANOMALY_DETECTION);
        assertThat(model.getAlgorithm()).isEqualTo("isolation_forest");
        assertThat(model.getStatus()).isEqualTo(MLModelStatus.DRAFT);
        assertThat(model.getDeviceScope()).isEqualTo("ALL");
        assertThat(model.getInferenceSchedule()).isEqualTo("0 0 * * * *");
        assertThat(model.getConfidenceThreshold()).isEqualByComparingTo(new BigDecimal("0.8"));
        assertThat(model.getAnomalyThreshold()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(model.getCreatedAt()).isNotNull();
        assertThat(model.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create MLModel with all fields")
    void shouldCreateWithAllFields() {
        UUID id = UUID.randomUUID();
        Long createdBy = 100L;
        Instant now = Instant.now();
        Organization org = new Organization();
        org.setId(1L);

        Map<String, Object> hyperparameters = Map.of(
                "n_estimators", 100,
                "contamination", 0.1
        );
        List<String> featureColumns = List.of("temperature", "pressure", "vibration");
        List<UUID> deviceIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        MLModel model = MLModel.builder()
                .id(id)
                .organization(org)
                .name("Anomaly Detector v2")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .version("2.0.0")
                .algorithm("isolation_forest")
                .hyperparameters(hyperparameters)
                .featureColumns(featureColumns)
                .targetColumn("anomaly_label")
                .status(MLModelStatus.DEPLOYED)
                .modelPath("/models/anomaly_v2.joblib")
                .modelSizeBytes(1024000L)
                .deviceScope("SELECTED")
                .deviceIds(deviceIds)
                .inferenceSchedule("0 */15 * * * *")
                .confidenceThreshold(new BigDecimal("0.9"))
                .anomalyThreshold(new BigDecimal("0.3"))
                .createdBy(createdBy)
                .trainedAt(now)
                .deployedAt(now)
                .build();

        assertThat(model.getId()).isEqualTo(id);
        assertThat(model.getOrganization()).isEqualTo(org);
        assertThat(model.getVersion()).isEqualTo("2.0.0");
        assertThat(model.getStatus()).isEqualTo(MLModelStatus.DEPLOYED);
        assertThat(model.getHyperparameters()).containsEntry("n_estimators", 100);
        assertThat(model.getFeatureColumns()).hasSize(3);
        assertThat(model.getDeviceIds()).hasSize(2);
        assertThat(model.getConfidenceThreshold()).isEqualByComparingTo(new BigDecimal("0.9"));
    }

    @Test
    @DisplayName("Should support all model types")
    void shouldSupportAllModelTypes() {
        for (MLModelType type : MLModelType.values()) {
            MLModel model = MLModel.builder()
                    .name("Test")
                    .modelType(type)
                    .algorithm("test_algo")
                    .build();
            assertThat(model.getModelType()).isEqualTo(type);
        }
    }

    @Test
    @DisplayName("Should support all model statuses")
    void shouldSupportAllStatuses() {
        MLModel model = MLModel.builder()
                .name("Test")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .algorithm("test")
                .build();

        for (MLModelStatus status : MLModelStatus.values()) {
            model.setStatus(status);
            assertThat(model.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() {
        MLModel model = MLModel.builder()
                .name("Minimal Model")
                .modelType(MLModelType.ENERGY_FORECAST)
                .algorithm("prophet")
                .build();

        assertThat(model.getModelPath()).isNull();
        assertThat(model.getModelSizeBytes()).isNull();
        assertThat(model.getTrainingMetrics()).isNull();
        assertThat(model.getValidationMetrics()).isNull();
        assertThat(model.getLastInferenceAt()).isNull();
        assertThat(model.getNextInferenceAt()).isNull();
        assertThat(model.getTrainedAt()).isNull();
        assertThat(model.getDeployedAt()).isNull();
    }
}
