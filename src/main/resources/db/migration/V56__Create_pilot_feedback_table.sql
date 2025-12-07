-- Migration V52: Create pilot feedback table for collecting user feedback during pilot program
-- This table stores feedback submissions from pilot program participants

CREATE TABLE pilot_feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    organization_id VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 10),
    title VARCHAR(500),
    message TEXT,
    email VARCHAR(255),
    metadata JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    admin_notes TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_pilot_feedback_organization_id ON pilot_feedback(organization_id);
CREATE INDEX idx_pilot_feedback_user_id ON pilot_feedback(user_id);
CREATE INDEX idx_pilot_feedback_rating ON pilot_feedback(rating);
CREATE INDEX idx_pilot_feedback_category ON pilot_feedback(category);
CREATE INDEX idx_pilot_feedback_status ON pilot_feedback(status);
CREATE INDEX idx_pilot_feedback_submitted_at ON pilot_feedback(submitted_at DESC);
CREATE INDEX idx_pilot_feedback_updated_at ON pilot_feedback(updated_at DESC);

-- Composite indexes for common queries
CREATE INDEX idx_pilot_feedback_org_submitted ON pilot_feedback(organization_id, submitted_at DESC);
CREATE INDEX idx_pilot_feedback_rating_submitted ON pilot_feedback(rating, submitted_at DESC);
CREATE INDEX idx_pilot_feedback_status_submitted ON pilot_feedback(status, submitted_at ASC);

-- GIN index for metadata JSONB column
CREATE INDEX idx_pilot_feedback_metadata ON pilot_feedback USING GIN(metadata);

-- Add comments for documentation
COMMENT ON TABLE pilot_feedback IS 'Stores feedback submissions from pilot program participants';
COMMENT ON COLUMN pilot_feedback.user_id IS 'ID of the user who submitted the feedback';
COMMENT ON COLUMN pilot_feedback.organization_id IS 'ID of the organization the user belongs to';
COMMENT ON COLUMN pilot_feedback.category IS 'Category of feedback (e.g., bug, feature_request, general)';
COMMENT ON COLUMN pilot_feedback.rating IS 'User rating from 1-10 (1=very dissatisfied, 10=very satisfied)';
COMMENT ON COLUMN pilot_feedback.title IS 'Brief title or summary of the feedback';
COMMENT ON COLUMN pilot_feedback.message IS 'Detailed feedback message from the user';
COMMENT ON COLUMN pilot_feedback.email IS 'Optional email for follow-up communication';
COMMENT ON COLUMN pilot_feedback.metadata IS 'Additional structured data (browser info, page context, etc.)';
COMMENT ON COLUMN pilot_feedback.status IS 'Processing status (NEW, IN_PROGRESS, RESOLVED, CLOSED)';
COMMENT ON COLUMN pilot_feedback.admin_notes IS 'Internal notes from administrators';
COMMENT ON COLUMN pilot_feedback.submitted_at IS 'When the feedback was submitted';
COMMENT ON COLUMN pilot_feedback.updated_at IS 'When the feedback was last updated by an admin';

-- Create a function to automatically update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_pilot_feedback_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at on row updates
CREATE TRIGGER trigger_pilot_feedback_updated_at
    BEFORE UPDATE ON pilot_feedback
    FOR EACH ROW
    EXECUTE FUNCTION update_pilot_feedback_updated_at();

-- Insert some sample feedback categories for reference
INSERT INTO pilot_feedback (user_id, organization_id, category, rating, title, message, status, submitted_at) VALUES
('sample-user-1', 'sample-org-1', 'general', 8, 'Great platform overall', 'Really enjoying the pilot program. The dashboard is intuitive and the device integration was smooth.', 'RESOLVED', CURRENT_TIMESTAMP - INTERVAL '5 days'),
('sample-user-2', 'sample-org-1', 'feature_request', 7, 'Would love mobile app', 'The web interface is great, but a mobile app would be very helpful for monitoring on the go.', 'NEW', CURRENT_TIMESTAMP - INTERVAL '3 days'),
('sample-user-3', 'sample-org-2', 'bug', 3, 'Dashboard loading slowly', 'The dashboard takes a long time to load when I have many devices. Sometimes it times out.', 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '2 days'),
('sample-user-4', 'sample-org-2', 'general', 9, 'Excellent support', 'The pilot support team has been incredibly helpful. Quick responses and very knowledgeable.', 'RESOLVED', CURRENT_TIMESTAMP - INTERVAL '1 day');

-- Create a view for feedback analytics
CREATE VIEW pilot_feedback_analytics AS
SELECT 
    DATE_TRUNC('day', submitted_at) as feedback_date,
    organization_id,
    category,
    COUNT(*) as feedback_count,
    AVG(rating) as average_rating,
    COUNT(CASE WHEN rating <= 2 THEN 1 END) as critical_count,
    COUNT(CASE WHEN rating >= 9 THEN 1 END) as promoter_count,
    COUNT(CASE WHEN rating <= 6 THEN 1 END) as detractor_count
FROM pilot_feedback
GROUP BY DATE_TRUNC('day', submitted_at), organization_id, category
ORDER BY feedback_date DESC;

COMMENT ON VIEW pilot_feedback_analytics IS 'Aggregated analytics view for pilot feedback data';

-- Create a view for NPS calculation
CREATE VIEW pilot_nps_summary AS
SELECT 
    organization_id,
    DATE_TRUNC('week', submitted_at) as week_start,
    COUNT(*) as total_responses,
    COUNT(CASE WHEN rating >= 9 THEN 1 END) as promoters,
    COUNT(CASE WHEN rating <= 6 THEN 1 END) as detractors,
    COUNT(CASE WHEN rating BETWEEN 7 AND 8 THEN 1 END) as passives,
    ROUND(
        (COUNT(CASE WHEN rating >= 9 THEN 1 END)::DECIMAL - COUNT(CASE WHEN rating <= 6 THEN 1 END)::DECIMAL) 
        / COUNT(*)::DECIMAL * 100, 
        2
    ) as nps_score
FROM pilot_feedback
GROUP BY organization_id, DATE_TRUNC('week', submitted_at)
ORDER BY week_start DESC, organization_id;

COMMENT ON VIEW pilot_nps_summary IS 'Net Promoter Score calculation by organization and week';