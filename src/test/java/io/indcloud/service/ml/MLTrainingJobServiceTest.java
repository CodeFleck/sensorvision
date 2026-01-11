package io.indcloud.service.ml;

import io.indcloud.dto.ml.TrainingJobResponseDto;
import io.indcloud.model.*;
import io.indcloud.repository.MLModelRepository;
import io.indcloud.repository.MLTrainingJobRepository;
import io.indcloud.repository.OrganizationRepository;
import io.indcloud.service.ml.MLTrainingJobService.MLServiceException;
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
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for MLTrainingJobService.
 * Tests training job lifecycle management and Python ML service integration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MLTrainingJobService Tests")
class MLTrainingJobServiceTest {

    @Mock
    private MLTrainingJobRepository trainingJobRepository;

    @Mock
    private MLModelRepository mlModelRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private MLServiceClient mlServiceClient;

    @Mock
    private MLModelService mlModelService;

    @InjectMocks
    private MLTrainingJobService trainingJobService;

    private Organization testOrg;
    private MLModel testModel;
    private MLTrainingJob testJob;
    private UUID modelId;
    private UUID jobId;
    private UUID externalJobId;
    private Long orgId = 1L;
    private Long triggeredBy;

    @BeforeEach
    void setUp() {
        testOrg = new Organization();
        testOrg.setId(orgId);
        testOrg.setName("Test Organization");

        modelId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        externalJobId = UUID.randomUUID();
        triggeredBy = 1L;

        testModel = MLModel.builder()
                .id(modelId)
                .organization(testOrg)
                .name("Test Anomaly Model")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .algorithm("isolation_forest")
                .version("1.0.0")
                .status(MLModelStatus.DRAFT)
                .deviceScope("ALL")
                .hyperparameters(Map.of("n_estimators", 100))
                .featureColumns(List.of("temperature", "pressure"))
                .confidenceThreshold(new BigDecimal("0.8"))
                .anomalyThreshold(new BigDecimal("0.5"))
                .build();

        testJob = MLTrainingJob.builder()
                .id(jobId)
                .model(testModel)
                .organization(testOrg)
                .jobType(MLTrainingJobType.INITIAL_TRAINING)
                .status(MLTrainingJobStatus.PENDING)
                .progressPercent(0)
                .currentStep("Initializing")
                .triggeredByUserId(triggeredBy)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("startTraining Tests")
    class StartTrainingTests {

        @Test
        @DisplayName("Should successfully start training and call Python ML service")
        void shouldStartTrainingSuccessfully() {
            // Arrange - Phase 1 mocks
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(organizationRepository.findById(orgId))
                    .thenReturn(Optional.of(testOrg));
            when(trainingJobRepository.existsActiveJobForModel(modelId))
                    .thenReturn(false);
            when(trainingJobRepository.save(any(MLTrainingJob.class)))
                    .thenAnswer(inv -> {
                        MLTrainingJob job = inv.getArgument(0);
                        if (job.getId() == null) {
                            job.setId(jobId);
                        }
                        job.setModel(testModel);  // Ensure model is set
                        return job;
                    });

            // Phase 3 mock - findById for updateJobWithExternalResponse
            when(trainingJobRepository.findById(jobId))
                    .thenAnswer(inv -> {
                        MLTrainingJob job = MLTrainingJob.builder()
                                .id(jobId)
                                .model(testModel)
                                .organization(testOrg)
                                .jobType(MLTrainingJobType.INITIAL_TRAINING)
                                .status(MLTrainingJobStatus.PENDING)
                                .build();
                        return Optional.of(job);
                    });

            TrainingJobResponseDto mlServiceResponse = TrainingJobResponseDto.builder()
                    .id(externalJobId)
                    .status("RUNNING")
                    .progressPercent(5)
                    .currentStep("Loading data")
                    .startedAt(Instant.now())
                    .build();

            when(mlServiceClient.createTrainingJob(any()))
                    .thenReturn(Mono.just(mlServiceResponse));

            // Act
            MLTrainingJob result = trainingJobService.startTraining(
                    modelId, orgId, triggeredBy, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getExternalJobId()).isEqualTo(externalJobId);
            assertThat(result.getStatus()).isEqualTo(MLTrainingJobStatus.RUNNING);
            assertThat(result.getProgressPercent()).isEqualTo(5);
            assertThat(result.getCurrentStep()).isEqualTo("Loading data");

            // Verify model status updated to TRAINING
            verify(mlModelRepository).save(argThat(model ->
                    model.getStatus() == MLModelStatus.TRAINING));

            // Verify training config was built correctly
            ArgumentCaptor<io.indcloud.dto.ml.TrainingJobCreateDto> captor =
                    ArgumentCaptor.forClass(io.indcloud.dto.ml.TrainingJobCreateDto.class);
            verify(mlServiceClient).createTrainingJob(captor.capture());
            assertThat(captor.getValue().getModelId()).isEqualTo(modelId);
            assertThat(captor.getValue().getJobType()).isEqualTo("INITIAL_TRAINING");
        }

        @Test
        @DisplayName("Should auto-detect RETRAINING job type for previously trained models")
        void shouldAutoDetectRetrainingJobType() {
            // Model was previously trained
            testModel.setTrainedAt(Instant.now().minus(Duration.ofDays(7)));
            testModel.setStatus(MLModelStatus.TRAINED);

            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(organizationRepository.findById(orgId))
                    .thenReturn(Optional.of(testOrg));
            when(trainingJobRepository.existsActiveJobForModel(modelId))
                    .thenReturn(false);
            when(trainingJobRepository.save(any(MLTrainingJob.class)))
                    .thenAnswer(inv -> {
                        MLTrainingJob job = inv.getArgument(0);
                        if (job.getId() == null) job.setId(jobId);
                        job.setModel(testModel);
                        return job;
                    });

            // Phase 3 mock
            when(trainingJobRepository.findById(jobId))
                    .thenAnswer(inv -> {
                        MLTrainingJob job = MLTrainingJob.builder()
                                .id(jobId)
                                .model(testModel)
                                .organization(testOrg)
                                .jobType(MLTrainingJobType.RETRAINING)
                                .status(MLTrainingJobStatus.PENDING)
                                .build();
                        return Optional.of(job);
                    });

            TrainingJobResponseDto mlServiceResponse = TrainingJobResponseDto.builder()
                    .id(externalJobId)
                    .status("PENDING")
                    .build();
            when(mlServiceClient.createTrainingJob(any()))
                    .thenReturn(Mono.just(mlServiceResponse));

            // Act - don't specify job type
            MLTrainingJob result = trainingJobService.startTraining(
                    modelId, orgId, triggeredBy, null);

            // Assert - should detect RETRAINING
            assertThat(result.getJobType()).isEqualTo(MLTrainingJobType.RETRAINING);
        }

        @Test
        @DisplayName("Should throw when model not found")
        void shouldThrowWhenModelNotFound() {
            when(mlModelRepository.findByIdAndOrganizationId(any(), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> trainingJobService.startTraining(
                    modelId, orgId, triggeredBy, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Model not found");
        }

        @Test
        @DisplayName("Should throw when organization not found")
        void shouldThrowWhenOrganizationNotFound() {
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(organizationRepository.findById(orgId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> trainingJobService.startTraining(
                    modelId, orgId, triggeredBy, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Organization not found");
        }

        @Test
        @DisplayName("Should throw when model already has active training job")
        void shouldThrowWhenActiveJobExists() {
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(organizationRepository.findById(orgId))
                    .thenReturn(Optional.of(testOrg));
            when(trainingJobRepository.existsActiveJobForModel(modelId))
                    .thenReturn(true);

            assertThatThrownBy(() -> trainingJobService.startTraining(
                    modelId, orgId, triggeredBy, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already has an active training job");
        }

        @Test
        @DisplayName("Should throw when model is already in TRAINING status")
        void shouldThrowWhenModelAlreadyTraining() {
            testModel.setStatus(MLModelStatus.TRAINING);

            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(organizationRepository.findById(orgId))
                    .thenReturn(Optional.of(testOrg));
            when(trainingJobRepository.existsActiveJobForModel(modelId))
                    .thenReturn(false);

            assertThatThrownBy(() -> trainingJobService.startTraining(
                    modelId, orgId, triggeredBy, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already in TRAINING status");
        }

        @Test
        @DisplayName("Should mark job as FAILED when ML service call fails")
        void shouldMarkJobAsFailedOnMLServiceError() {
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(organizationRepository.findById(orgId))
                    .thenReturn(Optional.of(testOrg));
            when(trainingJobRepository.existsActiveJobForModel(modelId))
                    .thenReturn(false);
            when(trainingJobRepository.save(any(MLTrainingJob.class)))
                    .thenAnswer(inv -> {
                        MLTrainingJob job = inv.getArgument(0);
                        if (job.getId() == null) job.setId(jobId);
                        return job;
                    });

            // Mock findById for markJobAsFailed
            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));

            when(mlServiceClient.createTrainingJob(any()))
                    .thenReturn(Mono.error(new RuntimeException("Connection refused")));

            assertThatThrownBy(() -> trainingJobService.startTraining(
                    modelId, orgId, triggeredBy, null))
                    .isInstanceOf(MLServiceException.class)
                    .hasMessageContaining("Failed to start training");

            // Verify job was saved with FAILED status (once for creation, once for failure)
            ArgumentCaptor<MLTrainingJob> captor = ArgumentCaptor.forClass(MLTrainingJob.class);
            verify(trainingJobRepository, atLeast(2)).save(captor.capture());

            List<MLTrainingJob> savedJobs = captor.getAllValues();
            MLTrainingJob lastSaved = savedJobs.get(savedJobs.size() - 1);
            assertThat(lastSaved.getStatus()).isEqualTo(MLTrainingJobStatus.FAILED);
        }

        @Test
        @DisplayName("Should build training config from model settings")
        void shouldBuildTrainingConfigFromModel() {
            UUID deviceId1 = UUID.randomUUID();
            UUID deviceId2 = UUID.randomUUID();

            testModel.setHyperparameters(Map.of("n_estimators", 100, "max_depth", 10));
            testModel.setFeatureColumns(List.of("temp", "humidity"));
            testModel.setTargetColumn("anomaly");
            testModel.setDeviceScope("SPECIFIC");
            testModel.setDeviceIds(List.of(deviceId1, deviceId2));

            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(organizationRepository.findById(orgId))
                    .thenReturn(Optional.of(testOrg));
            when(trainingJobRepository.existsActiveJobForModel(modelId))
                    .thenReturn(false);

            // Capture the saved job for findById
            final MLTrainingJob[] savedJob = new MLTrainingJob[1];
            when(trainingJobRepository.save(any())).thenAnswer(inv -> {
                MLTrainingJob job = inv.getArgument(0);
                if (job.getId() == null) job.setId(jobId);
                job.setModel(testModel);
                savedJob[0] = job;
                return job;
            });

            // Phase 3 mock - return the job that was saved in Phase 1
            when(trainingJobRepository.findById(jobId))
                    .thenAnswer(inv -> Optional.ofNullable(savedJob[0]));

            TrainingJobResponseDto mlServiceResponse = TrainingJobResponseDto.builder()
                    .id(externalJobId)
                    .status("PENDING")
                    .build();
            when(mlServiceClient.createTrainingJob(any()))
                    .thenReturn(Mono.just(mlServiceResponse));

            MLTrainingJob result = trainingJobService.startTraining(
                    modelId, orgId, triggeredBy, null);

            // Verify training config
            Map<String, Object> config = result.getTrainingConfig();
            assertThat(config).containsEntry("model_type", "ANOMALY_DETECTION");
            assertThat(config).containsEntry("algorithm", "isolation_forest");
            assertThat(config).containsEntry("n_estimators", 100);
            assertThat(config).containsEntry("max_depth", 10);
            assertThat(config).containsEntry("feature_columns", List.of("temp", "humidity"));
            assertThat(config).containsEntry("target_column", "anomaly");
            assertThat(config).containsEntry("device_scope", "SPECIFIC");
            assertThat(config).containsEntry("device_ids", List.of(deviceId1, deviceId2));
        }
    }

    @Nested
    @DisplayName("syncJobStatus Tests")
    class SyncJobStatusTests {

        @BeforeEach
        void setUpJob() {
            testJob.setExternalJobId(externalJobId);
            testJob.setStatus(MLTrainingJobStatus.RUNNING);
        }

        @Test
        @DisplayName("Should sync status from Python ML service")
        void shouldSyncStatusFromMLService() {
            TrainingJobResponseDto response = TrainingJobResponseDto.builder()
                    .id(externalJobId)
                    .status("RUNNING")
                    .progressPercent(50)
                    .currentStep("Training model")
                    .recordCount(10000L)
                    .deviceCount(5)
                    .build();

            when(mlServiceClient.getTrainingJob(externalJobId))
                    .thenReturn(Mono.just(response));
            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));
            when(trainingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLTrainingJob result = trainingJobService.syncJobStatus(testJob);

            assertThat(result.getProgressPercent()).isEqualTo(50);
            assertThat(result.getCurrentStep()).isEqualTo("Training model");
            assertThat(result.getRecordCount()).isEqualTo(10000);
            assertThat(result.getDeviceCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should handle job completion and update model")
        void shouldHandleJobCompletionAndUpdateModel() {
            Map<String, Object> resultMetrics = Map.of("accuracy", 0.95, "f1_score", 0.92);

            TrainingJobResponseDto response = TrainingJobResponseDto.builder()
                    .id(externalJobId)
                    .status("COMPLETED")
                    .progressPercent(100)
                    .currentStep("Complete")
                    .completedAt(Instant.now())
                    .durationSeconds(3600)
                    .resultMetrics(resultMetrics)
                    .build();

            when(mlServiceClient.getTrainingJob(externalJobId))
                    .thenReturn(Mono.just(response));
            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));
            when(trainingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLTrainingJob result = trainingJobService.syncJobStatus(testJob);

            assertThat(result.getStatus()).isEqualTo(MLTrainingJobStatus.COMPLETED);
            assertThat(result.getProgressPercent()).isEqualTo(100);
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getDurationSeconds()).isEqualTo(3600);
            assertThat(result.getResultMetrics()).containsEntry("accuracy", 0.95);

            // Verify model service was called to complete training
            verify(mlModelService).completeTraining(
                    eq(modelId), eq(orgId), eq(resultMetrics), isNull(), anyString(), isNull());
        }

        @Test
        @DisplayName("Should handle job failure and update model status")
        void shouldHandleJobFailureAndUpdateModelStatus() {
            TrainingJobResponseDto response = TrainingJobResponseDto.builder()
                    .id(externalJobId)
                    .status("FAILED")
                    .errorMessage("Out of memory error")
                    .completedAt(Instant.now())
                    .build();

            when(mlServiceClient.getTrainingJob(externalJobId))
                    .thenReturn(Mono.just(response));
            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));
            when(trainingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLTrainingJob result = trainingJobService.syncJobStatus(testJob);

            assertThat(result.getStatus()).isEqualTo(MLTrainingJobStatus.FAILED);
            assertThat(result.getErrorMessage()).isEqualTo("Out of memory error");

            // Verify model service was called to fail training
            verify(mlModelService).failTraining(modelId, orgId);
        }

        @Test
        @DisplayName("Should skip sync when external job ID is null")
        void shouldSkipSyncWhenExternalJobIdIsNull() {
            testJob.setExternalJobId(null);

            MLTrainingJob result = trainingJobService.syncJobStatus(testJob);

            assertThat(result).isEqualTo(testJob);
            verifyNoInteractions(mlServiceClient);
        }

        @Test
        @DisplayName("Should handle ML service errors gracefully")
        void shouldHandleMLServiceErrorsGracefully() {
            when(mlServiceClient.getTrainingJob(externalJobId))
                    .thenReturn(Mono.error(new RuntimeException("Connection timeout")));

            MLTrainingJob result = trainingJobService.syncJobStatus(testJob);

            // Should return original job without changes
            assertThat(result).isEqualTo(testJob);
            verify(trainingJobRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle unknown status from ML service")
        void shouldHandleUnknownStatusFromMLService() {
            TrainingJobResponseDto response = TrainingJobResponseDto.builder()
                    .id(externalJobId)
                    .status("UNKNOWN_STATUS")
                    .progressPercent(25)
                    .build();

            when(mlServiceClient.getTrainingJob(externalJobId))
                    .thenReturn(Mono.just(response));
            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));
            when(trainingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLTrainingJob result = trainingJobService.syncJobStatus(testJob);

            // Unknown status should map to PENDING as default
            assertThat(result.getStatus()).isEqualTo(MLTrainingJobStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("cancelJob Tests")
    class CancelJobTests {

        @BeforeEach
        void setUpJob() {
            testJob.setExternalJobId(externalJobId);
            testJob.setStatus(MLTrainingJobStatus.RUNNING);
            testJob.setStartedAt(Instant.now().minus(Duration.ofMinutes(10)));
        }

        @Test
        @DisplayName("Should cancel active job successfully")
        void shouldCancelActiveJobSuccessfully() {
            // Model is in TRAINING status
            testModel.setStatus(MLModelStatus.TRAINING);

            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));
            when(mlServiceClient.cancelTrainingJob(externalJobId))
                    .thenReturn(Mono.empty());
            when(trainingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLTrainingJob result = trainingJobService.cancelJob(jobId, orgId);

            assertThat(result.getStatus()).isEqualTo(MLTrainingJobStatus.CANCELLED);
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getDurationSeconds()).isNotNull();

            verify(mlServiceClient).cancelTrainingJob(externalJobId);
        }

        @Test
        @DisplayName("Should revert model status after cancellation")
        void shouldRevertModelStatusAfterCancellation() {
            testModel.setStatus(MLModelStatus.TRAINING);

            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));
            when(mlServiceClient.cancelTrainingJob(externalJobId))
                    .thenReturn(Mono.empty());
            when(trainingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            trainingJobService.cancelJob(jobId, orgId);

            // Verify model status reverted to DRAFT (never trained before)
            ArgumentCaptor<MLModel> captor = ArgumentCaptor.forClass(MLModel.class);
            verify(mlModelRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(MLModelStatus.DRAFT);
        }

        @Test
        @DisplayName("Should revert to TRAINED status if model was previously trained")
        void shouldRevertToTrainedStatusIfPreviouslyTrained() {
            testModel.setStatus(MLModelStatus.TRAINING);
            testModel.setTrainedAt(Instant.now().minus(Duration.ofDays(7)));

            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));
            when(mlServiceClient.cancelTrainingJob(externalJobId))
                    .thenReturn(Mono.empty());
            when(trainingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            trainingJobService.cancelJob(jobId, orgId);

            // Verify model status reverted to TRAINED
            ArgumentCaptor<MLModel> captor = ArgumentCaptor.forClass(MLModel.class);
            verify(mlModelRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(MLModelStatus.TRAINED);
        }

        @Test
        @DisplayName("Should throw when job not found")
        void shouldThrowWhenJobNotFound() {
            when(trainingJobRepository.findById(any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> trainingJobService.cancelJob(jobId, orgId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Training job not found");
        }

        @Test
        @DisplayName("Should throw when job belongs to different organization")
        void shouldThrowWhenJobBelongsToDifferentOrg() {
            Organization otherOrg = new Organization();
            otherOrg.setId(999L);
            testJob.setOrganization(otherOrg);

            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));

            assertThatThrownBy(() -> trainingJobService.cancelJob(jobId, orgId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Training job not found");
        }

        @Test
        @DisplayName("Should throw when trying to cancel completed job")
        void shouldThrowWhenCancellingCompletedJob() {
            testJob.setStatus(MLTrainingJobStatus.COMPLETED);

            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));

            assertThatThrownBy(() -> trainingJobService.cancelJob(jobId, orgId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel job with status: COMPLETED");
        }

        @Test
        @DisplayName("Should proceed with local cancellation even if ML service call fails")
        void shouldProceedWithLocalCancellationOnMLServiceError() {
            // Model is in TRAINING status
            testModel.setStatus(MLModelStatus.TRAINING);

            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));
            when(mlServiceClient.cancelTrainingJob(externalJobId))
                    .thenReturn(Mono.error(new RuntimeException("Service unavailable")));
            when(trainingJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLTrainingJob result = trainingJobService.cancelJob(jobId, orgId);

            // Should still be cancelled locally
            assertThat(result.getStatus()).isEqualTo(MLTrainingJobStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Query Methods Tests")
    class QueryMethodsTests {

        @Test
        @DisplayName("Should get job by ID and organization")
        void shouldGetJobByIdAndOrganization() {
            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));

            Optional<MLTrainingJob> result = trainingJobService.getJob(jobId, orgId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(jobId);
        }

        @Test
        @DisplayName("Should return empty when job belongs to different organization")
        void shouldReturnEmptyWhenJobBelongsToDifferentOrg() {
            Organization otherOrg = new Organization();
            otherOrg.setId(999L);
            testJob.setOrganization(otherOrg);

            when(trainingJobRepository.findById(jobId))
                    .thenReturn(Optional.of(testJob));

            Optional<MLTrainingJob> result = trainingJobService.getJob(jobId, orgId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get jobs for model with pagination")
        void shouldGetJobsForModelWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<MLTrainingJob> page = new PageImpl<>(List.of(testJob));

            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(trainingJobRepository.findByModelId(modelId, pageable))
                    .thenReturn(page);

            Page<MLTrainingJob> result = trainingJobService.getJobsForModel(modelId, orgId, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(jobId);
        }

        @Test
        @DisplayName("Should get jobs for organization with pagination")
        void shouldGetJobsForOrganizationWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<MLTrainingJob> page = new PageImpl<>(List.of(testJob));

            when(trainingJobRepository.findByOrganizationId(orgId, pageable))
                    .thenReturn(page);

            Page<MLTrainingJob> result = trainingJobService.getJobsForOrganization(orgId, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get latest job for model")
        void shouldGetLatestJobForModel() {
            when(trainingJobRepository.findFirstByModelIdOrderByCreatedAtDesc(modelId))
                    .thenReturn(Optional.of(testJob));

            Optional<MLTrainingJob> result = trainingJobService.getLatestJobForModel(modelId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(jobId);
        }

        @Test
        @DisplayName("Should get all active jobs with pagination")
        void shouldGetAllActiveJobs() {
            MLTrainingJob pendingJob = MLTrainingJob.builder()
                    .id(UUID.randomUUID())
                    .status(MLTrainingJobStatus.PENDING)
                    .build();

            Page<MLTrainingJob> jobsPage = new PageImpl<>(List.of(testJob, pendingJob));
            when(trainingJobRepository.findActiveJobsWithLimit(any(Pageable.class)))
                    .thenReturn(jobsPage);

            List<MLTrainingJob> result = trainingJobService.getActiveJobs();

            assertThat(result).hasSize(2);
            verify(trainingJobRepository).findActiveJobsWithLimit(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("toResponse Tests")
    class ToResponseTests {

        @Test
        @DisplayName("Should convert job entity to response DTO")
        void shouldConvertJobToResponseDto() {
            testJob.setExternalJobId(externalJobId);
            testJob.setProgressPercent(75);
            testJob.setCurrentStep("Validating model");
            testJob.setRecordCount(50000L);
            testJob.setDeviceCount(10);
            testJob.setStartedAt(Instant.now().minus(Duration.ofHours(1)));
            testJob.setResultMetrics(Map.of("accuracy", 0.95));
            testJob.setTrainingConfig(Map.of("n_estimators", 100));

            TrainingJobResponseDto result = trainingJobService.toResponse(testJob);

            assertThat(result.getId()).isEqualTo(jobId);
            assertThat(result.getModelId()).isEqualTo(modelId);
            assertThat(result.getOrganizationId()).isEqualTo(orgId);
            assertThat(result.getJobType()).isEqualTo("INITIAL_TRAINING");
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getProgressPercent()).isEqualTo(75);
            assertThat(result.getCurrentStep()).isEqualTo("Validating model");
            assertThat(result.getRecordCount()).isEqualTo(50000);
            assertThat(result.getDeviceCount()).isEqualTo(10);
            assertThat(result.getResultMetrics()).containsEntry("accuracy", 0.95);
            assertThat(result.getTrainingConfig()).containsEntry("n_estimators", 100);
        }

        @Test
        @DisplayName("Should handle null model in response conversion")
        void shouldHandleNullModelInResponseConversion() {
            testJob.setModel(null);

            TrainingJobResponseDto result = trainingJobService.toResponse(testJob);

            assertThat(result.getModelId()).isNull();
            assertThat(result.getOrganizationId()).isEqualTo(orgId);
        }
    }
}
