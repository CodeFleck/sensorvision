-- V35: Data Retention Policies
-- Automatically archive old telemetry data to reduce database size

CREATE TABLE data_retention_policies (
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

CREATE INDEX idx_retention_policies_org ON data_retention_policies(organization_id);
CREATE INDEX idx_retention_policies_enabled ON data_retention_policies(enabled) WHERE enabled = TRUE;

-- Archive execution history
CREATE TABLE data_archive_executions (
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

CREATE INDEX idx_archive_executions_policy ON data_archive_executions(policy_id);
CREATE INDEX idx_archive_executions_org ON data_archive_executions(organization_id);
CREATE INDEX idx_archive_executions_started ON data_archive_executions(started_at DESC);

-- Add archived flag to telemetry_records for tracking
ALTER TABLE telemetry_records ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_telemetry_archived ON telemetry_records(archived) WHERE archived = FALSE;

-- Add composite index for efficient archival queries
CREATE INDEX IF NOT EXISTS idx_telemetry_archival_lookup
    ON telemetry_records(organization_id, timestamp, archived)
    WHERE archived = FALSE;

COMMENT ON TABLE data_retention_policies IS 'Organization-level data retention and archival policies';
COMMENT ON TABLE data_archive_executions IS 'History of archival job executions';
COMMENT ON COLUMN telemetry_records.archived IS 'Indicates if this record has been archived';
