package io.indcloud.controller;

import io.indcloud.dto.ml.MLModelCreateRequest;
import io.indcloud.dto.ml.MLModelResponse;
import io.indcloud.dto.ml.MLModelUpdateRequest;
import io.indcloud.dto.ml.TrainingJobResponseDto;
import io.indcloud.model.*;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.ml.MLModelService;
import io.indcloud.service.ml.MLTrainingJobService;
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
 * Unit tests for MLModelController.
 * Tests REST API endpoints for ML model management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MLModelController Tests")
class MLModelControllerTest {

    @Mock
    private MLModelService mlModelService;

    @Mock
    private MLTrainingJobService trainingJobService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private MLModelController mlModelController;

    private Organization testOrganization;
    private User testUser;
    private MLModel testModel;
    private MLModelResponse testModelResponse;
    private UUID modelId;
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

        modelId = UUID.randomUUID();
        testModel = MLModel.builder()
                .id(modelId)
                .organization(testOrganization)
                .name("Test Anomaly Model")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .algorithm("isolation_forest")
                .version("1.0.0")
                .status(MLModelStatus.DRAFT)
                .createdBy(userId)
                .build();

        testModelResponse = MLModelResponse.builder()
                .id(modelId)
                .organizationId(orgId)
                .name("Test Anomaly Model")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .algorithm("isolation_forest")
                .version("1.0.0")
                .status(MLModelStatus.DRAFT)
                .createdBy(userId)
                .build();

        lenient().when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        lenient().when(securityUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("GET /api/v1/ml/models")
    class GetModelsTests {

        @Test
        @DisplayName("Should return paginated list of models")
        void shouldReturnPaginatedModels() {
            // Given
            Page<MLModel> modelPage = new PageImpl<>(List.of(testModel));
            Pageable pageable = PageRequest.of(0, 20);

            when(mlModelService.getModels(orgId, pageable)).thenReturn(modelPage);

            // When
            Page<MLModelResponse> response = mlModelController.getModels(pageable, null, null);

            // Then
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent().get(0).getName()).isEqualTo("Test Anomaly Model");
            verify(mlModelService).getModels(orgId, pageable);
        }

        @Test
        @DisplayName("Should filter by model type")
        void shouldFilterByModelType() {
            // Given
            Page<MLModel> modelPage = new PageImpl<>(List.of(testModel));
            Pageable pageable = PageRequest.of(0, 20);

            when(mlModelService.getModelsByType(eq(orgId), eq(MLModelType.ANOMALY_DETECTION), any()))
                    .thenReturn(modelPage);

            // When
            Page<MLModelResponse> response = mlModelController.getModels(pageable, MLModelType.ANOMALY_DETECTION, null);

            // Then
            assertThat(response.getTotalElements()).isEqualTo(1);
            verify(mlModelService).getModelsByType(orgId, MLModelType.ANOMALY_DETECTION, pageable);
        }

    }

    @Nested
    @DisplayName("GET /api/v1/ml/models/{id}")
    class GetModelByIdTests {

        @Test
        @DisplayName("Should return model by ID")
        void shouldReturnModelById() {
            // Given
            when(mlModelService.getModel(modelId, orgId)).thenReturn(Optional.of(testModel));

            // When
            ResponseEntity<MLModelResponse> response = mlModelController.getModel(modelId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(modelId);
        }

        @Test
        @DisplayName("Should return 404 when model not found")
        void shouldReturn404WhenNotFound() {
            // Given
            when(mlModelService.getModel(modelId, orgId)).thenReturn(Optional.empty());

            // When
            ResponseEntity<MLModelResponse> response = mlModelController.getModel(modelId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/models")
    class CreateModelTests {

        @Test
        @DisplayName("Should create new model")
        void shouldCreateModel() {
            // Given
            MLModelCreateRequest request = new MLModelCreateRequest();
            request.setName("New Model");
            request.setModelType(MLModelType.ANOMALY_DETECTION);
            request.setAlgorithm("isolation_forest");
            request.setVersion("1.0.0");
            request.setFeatureColumns(List.of("temperature", "pressure"));
            request.setConfidenceThreshold(new BigDecimal("0.8"));
            request.setAnomalyThreshold(new BigDecimal("0.5"));

            when(mlModelService.createModel(
                    eq(orgId), eq("New Model"), eq(MLModelType.ANOMALY_DETECTION),
                    eq("isolation_forest"), eq("1.0.0"), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), eq(userId)
            )).thenReturn(testModel);

            // When
            ResponseEntity<MLModelResponse> response = mlModelController.createModel(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Should throw when duplicate model exists")
        void shouldThrowWhenDuplicateExists() {
            // Given
            MLModelCreateRequest request = new MLModelCreateRequest();
            request.setName("Invalid Model");
            request.setModelType(MLModelType.ANOMALY_DETECTION);
            request.setAlgorithm("isolation_forest");

            when(mlModelService.createModel(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any()
            )).thenThrow(new IllegalArgumentException("Model already exists"));

            // When/Then - Exception propagates (handled by global exception handler)
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> mlModelController.createModel(request)
            );
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/ml/models/{id}")
    class UpdateModelTests {

        @Test
        @DisplayName("Should update model")
        void shouldUpdateModel() {
            // Given
            MLModelUpdateRequest request = new MLModelUpdateRequest();
            request.setName("Updated Model");
            request.setHyperparameters(Map.of("n_estimators", 200));

            when(mlModelService.updateModel(
                    eq(modelId), eq(orgId), eq("Updated Model"), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )).thenReturn(Optional.of(testModel));

            // When
            ResponseEntity<MLModelResponse> response = mlModelController.updateModel(modelId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should return 404 when model not found")
        void shouldReturn404WhenNotFound() {
            // Given
            MLModelUpdateRequest request = new MLModelUpdateRequest();
            request.setName("Updated Model");

            when(mlModelService.updateModel(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )).thenReturn(Optional.empty());

            // When
            ResponseEntity<MLModelResponse> response = mlModelController.updateModel(modelId, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw when updating deployed model")
        void shouldThrowWhenUpdatingDeployed() {
            // Given
            MLModelUpdateRequest request = new MLModelUpdateRequest();
            request.setName("Updated Model");

            when(mlModelService.updateModel(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )).thenThrow(new IllegalStateException("Cannot update deployed model"));

            // When/Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> mlModelController.updateModel(modelId, request)
            );
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/ml/models/{id}")
    class DeleteModelTests {

        @Test
        @DisplayName("Should delete model")
        void shouldDeleteModel() {
            // Given
            doNothing().when(mlModelService).deleteModel(modelId, orgId);

            // When
            mlModelController.deleteModel(modelId);

            // Then
            verify(mlModelService).deleteModel(modelId, orgId);
        }

        @Test
        @DisplayName("Should throw when model not found")
        void shouldThrowWhenNotFound() {
            // Given
            doThrow(new IllegalArgumentException("Model not found"))
                    .when(mlModelService).deleteModel(modelId, orgId);

            // When/Then - Exception propagates
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> mlModelController.deleteModel(modelId)
            );
        }

        @Test
        @DisplayName("Should throw when deleting deployed model")
        void shouldThrowWhenDeletingDeployed() {
            // Given
            doThrow(new IllegalStateException("Cannot delete deployed model"))
                    .when(mlModelService).deleteModel(modelId, orgId);

            // When/Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> mlModelController.deleteModel(modelId)
            );
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/models/{id}/deploy")
    class DeployModelTests {

        @Test
        @DisplayName("Should deploy trained model")
        void shouldDeployTrainedModel() {
            // Given
            testModel.setStatus(MLModelStatus.TRAINED);
            MLModel deployedModel = MLModel.builder()
                    .id(modelId)
                    .status(MLModelStatus.DEPLOYED)
                    .deployedAt(Instant.now())
                    .build();

            when(mlModelService.deployModel(modelId, orgId)).thenReturn(Optional.of(deployedModel));

            // When
            ResponseEntity<MLModelResponse> response = mlModelController.deployModel(modelId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo(MLModelStatus.DEPLOYED);
        }

        @Test
        @DisplayName("Should throw when deploying non-trained model")
        void shouldThrowWhenDeployingNonTrained() {
            // Given
            when(mlModelService.deployModel(modelId, orgId))
                    .thenThrow(new IllegalStateException("Only trained models can be deployed"));

            // When/Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> mlModelController.deployModel(modelId)
            );
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/models/{id}/archive")
    class ArchiveModelTests {

        @Test
        @DisplayName("Should archive model")
        void shouldArchiveModel() {
            // Given
            MLModel archivedModel = MLModel.builder()
                    .id(modelId)
                    .status(MLModelStatus.ARCHIVED)
                    .build();

            when(mlModelService.archiveModel(modelId, orgId)).thenReturn(Optional.of(archivedModel));

            // When
            ResponseEntity<MLModelResponse> response = mlModelController.archiveModel(modelId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo(MLModelStatus.ARCHIVED);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ml/models/{id}/train")
    class StartTrainingTests {

        @Test
        @DisplayName("Should start training and return TrainingStartResponse")
        void shouldStartTraining() {
            // Given
            MLModel trainingModel = MLModel.builder()
                    .id(modelId)
                    .organization(testOrganization)
                    .name("Test Model")
                    .modelType(MLModelType.ANOMALY_DETECTION)
                    .status(MLModelStatus.TRAINING)
                    .trainedBy(userId)
                    .build();

            UUID jobId = UUID.randomUUID();
            MLTrainingJob trainingJob = MLTrainingJob.builder()
                    .id(jobId)
                    .model(trainingModel)
                    .organization(testOrganization)
                    .jobType(MLTrainingJobType.INITIAL_TRAINING)
                    .status(MLTrainingJobStatus.PENDING)
                    .progressPercent(0)
                    .build();

            TrainingJobResponseDto jobResponse = TrainingJobResponseDto.builder()
                    .id(jobId)
                    .modelId(modelId)
                    .organizationId(orgId)
                    .jobType("INITIAL_TRAINING")
                    .status("PENDING")
                    .progressPercent(0)
                    .build();

            when(trainingJobService.startTraining(eq(modelId), eq(orgId), any(UUID.class), any()))
                    .thenReturn(trainingJob);
            when(trainingJobService.toResponse(trainingJob)).thenReturn(jobResponse);

            // When
            ResponseEntity<?> response = mlModelController.startTraining(modelId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isInstanceOf(MLModelController.TrainingStartResponse.class);

            MLModelController.TrainingStartResponse startResponse =
                    (MLModelController.TrainingStartResponse) response.getBody();
            assertThat(startResponse.model().getStatus()).isEqualTo(MLModelStatus.TRAINING);
            assertThat(startResponse.trainingJob().getId()).isEqualTo(jobId);
        }

        @Test
        @DisplayName("Should return conflict when already training")
        void shouldReturnConflictWhenAlreadyTraining() {
            // Given
            when(trainingJobService.startTraining(eq(modelId), eq(orgId), any(UUID.class), any()))
                    .thenThrow(new IllegalStateException("Model is already training"));

            // When
            ResponseEntity<?> response = mlModelController.startTraining(modelId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should return not found when model does not exist")
        void shouldReturnNotFoundWhenModelDoesNotExist() {
            // Given
            when(trainingJobService.startTraining(eq(modelId), eq(orgId), any(UUID.class), any()))
                    .thenThrow(new IllegalArgumentException("Model not found: " + modelId));

            // When
            ResponseEntity<?> response = mlModelController.startTraining(modelId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should return service unavailable when ML service is down")
        void shouldReturnServiceUnavailableWhenMLServiceIsDown() {
            // Given
            when(trainingJobService.startTraining(eq(modelId), eq(orgId), any(UUID.class), any()))
                    .thenThrow(new MLTrainingJobService.MLServiceException("ML service unavailable"));

            // When
            ResponseEntity<?> response = mlModelController.startTraining(modelId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/models/deployed")
    class GetDeployedModelsTests {

        @Test
        @DisplayName("Should return deployed models")
        void shouldReturnDeployedModels() {
            // Given
            testModel.setStatus(MLModelStatus.DEPLOYED);
            when(mlModelService.getDeployedModels(orgId)).thenReturn(List.of(testModel));

            // When
            List<MLModelResponse> response = mlModelController.getDeployedModels();

            // Then
            assertThat(response).hasSize(1);
            assertThat(response.get(0).getStatus()).isEqualTo(MLModelStatus.DEPLOYED);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ml/models/stats")
    class GetModelStatsTests {

        @Test
        @DisplayName("Should return model statistics")
        void shouldReturnModelStats() {
            // Given
            when(mlModelService.countDeployedModels(orgId)).thenReturn(3L);

            // When
            var response = mlModelController.getModelStats();

            // Then
            assertThat(response.deployedCount()).isEqualTo(3L);
        }
    }
}
