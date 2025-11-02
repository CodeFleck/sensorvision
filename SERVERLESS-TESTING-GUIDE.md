# Serverless Functions Engine - Local Testing Guide

## Environment Status

✅ Backend: Running on port 8080
✅ Frontend: Running on port 3001
✅ Database: PostgreSQL (all migrations applied V1-V43)
✅ MQTT Broker: Running on port 1883
✅ Node.js: v22.19.0 (required: 18+)
✅ Python: 3.14.0 (required: 3.11+)

## Database Tables Created

All serverless function tables successfully created:
- `serverless_functions` - Function definitions
- `function_triggers` - Trigger configurations (HTTP, MQTT, Scheduled, Device Events)
- `function_executions` - Execution history and logs
- `function_secrets` - Encrypted credentials (AES-256-GCM)
- `function_execution_quotas` - Rate limiting (60/min, 1000/hour, 10k/day, 100k/month)
- `function_execution_metrics` - Aggregated performance metrics

---

## Testing Scenarios

### 1. Test Python Runtime (Basic)

**Create a simple Python function:**

```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "hello-python",
    "description": "Simple Python function",
    "runtime": "PYTHON_3_11",
    "code": "def handler(event, context):\n    name = event.get(\"name\", \"World\")\n    return {\"message\": f\"Hello, {name}!\"}\n",
    "handlerMethod": "handler",
    "enabled": true,
    "memoryLimitMb": 256,
    "timeoutSeconds": 30
  }'
```

**Execute the function:**

```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions/{functionId}/execute \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"name": "SensorVision"}'
```

**Expected Response:**
```json
{
  "id": 1,
  "functionId": 1,
  "status": "SUCCESS",
  "output": {"message": "Hello, SensorVision!"},
  "errorMessage": null,
  "durationMs": 150,
  "memoryUsedMb": 25,
  "startedAt": "2025-11-02T15:00:00Z",
  "completedAt": "2025-11-02T15:00:00.150Z"
}
```

---

### 2. Test Node.js Runtime

**Create a Node.js function:**

```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "hello-nodejs",
    "description": "Simple Node.js function",
    "runtime": "NODE_18",
    "code": "exports.handler = async (event, context) => {\n    const name = event.name || \"World\";\n    return { message: `Hello, ${name}!` };\n};",
    "handlerMethod": "handler",
    "enabled": true,
    "memoryLimitMb": 256,
    "timeoutSeconds": 30
  }'
```

**Execute:**
```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions/{functionId}/execute \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"name": "JavaScript"}'
```

---

### 3. Test Secrets Management

**Add a secret to a function:**

```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions/{functionId}/secrets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "secretKey": "API_KEY",
    "secretValue": "sk-test-12345"
  }'
```

**Create a function that uses the secret:**

```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "secret-user",
    "runtime": "PYTHON_3_11",
    "code": "import os\ndef handler(event, context):\n    api_key = os.environ.get(\"API_KEY\", \"not-found\")\n    return {\"api_key_present\": api_key != \"not-found\", \"key_length\": len(api_key)}\n",
    "handlerMethod": "handler",
    "enabled": true
  }'
```

**Execute and verify secret injection:**
```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions/{functionId}/execute \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:** `{"api_key_present": true, "key_length": 14}`

---

### 4. Test MQTT Triggers

**Create an MQTT trigger:**

```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions/{functionId}/triggers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "triggerType": "MQTT",
    "enabled": true,
    "triggerConfig": {
      "topicPattern": "sensorvision/devices/+/telemetry",
      "deviceIdFilter": "test-device-001",
      "variableFilters": {
        "temperature": {
          "operator": "GT",
          "value": 25.0
        }
      },
      "debounceSeconds": 60
    }
  }'
```

**Publish a matching MQTT message:**

```bash
mosquitto_pub -h localhost -p 1883 \
  -t "sensorvision/devices/test-device-001/telemetry" \
  -m '{
    "deviceId": "test-device-001",
    "timestamp": "2025-11-02T15:00:00Z",
    "variables": {
      "temperature": 30.5,
      "humidity": 65.2
    }
  }'
```

**Verify execution in database:**
```bash
docker exec -i sensorvision-postgres psql -U sensorvision -d sensorvision \
  -c "SELECT id, status, duration_ms, trigger FROM function_executions ORDER BY started_at DESC LIMIT 5;"
```

**Expected:** New execution record with `trigger = 'MQTT'`

---

### 5. Test Scheduled Triggers (Cron)

**Create a scheduled trigger:**

```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions/{functionId}/triggers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "triggerType": "SCHEDULED",
    "enabled": true,
    "triggerConfig": {
      "cronExpression": "0 * * * * *",
      "timezone": "America/New_York"
    }
  }'
```

**What this does:**
- Executes the function at the start of every minute
- Uses New York timezone for scheduling

**Verify execution:**
Wait 1-2 minutes, then check execution logs:
```bash
curl -X GET "http://localhost:8080/api/v1/serverless/functions/{functionId}/executions?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:** Multiple executions with `trigger = 'SCHEDULED'`, spaced 1 minute apart

---

### 6. Test Device Event Triggers

**Create a device event trigger:**

```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions/{functionId}/triggers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "triggerType": "DEVICE_EVENT",
    "enabled": true,
    "triggerConfig": {
      "eventType": "device.created",
      "deviceIdFilter": "sensor-*",
      "tagFilters": ["production", "critical"]
    }
  }'
```

**Trigger by creating a device:**

```bash
curl -X POST http://localhost:8080/api/v1/devices \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "deviceId": "sensor-temp-001",
    "name": "Temperature Sensor 001",
    "description": "Production temperature sensor",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "tags": ["production", "critical"]
  }'
```

**Verify:** Function executes automatically with device creation event data

---

### 7. Test Rate Limiting

**Check current quota status:**

```bash
curl -X GET http://localhost:8080/api/v1/serverless/quota \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "executionsPerMinute": 60,
  "executionsPerHour": 1000,
  "executionsPerDay": 10000,
  "executionsPerMonth": 100000,
  "currentMinuteCount": 5,
  "currentHourCount": 42,
  "currentDayCount": 156,
  "currentMonthCount": 3891,
  "remainingMinute": 55,
  "remainingHour": 958,
  "remainingDay": 9844,
  "remainingMonth": 96109
}
```

**Test rate limit enforcement:**

Execute a function 100 times in quick succession:
```bash
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/v1/serverless/functions/{functionId}/execute \
    -H "Authorization: Bearer YOUR_JWT_TOKEN" \
    -d '{}' &
done
wait
```

**Expected:** First 60 succeed within 1 minute, remaining return:
```json
{
  "error": "Rate limit exceeded. Please try again later."
}
```

---

### 8. Test Execution History

**Get execution history:**

```bash
curl -X GET "http://localhost:8080/api/v1/serverless/functions/{functionId}/executions?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Get detailed execution:**

```bash
curl -X GET http://localhost:8080/api/v1/serverless/executions/{executionId} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected fields:**
- `id`, `functionId`, `status` (SUCCESS/FAILED/TIMEOUT)
- `input`, `output`, `errorMessage`, `errorStack`
- `durationMs`, `memoryUsedMb`
- `trigger` (HTTP/MQTT/SCHEDULED/DEVICE_EVENT)
- `startedAt`, `completedAt`

---

### 9. Test Error Handling

**Create a function that throws an error:**

```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "error-test",
    "runtime": "PYTHON_3_11",
    "code": "def handler(event, context):\n    raise ValueError(\"Intentional test error\")\n",
    "handlerMethod": "handler",
    "enabled": true
  }'
```

**Execute:**
```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions/{functionId}/execute \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "status": "FAILED",
  "errorMessage": "ValueError: Intentional test error",
  "errorStack": "Traceback (most recent call last):\n  File ...",
  "output": null
}
```

---

### 10. Test Timeout Enforcement

**Create a function that exceeds timeout:**

```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "timeout-test",
    "runtime": "PYTHON_3_11",
    "code": "import time\ndef handler(event, context):\n    time.sleep(10)\n    return {\"completed\": True}\n",
    "handlerMethod": "handler",
    "timeoutSeconds": 3,
    "enabled": true
  }'
```

**Execute:**
```bash
curl -X POST http://localhost:8080/api/v1/serverless/functions/{functionId}/execute \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:** Status `TIMEOUT` after ~3 seconds

---

## Frontend UI Testing

### Access the UI

Navigate to: **http://localhost:3001**

**Note:** You'll need to create/update the Serverless Functions UI components in the frontend. These were not part of this implementation phase and should be added separately.

**Recommended UI Flow:**
1. Functions List page (`/serverless/functions`)
2. Create Function modal (select runtime, paste code, configure)
3. Function Details page (view code, execution history, configure triggers)
4. Secrets Manager (add/view/delete secrets with security banner)
5. Triggers Manager (create HTTP/MQTT/Scheduled/Device Event triggers)
6. Execution Logs (view history, filter by status/trigger type)
7. Quota Dashboard (view current usage across all time windows)

---

## Verification Checklist

- [ ] Python function executes successfully
- [ ] Node.js function executes successfully
- [ ] Secrets are encrypted and injected correctly
- [ ] MQTT triggers fire on matching messages
- [ ] Scheduled triggers execute on cron schedule
- [ ] Device event triggers fire on device creation/update
- [ ] Rate limiting enforces quotas (60/min, 1000/hour, 10k/day, 100k/month)
- [ ] Execution history is recorded with all details
- [ ] Errors are caught and logged with stack traces
- [ ] Timeouts are enforced correctly
- [ ] Function enable/disable works
- [ ] Trigger enable/disable works

---

## Useful Database Queries

**View all functions:**
```sql
SELECT id, name, runtime, enabled, created_at
FROM serverless_functions
ORDER BY created_at DESC;
```

**View recent executions:**
```sql
SELECT fe.id, sf.name, fe.status, fe.duration_ms, fe.trigger, fe.started_at
FROM function_executions fe
JOIN serverless_functions sf ON fe.function_id = sf.id
ORDER BY fe.started_at DESC
LIMIT 20;
```

**View quota status:**
```sql
SELECT organization_id,
       current_minute_count || '/' || executions_per_minute as minute_quota,
       current_hour_count || '/' || executions_per_hour as hour_quota,
       current_day_count || '/' || executions_per_day as day_quota,
       current_month_count || '/' || executions_per_month as month_quota
FROM function_execution_quotas;
```

**View active triggers:**
```sql
SELECT ft.id, sf.name as function_name, ft.trigger_type, ft.enabled, ft.trigger_config
FROM function_triggers ft
JOIN serverless_functions sf ON ft.function_id = sf.id
WHERE ft.enabled = true
ORDER BY ft.created_at DESC;
```

---

## Troubleshooting

### Function execution fails with "No executor found"
**Problem:** Runtime not supported
**Solution:** Use `PYTHON_3_11` or `NODE_18` exactly as shown

### MQTT trigger not firing
**Problem:** Topic pattern or filters don't match
**Solution:** Check logs, verify topic pattern uses `+` for single level, `#` for multiple levels

### Rate limit exceeded immediately
**Problem:** Previous tests consumed quota
**Solution:** Wait for quota reset or manually reset in database:
```sql
UPDATE function_execution_quotas
SET current_minute_count = 0,
    current_hour_count = 0,
    minute_reset_at = NOW(),
    hour_reset_at = NOW();
```

### Secrets not injected
**Problem:** Secret key format invalid
**Solution:** Use UPPERCASE_SNAKE_CASE for secret keys (e.g., `API_KEY`, `DATABASE_URL`)

### Scheduled trigger not running
**Problem:** Invalid cron expression
**Solution:** Use 6-field Spring cron format: `second minute hour day month weekday`
Example: `0 */5 * * * *` (every 5 minutes)

---

## Next Steps

After testing locally, consider:

1. **Create Frontend UI** - Build React components for function management
2. **Add Metrics Dashboard** - Visualize execution stats from `function_execution_metrics`
3. **Implement HTTP Triggers** - Add REST API endpoint triggers (not yet implemented)
4. **Add More Examples** - Create function templates for common use cases
5. **Performance Testing** - Load test with 1000+ functions
6. **Production Deployment** - Deploy to AWS/GCP with proper secrets management

---

## Security Notes

⚠️ **Important:** The backend logs show a warning about the encryption key:
```
Generated random encryption key. To persist this key, add to application.properties:
serverless.secrets.encryption-key=LViKp4bI9AxGO7TvSurqIh++ALBlboZgl4eENUKiG6A=
```

**Action Required:** Add this line to `src/main/resources/application-dev.properties` to persist the encryption key across restarts. Otherwise, all encrypted secrets will become inaccessible after backend restart.

---

## API Endpoints Reference

- `POST /api/v1/serverless/functions` - Create function
- `GET /api/v1/serverless/functions` - List functions
- `GET /api/v1/serverless/functions/{id}` - Get function details
- `PUT /api/v1/serverless/functions/{id}` - Update function
- `DELETE /api/v1/serverless/functions/{id}` - Delete function
- `POST /api/v1/serverless/functions/{id}/execute` - Execute function (sync)
- `GET /api/v1/serverless/functions/{id}/executions` - Get execution history
- `GET /api/v1/serverless/executions/{id}` - Get execution details
- `POST /api/v1/serverless/functions/{id}/triggers` - Create trigger
- `GET /api/v1/serverless/functions/{id}/triggers` - List triggers
- `DELETE /api/v1/serverless/triggers/{id}` - Delete trigger
- `POST /api/v1/serverless/functions/{id}/secrets` - Add secret
- `GET /api/v1/serverless/functions/{id}/secrets` - List secrets (keys only, values never returned)
- `DELETE /api/v1/serverless/secrets/{id}` - Delete secret
- `GET /api/v1/serverless/quota` - Get quota status

---

Happy testing! 🚀
