package io.indcloud.model;

/**
 * Types of ML training jobs.
 */
public enum MLTrainingJobType {
    INITIAL_TRAINING,       // First training of a new model
    RETRAINING,            // Retraining with new data
    HYPERPARAMETER_TUNING  // Optimizing hyperparameters
}
