package io.indcloud.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MLTrainingJob Entity Tests")
class MLTrainingJobTest {

    @Test
    @DisplayName("Should create MLTrainingJob with defaults")
    void shouldCreateWithDefaults() {
        MLTrainingJob job = MLTrainingJob.builder()
                .jobType(MLTrainingJobType.INITIAL_TRAINING)
                .build();

        assertThat(job.getJobType()).isEqualTo(MLTrainingJobType.INITIAL_TRAINING);
        assertThat(job.getStatus()).isEqualTo(MLTrainingJobStatus.PENDING);
        assertThat(job.getProgressPercent()).isEqualTo(0);
        assertThat(job.getTrainingConfig()).isNotNull();
        assertThat(job.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create MLTrainingJob with all fields")
    void shouldCreateWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        Instant now = Instant.now();
        Instant dataStart = now.minusSeconds(86400 * 30); // 30 days ago
        Instant dataEnd = now;

        MLModel model = MLModel.builder()
                .name("Test Model")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .algorithm("isolation_forest")
                .build();

        Organization org = new Organization();
        org.setId(1L);

        Map<String, Object> config = Map.of(
                "n_estimators", 100,
                "max_features", 0.8,
                "validation_split", 0.2
        );

        Map<String, Object> metrics = Map.of(
                "accuracy", 0.95,
                "precision", 0.92,
                "recall", 0.88,
                "f1_score", 0.90
        );

        MLTrainingJob job = MLTrainingJob.builder()
                .id(id)
                .model(model)
                .organization(org)
                .jobType(MLTrainingJobType.RETRAINING)
                .status(MLTrainingJobStatus.COMPLETED)
                .trainingConfig(config)
                .trainingDataStart(dataStart)
                .trainingDataEnd(dataEnd)
                .recordCount(100000L)
                .deviceCount(50)
                .progressPercent(100)
                .currentStep("Completed")
                .resultMetrics(metrics)
                .startedAt(now.minusSeconds(3600))
                .completedAt(now)
                .durationSeconds(3600)
                .triggeredBy(triggeredBy)
                .build();

        assertThat(job.getId()).isEqualTo(id);
        assertThat(job.getModel()).isEqualTo(model);
        assertThat(job.getOrganization()).isEqualTo(org);
        assertThat(job.getJobType()).isEqualTo(MLTrainingJobType.RETRAINING);
        assertThat(job.getStatus()).isEqualTo(MLTrainingJobStatus.COMPLETED);
        assertThat(job.getTrainingConfig()).containsEntry("n_estimators", 100);
        assertThat(job.getRecordCount()).isEqualTo(100000L);
        assertThat(job.getDeviceCount()).isEqualTo(50);
        assertThat(job.getProgressPercent()).isEqualTo(100);
        assertThat(job.getResultMetrics()).containsEntry("accuracy", 0.95);
        assertThat(job.getDurationSeconds()).isEqualTo(3600);
        assertThat(job.getTriggeredBy()).isEqualTo(triggeredBy);
    }

    @Test
    @DisplayName("Should support all job types")
    void shouldSupportAllJobTypes() {
        for (MLTrainingJobType type : MLTrainingJobType.values()) {
            MLTrainingJob job = MLTrainingJob.builder()
                    .jobType(type)
                    .build();
            assertThat(job.getJobType()).isEqualTo(type);
        }
    }

    @Test
    @DisplayName("Should support all job statuses")
    void shouldSupportAllStatuses() {
        for (MLTrainingJobStatus status : MLTrainingJobStatus.values()) {
            MLTrainingJob job = MLTrainingJob.builder()
                    .jobType(MLTrainingJobType.INITIAL_TRAINING)
                    .status(status)
                    .build();
            assertThat(job.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("Should handle error fields for failed jobs")
    void shouldHandleErrorFields() {
        MLTrainingJob job = MLTrainingJob.builder()
                .jobType(MLTrainingJobType.INITIAL_TRAINING)
                .status(MLTrainingJobStatus.FAILED)
                .progressPercent(45)
                .currentStep("Feature extraction")
                .errorMessage("OutOfMemoryError: Java heap space")
                .errorStackTrace("java.lang.OutOfMemoryError: Java heap space\n\tat ...")
                .build();

        assertThat(job.getStatus()).isEqualTo(MLTrainingJobStatus.FAILED);
        assertThat(job.getProgressPercent()).isEqualTo(45);
        assertThat(job.getErrorMessage()).contains("OutOfMemoryError");
        assertThat(job.getErrorStackTrace()).isNotNull();
    }

    @Test
    @DisplayName("Should track progress correctly")
    void shouldTrackProgress() {
        MLTrainingJob job = MLTrainingJob.builder()
                .jobType(MLTrainingJobType.INITIAL_TRAINING)
                .status(MLTrainingJobStatus.RUNNING)
                .progressPercent(0)
                .currentStep("Initializing")
                .build();

        // Simulate progress updates
        job.setProgressPercent(25);
        job.setCurrentStep("Loading data");
        assertThat(job.getProgressPercent()).isEqualTo(25);

        job.setProgressPercent(50);
        job.setCurrentStep("Training model");
        assertThat(job.getProgressPercent()).isEqualTo(50);

        job.setProgressPercent(75);
        job.setCurrentStep("Validating");
        assertThat(job.getProgressPercent()).isEqualTo(75);

        job.setProgressPercent(100);
        job.setCurrentStep("Completed");
        job.setStatus(MLTrainingJobStatus.COMPLETED);
        assertThat(job.getProgressPercent()).isEqualTo(100);
        assertThat(job.getStatus()).isEqualTo(MLTrainingJobStatus.COMPLETED);
    }
}
