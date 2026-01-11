package io.indcloud.service.ml;

import io.indcloud.model.MLTrainingJob;
import io.indcloud.model.MLTrainingJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background service that monitors active training jobs and synchronizes their status
 * from the Python ML service.
 *
 * Features:
 * - Polls Python ML service every 10 seconds for active job updates
 * - Handles stale jobs (running too long without updates)
 * - Thread-safe with single execution guarantee
 * - Graceful error handling (doesn't fail on individual job errors)
 *
 * Enable/disable via: ml.training.monitor.enabled=true/false
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ml.training.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class MLTrainingJobMonitor {

    private final MLTrainingJobService trainingJobService;

    /**
     * Maximum time a job can be in RUNNING state before being marked as stale.
     * After this duration, we'll mark it as FAILED if no progress updates.
     */
    @Value("${ml.training.stale-threshold-hours:24}")
    private int staleThresholdHours;

    /**
     * Guard to prevent concurrent execution of the polling task.
     */
    private final AtomicBoolean isPolling = new AtomicBoolean(false);

    /**
     * Poll active training jobs every 10 seconds.
     * This method is thread-safe and will skip execution if a previous poll is still running.
     */
    @Scheduled(fixedRateString = "${ml.training.monitor.poll-rate-ms:10000}")
    public void pollActiveJobs() {
        // Ensure only one poll runs at a time
        if (!isPolling.compareAndSet(false, true)) {
            log.debug("Skipping poll - previous poll still running");
            return;
        }

        try {
            List<MLTrainingJob> activeJobs = trainingJobService.getActiveJobs();

            if (activeJobs.isEmpty()) {
                log.trace("No active training jobs to poll");
                return;
            }

            log.debug("Polling {} active training jobs", activeJobs.size());

            for (MLTrainingJob job : activeJobs) {
                try {
                    pollSingleJob(job);
                } catch (Exception e) {
                    log.error("Error polling job {}: {}", job.getId(), e.getMessage());
                    // Continue with other jobs
                }
            }

        } finally {
            isPolling.set(false);
        }
    }

    /**
     * Poll a single job and update its status.
     */
    private void pollSingleJob(MLTrainingJob job) {
        // Check for stale jobs
        if (isStaleJob(job)) {
            log.warn("Job {} is stale (started at {}, no progress for {} hours). Marking as FAILED.",
                    job.getId(), job.getStartedAt(), staleThresholdHours);
            handleStaleJob(job);
            return;
        }

        // Sync status from Python ML service
        MLTrainingJob updatedJob = trainingJobService.syncJobStatus(job);

        // Log significant status changes
        if (updatedJob != null && updatedJob.getStatus() != job.getStatus()) {
            logStatusChange(job, updatedJob);
        }
    }

    /**
     * Check if a job is stale (running too long without updates).
     */
    private boolean isStaleJob(MLTrainingJob job) {
        if (job.getStatus() != MLTrainingJobStatus.RUNNING) {
            return false;
        }

        Instant startTime = job.getStartedAt();
        if (startTime == null) {
            startTime = job.getCreatedAt();
        }

        Duration elapsed = Duration.between(startTime, Instant.now());
        return elapsed.toHours() >= staleThresholdHours;
    }

    /**
     * Handle a stale job by attempting to cancel it, then marking as FAILED if cancellation fails.
     * This ensures stale jobs don't remain in RUNNING state forever.
     */
    private void handleStaleJob(MLTrainingJob job) {
        try {
            // Try to cancel the job gracefully first
            trainingJobService.cancelJob(job.getId(), job.getOrganization().getId());
            log.info("Stale job {} has been cancelled", job.getId());
        } catch (Exception e) {
            log.error("Failed to cancel stale job {}: {}", job.getId(), e.getMessage());

            // If cancellation fails, force-mark the job as FAILED to prevent infinite retries
            try {
                trainingJobService.markJobAsStale(job.getId(),
                        "Job exceeded stale threshold of " + staleThresholdHours + " hours and could not be cancelled: " + e.getMessage());
                log.warn("Stale job {} has been force-marked as FAILED", job.getId());
            } catch (Exception markException) {
                log.error("Failed to mark stale job {} as FAILED: {}", job.getId(), markException.getMessage());
            }
        }
    }

    /**
     * Log status changes for monitoring purposes.
     */
    private void logStatusChange(MLTrainingJob oldJob, MLTrainingJob newJob) {
        switch (newJob.getStatus()) {
            case COMPLETED -> log.info("Training job {} COMPLETED in {}s (model: {})",
                    newJob.getId(),
                    newJob.getDurationSeconds(),
                    newJob.getModel() != null ? newJob.getModel().getId() : "unknown");

            case FAILED -> log.error("Training job {} FAILED: {} (model: {})",
                    newJob.getId(),
                    newJob.getErrorMessage(),
                    newJob.getModel() != null ? newJob.getModel().getId() : "unknown");

            case CANCELLED -> log.info("Training job {} CANCELLED (model: {})",
                    newJob.getId(),
                    newJob.getModel() != null ? newJob.getModel().getId() : "unknown");

            case RUNNING -> log.info("Training job {} started RUNNING (model: {})",
                    newJob.getId(),
                    newJob.getModel() != null ? newJob.getModel().getId() : "unknown");

            default -> log.debug("Training job {} status changed to {} (model: {})",
                    newJob.getId(),
                    newJob.getStatus(),
                    newJob.getModel() != null ? newJob.getModel().getId() : "unknown");
        }
    }

    /**
     * Manually trigger a poll (useful for testing or immediate refresh).
     */
    public void triggerPoll() {
        pollActiveJobs();
    }

    /**
     * Check if a poll is currently running.
     */
    public boolean isPollingInProgress() {
        return isPolling.get();
    }
}
