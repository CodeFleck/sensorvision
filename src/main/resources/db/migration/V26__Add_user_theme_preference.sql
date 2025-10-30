-- Add theme preference column to users table
-- Supports 'light', 'dark', 'system' (follow system preference)

ALTER TABLE users
ADD COLUMN theme_preference VARCHAR(20) DEFAULT 'system' NOT NULL;

-- Add constraint to ensure only valid values
ALTER TABLE users
ADD CONSTRAINT check_theme_preference
CHECK (theme_preference IN ('light', 'dark', 'system'));

-- Create index for potential filtering/reporting
CREATE INDEX idx_users_theme_preference ON users(theme_preference);
