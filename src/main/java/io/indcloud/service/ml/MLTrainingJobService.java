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
 *
 * IMPORTANT: External HTTP calls are made OUTSIDE of transactions to prevent
 * connection pool starvation. The pattern is:
 * 1. Create/update DB records in transaction
 * 2. Make HTTP call (non-transactional)
 * 3. Update DB with result in new transaction
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

    /** Maximum progress percent value (for validation) */
    private static final int MAX_PROGRESS_PERCENT = 100;
    private static final int MIN_PROGRESS_PERCENT = 0;

    /**
     * Start training for a model.
     *
     * This method follows a pattern to avoid blocking I/O inside transactions:
     * 1. Create local job record in transaction (PENDING status)
     * 2. Call Python ML service OUTSIDE transaction (no DB connection held)
     * 3. Update job and model in new transaction with results
     *
     * @param modelId The model to train
     * @param organizationId Organization ID for authorization
     * @param triggeredBy User ID who triggered training (as Long, converted internally)
     * @param jobType Type of training (INITIAL_TRAINING, RETRAINING, etc.)
     * @return The created training job
     * @throws IllegalArgumentException if model not found
     * @throws IllegalStateException if model already has active training job
     * @throws MLServiceException if Python ML service call fails
     */
    public MLTrainingJob startTraining(UUID modelId, Long organizationId, Long triggeredBy, MLTrainingJobType jobType) {
        log.info("Starting training for model {} (org={}, type={}, user={})", modelId, organizationId, jobType, triggeredBy);

        // Phase 1: Create local job record (transactional)
        MLTrainingJob job = createLocalTrainingJob(modelId, organizationId, triggeredBy, jobType);

        // Phase 2: Call Python ML service (NOT transactional - no DB connection held)
        TrainingJobResponseDto response;
        try {
            TrainingJobCreateDto request = TrainingJobCreateDto.builder()
                    .modelId(modelId)
                    .organizationId(organizationId)
                    .jobType(job.getJobType().name())
                    .trainingConfig(job.getTrainingConfig())
                    .trainingDataStart(job.getTrainingDataStart())
                    .trainingDataEnd(job.getTrainingDataEnd())
                    .build();

            response = mlServiceClient.createTrainingJob(request)
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                throw new MLServiceException("Python ML service returned null response");
            }

        } catch (Exception e) {
            // HTTP call failed - mark job as FAILED in separate transaction
            log.error("Failed to start training job {} on Python ML service", job.getId(), e);
            markJobAsFailed(job.getId(), "Failed to start training on ML service: " + sanitizeErrorMessage(e.getMessage()));
            throw new MLServiceException("Failed to start training: " + sanitizeErrorMessage(e.getMessage()), e);
        }

        // Phase 3: Update job with external ID and model status (transactional)
        return updateJobWithExternalResponse(job.getId(), response, triggeredBy);
    }

    /**
     * Phase 1: Create local training job record.
     * Validates model and creates PENDING job in transaction.
     */
    @Transactional
    protected MLTrainingJob createLocalTrainingJob(UUID modelId, Long organizationId, Long triggeredBy, MLTrainingJobType jobType) {
        // Validate model exists and belongs to organization
        MLModel model = mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        // Check if model already has an active training job
        if (trainingJobRepository.existsActiveJobForModel(modelId)) {
            throw new IllegalStateException("Model already has an active training job. Wait for it to complete or cancel it first.");
        }

        // Check if model is in a trainable state
        if (model.getStatus() == MLModelStatus.TRAINING) {
            throw new IllegalStateException("Model is already in TRAINING status");
        }

        // Determine job type automatically if not specified
        MLTrainingJobType effectiveJobType = jobType;
        if (effectiveJobType == null) {
            effectiveJobType = model.getTrainedAt() == null
                    ? MLTrainingJobType.INITIAL_TRAINING
                    : MLTrainingJobType.RETRAINING;
        }

        // Build training configuration from model
        Map<String, Object> trainingConfig = buildTrainingConfig(model);

        // Create local training job record (PENDING status)
        // Store triggeredBy as Long directly for proper audit trail
        MLTrainingJob job = MLTrainingJob.builder()
                .model(model)
                .organization(organization)
                .jobType(effectiveJobType)
                .status(MLTrainingJobStatus.PENDING)
                .trainingConfig(trainingConfig)
                .progressPercent(0)
                .currentStep("Initializing")
                .triggeredByUserId(triggeredBy)  // Store actual user ID for audit
                .build();

        job = trainingJobRepository.save(job);
        log.info("Created local training job: {} for model: {}", job.getId(), modelId);

        return job;
    }

    /**
     * Phase 3: Update job with external response and set model to TRAINING.
     */
    @Transactional
    protected MLTrainingJob updateJobWithExternalResponse(UUID jobId, TrainingJobResponseDto response, Long triggeredBy) {
        MLTrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found after creation: " + jobId));

        // Update job with external ID and status from Python
        job.setExternalJobId(response.getId());
        job.setStatus(mapStatus(response.getStatus()));
        job.setProgressPercent(validateProgressPercent(response.getProgressPercent()));
        job.setCurrentStep(response.getCurrentStep());
        if (response.getStartedAt() != null) {
            job.setStartedAt(response.getStartedAt());
        }

        job = trainingJobRepository.save(job);
        log.info("Training job {} linked to external job: {}", job.getId(), response.getId());

        // Update model status to TRAINING
        MLModel model = job.getModel();
        if (model != null) {
            model.setStatus(MLModelStatus.TRAINING);
            model.setTrainedBy(triggeredBy);  // Store actual user ID
            mlModelRepository.save(model);
        }

        return job;
    }

    /**
     * Mark a job as failed in a separate transaction.
     */
    @Transactional
    protected void markJobAsFailed(UUID jobId, String errorMessage) {
        trainingJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(MLTrainingJobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(Instant.now());
            trainingJobRepository.save(job);
        });
    }

    /**
     * Mark a job as stale/failed when it has exceeded the stale threshold.
     * Called by the monitor when cancellation fails.
     * Also updates the associated model status back to DRAFT or TRAINED.
     *
     * @param jobId The job ID to mark as stale
     * @param errorMessage The error message describing why the job is stale
     */
    @Transactional
    public void markJobAsStale(UUID jobId, String errorMessage) {
        MLTrainingJob job = trainingJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Cannot mark job {} as stale - job not found", jobId);
            return;
        }

        // Mark job as FAILED
        job.setStatus(MLTrainingJobStatus.FAILED);
        job.setErrorMessage(sanitizeErrorMessage(errorMessage));
        job.setCompletedAt(Instant.now());
        if (job.getStartedAt() != null) {
            job.setDurationSeconds((int) Math.min(
                    Duration.between(job.getStartedAt(), Instant.now()).getSeconds(),
                    Integer.MAX_VALUE
            ));
        }
        trainingJobRepository.save(job);

        // Update model status back to previous state
        MLModel model = job.getModel();
        if (model != null && model.getStatus() == MLModelStatus.TRAINING) {
            model.setStatus(model.getTrainedAt() != null ? MLModelStatus.TRAINED : MLModelStatus.DRAFT);
            mlModelRepository.save(model);
            log.info("Reverted model {} status to {} after stale job cleanup", model.getId(), model.getStatus());
        }
    }

    /**
     * Synchronize job status from Python ML service.
     * Called by MLTrainingJobMonitor to poll for updates.
     *
     * This method follows the same pattern as startTraining:
     * HTTP call OUTSIDE transaction, DB updates inside transaction.
     *
     * @param job The job to synchronize
     * @return Updated job, or the original job if sync failed
     */
    public MLTrainingJob syncJobStatus(MLTrainingJob job) {
        if (job.getExternalJobId() == null) {
            log.warn("Cannot sync job {} - no external job ID", job.getId());
            return job;
        }

        // Phase 1: Make HTTP call OUTSIDE transaction
        TrainingJobResponseDto response;
        try {
            response = mlServiceClient.getTrainingJob(job.getExternalJobId())
                    .block(Duration.ofSeconds(10));

            if (response == null) {
                log.warn("Python ML service returned null for job {}", job.getExternalJobId());
                return job;
            }
        } catch (Exception e) {
            log.error("Failed to sync job {} status from ML service: {}", job.getId(), sanitizeErrorMessage(e.getMessage()));
            return job;
        }

        // Phase 2: Update local job in transaction
        return updateJobFromSyncResponse(job.getId(), job.getStatus(), response);
    }

    /**
     * Update job status from sync response in a transaction.
     */
    @Transactional
    protected MLTrainingJob updateJobFromSyncResponse(UUID jobId, MLTrainingJobStatus previousStatus, TrainingJobResponseDto response) {
        MLTrainingJob job = trainingJobRepository.findById(jobId)
                .orElse(null);

        if (job == null) {
            log.warn("Job {} not found during sync update", jobId);
            return null;
        }

        MLTrainingJobStatus newStatus = mapStatus(response.getStatus());

        job.setStatus(newStatus);
        job.setProgressPercent(validateProgressPercent(response.getProgressPercent() != null ? response.getProgressPercent() : job.getProgressPercent()));
        job.setCurrentStep(response.getCurrentStep());
        job.setRecordCount(response.getRecordCount());
        job.setDeviceCount(response.getDeviceCount());
        job.setResultMetrics(response.getResultMetrics());

        if (response.getStartedAt() != null && job.getStartedAt() == null) {
            job.setStartedAt(response.getStartedAt());
        }

        if (response.getErrorMessage() != null) {
            // Sanitize error message before storing
            job.setErrorMessage(sanitizeErrorMessage(response.getErrorMessage()));
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
    }

    /**
     * Cancel a training job.
     *
     * This method follows the 3-phase pattern to avoid blocking I/O inside transactions:
     * 1. Validate job and check permissions in transaction
     * 2. Call Python ML service OUTSIDE transaction
     * 3. Update local job status in new transaction
     *
     * @param jobId The job ID to cancel
     * @param organizationId Organization ID for authorization
     * @return The cancelled job
     * @throws IllegalArgumentException if job not found
     * @throws IllegalStateException if job cannot be cancelled
     */
    public MLTrainingJob cancelJob(UUID jobId, Long organizationId) {
        log.info("Cancelling training job: {}", jobId);

        // Phase 1: Validate job in transaction
        CancelJobValidation validation = validateJobForCancellation(jobId, organizationId);

        // Phase 2: Call Python ML service OUTSIDE transaction (no DB connection held)
        if (validation.externalJobId() != null) {
            try {
                mlServiceClient.cancelTrainingJob(validation.externalJobId())
                        .block(Duration.ofSeconds(10));
                log.info("Cancelled external job: {}", validation.externalJobId());
            } catch (Exception e) {
                log.warn("Failed to cancel external job {}: {} (continuing with local cancellation)",
                        validation.externalJobId(), sanitizeErrorMessage(e.getMessage()));
            }
        }

        // Phase 3: Update local job status in new transaction
        return completeCancellation(jobId);
    }

    /**
     * Phase 1: Validate job can be cancelled.
     */
    @Transactional(readOnly = true)
    protected CancelJobValidation validateJobForCancellation(UUID jobId, Long organizationId) {
        MLTrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));

        // Verify organization ownership
        Organization org = job.getOrganization();
        if (org == null || !org.getId().equals(organizationId)) {
            throw new IllegalArgumentException("Training job not found: " + jobId);
        }

        // Check if job can be cancelled
        if (isTerminalStatus(job.getStatus())) {
            throw new IllegalStateException("Cannot cancel job with status: " + job.getStatus());
        }

        return new CancelJobValidation(job.getExternalJobId());
    }

    /**
     * Phase 3: Complete cancellation by updating local job status.
     */
    @Transactional
    protected MLTrainingJob completeCancellation(UUID jobId) {
        MLTrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found after validation: " + jobId));

        // Update local job status
        job.setStatus(MLTrainingJobStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        if (job.getStartedAt() != null) {
            long seconds = Duration.between(job.getStartedAt(), Instant.now()).getSeconds();
            job.setDurationSeconds((int) Math.min(seconds, Integer.MAX_VALUE));
        }
        job = trainingJobRepository.save(job);

        // Update model status back to previous state
        MLModel model = job.getModel();
        if (model != null && model.getStatus() == MLModelStatus.TRAINING) {
            model.setStatus(model.getTrainedAt() != null ? MLModelStatus.TRAINED : MLModelStatus.DRAFT);
            mlModelRepository.save(model);
            log.info("Reverted model {} status to {}", model.getId(), model.getStatus());
        }

        return job;
    }

    /**
     * Record for cancel job validation result.
     */
    private record CancelJobValidation(UUID externalJobId) {}

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
     * Includes authorization check to ensure model belongs to the organization.
     *
     * @param modelId The model ID
     * @param organizationId Organization ID for authorization
     * @param pageable Pagination parameters
     * @return Page of training jobs for the model
     * @throws IllegalArgumentException if model not found or doesn't belong to organization
     */
    @Transactional(readOnly = true)
    public Page<MLTrainingJob> getJobsForModel(UUID modelId, Long organizationId, Pageable pageable) {
        // Verify model belongs to organization (authorization check)
        mlModelRepository.findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

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
     * Get the latest job for a model (internal use, no authorization check).
     */
    @Transactional(readOnly = true)
    public Optional<MLTrainingJob> getLatestJobForModel(UUID modelId) {
        return trainingJobRepository.findFirstByModelIdOrderByCreatedAtDesc(modelId);
    }

    /**
     * Get the latest job for a model with authorization check.
     * Verifies that the model belongs to the specified organization.
     *
     * @param modelId The model ID
     * @param organizationId Organization ID for authorization
     * @return The latest training job, or empty if not found or not authorized
     */
    @Transactional(readOnly = true)
    public Optional<MLTrainingJob> getLatestJobForModel(UUID modelId, Long organizationId) {
        // First verify the model belongs to the organization
        if (!mlModelRepository.existsByIdAndOrganizationId(modelId, organizationId)) {
            return Optional.empty();
        }
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
     * Includes comprehensive null checks to prevent NPE.
     */
    private void handleJobCompletion(MLTrainingJob job, TrainingJobResponseDto response) {
        job.setCompletedAt(response.getCompletedAt() != null ? response.getCompletedAt() : Instant.now());
        job.setDurationSeconds(response.getDurationSeconds());
        job.setResultMetrics(response.getResultMetrics());

        // Update model status based on job result
        // Full null-safety: check model AND organization
        MLModel model = job.getModel();
        if (model == null) {
            log.warn("Job {} completed but has no associated model", job.getId());
            return;
        }

        Organization org = model.getOrganization();
        if (org == null) {
            log.warn("Job {} completed but model {} has no organization", job.getId(), model.getId());
            return;
        }

        UUID modelId = model.getId();
        Long orgId = org.getId();

        if (modelId == null || orgId == null) {
            log.warn("Job {} completed but model or organization ID is null", job.getId());
            return;
        }

        if (job.getStatus() == MLTrainingJobStatus.COMPLETED) {
            // Training succeeded - update model to TRAINED
            try {
                mlModelService.completeTraining(
                        modelId,
                        orgId,
                        response.getResultMetrics(),
                        null, // validationMetrics - could be extracted from resultMetrics
                        buildModelPath(modelId),
                        null  // modelSizeBytes - not tracked yet
                );
                log.info("Model {} training completed successfully", modelId);
            } catch (Exception e) {
                log.error("Failed to update model {} after training completion: {}", modelId, e.getMessage());
            }

        } else if (job.getStatus() == MLTrainingJobStatus.FAILED) {
            // Training failed - revert model to DRAFT or FAILED
            try {
                mlModelService.failTraining(modelId, orgId);
                log.warn("Model {} training failed: {}", modelId, sanitizeErrorMessage(job.getErrorMessage()));
            } catch (Exception e) {
                log.error("Failed to update model {} after training failure: {}", modelId, e.getMessage());
            }
        }
        // CANCELLED is handled in cancelJob method
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
     * Validate progress percent is within valid range (0-100).
     * Returns 0 if null, clamps to valid range otherwise.
     */
    private int validateProgressPercent(Integer progress) {
        if (progress == null) {
            return MIN_PROGRESS_PERCENT;
        }
        return Math.max(MIN_PROGRESS_PERCENT, Math.min(MAX_PROGRESS_PERCENT, progress));
    }

    /**
     * Sanitize error messages before logging or storing.
     * Removes potentially sensitive information like stack traces, file paths, and credentials.
     */
    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return null;
        }

        // Truncate very long messages
        String sanitized = message.length() > 500 ? message.substring(0, 500) + "..." : message;

        // Remove common patterns that might leak sensitive information
        // Remove file paths (both Unix and Windows style)
        sanitized = sanitized.replaceAll("[A-Za-z]:\\\\[^\\s]+", "[path]");
        sanitized = sanitized.replaceAll("/[a-zA-Z0-9_/-]+\\.[a-zA-Z]+", "[path]");

        // Remove potential connection strings
        sanitized = sanitized.replaceAll("(?i)(password|pwd|secret|key|token)\\s*[=:]\\s*[^\\s,;]+", "$1=[REDACTED]");

        // Remove IP addresses and ports that might be internal
        sanitized = sanitized.replaceAll("\\b(?:10|172\\.(?:1[6-9]|2\\d|3[01])|192\\.168)\\.[\\d.]+(?::\\d+)?\\b", "[internal-address]");

        return sanitized;
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
