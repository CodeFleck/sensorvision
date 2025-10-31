# Forgot Password Email - Production Fix Guide

## 🔍 Problem Identified

Forgot password emails work on localhost but **not in production** because:

### Root Cause
1. **Email notifications are DISABLED** by default in production (`EMAIL_ENABLED=false`)
2. **SMTP credentials are not configured** on the production server

## 📋 Investigation Summary

### Configuration Files Checked

1. **docker-compose.production.yml** (Line 77)
   ```yaml
   EMAIL_ENABLED: ${EMAIL_ENABLED:-false}  # ❌ Defaults to false!
   ```

2. **.env.production.template** (Line 53)
   ```env
   EMAIL_ENABLED=false  # ❌ Disabled
   SMTP_HOST=smtp.gmail.com  # Placeholder values
   SMTP_USERNAME=your-email@gmail.com  # Not configured
   ```

3. **application-prod.properties** (Line 110)
   ```properties
   notification.email.enabled=${EMAIL_ENABLED:false}  # Respects env var
   ```

### Why It Works on Localhost

On localhost, you likely have email configured in your local `.env` file or `application.properties` with working SMTP credentials.

---

## ✅ Solution

You need to configure email on the production EC2 instance.

### Step 1: SSH to Production Server

```bash
ssh ec2-user@35.88.65.186
# Or use your existing SSH key
ssh -i your-key.pem ec2-user@35.88.65.186
```

### Step 2: Navigate to Application Directory

```bash
cd /path/to/sensorvision  # Adjust to your actual path
# or
cd ~/sensorvision
```

### Step 3: Create/Edit Production Environment File

```bash
nano .env.production
# or use vi/vim if you prefer
```

---

## 🔧 Configuration Options

### Option 1: Gmail (Quick Setup - Good for Testing)

**Pros**: Easy to set up, free
**Cons**: Rate limits (500 emails/day), less reliable for production

Add to `.env.production`:
```env
EMAIL_ENABLED=true
EMAIL_FROM=noreply@sensorvision.com
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-actual-email@gmail.com
SMTP_PASSWORD=your-app-password-here
```

#### Important: Gmail App Password Setup

1. Go to [Google Account Security](https://myaccount.google.com/security)
2. Enable **2-Step Verification** (required for app passwords)
3. Go to [App Passwords](https://myaccount.google.com/apppasswords)
4. Create new app password for "Mail"
5. Copy the 16-character password
6. Use this in `SMTP_PASSWORD` (not your regular Gmail password)

---

### Option 2: AWS SES (Recommended for Production) ⭐

**Pros**: Reliable, scalable, better deliverability, already in AWS
**Cons**: Requires AWS SES setup (15 minutes)

Add to `.env.production`:
```env
EMAIL_ENABLED=true
EMAIL_FROM=noreply@sensorvision.com
SMTP_HOST=email-smtp.us-west-2.amazonaws.com
SMTP_PORT=587
SMTP_USERNAME=<your-ses-smtp-username>
SMTP_PASSWORD=<your-ses-smtp-password>
```

#### AWS SES Setup Steps

1. **Open AWS SES Console**
   - Region: us-west-2 (same as your EC2)
   - https://console.aws.amazon.com/ses/home?region=us-west-2

2. **Verify Email Address**
   ```
   SES Console → Verified identities → Create identity
   - Choose: Email address
   - Enter: noreply@sensorvision.com (or your domain email)
   - Click: Create identity
   - Check your email for verification link
   ```

3. **Request Production Access** (Important!)
   ```
   SES is in "Sandbox" mode by default (can only send to verified emails)

   To send to any email:
   - SES Console → Account dashboard
   - Click: Request production access
   - Fill form: Use case, daily sending quota estimate
   - AWS usually approves within 24 hours
   ```

4. **Create SMTP Credentials**
   ```
   SES Console → SMTP settings → Create SMTP credentials
   - IAM User Name: ses-smtp-user (or any name)
   - Click: Create user
   - Download or copy:
     - SMTP Username
     - SMTP Password
   ```

5. **Update Production Config**
   ```bash
   # Edit .env.production with SES credentials
   nano .env.production
   ```

---

### Option 3: SendGrid (Alternative)

**Pros**: Easy setup, free tier (100 emails/day), reliable
**Cons**: Requires SendGrid account

```env
EMAIL_ENABLED=true
EMAIL_FROM=noreply@sensorvision.com
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=<your-sendgrid-api-key>
```

SendGrid setup:
1. Create account at https://sendgrid.com
2. Verify sender email
3. Create API key with "Mail Send" permission
4. Use API key as SMTP_PASSWORD

---

## 🚀 Apply Configuration

### Step 4: Restart Backend Container

After saving `.env.production`:

```bash
# Load environment variables
export $(cat .env.production | xargs)

# Restart backend container
docker-compose -f docker-compose.production.yml restart backend
```

Or restart all services:
```bash
docker-compose -f docker-compose.production.yml down
docker-compose -f docker-compose.production.yml up -d
```

### Step 5: Verify Configuration

Check if email is enabled in logs:
```bash
docker logs sensorvision-backend | grep -i "email"
docker logs sensorvision-backend | grep -i "notification"
```

Expected output:
```
Email notifications enabled: true
SMTP Host: smtp.gmail.com (or email-smtp.us-west-2.amazonaws.com)
```

---

## 🧪 Testing

### Test 1: Backend Health Check
```bash
curl http://localhost:8080/actuator/health
```

Should show status: "UP"

### Test 2: Forgot Password Flow

1. **Open browser**: http://35.88.65.186.nip.io:8080/forgot-password
2. **Enter email**: A valid user email from your database
3. **Click**: Send Reset Link
4. **Check email inbox** (including spam folder)

Expected email content:
```
Subject: Reset Your Password

Click the link below to reset your password:
http://35.88.65.186.nip.io:8080/reset-password?token=...

This link expires in 24 hours.
```

### Test 3: Check Backend Logs

```bash
# Monitor logs in real-time
docker logs -f sensorvision-backend

# In another terminal, trigger forgot password
# You should see email sending logs
```

---

## 🐛 Troubleshooting

### Issue: Still No Email After Configuration

**Check 1**: Verify EMAIL_ENABLED is true
```bash
docker exec sensorvision-backend env | grep EMAIL
```

**Check 2**: Check for errors in logs
```bash
docker logs sensorvision-backend 2>&1 | grep -i error
docker logs sensorvision-backend 2>&1 | grep -i mail
```

**Check 3**: Test SMTP connection from server
```bash
telnet smtp.gmail.com 587
# or
nc -zv smtp.gmail.com 587
```

If connection fails, check firewall/security group.

### Issue: Gmail "Authentication Failed"

- Ensure you're using **App Password**, not regular password
- Verify 2-Step Verification is enabled
- Check username is correct (full email address)

### Issue: AWS SES "Email not verified"

- Verify sender email in SES Console
- Check verification email and click link
- Wait a few minutes after verification

### Issue: AWS SES "Sending rate exceeded"

- SES is in sandbox mode (can only send to verified emails)
- Request production access in SES Console
- Or verify recipient email for testing

### Issue: Port 587 Blocked

Check AWS Security Group allows outbound SMTP:
```bash
aws ec2 describe-security-groups \
  --group-ids sg-0255cea554e401228 \
  --region us-west-2 \
  --query 'SecurityGroups[0].IpPermissionsEgress'
```

If SMTP is blocked, add outbound rule:
```bash
aws ec2 authorize-security-group-egress \
  --group-id sg-0255cea554e401228 \
  --protocol tcp \
  --port 587 \
  --cidr 0.0.0.0/0 \
  --region us-west-2
```

---

## 🔒 Security Best Practices

1. **Protect .env.production file**
   ```bash
   chmod 600 .env.production
   ```

2. **Never commit to git**
   - `.env.production` should be in `.gitignore`
   - Use secret managers for enterprise setups

3. **Use App Passwords** (Gmail)
   - Never use main account password
   - Rotate passwords regularly

4. **Enable SPF/DKIM** (AWS SES)
   - Improves email deliverability
   - Prevents spoofing

5. **Monitor Usage**
   - Set up CloudWatch alarms for SES
   - Monitor bounce/complaint rates

---

## 📊 Email Service Comparison

| Feature | Gmail | AWS SES | SendGrid |
|---------|-------|---------|----------|
| Setup Time | 5 min | 15 min | 10 min |
| Free Tier | 500/day | 62,000/month | 100/day |
| Deliverability | Good | Excellent | Excellent |
| Production Ready | No | Yes | Yes |
| Cost (1M emails) | N/A | $100 | $15-$90 |
| Approval Required | No | Yes | No |
| Recommendation | Testing | **Production** | Alternative |

---

## 📝 Quick Reference Commands

```bash
# SSH to production
ssh ec2-user@35.88.65.186

# Edit config
nano /path/to/sensorvision/.env.production

# Restart backend
docker-compose -f docker-compose.production.yml restart backend

# Check logs
docker logs -f sensorvision-backend

# Test SMTP
telnet smtp.gmail.com 587

# Check environment
docker exec sensorvision-backend env | grep EMAIL
```

---

## ✅ Checklist

After completing the fix:

- [ ] SSH to production server
- [ ] Edit `.env.production` file
- [ ] Set `EMAIL_ENABLED=true`
- [ ] Configure SMTP credentials (Gmail/SES/SendGrid)
- [ ] Save file and set permissions (`chmod 600`)
- [ ] Restart backend container
- [ ] Verify logs show email enabled
- [ ] Test forgot password flow
- [ ] Receive reset email successfully
- [ ] Click link and verify password reset works
- [ ] Update GitHub issue #66 with resolution

---

## 📞 Need Help?

- **GitHub Issue**: #66
- **AWS SES Docs**: https://docs.aws.amazon.com/ses/
- **Gmail App Passwords**: https://support.google.com/accounts/answer/185833

---

**Created**: 2025-10-31
**Status**: Investigation Complete ✅
**Next Step**: Configure email on production server
**Estimated Time**: 15-30 minutes
