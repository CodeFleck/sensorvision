-- Add view tracking to issue_submissions
-- Tracks when user last viewed their ticket to show unread badges

ALTER TABLE issue_submissions
    ADD COLUMN last_viewed_at TIMESTAMP WITH TIME ZONE;

-- Add index for querying unread tickets
CREATE INDEX idx_issue_submissions_last_viewed ON issue_submissions(user_id, last_viewed_at);

COMMENT ON COLUMN issue_submissions.last_viewed_at IS 'Timestamp when user last viewed this ticket (for unread notification badges)';
