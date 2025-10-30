-- Email Templates System
-- Allows admins to create and manage customizable email templates for various notification types

CREATE TABLE email_templates (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Template identification
    name VARCHAR(255) NOT NULL,
    description TEXT,
    template_type VARCHAR(50) NOT NULL, -- ALERT, NOTIFICATION, REPORT, WELCOME, PASSWORD_RESET, etc.

    -- Email content
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,

    -- Template configuration
    variables JSONB DEFAULT '[]'::jsonb, -- Available variables like ["deviceName", "alertMessage", "timestamp"]
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Metadata
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT unique_default_template_per_type
        UNIQUE (organization_id, template_type, is_default)
        DEFERRABLE INITIALLY DEFERRED
);

-- Indexes
CREATE INDEX idx_email_templates_organization ON email_templates(organization_id);
CREATE INDEX idx_email_templates_type ON email_templates(template_type);
CREATE INDEX idx_email_templates_active ON email_templates(active);
CREATE INDEX idx_email_templates_default ON email_templates(organization_id, template_type, is_default) WHERE is_default = TRUE;

-- Comments
COMMENT ON TABLE email_templates IS 'Customizable email templates for various notification types';
COMMENT ON COLUMN email_templates.template_type IS 'Type of email: ALERT, NOTIFICATION, REPORT, WELCOME, PASSWORD_RESET, etc.';
COMMENT ON COLUMN email_templates.variables IS 'JSON array of available template variables like ["deviceName", "alertMessage"]';
COMMENT ON COLUMN email_templates.is_default IS 'Whether this is the default template for this type in the organization';
