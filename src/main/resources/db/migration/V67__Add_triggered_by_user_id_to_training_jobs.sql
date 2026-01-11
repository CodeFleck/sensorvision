-- Add triggered_by_user_id column for proper audit trail
-- This stores the actual user ID (Long) instead of a random UUID

ALTER TABLE ml_training_jobs
ADD COLUMN IF NOT EXISTS triggered_by_user_id BIGINT;

-- Create an index for querying jobs by user
CREATE INDEX IF NOT EXISTS idx_ml_training_jobs_triggered_by_user
ON ml_training_jobs(triggered_by_user_id)
WHERE triggered_by_user_id IS NOT NULL;

-- Add foreign key reference to users table for data integrity
-- Note: This is optional as the user might be deleted but we want to keep the audit trail
-- ALTER TABLE ml_training_jobs
-- ADD CONSTRAINT fk_ml_training_jobs_triggered_by_user
-- FOREIGN KEY (triggered_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

COMMENT ON COLUMN ml_training_jobs.triggered_by_user_id IS 'User ID who triggered this training job for audit trail';
