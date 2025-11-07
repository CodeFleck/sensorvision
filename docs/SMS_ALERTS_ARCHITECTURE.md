# SMS Alerts Architecture

Design document for SMS notification system using Twilio integration.

## Overview

Add SMS as a notification channel for critical alerts, providing:
- **Immediate delivery** (faster than email checking)
- **High visibility** (text messages have 98% open rate vs 20% for email)
- **Redundancy** (SMS + Email for critical alerts)
- **After-hours coverage** (on-call engineer notifications)

---

## Use Cases

1. **Critical Equipment Failures**
   - Temperature > 90°C → Send SMS immediately
   - Voltage drops below 180V → Send SMS to on-call engineer

2. **Production Line Downtime**
   - Conveyor belt stopped > 5 minutes → SMS to operations manager

3. **Security Alerts**
   - Unauthorized access detected → SMS to security team

4. **Cascade Failure Detection**
   - More than 10% of devices offline → SMS to DevOps team

---

## Architecture

### Components

```
┌─────────────────┐
│  RuleEngine     │
│  Service        │
└────────┬────────┘
         │ Alert Triggered
         ▼
┌─────────────────┐
│ AlertService    │  ← Existing
└────────┬────────┘
         │ Route to channels
         ├──────────┬──────────┬──────────┐
         ▼          ▼          ▼          ▼
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│Email       │ │SMS         │ │Webhook     │ │Slack       │
│Notifier    │ │Notifier    │ │Notifier    │ │Notifier    │
└────────────┘ └────────────┘ └────────────┘ └────────────┘
                     │
                     ▼
              ┌─────────────┐
              │Twilio API   │
              └─────────────┘
```

### Data Flow

1. **Alert Triggered** → RuleEngineService evaluates rule
2. **Alert Created** → AlertService creates alert record
3. **Channel Routing** → Determine which channels to use (Email, SMS, Webhook)
4. **SMS Delivery** → SmsNotificationService calls Twilio API
5. **Status Tracking** → Store delivery status in database

---

## Database Schema

### New Table: `user_phone_numbers`

Store user phone numbers for SMS delivery:

```sql
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
```

### Update Table: `rules`

Add SMS notification toggle:

```sql
ALTER TABLE rules ADD COLUMN send_sms BOOLEAN DEFAULT FALSE;
ALTER TABLE rules ADD COLUMN sms_recipients TEXT[];  -- Array of phone numbers or "primary"
```

### New Table: `sms_delivery_log`

Track SMS delivery status:

```sql
CREATE TABLE sms_delivery_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id UUID REFERENCES alerts(id),
    phone_number VARCHAR(20) NOT NULL,
    message_body TEXT NOT NULL,
    twilio_sid VARCHAR(50),  -- Twilio message SID
    status VARCHAR(20) NOT NULL,  -- 'queued', 'sent', 'delivered', 'failed', 'undelivered'
    error_code VARCHAR(10),
    error_message TEXT,
    cost NUMERIC(10, 4),  -- Track SMS costs
    sent_at TIMESTAMPTZ DEFAULT NOW(),
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_sms_delivery_log_alert_id ON sms_delivery_log(alert_id);
CREATE INDEX idx_sms_delivery_log_status ON sms_delivery_log(status);
CREATE INDEX idx_sms_delivery_log_sent_at ON sms_delivery_log(sent_at DESC);
```

### New Table: `organization_sms_settings`

Organization-level SMS configuration:

```sql
CREATE TABLE organization_sms_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    enabled BOOLEAN DEFAULT FALSE,
    daily_limit INTEGER DEFAULT 100,  -- Max SMS per day
    monthly_budget NUMERIC(10, 2) DEFAULT 50.00,  -- Max spend per month
    current_month_count INTEGER DEFAULT 0,
    current_month_cost NUMERIC(10, 2) DEFAULT 0.00,
    alert_on_budget_threshold BOOLEAN DEFAULT TRUE,
    budget_threshold_percentage INTEGER DEFAULT 80,  -- Alert at 80% of budget
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(organization_id)
);
```

---

## Backend Implementation

### 1. SmsNotificationService

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsNotificationService {

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.from}")
    private String fromNumber;

    @Value("${notification.sms.twilio.account-sid}")
    private String accountSid;

    @Value("${notification.sms.twilio.auth-token}")
    private String authToken;

    private final SmsDeliveryLogRepository smsDeliveryLogRepository;
    private final OrganizationSmsSettingsRepository smsSettingsRepository;

    @PostConstruct
    public void init() {
        if (smsEnabled) {
            Twilio.init(accountSid, authToken);
            log.info("SMS notifications enabled via Twilio");
        } else {
            log.info("SMS notifications disabled");
        }
    }

    /**
     * Send SMS notification
     */
    public SmsDeliveryLog sendSms(Alert alert, String phoneNumber, String message) {
        if (!smsEnabled) {
            log.warn("SMS notifications disabled, skipping SMS for alert {}", alert.getId());
            return null;
        }

        // Check organization SMS settings
        OrganizationSmsSettings settings = smsSettingsRepository
            .findByOrganizationId(alert.getRule().getDevice().getOrganization().getId())
            .orElse(null);

        if (settings == null || !settings.getEnabled()) {
            log.warn("SMS not enabled for organization {}", alert.getRule().getDevice().getOrganization().getId());
            return null;
        }

        // Check daily limit
        if (settings.getCurrentMonthCount() >= settings.getDailyLimit()) {
            log.warn("Daily SMS limit reached for organization {}", alert.getRule().getDevice().getOrganization().getId());
            return createFailedLog(alert, phoneNumber, message, "DAILY_LIMIT_EXCEEDED");
        }

        // Check monthly budget
        if (settings.getCurrentMonthCost().compareTo(settings.getMonthlyBudget()) >= 0) {
            log.warn("Monthly SMS budget exceeded for organization {}", alert.getRule().getDevice().getOrganization().getId());
            return createFailedLog(alert, phoneNumber, message, "BUDGET_EXCEEDED");
        }

        try {
            // Send SMS via Twilio
            Message twilioMessage = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(fromNumber),
                message
            ).create();

            // Log delivery
            SmsDeliveryLog deliveryLog = SmsDeliveryLog.builder()
                .alert(alert)
                .phoneNumber(phoneNumber)
                .messageBody(message)
                .twilioSid(twilioMessage.getSid())
                .status(twilioMessage.getStatus().name())
                .cost(new BigDecimal("0.0075"))  // Twilio US domestic SMS cost
                .sentAt(Instant.now())
                .build();

            deliveryLog = smsDeliveryLogRepository.save(deliveryLog);

            // Update organization SMS stats
            updateOrganizationStats(settings, new BigDecimal("0.0075"));

            log.info("SMS sent to {} for alert {}: SID={}", phoneNumber, alert.getId(), twilioMessage.getSid());
            return deliveryLog;

        } catch (TwilioException e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return createFailedLog(alert, phoneNumber, message, e.getMessage());
        }
    }

    /**
     * Send SMS to multiple recipients
     */
    public List<SmsDeliveryLog> sendSmsToMultiple(Alert alert, List<String> phoneNumbers, String message) {
        return phoneNumbers.stream()
            .map(phone -> sendSms(alert, phone, message))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Format alert message for SMS (160 char limit)
     */
    public String formatAlertMessage(Alert alert) {
        // SMS is limited to 160 characters for single segment
        // Longer messages are split into multiple segments (cost multiplier)
        String deviceName = alert.getRule().getDevice().getName();
        String ruleName = alert.getRule().getName();
        String severity = alert.getSeverity().name();

        return String.format(
            "[%s] %s: %s - %s",
            severity,
            deviceName,
            ruleName,
            alert.getMessage().length() > 80 ? alert.getMessage().substring(0, 77) + "..." : alert.getMessage()
        );
    }

    private SmsDeliveryLog createFailedLog(Alert alert, String phoneNumber, String message, String errorMessage) {
        SmsDeliveryLog deliveryLog = SmsDeliveryLog.builder()
            .alert(alert)
            .phoneNumber(phoneNumber)
            .messageBody(message)
            .status("FAILED")
            .errorMessage(errorMessage)
            .sentAt(Instant.now())
            .build();
        return smsDeliveryLogRepository.save(deliveryLog);
    }

    private void updateOrganizationStats(OrganizationSmsSettings settings, BigDecimal cost) {
        settings.setCurrentMonthCount(settings.getCurrentMonthCount() + 1);
        settings.setCurrentMonthCost(settings.getCurrentMonthCost().add(cost));

        // Alert if approaching budget
        if (settings.getAlertOnBudgetThreshold()) {
            BigDecimal threshold = settings.getMonthlyBudget()
                .multiply(new BigDecimal(settings.getBudgetThresholdPercentage()))
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

            if (settings.getCurrentMonthCost().compareTo(threshold) >= 0) {
                log.warn("Organization {} has reached {}% of SMS budget",
                    settings.getOrganizationId(), settings.getBudgetThresholdPercentage());
                // TODO: Send admin notification
            }
        }

        smsSettingsRepository.save(settings);
    }
}
```

### 2. Integration with AlertService

Update `AlertService` to route to SMS channel:

```java
@Service
@RequiredArgsConstructor
public class AlertService {

    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;  // NEW
    private final UserPhoneNumberRepository phoneNumberRepository;  // NEW

    public void sendAlert(Alert alert) {
        Rule rule = alert.getRule();

        // Send email if enabled
        if (rule.getSendEmail()) {
            emailService.sendAlertEmail(alert);
        }

        // Send SMS if enabled (NEW)
        if (rule.getSendSms()) {
            List<String> phoneNumbers = resolvePhoneNumbers(alert, rule);
            String message = smsService.formatAlertMessage(alert);
            smsService.sendSmsToMultiple(alert, phoneNumbers, message);
        }

        // Send webhook if configured
        if (rule.getWebhookUrl() != null) {
            webhookService.sendWebhook(alert, rule.getWebhookUrl());
        }
    }

    private List<String> resolvePhoneNumbers(Alert alert, Rule rule) {
        // Get phone numbers based on rule configuration
        if (rule.getSmsRecipients() != null && !rule.getSmsRecipients().isEmpty()) {
            // Specific recipients configured
            return Arrays.asList(rule.getSmsRecipients());
        } else {
            // Default: Send to device owner's primary phone
            return phoneNumberRepository.findByUserIdAndIsPrimaryTrue(
                alert.getRule().getDevice().getOwner().getId()
            ).map(phone -> phone.getPhoneNumber())
            .map(List::of)
            .orElse(Collections.emptyList());
        }
    }
}
```

---

## Frontend Implementation

### 1. Phone Number Management UI

**Component**: `frontend/src/pages/settings/PhoneNumbers.tsx`

Features:
- Add/remove phone numbers
- Set primary phone number
- Verify phone numbers (OTP via SMS)
- Enable/disable SMS notifications

### 2. Rule Configuration Updates

**Component**: `frontend/src/components/rules/RuleForm.tsx`

Add SMS configuration section:
- Toggle: "Send SMS notification"
- Dropdown: "SMS Recipients" (Primary phone, All phones, Custom list)
- Preview: SMS message format

### 3. SMS Delivery Dashboard

**Component**: `frontend/src/pages/admin/SmsDeliveryDashboard.tsx`

Metrics:
- SMS sent today/this month
- Delivery success rate
- Failed deliveries with error codes
- Current month cost
- Budget remaining

---

## Configuration

### application.properties

```properties
# SMS Notifications (Twilio)
notification.sms.enabled=${SMS_ENABLED:false}
notification.sms.from=${SMS_FROM_NUMBER:}
notification.sms.twilio.account-sid=${TWILIO_ACCOUNT_SID:}
notification.sms.twilio.auth-token=${TWILIO_AUTH_TOKEN:}
```

### Environment Variables

```bash
SMS_ENABLED=true
SMS_FROM_NUMBER=+15551234567  # Your Twilio phone number
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
```

---

## Cost Management

### Pricing (Twilio US)
- **SMS**: $0.0075 per message
- **Verification SMS**: $0.05 per attempt

### Budget Controls
1. **Daily Limit**: Max SMS per day per organization
2. **Monthly Budget**: Dollar limit per month
3. **Alert Thresholds**: Warn admins at 80% budget
4. **Auto-disable**: Stop SMS when budget exceeded

### Cost Dashboard
Show admins:
- Current month spend
- SMS count
- Average cost per alert
- Projected month-end cost

---

## Security & Privacy

### Phone Number Verification
1. User adds phone number
2. System sends 6-digit OTP via SMS
3. User enters OTP within 10 minutes
4. Phone number marked as verified

### Data Protection
- Phone numbers encrypted at rest
- PII compliance (GDPR, CCPA)
- User can delete phone numbers anytime
- SMS content excludes sensitive data

### Rate Limiting
- Max 5 SMS per phone number per hour
- Max 3 verification attempts per phone per day
- Prevent SMS bombing attacks

---

## Testing Strategy

### Unit Tests
- `SmsNotificationServiceTest` - Test Twilio integration (mocked)
- `PhoneNumberValidationTest` - Test E.164 format validation
- `BudgetControlsTest` - Test daily/monthly limits

### Integration Tests
- End-to-end alert → SMS flow
- Budget enforcement
- Phone verification flow

### Manual Testing
1. Add phone number
2. Verify with OTP
3. Create alert rule with SMS enabled
4. Trigger alert
5. Verify SMS received

---

## Deployment Checklist

- [ ] Sign up for Twilio account
- [ ] Purchase Twilio phone number
- [ ] Add Twilio credentials to production `.env`
- [ ] Run database migrations
- [ ] Test SMS in staging environment
- [ ] Set organization SMS budgets
- [ ] Enable SMS for production

---

## Future Enhancements

1. **SMS Templates** - Customizable message formats
2. **AWS SNS Alternative** - Use SNS instead of Twilio for AWS users
3. **International SMS** - Support non-US numbers
4. **SMS Campaigns** - Scheduled bulk SMS (for maintenance notifications)
5. **Two-Way SMS** - Respond to alerts via SMS (ACK, SNOOZE)
6. **MMS Support** - Send charts/graphs via MMS

---

## Related Documentation

- Twilio SMS API: https://www.twilio.com/docs/sms
- Phone Number Formats (E.164): https://en.wikipedia.org/wiki/E.164
- Issue #88: SMS Alert Notifications
- Issue #66: AWS SES Email Fix

---

**Last Updated**: 2025-11-06
**Status**: Design Document
**Implementation Target**: Q1 2025 (1 week effort)
