-- V46: Add SMS Notifications Support
-- Adds tables and columns for SMS alert notifications via Twilio

-- ================================================================
-- User Phone Numbers
-- ================================================================
CREATE TABLE user_phone_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phone_number VARCHAR(20) NOT NULL,  -- E.164 format: +15551234567
    country_code VARCHAR(5) NOT NULL,   -- e.g., "US", "BR", "IN"
    verified BOOLEAN DEFAULT FALSE,
    verification_code VARCHAR(6),       -- OTP for verification
    verification_expires_at TIMESTAMPTZ,
    is_primary BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, phone_number)
);

CREATE INDEX idx_user_phone_numbers_user_id ON user_phone_numbers(user_id);
CREATE INDEX idx_user_phone_numbers_primary ON user_phone_numbers(user_id, is_primary) WHERE is_primary = TRUE;
CREATE INDEX idx_user_phone_numbers_verified ON user_phone_numbers(user_id, verified) WHERE verified = TRUE;

COMMENT ON TABLE user_phone_numbers IS 'User phone numbers for SMS notifications';
COMMENT ON COLUMN user_phone_numbers.phone_number IS 'Phone number in E.164 format (e.g., +15551234567)';
COMMENT ON COLUMN user_phone_numbers.verified IS 'Phone number verified via OTP';
COMMENT ON COLUMN user_phone_numbers.is_primary IS 'Primary phone number for user (only one per user)';

-- ================================================================
-- SMS Delivery Log
-- ================================================================
CREATE TABLE sms_delivery_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id UUID REFERENCES alerts(id) ON DELETE SET NULL,
    phone_number VARCHAR(20) NOT NULL,
    message_body TEXT NOT NULL,
    twilio_sid VARCHAR(50),  -- Twilio message SID
    status VARCHAR(20) NOT NULL,  -- 'queued', 'sent', 'delivered', 'failed', 'undelivered'
    error_code VARCHAR(10),
    error_message TEXT,
    cost NUMERIC(10, 4),  -- Track SMS costs (e.g., 0.0075 for US SMS)
    sent_at TIMESTAMPTZ DEFAULT NOW(),
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_sms_delivery_log_alert_id ON sms_delivery_log(alert_id);
CREATE INDEX idx_sms_delivery_log_status ON sms_delivery_log(status);
CREATE INDEX idx_sms_delivery_log_sent_at ON sms_delivery_log(sent_at DESC);
CREATE INDEX idx_sms_delivery_log_phone ON sms_delivery_log(phone_number, sent_at DESC);

COMMENT ON TABLE sms_delivery_log IS 'Log of SMS notifications sent via Twilio';
COMMENT ON COLUMN sms_delivery_log.twilio_sid IS 'Twilio message SID for tracking';
COMMENT ON COLUMN sms_delivery_log.cost IS 'Cost in USD for this SMS';
COMMENT ON COLUMN sms_delivery_log.status IS 'Delivery status: queued, sent, delivered, failed, undelivered';

-- ================================================================
-- Organization SMS Settings
-- ================================================================
CREATE TABLE organization_sms_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    enabled BOOLEAN DEFAULT FALSE,
    daily_limit INTEGER DEFAULT 100,  -- Max SMS per day
    monthly_budget NUMERIC(10, 2) DEFAULT 50.00,  -- Max spend per month in USD
    current_month_count INTEGER DEFAULT 0,
    current_month_cost NUMERIC(10, 2) DEFAULT 0.00,
    alert_on_budget_threshold BOOLEAN DEFAULT TRUE,
    budget_threshold_percentage INTEGER DEFAULT 80,  -- Alert at 80% of budget
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(organization_id)
);

CREATE INDEX idx_organization_sms_settings_org_id ON organization_sms_settings(organization_id);
CREATE INDEX idx_organization_sms_settings_enabled ON organization_sms_settings(organization_id, enabled) WHERE enabled = TRUE;

COMMENT ON TABLE organization_sms_settings IS 'Organization-level SMS notification configuration and budget controls';
COMMENT ON COLUMN organization_sms_settings.daily_limit IS 'Maximum SMS per day to prevent abuse';
COMMENT ON COLUMN organization_sms_settings.monthly_budget IS 'Maximum monthly SMS spend in USD';
COMMENT ON COLUMN organization_sms_settings.current_month_count IS 'SMS sent this month (resets monthly)';
COMMENT ON COLUMN organization_sms_settings.budget_threshold_percentage IS 'Percentage of budget to trigger alert (default 80%)';

-- ================================================================
-- Update Rules Table for SMS
-- ================================================================
ALTER TABLE rules ADD COLUMN send_sms BOOLEAN DEFAULT FALSE;
ALTER TABLE rules ADD COLUMN sms_recipients TEXT[];  -- Array of phone numbers or special values: 'primary', 'all'

COMMENT ON COLUMN rules.send_sms IS 'Enable SMS notifications for this rule';
COMMENT ON COLUMN rules.sms_recipients IS 'Array of phone numbers to notify (E.164 format) or ["primary"] for user primary phone';

-- ================================================================
-- Create default SMS settings for existing organizations
-- ================================================================
INSERT INTO organization_sms_settings (organization_id, enabled, daily_limit, monthly_budget)
SELECT id, FALSE, 100, 50.00
FROM organizations
ON CONFLICT (organization_id) DO NOTHING;
