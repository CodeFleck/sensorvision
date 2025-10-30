-- Migration V32: Add serverless functions support
-- Enables users to run custom Python/Node.js code in response to events

-- Drop existing objects if they exist (handles partial migrations from failed deployments)
DROP TABLE IF EXISTS function_executions CASCADE;
DROP TABLE IF EXISTS function_triggers CASCADE;
DROP TABLE IF EXISTS serverless_functions CASCADE;
DROP TYPE IF EXISTS function_trigger_type;
DROP TYPE IF EXISTS function_runtime;

-- Function runtime types
CREATE TYPE function_runtime AS ENUM ('PYTHON_3_11', 'NODEJS_18');

-- Function trigger types
CREATE TYPE function_trigger_type AS ENUM ('HTTP', 'MQTT', 'SCHEDULED', 'DEVICE_EVENT');

-- Serverless functions table
CREATE TABLE serverless_functions (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    runtime function_runtime NOT NULL,
    code TEXT NOT NULL,
    handler VARCHAR(100) NOT NULL DEFAULT 'main',
    enabled BOOLEAN DEFAULT TRUE NOT NULL,
    timeout_seconds INTEGER DEFAULT 30 NOT NULL,
    memory_limit_mb INTEGER DEFAULT 512 NOT NULL,
    environment_variables JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),
    CONSTRAINT check_timeout CHECK (timeout_seconds > 0 AND timeout_seconds <= 300),
    CONSTRAINT check_memory CHECK (memory_limit_mb >= 128 AND memory_limit_mb <= 2048),
    UNIQUE(organization_id, name)
);

-- Function triggers table (one function can have multiple triggers)
CREATE TABLE function_triggers (
    id BIGSERIAL PRIMARY KEY,
    function_id BIGINT NOT NULL REFERENCES serverless_functions(id) ON DELETE CASCADE,
    trigger_type function_trigger_type NOT NULL,
    trigger_config JSON NOT NULL,
    enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT check_trigger_config CHECK (trigger_config IS NOT NULL)
);

-- Function execution logs
CREATE TABLE function_executions (
    id BIGSERIAL PRIMARY KEY,
    function_id BIGINT NOT NULL REFERENCES serverless_functions(id) ON DELETE CASCADE,
    trigger_id BIGINT REFERENCES function_triggers(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    duration_ms INTEGER,
    input_data JSON,
    output_data JSON,
    error_message TEXT,
    error_stack TEXT,
    memory_used_mb INTEGER,
    CONSTRAINT check_status CHECK (status IN ('RUNNING', 'SUCCESS', 'ERROR', 'TIMEOUT', 'CANCELLED'))
);

-- Indexes for performance
CREATE INDEX idx_serverless_functions_org ON serverless_functions(organization_id);
CREATE INDEX idx_serverless_functions_enabled ON serverless_functions(enabled) WHERE enabled = true;
CREATE INDEX idx_function_triggers_function ON function_triggers(function_id);
CREATE INDEX idx_function_triggers_enabled ON function_triggers(enabled) WHERE enabled = true;
CREATE INDEX idx_function_executions_function ON function_executions(function_id);
CREATE INDEX idx_function_executions_started_at ON function_executions(started_at);
CREATE INDEX idx_function_executions_status ON function_executions(status);

-- Partitioning hint for execution logs (consider time-series partitioning in production)
COMMENT ON TABLE function_executions IS 'Consider partitioning by started_at for better performance with large datasets';

-- Comments
COMMENT ON COLUMN serverless_functions.handler IS 'Name of the function to execute (e.g., "main" for Python, "handler" for Node.js)';
COMMENT ON COLUMN serverless_functions.environment_variables IS 'JSON object of environment variables available to the function';
COMMENT ON COLUMN function_triggers.trigger_config IS 'JSON configuration specific to trigger type (e.g., cron expression, MQTT topic, HTTP path)';
COMMENT ON COLUMN function_executions.duration_ms IS 'Execution time in milliseconds';
COMMENT ON COLUMN function_executions.memory_used_mb IS 'Peak memory usage in megabytes';
