# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Backend (Spring Boot)
```bash
# Build and test
./gradlew clean build                    # Full build with tests
./gradlew clean build -x test            # Build without running tests (faster)
./gradlew bootRun                        # Run application for development
./gradlew test                           # Run test suite
./gradlew test --tests "*DeviceService*" # Run specific test class
./gradlew test --info                    # Detailed test output for debugging
./gradlew test --tests "*DeviceService*" --info  # Specific test with detailed output

# Code coverage
./gradlew test jacocoTestReport          # Generate code coverage report
# View coverage report at: build/reports/jacoco/test/html/index.html

# Docker services (required for development)
docker-compose up -d                     # Start PostgreSQL, MQTT, Prometheus, Grafana
docker-compose down                      # Stop all services
docker-compose logs postgres             # View specific service logs
docker-compose logs mosquitto
docker-compose ps                        # Check running containers
```

### Frontend (React + TypeScript)
```bash
cd frontend
npm install                              # Install dependencies
npm run dev                              # Start development server (port 3001)
npm run build                            # Production build
npm run preview                          # Preview production build
```

### Development Workflow
```bash
# Full development setup
docker-compose up -d                     # Start infrastructure
./gradlew bootRun                        # Terminal 1: Backend
cd frontend && npm run dev               # Terminal 2: Frontend
```

### Testing MQTT Integration
```bash
# Publish test telemetry message
mosquitto_pub -h localhost -p 1883 -t "sensorvision/devices/test-001/telemetry" -m '{
  "deviceId": "test-001",
  "timestamp": "2024-01-01T12:00:00Z",
  "variables": {
    "kw_consumption": 50.5,
    "voltage": 220.1,
    "current": 0.57
  }
}'
```

## Architecture Overview

SensorVision is a comprehensive IoT monitoring platform built as a Spring Boot backend with React frontend, featuring real-time data processing, alerting, and analytics.

### Core Data Flow
```
MQTT Telemetry → TelemetryIngestionService → [Database Storage + Rules Engine + WebSocket Broadcast]
```

The `TelemetryIngestionService` is the central orchestrator that:
1. Processes incoming MQTT messages
2. Creates/updates device records automatically
3. Stores telemetry data with time-series optimization
4. Evaluates rules engine for alerting
5. Calculates synthetic variables (derived metrics)
6. Broadcasts real-time updates via WebSocket
7. Updates Prometheus metrics

### Key Service Dependencies
- **TelemetryIngestionService**: Coordinates all telemetry processing
- **RuleEngineService**: Evaluates conditional rules and triggers alerts
- **SyntheticVariableService**: Calculates derived metrics using expression engine
- **TelemetryWebSocketHandler**: Manages real-time WebSocket connections
- **AnalyticsService**: Provides data aggregation (MIN/MAX/AVG/SUM) with time intervals

### Database Schema Evolution
The system uses Flyway migrations with main schemas:
- **V1**: Core devices and telemetry_records tables
- **V2**: Rules engine (rules, alerts tables)
- **V3**: Synthetic variables (synthetic_variables, synthetic_variable_values tables)
- **V56**: Dynamic variables EAV pattern (variables extended with device_id, variable_values table)

### Frontend Architecture
React SPA with TypeScript organized by feature:
- **Real-time Dashboard**: WebSocket integration with Chart.js visualization
- **Device Management**: Full CRUD with modal forms
- **Rules & Alerts**: Conditional monitoring configuration
- **Analytics**: Historical data aggregation and charting

The frontend connects via:
- REST API calls to `/api/v1/*` endpoints
- WebSocket connection to `/ws/telemetry` for live updates
- Vite proxy configuration routes API calls to Spring Boot (port 8080)

## Configuration Notes

### MQTT Topics Structure
```
sensorvision/devices/{deviceId}/telemetry    # Telemetry data ingestion
sensorvision/devices/{deviceId}/status       # Device status updates (reserved)
sensorvision/devices/{deviceId}/commands     # Command channel (reserved)
```

### Key Application Properties
- `simulator.enabled=true`: Enables built-in smart meter simulator with configurable device count
- `mqtt.broker.url`: MQTT broker connection (defaults to tcp://localhost:1883)
- Database migrations automatically run on startup via Flyway

### WebSocket Real-time Features
- Live telemetry data streaming to dashboard charts
- Real-time device status updates
- Alert notifications (future enhancement)

## Development Patterns

### Adding New Telemetry Variables

**Option 1: Dynamic Variables (EAV Pattern) - Recommended**
No code changes required! Simply send any variable in the telemetry payload:
```json
{
  "deviceId": "my-device",
  "timestamp": "2024-01-01T12:00:00Z",
  "variables": {
    "temperature": 23.5,
    "humidity": 65.0,
    "custom_sensor_xyz": 100.0
  }
}
```
Variables are auto-provisioned on first use. Access via:
- REST API: `GET /api/v1/devices/{deviceId}/variables`
- WebSocket: Subscribe to dynamic telemetry updates

**Option 2: Fixed Schema (Legacy)**
For variables requiring special handling:
1. Update `TelemetryRecord` entity with new field
2. Create Flyway migration to add database column
3. Modify `TelemetryIngestionService` to process new variable
4. Update `TelemetryPointDto` for API responses
5. Add frontend visualization in charts/dashboard

### Rules Engine Extension
The rules engine supports operators: GT, GTE, LT, LTE, EQ with automatic severity calculation based on threshold deviation. Rules are evaluated on every telemetry ingestion with 5-minute cooldown to prevent alert spam.

### Synthetic Variables
Mathematical expressions like "kwConsumption * voltage" are parsed and calculated automatically. The expression engine supports basic arithmetic operations and references to telemetry variables.

### Dynamic Variables (EAV Pattern)
The system supports Ubidots-like dynamic variable auto-provisioning using the Entity-Attribute-Value pattern.

**Key Components:**
- **DynamicVariableService**: Auto-provisions variables when new telemetry arrives
- **Variable entity**: Extended with `device_id` for device-specific variables
- **VariableValue entity**: Time-series storage for variable values
- **DeviceVariableController**: REST API for variable management

**How It Works:**
1. Device sends telemetry with any variables in the `variables` map
2. `TelemetryIngestionService` calls `DynamicVariableService.processTelemetry()`
3. For each variable, `getOrCreateVariable()` finds or auto-provisions the variable
4. Values are stored in `variable_values` table with timestamps
5. `lastValue` and `lastValueAt` cached on Variable entity for quick access
6. WebSocket broadcasts include all dynamic variables

**REST API Endpoints:**
```
GET  /api/v1/devices/{deviceId}/variables              # List all variables
GET  /api/v1/devices/{deviceId}/variables/latest       # Get latest values map
GET  /api/v1/devices/{deviceId}/variables/{id}         # Get specific variable
GET  /api/v1/devices/{deviceId}/variables/{id}/values  # Get time-series history
GET  /api/v1/devices/{deviceId}/variables/{id}/values/latest?count=100  # Latest N values
GET  /api/v1/devices/{deviceId}/variables/{id}/statistics  # Aggregated stats
PUT  /api/v1/devices/{deviceId}/variables/{id}         # Update variable metadata
```

**Race Condition Handling:**
`getOrCreateVariable()` handles concurrent variable creation:
```java
try {
    return variableRepository.save(newVariable);
} catch (DataIntegrityViolationException e) {
    // Another thread created it first, fetch existing
    return variableRepository.findByDeviceAndName(device, name).orElseThrow();
}
```

**Prometheus Metrics:**
Dynamic gauges are created for each variable with cardinality limit (MAX_DYNAMIC_GAUGES=1000):
- Metric name: `iot_dynamic_{sanitized_variable_name}`
- Tags: `deviceId`, `variable`

### Rate Limiting
The application uses rate limiting to protect against abuse:
- **Implementation**: `RateLimitInterceptor` in `WebMvcConfig.java`
- **Limit**: 10 requests per minute per authenticated user
- **Applies to**: Most `/api/v1/**` endpoints
- **Excludes**:
  - `/api/v1/auth/**` (authentication endpoints)
  - `/api/v1/devices/**` (high-frequency device reads)
  - `/api/v1/data/**` (telemetry data queries)
  - `/api/v1/ingest/**` (IoT data ingestion)
  - `/api/v1/actuator/**` (health/metrics)
- **Error Response**: HTTP 429 with JSON error message

**Important for Development:**
- React StrictMode causes double useEffect calls, which can trigger rate limits
- If you get 429 errors during development, check `WebMvcConfig.java` exclusions
- Read endpoints for real-time dashboards should be excluded from rate limiting

### Testing Strategy
- Unit tests for services with mocked dependencies
- Integration tests using `@SpringBootTest` with H2 database
- MQTT integration tests should use test profile to avoid Docker dependency
- Frontend testing via npm test (Jest/React Testing Library)
- This project is already deployed to PROD. Project urls: https://github.com/CodeFleck/sensorvision, http://35.88.65.186.nip.io:8080/

## Production Operations

### Accessing Production Logs

**Backend Application Logs:**
```bash
# View backend logs
docker logs sensorvision-backend

# Follow logs in real-time
docker logs -f sensorvision-backend

# View last 100 lines
docker logs --tail 100 sensorvision-backend

# View logs with timestamps
docker logs -t sensorvision-backend

# Search for errors
docker logs --tail 200 sensorvision-backend | grep -i "error\|exception"

# Search for telemetry ingestion
docker logs --tail 200 sensorvision-backend | grep -i "telemetry\|ingestion"

# View logs from mounted volume (if available)
cat ./logs/sensorvision.log
tail -f ./logs/sensorvision.log
```

**MQTT Broker Logs:**
```bash
# View MQTT broker logs
docker logs sensorvision-mosquitto

# Follow MQTT logs in real-time
docker logs -f sensorvision-mosquitto

# Monitor MQTT messages (requires mosquitto-clients)
mosquitto_sub -h 35.88.65.186 -p 1883 -t "#" -v
```

**Database Logs:**
```bash
# View PostgreSQL logs
docker logs sensorvision-postgres

# Follow database logs
docker logs -f sensorvision-postgres
```

### Production Troubleshooting

**Check Service Health:**
```bash
# Backend health check
curl -s http://35.88.65.186.nip.io:8080/actuator/health

# View detailed health info
curl -s http://35.88.65.186.nip.io:8080/actuator/health | jq

# Check metrics
curl -s http://35.88.65.186.nip.io:8080/actuator/metrics
```

**Test MQTT Connectivity:**
```bash
# Test MQTT port accessibility from external network
telnet 35.88.65.186 1883

# Subscribe to all MQTT topics (requires mosquitto-clients)
mosquitto_sub -h 35.88.65.186 -p 1883 -t "#" -v

# Publish test message
mosquitto_pub -h 35.88.65.186 -p 1883 \
  -t "sensorvision/devices/test-001/telemetry" \
  -m '{"deviceId":"test-001","timestamp":"2024-01-01T12:00:00Z","variables":{"temperature":23.5}}'
```

**Common Production Issues:**

1. **MQTT Port Not Accessible:**
   - Issue: Customers can send data locally but not from remote devices
   - Solution: Ensure port 1883 is open in AWS Security Group/Firewall
   - Verify: `telnet 35.88.65.186 1883` from external network

2. **Integration Wizard URLs:**
   - Development: Uses `http://localhost:8080`
   - Production: Uses `http://35.88.65.186.nip.io:8080`
   - MQTT: Strips `.nip.io` suffix to get raw IP for broker connection

3. **WebSocket Connectivity:**
   - WebSocket endpoint: `ws://35.88.65.186.nip.io:8080/ws/telemetry`
   - Check browser console for connection errors
   - Verify backend logs for WebSocket handshake issues

### Production Deployment Checklist

**Network & Security:**
- [ ] Port 8080 (HTTP API) is open in security group/firewall
- [ ] Port 1883 (MQTT) is open for external device connections
- [ ] Port 5432 (PostgreSQL) is restricted to backend container only
- [ ] SSL/TLS certificates configured (if using HTTPS)
- [ ] CORS settings configured for frontend domain

**MQTT Broker:**
- [ ] MQTT broker is accessible from external network
- [ ] Test connectivity: `telnet <server-ip> 1883`
- [ ] MQTT authentication configured (if `MQTT_DEVICE_AUTH_REQUIRED=true`)
- [ ] MQTT topics configured: `sensorvision/devices/+/telemetry`

**Environment Variables:**
- [ ] `JWT_SECRET` set to secure random value
- [ ] `DB_PASSWORD` changed from default
- [ ] `MQTT_PASSWORD` changed from default (if auth enabled)
- [ ] `APP_BASE_URL` set to production domain
- [ ] `OAUTH2_REDIRECT_BASE_URL` set to production domain

**Monitoring:**
- [ ] Prometheus metrics endpoint accessible: `/actuator/metrics`
- [ ] Health check endpoint working: `/actuator/health`
- [ ] Log rotation configured for `./logs/sensorvision.log`
- [ ] Disk space monitoring for PostgreSQL volume

**Integration Wizard:**
- [ ] Verify generated code uses correct production URL
- [ ] Verify MQTT server address is correct (IP, not .nip.io)
- [ ] Test connection from Integration Wizard works
- [ ] SDK examples updated with production URL guidance

## Official SDKs and Integration Tools

### Python SDK (`sensorvision-sdk/`)
Located in the repository root, provides a production-ready Python client:

```bash
# Development and testing
cd sensorvision-sdk
pip install -e .                    # Install in editable mode
pytest                              # Run tests
pytest --cov=sensorvision          # Run with coverage
python -m pytest -v                 # Verbose output
```

**Key Files:**
- `sensorvision/client.py` - Main client implementation with retry logic
- `sensorvision/errors.py` - Custom exception classes
- `examples/` - Usage examples (basic, advanced, error handling)
- `tests/` - Comprehensive test suite

**Implementation Notes:**
- Uses configurable retry with exponential backoff (not hardcoded!)
- `_send_data_with_retry()` method honors `self.config.retry_attempts` and `self.config.retry_delay`
- Selective retry: doesn't retry auth/validation errors, only network/timeout errors

### JavaScript/TypeScript SDK (`sensorvision-sdk-js/`)
Cross-platform SDK supporting Node.js and browsers:

```bash
# Development and building
cd sensorvision-sdk-js
npm install                         # Install dependencies
npm run build                       # Build all formats (CJS, ESM, UMD)
npm test                            # Run tests
npm run lint                        # Lint code
```

**Key Files:**
- `src/client.ts` - REST client for telemetry ingestion
- `src/websocket.ts` - Cross-platform WebSocket client
- `src/types.ts` - TypeScript type definitions
- `src/errors.ts` - Error classes
- `examples/` - Node.js and browser examples

**Critical Implementation Details:**
- **Cross-platform WebSocket**: `websocket.ts` uses environment detection (`isBrowser`) to:
  - Load correct WebSocket constructor (native vs 'ws' package)
  - Use correct event API (addEventListener vs .on)
  - Extract message data correctly (event.data vs raw data)
- **Message Format Transformation**: `handleMessage()` transforms backend's flat TelemetryPointDto to SDK's nested format
- **Subscription Messages**: `subscribe()` and `unsubscribe()` send proper messages to backend
- **Package Entry Points**: Build includes `copy:entries` script to match declared paths

### Frontend Integration Wizard (`frontend/src/pages/IntegrationWizard.tsx`)
Interactive 5-step wizard for device onboarding:

**Key Features:**
- Platform selection (Python/JavaScript)
- Device setup (new or existing)
- Smart token management:
  - New devices: generate token
  - Existing devices: check token, rotate if needed
  - Proper error handling for all cases
- Live code generation with actual credentials
- Real-time WebSocket connection testing

**Important Logic:**
- `handleDeviceSetup()` method uses `getDeviceTokenInfo()` to check existing tokens
- Calls `rotateDeviceToken()` for existing devices (not `generateDeviceToken()` which would fail with 400)
- Generates new token only for truly new devices
- `generateCode()` creates runnable code snippets for selected platform

**Navigation:**
- Route: `/integration-wizard`
- Added to `frontend/src/App.tsx` routing
- Link added to main navigation in `frontend/src/components/Layout.tsx`

## Development Patterns for SDKs

### Adding New Features to Python SDK
1. Update `sensorvision/client.py` with new method
2. Add type hints and docstrings
3. Add error handling
4. Create example in `examples/`
5. Add tests in `tests/`
6. Update `README.md`

### Adding New Features to JavaScript SDK
1. Update TypeScript source in `src/`
2. Export new functionality from `src/index.ts`
3. Build: `npm run build`
4. Test cross-platform (Node.js and browser)
5. Add examples in `examples/`
6. Update `README.md`

### Modifying Integration Wizard
1. Update `frontend/src/pages/IntegrationWizard.tsx`
2. Test all 5 steps thoroughly
3. Verify both new and existing device flows
4. Test code generation for both platforms
5. Verify WebSocket connection test works
- all tests must pass before merging into main