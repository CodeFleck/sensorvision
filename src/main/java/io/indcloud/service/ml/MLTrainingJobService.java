package io.indcloud.service.ml;

import io.indcloud.dto.ml.TrainingJobCreateDto;
import io.indcloud.dto.ml.TrainingJobResponseDto;
import io.indcloud.model.*;
import io.indcloud.repository.MLModelRepository;
import io.indcloud.repository.MLTrainingJobRepository;
import io.indcloud.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service for managing ML training jobs and synchronizing with Python ML service.
 *
 * This service handles:
 * - Creating training jobs (local DB + Python ML service)
 * - Synchronizing job status from Python ML service
 * - Cancelling jobs
 * - Querying job history
 *
 * Architecture:
 * Spring Boot DB (ml_training_jobs) <-> MLTrainingJobService <-> Python ML Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MLTrainingJobService {

    private final MLTrainingJobRepository trainingJobRepository;
    private final MLModelRepository mlModelRepository;
    private final OrganizationRepository organizationRepository;
    private final MLServiceClient mlServiceClient;
    private final MLModelService mlModelService;

    /**
     * Start training for a model.
     *
     * This method:
     * 1. Validates the model exists and is trainable
     * 2. Creates a local training job record
     * 3. Calls Python ML service to start training
     * 4. Updates local record with external job ID
     * 5. Updates model status to TRAINING
     *
     * @param modelId The model to train
     * @param organizationId Organization ID for authorization
     * @param triggeredBy User ID who triggered training
     * @param jobType Type of training (INITIAL_TRAINING, RETRAINING, etc.)
     * @return The created training job
     * @throws IllegalArgumentException if model not found
     * @throws IllegalStateException if model already has active training job
     * @throws MLServiceException if Python ML service call fails
     */
    @Transactional
    public MLTrainingJob startTraining(UUID modelId, Long organizationId, UUID triggeredBy, MLTrainingJobType jobType) {
        log.info("Starting training for model {} (org={}, type={})", modelId, organizationId, jobType);

        // 1. Validate model exists and belongs to organization
        MLModel model = mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        // 2. Check if model already has an active training job
        if (trainingJobRepository.existsActiveJobForModel(modelId)) {
            throw new IllegalStateException("Model already has an active training job. Wait for it to complete or cancel it first.");
        }

        // 3. Check if model is in a trainable state
        if (model.getStatus() == MLModelStatus.TRAINING) {
            throw new IllegalStateException("Model is already in TRAINING status");
        }

        // 4. Determine job type automatically if not specified
        MLTrainingJobType effectiveJobType = jobType;
        if (effectiveJobType == null) {
            effectiveJobType = model.getTrainedAt() == null
                    ? MLTrainingJobType.INITIAL_TRAINING
                    : MLTrainingJobType.RETRAINING;
        }

        // 5. Build training configuration from model
        Map<String, Object> trainingConfig = buildTrainingConfig(model);

        // 6. Create local training job record (PENDING status)
        MLTrainingJob job = MLTrainingJob.builder()
                .model(model)
                .organization(organization)
                .jobType(effectiveJobType)
                .status(MLTrainingJobStatus.PENDING)
                .trainingConfig(trainingConfig)
                .progressPercent(0)
                .currentStep("Initializing")
                .triggeredBy(triggeredBy)
                .build();

        job = trainingJobRepository.save(job);
        log.info("Created local training job: {} for model: {}", job.getId(), modelId);

        // 7. Call Python ML service to create training job
        try {
            TrainingJobCreateDto request = TrainingJobCreateDto.builder()
                    .modelId(modelId)
                    .organizationId(organizationId)
                    .jobType(effectiveJobType.name())
                    .trainingConfig(trainingConfig)
                    .trainingDataStart(job.getTrainingDataStart())
                    .trainingDataEnd(job.getTrainingDataEnd())
                    .build();

            TrainingJobResponseDto response = mlServiceClient.createTrainingJob(request)
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                throw new MLServiceException("Python ML service returned null response");
            }

            // 8. Update local job with external ID and status from Python
            job.setExternalJobId(response.getId());
            job.setStatus(mapStatus(response.getStatus()));
            job.setProgressPercent(response.getProgressPercent() != null ? response.getProgressPercent() : 0);
            job.setCurrentStep(response.getCurrentStep());
            if (response.getStartedAt() != null) {
                job.setStartedAt(response.getStartedAt());
            }

            job = trainingJobRepository.save(job);
            log.info("Training job {} linked to external job: {}", job.getId(), response.getId());

            // 9. Update model status to TRAINING
            model.setStatus(MLModelStatus.TRAINING);
            model.setTrainedBy(triggeredBy != null ? triggeredBy.getMostSignificantBits() : null);
            mlModelRepository.save(model);

            return job;

        } catch (Exception e) {
            // Training request failed - mark job as FAILED
            log.error("Failed to start training job {} on Python ML service: {}", job.getId(), e.getMessage(), e);

            job.setStatus(MLTrainingJobStatus.FAILED);
            job.setErrorMessage("Failed to start training on ML service: " + e.getMessage());
            job.setCompletedAt(Instant.now());
            trainingJobRepository.save(job);

            // Keep model in its previous state (don't update to TRAINING since it didn't start)
            throw new MLServiceException("Failed to start training: " + e.getMessage(), e);
        }
    }

    /**
     * Synchronize job status from Python ML service.
     * Called by MLTrainingJobMonitor to poll for updates.
     *
     * @param job The job to synchronize
     * @return Updated job, or null if sync failed
     */
    @Transactional
    public MLTrainingJob syncJobStatus(MLTrainingJob job) {
        if (job.getExternalJobId() == null) {
            log.warn("Cannot sync job {} - no external job ID", job.getId());
            return job;
        }

        try {
            TrainingJobResponseDto response = mlServiceClient.getTrainingJob(job.getExternalJobId())
                    .block(Duration.ofSeconds(10));

            if (response == null) {
                log.warn("Python ML service returned null for job {}", job.getExternalJobId());
                return job;
            }

            // Update local job with latest status from Python
            MLTrainingJobStatus previousStatus = job.getStatus();
            MLTrainingJobStatus newStatus = mapStatus(response.getStatus());

            job.setStatus(newStatus);
            job.setProgressPercent(response.getProgressPercent() != null ? response.getProgressPercent() : job.getProgressPercent());
            job.setCurrentStep(response.getCurrentStep());
            job.setRecordCount(response.getRecordCount());
            job.setDeviceCount(response.getDeviceCount());
            job.setResultMetrics(response.getResultMetrics());

            if (response.getStartedAt() != null && job.getStartedAt() == null) {
                job.setStartedAt(response.getStartedAt());
            }

            if (response.getErrorMessage() != null) {
                job.setErrorMessage(response.getErrorMessage());
            }

            // Handle terminal states
            if (isTerminalStatus(newStatus) && !isTerminalStatus(previousStatus)) {
                handleJobCompletion(job, response);
            }

            job = trainingJobRepository.save(job);

            if (newStatus != previousStatus) {
                log.info("Job {} status changed: {} -> {} (progress: {}%)",
                        job.getId(), previousStatus, newStatus, job.getProgressPercent());
            }

            return job;

        } catch (Exception e) {
            log.error("Failed to sync job {} status: {}", job.getId(), e.getMessage());
            return job;
        }
    }

    /**
     * Cancel a training job.
     *
     * @param jobId The job ID to cancel
     * @param organizationId Organization ID for authorization
     * @return The cancelled job
     * @throws IllegalArgumentException if job not found
     * @throws IllegalStateException if job cannot be cancelled
     */
    @Transactional
    public MLTrainingJob cancelJob(UUID jobId, Long organizationId) {
        log.info("Cancelling training job: {}", jobId);

        MLTrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));

        // Verify organization ownership
        if (!job.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Training job not found: " + jobId);
        }

        // Check if job can be cancelled
        if (isTerminalStatus(job.getStatus())) {
            throw new IllegalStateException("Cannot cancel job with status: " + job.getStatus());
        }

        // Call Python ML service to cancel if external ID exists
        if (job.getExternalJobId() != null) {
            try {
                mlServiceClient.cancelTrainingJob(job.getExternalJobId())
                        .block(Duration.ofSeconds(10));
                log.info("Cancelled external job: {}", job.getExternalJobId());
            } catch (Exception e) {
                log.warn("Failed to cancel external job {}: {} (continuing with local cancellation)",
                        job.getExternalJobId(), e.getMessage());
            }
        }

        // Update local job status
        job.setStatus(MLTrainingJobStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        if (job.getStartedAt() != null) {
            job.setDurationSeconds((int) Duration.between(job.getStartedAt(), Instant.now()).getSeconds());
        }
        job = trainingJobRepository.save(job);

        // Update model status back to previous state
        if (job.getModel() != null) {
            MLModel model = job.getModel();
            // If model was TRAINING, revert to DRAFT or TRAINED based on whether it was ever trained
            if (model.getStatus() == MLModelStatus.TRAINING) {
                model.setStatus(model.getTrainedAt() != null ? MLModelStatus.TRAINED : MLModelStatus.DRAFT);
                mlModelRepository.save(model);
                log.info("Reverted model {} status to {}", model.getId(), model.getStatus());
            }
        }

        return job;
    }

    /**
     * Get a training job by ID.
     */
    @Transactional(readOnly = true)
    public Optional<MLTrainingJob> getJob(UUID jobId, Long organizationId) {
        return trainingJobRepository.findById(jobId)
                .filter(job -> job.getOrganization().getId().equals(organizationId));
    }

    /**
     * Get all jobs for a model.
     */
    @Transactional(readOnly = true)
    public Page<MLTrainingJob> getJobsForModel(UUID modelId, Pageable pageable) {
        return trainingJobRepository.findByModelId(modelId, pageable);
    }

    /**
     * Get all jobs for an organization.
     */
    @Transactional(readOnly = true)
    public Page<MLTrainingJob> getJobsForOrganization(Long organizationId, Pageable pageable) {
        return trainingJobRepository.findByOrganizationId(organizationId, pageable);
    }

    /**
     * Get the latest job for a model.
     */
    @Transactional(readOnly = true)
    public Optional<MLTrainingJob> getLatestJobForModel(UUID modelId) {
        return trainingJobRepository.findFirstByModelIdOrderByCreatedAtDesc(modelId);
    }

    /**
     * Get all active jobs (for the monitor to poll).
     */
    @Transactional(readOnly = true)
    public List<MLTrainingJob> getActiveJobs() {
        return trainingJobRepository.findActiveJobs();
    }

    /**
     * Convert job entity to response DTO.
     */
    public TrainingJobResponseDto toResponse(MLTrainingJob job) {
        return TrainingJobResponseDto.builder()
                .id(job.getId())
                .modelId(job.getModel() != null ? job.getModel().getId() : null)
                .organizationId(job.getOrganization().getId())
                .jobType(job.getJobType().name())
                .status(job.getStatus().name())
                .trainingConfig(job.getTrainingConfig())
                .trainingDataStart(job.getTrainingDataStart())
                .trainingDataEnd(job.getTrainingDataEnd())
                .recordCount(job.getRecordCount())
                .deviceCount(job.getDeviceCount())
                .progressPercent(job.getProgressPercent())
                .currentStep(job.getCurrentStep())
                .resultMetrics(job.getResultMetrics())
                .errorMessage(job.getErrorMessage())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .durationSeconds(job.getDurationSeconds())
                .triggeredBy(job.getTriggeredBy())
                .createdAt(job.getCreatedAt())
                .build();
    }

    // ========== Private Helper Methods ==========

    /**
     * Build training configuration from model settings.
     */
    private Map<String, Object> buildTrainingConfig(MLModel model) {
        Map<String, Object> config = new HashMap<>();

        // Required: model type for Python ML service
        config.put("model_type", model.getModelType().name());
        config.put("algorithm", model.getAlgorithm());

        // Hyperparameters
        if (model.getHyperparameters() != null && !model.getHyperparameters().isEmpty()) {
            config.putAll(model.getHyperparameters());
        }

        // Feature configuration
        if (model.getFeatureColumns() != null && !model.getFeatureColumns().isEmpty()) {
            config.put("feature_columns", model.getFeatureColumns());
        }
        if (model.getTargetColumn() != null) {
            config.put("target_column", model.getTargetColumn());
        }

        // Thresholds
        if (model.getConfidenceThreshold() != null) {
            config.put("confidence_threshold", model.getConfidenceThreshold().doubleValue());
        }
        if (model.getAnomalyThreshold() != null) {
            config.put("anomaly_threshold", model.getAnomalyThreshold().doubleValue());
        }

        // Device scope
        config.put("device_scope", model.getDeviceScope());
        if (model.getDeviceIds() != null && !model.getDeviceIds().isEmpty()) {
            config.put("device_ids", model.getDeviceIds());
        }
        if (model.getDeviceGroupId() != null) {
            config.put("device_group_id", model.getDeviceGroupId());
        }

        return config;
    }

    /**
     * Map Python ML service status string to local enum.
     */
    private MLTrainingJobStatus mapStatus(String statusString) {
        if (statusString == null) {
            return MLTrainingJobStatus.PENDING;
        }
        try {
            return MLTrainingJobStatus.valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown training job status: {}, defaulting to PENDING", statusString);
            return MLTrainingJobStatus.PENDING;
        }
    }

    /**
     * Check if status is terminal (job is finished).
     */
    private boolean isTerminalStatus(MLTrainingJobStatus status) {
        return status == MLTrainingJobStatus.COMPLETED
                || status == MLTrainingJobStatus.FAILED
                || status == MLTrainingJobStatus.CANCELLED;
    }

    /**
     * Handle job completion - update timestamps and model status.
     */
    private void handleJobCompletion(MLTrainingJob job, TrainingJobResponseDto response) {
        job.setCompletedAt(response.getCompletedAt() != null ? response.getCompletedAt() : Instant.now());
        job.setDurationSeconds(response.getDurationSeconds());
        job.setResultMetrics(response.getResultMetrics());

        // Update model status based on job result
        if (job.getModel() != null) {
            MLModel model = job.getModel();

            if (job.getStatus() == MLTrainingJobStatus.COMPLETED) {
                // Training succeeded - update model to TRAINED
                mlModelService.completeTraining(
                        model.getId(),
                        model.getOrganization().getId(),
                        response.getResultMetrics(),
                        null, // validationMetrics - could be extracted from resultMetrics
                        buildModelPath(model.getId()),
                        null  // modelSizeBytes - not tracked yet
                );
                log.info("Model {} training completed successfully", model.getId());

            } else if (job.getStatus() == MLTrainingJobStatus.FAILED) {
                // Training failed - revert model to DRAFT or FAILED
                mlModelService.failTraining(model.getId(), model.getOrganization().getId());
                log.warn("Model {} training failed: {}", model.getId(), job.getErrorMessage());
            }
            // CANCELLED is handled in cancelJob method
        }
    }

    /**
     * Build model storage path.
     */
    private String buildModelPath(UUID modelId) {
        // Models are stored by the Python ML service
        // This path is relative to the ML service's model storage directory
        return "models/" + modelId + ".joblib";
    }

    /**
     * Custom exception for ML service communication errors.
     */
    public static class MLServiceException extends RuntimeException {
        public MLServiceException(String message) {
            super(message);
        }

        public MLServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
