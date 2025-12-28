# SMS Alert Notifications - Testing Guide

Complete guide for testing the SMS alert notification system in Industrial Cloud.

---

## Prerequisites

### Development Environment
```bash
# Ensure services are running
docker-compose up -d

# Start backend
./gradlew bootRun

# Start frontend
cd frontend && npm run dev
```

### Test Twilio Account Setup
1. Sign up at https://www.twilio.com/try-twilio (free trial)
2. Get a free trial phone number
3. Note your Account SID and Auth Token
4. Add to `.env`:
```bash
SMS_ENABLED=true
SMS_FROM_NUMBER=+15551234567  # Your Twilio trial number
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token_here
```

---

## Unit Testing

### Run SMS-related Tests
```bash
# All SMS tests
./gradlew test --tests "*SmsNotificationServiceTest" --tests "*PhoneNumberVerificationServiceTest"

# Specific test class
./gradlew test --tests "SmsNotificationServiceTest"

# Specific test method
./gradlew test --tests "SmsNotificationServiceTest.sendSms_whenDailyLimitExceeded_logsFailure"

# With detailed output
./gradlew test --tests "*Sms*" --info
```

### Expected Results
- **SmsNotificationServiceTest**: 11 tests passing
- **PhoneNumberVerificationServiceTest**: 18 tests passing
- **NotificationServiceTest**: Updated for SMS integration
- **Total**: 29+ SMS-related tests

---

## API Testing with curl

### 1. Phone Number Management

#### Add Phone Number
```bash
curl -X POST http://localhost:8080/api/v1/phone-numbers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "phoneNumber": "+15551234567",
    "countryCode": "US"
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "data": {
    "id": "uuid-here",
    "phoneNumber": "+1****567",
    "countryCode": "US",
    "verified": false,
    "isPrimary": true,
    "enabled": true,
    "createdAt": "2025-11-11T..."
  },
  "message": "Phone number added. Verification code sent via SMS."
}
```

**Check SMS**: You should receive an SMS with 6-digit code.

#### Verify Phone Number
```bash
curl -X POST http://localhost:8080/api/v1/phone-numbers/{phoneId}/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "code": "123456"
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "data": "verified",
  "message": "Phone number verified successfully"
}
```

#### List Phone Numbers
```bash
curl -X GET http://localhost:8080/api/v1/phone-numbers \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Set Primary Phone
```bash
curl -X PUT http://localhost:8080/api/v1/phone-numbers/{phoneId}/set-primary \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Toggle Phone Enabled/Disabled
```bash
curl -X PUT http://localhost:8080/api/v1/phone-numbers/{phoneId}/toggle \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Delete Phone Number
```bash
curl -X DELETE http://localhost:8080/api/v1/phone-numbers/{phoneId} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 2. SMS Settings (Admin Only)

#### Get SMS Settings
```bash
curl -X GET http://localhost:8080/api/v1/sms-settings \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

**Expected Response**:
```json
{
  "id": "uuid-here",
  "enabled": false,
  "dailyLimit": 100,
  "monthlyBudget": 50.00,
  "currentMonthCount": 0,
  "currentMonthCost": 0.00,
  "currentDayCount": 0,
  "lastResetDate": null,
  "alertOnBudgetThreshold": true,
  "budgetThresholdPercentage": 80,
  "createdAt": "2025-11-11T...",
  "updatedAt": "2025-11-11T..."
}
```

#### Update SMS Settings
```bash
curl -X PUT http://localhost:8080/api/v1/sms-settings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "enabled": true,
    "dailyLimit": 50,
    "monthlyBudget": 25.00,
    "alertOnBudgetThreshold": true,
    "budgetThresholdPercentage": 80
  }'
```

#### Reset Monthly Counters
```bash
curl -X POST http://localhost:8080/api/v1/sms-settings/reset-monthly-counters \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

---

## Integration Testing

### Test End-to-End SMS Alert Flow

#### 1. Setup
```bash
# Enable SMS for organization
curl -X PUT http://localhost:8080/api/v1/sms-settings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "enabled": true,
    "dailyLimit": 100,
    "monthlyBudget": 50.00
  }'

# Add and verify phone number
curl -X POST http://localhost:8080/api/v1/phone-numbers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer USER_JWT_TOKEN" \
  -d '{
    "phoneNumber": "+15551234567",
    "countryCode": "US"
  }'

# Verify with code received via SMS
curl -X POST http://localhost:8080/api/v1/phone-numbers/{phoneId}/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer USER_JWT_TOKEN" \
  -d '{"code": "123456"}'
```

#### 2. Create Rule with SMS Enabled
```bash
curl -X POST http://localhost:8080/api/v1/rules \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer USER_JWT_TOKEN" \
  -d '{
    "name": "High Temperature Alert",
    "deviceId": "device-uuid-here",
    "variable": "temperature",
    "operator": "GT",
    "threshold": 80.0,
    "severity": "HIGH",
    "enabled": true,
    "sendEmail": true,
    "sendSms": true,
    "smsRecipients": ["+15551234567"]
  }'
```

#### 3. Trigger Alert via MQTT
```bash
mosquitto_pub -h localhost -p 1883 \
  -t "indcloud/devices/test-device/telemetry" \
  -m '{
    "deviceId": "test-device",
    "timestamp": "2025-11-11T12:00:00Z",
    "variables": {
      "temperature": 85.5
    }
  }'
```

#### 4. Verify SMS Delivery
- Check your phone for SMS
- Verify message format: `[HIGH] Device Name: Rule Name - Alert message`
- Check database:
```sql
SELECT * FROM sms_delivery_log ORDER BY sent_at DESC LIMIT 10;
```

#### 5. Check Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus | grep sms_
```

Expected metrics:
```
sms_sent_total 1.0
sms_failed_total 0.0
sms_daily_count{organization_id="1"} 1.0
sms_monthly_count{organization_id="1"} 1.0
sms_monthly_cost{organization_id="1"} 0.0075
sms_budget_utilization_percent{organization_id="1"} 0.015
```

---

## Budget Control Testing

### Test Daily Limit
```bash
# Set low daily limit
curl -X PUT http://localhost:8080/api/v1/sms-settings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "enabled": true,
    "dailyLimit": 3,
    "monthlyBudget": 50.00
  }'

# Trigger 4 alerts (4th should fail)
for i in {1..4}; do
  mosquitto_pub -h localhost -p 1883 \
    -t "indcloud/devices/test-device/telemetry" \
    -m "{\"deviceId\": \"test-device\", \"timestamp\": \"2025-11-11T12:0$i:00Z\", \"variables\": {\"temperature\": 85.5}}"
  sleep 2
done

# Check logs for DAILY_LIMIT_EXCEEDED
docker-compose logs -f backend | grep "Daily SMS limit reached"

# Verify in database
SELECT status, error_code, COUNT(*)
FROM sms_delivery_log
WHERE sent_at > NOW() - INTERVAL '1 hour'
GROUP BY status, error_code;
```

### Test Monthly Budget
```bash
# Set low monthly budget
curl -X PUT http://localhost:8080/api/v1/sms-settings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "enabled": true,
    "dailyLimit": 100,
    "monthlyBudget": 0.02
  }'

# Trigger 3 alerts (3rd should fail, 0.0075 * 3 = 0.0225 > 0.02)
for i in {1..3}; do
  mosquitto_pub -h localhost -p 1883 \
    -t "indcloud/devices/test-device/telemetry" \
    -m "{\"deviceId\": \"test-device\", \"timestamp\": \"2025-11-11T12:1$i:00Z\", \"variables\": {\"temperature\": 85.5}}"
  sleep 2
done

# Check logs for BUDGET_EXCEEDED
docker-compose logs -f backend | grep "Monthly SMS budget exceeded"
```

### Test Budget Threshold Alert
```bash
# Set budget to $0.10, threshold at 80%
curl -X PUT http://localhost:8080/api/v1/sms-settings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "enabled": true,
    "dailyLimit": 100,
    "monthlyBudget": 0.10,
    "alertOnBudgetThreshold": true,
    "budgetThresholdPercentage": 80
  }'

# Trigger 11 alerts (80% of $0.10 = $0.08, at 11 SMS = $0.0825)
for i in {1..11}; do
  mosquitto_pub -h localhost -p 1883 \
    -t "indcloud/devices/test-device/telemetry" \
    -m "{\"deviceId\": \"test-device\", \"timestamp\": \"2025-11-11T12:2$i:00Z\", \"variables\": {\"temperature\": 85.5}}"
  sleep 2
done

# Check admin email for budget threshold alert
# Check logs for budget alert
docker-compose logs -f backend | grep "reached.*% of SMS budget"
```

---

## Phone Verification Testing

### Test Valid OTP
```bash
# Add phone number
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/phone-numbers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"phoneNumber": "+15551234567", "countryCode": "US"}')

PHONE_ID=$(echo $RESPONSE | jq -r '.data.id')

# Check SMS for code, then verify
curl -X POST http://localhost:8080/api/v1/phone-numbers/$PHONE_ID/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"code": "123456"}'  # Replace with actual code
```

### Test Invalid OTP
```bash
curl -X POST http://localhost:8080/api/v1/phone-numbers/$PHONE_ID/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"code": "000000"}'

# Expected: 400 Bad Request with error message
```

### Test Expired OTP
```bash
# Add phone number
curl -X POST http://localhost:8080/api/v1/phone-numbers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"phoneNumber": "+15551234567", "countryCode": "US"}'

# Wait 11 minutes (OTP expires after 10 minutes)
sleep 660

# Try to verify with received code
curl -X POST http://localhost:8080/api/v1/phone-numbers/$PHONE_ID/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"code": "123456"}'

# Expected: 400 Bad Request - "Invalid or expired verification code"
```

### Test Resend Verification Code
```bash
curl -X POST http://localhost:8080/api/v1/phone-numbers/$PHONE_ID/resend-code \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected: New SMS with new 6-digit code
```

---

## Security Testing

### Test User Ownership Validation
```bash
# User A adds phone number
curl -X POST http://localhost:8080/api/v1/phone-numbers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer USER_A_TOKEN" \
  -d '{"phoneNumber": "+15551111111", "countryCode": "US"}'

# User B tries to toggle User A's phone (should fail)
curl -X PUT http://localhost:8080/api/v1/phone-numbers/{USER_A_PHONE_ID}/toggle \
  -H "Authorization: Bearer USER_B_TOKEN"

# Expected: 400 Bad Request - "Phone number not found"
```

### Test Admin-Only Endpoints
```bash
# Non-admin tries to update SMS settings (should fail)
curl -X PUT http://localhost:8080/api/v1/sms-settings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer NON_ADMIN_TOKEN" \
  -d '{"enabled": true, "dailyLimit": 100, "monthlyBudget": 50.00}'

# Expected: 403 Forbidden
```

### Test Primary Phone Removal
```bash
# Add 2 phone numbers for user
curl -X POST http://localhost:8080/api/v1/phone-numbers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"phoneNumber": "+15551111111", "countryCode": "US"}'

curl -X POST http://localhost:8080/api/v1/phone-numbers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"phoneNumber": "+15552222222", "countryCode": "US"}'

# Try to delete primary phone (should fail)
curl -X DELETE http://localhost:8080/api/v1/phone-numbers/{PRIMARY_PHONE_ID} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected: 400 Bad Request - "Cannot remove primary phone number. Set another phone as primary first."
```

---

## Database Testing

### Verify Data Integrity
```sql
-- Check user phone numbers
SELECT
  upn.id,
  u.username,
  upn.phone_number,
  upn.country_code,
  upn.verified,
  upn.is_primary,
  upn.enabled,
  upn.created_at
FROM user_phone_numbers upn
JOIN users u ON upn.user_id = u.id
ORDER BY upn.created_at DESC;

-- Check SMS delivery logs
SELECT
  sdl.id,
  sdl.phone_number,
  sdl.status,
  sdl.error_code,
  sdl.cost,
  sdl.sent_at,
  sdl.twilio_sid,
  a.message as alert_message
FROM sms_delivery_log sdl
LEFT JOIN alerts a ON sdl.alert_id = a.id
ORDER BY sdl.sent_at DESC
LIMIT 20;

-- Check organization SMS settings
SELECT
  oss.id,
  o.name as organization_name,
  oss.enabled,
  oss.daily_limit,
  oss.monthly_budget,
  oss.current_day_count,
  oss.current_month_count,
  oss.current_month_cost,
  oss.last_reset_date,
  oss.budget_threshold_percentage
FROM organization_sms_settings oss
JOIN organizations o ON oss.organization_id = o.id;

-- Check rules with SMS enabled
SELECT
  r.id,
  r.name,
  d.name as device_name,
  r.send_sms,
  r.sms_recipients,
  r.enabled
FROM rules r
JOIN devices d ON r.device_id = d.id
WHERE r.send_sms = true;

-- Calculate SMS costs by organization
SELECT
  o.name as organization,
  COUNT(sdl.id) as total_sms,
  COUNT(CASE WHEN sdl.status = 'FAILED' THEN 1 END) as failed_sms,
  SUM(sdl.cost) as total_cost
FROM sms_delivery_log sdl
JOIN alerts a ON sdl.alert_id = a.id
JOIN devices d ON a.device_id = d.id
JOIN organizations o ON d.organization_id = o.id
WHERE sdl.sent_at > NOW() - INTERVAL '30 days'
GROUP BY o.name;
```

---

## Performance Testing

### Load Test: Burst Alerts
```bash
# Trigger 100 alerts simultaneously
for i in {1..100}; do
  (mosquitto_pub -h localhost -p 1883 \
    -t "indcloud/devices/test-device-$i/telemetry" \
    -m "{\"deviceId\": \"test-device-$i\", \"timestamp\": \"2025-11-11T12:30:00Z\", \"variables\": {\"temperature\": 85.5}}" &)
done

# Monitor logs for processing time
docker-compose logs -f backend | grep "SMS sent"

# Check for any failures
SELECT status, COUNT(*)
FROM sms_delivery_log
WHERE sent_at > NOW() - INTERVAL '5 minutes'
GROUP BY status;
```

### Database Query Performance
```sql
-- Test index performance on phone number lookup
EXPLAIN ANALYZE
SELECT * FROM user_phone_numbers
WHERE user_id = 1 AND is_primary = true;

-- Test index performance on SMS delivery log by date
EXPLAIN ANALYZE
SELECT * FROM sms_delivery_log
WHERE sent_at BETWEEN '2025-11-01' AND '2025-11-30'
ORDER BY sent_at DESC;

-- Test SMS count aggregation performance
EXPLAIN ANALYZE
SELECT phone_number, COUNT(*)
FROM sms_delivery_log
WHERE sent_at > NOW() - INTERVAL '1 hour'
GROUP BY phone_number;
```

---

## Monitoring Testing

### Test Prometheus Metrics Endpoint
```bash
# Check all SMS metrics
curl http://localhost:8080/actuator/prometheus | grep ^sms_

# Expected output:
# sms_sent_total 10.0
# sms_failed_total 2.0
# sms_daily_count{organization_id="1",} 5.0
# sms_monthly_count{organization_id="1",} 12.0
# sms_monthly_cost{organization_id="1",} 0.09
# sms_budget_utilization_percent{organization_id="1",} 90.0
```

### Grafana Dashboard Setup
1. Add Prometheus data source (http://prometheus:9090)
2. Create dashboard with panels:
   - SMS Sent (rate): `rate(sms_sent_total[5m])`
   - SMS Failed (rate): `rate(sms_failed_total[5m])`
   - Daily Count by Org: `sms_daily_count`
   - Monthly Cost by Org: `sms_monthly_cost`
   - Budget Utilization: `sms_budget_utilization_percent`
3. Set alerts:
   - `sms_budget_utilization_percent > 90`
   - `rate(sms_failed_total[5m]) > 0.1`

---

## Troubleshooting

### SMS Not Sending
1. Check SMS globally enabled:
```bash
docker-compose logs backend | grep "SMS notifications enabled"
```

2. Check organization SMS enabled:
```sql
SELECT enabled FROM organization_sms_settings WHERE organization_id = 1;
```

3. Check Twilio credentials:
```bash
docker-compose logs backend | grep "Twilio"
```

4. Check daily/monthly limits:
```sql
SELECT daily_limit, current_day_count, monthly_budget, current_month_cost
FROM organization_sms_settings
WHERE organization_id = 1;
```

### Phone Verification Not Working
1. Check Twilio credentials in .env
2. Check SMS globally enabled
3. Verify phone number format (must be E.164: +15551234567)
4. Check verification code expiration (10 minutes)
5. Check backend logs for Twilio errors:
```bash
docker-compose logs backend | grep "verification SMS"
```

### Budget Alerts Not Sending
1. Check email notifications enabled:
```bash
docker-compose logs backend | grep "Email notifications enabled"
```

2. Check admin email configured:
```bash
echo $ADMIN_EMAIL
```

3. Check budget threshold settings:
```sql
SELECT alert_on_budget_threshold, budget_threshold_percentage
FROM organization_sms_settings
WHERE organization_id = 1;
```

4. Verify threshold crossed:
```sql
SELECT
  current_month_cost,
  monthly_budget,
  (current_month_cost / monthly_budget * 100) as utilization_percent,
  budget_threshold_percentage
FROM organization_sms_settings
WHERE organization_id = 1;
```

---

## Test Checklist

### Manual Testing Checklist
- [ ] Add phone number (receive SMS)
- [ ] Verify phone with valid OTP
- [ ] Verify phone with invalid OTP (fail)
- [ ] Verify phone with expired OTP (fail)
- [ ] Resend verification code
- [ ] Set phone as primary
- [ ] Toggle phone enabled/disabled
- [ ] Remove non-primary phone
- [ ] Remove primary phone with others (fail)
- [ ] Create rule with SMS enabled
- [ ] Trigger alert (receive SMS)
- [ ] Check SMS message format
- [ ] Verify Twilio SID in database
- [ ] Test daily limit enforcement
- [ ] Test monthly budget enforcement
- [ ] Test budget threshold email alert
- [ ] Check Prometheus metrics
- [ ] Verify admin-only endpoint security
- [ ] Test user ownership validation
- [ ] Test multiple recipients

### Automated Testing Checklist
- [ ] All unit tests passing (29+ tests)
- [ ] SmsNotificationServiceTest (11 tests)
- [ ] PhoneNumberVerificationServiceTest (18 tests)
- [ ] NotificationServiceTest updated
- [ ] Database migrations applied
- [ ] Database constraints enforced
- [ ] Indexes created

---

## Next Steps After Testing

1. **Fix any issues found** during testing
2. **Document workarounds** for known limitations
3. **Update configuration** based on test results
4. **Create test data** for staging environment
5. **Prepare production deployment** plan
6. **Set up monitoring dashboards** in Grafana
7. **Configure alerts** for budget/failures
8. **Train admin users** on SMS settings management

---

**Last Updated**: 2025-11-11
**Related Docs**:
- Implementation Report: `docs/SMS_ALERTS_IMPLEMENTATION_REPORT.md`
- Architecture: `docs/SMS_ALERTS_ARCHITECTURE.md`
- AWS SES Setup: `docs/AWS_SES_SETUP.md`
