-- Add soft delete support for admin-managed entities
-- Items can be restored within 30 days, after which they are permanently deleted

-- Add soft delete columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500);

-- Add soft delete columns to devices table
ALTER TABLE devices ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500);

-- Add soft delete columns to organizations table
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500);

-- Create indexes for efficient soft delete queries
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_devices_deleted_at ON devices(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_organizations_deleted_at ON organizations(deleted_at) WHERE deleted_at IS NOT NULL;

-- Create a trash_log table to track all soft delete operations for audit purposes
CREATE TABLE IF NOT EXISTS trash_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    entity_name VARCHAR(255),
    entity_snapshot JSONB,
    deleted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_by VARCHAR(255) NOT NULL,
    deletion_reason VARCHAR(500),
    restored_at TIMESTAMP WITH TIME ZONE,
    restored_by VARCHAR(255),
    permanently_deleted_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    organization_id BIGINT,
    CONSTRAINT fk_trash_log_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL
);

-- Index for finding items to permanently delete
CREATE INDEX IF NOT EXISTS idx_trash_log_expires_at ON trash_log(expires_at)
    WHERE permanently_deleted_at IS NULL AND restored_at IS NULL;

-- Index for listing deleted items by type
CREATE INDEX IF NOT EXISTS idx_trash_log_entity_type ON trash_log(entity_type, deleted_at DESC)
    WHERE permanently_deleted_at IS NULL AND restored_at IS NULL;

-- Index for listing deleted items by organization
CREATE INDEX IF NOT EXISTS idx_trash_log_organization ON trash_log(organization_id, deleted_at DESC)
    WHERE permanently_deleted_at IS NULL AND restored_at IS NULL;

COMMENT ON TABLE trash_log IS 'Audit log for soft-deleted items with 30-day retention';
COMMENT ON COLUMN trash_log.entity_snapshot IS 'JSON snapshot of entity at deletion time for restore';
COMMENT ON COLUMN trash_log.expires_at IS 'Date after which item will be permanently deleted (30 days from deletion)';
