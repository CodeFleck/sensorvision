-- Add geolocation columns to devices table
ALTER TABLE devices ADD COLUMN latitude DECIMAL(10, 8);
ALTER TABLE devices ADD COLUMN longitude DECIMAL(11, 8);
ALTER TABLE devices ADD COLUMN altitude DECIMAL(10, 2);  -- meters
ALTER TABLE devices ADD COLUMN location_updated_at TIMESTAMP;

-- Create index for geospatial queries
CREATE INDEX idx_devices_location ON devices(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Add geolocation to telemetry records (for moving devices/vehicles)
ALTER TABLE telemetry_records ADD COLUMN latitude DECIMAL(10, 8);
ALTER TABLE telemetry_records ADD COLUMN longitude DECIMAL(11, 8);
ALTER TABLE telemetry_records ADD COLUMN altitude DECIMAL(10, 2);

CREATE INDEX idx_telemetry_location ON telemetry_records(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Create geofence table for location-based alerts
CREATE TABLE geofences (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    shape VARCHAR(50) NOT NULL DEFAULT 'CIRCLE',  -- CIRCLE, POLYGON, RECTANGLE
    center_latitude DECIMAL(10, 8),
    center_longitude DECIMAL(11, 8),
    radius_meters DECIMAL(10, 2),  -- for CIRCLE
    polygon_coordinates JSONB,  -- for POLYGON: [[lat1,lon1], [lat2,lon2], ...]
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_geofences_organization ON geofences(organization_id);
CREATE INDEX idx_geofences_location ON geofences(center_latitude, center_longitude);

-- Geofence assignments to devices
CREATE TABLE geofence_assignments (
    geofence_id BIGINT NOT NULL REFERENCES geofences(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    alert_on_enter BOOLEAN NOT NULL DEFAULT true,
    alert_on_exit BOOLEAN NOT NULL DEFAULT true,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (geofence_id, device_id)
);

CREATE INDEX idx_geofence_assignments_device ON geofence_assignments(device_id);
