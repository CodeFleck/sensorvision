-- Fix timestamp columns in issue_submissions table
-- PostgreSQL TIMESTAMP (without time zone) maps to LocalDateTime in Java
-- PostgreSQL TIMESTAMPTZ (with time zone) maps to Instant in Java
-- The IssueSubmission entity uses Instant, so we need TIMESTAMPTZ

ALTER TABLE issue_submissions
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC';

COMMENT ON COLUMN issue_submissions.created_at IS 'Timestamp when the issue was created (stored as UTC)';
COMMENT ON COLUMN issue_submissions.updated_at IS 'Timestamp when the issue was last updated (stored as UTC)';
