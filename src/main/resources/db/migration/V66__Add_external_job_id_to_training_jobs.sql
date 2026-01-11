-- Add external_job_id column to track Python ML service job ID
-- This allows us to poll the Python ML service for status updates

ALTER TABLE ml_training_jobs
ADD COLUMN IF NOT EXISTS external_job_id UUID;

-- Create index for efficient lookup by external job ID
CREATE INDEX IF NOT EXISTS idx_ml_training_jobs_external
ON ml_training_jobs(external_job_id)
WHERE external_job_id IS NOT NULL;

-- Add index for finding active jobs (for status monitor polling)
CREATE INDEX IF NOT EXISTS idx_ml_training_jobs_active
ON ml_training_jobs(status, started_at)
WHERE status IN ('PENDING', 'RUNNING');

COMMENT ON COLUMN ml_training_jobs.external_job_id IS 'UUID of the training job in the Python ML service, used for status polling';
