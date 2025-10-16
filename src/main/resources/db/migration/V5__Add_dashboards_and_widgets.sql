-- V5: Add dashboards and widgets tables for customizable visualization

-- Dashboards table: container for multiple widgets
CREATE TABLE dashboards (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    layout_config JSONB, -- Stores grid layout configuration
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Widgets table: individual visualization components
CREATE TABLE widgets (
    id BIGSERIAL PRIMARY KEY,
    dashboard_id BIGINT NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,

    -- Position and size in grid layout
    position_x INT NOT NULL DEFAULT 0,
    position_y INT NOT NULL DEFAULT 0,
    width INT NOT NULL DEFAULT 4,
    height INT NOT NULL DEFAULT 4,

    -- Data source configuration
    device_id VARCHAR(255), -- Can be null for multi-device widgets
    variable_name VARCHAR(255), -- The telemetry variable to display
    aggregation VARCHAR(50) DEFAULT 'NONE',
    time_range_minutes INT, -- Time window for data (null = real-time only)

    -- Widget-specific configuration (thresholds, colors, limits, etc)
    config JSONB NOT NULL DEFAULT '{}',

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_dashboard FOREIGN KEY (dashboard_id) REFERENCES dashboards(id) ON DELETE CASCADE
);

-- Create default dashboard
INSERT INTO dashboards (name, description, is_default, layout_config)
VALUES (
    'Default Dashboard',
    'Auto-generated default dashboard with real-time monitoring',
    TRUE,
    '{"cols": 12, "rowHeight": 100}'
);

-- Indexes for performance
CREATE INDEX idx_widgets_dashboard_id ON widgets(dashboard_id);
CREATE INDEX idx_widgets_device_id ON widgets(device_id);
CREATE INDEX idx_widgets_type ON widgets(type);
CREATE INDEX idx_dashboards_default ON dashboards(is_default) WHERE is_default = TRUE;

-- Comments for documentation
COMMENT ON TABLE dashboards IS 'Stores dashboard configurations with customizable layouts';
COMMENT ON TABLE widgets IS 'Individual visualization widgets within dashboards';
COMMENT ON COLUMN widgets.config IS 'JSON configuration: thresholds, colors, refresh rates, display options, etc';
COMMENT ON COLUMN widgets.time_range_minutes IS 'Historical data window in minutes (null for real-time only)';
COMMENT ON COLUMN dashboards.layout_config IS 'Grid layout configuration for responsive design';
