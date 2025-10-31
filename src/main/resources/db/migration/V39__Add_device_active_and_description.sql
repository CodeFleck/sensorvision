-- Add description and active fields to devices table
-- These fields support better device management and E2E testing

-- Add description column for device documentation
ALTER TABLE devices
ADD COLUMN IF NOT EXISTS description TEXT;

-- Add active column for device enable/disable functionality
ALTER TABLE devices
ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;

-- Create index on active status for faster filtering
CREATE INDEX IF NOT EXISTS idx_devices_active ON devices(active);

-- Add comment for documentation
COMMENT ON COLUMN devices.description IS 'Optional description for the device';
COMMENT ON COLUMN devices.active IS 'Whether the device is currently active (enabled)';
