# SMS Alert Notifications - Next Steps & Deployment Guide

**Date**: 2025-11-11
**Branch**: `feature/sms-alerts-testing-completion`
**Status**: ✅ Implementation Complete | ⏭️ Ready for Testing & Deployment

---

## Current Status

### ✅ Completed
- **Backend**: Fully implemented with 29+ passing unit tests
  - SMS notification service with Twilio integration
  - Phone number management with OTP verification
  - Budget controls and cost tracking
  - Prometheus metrics
  - Database migrations (V46, V47)

- **Frontend**: Fully implemented with successful build
  - Phone number management UI
  - SMS settings dashboard (admin)
  - Rule form SMS integration
  - Navigation and routing
  - TypeScript type safety

### ⚠️ Pending
- Manual end-to-end testing
- Twilio account configuration
- AWS SES configuration
- Production deployment

---

## Immediate Next Steps

### Step 1: Code Review & Testing Preparation (Today)

#### 1.1 Review Implementation
```bash
# Review all changes
git diff main..feature/sms-alerts-testing-completion

# Review specific components
git show HEAD  # Latest frontend commit
git show HEAD~1  # Backend commit
```

#### 1.2 Clean Up Branch
There are untracked WIP files from Issues #81 and #82 that prevent compilation:
- GlobalRuleController.java
- GlobalAlertController.java
- PluginMarketplaceController.java
- FleetAggregatorService.java
- GlobalRuleEvaluatorService.java
- GlobalRuleService.java
- GlobalAlertService.java

**Options**:
1. **Stash for later**: `git stash -u` (stash untracked files)
2. **Move to separate branch**: Create `feature/global-rules-wip` and commit WIP
3. **Delete if not needed**: Remove and recreate from scratch later

**Recommended**:
```bash
# Create WIP branch for Global Rules/Plugins work
git checkout -b feature/global-rules-wip
git add src/main/java/org/sensorvision/controller/Global*
git add src/main/java/org/sensorvision/controller/PluginMarketplace*
git add src/main/java/org/sensorvision/service/Global*
git add src/main/java/org/sensorvision/service/FleetAggregator*
git add src/main/java/org/sensorvision/model/Global*
git add src/main/java/org/sensorvision/model/*Plugin*
git add src/main/java/org/sensorvision/dto/Global*
git add src/main/java/org/sensorvision/dto/*Plugin*
git add src/main/java/org/sensorvision/repository/Global*
git add src/main/java/org/sensorvision/repository/*Plugin*
git commit -m "WIP: Global Rules and Plugin Marketplace (incomplete)"

# Return to SMS branch
git checkout feature/sms-alerts-testing-completion
```

#### 1.3 Verify Build
```bash
# Backend build
./gradlew clean build

# Frontend build
cd frontend && npm run build
```

### Step 2: Set Up Twilio Test Account (1-2 hours)

#### 2.1 Sign Up for Twilio
1. Go to https://www.twilio.com/try-twilio
2. Sign up for free trial account
3. Verify your email
4. Complete phone verification

#### 2.2 Get Twilio Credentials
1. Navigate to Console Dashboard
2. Copy **Account SID** (starts with AC...)
3. Copy **Auth Token** (click to reveal)
4. Go to Phone Numbers → Buy a Number
5. Select a number (free trial provides 1 number)
6. Copy the phone number (E.164 format: +15551234567)

#### 2.3 Configure Development Environment
Create/update `.env.local`:
```bash
# SMS Notifications (Twilio)
SMS_ENABLED=true
SMS_FROM_NUMBER=+15551234567  # Your Twilio number
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token_here
SMS_COST_PER_MESSAGE=0.0075
ADMIN_EMAIL=your-email@example.com

# Email (for budget threshold alerts)
EMAIL_ENABLED=true
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
EMAIL_FROM=noreply@sensorvision.com
```

**Note**: For Gmail, use an [App Password](https://support.google.com/accounts/answer/185833), not your regular password.

### Step 3: Manual Testing (2-4 hours)

#### 3.1 Start Services
```bash
# Terminal 1: Start PostgreSQL
docker-compose up -d postgres

# Terminal 2: Start backend
./gradlew bootRun

# Terminal 3: Start frontend
cd frontend && npm run dev
```

#### 3.2 Testing Checklist

##### Phone Number Management
- [ ] Navigate to http://localhost:3001/phone-numbers
- [ ] Click "Add Phone Number"
- [ ] Enter your real phone number (to receive SMS)
- [ ] Verify SMS received with 6-digit code
- [ ] Enter code and verify
- [ ] Check phone shows "Verified" badge
- [ ] Test "Resend Code" button
- [ ] Add second phone number
- [ ] Set second phone as primary
- [ ] Toggle phone enabled/disabled
- [ ] Try to delete primary phone (should fail)
- [ ] Set first phone as primary
- [ ] Delete second phone (should succeed)

##### Rule SMS Configuration
- [ ] Navigate to Rules page
- [ ] Create new rule with SMS enabled
- [ ] Select your verified phone as recipient
- [ ] Add multiple recipients (if you have multiple phones)
- [ ] Save rule
- [ ] Trigger rule manually (send telemetry that exceeds threshold)
- [ ] Verify SMS received on phone
- [ ] Check SMS format and content
- [ ] Edit rule to disable SMS
- [ ] Save and verify SMS no longer sent

##### SMS Settings (Admin)
- [ ] Login as admin user
- [ ] Navigate to http://localhost:3001/sms-settings
- [ ] Verify current usage displays correctly
- [ ] Enable SMS globally
- [ ] Set daily limit to 10
- [ ] Set monthly budget to $5.00
- [ ] Set threshold to 80%
- [ ] Click "Save Settings"
- [ ] Trigger 9 alerts (should succeed)
- [ ] Trigger 10th alert (should fail - daily limit)
- [ ] Check logs for "DAILY_LIMIT_EXCEEDED"
- [ ] Reset daily counter (via API or wait 24h)
- [ ] Verify budget utilization calculation
- [ ] Test "Reset Monthly Counters" button

##### Database Verification
```sql
-- Check phone numbers
SELECT * FROM user_phone_numbers;

-- Check SMS delivery logs
SELECT * FROM sms_delivery_log ORDER BY sent_at DESC LIMIT 10;

-- Check organization SMS settings
SELECT * FROM organization_sms_settings;

-- Check rules with SMS enabled
SELECT id, name, send_sms, sms_recipients FROM rules WHERE send_sms = true;
```

##### API Testing
```bash
# Get phone numbers
curl -H "Authorization: Bearer YOUR_JWT" http://localhost:8080/api/v1/phone-numbers

# Get SMS settings (admin)
curl -H "Authorization: Bearer ADMIN_JWT" http://localhost:8080/api/v1/sms-settings

# Check Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep sms_
```

### Step 4: Bug Fixes & Refinements (As needed)

Document any issues found during testing:
- Create GitHub issues for bugs
- Fix critical bugs before deployment
- Document workarounds for minor issues

### Step 5: Prepare for Production Deployment

#### 5.1 Production Twilio Setup
1. **Upgrade Account** (if needed):
   - Trial accounts have limitations (verified numbers only)
   - Consider upgrading for unrestricted SMS

2. **Purchase Production Number**:
   - Buy dedicated number for production
   - Consider toll-free number for better deliverability

3. **Configure Production Credentials**:
   ```bash
   # .env.production
   SMS_ENABLED=true
   SMS_FROM_NUMBER=+1555XXXXXXX
   TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxx
   TWILIO_AUTH_TOKEN=production_token
   ```

#### 5.2 AWS SES Setup (for Budget Alerts)
Follow `docs/AWS_SES_SETUP.md`:
1. Create AWS account
2. Verify email/domain in SES
3. Request production access (exit sandbox)
4. Generate SMTP credentials
5. Update production environment

#### 5.3 Database Migrations
Ensure migrations applied:
```bash
# Check migration status
./gradlew flywayInfo

# Apply pending migrations
./gradlew flywayMigrate
```

Required migrations:
- V46__Add_sms_notifications.sql
- V47__Add_daily_sms_counter.sql

#### 5.4 Environment Variables Checklist
```bash
# Backend (.env.production)
SMS_ENABLED=true
SMS_FROM_NUMBER=+1555XXXXXXX
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxx
SMS_COST_PER_MESSAGE=0.0075
ADMIN_EMAIL=admin@yourcompany.com

# Email (AWS SES)
EMAIL_ENABLED=true
SMTP_HOST=email-smtp.us-west-2.amazonaws.com
SMTP_PORT=587
SMTP_USERNAME=AKIAxxxxxxxxxxxxx
SMTP_PASSWORD=your_ses_smtp_password
EMAIL_FROM=noreply@yourcompany.com

# Database
DATABASE_URL=jdbc:postgresql://prod-db:5432/sensorvision
DATABASE_USERNAME=sensorvision_user
DATABASE_PASSWORD=secure_password
```

### Step 6: Staging Deployment

#### 6.1 Deploy to Staging
```bash
# Build production artifacts
./gradlew clean build
cd frontend && npm run build

# Deploy backend JAR
scp build/libs/sensorvision-*.jar staging-server:/opt/sensorvision/

# Deploy frontend build
rsync -avz frontend/dist/ staging-server:/var/www/sensorvision/

# Restart services
ssh staging-server "systemctl restart sensorvision"
```

#### 6.2 Staging Tests
- [ ] All phone number operations work
- [ ] SMS delivery successful
- [ ] Budget tracking accurate
- [ ] Admin settings persist
- [ ] Rules trigger SMS correctly
- [ ] Email alerts sent at threshold
- [ ] Metrics visible in Prometheus/Grafana

### Step 7: Production Deployment

#### 7.1 Pre-Deployment Checklist
- [ ] All staging tests passed
- [ ] Database backup created
- [ ] Environment variables configured
- [ ] Twilio production account ready
- [ ] AWS SES production access granted
- [ ] Monitoring dashboards set up
- [ ] Rollback plan prepared
- [ ] Team notified

#### 7.2 Deployment Steps
```bash
# 1. Create database backup
pg_dump -h prod-db sensorvision > backup_$(date +%Y%m%d).sql

# 2. Apply migrations
./gradlew flywayMigrate -Dspring.profiles.active=production

# 3. Deploy backend
# ... (same as staging)

# 4. Deploy frontend
# ... (same as staging)

# 5. Verify deployment
curl https://your-domain.com/api/v1/health
```

#### 7.3 Post-Deployment Verification
- [ ] Health check passes
- [ ] User can add phone number
- [ ] SMS delivery works
- [ ] Admin can access settings
- [ ] Metrics reporting correctly
- [ ] No errors in logs
- [ ] Email alerts working

#### 7.4 Monitoring Setup
Create Grafana dashboard with:
- SMS sent rate (last 24h)
- SMS failed rate
- Daily count by organization
- Monthly cost tracking
- Budget utilization percentage
- Alert on 90% budget

### Step 8: User Communication

#### 8.1 Documentation
Create user guides:
1. **Phone Number Setup Guide**
   - How to add phone number
   - Verification process
   - Troubleshooting

2. **SMS Alerts Configuration Guide**
   - Enabling SMS on rules
   - Managing recipients
   - Understanding costs

3. **Admin Guide** (for admins)
   - Budget configuration
   - Monitoring usage
   - Managing limits

#### 8.2 Announce Feature
- [ ] Send email to all users
- [ ] Update in-app announcements
- [ ] Update documentation site
- [ ] Post on social media (if applicable)

---

## Troubleshooting Guide

### Issue: SMS Not Sending

**Check**:
1. SMS globally enabled? (admin settings)
2. Organization SMS enabled?
3. Daily limit not exceeded?
4. Monthly budget not exceeded?
5. Twilio credentials correct?
6. Phone number verified?

**Logs**:
```bash
# Backend logs
docker-compose logs -f backend | grep -i sms

# Database check
SELECT * FROM sms_delivery_log WHERE status = 'FAILED' ORDER BY sent_at DESC LIMIT 10;
```

### Issue: Verification Code Not Received

**Check**:
1. Phone number in E.164 format?
2. Twilio trial account verified the number?
3. Check Twilio logs at https://console.twilio.com/logs
4. Check backend logs for errors

**Solutions**:
- Use "Resend Code" button
- Check spam/blocked messages
- Verify Twilio balance
- Try different phone number

### Issue: Budget Threshold Email Not Sent

**Check**:
1. Email enabled in settings?
2. SMTP credentials correct?
3. Admin email configured?
4. Budget threshold actually reached?

**Test Email**:
```bash
# Test SMTP connection
telnet smtp.gmail.com 587
```

### Issue: Daily Limit Resetting Incorrectly

**Check**:
- `lastResetDate` in `organization_sms_settings`
- Server timezone configuration
- Counter reset logic in `SmsNotificationService`

**Manual Reset**:
```sql
UPDATE organization_sms_settings
SET current_day_count = 0,
    last_reset_date = NOW()
WHERE organization_id = 1;
```

---

## Cost Management

### Twilio Pricing (US)
- Standard SMS: $0.0075 per message
- Verification SMS: $0.05 per message
- Toll-free SMS: $0.0125 per message

### Cost Projections
| Alerts/Day | SMS/Month | Cost/Month |
|------------|-----------|------------|
| 10         | 300       | $2.25      |
| 50         | 1,500     | $11.25     |
| 100        | 3,000     | $22.50     |
| 500        | 15,000    | $112.50    |

### Budget Recommendations
1. **Start Conservative**: $25-50/month
2. **Monitor First Month**: Track actual usage
3. **Adjust Based on Data**: Increase if hitting limits
4. **Set Alerts**: 80% threshold recommended
5. **Review Monthly**: Optimize alert frequency

---

## Success Metrics

### Week 1
- [ ] 10+ users add phone numbers
- [ ] 50+ SMS sent successfully
- [ ] 0 critical bugs reported
- [ ] < 1% SMS delivery failure rate

### Month 1
- [ ] 50+ users using SMS alerts
- [ ] 1,000+ SMS delivered
- [ ] Average cost per user < $1.00
- [ ] User satisfaction > 4/5 stars

---

## Future Enhancements

### Planned (Next Sprint)
1. SMS Delivery Dashboard
   - Logs table with filtering
   - Success/failure charts
   - Cost analytics

2. Enhanced Phone Validation
   - libphonenumber-js integration
   - Auto-formatting by country
   - Better error messages

3. User Experience
   - Real-time delivery status
   - SMS preview before sending
   - Character counter

### Backlog
- Two-way SMS (reply to acknowledge)
- SMS templates/customization
- Scheduled SMS
- Bulk SMS campaigns
- MMS support (images/charts)
- Multi-language SMS

---

## Support & Resources

### Documentation
- Backend Implementation: `docs/SMS_ALERTS_IMPLEMENTATION_REPORT.md`
- Backend Architecture: `docs/SMS_ALERTS_ARCHITECTURE.md`
- Backend Testing: `docs/SMS_ALERTS_TESTING_GUIDE.md`
- Frontend Implementation: `docs/SMS_FRONTEND_IMPLEMENTATION.md`
- AWS SES Setup: `docs/AWS_SES_SETUP.md`

### External Resources
- Twilio Docs: https://www.twilio.com/docs/sms
- Twilio Console: https://console.twilio.com
- AWS SES Docs: https://docs.aws.amazon.com/ses/

### Contact
- GitHub Issues: https://github.com/CodeFleck/sensorvision/issues/88
- Email: admin@sensorvision.com

---

## Conclusion

The SMS Alert Notifications feature is **100% complete** and ready for testing and deployment. Follow this guide step-by-step to:

1. ✅ Clean up WIP files
2. ✅ Set up Twilio test account
3. ✅ Perform comprehensive manual testing
4. ✅ Fix any issues found
5. ✅ Deploy to staging
6. ✅ Deploy to production
7. ✅ Monitor and optimize

**Estimated Timeline**:
- Testing: 1-2 days
- Staging deployment: 1 day
- Production deployment: 1 day
- **Total**: 3-4 days to production

Good luck! 🚀

---

**Document Created**: 2025-11-11
**Last Updated**: 2025-11-11
**Version**: 1.0
