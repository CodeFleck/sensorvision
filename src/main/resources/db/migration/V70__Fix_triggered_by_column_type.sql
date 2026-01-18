-- Fix triggered_by column type from BIGINT to UUID
-- The deprecated triggered_by column should be UUID type to match entity definition

-- Drop the column and recreate with correct type
-- Data loss is acceptable since this is a deprecated column and triggered_by_user_id is the new standard
ALTER TABLE ml_training_jobs DROP COLUMN IF EXISTS triggered_by;

ALTER TABLE ml_training_jobs ADD COLUMN triggered_by UUID;

COMMENT ON COLUMN ml_training_jobs.triggered_by IS 'DEPRECATED: Use triggered_by_user_id instead. Kept for backward compatibility.';
