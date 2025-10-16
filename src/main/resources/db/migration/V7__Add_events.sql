-- Events table for audit trail and system activity tracking
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    metadata JSONB,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    device_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_events_organization_id ON events(organization_id);
CREATE INDEX idx_events_event_type ON events(event_type);
CREATE INDEX idx_events_severity ON events(severity);
CREATE INDEX idx_events_entity_type_id ON events(entity_type, entity_id);
CREATE INDEX idx_events_user_id ON events(user_id);
CREATE INDEX idx_events_device_id ON events(device_id);
CREATE INDEX idx_events_created_at ON events(created_at DESC);

-- Composite index for common queries (filtering by org + time range)
CREATE INDEX idx_events_org_created ON events(organization_id, created_at DESC);
