-- Add password protection for shared dashboards
ALTER TABLE dashboards
ADD COLUMN share_password_hash VARCHAR(255),
ADD COLUMN share_settings JSONB DEFAULT '{}';

-- Add comment for documentation
COMMENT ON COLUMN dashboards.share_password_hash IS 'BCrypt hashed password for password-protected shared dashboards';
COMMENT ON COLUMN dashboards.share_settings IS 'Additional sharing settings (e.g., allow embedding, show branding)';
