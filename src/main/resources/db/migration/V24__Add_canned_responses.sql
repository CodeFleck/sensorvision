-- V24: Add canned responses for admin efficiency
-- Allows admins to create and use template responses for common questions

CREATE TABLE canned_responses (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    use_count INTEGER NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_canned_responses_active ON canned_responses(is_active);
CREATE INDEX idx_canned_responses_category ON canned_responses(category);
CREATE INDEX idx_canned_responses_created_by ON canned_responses(created_by);

-- Insert some default templates
INSERT INTO canned_responses (title, body, category, created_by) VALUES
('Welcome & First Response', 'Thank you for contacting SensorVision support! We''ve received your ticket and one of our team members will review it shortly. We typically respond within 2 business hours.

In the meantime, you can check our documentation at http://35.88.65.186.nip.io:8080/how-it-works for common solutions.

Best regards,
SensorVision Support Team', 'GENERAL', 1),

('Password Reset', 'To reset your password:

1. Click "Sign Out" in the top right
2. On the login page, click "Forgot Password?"
3. Enter your email address
4. Check your email for a reset link (check spam folder if needed)
5. Follow the link and create a new password

If you continue to experience issues, please let us know and we''ll assist further.

Best regards,
SensorVision Support Team', 'AUTHENTICATION', 1),

('Issue Resolved - Please Confirm', 'We believe we''ve resolved the issue you reported.

Could you please test it on your end and confirm if everything is working as expected? If you encounter any problems or have additional questions, feel free to respond to this ticket.

We''ll mark this as resolved for now, but don''t hesitate to reach out if you need further assistance!

Best regards,
SensorVision Support Team', 'RESOLUTION', 1),

('Feature Request Acknowledged', 'Thank you for your feature request! We appreciate you taking the time to share your ideas.

We''ve added this to our product roadmap and our team will review it during our next planning cycle. While we can''t guarantee when or if this feature will be implemented, we do consider all suggestions carefully.

We''ll update this ticket if there are any developments regarding your request.

Best regards,
SensorVision Support Team', 'FEATURE_REQUEST', 1),

('Bug Confirmed - Investigating', 'Thank you for reporting this issue! Our team has confirmed this is a bug and we''re actively investigating.

We''ve added it to our bug tracking system and will prioritize a fix. We''ll keep you updated on our progress through this ticket.

If you have any additional information or steps to reproduce the issue, please share them - it helps us resolve this faster!

Best regards,
SensorVision Support Team', 'BUG', 1);
