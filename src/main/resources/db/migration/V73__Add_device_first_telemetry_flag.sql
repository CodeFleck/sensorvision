-- V73: Device First Telemetry Flag
-- Tracks whether initial widgets have been auto-generated for a device

ALTER TABLE devices ADD COLUMN initial_widgets_created BOOLEAN DEFAULT FALSE;

-- Partial index for efficient lookup of devices needing widget generation
CREATE INDEX idx_devices_initial_widgets ON devices(initial_widgets_created)
    WHERE initial_widgets_created = FALSE;
