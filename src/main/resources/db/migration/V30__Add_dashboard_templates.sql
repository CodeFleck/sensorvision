-- Migration V30: Add dashboard templates system
-- Enables pre-built dashboard templates for common use cases

CREATE TABLE dashboard_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL, -- SMART_METER, ENVIRONMENTAL, INDUSTRIAL, FLEET, ENERGY
    icon VARCHAR(50),
    preview_image_url VARCHAR(255),

    -- Template configuration (JSON)
    dashboard_config JSON NOT NULL, -- Dashboard metadata (name, description, layout)
    widgets_config JSON NOT NULL,   -- Widget definitions array

    -- Metadata
    is_system BOOLEAN DEFAULT FALSE NOT NULL, -- System templates vs user-created
    usage_count INTEGER DEFAULT 0 NOT NULL,  -- Track template popularity

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT check_category CHECK (category IN ('SMART_METER', 'ENVIRONMENTAL', 'INDUSTRIAL', 'FLEET', 'ENERGY', 'CUSTOM'))
);

-- Index for category-based filtering
CREATE INDEX idx_dashboard_templates_category ON dashboard_templates(category);
CREATE INDEX idx_dashboard_templates_is_system ON dashboard_templates(is_system);

COMMENT ON TABLE dashboard_templates IS 'Pre-built dashboard templates for quick start';
COMMENT ON COLUMN dashboard_templates.dashboard_config IS 'Dashboard metadata (name, description, layout)';
COMMENT ON COLUMN dashboard_templates.widgets_config IS 'Array of widget configurations';
COMMENT ON COLUMN dashboard_templates.is_system IS 'True for built-in templates, false for user-created';
COMMENT ON COLUMN dashboard_templates.usage_count IS 'Number of times this template has been used';
