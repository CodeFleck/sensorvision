-- Variable definitions table for metadata management
CREATE TABLE variables (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,  -- e.g., "kwConsumption", "temperature"
    display_name VARCHAR(255),  -- e.g., "Power Consumption"
    description TEXT,
    unit VARCHAR(50),  -- e.g., "kW", "°C", "V", "A"
    data_type VARCHAR(50) NOT NULL DEFAULT 'NUMBER',  -- NUMBER, BOOLEAN, STRING, JSON
    icon VARCHAR(100),  -- Icon name or URL
    color VARCHAR(7),  -- Hex color for visualization
    min_value DECIMAL(20, 6),
    max_value DECIMAL(20, 6),
    decimal_places INT DEFAULT 2,
    is_system_variable BOOLEAN NOT NULL DEFAULT false,  -- System vs custom variables
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, name)
);

CREATE INDEX idx_variables_organization ON variables(organization_id);
CREATE INDEX idx_variables_name ON variables(name);

-- Device-specific variable configuration (overrides)
CREATE TABLE device_variables (
    id BIGSERIAL PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    variable_id BIGINT NOT NULL REFERENCES variables(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT true,
    custom_unit VARCHAR(50),
    custom_min_value DECIMAL(20, 6),
    custom_max_value DECIMAL(20, 6),
    custom_display_name VARCHAR(255),
    last_value DECIMAL(20, 6),
    last_updated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(device_id, variable_id)
);

CREATE INDEX idx_device_variables_device ON device_variables(device_id);
CREATE INDEX idx_device_variables_variable ON device_variables(variable_id);

-- Insert default system variables
INSERT INTO variables (organization_id, name, display_name, description, unit, data_type, is_system_variable, decimal_places)
SELECT
    o.id,
    'kwConsumption',
    'Power Consumption',
    'Electrical power consumption',
    'kW',
    'NUMBER',
    true,
    2
FROM organizations o
ON CONFLICT DO NOTHING;

INSERT INTO variables (organization_id, name, display_name, description, unit, data_type, is_system_variable, decimal_places)
SELECT
    o.id,
    'voltage',
    'Voltage',
    'Electrical voltage',
    'V',
    'NUMBER',
    true,
    1
FROM organizations o
ON CONFLICT DO NOTHING;

INSERT INTO variables (organization_id, name, display_name, description, unit, data_type, is_system_variable, decimal_places)
SELECT
    o.id,
    'current',
    'Current',
    'Electrical current',
    'A',
    'NUMBER',
    true,
    2
FROM organizations o
ON CONFLICT DO NOTHING;

INSERT INTO variables (organization_id, name, display_name, description, unit, data_type, is_system_variable, decimal_places)
SELECT
    o.id,
    'powerFactor',
    'Power Factor',
    'Power factor ratio',
    '',
    'NUMBER',
    true,
    3
FROM organizations o
ON CONFLICT DO NOTHING;

INSERT INTO variables (organization_id, name, display_name, description, unit, data_type, is_system_variable, decimal_places)
SELECT
    o.id,
    'frequency',
    'Frequency',
    'AC frequency',
    'Hz',
    'NUMBER',
    true,
    2
FROM organizations o
ON CONFLICT DO NOTHING;
