-- Add scheduled_revocation_at column for grace period rotation
-- When set, the key remains valid until this timestamp
-- Used for zero-downtime key rotation in distributed systems

ALTER TABLE user_api_keys
    ADD COLUMN scheduled_revocation_at TIMESTAMP;

-- Index for the scheduled task that processes pending revocations
CREATE INDEX idx_user_api_keys_scheduled_revocation
    ON user_api_keys(scheduled_revocation_at)
    WHERE scheduled_revocation_at IS NOT NULL AND revoked_at IS NULL;

COMMENT ON COLUMN user_api_keys.scheduled_revocation_at IS 'Future timestamp when the key will be automatically revoked. Used for grace period during key rotation.';
