# Credential Setup Guide for Industrial Cloud

> [!NOTE]
> This guide explains how to configure **Twilio** (SMS) and **AWS SES** (email) credentials for the Industrial Cloud application. All values should be provided via environment variables or `application.properties`.

## 1. Twilio (SMS) Configuration

1. **Create a Twilio account** at https://www.twilio.com/.
2. In the Twilio Console, obtain:
   - **Account SID** (`TWILIO_ACCOUNT_SID`)
   - **Auth Token** (`TWILIO_AUTH_TOKEN`)
   - **Phone number** you will send SMS from (`TWILIO_FROM_NUMBER`).
3. Add the credentials to your environment or `src/main/resources/application.properties`:
   ```properties
   TWILIO_ACCOUNT_SID=your_account_sid_here
   TWILIO_AUTH_TOKEN=your_auth_token_here
   TWILIO_FROM_NUMBER=+1234567890   # E.164 format
   SMS_ENABLED=true
   SMS_FROM_NUMBER=${TWILIO_FROM_NUMBER}
   ```
4. Ensure the `SmsNotificationService` reads these values (it already uses `@Value` annotations).
5. **Test**: Run `SmsNotificationServiceTest` to verify sending works (mocked in CI).

## 2. AWS SES (Email) Configuration

1. **Verify your domain/email** in the AWS SES console.
2. Create **SMTP credentials** (or IAM user with `AmazonSESFullAccess`).
3. Obtain:
   - **SMTP Host** (`SMTP_HOST`)
   - **SMTP Port** (`SMTP_PORT`)
   - **Username** (`SMTP_USERNAME`)
   - **Password** (`SMTP_PASSWORD`)
4. Add to environment or `application.properties`:
   ```properties
   EMAIL_ENABLED=true
   EMAIL_FROM=your_verified_email@example.com
   SMTP_HOST=email-smtp.us-east-1.amazonaws.com
   SMTP_PORT=587
   SMTP_USERNAME=your_smtp_username
   SMTP_PASSWORD=your_smtp_password
   ```
5. The `EmailNotificationService` uses these properties to build a `JavaMailSender`.
6. **Test**: Run `EmailNotificationServiceTest` (it uses mocks; you can add an integration test if needed).

## 3. Loading Environment Variables

- **Local development**: Create a `.env` file in the project root and use a tool like `dotenv-java` or configure your IDE to load it.
- **Docker Compose**: Add the variables under the `environment:` section for the `app` service.
- **Production**: Set the variables in your deployment platform (e.g., AWS ECS task definition, Kubernetes secret, or CI/CD pipeline).

## 4. Security Tips

- Never commit real credentials to source control.
- Use a secret manager (AWS Secrets Manager, HashiCorp Vault) for production.
- Rotate credentials periodically.

---
*All steps are concise; adjust paths/values to match your environment.*
