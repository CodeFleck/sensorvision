-- Add API token field to devices table for per-device authentication
ALTER TABLE devices ADD COLUMN api_token VARCHAR(64) UNIQUE;
ALTER TABLE devices ADD COLUMN token_created_at TIMESTAMP;
ALTER TABLE devices ADD COLUMN token_last_used_at TIMESTAMP;

-- Create index for faster token lookups
CREATE INDEX idx_devices_api_token ON devices(api_token);

-- Add comment
COMMENT ON COLUMN devices.api_token IS 'Unique API token for device authentication (UUID format)';
COMMENT ON COLUMN devices.token_created_at IS 'Timestamp when the API token was created/rotated';
COMMENT ON COLUMN devices.token_last_used_at IS 'Last time the token was used for authentication';
