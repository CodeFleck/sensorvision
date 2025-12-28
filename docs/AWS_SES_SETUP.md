# AWS SES Setup Guide for Industrial Cloud

Complete guide to configure AWS Simple Email Service (SES) for production email delivery.

## Overview

AWS SES replaces Gmail SMTP to fix production email issues caused by AWS security group port blocking. SES provides:
- **Enterprise-grade deliverability** with domain verification
- **Cost-effective pricing**: $0.10 per 1,000 emails (first 62,000/month FREE from EC2)
- **Built-in bounce/complaint handling**
- **No SMTP port blocking** (uses AWS internal routing)

---

## Prerequisites

- AWS Account with admin access
- Access to your domain's DNS settings (for domain verification)
- Production EC2 instance running in us-west-2

---

## Step 1: Verify Email Identity in AWS SES

### Option A: Verify Individual Email Address (Quick Start)

If you don't have domain DNS access, verify a single email address:

```bash
# Using AWS CLI
aws ses verify-email-identity \
    --email-address noreply@indcloud.io \
    --region us-west-2
```

**Then**:
1. Check the email inbox for `noreply@indcloud.io`
2. Click the verification link in the email from AWS
3. Status will change to "Verified" in SES console

### Option B: Verify Entire Domain (Recommended for Production)

Verify the entire domain to send from any address:

```bash
# Using AWS CLI
aws ses verify-domain-identity \
    --domain indcloud.io \
    --region us-west-2
```

**Then**:
1. AWS will provide DNS verification records
2. Add the TXT record to your domain's DNS:
   ```
   Name: _amazonses.indcloud.io
   Type: TXT
   Value: [provided by AWS, looks like: ABC123XYZ...]
   ```
3. Wait 5-10 minutes for DNS propagation
4. Verify status in SES console

**Production Recommendation**: Use `nip.io` domain for quick testing, then migrate to custom domain later.

For nip.io (temporary):
```bash
aws ses verify-email-identity \
    --email-address noreply@35.88.65.186.nip.io \
    --region us-west-2
```

---

## Step 2: Request Production Access (Remove Sandbox)

By default, SES starts in **sandbox mode** with restrictions:
- Can only send to verified email addresses
- Max 200 emails per 24 hours
- Max 1 email per second

**Request production access**:

1. Go to AWS SES Console → Account Dashboard
2. Click **"Request production access"**
3. Fill out the form:
   - **Mail Type**: Transactional
   - **Website URL**: http://35.88.65.186.nip.io:8080
   - **Use Case Description**:
     ```
     Industrial Cloud is an IoT monitoring platform that sends transactional emails:
     - Password reset emails
     - Alert notifications when device metrics exceed thresholds
     - Support ticket notifications

     We expect to send approximately 1,000-5,000 emails per month to our users.
     All emails are opt-in (users must create accounts and configure alerts).
     We have proper unsubscribe mechanisms in place.
     ```
   - **Bounce/Complaint Process**:
     ```
     We monitor bounces and complaints via SES dashboard.
     Emails that bounce are logged and users are notified via the application.
     We maintain a suppression list for hard bounces.
     ```

4. Submit request
5. **Approval time**: Usually 24-48 hours

---

## Step 3: Generate SMTP Credentials

SES provides SMTP endpoints that work with Spring Boot's JavaMailSender.

### Generate SMTP Username and Password

1. Go to AWS SES Console → SMTP Settings
2. Click **"Create SMTP Credentials"**
3. Enter IAM User Name: `indcloud-ses-smtp-user`
4. Click **"Create"**
5. **Save the credentials** (they are shown only once):
   ```
   SMTP Username: AKIA...  (20 characters)
   SMTP Password: BPwJ...  (44 characters)
   ```

### SMTP Endpoint
```
Region: us-west-2
SMTP Endpoint: email-smtp.us-west-2.amazonaws.com
Port: 587 (TLS) or 465 (SSL)
```

---

## Step 4: Update Spring Boot Configuration

### Option A: Environment Variables (Recommended for Production)

Add these environment variables to your production `.env` file or docker-compose:

```bash
# AWS SES Configuration
SMTP_HOST=email-smtp.us-west-2.amazonaws.com
SMTP_PORT=587
SMTP_USERNAME=AKIA...  # Your SMTP username from Step 3
SMTP_PASSWORD=BPwJ...  # Your SMTP password from Step 3
EMAIL_FROM=noreply@35.88.65.186.nip.io  # Verified email from Step 1
EMAIL_ENABLED=true
```

### Option B: application-prod.properties (Alternative)

Create `src/main/resources/application-prod.properties`:

```properties
# AWS SES Email Configuration
spring.mail.host=email-smtp.us-west-2.amazonaws.com
spring.mail.port=587
spring.mail.username=${SMTP_USERNAME}
spring.mail.password=${SMTP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Email notifications
notification.email.enabled=true
notification.email.from=noreply@35.88.65.186.nip.io
```

---

## Step 5: Configure SPF, DKIM, and DMARC (Optional but Recommended)

Improve email deliverability by adding DNS records.

### SPF Record
Add this TXT record to your domain:
```
Name: @ (or root domain)
Type: TXT
Value: v=spf1 include:amazonses.com ~all
```

### DKIM Signing
1. Go to SES Console → Verified Identities → Your Domain
2. Click **"Generate DKIM Settings"**
3. AWS provides 3 CNAME records
4. Add all 3 CNAME records to your DNS

### DMARC Record
Add this TXT record:
```
Name: _dmarc
Type: TXT
Value: v=DMARC1; p=none; rua=mailto:postmaster@indcloud.io
```

---

## Step 6: Test Email Sending

### Test via Spring Boot Application

1. Restart application with new SES configuration
2. Go to forgot password page: http://35.88.65.186.nip.io:8080/forgot-password
3. Enter a verified email address (if still in sandbox)
4. Check email inbox for password reset link

### Test via AWS CLI

```bash
# Send test email
aws ses send-email \
    --from noreply@35.88.65.186.nip.io \
    --destination ToAddresses=your-email@example.com \
    --message "Subject={Data='Test Email',Charset='UTF-8'},Body={Text={Data='This is a test email from Industrial Cloud.',Charset='UTF-8'}}" \
    --region us-west-2
```

### Test via Java Code (Manual Test)

```java
// Add to a test controller
@GetMapping("/test-email")
public ResponseEntity<String> testEmail() {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("noreply@35.88.65.186.nip.io");
    message.setTo("your-email@example.com");
    message.setSubject("Industrial Cloud Test Email");
    message.setText("This is a test email from Industrial Cloud using AWS SES.");

    try {
        mailSender.send(message);
        return ResponseEntity.ok("Email sent successfully!");
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
    }
}
```

---

## Step 7: Monitor Email Sending

### SES Dashboard Metrics

Monitor in AWS Console → SES → Reputation Dashboard:
- **Sends**: Total emails sent
- **Bounces**: Emails that couldn't be delivered
- **Complaints**: Users marked email as spam
- **Reputation**: Keep bounce rate < 5%, complaint rate < 0.1%

### Bounce Handling

SES automatically handles bounces:
- **Soft Bounces**: Temporary issues (mailbox full, server down)
- **Hard Bounces**: Permanent failures (invalid email)

**Best Practice**: Set up SNS notifications for bounces:

```bash
# Create SNS topic for bounces
aws sns create-topic --name indcloud-ses-bounces --region us-west-2

# Configure SES to send bounce notifications
aws ses set-identity-notification-topic \
    --identity noreply@35.88.65.186.nip.io \
    --notification-type Bounce \
    --sns-topic arn:aws:sns:us-west-2:ACCOUNT_ID:indcloud-ses-bounces \
    --region us-west-2
```

---

## Troubleshooting

### Issue: "Email address is not verified"

**Cause**: Email is still in sandbox mode or sender email not verified
**Fix**: Verify email address in Step 1, or request production access in Step 2

### Issue: "Daily sending quota exceeded"

**Cause**: Still in sandbox mode (200 emails/day limit)
**Fix**: Request production access in Step 2

### Issue: "SMTP connection timeout"

**Cause**: Security group blocks port 587
**Fix**: Add outbound rule for port 587 to AWS security group:
```bash
aws ec2 authorize-security-group-egress \
    --group-id sg-0255cea554e401228 \
    --protocol tcp \
    --port 587 \
    --cidr 0.0.0.0/0 \
    --region us-west-2
```

### Issue: "Authentication failed"

**Cause**: Wrong SMTP credentials
**Fix**: Double-check SMTP username/password from Step 3, regenerate if needed

### Issue: Emails go to spam

**Cause**: Missing SPF/DKIM/DMARC records
**Fix**: Configure DNS records in Step 5

---

## Cost Estimation

### Pricing

- **First 62,000 emails/month**: FREE (when sent from EC2)
- **After that**: $0.10 per 1,000 emails

### Example Costs

| Monthly Emails | Cost |
|---|---|
| 10,000 | $0 (free tier) |
| 100,000 | $3.80 |
| 500,000 | $43.80 |
| 1,000,000 | $93.80 |

**For Industrial Cloud**: With ~1,000-5,000 emails/month, cost is **$0** (free tier).

---

## Security Best Practices

1. **Rotate SMTP credentials** every 90 days
2. **Use IAM roles** for EC2 instead of SMTP credentials (more secure)
3. **Monitor bounce/complaint rates** (keep below thresholds)
4. **Use TLS encryption** (port 587 with STARTTLS)
5. **Store credentials in environment variables** (not in code)
6. **Enable CloudWatch logging** for audit trail

---

## Migration from Gmail SMTP

### Before (Gmail SMTP - Not Working in Production)
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-gmail@gmail.com
spring.mail.password=app-password
```

### After (AWS SES - Production Ready)
```properties
spring.mail.host=email-smtp.us-west-2.amazonaws.com
spring.mail.port=587
spring.mail.username=AKIA...
spring.mail.password=BPwJ...
```

**No code changes required** - just configuration!

---

## Next Steps

After AWS SES is working:

1. **Monitor for 1 week** - Check bounce/complaint rates
2. **Add custom domain** - Replace nip.io with indcloud.io
3. **Set up SNS notifications** - Get alerts for bounces/complaints
4. **Enable DKIM signing** - Improve deliverability
5. **Create email templates** - Use SES templates for consistent formatting

---

## Related Documentation

- AWS SES Developer Guide: https://docs.aws.amazon.com/ses/
- Spring Boot Mail Configuration: https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.email
- Issue #66: Production Email Fix
- Issue #88: SMS Alerts (will use AWS SNS)

---

**Last Updated**: 2025-11-06
**Status**: Implementation Guide
**Target Environment**: Production (us-west-2)
