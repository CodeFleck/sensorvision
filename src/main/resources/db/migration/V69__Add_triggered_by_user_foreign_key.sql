-- Add foreign key constraint for triggered_by_user_id
-- Uses ON DELETE SET NULL to preserve training job records when user is deleted
-- This maintains audit trail while allowing user deletion

-- First, clean up any orphaned user IDs that don't exist in users table
UPDATE ml_training_jobs
SET triggered_by_user_id = NULL
WHERE triggered_by_user_id IS NOT NULL
  AND triggered_by_user_id NOT IN (SELECT id FROM users);

-- Add foreign key constraint
ALTER TABLE ml_training_jobs
ADD CONSTRAINT fk_ml_training_jobs_triggered_by_user
FOREIGN KEY (triggered_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

COMMENT ON CONSTRAINT fk_ml_training_jobs_triggered_by_user ON ml_training_jobs IS
'Foreign key to users table with ON DELETE SET NULL to preserve training job history when user is deleted';
