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
 * Unit tests for MLModelRepository.
 * These tests verify repository method contracts using Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MLModelRepository Tests")
class MLModelRepositoryTest {

    @Mock
    private MLModelRepository mlModelRepository;

    private Organization testOrg;
    private MLModel testModel;
    private Long orgId = 1L;

    @BeforeEach
    void setUp() {
        testOrg = new Organization();
        testOrg.setId(orgId);
        testOrg.setName("Test Organization");

        testModel = MLModel.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .name("Test Model")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .algorithm("isolation_forest")
                .version("1.0.0")
                .status(MLModelStatus.DRAFT)
                .confidenceThreshold(new BigDecimal("0.8"))
                .anomalyThreshold(new BigDecimal("0.5"))
                .build();
    }

    @Nested
    @DisplayName("Find By Organization Tests")
    class FindByOrganizationTests {

        @Test
        @DisplayName("Should find models by organization")
        void shouldFindByOrganization() {
            Page<MLModel> page = new PageImpl<>(List.of(testModel));
            when(mlModelRepository.findByOrganizationId(eq(orgId), any())).thenReturn(page);

            Page<MLModel> result = mlModelRepository.findByOrganizationId(orgId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(mlModelRepository).findByOrganizationId(eq(orgId), any());
        }

        @Test
        @DisplayName("Should find models by organization and status")
        void shouldFindByOrganizationAndStatus() {
            testModel.setStatus(MLModelStatus.DEPLOYED);
            when(mlModelRepository.findByOrganizationIdAndStatus(orgId, MLModelStatus.DEPLOYED))
                    .thenReturn(List.of(testModel));

            List<MLModel> result = mlModelRepository.findByOrganizationIdAndStatus(orgId, MLModelStatus.DEPLOYED);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(MLModelStatus.DEPLOYED);
        }

        @Test
        @DisplayName("Should find models by organization and type")
        void shouldFindByOrganizationAndType() {
            when(mlModelRepository.findByOrganizationIdAndModelType(orgId, MLModelType.ANOMALY_DETECTION))
                    .thenReturn(List.of(testModel));

            List<MLModel> result = mlModelRepository.findByOrganizationIdAndModelType(
                    orgId, MLModelType.ANOMALY_DETECTION);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getModelType()).isEqualTo(MLModelType.ANOMALY_DETECTION);
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find by id and organization")
        void shouldFindByIdAndOrganization() {
            UUID modelId = testModel.getId();
            when(mlModelRepository.findByIdAndOrganizationId(modelId, orgId))
                    .thenReturn(Optional.of(testModel));

            Optional<MLModel> result = mlModelRepository.findByIdAndOrganizationId(modelId, orgId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(modelId);
        }

        @Test
        @DisplayName("Should return empty when id not found")
        void shouldReturnEmptyWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(mlModelRepository.findByIdAndOrganizationId(unknownId, orgId))
                    .thenReturn(Optional.empty());

            Optional<MLModel> result = mlModelRepository.findByIdAndOrganizationId(unknownId, orgId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for wrong organization")
        void shouldReturnEmptyForWrongOrg() {
            UUID modelId = testModel.getId();
            when(mlModelRepository.findByIdAndOrganizationId(modelId, 999L))
                    .thenReturn(Optional.empty());

            Optional<MLModel> result = mlModelRepository.findByIdAndOrganizationId(modelId, 999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Inference Ready Tests")
    class InferenceReadyTests {

        @Test
        @DisplayName("Should find deployed models ready for inference")
        void shouldFindModelsReadyForInference() {
            Instant now = Instant.now();
            testModel.setStatus(MLModelStatus.DEPLOYED);
            testModel.setNextInferenceAt(now.minusSeconds(60));
            when(mlModelRepository.findDeployedModelsReadyForInference(any()))
                    .thenReturn(List.of(testModel));

            List<MLModel> result = mlModelRepository.findDeployedModelsReadyForInference(now);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(MLModelStatus.DEPLOYED);
        }

        @Test
        @DisplayName("Should not find models with future inference time")
        void shouldNotFindModelsWithFutureInference() {
            Instant now = Instant.now();
            when(mlModelRepository.findDeployedModelsReadyForInference(now))
                    .thenReturn(List.of());

            List<MLModel> result = mlModelRepository.findDeployedModelsReadyForInference(now);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Should count deployed models by organization")
        void shouldCountDeployedModels() {
            when(mlModelRepository.countDeployedModelsByOrganization(orgId)).thenReturn(5L);

            long count = mlModelRepository.countDeployedModelsByOrganization(orgId);

            assertThat(count).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should return zero when no deployed models")
        void shouldReturnZeroWhenNoDeployed() {
            when(mlModelRepository.countDeployedModelsByOrganization(orgId)).thenReturn(0L);

            long count = mlModelRepository.countDeployedModelsByOrganization(orgId);

            assertThat(count).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Existence Tests")
    class ExistenceTests {

        @Test
        @DisplayName("Should check existence by organization, name, and version")
        void shouldCheckExistence() {
            when(mlModelRepository.existsByOrganizationIdAndNameAndVersion(orgId, "Test Model", "1.0.0"))
                    .thenReturn(true);

            boolean exists = mlModelRepository.existsByOrganizationIdAndNameAndVersion(
                    orgId, "Test Model", "1.0.0");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when model doesn't exist")
        void shouldReturnFalseWhenNotExists() {
            when(mlModelRepository.existsByOrganizationIdAndNameAndVersion(orgId, "Unknown", "1.0.0"))
                    .thenReturn(false);

            boolean exists = mlModelRepository.existsByOrganizationIdAndNameAndVersion(
                    orgId, "Unknown", "1.0.0");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Complex Query Tests")
    class ComplexQueryTests {

        @Test
        @DisplayName("Should find by organization, status, and type")
        void shouldFindByOrganizationStatusAndType() {
            testModel.setStatus(MLModelStatus.DEPLOYED);
            when(mlModelRepository.findByOrganizationStatusAndType(
                    orgId, MLModelStatus.DEPLOYED, MLModelType.ANOMALY_DETECTION))
                    .thenReturn(List.of(testModel));

            List<MLModel> result = mlModelRepository.findByOrganizationStatusAndType(
                    orgId, MLModelStatus.DEPLOYED, MLModelType.ANOMALY_DETECTION);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(MLModelStatus.DEPLOYED);
            assertThat(result.get(0).getModelType()).isEqualTo(MLModelType.ANOMALY_DETECTION);
        }

        @Test
        @DisplayName("Should return empty list when no match")
        void shouldReturnEmptyWhenNoMatch() {
            when(mlModelRepository.findByOrganizationStatusAndType(
                    orgId, MLModelStatus.DEPLOYED, MLModelType.ENERGY_FORECAST))
                    .thenReturn(List.of());

            List<MLModel> result = mlModelRepository.findByOrganizationStatusAndType(
                    orgId, MLModelStatus.DEPLOYED, MLModelType.ENERGY_FORECAST);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Save and Delete Tests")
    class SaveAndDeleteTests {

        @Test
        @DisplayName("Should save model")
        void shouldSaveModel() {
            when(mlModelRepository.save(any(MLModel.class))).thenAnswer(inv -> {
                MLModel m = inv.getArgument(0);
                if (m.getId() == null) {
                    m.setId(UUID.randomUUID());
                }
                return m;
            });

            MLModel newModel = MLModel.builder()
                    .organization(testOrg)
                    .name("New Model")
                    .modelType(MLModelType.PREDICTIVE_MAINTENANCE)
                    .algorithm("gradient_boosting")
                    .version("1.0.0")
                    .build();

            MLModel saved = mlModelRepository.save(newModel);

            assertThat(saved.getId()).isNotNull();
            verify(mlModelRepository).save(newModel);
        }

        @Test
        @DisplayName("Should find by id")
        void shouldFindById() {
            UUID modelId = testModel.getId();
            when(mlModelRepository.findById(modelId)).thenReturn(Optional.of(testModel));

            Optional<MLModel> result = mlModelRepository.findById(modelId);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Should delete model")
        void shouldDeleteModel() {
            doNothing().when(mlModelRepository).delete(testModel);

            mlModelRepository.delete(testModel);

            verify(mlModelRepository).delete(testModel);
        }
    }
}
