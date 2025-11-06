-- Migration V45: Add POLLING plugin type for Modbus TCP and other polling plugins
-- Extends the data_plugins table to support polling-based plugins that actively fetch data

-- Drop existing constraints
ALTER TABLE data_plugins DROP CONSTRAINT IF EXISTS check_plugin_type;

-- Add new constraint with POLLING type
ALTER TABLE data_plugins ADD CONSTRAINT check_plugin_type
    CHECK (plugin_type IN ('PROTOCOL_PARSER', 'WEBHOOK', 'INTEGRATION', 'CSV_IMPORT', 'POLLING'));
