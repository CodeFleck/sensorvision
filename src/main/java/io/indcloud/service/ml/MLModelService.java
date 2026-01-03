package io.indcloud.service.ml;

import io.indcloud.dto.ml.MLModelResponse;
import io.indcloud.model.*;
import io.indcloud.repository.MLModelRepository;
import io.indcloud.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
     * Get models by type with pagination.
     */
    @Transactional(readOnly = true)
    public Page<MLModel> getModelsByType(Long organizationId, MLModelType modelType, Pageable pageable) {
        List<MLModel> models = mlModelRepository.findByOrganizationIdAndModelType(organizationId, modelType);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), models.size());
        List<MLModel> pageContent = start > models.size() ? List.of() : models.subList(start, end);
        return new PageImpl<>(pageContent, pageable, models.size());
    }

    /**
     * Get deployed models.
     */
    @Transactional(readOnly = true)
    public List<MLModel> getDeployedModels(Long organizationId) {
        return mlModelRepository.findByOrganizationIdAndStatus(organizationId, MLModelStatus.DEPLOYED);
    }

    /**
     * Create a new ML model with all configuration options.
     */
    @Transactional
    public MLModel createModel(Long organizationId, String name, MLModelType modelType,
                                String algorithm, String version, Map<String, Object> hyperparameters,
                                List<String> featureColumns, String targetColumn,
                                String deviceScope, List<UUID> deviceIds, Long deviceGroupId,
                                String inferenceSchedule, BigDecimal confidenceThreshold,
                                BigDecimal anomalyThreshold, Long createdBy) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        String effectiveVersion = version != null ? version : "1.0.0";

        // Check for duplicate name/version
        if (mlModelRepository.existsByOrganizationIdAndNameAndVersion(organizationId, name, effectiveVersion)) {
            throw new IllegalArgumentException("Model with name '" + name + "' version '" + effectiveVersion + "' already exists");
        }

        MLModel model = MLModel.builder()
                .organization(org)
                .name(name)
                .modelType(modelType)
                .algorithm(algorithm)
                .version(effectiveVersion)
                .hyperparameters(hyperparameters)
                .featureColumns(featureColumns)
                .targetColumn(targetColumn)
                .deviceScope(deviceScope != null ? deviceScope : "ALL")
                .deviceIds(deviceIds)
                .deviceGroupId(deviceGroupId)
                .inferenceSchedule(inferenceSchedule)
                .confidenceThreshold(confidenceThreshold != null ? confidenceThreshold : new BigDecimal("0.8"))
                .anomalyThreshold(anomalyThreshold != null ? anomalyThreshold : new BigDecimal("0.5"))
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
    public Optional<MLModel> updateModel(UUID modelId, Long organizationId, String name,
                                          Map<String, Object> hyperparameters, List<String> featureColumns,
                                          String targetColumn, String deviceScope, List<UUID> deviceIds,
                                          Long deviceGroupId, String inferenceSchedule,
                                          BigDecimal confidenceThreshold, BigDecimal anomalyThreshold) {
        return mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .map(model -> {
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
                    if (targetColumn != null) {
                        model.setTargetColumn(targetColumn);
                    }
                    if (deviceScope != null) {
                        model.setDeviceScope(deviceScope);
                    }
                    if (deviceIds != null) {
                        model.setDeviceIds(deviceIds);
                    }
                    if (deviceGroupId != null) {
                        model.setDeviceGroupId(deviceGroupId);
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
                });
    }

    /**
     * Deploy a trained model.
     */
    @Transactional
    public Optional<MLModel> deployModel(UUID modelId, Long organizationId) {
        return mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .map(model -> {
                    if (model.getStatus() != MLModelStatus.TRAINED) {
                        throw new IllegalStateException("Only trained models can be deployed. Current status: " + model.getStatus());
                    }

                    model.setStatus(MLModelStatus.DEPLOYED);
                    model.setDeployedAt(Instant.now());
                    model.setNextInferenceAt(Instant.now());

                    MLModel deployed = mlModelRepository.save(model);
                    log.info("Deployed ML model: {}", modelId);
                    return deployed;
                });
    }

    /**
     * Archive a model.
     */
    @Transactional
    public Optional<MLModel> archiveModel(UUID modelId, Long organizationId) {
        return mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .map(model -> {
                    model.setStatus(MLModelStatus.ARCHIVED);
                    model.setNextInferenceAt(null);

                    MLModel archived = mlModelRepository.save(model);
                    log.info("Archived ML model: {}", modelId);
                    return archived;
                });
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
    public Optional<MLModel> startTraining(UUID modelId, Long organizationId, Long trainedBy) {
        return mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .map(model -> {
                    if (model.getStatus() == MLModelStatus.TRAINING) {
                        throw new IllegalStateException("Model is already training");
                    }

                    model.setStatus(MLModelStatus.TRAINING);
                    model.setTrainedBy(trainedBy);

                    return mlModelRepository.save(model);
                });
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
     * Convert model entity to response DTO.
     */
    public MLModelResponse toResponse(MLModel model) {
        return MLModelResponse.builder()
                .id(model.getId())
                .organizationId(model.getOrganization() != null ? model.getOrganization().getId() : null)
                .name(model.getName())
                .modelType(model.getModelType())
                .version(model.getVersion())
                .algorithm(model.getAlgorithm())
                .hyperparameters(model.getHyperparameters())
                .featureColumns(model.getFeatureColumns())
                .targetColumn(model.getTargetColumn())
                .status(model.getStatus())
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
