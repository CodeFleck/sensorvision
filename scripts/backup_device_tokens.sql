-- =============================================================================
-- BACKUP DEVICE API TOKENS BEFORE MIGRATION
-- =============================================================================
-- This script exports existing device tokens to a backup table
-- Run this BEFORE deploying the token hashing changes
--
-- Usage:
--   psql -U sensorvision -d sensorvision -f backup_device_tokens.sql
--
-- To restore tokens if needed:
--   See restore_device_tokens.sql
-- =============================================================================

-- Create backup table if it doesn't exist
CREATE TABLE IF NOT EXISTS device_tokens_backup (
    backup_id SERIAL PRIMARY KEY,
    device_id UUID NOT NULL,
    external_id VARCHAR(64) NOT NULL,
    device_name VARCHAR(255),
    api_token VARCHAR(64),
    token_created_at TIMESTAMP,
    token_last_used_at TIMESTAMP,
    backed_up_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_device_tokens_backup_device_id
    ON device_tokens_backup(device_id);

-- Backup all devices with API tokens
INSERT INTO device_tokens_backup (
    device_id,
    external_id,
    device_name,
    api_token,
    token_created_at,
    token_last_used_at,
    notes
)
SELECT
    d.id,
    d.external_id,
    d.name,
    d.api_token,
    d.token_created_at,
    d.token_last_used_at,
    'Pre-hash-migration backup - ' || CURRENT_TIMESTAMP::TEXT
FROM devices d
WHERE d.api_token IS NOT NULL
ON CONFLICT DO NOTHING;

-- Export to CSV file for additional safety
-- Run this command separately:
-- \COPY (SELECT device_id, external_id, device_name, api_token, token_created_at, token_last_used_at FROM device_tokens_backup WHERE backed_up_at >= CURRENT_DATE) TO '/tmp/device_tokens_backup.csv' WITH CSV HEADER;

-- Display backup summary
SELECT
    COUNT(*) as total_tokens_backed_up,
    MIN(backed_up_at) as first_backup,
    MAX(backed_up_at) as last_backup
FROM device_tokens_backup
WHERE DATE(backed_up_at) = CURRENT_DATE;

-- Display devices with tokens
SELECT
    d.external_id,
    d.name,
    d.token_created_at,
    d.token_last_used_at,
    LENGTH(d.api_token) as token_length,
    CASE
        WHEN d.token_last_used_at IS NULL THEN 'Never Used'
        WHEN d.token_last_used_at < NOW() - INTERVAL '30 days' THEN 'Inactive (30+ days)'
        WHEN d.token_last_used_at < NOW() - INTERVAL '7 days' THEN 'Inactive (7-30 days)'
        ELSE 'Active'
    END as usage_status
FROM devices d
WHERE d.api_token IS NOT NULL
ORDER BY d.token_last_used_at DESC NULLS LAST;

\echo ''
\echo '=================================='
\echo 'BACKUP COMPLETE'
\echo '=================================='
\echo 'Tokens have been backed up to device_tokens_backup table'
\echo ''
\echo 'IMPORTANT: These are PLAINTEXT tokens. Keep this backup secure!'
\echo ''
\echo 'After migration, you can:'
\echo '1. Keep this backup for token rotation/recovery'
\echo '2. Export to CSV for offline storage (encrypted)'
\echo '3. Drop the backup table once migration is verified'
\echo ''
