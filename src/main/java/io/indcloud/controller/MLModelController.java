package io.indcloud.controller;

import io.indcloud.dto.ml.MLModelCreateRequest;
import io.indcloud.dto.ml.MLModelResponse;
import io.indcloud.dto.ml.MLModelUpdateRequest;
import io.indcloud.dto.ml.TrainingJobResponseDto;
import io.indcloud.model.MLModel;
import io.indcloud.model.MLModelStatus;
import io.indcloud.model.MLModelType;
import io.indcloud.model.MLTrainingJob;
import io.indcloud.model.Organization;
import io.indcloud.model.User;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.ml.MLModelService;
import io.indcloud.service.ml.MLTrainingJobService;
import io.indcloud.service.ml.MLTrainingJobService.MLServiceException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for ML model management.
 * Provides endpoints for CRUD operations, model lifecycle management, and training.
 */
@RestController
@RequestMapping("/api/v1/ml/models")
@RequiredArgsConstructor
@Slf4j
public class MLModelController {

    private final MLModelService mlModelService;
    private final MLTrainingJobService trainingJobService;
    private final SecurityUtils securityUtils;

    /**
     * List all ML models for the current organization.
     */
    @GetMapping
    public Page<MLModelResponse> getModels(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) MLModelType type,
            @RequestParam(required = false) MLModelStatus status) {

        Organization org = securityUtils.getCurrentUserOrganization();
        Page<MLModel> models;

        if (type != null) {
            models = mlModelService.getModelsByType(org.getId(), type, pageable);
        } else {
            models = mlModelService.getModels(org.getId(), pageable);
        }

        return models.map(this::toResponse);
    }

    /**
     * Get a specific ML model by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MLModelResponse> getModel(@PathVariable UUID id) {
        Organization org = securityUtils.getCurrentUserOrganization();

        return mlModelService.getModel(id, org.getId())
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new ML model.
     */
    @PostMapping
    public ResponseEntity<MLModelResponse> createModel(@Valid @RequestBody MLModelCreateRequest request) {
        User user = securityUtils.getCurrentUser();
        Organization org = user.getOrganization();

        MLModel model = mlModelService.createModel(
                org.getId(),
                request.getName(),
                request.getModelType(),
                request.getAlgorithm(),
                request.getVersion(),
                request.getHyperparameters(),
                request.getFeatureColumns(),
                request.getTargetColumn(),
                request.getDeviceScope(),
                request.getDeviceIds(),
                request.getDeviceGroupId(),
                request.getInferenceSchedule(),
                request.getConfidenceThreshold(),
                request.getAnomalyThreshold(),
                user.getId()  // Long user ID
        );

        log.info("Created ML model '{}' (ID: {}) for organization {}",
                model.getName(), model.getId(), org.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(model));
    }

    /**
     * Update an existing ML model.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MLModelResponse> updateModel(
            @PathVariable UUID id,
            @Valid @RequestBody MLModelUpdateRequest request) {

        Organization org = securityUtils.getCurrentUserOrganization();

        return mlModelService.updateModel(
                id,
                org.getId(),
                request.getName(),
                request.getHyperparameters(),
                request.getFeatureColumns(),
                request.getTargetColumn(),
                request.getDeviceScope(),
                request.getDeviceIds(),
                request.getDeviceGroupId(),
                request.getInferenceSchedule(),
                request.getConfidenceThreshold(),
                request.getAnomalyThreshold()
        )
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete an ML model.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModel(@PathVariable UUID id) {
        Organization org = securityUtils.getCurrentUserOrganization();
        mlModelService.deleteModel(id, org.getId());
        log.info("Deleted ML model ID: {} for organization {}", id, org.getId());
    }

    /**
     * Deploy an ML model for production inference.
     */
    @PostMapping("/{id}/deploy")
    public ResponseEntity<MLModelResponse> deployModel(@PathVariable UUID id) {
        Organization org = securityUtils.getCurrentUserOrganization();

        return mlModelService.deployModel(id, org.getId())
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Archive an ML model (soft remove from active use).
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<MLModelResponse> archiveModel(@PathVariable UUID id) {
        Organization org = securityUtils.getCurrentUserOrganization();

        return mlModelService.archiveModel(id, org.getId())
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Start training for an ML model.
     * This now creates a training job and triggers training on the Python ML service.
     *
     * @param id The model ID to train
     * @return Training start response with model and job information
     */
    @PostMapping("/{id}/train")
    public ResponseEntity<?> startTraining(@PathVariable UUID id) {
        User user = securityUtils.getCurrentUser();
        Organization org = user.getOrganization();

        try {
            // Start training via the training job service (calls Python ML service)
            MLTrainingJob job = trainingJobService.startTraining(
                    id,
                    org.getId(),
                    UUID.randomUUID(), // Generate UUID for triggeredBy tracking
                    null  // Auto-detect job type (INITIAL_TRAINING or RETRAINING)
            );

            log.info("Started training job {} for model {} (user={}, org={})",
                    job.getId(), id, user.getEmail(), org.getId());

            // Return combined response with model and job info
            MLModel model = job.getModel();
            TrainingStartResponse response = new TrainingStartResponse(
                    toResponse(model),
                    trainingJobService.toResponse(job)
            );

            return ResponseEntity.accepted().body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Training start failed - not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            log.warn("Training start failed - conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));

        } catch (MLServiceException e) {
            log.error("Training start failed - ML service error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "ML service unavailable. Please try again later.",
                                 "details", e.getMessage()));
        }
    }

    /**
     * Response DTO for training start operation.
     * Contains both the model (with updated status) and the training job details.
     */
    public record TrainingStartResponse(
            MLModelResponse model,
            TrainingJobResponseDto trainingJob
    ) {}

    /**
     * Get deployed models for the organization.
     */
    @GetMapping("/deployed")
    public List<MLModelResponse> getDeployedModels() {
        Organization org = securityUtils.getCurrentUserOrganization();
        return mlModelService.getDeployedModels(org.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get model statistics.
     */
    @GetMapping("/stats")
    public ModelStatsResponse getModelStats() {
        Organization org = securityUtils.getCurrentUserOrganization();
        long deployedCount = mlModelService.countDeployedModels(org.getId());

        return new ModelStatsResponse(deployedCount);
    }

    /**
     * Convert MLModel entity to response DTO.
     */
    private MLModelResponse toResponse(MLModel model) {
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

    /**
     * Model statistics response.
     */
    public record ModelStatsResponse(long deployedCount) {}
}
