-- V36: Webhook Testing Tool
-- Store webhook test history for debugging and auditing

-- Drop existing table if it exists (handles partial migrations from failed deployments)
DROP TABLE IF EXISTS webhook_tests CASCADE;

CREATE TABLE webhook_tests (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,

    -- Request details
    name VARCHAR(200),
    url TEXT NOT NULL,
    http_method VARCHAR(10) NOT NULL DEFAULT 'POST',
    headers JSONB DEFAULT '{}'::jsonb,
    request_body TEXT,

    -- Response details
    status_code INTEGER,
    response_body TEXT,
    response_headers JSONB,
    duration_ms BIGINT,

    -- Error handling
    error_message TEXT,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_http_method CHECK (http_method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE'))
);

CREATE INDEX idx_webhook_tests_org ON webhook_tests(organization_id);
CREATE INDEX idx_webhook_tests_created_at ON webhook_tests(created_at DESC);
CREATE INDEX idx_webhook_tests_created_by ON webhook_tests(created_by);

COMMENT ON TABLE webhook_tests IS 'History of webhook test executions for debugging';
COMMENT ON COLUMN webhook_tests.duration_ms IS 'Request duration in milliseconds';
