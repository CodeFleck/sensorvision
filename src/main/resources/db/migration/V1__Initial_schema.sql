CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY,
    external_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    sensor_type VARCHAR(100),
    firmware_version VARCHAR(100),
    status VARCHAR(32) NOT NULL,
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS telemetry_records (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    measurement_timestamp TIMESTAMPTZ NOT NULL,
    kw_consumption NUMERIC(12,3),
    voltage NUMERIC(10,3),
    current NUMERIC(10,3),
    power_factor NUMERIC(5,3),
    frequency NUMERIC(6,3),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telemetry_device_time
    ON telemetry_records (device_id, measurement_timestamp DESC);
