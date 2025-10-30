-- Migration V34: Create Data Plugins / Protocol Parsers System
-- Allows extensible data ingestion from multiple sources and formats

-- Drop existing tables if they exist (handles partial migrations from failed deployments)
DROP TABLE IF EXISTS plugin_executions CASCADE;
DROP TABLE IF EXISTS data_plugins CASCADE;

-- Data Plugins table - stores plugin configurations
CREATE TABLE data_plugins (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    plugin_type VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_plugin_name_per_org UNIQUE (organization_id, name),
    CONSTRAINT check_plugin_type CHECK (plugin_type IN ('PROTOCOL_PARSER', 'WEBHOOK', 'INTEGRATION', 'CSV_IMPORT')),
    CONSTRAINT check_provider CHECK (provider IN ('LORAWAN_TTN', 'MODBUS_TCP', 'SIGFOX', 'PARTICLE_CLOUD', 'HTTP_WEBHOOK', 'CSV_FILE', 'CUSTOM_PARSER', 'MQTT_CUSTOM'))
);

-- Plugin Executions table - tracks execution history
CREATE TABLE plugin_executions (
    id BIGSERIAL PRIMARY KEY,
    plugin_id BIGINT NOT NULL REFERENCES data_plugins(id) ON DELETE CASCADE,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    records_processed INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    duration_ms BIGINT,
    CONSTRAINT check_execution_status CHECK (status IN ('SUCCESS', 'FAILED', 'PARTIAL'))
);

-- Indexes for performance
CREATE INDEX idx_data_plugins_org ON data_plugins(organization_id);
CREATE INDEX idx_data_plugins_type ON data_plugins(plugin_type);
CREATE INDEX idx_data_plugins_enabled ON data_plugins(enabled) WHERE enabled = TRUE;
CREATE INDEX idx_plugin_executions_plugin ON plugin_executions(plugin_id);
CREATE INDEX idx_plugin_executions_executed_at ON plugin_executions(executed_at DESC);
