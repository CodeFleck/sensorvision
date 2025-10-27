-- Issue Comments Table
-- Stores comments/replies on issue submissions for admin-user communication

CREATE TABLE issue_comments (
    id BIGSERIAL PRIMARY KEY,

    -- References
    issue_id BIGINT NOT NULL REFERENCES issue_submissions(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Comment content
    message TEXT NOT NULL,

    -- Internal comments are only visible to admins (for admin notes)
    is_internal BOOLEAN NOT NULL DEFAULT false,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_issue_comments_issue_id ON issue_comments(issue_id);
CREATE INDEX idx_issue_comments_author_id ON issue_comments(author_id);
CREATE INDEX idx_issue_comments_created_at ON issue_comments(created_at DESC);

-- Comments for documentation
COMMENT ON TABLE issue_comments IS 'Stores comments and replies on issue submissions for admin-user communication';
COMMENT ON COLUMN issue_comments.is_internal IS 'If true, comment is only visible to admins (for internal notes)';
