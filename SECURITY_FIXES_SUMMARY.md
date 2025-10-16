# Security Fixes Implementation Summary

This document summarizes the security vulnerabilities that were fixed and the changes made to address them.

## Fixed Issues

### 1. High - Data Export Endpoints Cross-Tenant Leak ✅

**Issue**: Data export endpoints (CSV/JSON) queried TelemetryRecordRepository directly without verifying the caller's organization, allowing any authenticated user to pull another tenant's device data by ID.

**Location**: `src/main/java/org/sensorvision/controller/DataExportController.java:42-96`

**Fix**:
- Routed all export queries through `TelemetryService` which includes organization checks
- Updated CSV export endpoint (line 50) to use `telemetryService.queryTelemetry()`
- Updated JSON export endpoint (line 87) to use `telemetryService.queryTelemetry()`
- Excel export already had proper organization checks via `ExcelExportService`

**Impact**: All export endpoints now verify organization ownership before returning telemetry data.

---

### 2. High - Notification APIs JWT/OAuth2 Authentication Crash ✅

**Issue**: Notification controller expected `@AuthenticationPrincipal User` but the security stack populated with `Jwt`/`UserPrincipal`, causing ClassCastException.

**Location**: `src/main/java/org/sensorvision/controller/NotificationController.java:38-123`

**Fix**:
- Removed `@AuthenticationPrincipal User` parameter from all endpoints
- Updated all methods to use `SecurityUtils.getCurrentUser()` instead
- `SecurityUtils` already handles both Jwt and UserPrincipal authentication types correctly
- Affected methods:
  - `getPreferences()` (line 39)
  - `savePreference()` (line 56)
  - `deletePreference()` (line 83)
  - `getNotificationLogs()` (line 98)
  - `getNotificationStats()` (line 118)

**Impact**: Notification APIs now work correctly with both JWT and OAuth2 authentication.

---

### 3. High - Webhook SSRF Protection ✅

**Issue**: User-provided webhook URLs were posted with raw RestTemplate lacking allowlists, egress restrictions, or configured timeout.

**Location**: `src/main/java/org/sensorvision/service/WebhookNotificationService.java:22-161`

**Fix**:
- Implemented URL validation method `validateWebhookUrl()` with:
  - Protocol allowlist (HTTPS only)
  - Host blocklist for private/internal IPs (127.0.0.1, 10.x.x.x, 172.16-31.x.x, 192.168.x.x, metadata.google.internal)
  - Proper URI parsing and validation
- Configured RestTemplate with proper connect and read timeouts (from application.yml)
- Added validation calls in both `sendAlertWebhook()` and `testWebhook()` methods
- Enhanced error handling for validation failures

**Impact**: Webhooks now reject internal/private IPs and enforce HTTPS, preventing SSRF attacks.

---

### 4. High - MQTT Ingestion Device Authentication ✅

**Issue**: Unknown MQTT device IDs automatically created devices in "Default Organization", allowing anyone who can reach the broker to write arbitrary telemetry.

**Location**:
- `src/main/java/org/sensorvision/service/TelemetryIngestionService.java:62-110`
- `src/main/java/org/sensorvision/mqtt/TelemetryMessageHandler.java`

**Fix**:
- Modified `TelemetryIngestionService.ingest()` to accept `allowAutoProvision` parameter
- Added device token authentication in `TelemetryMessageHandler`:
  - Extracts `apiToken` from MQTT payload
  - Validates token via `DeviceService.authenticateDeviceByToken()`
  - Verifies device ID matches authenticated device
  - Rejects telemetry if authentication fails
- Added configuration: `mqtt.device-auth.required` (default: true)
- HTTP endpoints still allow auto-provisioning since they're authenticated at user level
- Added proper logging for authentication failures

**Impact**: MQTT ingestion now requires valid device API tokens by default. Auto-provisioning disabled unless explicitly enabled.

---

### 5. Medium - Device API Token Hashing ✅

**Issue**: Device API tokens stored and returned in plaintext, regenerated with raw UUIDs, logged, and returned to callers.

**Location**:
- `src/main/java/org/sensorvision/model/Device.java:65-73`
- `src/main/java/org/sensorvision/service/DeviceService.java:72-214`

**Fix**:
- Updated `generateApiToken()` to:
  - Generate secure random token (UUID without hyphens)
  - Hash token using BCrypt (via PasswordEncoder)
  - Store only hashed version in database
  - Return raw token for one-time display
- Updated `authenticateDeviceByToken()` to:
  - Query all devices with non-null tokens
  - Use BCrypt to verify raw token against hashed versions
  - Update last used timestamp on successful match
- Updated `rotateDeviceToken()` to return raw token for display
- Updated logging to avoid exposing token values
- Added `findAllByApiTokenIsNotNull()` repository method

**Note**: This implementation queries all devices for authentication. For large-scale deployments, consider:
- Token prefix indexing
- Separate token table with indexed lookups
- Caching layer for frequently used tokens

**Impact**: Device API tokens now stored securely using BCrypt hashing. Tokens only displayed once during creation/rotation.

---

### 6. Medium - Sensitive Configuration Defaults ✅

**Issue**: JWT signing key, database, and MQTT credentials shipped with usable fallback values in source control.

**Location**: `src/main/resources/application.yml:8-10,42,63-64,85-88`

**Fix**:
- Removed hardcoded defaults for sensitive values:
  - `DB_USERNAME`: now requires environment variable (was: sensorvision)
  - `DB_PASSWORD`: now requires environment variable (was: sensorvision123)
  - `JWT_SECRET`: now requires environment variable (was: hardcoded hex value)
  - `MQTT_USERNAME`: now requires environment variable (was: sensorvision)
  - `MQTT_PASSWORD`: now requires environment variable (was: sensorvision123)
- Added security configuration validation:
  - Created `SecurityConfigValidation.java`
  - Validates required environment variables at startup
  - Logs clear error messages for missing configuration
  - Skips validation for test profile
- Added configuration for MQTT device authentication requirement
- Added security comments documenting requirements

**Impact**: Application now fails safely if critical security configuration is missing. Production deployments must explicitly set credentials.

---

## Configuration Requirements

### Required Environment Variables (Production)

```bash
# Database
export DB_USERNAME="your_db_user"
export DB_PASSWORD="your_secure_db_password"

# JWT Authentication
export JWT_SECRET="your_long_secure_random_string_minimum_32_chars"

# MQTT Broker
export MQTT_USERNAME="your_mqtt_user"
export MQTT_PASSWORD="your_secure_mqtt_password"

# Optional
export MQTT_DEVICE_AUTH_REQUIRED=true  # Enable device token authentication (default: true)
export JWT_EXPIRATION_MS=86400000      # 24 hours
export JWT_ISSUER="https://your-domain.com"
```

### Development Setup

For development, you can:
1. Set environment variables in your IDE
2. Create a `.env` file (ensure it's in .gitignore)
3. Use docker-compose environment configuration

---

## Testing Recommendations

### 1. Data Export Security
```bash
# Test cross-tenant isolation
curl -H "Authorization: Bearer $TOKEN_ORG1" \
  http://localhost:8080/api/v1/export/csv/device-from-org2?from=...&to=...
# Should return 403 Access Denied
```

### 2. Notification API
```bash
# Test with JWT token
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/v1/notifications/preferences
# Should work without ClassCastException
```

### 3. Webhook SSRF Protection
```bash
# Test blocked hosts
curl -X POST http://localhost:8080/api/v1/webhooks/test \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:8080/admin"}'
# Should reject with validation error

curl -X POST http://localhost:8080/api/v1/webhooks/test \
  -H "Content-Type: application/json" \
  -d '{"url": "http://192.168.1.1/api"}'
# Should reject internal IP

# Test HTTPS requirement
curl -X POST http://localhost:8080/api/v1/webhooks/test \
  -H "Content-Type: application/json" \
  -d '{"url": "http://external-site.com/webhook"}'
# Should reject non-HTTPS

# Valid webhook
curl -X POST http://localhost:8080/api/v1/webhooks/test \
  -H "Content-Type: application/json" \
  -d '{"url": "https://external-site.com/webhook"}'
# Should succeed
```

### 4. MQTT Device Authentication
```bash
# Publish without token (should be rejected)
mosquitto_pub -h localhost -p 1883 \
  -t "sensorvision/devices/test-001/telemetry" \
  -m '{"deviceId":"test-001","variables":{"kw_consumption":50.5}}'

# Publish with valid token (should succeed)
mosquitto_pub -h localhost -p 1883 \
  -t "sensorvision/devices/test-001/telemetry" \
  -m '{"deviceId":"test-001","apiToken":"VALID_TOKEN_HERE","variables":{"kw_consumption":50.5}}'
```

### 5. Device Token Security
```bash
# Create device and receive token
TOKEN=$(curl -X POST http://localhost:8080/api/v1/devices \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"externalId":"test-device","name":"Test Device"}' \
  | jq -r '.apiToken')

# Verify token is returned only once (subsequent calls should not expose it)
curl http://localhost:8080/api/v1/devices/test-device \
  -H "Authorization: Bearer $JWT"
# Response should NOT contain the raw token

# Test authentication with token
curl -X POST http://localhost:8080/api/v1/data/ingest/test-device \
  -H "X-Device-Token: $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"variables":{"temperature":25.5}}'
# Should succeed with valid token
```

---

## Security Best Practices Implemented

1. **Principle of Least Privilege**: All operations verify organization/device ownership
2. **Defense in Depth**: Multiple layers of validation (auth, organization, device tokens)
3. **Secure by Default**: Security features enabled by default (MQTT auth required)
4. **No Secrets in Code**: All sensitive values must come from environment
5. **Password Hashing**: API tokens hashed using industry-standard BCrypt
6. **Input Validation**: URLs validated before external requests
7. **SSRF Protection**: Private IP ranges blocked, HTTPS enforced
8. **Audit Logging**: Security events logged for monitoring
9. **Fail Securely**: Missing configuration prevents startup rather than using insecure defaults

---

## Migration Notes

### For Existing Deployments

1. **Database Credentials**: Set `DB_USERNAME` and `DB_PASSWORD` environment variables
2. **JWT Secret**: Generate a strong random secret (32+ chars) and set `JWT_SECRET`
3. **MQTT Credentials**: Set `MQTT_USERNAME` and `MQTT_PASSWORD`
4. **Device Tokens**: Existing plaintext tokens in database will not work after upgrade
   - Users must rotate all device tokens via API
   - Tokens will be automatically hashed on rotation
   - Consider a migration script to hash existing tokens if needed
5. **MQTT Authentication**: Devices must include `apiToken` in MQTT payloads
   - Update device firmware/clients to include token
   - Or temporarily set `MQTT_DEVICE_AUTH_REQUIRED=false` during transition

### Token Migration Script Recommendation

Create a one-time migration to hash existing tokens:
```sql
-- Backup existing tokens first!
-- UPDATE devices SET api_token = bcrypt_hash(api_token) WHERE api_token IS NOT NULL;
```

Or force token rotation:
```sql
-- Invalidate all tokens, requiring users to rotate them
UPDATE devices SET api_token = NULL WHERE api_token IS NOT NULL;
```

---

## Open Questions / Assumptions Addressed

1. **Are MQTT endpoints exposed beyond trusted networks?**
   - Answer: Authentication now mandatory by default regardless of network
   - Can be disabled via config for fully trusted internal networks

2. **Do you rely on notification APIs today?**
   - Answer: Fixed to work with both JWT and OAuth2 authentication
   - No breaking changes for API consumers

---

## Files Modified

### Controllers
- `DataExportController.java` - Added organization checks via TelemetryService
- `NotificationController.java` - Fixed authentication principal handling
- `TelemetryController.java` - Updated ingest method signature
- `DataIngestionController.java` - Updated ingest method signature

### Services
- `TelemetryIngestionService.java` - Added auto-provision flag, device authentication
- `WebhookNotificationService.java` - Added SSRF protection with URL validation
- `DeviceService.java` - Implemented token hashing with BCrypt
- `ExcelExportService.java` - Already had proper checks (no changes needed)
- `DataImportService.java` - Updated ingest method calls

### MQTT/Messaging
- `TelemetryMessageHandler.java` - Added device token authentication

### Configuration
- `application.yml` - Removed sensitive defaults, added security comments
- `SecurityConfigValidation.java` - NEW: Validates security config at startup

### Repositories
- `DeviceRepository.java` - Added `findAllByApiTokenIsNotNull()` method

---

## Summary

All high and medium security vulnerabilities have been addressed:
- ✅ Cross-tenant data leakage prevented
- ✅ Authentication compatibility issues resolved
- ✅ SSRF attacks mitigated
- ✅ MQTT ingestion secured with device tokens
- ✅ API tokens hashed at rest
- ✅ Sensitive defaults removed from source

The application now enforces multi-tenant isolation, requires proper authentication for all operations, protects against common web vulnerabilities, and fails securely when misconfigured.
