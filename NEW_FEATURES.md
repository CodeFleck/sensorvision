# 🚀 SensorVision New Features Documentation

## Table of Contents
1. [HTTP REST Data Ingestion](#http-rest-data-ingestion)
2. [Device Groups & Tags](#device-groups--tags)
3. [Geolocation & GPS Tracking](#geolocation--gps-tracking)
4. [Data Export](#data-export)
5. [Swagger/OpenAPI Documentation](#swaggeropenapi-documentation)
6. [Variable Management](#variable-management)
7. [Dashboard Sharing](#dashboard-sharing)

---

## HTTP REST Data Ingestion

### Overview
In addition to MQTT, devices can now send telemetry data via HTTP REST API. This greatly expands device compatibility.

### Endpoints

#### 1. Full Telemetry Ingestion
```http
POST /api/v1/data/ingest
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceId": "sensor-001",
  "timestamp": "2024-01-15T10:30:00Z",  // Optional, defaults to now
  "variables": {
    "kw_consumption": 125.5,
    "voltage": 220.1,
    "current": 0.57,
    "powerFactor": 0.92
  },
  "metadata": {
    "location": "Building A",
    "sensor_type": "smart_meter"
  }
}
```

**Response:**
```json
{
  "deviceId": "sensor-001",
  "timestamp": "2024-01-15T10:30:00Z",
  "variablesReceived": 4,
  "status": "success",
  "message": "Telemetry data ingested successfully"
}
```

#### 2. Single Variable Ingestion
```http
POST /api/v1/data/{deviceId}/{variableName}
Content-Type: application/json
```

**Request Body:**
```json
125.5
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/data/sensor-001/temperature \
  -H "Content-Type: application/json" \
  -d "22.5"
```

#### 3. Bulk Ingestion
```http
POST /api/v1/data/bulk
Content-Type: application/json
```

**Request Body:**
```json
[
  {
    "deviceId": "sensor-001",
    "variables": {"temperature": 22.5}
  },
  {
    "deviceId": "sensor-002",
    "variables": {"temperature": 23.1}
  }
]
```

**Response:** Array of individual responses for each device.

---

## Device Groups & Tags

### Overview
Organize devices into groups and add flexible tags for better management and filtering.

### Database Tables
- `device_groups` - Group definitions
- `device_group_members` - Device-group relationships
- `device_tags` - Tag definitions with colors
- `device_tag_assignments` - Device-tag relationships
- `device_properties` - Custom key-value properties

### Use Cases

#### Device Groups
- Organize by location: "Building A Sensors", "Factory Floor 1"
- Organize by type: "Temperature Sensors", "Smart Meters"
- Organize by project: "Q1 2024 Deployment"

#### Device Tags
- Status indicators: "critical", "maintenance-required"
- Categories: "indoor", "outdoor", "mobile"
- Custom labels with color coding: `#FF5733` for "high-priority"

#### Custom Properties
- Store device-specific metadata
- Support types: STRING, NUMBER, BOOLEAN, JSON
- Examples:
  - `installation_date`: "2024-01-15"
  - `warranty_months`: 24
  - `is_certified`: true
  - `calibration_data`: {"last_date": "2024-01-01", "next_date": "2024-07-01"}

---

## Geolocation & GPS Tracking

### Overview
Track device locations with GPS coordinates, geofencing, and location-based alerting.

### Features

#### 1. Device Location
Devices can store static GPS coordinates:
```sql
UPDATE devices SET
  latitude = 37.7749,
  longitude = -122.4194,
  altitude = 15.5,
  location_updated_at = CURRENT_TIMESTAMP
WHERE external_id = 'sensor-001';
```

#### 2. Telemetry Location (Moving Devices)
Track location changes in telemetry records for vehicles/mobile devices:
```json
{
  "deviceId": "vehicle-001",
  "variables": {"speed": 65.5},
  "latitude": 37.7749,
  "longitude": -122.4194,
  "altitude": 15.5
}
```

#### 3. Geofences
Create virtual boundaries:
- **CIRCLE**: Defined by center point and radius
- **POLYGON**: Defined by array of coordinates
- **RECTANGLE**: Defined by bounds

**Geofence Alerts:**
- Alert when device enters geofence
- Alert when device exits geofence
- Per-device configuration

#### 4. Distance Calculation
The `LocationDto` includes Haversine formula for calculating distances:
```java
LocationDto loc1 = new LocationDto(37.7749, -122.4194, null);
LocationDto loc2 = new LocationDto(37.7849, -122.4094, null);
double distanceMeters = loc1.distanceTo(loc2);
```

---

## Data Export

### Overview
Export historical telemetry data in CSV or JSON formats.

### Endpoints

#### 1. CSV Export
```http
GET /api/v1/export/csv/{deviceId}?from={timestamp}&to={timestamp}
```

**Example:**
```bash
curl "http://localhost:8080/api/v1/export/csv/sensor-001?from=2024-01-01T00:00:00Z&to=2024-01-31T23:59:59Z" \
  -o telemetry.csv
```

**Output Format:**
```csv
Timestamp,Device ID,kW Consumption,Voltage,Current,Power Factor,Frequency
2024-01-15T10:30:00Z,sensor-001,125.5,220.1,0.57,0.92,50.02
2024-01-15T10:31:00Z,sensor-001,126.2,220.3,0.58,0.91,50.01
```

#### 2. JSON Export
```http
GET /api/v1/export/json/{deviceId}?from={timestamp}&to={timestamp}
```

**Example:**
```bash
curl "http://localhost:8080/api/v1/export/json/sensor-001?from=2024-01-01T00:00:00Z&to=2024-01-31T23:59:59Z" \
  -o telemetry.json
```

**Output:** Array of telemetry record objects with full metadata.

---

## Swagger/OpenAPI Documentation

### Overview
Interactive API documentation with built-in testing capabilities.

### Access
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

### Features
- Try out APIs directly from the browser
- JWT authentication support (click "Authorize" button)
- Request/response examples
- Schema definitions
- Error response documentation

### Authentication
1. Login via `/api/v1/auth/login` to get JWT token
2. Click "Authorize" button in Swagger UI
3. Enter: `Bearer <your-jwt-token>`
4. All subsequent requests will include authentication

---

## Variable Management

### Overview
Define variables with rich metadata including units, display names, icons, and value ranges.

### Database Tables
- `variables` - Variable definitions with metadata
- `device_variables` - Device-specific variable configurations

### Features

#### System Variables
Pre-defined variables:
- `kwConsumption` - Power Consumption (kW)
- `voltage` - Voltage (V)
- `current` - Current (A)
- `powerFactor` - Power Factor (unitless)
- `frequency` - Frequency (Hz)

#### Custom Variables
Create your own variables with:
- `name`: Internal identifier
- `display_name`: User-friendly name
- `description`: Detailed explanation
- `unit`: Measurement unit (°C, kW, m/s, etc.)
- `data_type`: NUMBER, BOOLEAN, STRING, JSON
- `icon`: Icon name or URL
- `color`: Hex color for visualization
- `min_value` / `max_value`: Value constraints
- `decimal_places`: Precision control

#### Device-Specific Overrides
Customize variables per device:
- Override display name
- Override unit (e.g., °F instead of °C)
- Override min/max values
- Enable/disable specific variables
- Track last value and timestamp

**Example Use Case:**
Global variable "temperature" in Celsius, but device in US uses Fahrenheit override.

---

## Dashboard Sharing

### Overview
Share dashboards publicly or with specific users, with granular permissions.

### Features

#### 1. Public Sharing
Generate shareable public links:
```sql
UPDATE dashboards SET
  is_public = true,
  public_share_token = 'abc123xyz789',
  allow_anonymous_view = true,
  share_expires_at = '2024-12-31T23:59:59Z'
WHERE id = 1;
```

**Public URL:** `http://localhost:3001/public/dashboard/abc123xyz789`

#### 2. User Permissions
Grant specific users access:
- **VIEW**: Read-only access
- **EDIT**: Can modify widgets and layout
- **ADMIN**: Full control including permissions

#### 3. Access Logging
Track dashboard access:
- User ID (or anonymous)
- Access type (VIEW, EDIT, SHARE, EXPORT)
- Access method (WEB, MOBILE, API, PUBLIC_LINK)
- IP address and user agent
- Timestamp

#### 4. Dashboard Templates
Pre-configured dashboard templates by category:
- **ENERGY**: Power monitoring dashboards
- **INDUSTRIAL**: Manufacturing monitoring
- **SMART_HOME**: Home automation
- **AGRICULTURE**: Farm monitoring
- **FLEET**: Vehicle tracking

---

## API Testing Examples

### Test HTTP Ingestion
```bash
# Full ingestion
curl -X POST http://localhost:8080/api/v1/data/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "test-sensor-001",
    "variables": {
      "kw_consumption": 42.5,
      "voltage": 220.0
    }
  }'

# Single variable
curl -X POST http://localhost:8080/api/v1/data/test-sensor-001/temperature \
  -H "Content-Type: application/json" \
  -d "23.5"
```

### Test Data Export
```bash
# CSV export
curl "http://localhost:8080/api/v1/export/csv/test-sensor-001?from=2024-01-01T00:00:00Z&to=2024-12-31T23:59:59Z" \
  -o data.csv

# JSON export
curl "http://localhost:8080/api/v1/export/json/test-sensor-001?from=2024-01-01T00:00:00Z&to=2024-12-31T23:59:59Z" \
  -o data.json
```

---

## Database Migrations

All features are implemented via Flyway migrations:
- **V10**: Device Groups and Tags
- **V11**: Geolocation Support
- **V12**: Variable Management
- **V13**: Dashboard Sharing

Migrations run automatically on application startup.

---

## Next Steps

1. **Access Swagger UI**: http://localhost:8080/swagger-ui.html
2. **Test HTTP Ingestion**: Send test data via REST API
3. **Create Device Groups**: Organize your devices
4. **Add GPS Coordinates**: Enable location tracking
5. **Export Data**: Download historical data
6. **Share Dashboards**: Create public dashboard links

---

## Support

For questions or issues:
- GitHub Issues: https://github.com/your-org/sensorvision/issues
- Documentation: https://docs.sensorvision.io
- Email: support@sensorvision.io
