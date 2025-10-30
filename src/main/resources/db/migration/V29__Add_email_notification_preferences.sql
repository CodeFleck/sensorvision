-- Migration V29: Add email notification preferences to users table
-- This enables per-user opt-in/out for email notifications (especially ticket reply emails)

ALTER TABLE users
ADD COLUMN email_notifications_enabled BOOLEAN DEFAULT TRUE NOT NULL;

-- Add index for performance when checking email preferences
CREATE INDEX idx_users_email_notifications ON users(email_notifications_enabled);

COMMENT ON COLUMN users.email_notifications_enabled IS 'User preference for receiving email notifications (ticket replies, alerts, etc.)';
