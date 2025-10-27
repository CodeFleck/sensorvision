# Scripts Directory

This directory contains utility scripts for development, deployment, and maintenance.

## 📁 Available Scripts

### Environment Setup

#### `load-env.sh` (Linux/Mac)
Loads environment variables from `.env` file into your shell session.

```bash
# Usage (must be sourced)
source ./scripts/load-env.sh

# Or
. ./scripts/load-env.sh
```

**Features:**
- Loads all variables from `.env`
- Validates critical security variables
- Color-coded output
- Checks for default/insecure values

#### `load-env.ps1` (Windows PowerShell)
PowerShell equivalent for Windows users.

```powershell
# Usage
.\scripts\load-env.ps1

# Or from project root
powershell -ExecutionPolicy Bypass -File .\scripts\load-env.ps1
```

### Database Scripts

#### `backup_device_tokens.sql`
Backs up existing device API tokens before migration to hashed format.

```bash
# Usage
psql -U sensorvision -d sensorvision -f scripts/backup_device_tokens.sql

# Export backup to CSV
psql -U sensorvision -d sensorvision -c \
  "\COPY (SELECT * FROM device_tokens_backup) TO 'device_tokens_backup.csv' WITH CSV HEADER;"
```

**What it does:**
- Creates `device_tokens_backup` table
- Copies all existing device tokens (plaintext)
- Provides summary of backed up tokens
- Shows token usage statistics

**IMPORTANT:** Store the backup securely and encrypt it:
```bash
gpg -c device_tokens_backup.csv
```

#### `migrate_tokens_to_hashed.sql`
Migrates device tokens from plaintext to hashed format.

```bash
# Usage
psql -U sensorvision -d sensorvision -f scripts/migrate_tokens_to_hashed.sql
```

**Migration Options:**

**Option 1: INVALIDATE ALL TOKENS (Recommended)**
- Sets all tokens to NULL
- Forces users to rotate tokens via API
- Most secure approach
- Uncomment the OPTION 1 block in the SQL file

**Option 2: Manual Migration**
- Keep plaintext tokens
- Requires custom BCrypt hashing script
- NOT RECOMMENDED

**Post-Migration:**
After running this script, users must rotate their device tokens:
```bash
curl -X POST http://localhost:8080/api/v1/devices/{deviceId}/rotate-token \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🔄 Complete Migration Workflow

Follow these steps for a complete token migration:

### Step 1: Backup Tokens (Before Deployment)

```bash
# Connect to database
docker exec -it sensorvision-postgres psql -U sensorvision

# Run backup
\i scripts/backup_device_tokens.sql

# Export to CSV
\COPY (SELECT * FROM device_tokens_backup) TO '/tmp/tokens.csv' WITH CSV HEADER;

# Exit psql
\q

# Copy from container
docker cp sensorvision-postgres:/tmp/tokens.csv ./device_tokens_backup.csv

# Encrypt backup
gpg -c device_tokens_backup.csv

# Store encrypted backup safely
mv device_tokens_backup.csv.gpg ~/backups/
rm device_tokens_backup.csv
```

### Step 2: Deploy New Code

```bash
# Pull latest changes
git pull

# Build new image
docker-compose build backend

# Stop old container
docker-compose stop backend

# Start with new code
docker-compose up -d backend

# Monitor logs
docker-compose logs -f backend
```

### Step 3: Run Migration

```bash
# Review migration script first
cat scripts/migrate_tokens_to_hashed.sql

# Edit to uncomment OPTION 1 (invalidate all tokens)
nano scripts/migrate_tokens_to_hashed.sql

# Run migration
docker exec -i sensorvision-postgres psql -U sensorvision < scripts/migrate_tokens_to_hashed.sql

# Verify migration
docker exec -it sensorvision-postgres psql -U sensorvision -c \
  "SELECT COUNT(*) FROM token_migration_log;"
```

### Step 4: Rotate Tokens

Create a script to rotate all tokens:

```bash
#!/bin/bash
# rotate-all-tokens.sh

JWT_TOKEN="your_admin_jwt_token"
API_URL="http://localhost:8080/api/v1"

# Get all devices
devices=$(curl -s -H "Authorization: Bearer $JWT_TOKEN" \
  "$API_URL/devices" | jq -r '.[].externalId')

echo "Rotating tokens for devices..."
for device in $devices; do
    echo "  Rotating: $device"
    new_token=$(curl -s -X POST \
      -H "Authorization: Bearer $JWT_TOKEN" \
      "$API_URL/devices/$device/rotate-token" | jq -r '.apiToken')

    echo "    New token: ${new_token:0:10}..."

    # Store token securely for device configuration
    echo "$device,$new_token" >> device_tokens_new.csv
done

echo "Done! Tokens saved to device_tokens_new.csv"
echo "IMPORTANT: Distribute tokens securely to device owners"
```

### Step 5: Update Devices

Update device firmware/configuration with new tokens:

```python
# Example Python script
import csv
import paho.mqtt.client as mqtt

def update_device_config(device_id, token):
    # Your device update logic here
    print(f"Updating {device_id} with new token")
    # ...

with open('device_tokens_new.csv', 'r') as f:
    reader = csv.reader(f)
    for row in reader:
        device_id, token = row
        update_device_config(device_id, token)
```

## 🛠️ Development Scripts

Add more scripts as needed:

### Example: Database Reset Script

```bash
#!/bin/bash
# reset-database.sh - Reset database to clean state

docker-compose down -v
docker-compose up -d postgres
sleep 5
docker exec -i sensorvision-postgres psql -U sensorvision < init.sql
```

### Example: Load Test Data

```bash
#!/bin/bash
# load-test-data.sh - Load sample data for testing

docker exec -i sensorvision-postgres psql -U sensorvision << EOF
INSERT INTO devices (external_id, name, status, organization_id)
VALUES ('test-001', 'Test Device 1', 'ONLINE', 1);
-- ... more test data
EOF
```

## 📝 Best Practices

1. **Always backup before migrations**
   - Run `backup_device_tokens.sql`
   - Export to CSV
   - Encrypt and store safely

2. **Test migrations in staging first**
   - Never run directly in production
   - Verify with sample data
   - Check application logs

3. **Keep scripts version controlled**
   - Track changes in Git
   - Document modifications
   - Review before running

4. **Secure sensitive data**
   - Encrypt token backups
   - Use secure file permissions
   - Delete plaintext backups

5. **Monitor after migrations**
   - Check application logs
   - Verify token authentication
   - Monitor error rates

## 🆘 Troubleshooting

### Script fails with "permission denied"

```bash
# Linux/Mac
chmod +x scripts/*.sh

# Windows - run PowerShell as Administrator
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Cannot connect to database

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check connection
docker exec sensorvision-postgres pg_isready -U sensorvision

# View logs
docker-compose logs postgres
```

### Migration script hangs

```bash
# Check for locks
docker exec -it sensorvision-postgres psql -U sensorvision -c \
  "SELECT * FROM pg_locks WHERE NOT granted;"

# Kill stuck queries
docker exec -it sensorvision-postgres psql -U sensorvision -c \
  "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle in transaction';"
```

### Token rotation fails

```bash
# Check application logs
docker-compose logs backend | grep -i "token\|auth"

# Verify JWT token is valid
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/v1/devices

# Check device exists
docker exec -it sensorvision-postgres psql -U sensorvision -c \
  "SELECT external_id, name FROM devices WHERE external_id = 'device-id';"
```

## 📚 Additional Resources

- [Development Setup Guide](../DEVELOPMENT_SETUP.md)
- [Security Fixes Summary](../SECURITY_FIXES_SUMMARY.md)
- [Quick Start Guide](../QUICK_START.md)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

## 🔐 Security Notes

- **Never commit tokens or credentials**
- **Encrypt all backups**
- **Use secure file permissions** (chmod 600 for sensitive files)
- **Rotate tokens regularly**
- **Monitor for suspicious activity**
- **Keep scripts up to date**

---

## 📋 Deployment Documentation Scripts

### create-deployment-issue.sh / create-deployment-issue.ps1
Automatically creates GitHub issues when deployment errors occur.

**Usage (Bash):**
```bash
./scripts/create-deployment-issue.sh \
  "Connection timeout during deployment" \
  "Docker container failed to start on EC2" \
  "critical"
```

**Usage (PowerShell):**
```powershell
.\scripts\create-deployment-issue.ps1 `
  -ErrorTitle "Connection timeout during deployment" `
  -ErrorDescription "Docker container failed to start on EC2" `
  -Severity critical
```

**Severity Levels:**
- `critical` - 🔴 Critical issues requiring immediate attention
- `high` - 🟠 High priority issues
- `medium` - 🟡 Medium priority (default)
- `low` - 🟢 Low priority

**What it does:**
- Creates a GitHub issue with detailed error information
- Automatically adds appropriate labels
- Updates DEPLOY_DOCUMENTATION.md with error reference
- Includes environment info, investigation steps, and rollback procedures

### update-deployment-docs.ps1
Helper script to document successful deployments.

**Usage:**
```powershell
.\scripts\update-deployment-docs.ps1 `
  -Title "Integration Wizard Fix" `
  -Type "Bug Fix" `
  -Description "Fixed boolean field in connection test" `
  -FilesChanged "frontend/src/pages/IntegrationWizard.tsx" `
  -Impact "Users can now test connections successfully"
```

**Interactive Mode:**
```powershell
.\scripts\update-deployment-docs.ps1
# Will prompt for all required information
```

**Types:**
- `Feature` - New functionality
- `Bug Fix` - Bug fixes
- `Enhancement` - Improvements to existing features
- `Hotfix` - Urgent production fixes

**What it does:**
- Adds deployment entry to DEPLOY_DOCUMENTATION.md
- Includes commit info, deployment time, and verification steps
- Formats the entry consistently
- Prompts to commit the documentation update

### Quick Deployment Documentation Workflow

**After a successful deployment:**
```powershell
.\scripts\update-deployment-docs.ps1
git add DEPLOY_DOCUMENTATION.md
git commit -m "docs: update deployment documentation"
git push
```

**When a deployment fails:**
```powershell
.\scripts\create-deployment-issue.ps1 `
  -ErrorTitle "Brief error description" `
  -ErrorDescription "Detailed error with logs" `
  -Severity high
```

---

For questions or issues, check the main documentation or open a GitHub issue.
