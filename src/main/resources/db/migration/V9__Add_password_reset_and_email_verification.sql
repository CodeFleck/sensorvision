-- Add password reset fields to users table
ALTER TABLE users
ADD COLUMN password_reset_token VARCHAR(255),
ADD COLUMN password_reset_token_expiry TIMESTAMP,
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN email_verification_token VARCHAR(255),
ADD COLUMN email_verification_token_expiry TIMESTAMP;

-- Create index for password reset token lookups
CREATE INDEX idx_users_password_reset_token ON users(password_reset_token);

-- Create index for email verification token lookups
CREATE INDEX idx_users_email_verification_token ON users(email_verification_token);
