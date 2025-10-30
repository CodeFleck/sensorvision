-- V38: Add theme_preference column if it doesn't exist
-- Fixes production issue where V26 may have been marked as executed but column wasn't created

DO $$
BEGIN
    -- Add theme_preference column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'theme_preference'
    ) THEN
        ALTER TABLE users
        ADD COLUMN theme_preference VARCHAR(20) DEFAULT 'system' NOT NULL;

        -- Add constraint to ensure only valid values
        ALTER TABLE users
        ADD CONSTRAINT check_theme_preference
        CHECK (theme_preference IN ('light', 'dark', 'system'));

        -- Create index for potential filtering/reporting
        CREATE INDEX idx_users_theme_preference ON users(theme_preference);

        RAISE NOTICE 'Added theme_preference column to users table';
    ELSE
        RAISE NOTICE 'theme_preference column already exists, skipping';
    END IF;
END $$;
