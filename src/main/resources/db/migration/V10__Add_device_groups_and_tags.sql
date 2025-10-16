-- Add device groups for organizing devices
CREATE TABLE device_groups (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, name)
);

CREATE INDEX idx_device_groups_organization ON device_groups(organization_id);

-- Many-to-many relationship between devices and groups
CREATE TABLE device_group_members (
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    group_id BIGINT NOT NULL REFERENCES device_groups(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (device_id, group_id)
);

CREATE INDEX idx_device_group_members_device ON device_group_members(device_id);
CREATE INDEX idx_device_group_members_group ON device_group_members(group_id);

-- Add tags for flexible labeling
CREATE TABLE device_tags (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7),  -- Hex color code like #FF5733
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, name)
);

CREATE INDEX idx_device_tags_organization ON device_tags(organization_id);

-- Many-to-many relationship between devices and tags
CREATE TABLE device_tag_assignments (
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES device_tags(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (device_id, tag_id)
);

CREATE INDEX idx_device_tag_assignments_device ON device_tag_assignments(device_id);
CREATE INDEX idx_device_tag_assignments_tag ON device_tag_assignments(tag_id);

-- Add custom properties to devices (key-value pairs)
CREATE TABLE device_properties (
    id BIGSERIAL PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    property_key VARCHAR(100) NOT NULL,
    property_value TEXT,
    data_type VARCHAR(50) DEFAULT 'STRING',  -- STRING, NUMBER, BOOLEAN, JSON
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(device_id, property_key)
);

CREATE INDEX idx_device_properties_device ON device_properties(device_id);
CREATE INDEX idx_device_properties_key ON device_properties(property_key);
