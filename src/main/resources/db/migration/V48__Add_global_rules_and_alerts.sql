-- Global Rules and Fleet-Wide Alerts Schema
-- Sprint 5: Global Events / Fleet-Wide Rules

-- Global rules table for fleet-wide monitoring
CREATE TABLE IF NOT EXISTS global_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Device selection criteria
    selector_type VARCHAR(50) NOT NULL, -- TAG, GROUP, ORGANIZATION, CUSTOM_FILTER
    selector_value TEXT, -- Tag name, group ID, or custom filter query

    -- Aggregation function and condition
    aggregation_function VARCHAR(100) NOT NULL, -- countDevices, countOnline, sum, avg, etc.
    aggregation_variable VARCHAR(100), -- Variable name for metric-based aggregations
    aggregation_params JSONB, -- Additional parameters (e.g., time windows, percentiles)

    -- Condition
    operator VARCHAR(10) NOT NULL, -- GT, GTE, LT, LTE, EQ
    threshold NUMERIC(15,6) NOT NULL,

    -- Evaluation settings
    enabled BOOLEAN NOT NULL DEFAULT true,
    evaluation_interval VARCHAR(50) NOT NULL DEFAULT '5m', -- 1m, 5m, 15m, 30m, 1h, etc.
    cooldown_minutes INTEGER NOT NULL DEFAULT 5, -- Minimum minutes between alerts

    -- Metadata
    last_evaluated_at TIMESTAMPTZ,
    last_triggered_at TIMESTAMPTZ,

    -- Notification settings
    send_sms BOOLEAN DEFAULT false,
    sms_recipients TEXT[],

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Global alerts table for fleet-wide alert records
CREATE TABLE IF NOT EXISTS global_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    global_rule_id UUID NOT NULL REFERENCES global_rules(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL

    -- Aggregation result at trigger time
    triggered_value NUMERIC(15,6),
    device_count INTEGER, -- Number of devices in scope
    affected_devices JSONB, -- Array of device IDs that contributed to the alert

    -- Alert lifecycle
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged BOOLEAN NOT NULL DEFAULT false,
    acknowledged_at TIMESTAMPTZ,
    acknowledged_by BIGINT REFERENCES users(id),

    -- Resolution tracking
    resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_at TIMESTAMPTZ,
    resolved_by BIGINT REFERENCES users(id),
    resolution_note TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_global_rules_org_enabled ON global_rules (organization_id, enabled);
CREATE INDEX IF NOT EXISTS idx_global_rules_selector_type ON global_rules (selector_type);
CREATE INDEX IF NOT EXISTS idx_global_rules_last_evaluated ON global_rules (last_evaluated_at);

CREATE INDEX IF NOT EXISTS idx_global_alerts_rule_triggered ON global_alerts (global_rule_id, triggered_at DESC);
CREATE INDEX IF NOT EXISTS idx_global_alerts_org_triggered ON global_alerts (organization_id, triggered_at DESC);
CREATE INDEX IF NOT EXISTS idx_global_alerts_acknowledged ON global_alerts (acknowledged, triggered_at DESC);
CREATE INDEX IF NOT EXISTS idx_global_alerts_resolved ON global_alerts (resolved, triggered_at DESC);

-- Add GIN index for JSONB columns for efficient querying
CREATE INDEX IF NOT EXISTS idx_global_rules_aggregation_params ON global_rules USING GIN (aggregation_params);
CREATE INDEX IF NOT EXISTS idx_global_alerts_affected_devices ON global_alerts USING GIN (affected_devices);
