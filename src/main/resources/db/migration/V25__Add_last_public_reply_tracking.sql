-- V25: Add last_public_reply_at to track when admin last replied publicly
-- This fixes the unread badge to only show for actual admin replies, not status changes

-- Add column to track when the last public (non-internal) admin reply was posted
ALTER TABLE issue_submissions
ADD COLUMN last_public_reply_at TIMESTAMP WITH TIME ZONE;

-- Add index for efficient querying of unread tickets
CREATE INDEX idx_issue_submissions_last_public_reply
ON issue_submissions(last_public_reply_at)
WHERE last_public_reply_at IS NOT NULL;

-- Add composite index for the unread count query
CREATE INDEX idx_issue_submissions_unread_check
ON issue_submissions(user_id, last_public_reply_at, last_viewed_at)
WHERE last_public_reply_at IS NOT NULL;

-- Backfill existing tickets: set last_public_reply_at to the timestamp of the latest public admin comment
-- This ensures existing tickets with admin replies will show as unread if not yet viewed
-- Uses role-based authorization: joins user_roles and roles to identify ROLE_ADMIN
UPDATE issue_submissions iss
SET last_public_reply_at = (
    SELECT MAX(ic.created_at)
    FROM issue_comments ic
    JOIN users u ON ic.author_id = u.id
    JOIN user_roles ur ON u.id = ur.user_id
    JOIN roles r ON ur.role_id = r.id
    WHERE ic.issue_id = iss.id
      AND ic.is_internal = false
      AND r.name = 'ROLE_ADMIN'
)
WHERE EXISTS (
    SELECT 1
    FROM issue_comments ic
    JOIN users u ON ic.author_id = u.id
    JOIN user_roles ur ON u.id = ur.user_id
    JOIN roles r ON ur.role_id = r.id
    WHERE ic.issue_id = iss.id
      AND ic.is_internal = false
      AND r.name = 'ROLE_ADMIN'
);

-- Add comment for documentation
COMMENT ON COLUMN issue_submissions.last_public_reply_at IS 'Timestamp of the last public (non-internal) admin reply. Used to determine if ticket has unread admin responses.';
