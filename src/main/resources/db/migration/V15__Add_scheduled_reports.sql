-- Add scheduled_reports table
CREATE TABLE scheduled_reports (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    report_type VARCHAR(50) NOT NULL,
    export_format VARCHAR(20) NOT NULL,
    schedule_frequency VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    email_recipients TEXT,
    report_parameters TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_run_at TIMESTAMP,
    next_run_at TIMESTAMP,
    CONSTRAINT chk_report_type CHECK (report_type IN ('TELEMETRY_DATA', 'DEVICE_STATUS', 'ALERT_SUMMARY', 'ANALYTICS_SUMMARY')),
    CONSTRAINT chk_export_format CHECK (export_format IN ('CSV', 'JSON', 'EXCEL')),
    CONSTRAINT chk_schedule_frequency CHECK (schedule_frequency IN ('DAILY', 'WEEKLY', 'MONTHLY'))
);

-- Add report_executions table
CREATE TABLE report_executions (
    id BIGSERIAL PRIMARY KEY,
    scheduled_report_id BIGINT NOT NULL REFERENCES scheduled_reports(id) ON DELETE CASCADE,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    record_count INTEGER,
    file_size_bytes BIGINT,
    file_url TEXT,
    CONSTRAINT chk_execution_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

-- Add indexes
CREATE INDEX idx_scheduled_reports_organization ON scheduled_reports(organization_id);
CREATE INDEX idx_scheduled_reports_enabled ON scheduled_reports(enabled);
CREATE INDEX idx_scheduled_reports_next_run ON scheduled_reports(next_run_at);
CREATE INDEX idx_report_executions_scheduled_report ON report_executions(scheduled_report_id);
CREATE INDEX idx_report_executions_started_at ON report_executions(started_at);
