-- Rules table for defining conditional logic
CREATE TABLE IF NOT EXISTS rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    variable VARCHAR(50) NOT NULL, -- kwConsumption, voltage, current, etc.
    operator VARCHAR(10) NOT NULL, -- GT, GTE, LT, LTE, EQ
    threshold NUMERIC(15,6) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Alerts table for storing triggered alerts
CREATE TABLE IF NOT EXISTS alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    triggered_value NUMERIC(15,6),
    acknowledged BOOLEAN NOT NULL DEFAULT false,
    acknowledged_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_rules_device_enabled ON rules (device_id, enabled);
CREATE INDEX IF NOT EXISTS idx_alerts_rule_created ON alerts (rule_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_acknowledged_created ON alerts (acknowledged, created_at DESC);