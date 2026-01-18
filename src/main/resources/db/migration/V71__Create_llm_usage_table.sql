-- LLM Usage tracking table for AI-powered features
-- Tracks token usage, costs, and performance metrics for billing and analytics

CREATE TABLE llm_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,

    -- Provider and model info
    provider VARCHAR(20) NOT NULL,
    model_id VARCHAR(50) NOT NULL,
    feature_type VARCHAR(30) NOT NULL,

    -- Token usage
    input_tokens INTEGER NOT NULL DEFAULT 0,
    output_tokens INTEGER NOT NULL DEFAULT 0,
    total_tokens INTEGER NOT NULL DEFAULT 0,

    -- Cost tracking (in USD cents)
    estimated_cost_cents INTEGER,

    -- Performance metrics
    latency_ms INTEGER,

    -- Request status
    success BOOLEAN NOT NULL DEFAULT true,
    error_message VARCHAR(500),

    -- Optional reference to related entity (e.g., anomaly being explained)
    reference_type VARCHAR(50),
    reference_id UUID,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_llm_usage_org_created ON llm_usage(organization_id, created_at DESC);
CREATE INDEX idx_llm_usage_org_feature ON llm_usage(organization_id, feature_type);
CREATE INDEX idx_llm_usage_user ON llm_usage(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_llm_usage_provider ON llm_usage(organization_id, provider);
CREATE INDEX idx_llm_usage_reference ON llm_usage(reference_type, reference_id) WHERE reference_id IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE llm_usage IS 'Tracks LLM API usage for billing and analytics';
COMMENT ON COLUMN llm_usage.provider IS 'LLM provider: CLAUDE, OPENAI, GEMINI';
COMMENT ON COLUMN llm_usage.feature_type IS 'Feature type: ANOMALY_EXPLANATION, NL_QUERY, REPORT_GENERATION, ROOT_CAUSE, etc.';
COMMENT ON COLUMN llm_usage.estimated_cost_cents IS 'Estimated cost in USD cents based on token pricing';
COMMENT ON COLUMN llm_usage.reference_type IS 'Type of entity this request relates to (e.g., anomaly, device, alert)';
COMMENT ON COLUMN llm_usage.reference_id IS 'UUID of the related entity';
