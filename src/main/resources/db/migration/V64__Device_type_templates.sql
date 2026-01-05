-- V64: Device Type Templates for Auto-Provisioning
-- Adds rule templates, dashboard templates, and system templates to device types

-- Add additional columns to device_types for template features
ALTER TABLE device_types
ADD COLUMN IF NOT EXISTS is_system_template BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS template_category VARCHAR(50),
ADD COLUMN IF NOT EXISTS color VARCHAR(7),
ADD COLUMN IF NOT EXISTS dashboard_layout JSONB;

-- Device Type Rule Templates
-- Rules that will be auto-created when a device is assigned to this type
CREATE TABLE IF NOT EXISTS device_type_rule_templates (
    id BIGSERIAL PRIMARY KEY,
    device_type_id BIGINT NOT NULL REFERENCES device_types(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    variable_name VARCHAR(100) NOT NULL,
    operator VARCHAR(20) NOT NULL,
    threshold_value NUMERIC(20,6) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    notification_message TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_device_type_rule_templates_device_type
ON device_type_rule_templates(device_type_id);

-- Device Type Dashboard Templates
-- Dashboard widgets that will be auto-created when a device is assigned to this type
CREATE TABLE IF NOT EXISTS device_type_dashboard_templates (
    id BIGSERIAL PRIMARY KEY,
    device_type_id BIGINT NOT NULL REFERENCES device_types(id) ON DELETE CASCADE,
    widget_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    variable_name VARCHAR(100),
    config JSONB,
    grid_x INTEGER NOT NULL DEFAULT 0,
    grid_y INTEGER NOT NULL DEFAULT 0,
    grid_width INTEGER NOT NULL DEFAULT 4,
    grid_height INTEGER NOT NULL DEFAULT 2,
    display_order INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_device_type_dashboard_templates_device_type
ON device_type_dashboard_templates(device_type_id);

-- Track which templates have been applied to devices
CREATE TABLE IF NOT EXISTS device_template_applications (
    id BIGSERIAL PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    device_type_id BIGINT NOT NULL REFERENCES device_types(id) ON DELETE CASCADE,
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_by VARCHAR(255),
    variables_created INTEGER NOT NULL DEFAULT 0,
    rules_created INTEGER NOT NULL DEFAULT 0,
    dashboard_created BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(device_id, device_type_id)
);

CREATE INDEX IF NOT EXISTS idx_device_template_applications_device
ON device_template_applications(device_id);
CREATE INDEX IF NOT EXISTS idx_device_template_applications_device_type
ON device_template_applications(device_type_id);

COMMENT ON TABLE device_type_rule_templates IS 'Rule templates that will be auto-created when a device is assigned to this type';
COMMENT ON TABLE device_type_dashboard_templates IS 'Dashboard widget templates that will be auto-created when a device is assigned to this type';
COMMENT ON TABLE device_template_applications IS 'Tracks which device types have been applied to which devices';
COMMENT ON COLUMN device_types.is_system_template IS 'True if this is a system-provided template (cannot be modified or deleted)';
COMMENT ON COLUMN device_types.template_category IS 'Category for grouping templates: ENERGY, ENVIRONMENTAL, INDUSTRIAL, SMART_HOME, CUSTOM';
