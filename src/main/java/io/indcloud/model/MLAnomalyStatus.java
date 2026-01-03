package io.indcloud.model;

/**
 * Status workflow for ML-detected anomalies.
 */
public enum MLAnomalyStatus {
    NEW,            // Just detected, not yet reviewed
    ACKNOWLEDGED,   // Operator has seen the anomaly
    INVESTIGATING,  // Under investigation
    RESOLVED,       // Issue has been resolved
    FALSE_POSITIVE  // Marked as not a real anomaly
}
