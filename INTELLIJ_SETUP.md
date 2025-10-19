# IntelliJ IDEA Local Development Setup

This guide explains how to run the SensorVision backend in IntelliJ IDE for debugging, while keeping PostgreSQL and MQTT in Docker.

## Architecture

- **Docker**: PostgreSQL (port 5432) + MQTT/Mosquitto (port 1883)
- **IntelliJ**: Spring Boot backend (port 8080)
- **npm**: React frontend (port 3001)

## Prerequisites

1. Docker services running:
```bash
# Start only the infrastructure services (exclude backend)
docker-compose up -d postgres mosquitto
```

2. Verify services are running:
```bash
docker ps
# Should show: sensorvision-postgres and sensorvision-mosquitto
```

## IntelliJ Configuration

### Method 1: Using Spring Profiles (Recommended)

1. **Open Run/Debug Configurations** (Run > Edit Configurations...)

2. **Create/Edit Spring Boot configuration**:
   - Main class: `org.sensorvision.Application`
   - Active profiles: `local`

3. **Add Environment Variables** (optional, if you want to override defaults):
   ```
   SPRING_PROFILES_ACTIVE=local
   ```

4. **Run the application**
   - The backend will automatically use the `application-local.yml` profile
   - It will connect to `localhost:5432` for PostgreSQL
   - It will connect to `localhost:1883` for MQTT

### Method 2: Using Environment Variables

If you prefer not to use the `local` profile, you can set environment variables directly:

1. **Open Run/Debug Configurations**

2. **Add Environment Variables**:
   ```
   DB_URL=jdbc:postgresql://localhost:5432/sensorvision
   DB_USERNAME=sensorvision
   DB_PASSWORD=sensorvision123
   MQTT_BROKER_URL=tcp://localhost:1883
   MQTT_USERNAME=sensorvision
   MQTT_PASSWORD=sensorvision123
   JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
   ```

## Frontend Configuration

The frontend is already configured to work with both Docker and local backend:

**Location**: `frontend/vite.config.ts`

```typescript
server: {
  port: 3001,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // Works for both Docker and local
      changeOrigin: true,
    }
  }
}
```

The frontend **always connects to `localhost:8080`**, so it works whether your backend runs:
- In Docker (exposed on `localhost:8080`)
- In IntelliJ (running on `localhost:8080`)

## Complete Development Workflow

### Starting Everything

```bash
# Terminal 1: Start infrastructure
docker-compose up -d postgres mosquitto

# Terminal 2: Start backend in IntelliJ
# Run > Run 'Application' (with 'local' profile)

# Terminal 3: Start frontend
cd frontend
npm run dev
```

### Stopping Everything

```bash
# Stop IntelliJ application (Shift+F5 or stop button)

# Stop Docker services
docker-compose down

# Frontend stops with Ctrl+C in terminal
```

## Troubleshooting

### Backend can't connect to database

**Symptom**: `PSQLException: The connection attempt failed`

**Solution**:
1. Verify PostgreSQL is running: `docker ps | findstr postgres`
2. Check connection details in IntelliJ run configuration
3. Verify the `local` profile is active

### Backend can't connect to MQTT

**Symptom**: `MqttException: Unable to connect to server`

**Solution**:
1. Verify Mosquitto is running: `docker ps | findstr mosquitto`
2. Test MQTT connection:
   ```bash
   docker exec -it sensorvision-mosquitto mosquitto_sub -t "test"
   ```

### Frontend shows "Network Error"

**Symptom**: Frontend can't reach backend APIs

**Solution**:
1. Verify backend is running on port 8080:
   ```bash
   netstat -an | findstr "8080.*LISTENING"
   ```
2. Check IntelliJ console for startup errors
3. Verify actuator health endpoint:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### Port 8080 already in use

**Symptom**: `Port 8080 was already in use`

**Solution**:
1. Stop the Docker backend container:
   ```bash
   docker-compose stop backend
   # OR
   docker stop sensorvision-backend
   ```
2. Find and kill process using port 8080:
   ```bash
   netstat -ano | findstr ":8080"
   taskkill /PID <PID> /F
   ```

## Switching Between Docker and Local Backend

### Use Docker Backend
```bash
docker-compose up -d  # Starts all services including backend
```

### Use Local Backend (IntelliJ)
```bash
docker-compose up -d postgres mosquitto  # Only infrastructure
# Then run backend in IntelliJ
```

## Quick Verification

After starting, verify everything is working:

```bash
# 1. Check backend health
curl http://localhost:8080/actuator/health

# 2. Test MQTT (send telemetry)
mosquitto_pub -h localhost -p 1883 -t "sensorvision/devices/test-dev/telemetry" -m '{
  "deviceId": "test-dev",
  "timestamp": "2024-01-01T12:00:00Z",
  "variables": {
    "kw_consumption": 50.5
  }
}'

# 3. Open frontend
# Navigate to http://localhost:3001
```
