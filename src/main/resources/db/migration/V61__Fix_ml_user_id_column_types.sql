-- Fix ML tables user ID columns from UUID to BIGINT to match users.id type

-- ml_models table
ALTER TABLE ml_models
    ALTER COLUMN created_by TYPE BIGINT USING NULL,
    ALTER COLUMN trained_by TYPE BIGINT USING NULL;

-- ml_predictions table
ALTER TABLE ml_predictions
    ALTER COLUMN feedback_by TYPE BIGINT USING NULL;

-- ml_anomalies table
ALTER TABLE ml_anomalies
    ALTER COLUMN acknowledged_by TYPE BIGINT USING NULL,
    ALTER COLUMN resolved_by TYPE BIGINT USING NULL;

-- ml_training_jobs table
ALTER TABLE ml_training_jobs
    ALTER COLUMN triggered_by TYPE BIGINT USING NULL;

-- Add foreign key constraints to users table
ALTER TABLE ml_models
    ADD CONSTRAINT fk_ml_models_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_ml_models_trained_by FOREIGN KEY (trained_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE ml_predictions
    ADD CONSTRAINT fk_ml_predictions_feedback_by FOREIGN KEY (feedback_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE ml_anomalies
    ADD CONSTRAINT fk_ml_anomalies_acknowledged_by FOREIGN KEY (acknowledged_by) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_ml_anomalies_resolved_by FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE ml_training_jobs
    ADD CONSTRAINT fk_ml_training_jobs_triggered_by FOREIGN KEY (triggered_by) REFERENCES users(id) ON DELETE SET NULL;

COMMENT ON COLUMN ml_models.created_by IS 'User ID who created the model';
COMMENT ON COLUMN ml_models.trained_by IS 'User ID who initiated training';
COMMENT ON COLUMN ml_anomalies.acknowledged_by IS 'User ID who acknowledged the anomaly';
COMMENT ON COLUMN ml_anomalies.resolved_by IS 'User ID who resolved the anomaly';
COMMENT ON COLUMN ml_predictions.feedback_by IS 'User ID who provided feedback';
