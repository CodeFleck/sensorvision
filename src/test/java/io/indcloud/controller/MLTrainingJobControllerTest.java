package io.indcloud.controller;

import io.indcloud.dto.ml.TrainingJobResponseDto;
import io.indcloud.model.*;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.ml.MLTrainingJobService;
import io.indcloud.service.ml.MLTrainingJobService.MLServiceException;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MLTrainingJobController.
 * Tests REST endpoints for training job management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MLTrainingJobController Tests")
class MLTrainingJobControllerTest {

    @Mock
    private MLTrainingJobService trainingJobService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private MLTrainingJobController controller;

    private Organization testOrg;
    private User testUser;
    private MLModel testModel;
    private MLTrainingJob testJob;
    private TrainingJobResponseDto testJobResponse;
    private UUID modelId;
    private UUID jobId;
    private Long orgId = 1L;

    @BeforeEach
    void setUp() {
        testOrg = new Organization();
        testOrg.setId(orgId);
        testOrg.setName("Test Organization");

        testUser = new User();
        testUser.setId(100L);
        testUser.setEmail("test@example.com");
        testUser.setOrganization(testOrg);

        modelId = UUID.randomUUID();
        testModel = MLModel.builder()
                .id(modelId)
                .organization(testOrg)
                .name("Test Model")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .status(MLModelStatus.DRAFT)
                .build();

        jobId = UUID.randomUUID();
        testJob = MLTrainingJob.builder()
                .id(jobId)
                .model(testModel)
                .organization(testOrg)
                .jobType(MLTrainingJobType.INITIAL_TRAINING)
                .status(MLTrainingJobStatus.RUNNING)
                .progressPercent(50)
                .currentStep("Training model")
                .startedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        testJobResponse = TrainingJobResponseDto.builder()
                .id(jobId)
                .modelId(modelId)
                .organizationId(orgId)
                .jobType("INITIAL_TRAINING")
                .status("RUNNING")
                .progressPercent(50)
                .currentStep("Training model")
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/ml/training-jobs/start/{modelId}")
    class StartTrainingTests {

        @Test
        @DisplayName("Should start training and return 201 Created")
        void shouldStartTrainingSuccessfully() {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(trainingJobService.startTraining(eq(modelId), eq(orgId), anyLong(), isNull()))
                    .thenReturn(testJob);
            when(trainingJobService.toResponse(testJob)).thenReturn(testJobResponse);

            ResponseEntity<?> response = controller.startTraining(modelId, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(testJobResponse);
        }

        @Test
        @DisplayName("Should start training with specific job type")
        void shouldStartTrainingWithJobType() {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(trainingJobService.startTraining(eq(modelId), eq(orgId), anyLong(),
                    eq(MLTrainingJobType.RETRAINING)))
                    .thenReturn(testJob);
            when(trainingJobService.toResponse(testJob)).thenReturn(testJobResponse);

            MLTrainingJobController.TrainingStartRequest request =
                    new MLTrainingJobController.TrainingStartRequest("RETRAINING");

            ResponseEntity<?> response = controller.startTraining(modelId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(trainingJobService).startTraining(eq(modelId), eq(orgId), anyLong(),
                    eq(MLTrainingJobType.RETRAINING));
        }

        @Test
        @DisplayName("Should return 400 Bad Request for invalid job type")
        void shouldReturnBadRequestForInvalidJobType() {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);

            MLTrainingJobController.TrainingStartRequest request =
                    new MLTrainingJobController.TrainingStartRequest("INVALID_TYPE");

            ResponseEntity<?> response = controller.startTraining(modelId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isInstanceOf(Map.class);

            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertThat(body.get("error")).contains("Invalid job type");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when model not found")
        void shouldReturnBadRequestWhenModelNotFound() {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(trainingJobService.startTraining(any(), any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Model not found: " + modelId));

            ResponseEntity<?> response = controller.startTraining(modelId, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertThat(body.get("error")).contains("Model not found");
        }

        @Test
        @DisplayName("Should return 409 Conflict when model already training")
        void shouldReturnConflictWhenAlreadyTraining() {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(trainingJobService.startTraining(any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("Model already has an active training job"));

            ResponseEntity<?> response = controller.startTraining(modelId, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertThat(body.get("error")).contains("active training job");
        }

        @Test
        @DisplayName("Should return 503 Service Unavailable when ML service is down")
        void shouldReturnServiceUnavailableOnMLServiceError() {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(trainingJobService.startTraining(any(), any(), any(), any()))
                    .thenThrow(new MLServiceException("Connection refused"));

            ResponseEntity<?> response = controller.startTraining(modelId, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertThat(body.get("error")).contains("ML service unavailable");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/training-jobs/{jobId}")
    class GetJobTests {

        @Test
        @DisplayName("Should return job by ID")
        void shouldReturnJobById() {
            when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
            when(trainingJobService.getJob(jobId, orgId))
                    .thenReturn(Optional.of(testJob));
            when(trainingJobService.toResponse(testJob)).thenReturn(testJobResponse);

            ResponseEntity<TrainingJobResponseDto> response = controller.getJob(jobId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(testJobResponse);
        }

        @Test
        @DisplayName("Should return 404 when job not found")
        void shouldReturnNotFoundWhenJobNotFound() {
            when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
            when(trainingJobService.getJob(any(), any()))
                    .thenReturn(Optional.empty());

            ResponseEntity<TrainingJobResponseDto> response = controller.getJob(jobId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/training-jobs")
    class ListJobsTests {

        @Test
        @DisplayName("Should list jobs for organization")
        void shouldListJobsForOrganization() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<MLTrainingJob> jobPage = new PageImpl<>(List.of(testJob));

            when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
            when(trainingJobService.getJobsForOrganization(orgId, pageable))
                    .thenReturn(jobPage);
            when(trainingJobService.toResponse(testJob)).thenReturn(testJobResponse);

            Page<TrainingJobResponseDto> result = controller.listJobs(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0)).isEqualTo(testJobResponse);
        }

        @Test
        @DisplayName("Should return empty page when no jobs")
        void shouldReturnEmptyPageWhenNoJobs() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<MLTrainingJob> emptyPage = Page.empty();

            when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
            when(trainingJobService.getJobsForOrganization(orgId, pageable))
                    .thenReturn(emptyPage);

            Page<TrainingJobResponseDto> result = controller.listJobs(pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/training-jobs/model/{modelId}")
    class GetJobsForModelTests {

        @Test
        @DisplayName("Should return jobs for specific model")
        void shouldReturnJobsForModel() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<MLTrainingJob> jobPage = new PageImpl<>(List.of(testJob));

            when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
            when(trainingJobService.getJobsForModel(modelId, orgId, pageable))
                    .thenReturn(jobPage);
            when(trainingJobService.toResponse(testJob)).thenReturn(testJobResponse);

            ResponseEntity<Page<TrainingJobResponseDto>> response = controller.getJobsForModel(modelId, pageable);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getTotalElements()).isEqualTo(1);
            assertThat(response.getBody().getContent().get(0).getModelId()).isEqualTo(modelId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/training-jobs/model/{modelId}/latest")
    class GetLatestJobForModelTests {

        @Test
        @DisplayName("Should return latest job for model")
        void shouldReturnLatestJobForModel() {
            when(trainingJobService.getLatestJobForModel(modelId))
                    .thenReturn(Optional.of(testJob));
            when(trainingJobService.toResponse(testJob)).thenReturn(testJobResponse);

            ResponseEntity<TrainingJobResponseDto> response =
                    controller.getLatestJobForModel(modelId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(testJobResponse);
        }

        @Test
        @DisplayName("Should return 404 when no jobs for model")
        void shouldReturnNotFoundWhenNoJobsForModel() {
            when(trainingJobService.getLatestJobForModel(modelId))
                    .thenReturn(Optional.empty());

            ResponseEntity<TrainingJobResponseDto> response =
                    controller.getLatestJobForModel(modelId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/training-jobs/{jobId}/cancel")
    class CancelJobTests {

        @Test
        @DisplayName("Should cancel active job successfully")
        void shouldCancelActiveJobSuccessfully() {
            MLTrainingJob cancelledJob = MLTrainingJob.builder()
                    .id(jobId)
                    .model(testModel)
                    .organization(testOrg)
                    .status(MLTrainingJobStatus.CANCELLED)
                    .build();

            TrainingJobResponseDto cancelledResponse = TrainingJobResponseDto.builder()
                    .id(jobId)
                    .status("CANCELLED")
                    .build();

            when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
            when(trainingJobService.cancelJob(jobId, orgId)).thenReturn(cancelledJob);
            when(trainingJobService.toResponse(cancelledJob)).thenReturn(cancelledResponse);

            ResponseEntity<?> response = controller.cancelJob(jobId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(cancelledResponse);
        }

        @Test
        @DisplayName("Should return 404 when job not found")
        void shouldReturnNotFoundWhenJobNotFound() {
            when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
            when(trainingJobService.cancelJob(any(), any()))
                    .thenThrow(new IllegalArgumentException("Training job not found"));

            ResponseEntity<?> response = controller.cancelJob(jobId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should return 400 when job cannot be cancelled")
        void shouldReturnBadRequestWhenJobCannotBeCancelled() {
            when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
            when(trainingJobService.cancelJob(any(), any()))
                    .thenThrow(new IllegalStateException("Cannot cancel job with status: COMPLETED"));

            ResponseEntity<?> response = controller.cancelJob(jobId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertThat(body.get("error")).contains("Cannot cancel");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/training-jobs/{jobId}/refresh")
    class RefreshJobStatusTests {

        @Test
        @DisplayName("Should refresh job status successfully")
        void shouldRefreshJobStatusSuccessfully() {
            MLTrainingJob refreshedJob = MLTrainingJob.builder()
                    .id(jobId)
                    .model(testModel)
                    .organization(testOrg)
                    .status(MLTrainingJobStatus.RUNNING)
                    .progressPercent(75)
                    .currentStep("Validating model")
                    .build();

            TrainingJobResponseDto refreshedResponse = TrainingJobResponseDto.builder()
                    .id(jobId)
                    .status("RUNNING")
                    .progressPercent(75)
                    .currentStep("Validating model")
                    .build();

            when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
            when(trainingJobService.getJob(jobId, orgId))
                    .thenReturn(Optional.of(testJob));
            when(trainingJobService.syncJobStatus(testJob)).thenReturn(refreshedJob);
            when(trainingJobService.toResponse(refreshedJob)).thenReturn(refreshedResponse);

            ResponseEntity<TrainingJobResponseDto> response =
                    controller.refreshJobStatus(jobId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getProgressPercent()).isEqualTo(75);
            assertThat(response.getBody().getCurrentStep()).isEqualTo("Validating model");
        }

        @Test
        @DisplayName("Should return 404 when job not found for refresh")
        void shouldReturnNotFoundWhenJobNotFoundForRefresh() {
            when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
            when(trainingJobService.getJob(any(), any()))
                    .thenReturn(Optional.empty());

            ResponseEntity<TrainingJobResponseDto> response =
                    controller.refreshJobStatus(jobId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(trainingJobService, never()).syncJobStatus(any());
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should use organization from security context")
        void shouldUseOrganizationFromSecurityContext() {
            Organization differentOrg = new Organization();
            differentOrg.setId(999L);

            when(securityUtils.getCurrentUserOrganization()).thenReturn(differentOrg);
            when(trainingJobService.getJob(jobId, 999L))
                    .thenReturn(Optional.empty());

            controller.getJob(jobId);

            verify(trainingJobService).getJob(jobId, 999L);
        }

        @Test
        @DisplayName("Should extract user from security context for training")
        void shouldExtractUserFromSecurityContextForTraining() {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(trainingJobService.startTraining(any(), eq(orgId), any(), any()))
                    .thenReturn(testJob);
            when(trainingJobService.toResponse(testJob)).thenReturn(testJobResponse);

            controller.startTraining(modelId, null);

            verify(securityUtils).getCurrentUser();
            verify(trainingJobService).startTraining(eq(modelId), eq(orgId), anyLong(), any());
        }
    }
}
