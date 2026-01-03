-- Migration V63: Add Sprint 3 Plugin Providers
-- Adds MQTT_BRIDGE provider to the data_plugins provider constraint

-- Drop existing provider constraint
ALTER TABLE data_plugins DROP CONSTRAINT IF EXISTS check_provider;

-- Add new constraint with MQTT_BRIDGE
ALTER TABLE data_plugins ADD CONSTRAINT check_provider
    CHECK (provider IN (
        'LORAWAN_TTN',
        'MODBUS_TCP',
        'SIGFOX',
        'PARTICLE_CLOUD',
        'HTTP_WEBHOOK',
        'CSV_FILE',
        'CUSTOM_PARSER',
        'MQTT_CUSTOM',
        'MQTT_BRIDGE',
        'SLACK',
        'DISCORD',
        'TELEGRAM',
        'PAGERDUTY'
    ));
