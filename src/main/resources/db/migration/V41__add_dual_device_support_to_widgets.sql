-- Add dual device support to widgets table
ALTER TABLE widgets ADD COLUMN IF NOT EXISTS second_device_id VARCHAR(255);
ALTER TABLE widgets ADD COLUMN IF NOT EXISTS second_variable_name VARCHAR(255);
ALTER TABLE widgets ADD COLUMN IF NOT EXISTS second_device_label VARCHAR(255);

-- Remove the use_context_device column (legacy feature)
ALTER TABLE widgets DROP COLUMN IF EXISTS use_context_device;

-- Create indexes for second device lookup
CREATE INDEX IF NOT EXISTS idx_widgets_second_device_id ON widgets(second_device_id);

-- Comment on new columns
COMMENT ON COLUMN widgets.second_device_id IS 'Optional second device ID for dual-device widgets (e.g., comparing two sensors)';
COMMENT ON COLUMN widgets.second_variable_name IS 'Variable name for the second device data';
COMMENT ON COLUMN widgets.second_device_label IS 'Display label for the second device data series';
