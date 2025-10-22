# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Backend (Spring Boot)
```bash
# Build and test
./gradlew clean build                    # Full build with tests
./gradlew bootRun                        # Run application for development
./gradlew test                           # Run test suite
./gradlew test --tests "*DeviceService*" # Run specific test class
./gradlew test --info                    # Detailed test output for debugging

# Docker services (required for development)
docker-compose up -d                     # Start PostgreSQL, MQTT, Prometheus, Grafana
docker-compose down                      # Stop all services
docker-compose logs postgres             # View specific service logs
docker-compose logs mosquitto
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
The system uses Flyway migrations with three main schemas:
- **V1**: Core devices and telemetry_records tables
- **V2**: Rules engine (rules, alerts tables)
- **V3**: Synthetic variables (synthetic_variables, synthetic_variable_values tables)

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
1. Update `TelemetryRecord` entity with new field
2. Create Flyway migration to add database column
3. Modify `TelemetryIngestionService` to process new variable
4. Update `TelemetryPointDto` for API responses
5. Add frontend visualization in charts/dashboard

### Rules Engine Extension
The rules engine supports operators: GT, GTE, LT, LTE, EQ with automatic severity calculation based on threshold deviation. Rules are evaluated on every telemetry ingestion with 5-minute cooldown to prevent alert spam.

### Synthetic Variables
Mathematical expressions like "kwConsumption * voltage" are parsed and calculated automatically. The expression engine supports basic arithmetic operations and references to telemetry variables.

### Testing Strategy
- Unit tests for services with mocked dependencies
- Integration tests using `@SpringBootTest` with H2 database
- MQTT integration tests should use test profile to avoid Docker dependency
- Frontend testing via npm test (Jest/React Testing Library)
- This project is already deployed to PROD. Project urls: https://github.com/CodeFleck/sensorvision, http://35.88.65.186.nip.io:8080/