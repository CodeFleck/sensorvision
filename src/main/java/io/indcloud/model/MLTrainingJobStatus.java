package io.indcloud.model;

/**
 * Status lifecycle for ML training jobs.
 */
public enum MLTrainingJobStatus {
    PENDING,    // Job created, waiting to start
    RUNNING,    // Training in progress
    COMPLETED,  // Training finished successfully
    FAILED,     // Training failed with error
    CANCELLED   // Job was cancelled
}
