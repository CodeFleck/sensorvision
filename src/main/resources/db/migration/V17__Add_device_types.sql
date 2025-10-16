-- Create device_types table
CREATE TABLE device_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (organization_id, name)
);

-- Create device_type_variables table
CREATE TABLE device_type_variables (
    id BIGSERIAL PRIMARY KEY,
    device_type_id BIGINT NOT NULL REFERENCES device_types(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    label VARCHAR(200) NOT NULL,
    unit VARCHAR(50),
    data_type VARCHAR(20) NOT NULL,
    min_value NUMERIC(20, 6),
    max_value NUMERIC(20, 6),
    required BOOLEAN NOT NULL DEFAULT false,
    default_value VARCHAR(500),
    description TEXT,
    display_order INTEGER,
    UNIQUE (device_type_id, name)
);

-- Add device_type_id to devices table
ALTER TABLE devices ADD COLUMN device_type_id BIGINT REFERENCES device_types(id) ON DELETE SET NULL;

-- Create indexes
CREATE INDEX idx_device_types_organization ON device_types(organization_id);
CREATE INDEX idx_device_types_active ON device_types(is_active);
CREATE INDEX idx_device_type_variables_device_type ON device_type_variables(device_type_id);
CREATE INDEX idx_devices_device_type ON devices(device_type_id);

-- Comments
COMMENT ON TABLE device_types IS 'Defines templates for device types with predefined variable schemas';
COMMENT ON TABLE device_type_variables IS 'Defines expected variables for each device type';
COMMENT ON COLUMN device_type_variables.data_type IS 'Data type: NUMBER, STRING, BOOLEAN, LOCATION, DATETIME, JSON';
COMMENT ON COLUMN devices.device_type_id IS 'Optional device type template';
