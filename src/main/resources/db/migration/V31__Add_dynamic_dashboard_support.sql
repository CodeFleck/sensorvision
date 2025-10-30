-- Migration V31: Add dynamic dashboard support
-- Enables widgets to use context device from URL parameter instead of fixed device binding

-- Add flag to widgets to indicate they should use context device
ALTER TABLE widgets
ADD COLUMN use_context_device BOOLEAN DEFAULT FALSE NOT NULL;

-- Add device label support for flexible binding
ALTER TABLE widgets
ADD COLUMN device_label VARCHAR(255);

-- Add dashboard-level default device
ALTER TABLE dashboards
ADD COLUMN default_device_id VARCHAR(255);

-- Add index for device label queries
CREATE INDEX idx_widgets_device_label ON widgets(device_label);

COMMENT ON COLUMN widgets.use_context_device IS 'If true, widget uses device from dashboard context (URL param) instead of fixed deviceId';
COMMENT ON COLUMN widgets.device_label IS 'Optional device label for flexible binding (e.g., "primary_meter")';
COMMENT ON COLUMN dashboards.default_device_id IS 'Default device to use when no device is selected via URL parameter';
