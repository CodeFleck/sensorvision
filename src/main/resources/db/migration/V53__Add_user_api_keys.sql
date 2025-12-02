-- User API Keys table
-- Allows users to have API keys that work for all devices in their organization
-- Similar to Ubidots "Default Token" model

CREATE TABLE user_api_keys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    key_value VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL DEFAULT 'Default Token',
    description TEXT,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    CONSTRAINT fk_user_api_keys_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for fast token lookups during authentication
CREATE INDEX idx_user_api_keys_key_value ON user_api_keys(key_value) WHERE revoked_at IS NULL;

-- Index for finding user's API keys
CREATE INDEX idx_user_api_keys_user_id ON user_api_keys(user_id);

-- Add comment for documentation
COMMENT ON TABLE user_api_keys IS 'User-level API keys for programmatic access. One key works for all devices in the user''s organization.';
COMMENT ON COLUMN user_api_keys.key_value IS 'The API key value (UUID format). Used in X-API-Key header.';
COMMENT ON COLUMN user_api_keys.revoked_at IS 'Soft delete timestamp. NULL means the key is active.';
