-- Add avatar fields to users table for profile picture functionality
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(255);
ALTER TABLE users ADD COLUMN avatar_version BIGINT DEFAULT 0;

-- Add index for faster avatar lookups
CREATE INDEX idx_users_avatar_url ON users(avatar_url);

-- Add comments for documentation
COMMENT ON COLUMN users.avatar_url IS 'Relative path to user avatar image file';
COMMENT ON COLUMN users.avatar_version IS 'Timestamp for cache busting, updated on avatar upload';
