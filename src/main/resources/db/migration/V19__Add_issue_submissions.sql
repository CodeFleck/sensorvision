-- Issue Submissions Table
-- Stores user-submitted bug reports, feature requests, and support questions

CREATE TABLE issue_submissions (
    id BIGSERIAL PRIMARY KEY,

    -- User and organization tracking
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Issue details
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('BUG', 'FEATURE_REQUEST', 'QUESTION', 'OTHER')),
    severity VARCHAR(50) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'IN_REVIEW', 'RESOLVED', 'CLOSED')),

    -- Screenshot attachment (stored as binary data)
    screenshot_filename VARCHAR(255),
    screenshot_data BYTEA,
    screenshot_content_type VARCHAR(100),

    -- Browser and context information
    browser_info TEXT,
    page_url VARCHAR(500),
    user_agent TEXT,
    screen_resolution VARCHAR(50),

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_issue_submissions_user_id ON issue_submissions(user_id);
CREATE INDEX idx_issue_submissions_organization_id ON issue_submissions(organization_id);
CREATE INDEX idx_issue_submissions_category ON issue_submissions(category);
CREATE INDEX idx_issue_submissions_status ON issue_submissions(status);
CREATE INDEX idx_issue_submissions_created_at ON issue_submissions(created_at DESC);

-- Add comment for documentation
COMMENT ON TABLE issue_submissions IS 'Stores user-submitted issues, bug reports, and feature requests with optional screenshots';
COMMENT ON COLUMN issue_submissions.screenshot_data IS 'Binary data of screenshot image (PNG format)';
COMMENT ON COLUMN issue_submissions.category IS 'Type of issue: BUG, FEATURE_REQUEST, QUESTION, or OTHER';
COMMENT ON COLUMN issue_submissions.severity IS 'Priority level: LOW, MEDIUM, HIGH, or CRITICAL';
