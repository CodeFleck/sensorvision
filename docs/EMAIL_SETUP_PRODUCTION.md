# Email Configuration for Production (AWS SES)

## Problem
Forgot password emails work on localhost but fail in production due to:
1. AWS Security Group blocks SMTP ports (25, 465, 587)
2. `EMAIL_ENABLED` defaults to `false`
3. SMTP environment variables not configured

## Solution: AWS SES (Simple Email Service)

### Why AWS SES?
- ✅ No firewall/security group issues (uses AWS SDK internally)
- ✅ Higher deliverability rates
- ✅ Production-ready with built-in monitoring
- ✅ Cost-effective: $0.10 per 1,000 emails
- ✅ Works seamlessly with EC2 instances

---

## Step 1: Configure AWS SES

### 1.1 Verify Sender Email Address

```bash
# Verify the sender email address (noreply@indcloud.com or your domain)
aws ses verify-email-identity \
  --email-address noreply@indcloud.com \
  --region us-west-2

# Check verification status
aws ses get-identity-verification-attributes \
  --identities noreply@indcloud.com \
  --region us-west-2
```

**Important**: You'll receive a verification email. Click the link to verify.

### 1.2 (Optional) Verify Domain for Better Deliverability

If you own a domain (e.g., indcloud.com):

```bash
# Verify entire domain
aws ses verify-domain-identity \
  --domain indcloud.com \
  --region us-west-2

# This will return DNS TXT records you need to add to your domain
```

Add the TXT record to your DNS:
```
Name: _amazonses.indcloud.com
Type: TXT
Value: <verification-token-from-aws>
```

### 1.3 Request Production Access (Remove Sandbox Limits)

By default, SES is in **sandbox mode** (can only send to verified addresses). Request production access:

1. Go to AWS SES Console → Account Dashboard
2. Click "Request production access"
3. Fill out the form:
   - **Use Case**: "IoT monitoring platform sending password reset and alert emails"
   - **Website URL**: http://35.88.65.186.nip.io:8080
   - **Description**: "Transactional emails for user authentication and IoT device alerts"
   - **Compliance**: Describe bounce/complaint handling (we use JavaMailSender)

Approval typically takes 24 hours.

### 1.4 Create SMTP Credentials

```bash
# Create IAM user for SMTP
aws iam create-user --user-name ses-smtp-user

# Attach SES sending policy
aws iam attach-user-policy \
  --user-name ses-smtp-user \
  --policy-arn arn:aws:iam::aws:policy/AmazonSESFullAccess

# Create access key
aws iam create-access-key --user-name ses-smtp-user
```

**Save the output**: You'll need `AccessKeyId` and `SecretAccessKey`.

### 1.5 Convert AWS Credentials to SMTP Password

AWS SES SMTP passwords are different from AWS secret keys. Use this script:

```bash
# Run this Python script to convert secret key to SMTP password
python3 << 'EOF'
import hmac
import hashlib
import base64

# Replace with your AWS Secret Access Key
SECRET = "YOUR_AWS_SECRET_ACCESS_KEY"
DATE = "11111111"
SERVICE = "ses"
MESSAGE = "SendRawEmail"
REGION = "us-west-2"
VERSION = 0x04

def sign(key, msg):
    return hmac.new(key, msg.encode("utf-8"), hashlib.sha256).digest()

signature = sign(("AWS4" + SECRET).encode("utf-8"), DATE)
signature = sign(signature, REGION)
signature = sign(signature, SERVICE)
signature = sign(signature, "aws4_request")
signature = sign(signature, MESSAGE)
signature_and_version = bytes([VERSION]) + signature
smtp_password = base64.b64encode(signature_and_version)
print("SMTP Password:", smtp_password.decode("utf-8"))
EOF
```

---

## Step 2: Configure Production Environment Variables

### 2.1 Create `.env.production` File

On your production EC2 instance, create/update `.env.production`:

```bash
# Email Configuration (AWS SES)
EMAIL_ENABLED=true
EMAIL_FROM=noreply@indcloud.com
SMTP_HOST=email-smtp.us-west-2.amazonaws.com
SMTP_PORT=587
SMTP_USERNAME=<YOUR_SMTP_USERNAME>  # AccessKeyId from Step 1.4
SMTP_PASSWORD=<YOUR_SMTP_PASSWORD>   # SMTP password from Step 1.5

# Support Email
SUPPORT_ISSUE_EMAIL=support@indcloud.com

# Application Base URL (for email links)
APP_BASE_URL=http://35.88.65.186.nip.io:8080
```

### 2.2 Update docker-compose.production.yml

Ensure `docker-compose.production.yml` loads the `.env.production` file:

```yaml
# At the top of docker-compose.production.yml
env_file:
  - .env.production
```

Or export variables before starting Docker:

```bash
# Load environment variables
set -a
source .env.production
set +a

# Restart backend
docker-compose -f docker-compose.production.yml up -d backend
```

---

## Step 3: Test Email Functionality

### 3.1 Test SMTP Connection from EC2

```bash
# SSH into production server
ssh ec2-user@35.88.65.186

# Test SMTP connectivity
openssl s_client -connect email-smtp.us-west-2.amazonaws.com:587 -starttls smtp
# Should see "CONNECTED"
```

### 3.2 Test Email Sending from Application

```bash
# Check backend logs
docker logs indcloud-backend -f

# Trigger password reset from UI
# Visit: http://35.88.65.186.nip.io:8080/forgot-password
# Enter a test email address

# Check logs for email sending confirmation
docker logs indcloud-backend | grep -i "email\|smtp"
```

### 3.3 Verify Email Delivery

Check the email inbox for the password reset email. If not received:

1. **Check SES Dashboard**: AWS Console → SES → Sending Statistics
2. **Check Bounce/Complaints**: Look for bounces or complaints
3. **Check Spam Folder**: First emails might land in spam
4. **Verify Email Address**: Ensure recipient email is verified (if in sandbox mode)

---

## Step 4: Alternative - Gmail SMTP (Not Recommended)

If you prefer Gmail SMTP instead of AWS SES:

### 4.1 Enable Security Group Egress Rules

```bash
# Allow outbound SMTP traffic
aws ec2 authorize-security-group-egress \
  --group-id sg-0255cea554e401228 \
  --protocol tcp \
  --port 587 \
  --cidr 0.0.0.0/0 \
  --region us-west-2

# Verify egress rules
aws ec2 describe-security-groups \
  --group-ids sg-0255cea554e401228 \
  --region us-west-2 \
  --query 'SecurityGroups[0].IpPermissionsEgress'
```

### 4.2 Configure Gmail App Password

1. Go to Google Account → Security → 2-Step Verification
2. Scroll to "App passwords" → Generate new password
3. Select "Mail" and "Other (Custom)" → Name it "Industrial Cloud Prod"
4. Copy the 16-character password

### 4.3 Update Environment Variables

```bash
EMAIL_ENABLED=true
EMAIL_FROM=your-gmail@gmail.com
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-gmail@gmail.com
SMTP_PASSWORD=<16-char-app-password>
```

**Note**: Gmail limits sending to 500 emails/day and may block you for suspicious activity.

---

## Step 5: Monitoring & Troubleshooting

### Check Email Sending Metrics

```bash
# AWS SES metrics
aws ses get-send-statistics --region us-west-2

# Application logs
docker logs indcloud-backend | grep -A 5 "Sending email"
```

### Common Issues

#### Issue: "Email address not verified" (Sandbox Mode)
**Solution**: Verify recipient email OR request production access (Step 1.3)

#### Issue: "Daily sending quota exceeded"
**Solution**: Request quota increase in SES console (default 200 emails/day in sandbox)

#### Issue: "Connection timeout"
**Solution**:
- Check security group egress rules
- Verify SMTP_HOST is correct: `email-smtp.us-west-2.amazonaws.com`
- Ensure port 587 is accessible

#### Issue: "Authentication failed"
**Solution**:
- Regenerate SMTP password using script in Step 1.5
- Verify SMTP_USERNAME is the IAM access key ID
- Ensure no extra spaces in environment variables

---

## Step 6: Enhanced Email Features (Optional)

### 6.1 Email Templates with HTML

SES supports HTML emails. Our `EmailNotificationService` already supports this.

### 6.2 Bounce & Complaint Handling

Set up SNS topics for bounces and complaints:

```bash
# Create SNS topic for bounces
aws sns create-topic --name ses-bounces --region us-west-2

# Configure SES to publish bounces
aws ses set-identity-notification-topic \
  --identity noreply@indcloud.com \
  --notification-type Bounce \
  --sns-topic arn:aws:sns:us-west-2:ACCOUNT_ID:ses-bounces \
  --region us-west-2
```

### 6.3 Email Sending Rate Limits

Configure rate limiting in `application-prod.properties`:

```properties
# Limit email sending to 5 per second (SES default)
notification.email.ratelimit.enabled=true
notification.email.ratelimit.max-per-second=5
```

---

## Summary

**Recommended Solution**: AWS SES
- ✅ No security group changes needed
- ✅ Production-ready
- ✅ Cost-effective
- ✅ Better deliverability

**Time to Implement**: 30 minutes (+ 24 hours for production access approval)

**Cost**: ~$0.10 per 1,000 emails (first 62,000 emails/month free if using EC2)

---

## Quick Start Commands

```bash
# 1. Verify email
aws ses verify-email-identity --email-address noreply@indcloud.com --region us-west-2

# 2. Create SMTP user
aws iam create-user --user-name ses-smtp-user
aws iam attach-user-policy --user-name ses-smtp-user --policy-arn arn:aws:iam::aws:policy/AmazonSESFullAccess
aws iam create-access-key --user-name ses-smtp-user

# 3. Convert secret key to SMTP password (use Python script above)

# 4. Update .env.production on EC2
# 5. Restart backend: docker-compose -f docker-compose.production.yml restart backend

# 6. Test password reset flow
```

---

**Issue**: #66
**Status**: Solution Documented
**Next Steps**: Implement AWS SES configuration on production server
