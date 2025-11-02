-- Migration V40: Add function rate limiting and quotas
-- Enables tracking of function executions and enforcement of rate limits

-- Function execution quotas table (tracks usage per organization)
CREATE TABLE function_execution_quotas (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Quota limits
    executions_per_minute INTEGER DEFAULT 60 NOT NULL,
    executions_per_hour INTEGER DEFAULT 1000 NOT NULL,
    executions_per_day INTEGER DEFAULT 10000 NOT NULL,
    executions_per_month INTEGER DEFAULT 100000 NOT NULL,

    -- Current usage (reset periodically)
    current_minute_count INTEGER DEFAULT 0 NOT NULL,
    current_hour_count INTEGER DEFAULT 0 NOT NULL,
    current_day_count INTEGER DEFAULT 0 NOT NULL,
    current_month_count INTEGER DEFAULT 0 NOT NULL,

    -- Reset timestamps
    minute_reset_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    hour_reset_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    day_reset_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    month_reset_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT check_quota_limits CHECK (
        executions_per_minute > 0 AND
        executions_per_hour > 0 AND
        executions_per_day > 0 AND
        executions_per_month > 0
    ),
    UNIQUE(organization_id)
);

-- Function execution metrics (for monitoring)
CREATE TABLE function_execution_metrics (
    id BIGSERIAL PRIMARY KEY,
    function_id BIGINT NOT NULL REFERENCES serverless_functions(id) ON DELETE CASCADE,

    -- Metrics period (hourly aggregation)
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,

    -- Execution counts
    total_executions INTEGER DEFAULT 0 NOT NULL,
    successful_executions INTEGER DEFAULT 0 NOT NULL,
    failed_executions INTEGER DEFAULT 0 NOT NULL,
    timeout_executions INTEGER DEFAULT 0 NOT NULL,

    -- Performance metrics
    avg_duration_ms BIGINT,
    min_duration_ms BIGINT,
    max_duration_ms BIGINT,
    total_duration_ms BIGINT,

    -- Resource usage
    avg_memory_mb INTEGER,
    max_memory_mb INTEGER,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT check_period CHECK (period_end > period_start),
    UNIQUE(function_id, period_start)
);

-- Indexes for performance
CREATE INDEX idx_function_execution_quotas_org ON function_execution_quotas(organization_id);
CREATE INDEX idx_function_execution_metrics_function ON function_execution_metrics(function_id);
CREATE INDEX idx_function_execution_metrics_period ON function_execution_metrics(period_start, period_end);

-- Default quota for existing organizations
INSERT INTO function_execution_quotas (organization_id)
SELECT id FROM organizations
ON CONFLICT (organization_id) DO NOTHING;

-- Comments
COMMENT ON TABLE function_execution_quotas IS 'Rate limiting quotas per organization for serverless function executions';
COMMENT ON TABLE function_execution_metrics IS 'Aggregated metrics for function executions (hourly periods)';
COMMENT ON COLUMN function_execution_quotas.executions_per_minute IS 'Maximum function executions allowed per minute';
COMMENT ON COLUMN function_execution_quotas.executions_per_hour IS 'Maximum function executions allowed per hour';
COMMENT ON COLUMN function_execution_quotas.executions_per_day IS 'Maximum function executions allowed per day';
COMMENT ON COLUMN function_execution_quotas.executions_per_month IS 'Maximum function executions allowed per month';
