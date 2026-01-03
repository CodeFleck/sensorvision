# Data Plugins Guide

Data Plugins enable Industrial Cloud to ingest telemetry data from various external sources and protocols beyond the standard MQTT/HTTP ingestion endpoints.

## Overview

The plugin system provides a flexible, extensible architecture for integrating with:
- **Webhook-based IoT platforms** (LoRaWAN, Sigfox, Particle Cloud)
- **Protocol parsers** (Modbus TCP, custom binary protocols)
- **External integrations** (pull data from 3rd party APIs)
- **Bulk imports** (CSV file uploads)

## Architecture

### Components

1. **Plugin Processors** (`org.indcloud.plugin.*`)
   - Base classes for different plugin types
   - Provider-specific implementations (LoRaWAN TTN, HTTP Webhook, CSV Import)

2. **Database Models** (`org.indcloud.model.DataPlugin`)
   - Plugin configuration storage
   - Execution history tracking
   - Multi-tenant isolation

3. **Webhook Receiver** (`/api/v1/webhooks/{orgId}/{pluginName}`)
   - Public endpoint for receiving external webhooks
   - Routes data to configured plugins
   - Records execution metrics

4. **Management API** (`/api/v1/plugins`)
   - CRUD operations for plugin configurations
   - Enable/disable plugins
   - View execution history

5. **Frontend UI** (`/data-plugins`)
   - Plugin management interface
   - Configuration editor with JSON validation
   - Execution history viewer

### Data Flow

```
External Source → Webhook Endpoint → Plugin Processor → Telemetry Ingestion → Database
                                   ↓
                            Execution History
```

## Plugin Types

### 1. Webhook Plugins

Receive HTTP POST webhooks from external systems and transform them into Industrial Cloud telemetry data.

**Supported Providers:**
- **LoRaWAN (The Things Network)** - Parse TTN uplink messages
- **HTTP Webhook** - Generic webhook receiver with configurable field mapping
- **Sigfox** - Parse Sigfox callbacks with custom binary decoder
- **MQTT Bridge** - Subscribe to external MQTT brokers
- **Particle Cloud** - Parse Particle webhook events (planned)

### 2. Protocol Parser Plugins

Parse binary or custom protocol data into telemetry records.

**Supported Providers:**
- **Modbus TCP** - Poll Modbus devices and parse registers (planned)
- **Custom Parser** - User-defined JavaScript/Python decoder functions (planned)

### 3. Integration Plugins

Pull data from external APIs on a schedule.

**Status:** Planned for future releases

### 4. CSV Import Plugins

Import historical telemetry data from CSV files.

**Status:** Partially implemented

## Configuration

### Plugin Configuration Schema

Each plugin requires:

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Unique plugin name (used in webhook URL) |
| `description` | String | Optional description |
| `pluginType` | Enum | `WEBHOOK`, `PROTOCOL_PARSER`, `INTEGRATION`, `CSV_IMPORT` |
| `provider` | Enum | Specific provider implementation |
| `enabled` | Boolean | Whether plugin is active |
| `configuration` | JSON | Provider-specific configuration |

### Provider-Specific Configuration

#### LoRaWAN TTN

```json
{
  "deviceIdPrefix": "",
  "deviceIdSuffix": ""
}
```

**Fields:**
- `deviceIdPrefix` (optional) - Prefix to add to TTN device IDs
- `deviceIdSuffix` (optional) - Suffix to add to TTN device IDs

**Example:**
If TTN device ID is `sensor-001` and prefix is `ttn-`, the resulting device ID will be `ttn-sensor-001`.

#### HTTP Webhook

```json
{
  "deviceIdField": "deviceId",
  "timestampField": "timestamp",
  "variablesField": "variables",
  "metadataField": "metadata"
}
```

**Fields:**
- `deviceIdField` - JSON path to device ID in webhook payload
- `timestampField` - JSON path to timestamp (ISO 8601 format)
- `variablesField` - JSON path to telemetry variables object
- `metadataField` - JSON path to optional metadata object

## Usage Guide

### Creating a Plugin

1. Navigate to **Data Plugins** in the sidebar (Admin only)
2. Click **New Plugin**
3. Fill in the form:
   - **Name**: Unique identifier (lowercase, hyphens allowed)
   - **Description**: Optional description
   - **Provider**: Select from available providers
   - **Configuration**: Provider-specific JSON config
   - **Enabled**: Check to activate immediately
4. Click **Create**

### Webhook URL Format

Once created, the plugin webhook URL will be:

```
https://your-domain.com/api/v1/webhooks/{organizationId}/{pluginName}
```

**Example:**
- Organization ID: `1`
- Plugin name: `my-lorawan-integration`
- URL: `https://indcloud.example.com/api/v1/webhooks/1/my-lorawan-integration`

### Viewing Execution History

1. Go to **Data Plugins**
2. Click the **History** icon (clock) next to a plugin
3. View execution logs:
   - Timestamp
   - Status (SUCCESS, FAILED, PARTIAL)
   - Records processed
   - Duration
   - Error messages (if any)

### Testing a Plugin

Send a test webhook:

```bash
curl -X POST https://your-domain.com/api/v1/webhooks/1/my-plugin \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "test-device",
    "timestamp": "2024-01-01T12:00:00Z",
    "variables": {
      "temperature": 23.5,
      "humidity": 65.2
    }
  }'
```

Check execution history for results.

## Pre-Built Plugins

### LoRaWAN (The Things Network)

**Status:** ✅ Production Ready

Full integration guide available in [LORAWAN_TTN_INTEGRATION.md](LORAWAN_TTN_INTEGRATION.md)

**Features:**
- Parses TTN v3 uplink messages
- Extracts device ID from `end_device_ids.device_id`
- Uses `received_at` timestamp
- Reads telemetry from `uplink_message.decoded_payload`
- Captures LoRaWAN metadata (RSSI, SNR, spreading factor)

### HTTP Webhook

**Status:** ✅ Production Ready

Generic webhook receiver with configurable field mapping.

**Use Cases:**
- Custom IoT platforms
- Internal services sending telemetry
- Third-party integrations

### Modbus TCP

**Status:** ✅ Production Ready (Sprint 3)

Poll Modbus TCP devices and parse registers into telemetry variables.

**Configuration Example:**
```json
{
  "host": "192.168.1.100",
  "port": 502,
  "unitId": 1,
  "deviceId": "plc-001",
  "pollingIntervalSeconds": 60,
  "timeout": 3000,
  "registers": [
    {
      "type": "HOLDING",
      "address": 40001,
      "count": 1,
      "variableName": "temperature",
      "dataType": "INT16",
      "scale": 0.1,
      "offset": -40.0
    },
    {
      "type": "INPUT",
      "address": 30001,
      "count": 2,
      "variableName": "pressure",
      "dataType": "FLOAT32"
    }
  ]
}
```

**Supported Data Types:**
- `INT16`: 16-bit signed integer (1 register)
- `UINT16`: 16-bit unsigned integer (1 register)
- `INT32`: 32-bit signed integer (2 registers, big-endian)
- `UINT32`: 32-bit unsigned integer (2 registers, big-endian)
- `FLOAT32`: 32-bit IEEE 754 floating point (2 registers, big-endian)

**Address Conventions:**
- Supports both Modicon (40001+, 30001+) and 0-based addressing
- Automatically converts Modicon addresses to 0-based for protocol

**Features:**
- Configurable polling interval (1-86400 seconds)
- Scale and offset transformations
- Support for both holding and input registers
- Connection timeout configuration
- Automatic retry on connection failure

### Sigfox

**Status:** ✅ Production Ready (Sprint 3)

Parse Sigfox callback webhooks with custom binary payload decoder.

**Configuration Example:**
```json
{
  "deviceIdPrefix": "sigfox-",
  "includeMetadata": true,
  "dataParser": "custom",
  "customVariables": [
    {
      "name": "temperature",
      "byteOffset": 0,
      "dataType": "INT16",
      "scale": 0.1
    },
    {
      "name": "humidity",
      "byteOffset": 2,
      "dataType": "UINT8",
      "scale": 1.0
    },
    {
      "name": "battery",
      "byteOffset": 3,
      "dataType": "UINT8",
      "scale": 0.01
    }
  ]
}
```

**Sigfox Webhook Format:**
Sigfox sends callbacks in this format:
```json
{
  "device": "AABBCC",
  "time": 1609459200,
  "data": "00FA3C64",
  "seqNumber": 123,
  "avgSnr": "15.50",
  "rssi": "-120.00",
  "lat": "48.8566",
  "lng": "2.3522"
}
```

**Supported Data Types:**
- `UINT8`: 8-bit unsigned integer (1 byte)
- `INT8`: 8-bit signed integer (1 byte)
- `UINT16`: 16-bit unsigned integer (2 bytes, big-endian)
- `INT16`: 16-bit signed integer (2 bytes, big-endian)
- `UINT32`: 32-bit unsigned integer (4 bytes, big-endian)
- `INT32`: 32-bit signed integer (4 bytes, big-endian)
- `FLOAT32`: 32-bit IEEE 754 float (4 bytes, big-endian)

**Features:**
- Custom binary payload parsing with byte offset and data type
- Scale factors for unit conversion
- Automatic metadata extraction (RSSI, SNR, location)
- Device ID prefix/suffix customization
- Sigfox payload size limit: 12 bytes

**Webhook URL:**
Configure in Sigfox backend:
```
https://your-domain.com/api/v1/webhooks/{organizationId}/{pluginName}
```

### MQTT Bridge

**Status:** ✅ Production Ready (Sprint 3)

Connect to external MQTT brokers and forward messages to SensorVision.

**Configuration Example:**
```json
{
  "brokerUrl": "tcp://mqtt.example.com:1883",
  "clientId": "sensorvision-bridge",
  "username": "mqtt_user",
  "password": "mqtt_pass",
  "cleanSession": true,
  "qos": 1,
  "connectionTimeoutSeconds": 30,
  "keepAliveSeconds": 60,
  "topics": [
    {
      "topic": "devices/+/telemetry",
      "deviceIdExtractor": "payload",
      "payloadFormat": "json"
    },
    {
      "topic": "warehouse/sensors/#",
      "deviceIdExtractor": "payload",
      "payloadFormat": "json"
    }
  ],
  "deviceIdField": "deviceId",
  "timestampField": "timestamp",
  "variablesField": "variables"
}
```

**Broker URL Formats:**
- TCP: `tcp://broker.example.com:1883`
- SSL/TLS: `ssl://broker.example.com:8883`

**Topic Wildcards:**
- `+` - Single level wildcard (e.g., `devices/+/telemetry`)
- `#` - Multi-level wildcard (e.g., `warehouse/#`)

**Message Format:**
Expected JSON payload:
```json
{
  "deviceId": "sensor-001",
  "timestamp": "2024-01-01T12:00:00Z",
  "variables": {
    "temperature": 23.5,
    "humidity": 65.0
  }
}
```

**Features:**
- Subscribe to multiple topics simultaneously
- QoS 0, 1, or 2 support
- Automatic reconnection on connection loss
- SSL/TLS support for secure connections
- Configurable keep-alive and timeouts
- Message queue buffering
- Wildcard topic subscriptions

**Use Cases:**
- Bridge external MQTT infrastructure to SensorVision
- Integrate with existing IoT deployments
- Migrate from other MQTT-based platforms
- Connect to cloud MQTT brokers (AWS IoT, Azure IoT Hub, etc.)

### CSV Import

**Status:** ⚠️ Partially Implemented

Import historical telemetry data from CSV files.

## Development

### Creating a Custom Plugin

To add a new plugin provider:

1. **Create Processor Class**

```java
@Component
public class MyCustomPlugin extends BaseWebhookPlugin {

    @Override
    protected String extractDeviceId(JsonNode payload, JsonNode config) {
        // Extract device ID from payload
        return payload.path("device").asText();
    }

    @Override
    protected Instant extractTimestamp(JsonNode payload, JsonNode config) {
        // Extract timestamp
        return Instant.parse(payload.path("time").asText());
    }

    @Override
    protected Map<String, BigDecimal> extractVariables(JsonNode payload, JsonNode config) {
        // Extract telemetry variables
        Map<String, BigDecimal> variables = new HashMap<>();
        JsonNode data = payload.path("data");
        data.fields().forEachRemaining(entry -> {
            if (entry.getValue().isNumber()) {
                variables.put(entry.getKey(), entry.getValue().decimalValue());
            }
        });
        return variables;
    }

    @Override
    public PluginProvider getSupportedProvider() {
        return PluginProvider.MY_CUSTOM;
    }
}
```

2. **Add Provider Enum**

Update `PluginProvider.java`:

```java
MY_CUSTOM("My Custom Integration", PluginType.WEBHOOK)
```

3. **Update Database Migration**

Add to `V34__Create_data_plugins.sql` constraint:

```sql
CONSTRAINT check_provider CHECK (provider IN (..., 'MY_CUSTOM'))
```

4. **Add Frontend Support**

Update `PluginFormDialog.tsx` to include the new provider in the dropdown.

### Testing Plugins

Unit test example:

```java
@Test
void testCustomPluginProcessing() {
    MyCustomPlugin plugin = new MyCustomPlugin();

    DataPlugin config = new DataPlugin();
    config.setProvider(PluginProvider.MY_CUSTOM);
    config.setConfiguration(objectMapper.createObjectNode());

    String webhookPayload = """
        {
          "device": "sensor-001",
          "time": "2024-01-01T12:00:00Z",
          "data": {
            "temperature": 23.5,
            "humidity": 65.2
          }
        }
        """;

    List<TelemetryPayload> result = plugin.process(config, webhookPayload);

    assertEquals(1, result.size());
    assertEquals("sensor-001", result.get(0).deviceId());
    assertEquals(2, result.get(0).variables().size());
}
```

## Security Considerations

### Webhook Authentication

Currently, webhook endpoints are **public** and accept any POST request. This is by design for compatibility with external systems.

**Best Practices:**
1. **Unique Plugin Names**: Use UUIDs or hard-to-guess names
2. **Firewall Rules**: Restrict webhook endpoint access by IP (e.g., TTN IPs only)
3. **HTTPS**: Always use HTTPS in production
4. **Payload Validation**: Plugins validate payloads and reject malformed data

**Future Enhancements:**
- API key authentication for webhooks
- IP whitelist configuration per plugin
- Request signing (HMAC) verification

### Multi-Tenancy Isolation

- Each plugin belongs to a specific organization
- Webhook URL includes organization ID for routing
- No cross-organization data access
- Execution history isolated per organization

## Troubleshooting

### Common Issues

#### Plugin Not Found (404)

**Cause:** Plugin name in URL doesn't match database
**Solution:** Verify plugin name is correct and enabled

```bash
# Check plugin exists
GET /api/v1/plugins
```

#### Invalid Payload (400)

**Cause:** Webhook payload doesn't match expected format
**Solution:** Check provider documentation and validate JSON structure

**For LoRaWAN TTN:**
- Ensure TTN application includes `decoded_payload` in uplinks
- Verify device ID exists in `end_device_ids.device_id`

#### No Telemetry Variables Found

**Cause:** Plugin can't extract variables from payload
**Solution:**
- Check `decoded_payload` exists in TTN uplinks
- Verify all values are numeric (strings won't be extracted)
- Review execution history for detailed error messages

#### Execution Status: PARTIAL

**Cause:** Some records succeeded, others failed
**Solution:** Check execution history error message for details about which records failed

### Debug Mode

Enable debug logging for plugin execution:

```properties
# application.properties
logging.level.org.indcloud.plugin=DEBUG
logging.level.org.indcloud.service.DataPluginService=DEBUG
```

View logs:

```bash
docker logs indcloud-backend 2>&1 | grep -i plugin
```

## Performance

### Execution Metrics

Each plugin execution records:
- **Timestamp**: When execution started
- **Duration**: Time to process (milliseconds)
- **Records Processed**: Number of telemetry records created
- **Status**: SUCCESS, FAILED, or PARTIAL
- **Error Message**: Details if execution failed

### Scalability

- Plugin processors are stateless Spring beans
- Multiple webhook requests processed concurrently
- Database transactions ensure data consistency
- Execution history kept for 90 days (configurable retention)

### Rate Limiting

No built-in rate limiting currently. Consider:
- API Gateway rate limiting
- CloudFlare/WAF protection
- Nginx rate limit rules

## Roadmap

### Sprint 2 (Current)
- ✅ LoRaWAN TTN Plugin
- ✅ HTTP Webhook Plugin
- ✅ CSV Import Plugin
- ✅ Frontend UI
- ✅ Execution History

### Sprint 3 (Completed)
- ✅ Modbus TCP Plugin
- ✅ Sigfox Plugin
- ✅ MQTT Bridge Plugin
- ✅ Plugin Registry System
- ✅ Pre-built plugin templates

### Sprint 4+ (Future)
- 📋 Particle Cloud Plugin
- 📋 Custom decoder functions (JavaScript/Python)
- 📋 Plugin marketplace
- 📋 Webhook authentication
- 📋 IP whitelist per plugin
- 📋 OPC UA Plugin
- 📋 BACnet Plugin

## API Reference

### Create Plugin

```
POST /api/v1/plugins
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "my-lorawan-integration",
  "description": "TTN warehouse sensors",
  "pluginType": "WEBHOOK",
  "provider": "LORAWAN_TTN",
  "enabled": true,
  "configuration": {
    "deviceIdPrefix": "warehouse-"
  }
}
```

### Update Plugin

```
PUT /api/v1/plugins/{id}
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "my-lorawan-integration",
  "description": "Updated description",
  "pluginType": "WEBHOOK",
  "provider": "LORAWAN_TTN",
  "enabled": false,
  "configuration": {}
}
```

### Delete Plugin

```
DELETE /api/v1/plugins/{id}
Authorization: Bearer {token}
```

### Get Execution History

```
GET /api/v1/plugins/{id}/executions?page=0&size=20
Authorization: Bearer {token}
```

### Receive Webhook (Public)

```
POST /api/v1/webhooks/{organizationId}/{pluginName}
Content-Type: application/json

{
  "deviceId": "sensor-001",
  "timestamp": "2024-01-01T12:00:00Z",
  "variables": {
    "temperature": 23.5
  }
}
```

## Support

- **Documentation**: [GitHub Wiki](https://github.com/CodeFleck/indcloud/wiki)
- **Issues**: [GitHub Issues](https://github.com/CodeFleck/indcloud/issues)
- **Examples**: See `docs/LORAWAN_TTN_INTEGRATION.md` for detailed integration guide
