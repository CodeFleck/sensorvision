-- V56: Extend variable system for EAV (Entity-Attribute-Value) pattern
-- This enables Ubidots-like functionality where any device can send any variable
-- without requiring schema changes
--
-- Strategy:
-- 1. Add device_id to variables table (nullable for org-level templates)
-- 2. Create variable_values table for time-series data storage
-- 3. Variables can be:
--    - Organization-level templates (device_id IS NULL, is_system_variable = true)
--    - Device-specific variables (device_id IS NOT NULL, auto-created on first data)

-- Add device_id column to variables for device-specific variables
ALTER TABLE variables ADD COLUMN IF NOT EXISTS device_id UUID REFERENCES devices(id) ON DELETE CASCADE;

-- Add data_source column to track where the variable came from
ALTER TABLE variables ADD COLUMN IF NOT EXISTS data_source VARCHAR(50) DEFAULT 'manual';
-- Values: 'manual' (user created), 'auto' (auto-provisioned from telemetry), 'synthetic' (calculated)

-- Add last_value columns for quick access without querying time-series
ALTER TABLE variables ADD COLUMN IF NOT EXISTS last_value DECIMAL(20, 6);
ALTER TABLE variables ADD COLUMN IF NOT EXISTS last_value_at TIMESTAMP WITH TIME ZONE;

-- Modify unique constraint to allow same variable name for different devices
-- First, drop the existing constraint
ALTER TABLE variables DROP CONSTRAINT IF EXISTS variables_organization_id_name_key;

-- Create new composite unique constraint
-- For device-specific: (device_id, name) must be unique
-- For org-level: (organization_id, name) where device_id IS NULL
CREATE UNIQUE INDEX IF NOT EXISTS idx_variables_device_name
    ON variables(device_id, name) WHERE device_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_variables_org_name_system
    ON variables(organization_id, name) WHERE device_id IS NULL;

-- Variable values table: Time-series storage for all variable data points
CREATE TABLE IF NOT EXISTS variable_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variable_id BIGINT NOT NULL REFERENCES variables(id) ON DELETE CASCADE,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    value DECIMAL(20, 6) NOT NULL,
    context JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Time-series optimized indexes for variable_values
CREATE INDEX IF NOT EXISTS idx_variable_values_variable_id ON variable_values(variable_id);
CREATE INDEX IF NOT EXISTS idx_variable_values_timestamp ON variable_values(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_variable_values_variable_timestamp ON variable_values(variable_id, timestamp DESC);

-- For range queries (analytics)
CREATE INDEX IF NOT EXISTS idx_variable_values_variable_time_range ON variable_values(variable_id, timestamp);

-- Index for querying device-specific variables
CREATE INDEX IF NOT EXISTS idx_variables_device_id ON variables(device_id) WHERE device_id IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE variable_values IS 'Time-series storage for variable data points (EAV pattern)';
COMMENT ON COLUMN variables.device_id IS 'Device this variable belongs to. NULL for organization-level templates.';
COMMENT ON COLUMN variables.data_source IS 'How the variable was created: manual, auto (from telemetry), or synthetic';
COMMENT ON COLUMN variables.last_value IS 'Most recent value for quick access without time-series query';
COMMENT ON COLUMN variable_values.context IS 'Optional JSON context data (e.g., location, quality flags)';
