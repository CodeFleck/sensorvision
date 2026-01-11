package io.indcloud.controller;

import io.indcloud.dto.ml.TrainingJobResponseDto;
import io.indcloud.model.MLTrainingJob;
import io.indcloud.model.MLTrainingJobType;
import io.indcloud.model.Organization;
import io.indcloud.model.User;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.ml.MLTrainingJobService;
import io.indcloud.service.ml.MLTrainingJobService.MLServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for ML training job management.
 * Provides endpoints for creating, monitoring, and managing training jobs.
 */
@RestController
@RequestMapping("/api/v1/ml/training-jobs")
@RequiredArgsConstructor
@Slf4j
public class MLTrainingJobController {

    private final MLTrainingJobService trainingJobService;
    private final SecurityUtils securityUtils;

    /**
     * Start training for an ML model.
     * Creates a new training job and triggers training on the Python ML service.
     *
     * @param modelId The model ID to train
     * @param request Optional request body with training options
     * @return The created training job
     */
    @PostMapping("/start/{modelId}")
    public ResponseEntity<?> startTraining(
            @PathVariable UUID modelId,
            @RequestBody(required = false) TrainingStartRequest request) {

        User user = securityUtils.getCurrentUser();
        Organization org = user.getOrganization();

        try {
            MLTrainingJobType jobType = null;
            if (request != null && request.jobType() != null) {
                try {
                    jobType = MLTrainingJobType.valueOf(request.jobType().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid job type: " + request.jobType()));
                }
            }

            MLTrainingJob job = trainingJobService.startTraining(
                    modelId,
                    org.getId(),
                    user.getId(), // Pass actual user ID for audit trail
                    jobType
            );

            log.info("Started training job {} for model {} (org={})",
                    job.getId(), modelId, org.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(trainingJobService.toResponse(job));

        } catch (IllegalArgumentException e) {
            log.warn("Training start failed - bad request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("Training start failed - conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));

        } catch (MLServiceException e) {
            log.error("Training start failed - ML service error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "ML service unavailable: " + e.getMessage()));
        }
    }

    /**
     * Get a specific training job by ID.
     *
     * @param jobId The job ID
     * @return The training job details
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<TrainingJobResponseDto> getJob(@PathVariable UUID jobId) {
        Organization org = securityUtils.getCurrentUserOrganization();

        return trainingJobService.getJob(jobId, org.getId())
                .map(trainingJobService::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List all training jobs for the current organization.
     *
     * @param pageable Pagination parameters
     * @return Page of training jobs
     */
    @GetMapping
    public Page<TrainingJobResponseDto> listJobs(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Organization org = securityUtils.getCurrentUserOrganization();

        return trainingJobService.getJobsForOrganization(org.getId(), pageable)
                .map(trainingJobService::toResponse);
    }

    /**
     * List training jobs for a specific model.
     * Includes authorization check to verify model belongs to user's organization.
     *
     * @param modelId The model ID
     * @param pageable Pagination parameters
     * @return Page of training jobs for the model
     */
    @GetMapping("/model/{modelId}")
    public ResponseEntity<Page<TrainingJobResponseDto>> getJobsForModel(
            @PathVariable UUID modelId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Organization org = securityUtils.getCurrentUserOrganization();

        try {
            Page<TrainingJobResponseDto> jobs = trainingJobService.getJobsForModel(modelId, org.getId(), pageable)
                    .map(trainingJobService::toResponse);
            return ResponseEntity.ok(jobs);
        } catch (IllegalArgumentException e) {
            // Model not found or doesn't belong to organization
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get the latest training job for a model.
     *
     * @param modelId The model ID
     * @return The latest training job or 404 if none exist
     */
    @GetMapping("/model/{modelId}/latest")
    public ResponseEntity<TrainingJobResponseDto> getLatestJobForModel(@PathVariable UUID modelId) {
        return trainingJobService.getLatestJobForModel(modelId)
                .map(trainingJobService::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancel a training job.
     *
     * @param jobId The job ID to cancel
     * @return The cancelled job
     */
    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<?> cancelJob(@PathVariable UUID jobId) {
        Organization org = securityUtils.getCurrentUserOrganization();

        try {
            MLTrainingJob job = trainingJobService.cancelJob(jobId, org.getId());
            log.info("Cancelled training job {} (org={})", jobId, org.getId());

            return ResponseEntity.ok(trainingJobService.toResponse(job));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Refresh job status from Python ML service.
     * Useful for immediate status update without waiting for the monitor.
     *
     * @param jobId The job ID to refresh
     * @return The updated job status
     */
    @PostMapping("/{jobId}/refresh")
    public ResponseEntity<TrainingJobResponseDto> refreshJobStatus(@PathVariable UUID jobId) {
        Organization org = securityUtils.getCurrentUserOrganization();

        return trainingJobService.getJob(jobId, org.getId())
                .map(job -> {
                    MLTrainingJob refreshed = trainingJobService.syncJobStatus(job);
                    return ResponseEntity.ok(trainingJobService.toResponse(refreshed));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Request body for starting training with optional parameters.
     */
    public record TrainingStartRequest(
            String jobType  // INITIAL_TRAINING, RETRAINING, HYPERPARAMETER_TUNING
    ) {}
}
