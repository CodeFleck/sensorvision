-- Migration V44: Add file attachment support to issue comments
-- Allows users and admins to attach log files, screenshots, config files, etc.

-- Add attachment columns to issue_comments table
ALTER TABLE issue_comments
ADD COLUMN attachment_filename VARCHAR(255),
ADD COLUMN attachment_data BYTEA,
ADD COLUMN attachment_content_type VARCHAR(100),
ADD COLUMN attachment_size_bytes BIGINT;

-- Create index for queries filtering by comments with attachments
CREATE INDEX idx_issue_comments_has_attachment
ON issue_comments(issue_id)
WHERE attachment_filename IS NOT NULL;

-- Add comments to document the columns
COMMENT ON COLUMN issue_comments.attachment_filename IS 'Original filename of attached file (e.g., error-log.txt, config.json)';
COMMENT ON COLUMN issue_comments.attachment_data IS 'Binary data of the attached file';
COMMENT ON COLUMN issue_comments.attachment_content_type IS 'MIME type of the attachment (e.g., text/plain, application/json, image/png)';
COMMENT ON COLUMN issue_comments.attachment_size_bytes IS 'Size of attachment in bytes for display and validation';
