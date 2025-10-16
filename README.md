# SensorVision IoT Monitoring Platform

A comprehensive Ubidots-like IoT monitoring platform built on Spring Boot and MQTT that provides complete device management, real-time data visualization, alerting, and analytics capabilities.

## 🚀 Features

### Core Infrastructure
- **MQTT & HTTP REST ingestion** for flexible device connectivity
- **PostgreSQL persistence** with Flyway-managed schema migrations
- **WebSocket real-time streaming** for live dashboard updates
- **Docker-first infrastructure**: Mosquitto and PostgreSQL in a lightweight stack
- **Swagger/OpenAPI documentation** for interactive API testing

### Frontend Dashboard
- **Modern React Web Dashboard** built with Vite and TypeScript
- **Real-time data visualization** with Chart.js and WebSocket integration
- **Device management interface** with CRUD operations
- **Responsive design** with Tailwind CSS
- **Widget-based dashboards** with drag-and-drop functionality

### Device & Data Management
- **Device lifecycle management** with status tracking
- **Device groups & tags** for flexible organization
- **Time-series telemetry storage** with optimized indexing
- **Data aggregation endpoints** (MIN/MAX/AVG/SUM) with time intervals
- **Data export** in CSV and JSON formats
- **Variable management** with rich metadata (units, ranges, colors)
- **RESTful APIs** for all operations
- **Multi-tenant organization** support

### Advanced Analytics & Alerting
- **Rules engine** for conditional monitoring and automation
- **Alert system** with severity levels and acknowledgment
- **Synthetic variables** for derived metrics and calculations
- **Historical data analytics** with flexible time ranges
- **Multi-channel notifications** - Email, SMS, Webhook, and In-App
- **Configurable notification preferences** with severity thresholds

### Security & Access Control
- **JWT-based authentication** with secure token management
- **Role-based access control** (Admin, User, Viewer roles)
- **Multi-tenant architecture** with organization isolation
- **User registration** with automatic organization creation
- **Secure password hashing** with BCrypt

### Events & Audit Trail
- **Comprehensive event system** tracking all platform activities
- **Event filtering** by type, severity, device, and time range
- **Event statistics** and analytics dashboards
- **Real-time event notifications** via WebSocket

### Monitoring & Observability
- **Structured logging** with rolling file appenders
- **Smart meter simulator** for testing and demonstration
- **Real-time connection status** and health monitoring
- **Notification delivery tracking** with success/failure logs
- **Health endpoints** for service monitoring

## Quick Start

### Prerequisites

- Java 17+
- Docker Desktop
- Git

### 1. Clone and Setup

```bash
git clone <your-repo-url>
cd sensorvision
```

### 2. Start Infrastructure Services

```bash
# Start PostgreSQL and MQTT broker
docker-compose up -d
```

### 3. Run the Application

```bash
# On Windows PowerShell (use .\ prefix)
.\gradlew.bat bootRun

# On Windows Command Prompt
gradlew.bat bootRun

# On Linux/Mac
./gradlew bootRun
```

### 4. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

### 5. Access the Services

- **🌟 SensorVision Dashboard**: http://localhost:3001 (React Web UI)
- **📚 Swagger API Documentation**: http://localhost:8080/swagger-ui.html
- **Backend API**: http://localhost:8080/api/v1/*
- **MQTT Broker**: localhost:1883

### 6. First Time Setup

1. Register a new account at http://localhost:3001/register
2. Login with your credentials
3. The simulator will auto-create 10 test devices
4. Explore the dashboard and test the new features!

## 🏗️ Architecture Overview

### System Architecture
```
┌─────────────┐  MQTT/HTTP  ┌─────────────────┐    WebSocket    ┌─────────────────┐
│ IoT Devices │─────────────>│   Spring Boot   │<───────────────>│   React Web     │
└─────────────┘             │   Application   │                 │   Dashboard     │
                            └─────────────────┘                 └─────────────────┘
                                     │
                                     ▼
┌─────────────┐              ┌─────────────────┐              ┌─────────────────┐
│ PostgreSQL  │<─────────────│  Data Pipeline  │─────────────>│ Custom Widgets  │
│ Time-Series │              │  & Processing   │              │ & Dashboards    │
└─────────────┘              └─────────────────┘              └─────────────────┘
                                     │
                                     ▼
                             ┌─────────────────┐
                             │ Rules Engine &  │
                             │ Alert System    │
                             └─────────────────┘
```

### Data Flow Architecture
```
MQTT Message Ingestion → Device Management → Telemetry Storage
         │                                           │
         ▼                                           ▼
WebSocket Broadcasting ← Real-time Dashboard    Rules Engine
         │                                           │
         ▼                                           ▼
  Live Visualization                           Alert Generation
                                                     │
                                                     ▼
                                            Notification System
```

## IoT Data Simulation

The built-in simulator publishes synthetic smart meter telemetry when `simulator.enabled=true`:

- **10 simulated devices** by default (configurable)
- **Time-of-day consumption curves** with random jitter
- **Multiple metrics**: kW, voltage, current, power factor, frequency
- **30-second cadence** (configurable)

To disable the simulator, set `SIMULATOR_ENABLED=false` or toggle the property in `application.yml`.

## 🆕 New Features

### HTTP REST Data Ingestion
In addition to MQTT, devices can now send telemetry via HTTP REST API:

```bash
# Full telemetry ingestion
curl -X POST http://localhost:8080/api/v1/data/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "sensor-001",
    "variables": {
      "temperature": 23.5,
      "humidity": 65.2
    }
  }'

# Single variable ingestion
curl -X POST http://localhost:8080/api/v1/data/sensor-001/temperature \
  -H "Content-Type: application/json" \
  -d "23.5"
```

### Device Groups & Tags
Organize devices with flexible grouping and color-coded tags:
- Create groups like "Building A Sensors" or "Production Line 1"
- Add tags with custom colors for quick filtering
- Store custom properties per device

### Data Export
Export historical telemetry data:

```bash
# CSV export
curl "http://localhost:8080/api/v1/export/csv/sensor-001?from=2024-01-01T00:00:00Z&to=2024-01-31T23:59:59Z" \
  -o data.csv

# JSON export
curl "http://localhost:8080/api/v1/export/json/sensor-001?from=2024-01-01T00:00:00Z&to=2024-01-31T23:59:59Z" \
  -o data.json
```

### Variable Management
Define variables with rich metadata:
- Display names and descriptions
- Units (kW, °C, V, etc.)
- Min/max value constraints
- Custom colors and icons
- Decimal precision control

### Dashboard Sharing
Share dashboards with team members or publicly:
- Generate public share links
- Set user permissions (VIEW, EDIT, ADMIN)
- Configure expiration dates
- Track dashboard access logs

### Interactive API Documentation
Explore and test all APIs at: **http://localhost:8080/swagger-ui.html**

## 🔧 API Examples

### Authentication

```bash
# Register a new user and organization
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "organizationName": "My IoT Company"
  }'

# Login and get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePass123!"
  }'

# Use the token in subsequent requests
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/v1/devices
```

### Device Management

```bash
# Get all devices
curl http://localhost:8080/api/v1/devices

# Create a new device
curl -X POST http://localhost:8080/api/v1/devices \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": "sensor-001",
    "name": "Temperature Sensor",
    "location": "Building A",
    "sensorType": "temperature"
  }'

# Update a device
curl -X PUT http://localhost:8080/api/v1/devices/sensor-001 \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Sensor Name"}'
```

### Telemetry Data

```bash
# Query historical data
curl "http://localhost:8080/api/v1/data/query?deviceId=meter-001&from=2024-01-01T00:00:00Z&to=2024-01-02T00:00:00Z"

# Get latest values for multiple devices
curl "http://localhost:8080/api/v1/data/latest?deviceIds=meter-001,meter-002"

# Get latest value for single device
curl http://localhost:8080/api/v1/data/latest/meter-001
```

### Analytics & Aggregation

```bash
# Get aggregated data (hourly averages)
curl "http://localhost:8080/api/v1/analytics/aggregate?deviceId=meter-001&variable=kwConsumption&aggregation=AVG&from=2024-01-01T00:00:00Z&to=2024-01-02T00:00:00Z&interval=1h"

# Get daily maximum values
curl "http://localhost:8080/api/v1/analytics/aggregate?deviceId=meter-001&variable=voltage&aggregation=MAX&from=2024-01-01T00:00:00Z&to=2024-01-31T00:00:00Z&interval=1d"
```

### Rules Management

```bash
# Get all rules
curl http://localhost:8080/api/v1/rules

# Create a new rule
curl -X POST http://localhost:8080/api/v1/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "High Power Alert",
    "description": "Alert when power consumption exceeds 100kW",
    "deviceId": "meter-001",
    "variable": "kwConsumption",
    "operator": "GT",
    "threshold": 100.0,
    "enabled": true
  }'

# Update a rule
curl -X PUT http://localhost:8080/api/v1/rules/{ruleId} \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

### Alerts Management

```bash
# Get all alerts
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/v1/alerts

# Get only unacknowledged alerts
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:8080/api/v1/alerts?unacknowledgedOnly=true"

# Acknowledge an alert
curl -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/v1/alerts/{alertId}/acknowledge
```

### Notification Management

```bash
# Get notification preferences
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/v1/notifications/preferences

# Configure email notifications
curl -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "EMAIL",
    "enabled": true,
    "destination": "alerts@example.com",
    "minSeverity": "HIGH",
    "immediate": true
  }' \
  http://localhost:8080/api/v1/notifications/preferences

# Configure SMS notifications
curl -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "SMS",
    "enabled": true,
    "destination": "+1234567890",
    "minSeverity": "CRITICAL",
    "immediate": true
  }' \
  http://localhost:8080/api/v1/notifications/preferences

# Get notification history
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:8080/api/v1/notifications/logs?page=0&size=20"

# Get notification statistics
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/v1/notifications/stats

# Delete a notification channel
curl -X DELETE \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/v1/notifications/preferences/EMAIL
```

### Events & Audit Trail

```bash
# Get all events with filters
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:8080/api/v1/events?page=0&size=50"

# Filter by event type
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:8080/api/v1/events?eventType=ALERT_CREATED&severity=CRITICAL"

# Get recent events (last 24 hours)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:8080/api/v1/events/recent?hours=24"

# Get event statistics by type
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:8080/api/v1/events/statistics/by-type?hours=24"

# Get event statistics by severity
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  "http://localhost:8080/api/v1/events/statistics/by-severity?hours=24"
```

## MQTT Topics

- `sensorvision/devices/{deviceId}/telemetry` � Telemetry messages
- `sensorvision/devices/{deviceId}/status` � Device status updates (reserved)
- `sensorvision/devices/{deviceId}/commands` � Command channel to devices

### Sample Telemetry Message

```json
{
  "deviceId": "meter-001",
  "timestamp": "2024-01-15T10:30:00Z",
  "variables": {
    "kw_consumption": 125.5,
    "voltage": 220.1,
    "current": 0.57,
    "power_factor": 0.92,
    "frequency": 50.02
  },
  "metadata": {
    "location": "Building A - Floor 1",
    "sensor_type": "smart_meter",
    "firmware_version": "2.1.0"
  }
}
```

## Configuration

Key configuration options live in `src/main/resources/application.yml`:

```yaml
mqtt:
  broker:
    url: tcp://localhost:1883
    username: sensorvision
    password: sensorvision123

simulator:
  enabled: true
  device-count: 10
  interval-seconds: 30

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sensorvision
    username: sensorvision
    password: sensorvision123
```

Override any property with environment variables (e.g., `SPRING_DATASOURCE_URL`).

## Monitoring & Dashboards

### Prometheus

Metrics are exposed at `/actuator/prometheus` and include:

- `mqtt_messages_total` � MQTT messages processed
- `iot_kw_consumption`, `iot_voltage`, `iot_current` � Distribution summaries of telemetry values
- `iot_device_status{deviceId=...,status="ONLINE"}` � Gauge indicating last seen state per device
- JVM, HTTP, and system metrics provided by Spring Boot Actuator

### Grafana

Access Grafana at http://localhost:3000 (admin/admin123). Provisioned dashboards include:

- Real-time power consumption trends
- Device availability overview
- MQTT ingestion throughput

Dashboards and data sources are provisioned from `ops/grafana` for easy customization.

## 🎯 Use Case Flow Diagrams

### 1. Device Registration & Telemetry Flow
```mermaid
sequenceDiagram
    participant D as IoT Device
    participant M as MQTT Broker
    participant S as Spring Boot App
    participant DB as PostgreSQL
    participant W as WebSocket
    participant UI as React Dashboard

    D->>M: Publish telemetry data
    M->>S: Forward MQTT message
    S->>DB: Store telemetry record
    S->>S: Evaluate rules engine
    S->>S: Calculate synthetic variables
    S->>W: Broadcast to WebSocket clients
    W->>UI: Real-time data update

    Note over S,DB: Auto-creates device if not exists
    Note over S: Triggers alerts if rules match
```

### 2. Rule-Based Alerting Flow
```mermaid
sequenceDiagram
    participant UI as Dashboard
    participant API as REST API
    participant RE as Rules Engine
    participant AS as Alert Service
    participant DB as Database
    participant N as Notification

    UI->>API: Create monitoring rule
    API->>DB: Save rule configuration

    Note over RE: On every telemetry ingestion
    RE->>DB: Query active rules
    RE->>RE: Evaluate conditions

    alt Rule condition met
        RE->>AS: Trigger alert
        AS->>DB: Save alert record
        AS->>N: Send notification
        AS->>UI: WebSocket alert broadcast
    end
```

### 3. Analytics & Aggregation Flow
```mermaid
sequenceDiagram
    participant UI as Dashboard
    participant API as Analytics API
    participant AS as Analytics Service
    participant DB as PostgreSQL
    participant Cache as Redis (Future)

    UI->>API: Request aggregated data
    API->>AS: Process aggregation request
    AS->>DB: Query time-series data
    AS->>AS: Calculate MIN/MAX/AVG/SUM
    AS->>Cache: Cache results (Future)
    AS->>API: Return aggregated data
    API->>UI: Display charts/graphs
```

### 4. Synthetic Variables (Derived Metrics) Flow
```mermaid
sequenceDiagram
    participant T as Telemetry Ingestion
    participant SV as Synthetic Variables Service
    participant DB as Database
    participant Calc as Expression Calculator

    Note over T: After telemetry storage
    T->>SV: Calculate synthetic variables
    SV->>DB: Query active synthetic variables
    SV->>Calc: Evaluate mathematical expressions
    Calc->>SV: Return calculated values
    SV->>DB: Store synthetic variable values

    Note over Calc: Supports expressions like:<br/>"kwConsumption * voltage"<br/>"voltage / current"
```

## 🛠️ Development Guide

### Project Structure

```
├── src/main/java/org/sensorvision/
│   ├── controller/          # REST API controllers
│   │   ├── AuthController.java
│   │   ├── DeviceController.java
│   │   ├── TelemetryController.java
│   │   ├── RuleController.java
│   │   ├── AlertController.java
│   │   ├── AnalyticsController.java
│   │   ├── DashboardController.java
│   │   ├── EventController.java
│   │   └── NotificationController.java
│   ├── service/             # Business logic services
│   │   ├── UserService.java
│   │   ├── DeviceService.java
│   │   ├── TelemetryService.java
│   │   ├── TelemetryIngestionService.java
│   │   ├── RuleEngineService.java
│   │   ├── AlertService.java
│   │   ├── AnalyticsService.java
│   │   ├── SyntheticVariableService.java
│   │   ├── DashboardService.java
│   │   ├── EventService.java
│   │   ├── NotificationService.java
│   │   ├── EmailNotificationService.java
│   │   └── SmsNotificationService.java
│   ├── model/               # JPA entities
│   │   ├── User.java
│   │   ├── Organization.java
│   │   ├── Device.java
│   │   ├── TelemetryRecord.java
│   │   ├── Rule.java
│   │   ├── Alert.java
│   │   ├── SyntheticVariable.java
│   │   ├── Dashboard.java
│   │   ├── Widget.java
│   │   ├── Event.java
│   │   ├── UserNotificationPreference.java
│   │   └── NotificationLog.java
│   ├── repository/          # Spring Data repositories
│   ├── security/            # Security configuration & JWT
│   ├── websocket/           # WebSocket handling
│   ├── mqtt/                # MQTT message handling
│   ├── simulator/           # Smart meter simulator
│   ├── config/              # Spring configuration
│   └── dto/                 # Data transfer objects
├── frontend/                # React TypeScript frontend
│   ├── src/
│   │   ├── components/      # Reusable UI components
│   │   │   ├── Layout.tsx
│   │   │   ├── ProtectedRoute.tsx
│   │   │   └── RealTimeChart.tsx
│   │   ├── pages/          # Page-level components
│   │   │   ├── Login.tsx
│   │   │   ├── Register.tsx
│   │   │   ├── Dashboard.tsx
│   │   │   ├── Dashboards.tsx
│   │   │   ├── Devices.tsx
│   │   │   ├── Analytics.tsx
│   │   │   ├── Rules.tsx
│   │   │   ├── Alerts.tsx
│   │   │   ├── Events.tsx
│   │   │   └── Notifications.tsx
│   │   ├── contexts/       # React contexts (Auth)
│   │   ├── services/       # API client services
│   │   ├── hooks/          # Custom React hooks
│   │   └── types/          # TypeScript type definitions
│   └── package.json
└── src/main/resources/
    └── db/migration/        # Flyway database migrations
        ├── V1__Initial_schema.sql
        ├── V2__Add_rules_and_alerts.sql
        ├── V3__Add_synthetic_variables.sql
        ├── V4__Add_dashboards_and_widgets.sql
        ├── V5__Add_users_and_organizations.sql
        ├── V6__Add_user_roles.sql
        ├── V7__Add_events.sql
        └── V8__Add_notifications.sql
```

### Database Schema Overview

```sql
-- Authentication & Authorization
users                          # User accounts
organizations                  # Multi-tenant organizations
user_roles                     # User role assignments

-- Core IoT entities
devices                        # IoT device registry
telemetry_records             # Time-series sensor data

-- Rules & Alerting
rules                          # Monitoring rules/conditions
alerts                         # Triggered alert records

-- Analytics & Dashboards
synthetic_variables            # Calculated metric definitions
synthetic_variable_values      # Computed synthetic values
dashboards                     # Custom dashboard configurations
widgets                        # Dashboard widget definitions

-- Notifications
user_notification_preferences  # User notification settings
notification_logs              # Notification delivery history

-- Audit & Events
events                         # System-wide event log
```

### Key Technologies Used

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Backend** | Spring Boot 3.3 | REST API and business logic |
| **Database** | PostgreSQL 15 | Time-series data storage |
| **Messaging** | MQTT (Eclipse Mosquitto) | IoT device communication |
| **Real-time** | WebSocket | Live dashboard updates |
| **Frontend** | React 18 + TypeScript | Modern web interface |
| **Build Tool** | Vite | Fast frontend development |
| **Styling** | Tailwind CSS | Responsive UI design |
| **Charts** | Chart.js + react-chartjs-2 | Data visualization |
| **Monitoring** | Prometheus + Grafana | System observability |

### Testing Strategy

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Frontend tests
cd frontend
npm test
```

## 🚀 Deployment

### Docker Deployment

```bash
# Build the application
./gradlew build

# Build Docker image
docker build -t sensorvision-app .

# Start all services
docker-compose up --build

# Start frontend separately
cd frontend && npm run build
# Serve the built frontend with your preferred web server
```

### Production Deployment Checklist

#### Security Hardening
- [ ] Configure MQTT authentication and authorization
- [ ] Enable TLS/SSL for all communications (MQTT, HTTP, WebSocket)
- [ ] Set up proper firewall rules
- [ ] Use environment variables for secrets
- [ ] Implement API rate limiting
- [ ] Set up CORS properly for production domains

#### Database & Performance
- [ ] Configure PostgreSQL connection pooling
- [ ] Set up database backups (WAL archiving)
- [ ] Configure time-series data partitioning
- [ ] Add Redis for caching (future enhancement)
- [ ] Optimize database indexes for your query patterns

#### Monitoring & Observability
- [ ] Configure log aggregation (ELK stack, Loki)
- [ ] Set up application performance monitoring (APM)
- [ ] Configure alerting for system metrics
- [ ] Set up health checks for all services
- [ ] Monitor WebSocket connection health

#### Scalability
- [ ] Use container orchestration (Kubernetes, Docker Swarm)
- [ ] Configure horizontal pod autoscaling
- [ ] Set up load balancing for multiple app instances
- [ ] Implement database read replicas
- [ ] Consider MQTT broker clustering

### Environment Variables

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sensorvision
SPRING_DATASOURCE_USERNAME=sensorvision
SPRING_DATASOURCE_PASSWORD=your_secure_password

# MQTT Configuration
MQTT_BROKER_URL=tcp://localhost:1883
MQTT_BROKER_USERNAME=sensorvision
MQTT_BROKER_PASSWORD=your_mqtt_password

# Simulator Configuration
SIMULATOR_ENABLED=true
SIMULATOR_DEVICE_COUNT=10
SIMULATOR_INTERVAL_SECONDS=30

# Frontend Configuration
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws/telemetry
```

## 🔧 Troubleshooting

### Common Issues

#### Backend Issues

| Issue | Symptoms | Solution |
|-------|----------|----------|
| **MQTT Connection Failed** | No telemetry data ingestion | Check MQTT broker is running: `docker-compose ps` |
| **Database Connection Error** | App fails to start | Verify PostgreSQL container: `docker-compose logs postgres` |
| **WebSocket Not Working** | No real-time updates | Check WebSocket endpoint: `ws://localhost:8080/ws/telemetry` |
| **Rules Not Triggering** | Alerts not generated | Check rule configuration and telemetry data types |
| **High Memory Usage** | OutOfMemoryError | Increase JVM heap size: `-Xmx2g` |

#### Frontend Issues

| Issue | Symptoms | Solution |
|-------|----------|----------|
| **White Screen** | React app not loading | Check browser console for errors |
| **API Calls Failing** | Data not displaying | Verify backend is running on port 8080 |
| **WebSocket Connection Failed** | No live data | Check CORS settings and WebSocket URL |
| **Build Errors** | npm run build fails | Clear node_modules and reinstall: `rm -rf node_modules && npm install` |

#### Data Issues

| Issue | Symptoms | Solution |
|-------|----------|----------|
| **No Telemetry Data** | Empty charts/graphs | Enable simulator: `simulator.enabled=true` |
| **Synthetic Variables Not Calculating** | Missing derived metrics | Check expression syntax in synthetic variable definitions |
| **Aggregation Queries Slow** | Timeout errors | Add database indexes or reduce time range |
| **Alert Spam** | Too many alerts | Increase rule evaluation cooldown period |

### Debug Commands

```bash
# View all service logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f postgres
docker-compose logs -f mosquitto

# Backend debug logging
./gradlew bootRun --args='--logging.level.org.sensorvision=DEBUG'

# Check database connectivity
docker exec -it sensorvision-postgres psql -U sensorvision -d sensorvision

# Test MQTT connectivity
mosquitto_pub -h localhost -p 1883 -t "sensorvision/devices/test-001/telemetry" -m '{"deviceId":"test-001","timestamp":"2024-01-01T12:00:00Z","variables":{"kw_consumption":50.5}}'

# Frontend debug mode
cd frontend && npm run dev -- --debug

# Check WebSocket connection
wscat -c ws://localhost:8080/ws/telemetry
```

### Performance Tuning

#### Database Optimization
```sql
-- Add indexes for common queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_telemetry_device_time_desc
ON telemetry_records (device_id, measurement_timestamp DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_alerts_unacknowledged
ON alerts (acknowledged, created_at DESC) WHERE NOT acknowledged;

-- Analyze table statistics
ANALYZE telemetry_records;
ANALYZE devices;
ANALYZE rules;
ANALYZE alerts;
```

#### Application Tuning
```yaml
# application.yml optimizations
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
  jpa:
    hibernate:
      jdbc:
        batch_size: 100
    properties:
      hibernate:
        order_inserts: true
        order_updates: true
        jdbc.batch_versioned_data: true
```

## 🤝 Contributing

We welcome contributions! Here's how to get started:

### Development Setup

1. **Fork and Clone**
   ```bash
   git clone https://github.com/your-username/sensorvision.git
   cd sensorvision
   ```

2. **Backend Setup**
   ```bash
   ./gradlew build
   docker-compose up -d postgres mosquitto
   ./gradlew bootRun
   ```

3. **Frontend Setup**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

### Contribution Guidelines

1. 🍴 Fork the repository
2. 🌿 Create a feature branch (`git checkout -b feature/amazing-feature`)
3. 📝 Write tests for your changes
4. ✅ Ensure all tests pass (`./gradlew test && cd frontend && npm test`)
5. 📋 Update documentation if needed
6. 🚀 Create a pull request

### Code Standards

- Follow existing code style and conventions
- Add JavaDoc comments for public methods
- Write unit tests for new functionality
- Update README for significant changes
- Use meaningful commit messages

Refer to [Repository Guidelines](AGENTS.md) for detailed development guidelines.

## 📊 Current vs Ubidots Feature Comparison

| Feature | SensorVision | Ubidots | Status |
|---------|-------------|---------|---------|
| Device Management | ✅ | ✅ | Complete |
| Real-time Dashboard | ✅ | ✅ | Complete |
| Widget Dashboards | ✅ | ✅ | Complete |
| Data Visualization | ✅ | ✅ | Complete |
| Rules Engine | ✅ | ✅ | Complete |
| Alerting System | ✅ | ✅ | Complete |
| Data Aggregation | ✅ | ✅ | Complete |
| WebSocket Streaming | ✅ | ✅ | Complete |
| Synthetic Variables | ✅ | ✅ | Complete |
| User Authentication | ✅ | ✅ | Complete |
| Multi-tenancy | ✅ | ✅ | Complete |
| Role-based Access | ✅ | ✅ | Complete |
| Email Notifications | ✅ | ✅ | Complete (stub) |
| SMS Notifications | ✅ | ✅ | Complete (stub) |
| Webhook Notifications | 🔶 | ✅ | Partial |
| Events & Audit Trail | ✅ | ✅ | Complete |
| Multi-Protocol Support | 🔶 MQTT Only | ✅ HTTP/MQTT/TCP/UDP | Future |
| Mobile Apps | ❌ | ✅ | Future |
| Machine Learning | ❌ | ✅ | Future |
| Geolocation/Maps | ❌ | ✅ | Future |
| Data Export | ❌ | ✅ | Future |

**Legend:** ✅ Complete | 🔶 Partial | ❌ Not Implemented

## 🔮 Feature Roadmap

### Phase 1 - Core Platform ✅ COMPLETE
- [x] MQTT ingestion pipeline
- [x] Real-time web dashboard
- [x] Device management
- [x] Rules engine & alerting
- [x] Data aggregation & analytics
- [x] Synthetic variables
- [x] Widget-based dashboards

### Phase 2 - Authentication & Security ✅ COMPLETE
- [x] JWT-based authentication
- [x] Role-based access control (Admin, User, Viewer)
- [x] Multi-tenant architecture
- [x] Organization isolation
- [x] User registration & management
- [ ] API rate limiting
- [ ] HTTPS/WSS enforcement

### Phase 3 - Advanced Alerting ✅ COMPLETE
- [x] Multi-channel notifications (Email, SMS, Webhook, In-App)
- [x] Configurable notification preferences
- [x] Severity-based filtering
- [x] Immediate vs digest delivery
- [x] Notification history & tracking
- [x] Events & audit trail system
- [ ] Slack/Teams integration
- [ ] PagerDuty integration

### Phase 4 - Extended Protocol Support 🔄 PLANNED
- [ ] HTTP REST data ingestion
- [ ] TCP/UDP protocol support
- [ ] CoAP protocol support
- [ ] LoRaWAN integration
- [ ] Zigbee/Z-Wave support

### Phase 5 - Advanced Features 🔄 PLANNED
- [ ] Geolocation & map widgets
- [ ] Device groups & tagging
- [ ] Data export/import (CSV, JSON, Excel)
- [ ] Machine learning anomaly detection
- [ ] Predictive maintenance algorithms
- [ ] Mobile app (React Native)
- [ ] Scheduled reports

### Phase 6 - Enterprise Features 🔄 PLANNED
- [ ] White-label customization
- [ ] Advanced user permissions
- [ ] Team/department hierarchies
- [ ] Data retention policies
- [ ] Custom plugin system
- [ ] Advanced analytics & BI integration
- [ ] SSO/SAML authentication

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Ubidots** for inspiration and feature reference
- **Spring Boot** community for excellent documentation
- **React** and **Vite** teams for modern frontend tools
- **PostgreSQL** for reliable time-series data storage
- **MQTT** community for IoT messaging standards

---

<div align="center">
  <h3>🌟 SensorVision - Bringing Intelligence to IoT Monitoring 🌟</h3>
  <p><i>A modern, open-source alternative to commercial IoT platforms</i></p>

  **[🚀 Get Started](#quick-start) | [📖 Documentation](#architecture-overview) | [🤝 Contribute](#contributing) | [💬 Discussions](https://github.com/your-repo/discussions)**
</div>
