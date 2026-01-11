-- Add unique partial index to prevent multiple active training jobs for the same model
-- This prevents race conditions where two concurrent requests could create duplicate active jobs
-- PostgreSQL supports partial indexes which allow this constraint to only apply to active jobs

-- Create unique index on model_id where status is PENDING or RUNNING
CREATE UNIQUE INDEX IF NOT EXISTS idx_ml_training_jobs_model_active
ON ml_training_jobs (model_id)
WHERE status IN ('PENDING', 'RUNNING');

-- Add comment for documentation
COMMENT ON INDEX idx_ml_training_jobs_model_active IS
'Ensures only one active (PENDING/RUNNING) training job can exist per model at a time';
