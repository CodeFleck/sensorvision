-- V51: Add custom_variables column to telemetry_records for flexible demo mode telemetry
-- This allows storing arbitrary sensor data (temperature, vibration, RPM, etc.) alongside standard metrics

ALTER TABLE telemetry_records
ADD COLUMN IF NOT EXISTS custom_variables JSONB;

-- Add index for JSONB queries (useful for demo mode and future flexible telemetry)
CREATE INDEX IF NOT EXISTS idx_telemetry_custom_variables ON telemetry_records USING GIN (custom_variables);

-- Add comment for documentation
COMMENT ON COLUMN telemetry_records.custom_variables IS 'Flexible JSONB storage for custom sensor variables (e.g., temperature, vibration, RPM for manufacturing sensors)';
