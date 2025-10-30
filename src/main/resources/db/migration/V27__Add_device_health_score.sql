-- Add health_score column to devices table
-- Score range: 0-100 (0 = critical, 100 = perfect health)

ALTER TABLE devices
ADD COLUMN health_score INTEGER DEFAULT 100;

-- Add constraint to ensure score is within valid range
ALTER TABLE devices
ADD CONSTRAINT check_health_score_range
CHECK (health_score >= 0 AND health_score <= 100);

-- Add last_health_check timestamp
ALTER TABLE devices
ADD COLUMN last_health_check_at TIMESTAMP;

-- Create index for filtering by health status
CREATE INDEX idx_devices_health_score ON devices(health_score);

-- Add comments for documentation
COMMENT ON COLUMN devices.health_score IS 'Device health score from 0 (critical) to 100 (perfect). Calculated based on uptime, alerts, and data quality.';
COMMENT ON COLUMN devices.last_health_check_at IS 'Timestamp of last health score calculation';
