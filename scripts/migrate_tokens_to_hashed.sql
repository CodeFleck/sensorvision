-- =============================================================================
-- MIGRATE DEVICE TOKENS TO HASHED FORMAT
-- =============================================================================
-- This script invalidates existing plaintext tokens, requiring users to rotate
--
-- IMPORTANT: Run backup_device_tokens.sql BEFORE running this migration
--
-- Usage:
--   psql -U sensorvision -d sensorvision -f migrate_tokens_to_hashed.sql
--
-- Options:
--   1. Invalidate all tokens (users must rotate) - RECOMMENDED
--   2. Keep tokens for manual migration - requires application-level hashing
-- =============================================================================

\echo ''
\echo '======================================================================'
\echo 'DEVICE TOKEN MIGRATION TO HASHED FORMAT'
\echo '======================================================================'
\echo ''

-- Display current token status
\echo 'Current token status:'
SELECT
    COUNT(*) as total_devices,
    COUNT(api_token) as devices_with_tokens,
    COUNT(CASE WHEN LENGTH(api_token) = 32 THEN 1 END) as plaintext_tokens,
    COUNT(CASE WHEN LENGTH(api_token) > 50 THEN 1 END) as hashed_tokens
FROM devices;

\echo ''
\echo 'Devices with active tokens:'
SELECT
    external_id,
    name,
    token_created_at,
    token_last_used_at,
    LENGTH(api_token) as token_length,
    CASE
        WHEN LENGTH(api_token) = 32 THEN 'Plaintext (needs migration)'
        WHEN LENGTH(api_token) > 50 THEN 'Hashed (already migrated)'
        ELSE 'Unknown format'
    END as token_format
FROM devices
WHERE api_token IS NOT NULL
ORDER BY token_last_used_at DESC NULLS LAST;

\echo ''
\echo '======================================================================'
\echo 'MIGRATION OPTIONS'
\echo '======================================================================'
\echo ''
\echo 'Option 1: INVALIDATE ALL TOKENS (Recommended for security)'
\echo '  - Sets all tokens to NULL'
\echo '  - Users must rotate tokens via API'
\echo '  - New tokens will be automatically hashed'
\echo '  - Most secure approach'
\echo ''
\echo 'Option 2: KEEP TOKENS for manual migration'
\echo '  - Leaves plaintext tokens in database'
\echo '  - Requires custom migration script with BCrypt hashing'
\echo '  - NOT RECOMMENDED - tokens remain exposed'
\echo ''

-- Uncomment the option you want to use:

-- OPTION 1: INVALIDATE ALL TOKENS (RECOMMENDED)
-- This forces all users to rotate their device tokens
-- New tokens will be automatically hashed by the application

/*
BEGIN;

-- Create a migration log
CREATE TABLE IF NOT EXISTS token_migration_log (
    id SERIAL PRIMARY KEY,
    migration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    devices_affected INTEGER,
    migration_type VARCHAR(50),
    notes TEXT
);

-- Count devices before migration
DO $$
DECLARE
    token_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO token_count FROM devices WHERE api_token IS NOT NULL;

    -- Invalidate all tokens
    UPDATE devices
    SET
        api_token = NULL,
        token_created_at = NULL,
        token_last_used_at = NULL
    WHERE api_token IS NOT NULL;

    -- Log the migration
    INSERT INTO token_migration_log (devices_affected, migration_type, notes)
    VALUES (
        token_count,
        'INVALIDATE_ALL',
        'Invalidated all plaintext tokens. Users must rotate tokens via API. New tokens will be BCrypt hashed.'
    );

    RAISE NOTICE 'Migration complete: % tokens invalidated', token_count;
END $$;

COMMIT;
*/

-- OPTION 2: MANUAL MIGRATION (NOT RECOMMENDED)
-- This approach requires a Java/Python script to hash existing tokens
-- Example migration script structure:
--
-- 1. Read all devices with plaintext tokens
-- 2. For each token:
--    a. Generate BCrypt hash using same PasswordEncoder
--    b. Update device with hashed token
-- 3. Log migration results
--
-- This is complex and error-prone. OPTION 1 is strongly recommended.

\echo ''
\echo '======================================================================'
\echo 'POST-MIGRATION STEPS'
\echo '======================================================================'
\echo ''
\echo '1. Deploy application with token hashing changes'
\echo '2. Notify users to rotate device tokens via:'
\echo '   POST /api/v1/devices/{deviceId}/rotate-token'
\echo '3. Users will receive new tokens (displayed once)'
\echo '4. Update device firmware/clients with new tokens'
\echo '5. Monitor logs for authentication failures'
\echo ''
\echo 'To rotate a device token:'
\echo '  curl -X POST http://localhost:8080/api/v1/devices/{deviceId}/rotate-token \'
\echo '    -H "Authorization: Bearer YOUR_JWT_TOKEN"'
\echo ''

\echo ''
\echo '======================================================================'
\echo 'VERIFICATION QUERIES'
\echo '======================================================================'
\echo ''
\echo 'Check migration status:'
\echo '  SELECT * FROM token_migration_log ORDER BY migration_date DESC;'
\echo ''
\echo 'Check devices needing token rotation:'
\echo '  SELECT external_id, name FROM devices WHERE api_token IS NULL;'
\echo ''
\echo 'Check BCrypt hash format (after rotation):'
\echo '  SELECT external_id, LEFT(api_token, 10) as hash_prefix'
\echo '  FROM devices WHERE api_token IS NOT NULL;'
\echo '  (Should start with "$2a$" or "$2b$" for BCrypt)'
\echo ''
