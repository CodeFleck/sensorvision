# Admin User Initialization

This document explains how to create the initial admin user for Industrial Cloud.

## Overview

The `AdminUserInitializer` component automatically creates an admin user on application startup using environment variables. This approach ensures credentials are never hardcoded in the source code.

## Security Features

- ✅ Credentials are passed via environment variables only
- ✅ Passwords are hashed with BCrypt before storage
- ✅ Initializer only runs if admin doesn't already exist
- ✅ Environment variables should be removed after admin creation
- ✅ Email is auto-verified for the admin user
- ✅ Admin gets both `ROLE_ADMIN` and `ROLE_USER` roles

## Usage

### Step 1: Set Environment Variables

Set these environment variables before deploying/starting the application:

```bash
# Required
export ADMIN_EMAIL="admin@yourcompany.com"
export ADMIN_PASSWORD="YourSecurePassword123!"

# Optional (with defaults)
export ADMIN_FIRST_NAME="Admin"           # Default: "Admin"
export ADMIN_LAST_NAME="User"             # Default: "User"
export ADMIN_ORG_NAME="System Administration"  # Default: "System Administration"
```

### Step 2: Deploy/Start Application

The admin user will be created automatically on startup:

```bash
./gradlew bootRun
# or
docker-compose up
```

### Step 3: Verify Creation

Check the application logs for:

```
=================================================================
ADMIN USER CREATED SUCCESSFULLY
Email: admin@yourcompany.com
=================================================================
SECURITY REMINDER: Remove ADMIN_EMAIL and ADMIN_PASSWORD environment variables and redeploy
=================================================================
```

### Step 4: Remove Environment Variables and Redeploy

**IMPORTANT:** After verifying the admin user was created, remove the environment variables and redeploy:

```bash
unset ADMIN_EMAIL
unset ADMIN_PASSWORD
unset ADMIN_FIRST_NAME
unset ADMIN_LAST_NAME
unset ADMIN_ORG_NAME
```

Then redeploy the application.

## Production Deployment (AWS ECS)

### Option 1: Using ECS Task Definition Environment Variables

Add environment variables to your ECS task definition **temporarily**:

```json
{
  "environment": [
    {
      "name": "ADMIN_EMAIL",
      "value": "admin@yourcompany.com"
    },
    {
      "name": "ADMIN_PASSWORD",
      "value": "YourSecurePassword123!"
    }
  ]
}
```

After the admin is created, remove these from the task definition and force a new deployment.

### Option 2: Using AWS Systems Manager Parameter Store

1. Store credentials in Parameter Store (encrypted):
   ```bash
   aws ssm put-parameter \
     --name "/indcloud/admin/email" \
     --value "admin@yourcompany.com" \
     --type "String"

   aws ssm put-parameter \
     --name "/indcloud/admin/password" \
     --value "YourSecurePassword123!" \
     --type "SecureString"
   ```

2. Update ECS task definition to read from Parameter Store:
   ```json
   {
     "secrets": [
       {
         "name": "ADMIN_EMAIL",
         "valueFrom": "/indcloud/admin/email"
       },
       {
         "name": "ADMIN_PASSWORD",
         "valueFrom": "/indcloud/admin/password"
       }
     ]
   }
   ```

3. After admin creation, delete the parameters:
   ```bash
   aws ssm delete-parameter --name "/indcloud/admin/email"
   aws ssm delete-parameter --name "/indcloud/admin/password"
   ```

## Troubleshooting

### Admin user not created

Check logs for:
```
Admin user initialization skipped: ADMIN_EMAIL or ADMIN_PASSWORD not set
```

**Solution:** Ensure environment variables are set correctly.

### User already exists

```
Admin user initialization skipped: User with email 'admin@example.com' already exists
```

**Solution:** The admin user already exists. You can log in with the existing credentials.

### Role not found error

```
ROLE_ADMIN not found in database. Ensure migrations have run.
```

**Solution:** Run Flyway migrations: `./gradlew flywayMigrate`

## Login After Creation

Once created, you can log in via:

1. **Web UI:** Navigate to http://your-domain.com and click "Login"
2. **API:**
   ```bash
   curl -X POST http://your-domain.com/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "username": "admin@yourcompany.com",
       "password": "YourSecurePassword123!"
     }'
   ```

## Changing Admin Password

After first login, immediately change the admin password:

1. Log in to the web UI
2. Navigate to Profile/Settings
3. Change password

Or use the API:
```bash
curl -X POST http://your-domain.com/api/v1/auth/change-password \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "YourSecurePassword123!",
    "newPassword": "NewSecurePassword456!"
  }'
```

## Best Practices

1. ✅ Use a strong password (minimum 12 characters, mixed case, numbers, symbols)
2. ✅ Remove environment variables immediately after admin creation
3. ✅ Change the admin password after first login
4. ✅ Enable MFA/2FA if available
5. ✅ Use a company email address for the admin account
6. ✅ Create individual admin accounts for each administrator (don't share credentials)
7. ✅ Audit admin actions regularly
