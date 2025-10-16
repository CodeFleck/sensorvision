-- Add triggered_at column to alerts table for geofence alerts
ALTER TABLE alerts ADD COLUMN triggered_at TIMESTAMP;
