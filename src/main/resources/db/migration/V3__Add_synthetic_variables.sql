-- Synthetic variables table for calculated metrics
CREATE TABLE IF NOT EXISTS synthetic_variables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    expression TEXT NOT NULL, -- Mathematical expression like "kwConsumption * voltage"
    unit VARCHAR(50), -- Unit of measurement
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table to store computed synthetic variable values
CREATE TABLE IF NOT EXISTS synthetic_variable_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    synthetic_variable_id UUID NOT NULL REFERENCES synthetic_variables(id) ON DELETE CASCADE,
    telemetry_record_id UUID NOT NULL REFERENCES telemetry_records(id) ON DELETE CASCADE,
    calculated_value NUMERIC(15,6),
    timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_synthetic_variables_device_enabled ON synthetic_variables (device_id, enabled);
CREATE INDEX IF NOT EXISTS idx_synthetic_variable_values_var_time ON synthetic_variable_values (synthetic_variable_id, timestamp DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_synthetic_variable_values_unique ON synthetic_variable_values (synthetic_variable_id, telemetry_record_id);