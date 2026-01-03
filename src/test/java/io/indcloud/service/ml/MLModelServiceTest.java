package io.indcloud.service.ml;

import io.indcloud.model.*;
import io.indcloud.repository.MLModelRepository;
import io.indcloud.repository.OrganizationRepository;
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
@DisplayName("MLModelService Tests")
class MLModelServiceTest {

    @Mock
    private MLModelRepository mlModelRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private MLModelService mlModelService;

    private Organization testOrg;
    private MLModel testModel;
    private UUID modelId;
    private Long orgId = 1L;
    private UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testOrg = new Organization();
        testOrg.setId(orgId);
        testOrg.setName("Test Organization");

        modelId = UUID.randomUUID();
        testModel = MLModel.builder()
                .id(modelId)
                .organization(testOrg)
                .name("Test Model")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .algorithm("isolation_forest")
                .version("1.0.0")
                .status(MLModelStatus.DRAFT)
                .build();
    }

    @Nested
    @DisplayName("Get Models Tests")
    class GetModelsTests {

        @Test
        @DisplayName("Should get models by organization")
        void shouldGetModelsByOrganization() {
            Page<MLModel> page = new PageImpl<>(List.of(testModel));
            when(mlModelRepository.findByOrganizationId(eq(orgId), any())).thenReturn(page);

            Page<MLModel> result = mlModelService.getModels(orgId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(mlModelRepository).findByOrganizationId(orgId, PageRequest.of(0, 10));
        }

        @Test
        @DisplayName("Should get model by ID")
        void shouldGetModelById() {
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));

            Optional<MLModel> result = mlModelService.getModel(modelId, orgId);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Test Model");
        }

        @Test
        @DisplayName("Should get models by type")
        void shouldGetModelsByType() {
            when(mlModelRepository.findByOrganizationIdAndModelType(orgId, MLModelType.ANOMALY_DETECTION))
                    .thenReturn(List.of(testModel));

            List<MLModel> result = mlModelService.getModelsByType(orgId, MLModelType.ANOMALY_DETECTION);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getModelType()).isEqualTo(MLModelType.ANOMALY_DETECTION);
        }

        @Test
        @DisplayName("Should get deployed models")
        void shouldGetDeployedModels() {
            testModel.setStatus(MLModelStatus.DEPLOYED);
            when(mlModelRepository.findByOrganizationIdAndStatus(orgId, MLModelStatus.DEPLOYED))
                    .thenReturn(List.of(testModel));

            List<MLModel> result = mlModelService.getDeployedModels(orgId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(MLModelStatus.DEPLOYED);
        }
    }

    @Nested
    @DisplayName("Create Model Tests")
    class CreateModelTests {

        @Test
        @DisplayName("Should create a new model")
        void shouldCreateModel() {
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
            when(mlModelRepository.existsByOrganizationIdAndNameAndVersion(any(), any(), any()))
                    .thenReturn(false);
            when(mlModelRepository.save(any())).thenAnswer(inv -> {
                MLModel m = inv.getArgument(0);
                m.setId(UUID.randomUUID());
                return m;
            });

            MLModel result = mlModelService.createModel(
                    orgId, "New Model", MLModelType.ANOMALY_DETECTION,
                    "isolation_forest", Map.of("n_estimators", 100),
                    List.of("temperature", "pressure"), userId);

            assertThat(result.getName()).isEqualTo("New Model");
            assertThat(result.getStatus()).isEqualTo(MLModelStatus.DRAFT);
            assertThat(result.getVersion()).isEqualTo("1.0.0");
            assertThat(result.getCreatedBy()).isEqualTo(userId);

            ArgumentCaptor<MLModel> captor = ArgumentCaptor.forClass(MLModel.class);
            verify(mlModelRepository).save(captor.capture());
            assertThat(captor.getValue().getFeatureColumns()).containsExactly("temperature", "pressure");
        }

        @Test
        @DisplayName("Should throw when organization not found")
        void shouldThrowWhenOrgNotFound() {
            when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mlModelService.createModel(
                    999L, "Model", MLModelType.ANOMALY_DETECTION,
                    "algo", null, null, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Organization not found");
        }

        @Test
        @DisplayName("Should throw when duplicate model exists")
        void shouldThrowWhenDuplicateExists() {
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
            when(mlModelRepository.existsByOrganizationIdAndNameAndVersion(orgId, "Duplicate", "1.0.0"))
                    .thenReturn(true);

            assertThatThrownBy(() -> mlModelService.createModel(
                    orgId, "Duplicate", MLModelType.ANOMALY_DETECTION,
                    "algo", null, null, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("Update Model Tests")
    class UpdateModelTests {

        @Test
        @DisplayName("Should update model")
        void shouldUpdateModel() {
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLModel result = mlModelService.updateModel(
                    modelId, orgId, "Updated Name",
                    Map.of("n_estimators", 200),
                    List.of("temp"), "0 */30 * * * *",
                    new BigDecimal("0.9"), new BigDecimal("0.4"));

            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getHyperparameters()).containsEntry("n_estimators", 200);
            assertThat(result.getInferenceSchedule()).isEqualTo("0 */30 * * * *");
        }

        @Test
        @DisplayName("Should throw when updating deployed model")
        void shouldThrowWhenUpdatingDeployed() {
            testModel.setStatus(MLModelStatus.DEPLOYED);
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));

            assertThatThrownBy(() -> mlModelService.updateModel(
                    modelId, orgId, "New Name", null, null, null, null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot update deployed model");
        }

        @Test
        @DisplayName("Should throw when model not found")
        void shouldThrowWhenModelNotFound() {
            when(mlModelRepository.findByIdAndOrganizationId(any(), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> mlModelService.updateModel(
                    modelId, orgId, "Name", null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Model not found");
        }
    }

    @Nested
    @DisplayName("Deploy Model Tests")
    class DeployModelTests {

        @Test
        @DisplayName("Should deploy trained model")
        void shouldDeployTrainedModel() {
            testModel.setStatus(MLModelStatus.TRAINED);
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLModel result = mlModelService.deployModel(modelId, orgId);

            assertThat(result.getStatus()).isEqualTo(MLModelStatus.DEPLOYED);
            assertThat(result.getDeployedAt()).isNotNull();
            assertThat(result.getNextInferenceAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw when deploying non-trained model")
        void shouldThrowWhenDeployingNonTrained() {
            testModel.setStatus(MLModelStatus.DRAFT);
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));

            assertThatThrownBy(() -> mlModelService.deployModel(modelId, orgId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only trained models can be deployed");
        }
    }

    @Nested
    @DisplayName("Archive Model Tests")
    class ArchiveModelTests {

        @Test
        @DisplayName("Should archive model")
        void shouldArchiveModel() {
            testModel.setStatus(MLModelStatus.DEPLOYED);
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLModel result = mlModelService.archiveModel(modelId, orgId);

            assertThat(result.getStatus()).isEqualTo(MLModelStatus.ARCHIVED);
            assertThat(result.getNextInferenceAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Delete Model Tests")
    class DeleteModelTests {

        @Test
        @DisplayName("Should delete non-deployed model")
        void shouldDeleteNonDeployedModel() {
            testModel.setStatus(MLModelStatus.DRAFT);
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));

            mlModelService.deleteModel(modelId, orgId);

            verify(mlModelRepository).delete(testModel);
        }

        @Test
        @DisplayName("Should throw when deleting deployed model")
        void shouldThrowWhenDeletingDeployed() {
            testModel.setStatus(MLModelStatus.DEPLOYED);
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));

            assertThatThrownBy(() -> mlModelService.deleteModel(modelId, orgId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete deployed model");

            verify(mlModelRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Training Lifecycle Tests")
    class TrainingLifecycleTests {

        @Test
        @DisplayName("Should start training")
        void shouldStartTraining() {
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLModel result = mlModelService.startTraining(modelId, orgId, userId);

            assertThat(result.getStatus()).isEqualTo(MLModelStatus.TRAINING);
            assertThat(result.getTrainedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw when already training")
        void shouldThrowWhenAlreadyTraining() {
            testModel.setStatus(MLModelStatus.TRAINING);
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));

            assertThatThrownBy(() -> mlModelService.startTraining(modelId, orgId, userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already training");
        }

        @Test
        @DisplayName("Should complete training")
        void shouldCompleteTraining() {
            testModel.setStatus(MLModelStatus.TRAINING);
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> metrics = Map.of("accuracy", 0.95);
            MLModel result = mlModelService.completeTraining(
                    modelId, orgId, metrics, metrics, "/models/test.joblib", 1024L);

            assertThat(result.getStatus()).isEqualTo(MLModelStatus.TRAINED);
            assertThat(result.getTrainedAt()).isNotNull();
            assertThat(result.getTrainingMetrics()).containsEntry("accuracy", 0.95);
            assertThat(result.getModelPath()).isEqualTo("/models/test.joblib");
            assertThat(result.getModelSizeBytes()).isEqualTo(1024L);
        }

        @Test
        @DisplayName("Should fail training")
        void shouldFailTraining() {
            testModel.setStatus(MLModelStatus.TRAINING);
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MLModel result = mlModelService.failTraining(modelId, orgId);

            assertThat(result.getStatus()).isEqualTo(MLModelStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("Inference Tests")
    class InferenceTests {

        @Test
        @DisplayName("Should get models ready for inference")
        void shouldGetModelsReadyForInference() {
            testModel.setStatus(MLModelStatus.DEPLOYED);
            when(mlModelRepository.findDeployedModelsReadyForInference(any()))
                    .thenReturn(List.of(testModel));

            List<MLModel> result = mlModelService.getModelsReadyForInference();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should update inference timestamp")
        void shouldUpdateInferenceTimestamp() {
            when(mlModelRepository.findById(modelId)).thenReturn(Optional.of(testModel));
            when(mlModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Instant nextInference = Instant.now().plusSeconds(3600);
            mlModelService.updateInferenceTimestamp(modelId, nextInference);

            ArgumentCaptor<MLModel> captor = ArgumentCaptor.forClass(MLModel.class);
            verify(mlModelRepository).save(captor.capture());
            assertThat(captor.getValue().getLastInferenceAt()).isNotNull();
            assertThat(captor.getValue().getNextInferenceAt()).isEqualTo(nextInference);
        }

        @Test
        @DisplayName("Should count deployed models")
        void shouldCountDeployedModels() {
            when(mlModelRepository.countDeployedModelsByOrganization(orgId)).thenReturn(5L);

            long count = mlModelService.countDeployedModels(orgId);

            assertThat(count).isEqualTo(5L);
        }
    }

    @Test
    @DisplayName("Should convert model to DTO")
    void shouldConvertToDto() {
        testModel.setTrainingMetrics(Map.of("accuracy", 0.95));
        testModel.setValidationMetrics(Map.of("f1", 0.92));

        var dto = mlModelService.toDto(testModel);

        assertThat(dto.getId()).isEqualTo(modelId);
        assertThat(dto.getName()).isEqualTo("Test Model");
        assertThat(dto.getModelType()).isEqualTo("ANOMALY_DETECTION");
        assertThat(dto.getStatus()).isEqualTo("DRAFT");
        assertThat(dto.getTrainingMetrics()).containsEntry("accuracy", 0.95);
    }
}
