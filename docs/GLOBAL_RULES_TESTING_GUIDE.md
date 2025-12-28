# Global Rules Testing Guide

This guide will help you test the Global Events: Fleet-Wide Rules & Monitoring feature (Issue #81).

## Prerequisites

1. **Backend Running**: Spring Boot application on port 8080
2. **Frontend Running**: React dev server on port 3001
3. **Database**: PostgreSQL with migrations applied
4. **MQTT Broker**: Mosquitto running on port 1883
5. **Test Devices**: At least 3-5 devices with telemetry data

## Setup Test Environment

### 1. Ensure Services Are Running

```bash
# Check if PostgreSQL is running
docker-compose ps

# Backend should be on port 8080
# Frontend should be on port 3001
# Mosquitto should be on port 1883
```

### 2. Create Test Devices (if needed)

```bash
# Publish test telemetry to create devices
mosquitto_pub -h localhost -p 1883 -t "indcloud/devices/fleet-test-001/telemetry" -m '{
  "deviceId": "fleet-test-001",
  "timestamp": "2025-01-11T20:00:00Z",
  "variables": {
    "kw_consumption": 45.5,
    "voltage": 220.1,
    "current": 0.57
  }
}'

mosquitto_pub -h localhost -p 1883 -t "indcloud/devices/fleet-test-002/telemetry" -m '{
  "deviceId": "fleet-test-002",
  "timestamp": "2025-01-11T20:00:00Z",
  "variables": {
    "kw_consumption": 65.3,
    "voltage": 219.8,
    "current": 0.72
  }
}'

mosquitto_pub -h localhost -p 1883 -t "indcloud/devices/fleet-test-003/telemetry" -m '{
  "deviceId": "fleet-test-003",
  "timestamp": "2025-01-11T20:00:00Z",
  "variables": {
    "kw_consumption": 52.1,
    "voltage": 221.3,
    "current": 0.63
  }
}'
```

## Test Scenarios

### Test 1: Access Global Rules Page

**Steps:**
1. Open browser to `http://localhost:3001`
2. Login with your credentials
3. Navigate to **MONITORING** section in sidebar
4. Click on **Global Rules** (with Network icon)

**Expected Result:**
- Page loads successfully
- Shows empty state if no rules exist
- "Create Fleet Rule" button visible

---

### Test 2: Create a Simple Fleet Rule (Count Online Devices)

**Scenario:** Alert when less than 3 devices are online

**Steps:**
1. Click "Create Fleet Rule" button
2. Fill in the form:
   - **Rule Name**: "Low Online Device Count"
   - **Description**: "Alert when less than 3 devices are online"
   - **Target Devices**: "All Devices in Organization"
   - **Aggregation Function**: "Count Online Devices"
   - **Operator**: "Less Than (<)"
   - **Threshold**: 3
   - **Evaluation Interval**: "Every 5 Minutes"
   - **Cooldown**: 15 minutes
   - **Enable**: Checked
3. Click "Create Rule"

**Expected Result:**
- Rule appears in the list
- Shows enabled status
- Last evaluated timestamp appears after first evaluation (wait 5 minutes)

**How to Trigger:**
- Stop 2 of your test devices (they'll go offline after a few minutes)
- Wait for next evaluation cycle
- Check Global Alerts page for alert

---

### Test 3: Create Average Power Consumption Rule

**Scenario:** Alert when average fleet power consumption exceeds threshold

**Steps:**
1. Click "Create Fleet Rule"
2. Fill in:
   - **Rule Name**: "High Average Power Consumption"
   - **Description**: "Fleet average power exceeds 60 kW"
   - **Target Devices**: "All Devices in Organization"
   - **Aggregation Function**: "Average"
   - **Variable to Aggregate**: "kwConsumption"
   - **Operator**: "Greater Than (>)"
   - **Threshold**: 60
   - **Evaluation Interval**: "Every 5 Minutes"
   - **Cooldown**: 10 minutes
   - **Send SMS**: Checked (optional)
   - **SMS Recipients**: Add your test phone number
   - **Enable**: Checked
3. Click "Create Rule"

**Expected Result:**
- Rule created successfully
- SMS recipients shown if configured

**How to Trigger:**
```bash
# Publish high consumption values to trigger the alert
mosquitto_pub -h localhost -p 1883 -t "indcloud/devices/fleet-test-001/telemetry" -m '{
  "deviceId": "fleet-test-001",
  "timestamp": "2025-01-11T20:05:00Z",
  "variables": {
    "kw_consumption": 75.0,
    "voltage": 220.0,
    "current": 0.80
  }
}'

mosquitto_pub -h localhost -p 1883 -t "indcloud/devices/fleet-test-002/telemetry" -m '{
  "deviceId": "fleet-test-002",
  "timestamp": "2025-01-11T20:05:00Z",
  "variables": {
    "kw_consumption": 80.0,
    "voltage": 220.0,
    "current": 0.85
  }
}'

mosquitto_pub -h localhost -p 1883 -t "indcloud/devices/fleet-test-003/telemetry" -m '{
  "deviceId": "fleet-test-003",
  "timestamp": "2025-01-11T20:05:00Z",
  "variables": {
    "kw_consumption": 70.0,
    "voltage": 220.0,
    "current": 0.75
  }
}'
```

Wait 5 minutes for evaluation, then check Global Alerts.

---

### Test 4: Tag-Based Fleet Rule

**Scenario:** Monitor only devices with a specific tag

**Pre-requisite:** Tag some devices first
1. Go to Devices page
2. Add tag "production" to 2-3 devices

**Steps:**
1. Create new rule:
   - **Rule Name**: "Production Fleet High Voltage"
   - **Target Devices**: "Devices with Tag"
   - **Tag Name**: "production"
   - **Aggregation Function**: "Maximum"
   - **Variable**: "voltage"
   - **Operator**: "Greater Than (>)"
   - **Threshold**: 225
   - **Enable**: Checked

**How to Test:**
- Publish high voltage to a tagged device
- Verify alert only considers tagged devices

---

### Test 5: Manual Rule Evaluation

**Steps:**
1. Go to Global Rules page
2. Find a rule
3. Click the "Play" button (manual evaluate)
4. Check browser console for response
5. Navigate to Global Alerts to see if alert was generated

**Expected Result:**
- Manual evaluation executes immediately
- Alert created if condition met
- Toast notification shows success

---

### Test 6: Toggle Rule Enable/Disable

**Steps:**
1. Find an enabled rule
2. Click the toggle button
3. Verify status changes to disabled
4. Toggle again to re-enable

**Expected Result:**
- Status updates immediately
- Disabled rules don't evaluate automatically
- Can re-enable anytime

---

### Test 7: Edit Existing Rule

**Steps:**
1. Click Edit button on a rule
2. Modify threshold value (e.g., from 60 to 50)
3. Click "Update Rule"

**Expected Result:**
- Modal shows current values pre-filled
- Updates save successfully
- New threshold applies on next evaluation

---

### Test 8: Delete Rule

**Steps:**
1. Click Delete button on a rule
2. Confirm deletion

**Expected Result:**
- Rule removed from list
- Associated alerts remain (historical record)

---

### Test 9: View Global Alerts

**Steps:**
1. Navigate to **Monitoring > Alerts** menu
2. Look for fleet-wide alerts (they'll have globalRule reference)
3. Click on an alert to see details

**Expected Result:**
- Shows triggered value
- Shows device count affected
- Shows which rule triggered it

---

### Test 10: Acknowledge and Resolve Alerts

**Steps:**
1. Find an unacknowledged alert
2. Click "Acknowledge" button
3. Find an unresolved alert
4. Click "Resolve" button
5. Add optional resolution note

**Expected Result:**
- Acknowledgment timestamp recorded
- Resolution timestamp recorded
- Status badges update

---

### Test 11: Statistical Functions

Test advanced aggregation functions:

**95th Percentile Test:**
```
- Create rule with "95th Percentile" function
- Variable: voltage
- Send varied voltage values across fleet
- Verify 95th percentile calculation is correct
```

**Standard Deviation Test:**
```
- Create rule with "Standard Deviation"
- Send varied kW consumption values
- Alert when fleet has high variance (unstable consumption)
```

---

## API Testing with cURL

### Get All Global Rules
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/global-rules
```

### Create Global Rule
```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Fleet Rule",
    "description": "API Test",
    "selectorType": "ORGANIZATION",
    "aggregationFunction": "COUNT_ONLINE",
    "operator": "LT",
    "threshold": 5,
    "enabled": true,
    "evaluationInterval": "EVERY_5_MINUTES",
    "cooldownMinutes": 15,
    "sendSms": false
  }' \
  http://localhost:8080/api/v1/global-rules
```

### Manually Evaluate Rule
```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/global-rules/{ruleId}/evaluate
```

### Get Global Alerts
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/global-alerts
```

### Get Alert Statistics
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/global-alerts/stats
```

---

## Backend Logs to Monitor

Watch for these log messages:

```bash
# Rule evaluation
tail -f logs/application.log | grep "GlobalRuleEvaluatorService"

# Alert generation
tail -f logs/application.log | grep "Global alert triggered"

# SMS notifications
tail -f logs/application.log | grep "Sending SMS notification"
```

---

## Common Issues and Troubleshooting

### Issue: Rules not evaluating
**Solution:**
- Check that rule is enabled
- Verify evaluation interval has passed
- Check backend logs for scheduler execution

### Issue: Alerts not appearing
**Solution:**
- Verify condition is actually met (check calculated values in logs)
- Check cooldown hasn't prevented alert
- Ensure devices have recent telemetry data

### Issue: SMS not sending
**Solution:**
- Check SMS service is configured in `application.properties`
- Verify phone numbers in E.164 format
- Check Twilio credentials if using SMS

### Issue: Wrong device count
**Solution:**
- Verify device selector (tag, group, or organization)
- Check that devices are actually online/active
- Review device filter logic

---

## Success Criteria

✅ All rules CRUD operations work
✅ Manual evaluation triggers correctly
✅ Automatic evaluation runs on schedule
✅ Alerts are created when conditions met
✅ Cooldown prevents alert spam
✅ SMS notifications send (if configured)
✅ Multiple aggregation functions work
✅ Device selectors filter correctly
✅ UI updates in real-time
✅ All edge cases handled gracefully

---

## Performance Testing

### Load Test
1. Create 10+ global rules
2. Have 50+ devices sending telemetry
3. Monitor evaluation performance
4. Check database query times

### Expected Performance:
- Rule evaluation: < 1 second per rule
- Alert creation: < 500ms
- Page load: < 2 seconds

---

## Next Steps After Testing

1. Document any bugs found
2. Test with production-like data volumes
3. Verify SMS integration with real phone numbers
4. Test multi-organization isolation
5. Performance tune if needed

---

## Questions?

If you encounter issues:
1. Check backend logs: `./logs/application.log`
2. Check frontend console (F12 in browser)
3. Verify database migrations ran: `SELECT * FROM flyway_schema_history;`
4. Check MQTT broker: `docker-compose logs mosquitto`
