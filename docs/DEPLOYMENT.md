# Production Deployment Guide

## Overview
This guide covers deploying Industrial Cloud to production with feature flags for SMS and email notifications.

## Pre-Deployment Checklist

- [ ] All tests passing (`./gradlew test`)
- [ ] Feature flags configured in `application-prod.properties`
- [ ] Environment variables set in deployment platform
- [ ] Database migrations ready
- [ ] Docker containers built and pushed

## Feature Flags

### Default State (Production)
```properties
notification.email.enabled=false
notification.sms.enabled=false
```

### Enabling Notifications
After configuring credentials, set environment variables:
```bash
EMAIL_ENABLED=true
SMS_ENABLED=true
```

## Deployment Steps

### 1. Build the Application
```bash
./gradlew clean build
```

### 2. Build Docker Image
```bash
docker build -t indcloud:latest .
```

### 3. Push to Registry
```bash
docker tag indcloud:latest your-registry/indcloud:latest
docker push your-registry/indcloud:latest
```

### 4. Deploy to Production
```bash
# Using Docker Compose
docker-compose -f docker-compose.prod.yml up -d

# Or using your deployment platform (AWS ECS, Kubernetes, etc.)
```

### 5. Verify Deployment
```bash
# Check health endpoint
curl http://your-domain/actuator/health

# Verify feature flags
curl http://your-domain/actuator/env | grep notification
```

## Enabling Notifications After Deployment

### Step 1: Configure Credentials
Follow the [Credential Setup Guide](CREDENTIAL_SETUP.md) to configure:
- Twilio (SMS)
- AWS SES (Email)

### Step 2: Update Environment Variables
```bash
# Set in your deployment platform
EMAIL_ENABLED=true
SMS_ENABLED=true
```

### Step 3: Restart Application
```bash
docker-compose restart app
```

### Step 4: Test Notifications
1. Trigger a test alert
2. Verify SMS/email is sent
3. Check logs for any errors

## Rollback Procedure

If issues occur:

### Quick Disable
```bash
# Disable notifications without redeployment
EMAIL_ENABLED=false
SMS_ENABLED=false
docker-compose restart app
```

### Full Rollback
```bash
# Revert to previous version
docker-compose down
docker-compose -f docker-compose.prod.yml up -d your-registry/indcloud:previous-tag
```

## Monitoring

### Key Metrics
- `notifications.sent` - Total notifications sent
- `notifications.failed` - Failed notifications
- `sms.rate_limit_exceeded` - SMS rate limit hits

### Log Monitoring
```bash
# Watch for feature flag messages
docker logs -f indcloud | grep "feature flag"

# Watch for notification errors
docker logs -f indcloud | grep "notification.*ERROR"
```

## Troubleshooting

### Notifications Not Sending
1. Check feature flags: `EMAIL_ENABLED=true`, `SMS_ENABLED=true`
2. Verify credentials are set
3. Check logs for authentication errors
4. Test SMTP/Twilio connectivity

### Feature Flag Not Respected
1. Verify environment variables are loaded
2. Check Spring profile is set correctly
3. Restart application to reload configuration

## Security Notes

- Never commit `.env` file
- Rotate credentials regularly
- Use secret manager in production
- Monitor for unauthorized access
