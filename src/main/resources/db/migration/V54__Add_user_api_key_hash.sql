-- Add key_hash and key_prefix columns for secure API key storage
-- key_prefix: first 8 characters of the key (for fast lookups)
-- key_hash: BCrypt hash of the full key (for secure validation)

ALTER TABLE user_api_keys
    ADD COLUMN key_prefix VARCHAR(8),
    ADD COLUMN key_hash VARCHAR(255);

-- Index for prefix-based lookups during authentication
CREATE INDEX idx_user_api_keys_key_prefix ON user_api_keys(key_prefix) WHERE revoked_at IS NULL;

-- Migrate existing plaintext keys to hashed format
-- This will be done by the application on first startup
-- For now, just add a comment explaining the migration
COMMENT ON COLUMN user_api_keys.key_prefix IS 'First 8 characters of the API key for fast lookups';
COMMENT ON COLUMN user_api_keys.key_hash IS 'BCrypt hash of the full API key for secure validation';
COMMENT ON COLUMN user_api_keys.key_value IS 'DEPRECATED: Will be NULL for new keys. Plaintext key value, only stored temporarily for display after creation.';
