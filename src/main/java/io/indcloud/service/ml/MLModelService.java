package io.indcloud.service.ml;

import io.indcloud.dto.ml.MLModelResponseDto;
import io.indcloud.model.*;
import io.indcloud.repository.MLModelRepository;
import io.indcloud.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing ML models.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MLModelService {

    private final MLModelRepository mlModelRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Get all models for an organization.
     */
    @Transactional(readOnly = true)
    public Page<MLModel> getModels(Long organizationId, Pageable pageable) {
        return mlModelRepository.findByOrganizationId(organizationId, pageable);
    }

    /**
     * Get a model by ID.
     */
    @Transactional(readOnly = true)
    public Optional<MLModel> getModel(UUID modelId, Long organizationId) {
        return mlModelRepository.findByIdAndOrganizationId(modelId, organizationId);
    }

    /**
     * Get models by type.
     */
    @Transactional(readOnly = true)
    public List<MLModel> getModelsByType(Long organizationId, MLModelType modelType) {
        return mlModelRepository.findByOrganizationIdAndModelType(organizationId, modelType);
    }

    /**
     * Get deployed models.
     */
    @Transactional(readOnly = true)
    public List<MLModel> getDeployedModels(Long organizationId) {
        return mlModelRepository.findByOrganizationIdAndStatus(organizationId, MLModelStatus.DEPLOYED);
    }

    /**
     * Create a new ML model.
     */
    @Transactional
    public MLModel createModel(Long organizationId, String name, MLModelType modelType,
                                String algorithm, Map<String, Object> hyperparameters,
                                List<String> featureColumns, UUID createdBy) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        // Check for duplicate name/version
        String version = "1.0.0";
        if (mlModelRepository.existsByOrganizationIdAndNameAndVersion(organizationId, name, version)) {
            throw new IllegalArgumentException("Model with name '" + name + "' version '" + version + "' already exists");
        }

        MLModel model = MLModel.builder()
                .organization(org)
                .name(name)
                .modelType(modelType)
                .algorithm(algorithm)
                .version(version)
                .hyperparameters(hyperparameters)
                .featureColumns(featureColumns)
                .status(MLModelStatus.DRAFT)
                .createdBy(createdBy)
                .build();

        MLModel saved = mlModelRepository.save(model);
        log.info("Created ML model: {} (type={}, algorithm={})", saved.getId(), modelType, algorithm);
        return saved;
    }

    /**
     * Update model configuration.
     */
    @Transactional
    public MLModel updateModel(UUID modelId, Long organizationId, String name,
                                Map<String, Object> hyperparameters, List<String> featureColumns,
                                String inferenceSchedule, BigDecimal confidenceThreshold,
                                BigDecimal anomalyThreshold) {
        MLModel model = mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        if (model.getStatus() == MLModelStatus.DEPLOYED) {
            throw new IllegalStateException("Cannot update deployed model. Archive it first.");
        }

        if (name != null) {
            model.setName(name);
        }
        if (hyperparameters != null) {
            model.setHyperparameters(hyperparameters);
        }
        if (featureColumns != null) {
            model.setFeatureColumns(featureColumns);
        }
        if (inferenceSchedule != null) {
            model.setInferenceSchedule(inferenceSchedule);
        }
        if (confidenceThreshold != null) {
            model.setConfidenceThreshold(confidenceThreshold);
        }
        if (anomalyThreshold != null) {
            model.setAnomalyThreshold(anomalyThreshold);
        }

        MLModel updated = mlModelRepository.save(model);
        log.info("Updated ML model: {}", modelId);
        return updated;
    }

    /**
     * Deploy a trained model.
     */
    @Transactional
    public MLModel deployModel(UUID modelId, Long organizationId) {
        MLModel model = mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        if (model.getStatus() != MLModelStatus.TRAINED) {
            throw new IllegalStateException("Only trained models can be deployed. Current status: " + model.getStatus());
        }

        model.setStatus(MLModelStatus.DEPLOYED);
        model.setDeployedAt(Instant.now());
        model.setNextInferenceAt(Instant.now()); // Start inference immediately

        MLModel deployed = mlModelRepository.save(model);
        log.info("Deployed ML model: {}", modelId);
        return deployed;
    }

    /**
     * Archive a model.
     */
    @Transactional
    public MLModel archiveModel(UUID modelId, Long organizationId) {
        MLModel model = mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        model.setStatus(MLModelStatus.ARCHIVED);
        model.setNextInferenceAt(null);

        MLModel archived = mlModelRepository.save(model);
        log.info("Archived ML model: {}", modelId);
        return archived;
    }

    /**
     * Delete a model.
     */
    @Transactional
    public void deleteModel(UUID modelId, Long organizationId) {
        MLModel model = mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        if (model.getStatus() == MLModelStatus.DEPLOYED) {
            throw new IllegalStateException("Cannot delete deployed model. Archive it first.");
        }

        mlModelRepository.delete(model);
        log.info("Deleted ML model: {}", modelId);
    }

    /**
     * Mark model as training.
     */
    @Transactional
    public MLModel startTraining(UUID modelId, Long organizationId, UUID trainedBy) {
        MLModel model = mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        if (model.getStatus() == MLModelStatus.TRAINING) {
            throw new IllegalStateException("Model is already training");
        }

        model.setStatus(MLModelStatus.TRAINING);
        model.setTrainedBy(trainedBy);

        return mlModelRepository.save(model);
    }

    /**
     * Mark training as complete.
     */
    @Transactional
    public MLModel completeTraining(UUID modelId, Long organizationId,
                                     Map<String, Object> trainingMetrics,
                                     Map<String, Object> validationMetrics,
                                     String modelPath, Long modelSizeBytes) {
        MLModel model = mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        model.setStatus(MLModelStatus.TRAINED);
        model.setTrainedAt(Instant.now());
        model.setTrainingMetrics(trainingMetrics);
        model.setValidationMetrics(validationMetrics);
        model.setModelPath(modelPath);
        model.setModelSizeBytes(modelSizeBytes);

        // Increment version if retrained
        if (model.getTrainedAt() != null) {
            String[] parts = model.getVersion().split("\\.");
            int minor = Integer.parseInt(parts[1]) + 1;
            model.setVersion(parts[0] + "." + minor + "." + parts[2]);
        }

        MLModel trained = mlModelRepository.save(model);
        log.info("Training completed for model: {} (version={})", modelId, trained.getVersion());
        return trained;
    }

    /**
     * Mark training as failed.
     */
    @Transactional
    public MLModel failTraining(UUID modelId, Long organizationId) {
        MLModel model = mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        model.setStatus(MLModelStatus.FAILED);

        return mlModelRepository.save(model);
    }

    /**
     * Update last inference timestamp and schedule next.
     */
    @Transactional
    public void updateInferenceTimestamp(UUID modelId, Instant nextInferenceAt) {
        mlModelRepository.findById(modelId).ifPresent(model -> {
            model.setLastInferenceAt(Instant.now());
            model.setNextInferenceAt(nextInferenceAt);
            mlModelRepository.save(model);
        });
    }

    /**
     * Get models ready for inference.
     */
    @Transactional(readOnly = true)
    public List<MLModel> getModelsReadyForInference() {
        return mlModelRepository.findDeployedModelsReadyForInference(Instant.now());
    }

    /**
     * Count deployed models for an organization.
     */
    @Transactional(readOnly = true)
    public long countDeployedModels(Long organizationId) {
        return mlModelRepository.countDeployedModelsByOrganization(organizationId);
    }

    /**
     * Convert model entity to DTO.
     */
    public MLModelResponseDto toDto(MLModel model) {
        return MLModelResponseDto.builder()
                .id(model.getId())
                .organizationId(model.getOrganization().getId())
                .name(model.getName())
                .modelType(model.getModelType().name())
                .version(model.getVersion())
                .algorithm(model.getAlgorithm())
                .hyperparameters(model.getHyperparameters())
                .featureColumns(model.getFeatureColumns())
                .targetColumn(model.getTargetColumn())
                .status(model.getStatus().name())
                .modelPath(model.getModelPath())
                .modelSizeBytes(model.getModelSizeBytes())
                .trainingMetrics(model.getTrainingMetrics())
                .validationMetrics(model.getValidationMetrics())
                .deviceScope(model.getDeviceScope())
                .deviceIds(model.getDeviceIds())
                .deviceGroupId(model.getDeviceGroupId())
                .inferenceSchedule(model.getInferenceSchedule())
                .lastInferenceAt(model.getLastInferenceAt())
                .nextInferenceAt(model.getNextInferenceAt())
                .confidenceThreshold(model.getConfidenceThreshold())
                .anomalyThreshold(model.getAnomalyThreshold())
                .createdBy(model.getCreatedBy())
                .trainedBy(model.getTrainedBy())
                .trainedAt(model.getTrainedAt())
                .deployedAt(model.getDeployedAt())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
