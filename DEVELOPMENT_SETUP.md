# Development Setup Guide

This guide walks you through setting up the SensorVision development environment with proper security configuration.

## Prerequisites

- Java 17+
- Docker & Docker Compose
- PostgreSQL client (for manual database operations)
- Node.js 18+ (for frontend)
- Git

## Quick Start

### 1. Clone and Setup Environment

```bash
# Clone the repository (if not already done)
git clone <repository-url>
cd sensorvision

# Copy environment template
cp .env.example .env

# Edit .env with your development credentials
# For local development, the defaults should work
```

### 2. Start Infrastructure with Docker Compose

```bash
# Start PostgreSQL and MQTT broker
docker-compose up -d postgres mosquitto

# Verify services are running
docker-compose ps

# View logs if needed
docker-compose logs -f postgres
docker-compose logs -f mosquitto
```

### 3. Run the Application

#### Option A: Run with Gradle (Recommended for Development)

```bash
# Load environment variables (Linux/Mac)
export $(cat .env | xargs)

# Or on Windows (PowerShell)
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}

# Run the backend
./gradlew bootRun

# In a separate terminal, run the frontend
cd frontend
npm install
npm run dev
```

#### Option B: Run Everything with Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f backend
```

### 4. Verify Setup

- Backend API: http://localhost:8080
- Frontend: http://localhost:3001
- API Documentation: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health

## Token Migration (For Existing Deployments)

If you're upgrading an existing deployment with plaintext tokens:

### Step 1: Backup Existing Tokens

```bash
# Connect to PostgreSQL
psql -U sensorvision -d sensorvision

# Run backup script
\i scripts/backup_device_tokens.sql

# Export to CSV for safekeeping
\COPY (SELECT * FROM device_tokens_backup) TO 'device_tokens_backup.csv' WITH CSV HEADER;

# Exit psql
\q

# Store backup securely (encrypt if possible)
gpg -c device_tokens_backup.csv
rm device_tokens_backup.csv  # Remove plaintext version
```

### Step 2: Migrate Tokens

```bash
# Review migration script
cat scripts/migrate_tokens_to_hashed.sql

# Run migration (Option 1: Invalidate all tokens - RECOMMENDED)
psql -U sensorvision -d sensorvision -f scripts/migrate_tokens_to_hashed.sql

# Uncomment the OPTION 1 block in the SQL file before running
```

### Step 3: Rotate Tokens

After deployment, users must rotate their device tokens:

```bash
# Example: Rotate token for device "sensor-001"
curl -X POST http://localhost:8080/api/v1/devices/sensor-001/rotate-token \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Response will contain the new token (displayed only once)
{
  "deviceId": "sensor-001",
  "apiToken": "abc123def456..."  # Save this immediately!
}
```

### Step 4: Update Device Clients

Update your device firmware/clients to use the new token:

```json
// MQTT payload with token
{
  "deviceId": "sensor-001",
  "apiToken": "abc123def456...",
  "timestamp": "2025-01-15T10:30:00Z",
  "variables": {
    "temperature": 25.5,
    "humidity": 60.0
  }
}
```

## Environment Configuration

### Required Environment Variables

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/sensorvision
DB_USERNAME=sensorvision
DB_PASSWORD=your_secure_password

# JWT (REQUIRED - generate a strong secret)
JWT_SECRET=$(openssl rand -hex 32)
JWT_EXPIRATION_MS=86400000
JWT_ISSUER=http://localhost:8080

# MQTT
MQTT_BROKER_URL=tcp://localhost:1883
MQTT_USERNAME=sensorvision
MQTT_PASSWORD=your_mqtt_password
MQTT_DEVICE_AUTH_REQUIRED=false  # Set to true in production
```

### Generating Secure Secrets

```bash
# Generate JWT secret
openssl rand -hex 32

# Or using Python
python3 -c "import secrets; print(secrets.token_hex(32))"

# Or using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

## IDE Configuration

### IntelliJ IDEA

1. **Set Environment Variables:**
   - Run → Edit Configurations
   - Select your Spring Boot configuration
   - Environment Variables → Add from `.env` file
   - Or use EnvFile plugin: https://plugins.jetbrains.com/plugin/7861-envfile

2. **Enable Annotation Processing:**
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Enable annotation processing

### VS Code

1. **Install Extensions:**
   - Extension Pack for Java
   - Spring Boot Extension Pack

2. **Configure launch.json:**

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot-SensorVisionApplication",
      "request": "launch",
      "cwd": "${workspaceFolder}",
      "mainClass": "org.sensorvision.SensorVisionApplication",
      "projectName": "sensorvision",
      "envFile": "${workspaceFolder}/.env"
    }
  ]
}
```

## Docker Compose Profiles

The docker-compose.yml now supports environment variable substitution:

```bash
# Override specific variables
DB_PASSWORD=my_secure_pass docker-compose up -d

# Use a different .env file
docker-compose --env-file .env.production up -d
```

## Testing the Security Fixes

### 1. Test MQTT Device Authentication

```bash
# Should be rejected (no token)
mosquitto_pub -h localhost -p 1883 \
  -t "sensorvision/devices/test-001/telemetry" \
  -m '{"deviceId":"test-001","variables":{"temperature":25.5}}'

# Should succeed (with valid token)
mosquitto_pub -h localhost -p 1883 \
  -t "sensorvision/devices/test-001/telemetry" \
  -m '{"deviceId":"test-001","apiToken":"YOUR_TOKEN","variables":{"temperature":25.5}}'
```

### 2. Test Cross-Tenant Isolation

```bash
# Try to access another org's device data
curl -H "Authorization: Bearer ORG1_TOKEN" \
  http://localhost:8080/api/v1/export/csv/org2-device?from=2025-01-01T00:00:00Z&to=2025-01-15T23:59:59Z

# Should return 403 Forbidden
```

### 3. Test Webhook SSRF Protection

```bash
# Try to webhook to internal IP (should fail)
curl -X POST http://localhost:8080/api/v1/webhooks/test \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://192.168.1.1/admin"}'

# Try HTTP (should fail - HTTPS required)
curl -X POST http://localhost:8080/api/v1/webhooks/test \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://external-site.com/webhook"}'

# Valid webhook (should succeed)
curl -X POST http://localhost:8080/api/v1/webhooks/test \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://webhook.site/unique-id"}'
```

## Troubleshooting

### Database Connection Errors

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check connection
psql -h localhost -U sensorvision -d sensorvision -c "SELECT 1"

# View logs
docker-compose logs postgres
```

### MQTT Connection Errors

```bash
# Test MQTT connectivity
mosquitto_sub -h localhost -p 1883 -t '#' -v

# Check broker logs
docker-compose logs mosquitto

# Verify credentials in mosquitto.conf
cat mosquitto/config/mosquitto.conf
```

### JWT Secret Not Set Error

```bash
# Check if JWT_SECRET is set
echo $JWT_SECRET

# Generate and set
export JWT_SECRET=$(openssl rand -hex 32)

# Or add to .env file
echo "JWT_SECRET=$(openssl rand -hex 32)" >> .env
```

### Token Authentication Failures

```bash
# Check token format in database
psql -U sensorvision -d sensorvision -c \
  "SELECT external_id, LEFT(api_token, 10) as token_prefix FROM devices WHERE api_token IS NOT NULL;"

# BCrypt tokens should start with "$2a$" or "$2b$"
# If showing plaintext, tokens need migration

# Check application logs
docker-compose logs backend | grep -i "token\|auth"
```

## Development Workflows

### Hot Reload Development

```bash
# Backend (Spring Boot DevTools)
./gradlew bootRun

# Frontend (Vite HMR)
cd frontend && npm run dev

# Make changes and they'll reload automatically
```

### Database Migrations

```bash
# Create new migration
cat > src/main/resources/db/migration/V19__your_migration.sql << 'EOF'
-- Your SQL here
ALTER TABLE devices ADD COLUMN new_field VARCHAR(255);
EOF

# Restart application - Flyway runs automatically
./gradlew bootRun
```

### Cleaning Up

```bash
# Stop all containers
docker-compose down

# Remove volumes (WARNING: deletes data)
docker-compose down -v

# Clean build artifacts
./gradlew clean
```

## Production Deployment Checklist

- [ ] Set strong JWT_SECRET (32+ characters)
- [ ] Set secure database password
- [ ] Set secure MQTT credentials
- [ ] Enable MQTT device authentication (`MQTT_DEVICE_AUTH_REQUIRED=true`)
- [ ] Remove development defaults from docker-compose.yml
- [ ] Configure SSL/TLS for PostgreSQL
- [ ] Configure SSL/TLS for MQTT (port 8883)
- [ ] Set up proper firewall rules
- [ ] Enable HTTPS for the backend API
- [ ] Configure log rotation
- [ ] Set up monitoring and alerting
- [ ] Back up the database regularly
- [ ] Rotate all device tokens after deployment
- [ ] Review and update CORS settings
- [ ] Configure rate limiting appropriately

## Additional Resources

- [Security Fixes Summary](./SECURITY_FIXES_SUMMARY.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Flyway Migrations](https://flywaydb.org/documentation/)

## Support

If you encounter issues:

1. Check application logs: `docker-compose logs backend`
2. Check database logs: `docker-compose logs postgres`
3. Verify environment variables: `docker-compose config`
4. Review the troubleshooting section above
5. Check GitHub issues

## Security Notes

- **Never commit `.env` files** - they contain secrets
- **Rotate tokens regularly** - especially after team changes
- **Use environment-specific configs** - dev/staging/prod
- **Enable all security features in production**
- **Monitor authentication failures**
- **Keep dependencies updated**
