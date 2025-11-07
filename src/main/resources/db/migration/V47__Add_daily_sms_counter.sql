-- V47: Add daily SMS counter to organization_sms_settings
-- Fixes bug where monthly counter was being used for daily limits

ALTER TABLE organization_sms_settings
ADD COLUMN current_day_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE organization_sms_settings
ADD COLUMN last_reset_date TIMESTAMPTZ;

COMMENT ON COLUMN organization_sms_settings.current_day_count IS 'SMS sent today (resets daily)';
COMMENT ON COLUMN organization_sms_settings.last_reset_date IS 'Last time daily counter was reset';
