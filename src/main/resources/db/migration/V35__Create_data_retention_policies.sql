-- V35: Data Retention Policies
-- Automatically archive old telemetry data to reduce database size

-- Drop existing tables if they exist (handles partial migrations from failed deployments)
DROP TABLE IF EXISTS data_archive_executions CASCADE;
DROP TABLE IF EXISTS data_retention_policies CASCADE;

CREATE TABLE IF NOT EXISTS data_retention_policies (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Retention configuration
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    retention_days INTEGER NOT NULL DEFAULT 90,

    -- Archival settings
    archive_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    archive_storage_type VARCHAR(50) NOT NULL DEFAULT 'LOCAL_FILE', -- LOCAL_FILE, S3, AZURE_BLOB
    archive_storage_config JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Execution schedule
    archive_schedule_cron VARCHAR(100) NOT NULL DEFAULT '0 2 * * *', -- Daily at 2 AM
    last_archive_run TIMESTAMP,
    last_archive_status VARCHAR(50), -- SUCCESS, FAILED, RUNNING
    last_archive_error TEXT,

    -- Statistics
    total_records_archived BIGINT NOT NULL DEFAULT 0,
    total_archive_size_bytes BIGINT NOT NULL DEFAULT 0,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- One policy per organization
    CONSTRAINT unique_policy_per_org UNIQUE (organization_id),
    CONSTRAINT check_retention_days CHECK (retention_days >= 7 AND retention_days <= 3650),
    CONSTRAINT check_storage_type CHECK (archive_storage_type IN ('LOCAL_FILE', 'S3', 'AZURE_BLOB', 'GCS'))
);

CREATE INDEX IF NOT EXISTS idx_retention_policies_org ON data_retention_policies(organization_id);
CREATE INDEX IF NOT EXISTS idx_retention_policies_enabled ON data_retention_policies(enabled) WHERE enabled = TRUE;

-- Archive execution history
CREATE TABLE IF NOT EXISTS data_archive_executions (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES data_retention_policies(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Execution details
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING', -- RUNNING, SUCCESS, FAILED

    -- Archive range
    archive_from_date TIMESTAMP NOT NULL,
    archive_to_date TIMESTAMP NOT NULL,

    -- Results
    records_archived INTEGER NOT NULL DEFAULT 0,
    archive_file_path TEXT,
    archive_size_bytes BIGINT NOT NULL DEFAULT 0,

    -- Error handling
    error_message TEXT,
    stack_trace TEXT,

    CONSTRAINT check_archive_execution_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_archive_executions_policy ON data_archive_executions(policy_id);
CREATE INDEX IF NOT EXISTS idx_archive_executions_org ON data_archive_executions(organization_id);
CREATE INDEX IF NOT EXISTS idx_archive_executions_started ON data_archive_executions(started_at DESC);

-- Add archived flag to telemetry_records for tracking
ALTER TABLE telemetry_records ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_telemetry_archived ON telemetry_records(archived) WHERE archived = FALSE;

-- Add organization_id to telemetry_records for efficient multi-tenant queries
-- This was missed in V6 when organization_id was added to other tables
DO $$
BEGIN
    -- Only add if column doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'telemetry_records' AND column_name = 'organization_id'
    ) THEN
        -- Add column as nullable first
        ALTER TABLE telemetry_records ADD COLUMN organization_id BIGINT;

        -- Populate from devices table (telemetry_records -> devices -> organization_id)
        UPDATE telemetry_records tr
        SET organization_id = d.organization_id
        FROM devices d
        WHERE tr.device_id = d.id;

        -- Make it NOT NULL after population
        ALTER TABLE telemetry_records ALTER COLUMN organization_id SET NOT NULL;

        -- Add foreign key constraint
        ALTER TABLE telemetry_records ADD CONSTRAINT fk_telemetry_organization
            FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;

        -- Add index for organization filtering
        CREATE INDEX idx_telemetry_organization ON telemetry_records(organization_id);
    END IF;
END $$;

-- Add composite index for efficient archival queries
CREATE INDEX IF NOT EXISTS idx_telemetry_archival_lookup
    ON telemetry_records(organization_id, measurement_timestamp, archived)
    WHERE archived = FALSE;

COMMENT ON TABLE data_retention_policies IS 'Organization-level data retention and archival policies';
COMMENT ON TABLE data_archive_executions IS 'History of archival job executions';
COMMENT ON COLUMN telemetry_records.archived IS 'Indicates if this record has been archived';
