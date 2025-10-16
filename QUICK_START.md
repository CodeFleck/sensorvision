# Quick Start Guide

## 🚀 Get Running in 5 Minutes

### 1. Setup Environment (One-time)

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your credentials
# For development, the defaults will work
notepad .env  # Windows
# or
nano .env     # Linux/Mac
```

### 2. Start Services

#### 🎯 Quick Start (Recommended)

**Windows (PowerShell):**
```powershell
# One command to start everything
.\scripts\start-dev.ps1
```

**Linux/Mac:**
```bash
# One command to start everything
./scripts/start-dev.sh
```

The start script will:
- ✅ Stop any existing containers
- ✅ Kill processes on port 8080
- ✅ Load environment variables
- ✅ Start all Docker services
- ✅ Show service status

#### Manual Start (Alternative)

**Windows (PowerShell):**
```powershell
# Start infrastructure
docker-compose up -d postgres mosquitto

# Load environment variables
.\scripts\load-env.ps1

# Run application
.\gradlew.bat bootRun
```

**Linux/Mac:**
```bash
# Start infrastructure
docker-compose up -d postgres mosquitto

# Load environment variables
export $(cat .env | xargs)

# Run application
./gradlew bootRun
```

### 3. Access Application

- 🌐 **Frontend**: http://localhost:3001
- 🔌 **API**: http://localhost:8080
- 📚 **API Docs**: http://localhost:8080/swagger-ui.html
- ❤️ **Health**: http://localhost:8080/actuator/health

## 🔐 Token Migration (For Existing Users)

```bash
# 1. Backup tokens
psql -U sensorvision -d sensorvision -f scripts/backup_device_tokens.sql

# 2. Run migration
psql -U sensorvision -d sensorvision -f scripts/migrate_tokens_to_hashed.sql
# (Uncomment OPTION 1 in the SQL file before running)

# 3. Rotate tokens via API
curl -X POST http://localhost:8080/api/v1/devices/{deviceId}/rotate-token \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🛠️ Common Commands

### Development

```bash
# Full stack with Docker
docker-compose up -d

# Backend only (with hot reload)
./gradlew bootRun

# Frontend only (with HMR)
cd frontend && npm run dev

# Clean build
./gradlew clean build
```

### Database

```bash
# Connect to DB
docker exec -it sensorvision-postgres psql -U sensorvision

# View logs
docker-compose logs postgres

# Backup database
docker exec sensorvision-postgres pg_dump -U sensorvision sensorvision > backup.sql
```

### Testing

```bash
# Run tests
./gradlew test

# Specific test
./gradlew test --tests "*DeviceServiceTest"

# With coverage
./gradlew test jacocoTestReport
```

### MQTT Testing

```bash
# Subscribe to all topics
mosquitto_sub -h localhost -p 1883 -t '#' -v

# Publish telemetry (with token)
mosquitto_pub -h localhost -p 1883 \
  -t "sensorvision/devices/test-001/telemetry" \
  -m '{"deviceId":"test-001","apiToken":"YOUR_TOKEN","variables":{"temperature":25.5}}'
```

## 📝 Environment Variables Cheat Sheet

| Variable | Required | Example | Description |
|----------|----------|---------|-------------|
| `DB_USERNAME` | ✅ | `sensorvision` | Database user |
| `DB_PASSWORD` | ✅ | `secret123` | Database password |
| `JWT_SECRET` | ✅ | `32+ char string` | JWT signing key |
| `MQTT_USERNAME` | ✅ | `sensorvision` | MQTT broker user |
| `MQTT_PASSWORD` | ✅ | `secret123` | MQTT broker password |
| `MQTT_DEVICE_AUTH_REQUIRED` | ❌ | `true` | Require device tokens |
| `EMAIL_ENABLED` | ❌ | `false` | Enable email notifications |
| `WEBHOOK_ENABLED` | ❌ | `true` | Enable webhooks |

## 🐛 Troubleshooting

### "JWT_SECRET not set" Error

```bash
# Generate secret
openssl rand -hex 32

# Add to .env
echo "JWT_SECRET=$(openssl rand -hex 32)" >> .env
```

### Database Connection Failed

```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Restart if needed
docker-compose restart postgres

# Check credentials match .env
docker-compose config | grep -A5 postgres
```

### MQTT Connection Failed

```bash
# Check broker status
docker-compose ps mosquitto

# Test connection
mosquitto_sub -h localhost -p 1883 -t 'test' -v

# Check logs
docker-compose logs mosquitto
```

### Port Already in Use

```bash
# Find process using port 8080
# Windows
netstat -ano | findstr :8080

# Linux/Mac
lsof -i :8080

# Kill process or change port in .env
echo "SERVER_PORT=8081" >> .env
```

## 📚 Next Steps

1. 📖 Read [DEVELOPMENT_SETUP.md](./DEVELOPMENT_SETUP.md) for detailed setup
2. 🔒 Review [SECURITY_FIXES_SUMMARY.md](./SECURITY_FIXES_SUMMARY.md) for security info
3. 🌐 Access API documentation at http://localhost:8080/swagger-ui.html
4. 📊 Set up monitoring with Prometheus/Grafana (optional)

## ⚡ Pro Tips

- Use **IntelliJ EnvFile plugin** to load .env automatically
- Run **Postgres on startup**: `docker-compose up -d postgres mosquitto`
- Enable **Spring DevTools** for hot reload
- Use **Postman collection** for API testing (if available)
- Set up **git hooks** for linting/formatting

## 🆘 Need Help?

- Check logs: `docker-compose logs -f backend`
- Review configuration: `docker-compose config`
- Verify environment: `.\scripts\load-env.ps1`
- Read full docs: [DEVELOPMENT_SETUP.md](./DEVELOPMENT_SETUP.md)

---

**Happy Coding! 🎉**
