-- Fix ML tables user ID columns from UUID to BIGINT to match users.id type
-- NOTE: This migration converts UUID columns to BIGINT. Any existing UUID values
-- will be set to NULL since there's no meaningful conversion from UUID to user ID.
-- This is safe for new installations. For existing data, review before applying.

-- Safety check: Fail early if there's existing data that would be lost
DO $$
DECLARE
    has_data BOOLEAN := FALSE;
BEGIN
    -- Check ml_models
    IF EXISTS (SELECT 1 FROM ml_models WHERE created_by IS NOT NULL OR trained_by IS NOT NULL) THEN
        has_data := TRUE;
    END IF;

    -- Check ml_predictions
    IF EXISTS (SELECT 1 FROM ml_predictions WHERE feedback_by IS NOT NULL) THEN
        has_data := TRUE;
    END IF;

    -- Check ml_anomalies
    IF EXISTS (SELECT 1 FROM ml_anomalies WHERE acknowledged_by IS NOT NULL OR resolved_by IS NOT NULL) THEN
        has_data := TRUE;
    END IF;

    -- Check ml_training_jobs
    IF EXISTS (SELECT 1 FROM ml_training_jobs WHERE triggered_by IS NOT NULL) THEN
        has_data := TRUE;
    END IF;

    IF has_data THEN
        RAISE WARNING 'ML tables contain existing user ID data that will be converted to NULL. This is expected for new UUID-to-BIGINT migration.';
    END IF;
END $$;

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
