# Claiming io.sensorvision Namespace

Before publishing to Maven Central, you must claim the `io.sensorvision` namespace.

## Step 1: Login to Central Portal

Visit: https://central.sonatype.com/
Login with your account (the one with token: a55v0y)

## Step 2: Navigate to Namespaces

Click on your profile → "Namespaces" or visit:
https://central.sonatype.com/publishing/namespaces

## Step 3: Add Namespace

1. Click "Add Namespace"
2. Enter: `io.sensorvision`
3. Choose a verification method (recommended: GitHub)

## Verification Methods

### Option 1: GitHub Verification (RECOMMENDED - Easiest)

1. Go to: https://github.com/CodeFleck/sensorvision/settings
2. Click "Topics"
3. Add the verification code as a topic (Central Portal will show you the code)
   - Format: `central-namespace-XXXXXX` or similar
4. Save the topic
5. Return to Central Portal and click "Verify"

**Benefits**: Instant verification, easy to manage

### Option 2: DNS Verification

If you own the `sensorvision.io` domain:

1. Add a TXT record to your DNS:
   ```
   Host: _maven-central-verification
   Value: [verification-code-from-central-portal]
   ```
2. Wait for DNS propagation (~5-15 minutes)
3. Click "Verify" in Central Portal

### Option 3: Email Verification

If you have access to an `@sensorvision.io` email:

1. Send an email from that address
2. Follow instructions in Central Portal

### Option 4: Support Ticket

If none of the above work:

Email: central-support@sonatype.com

Subject: Namespace verification for io.sensorvision

Body:
```
Hello,

I would like to claim the io.sensorvision namespace for Maven Central publishing.

GitHub Repository: https://github.com/CodeFleck/sensorvision
Central Portal Username: [your username]

This is for the SensorVision IoT Platform's official Java SDK.

Thank you!
```

## Step 4: Wait for Approval

- **GitHub verification**: Usually instant
- **DNS verification**: A few minutes
- **Email/Support**: 1-2 business days

## Step 5: You're Ready to Publish!

Once the namespace is verified, you can deploy:

```bash
cd sensorvision-sdk-java
mvn clean deploy
```

## Current Configuration Status

✅ Maven Central Portal account created
✅ User Token generated (username: a55v0y)
✅ GPG key generated and published
✅ Maven settings.xml configured
✅ POM configured for Central Portal
⏳ **PENDING**: Namespace verification

**Next Action**: Claim the `io.sensorvision` namespace using one of the methods above.
