-- Migration V39: Add function secrets support
-- Enables secure storage of encrypted secrets for serverless functions
-- Secrets are encrypted at rest using AES-256-GCM and injected as environment variables at runtime

-- Function secrets table
CREATE TABLE function_secrets (
    id BIGSERIAL PRIMARY KEY,
    function_id BIGINT NOT NULL REFERENCES serverless_functions(id) ON DELETE CASCADE,
    secret_key VARCHAR(100) NOT NULL,
    encrypted_value TEXT NOT NULL,
    encryption_version VARCHAR(10) DEFAULT 'v1' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),
    CONSTRAINT check_secret_key_format CHECK (secret_key ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT check_secret_key_length CHECK (LENGTH(secret_key) >= 2 AND LENGTH(secret_key) <= 100),
    UNIQUE(function_id, secret_key)
);

-- Indexes for performance
CREATE INDEX idx_function_secrets_function ON function_secrets(function_id);
CREATE INDEX idx_function_secrets_key ON function_secrets(secret_key);

-- Comments
COMMENT ON TABLE function_secrets IS 'Encrypted secrets for serverless functions, injected as environment variables at runtime';
COMMENT ON COLUMN function_secrets.secret_key IS 'Environment variable name (uppercase with underscores, e.g., API_KEY, DATABASE_PASSWORD)';
COMMENT ON COLUMN function_secrets.encrypted_value IS 'AES-256-GCM encrypted secret value (base64 encoded)';
COMMENT ON COLUMN function_secrets.encryption_version IS 'Encryption algorithm version for key rotation support';

-- Security note: The encryption key should be stored in application.properties or environment variables,
-- NEVER committed to version control
