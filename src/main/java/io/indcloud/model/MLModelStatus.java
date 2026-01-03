package io.indcloud.model;

/**
 * Status lifecycle for ML models.
 */
public enum MLModelStatus {
    DRAFT,      // Model created but not trained
    TRAINING,   // Model is currently being trained
    TRAINED,    // Model trained successfully, awaiting deployment
    DEPLOYED,   // Model is active and running inference
    ARCHIVED,   // Model is no longer active
    FAILED      // Training or deployment failed
}
