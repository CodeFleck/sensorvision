# LoRaWAN The Things Network (TTN) Integration Guide

Complete guide for integrating Industrial Cloud with The Things Network (TTN) v3 for LoRaWAN device management.

---

## Overview

This integration enables Industrial Cloud to receive telemetry data from LoRaWAN devices connected to The Things Network via HTTP webhooks.

**Features:**
- ✅ Automatic device creation on first uplink
- ✅ Telemetry data extraction from decoded payloads
- ✅ LoRaWAN metadata capture (RSSI, SNR, spreading factor)
- ✅ Support for TTN payload formatters
- ✅ Multi-device management with configurable ID prefixes

---

## Prerequisites

1. **The Things Network Account** - [Sign up at console.thethingsnetwork.org](https://console.thethingsnetwork.org/)
2. **LoRaWAN Devices** - Registered in your TTN application
3. **Payload Decoder** - Configured in TTN to decode binary payloads to JSON
4. **Industrial Cloud Account** - Admin access to create plugins

---

## Step-by-Step Setup

### 1. Configure TTN Payload Decoder

Before setting up the webhook, ensure your TTN application has a payload decoder configured.

#### Example Payload Decoder (JavaScript)

In TTN Console → Applications → Your Application → Payload Formatters → Uplink:

```javascript
function decodeUplink(input) {
  var bytes = input.bytes;
  var decoded = {};

  // Example: Temperature (2 bytes, signed int16, °C * 100)
  decoded.temperature = ((bytes[0] << 8) | bytes[1]) / 100.0;

  // Example: Humidity (1 byte, 0-100%)
  decoded.humidity = bytes[2];

  // Example: Battery voltage (2 bytes, mV)
  decoded.battery_voltage = ((bytes[3] << 8) | bytes[4]) / 1000.0;

  return {
    data: decoded,
    warnings: [],
    errors: []
  };
}
```

**Important:** The decoded values must be in the `data` object and will appear in `uplink_message.decoded_payload` in the webhook.

### 2. Create LoRaWAN Plugin in Industrial Cloud

1. Log in to Industrial Cloud as an admin
2. Navigate to **Data Plugins** in the sidebar
3. Click **New Plugin**
4. Fill in the form:

**Plugin Configuration:**
```
Name: my-lorawan-ttn
Description: TTN warehouse temperature sensors
Provider: LoRaWAN (The Things Network)
Plugin Type: WEBHOOK (auto-filled)
Enabled: ✓ Checked

Configuration (JSON):
{
  "deviceIdPrefix": "",
  "deviceIdSuffix": ""
}
```

5. Click **Create**

**Your webhook URL will be:**
```
https://your-indcloud-domain.com/api/v1/webhooks/{organizationId}/my-lorawan-ttn
```

**Example:**
```
https://indcloud.example.com/api/v1/webhooks/1/my-lorawan-ttn
```

**Note:** Save this URL - you'll need it for TTN configuration.

### 3. Configure TTN Webhook Integration

In The Things Network Console:

1. Go to **Applications** → Your Application
2. Click **Integrations** → **Webhooks**
3. Click **+ Add webhook**
4. Select **Custom webhook**

**Webhook Configuration:**

| Field | Value |
|-------|-------|
| **Webhook ID** | `indcloud` (or any unique identifier) |
| **Webhook format** | `JSON` |
| **Base URL** | `https://your-indcloud-domain.com/api/v1/webhooks/{orgId}/{pluginName}` |
| **Downlink API key** | (leave empty - not needed for uplinks) |
| **Enabled** | ✓ Checked |

**Uplink message:**
- Check ✓ **Uplink message**
- URL: (uses Base URL)
- HTTP Method: `POST`
- Additional headers: (none needed)

**Leave unchecked:**
- Join accept
- Downlink ack
- Downlink nack
- Downlink sent
- Downlink failed
- Downlink queued
- Location solved
- Service data

4. Click **Add webhook**

### 4. Test the Integration

#### Send Test Uplink

Trigger an uplink from your LoRaWAN device or use TTN's test feature:

1. In TTN Console → Devices → Your Device
2. Scroll to **Live data** section
3. Wait for a real uplink OR use simulator

#### Verify in Industrial Cloud

1. Go to **Data Plugins** in Industrial Cloud
2. Find your `my-lorawan-ttn` plugin
3. Click the **History** (clock) icon
4. Verify execution shows:
   - Status: `SUCCESS`
   - Records Processed: `1`
   - Duration: `~50-200ms`

5. Go to **Devices** page
6. Verify new device created with ID matching TTN device ID
7. Click on the device to view telemetry data

---

## TTN Payload Format

### Expected Uplink Message Format

TTN sends uplink webhooks in this format:

```json
{
  "end_device_ids": {
    "device_id": "my-sensor-001",
    "application_ids": {
      "application_id": "my-app"
    },
    "dev_eui": "70B3D57ED0012345",
    "join_eui": "0000000000000000",
    "dev_addr": "260B1234"
  },
  "received_at": "2024-01-15T14:32:45.123456Z",
  "uplink_message": {
    "session_key_id": "AYxxx",
    "f_port": 1,
    "f_cnt": 42,
    "frm_payload": "AQIDBAUGBwg=",
    "decoded_payload": {
      "temperature": 23.5,
      "humidity": 65.2,
      "battery_voltage": 3.6
    },
    "rx_metadata": [
      {
        "gateway_ids": {
          "gateway_id": "my-gateway"
        },
        "time": "2024-01-15T14:32:45.123Z",
        "timestamp": 1234567890,
        "rssi": -87,
        "channel_rssi": -87,
        "snr": 9.5,
        "uplink_token": "xxx"
      }
    ],
    "settings": {
      "data_rate": {
        "lora": {
          "bandwidth": 125000,
          "spreading_factor": 7
        }
      },
      "frequency": "868100000"
    }
  }
}
```

### What Industrial Cloud Extracts

#### Device ID
```
end_device_ids.device_id → "my-sensor-001"
```

With prefix/suffix configuration:
```json
{
  "deviceIdPrefix": "warehouse-",
  "deviceIdSuffix": "-main"
}
```

Results in: `warehouse-my-sensor-001-main`

#### Timestamp
```
received_at → "2024-01-15T14:32:45.123456Z"
```

Parsed as ISO 8601 instant.

#### Telemetry Variables
```
uplink_message.decoded_payload → {
  "temperature": 23.5,
  "humidity": 65.2,
  "battery_voltage": 3.6
}
```

Each numeric field becomes a telemetry variable in Industrial Cloud.

#### LoRaWAN Metadata

Extracted from first gateway in `rx_metadata`:

| Metadata Field | Source | Example |
|----------------|--------|---------|
| `lorawan_rssi` | `rx_metadata[0].rssi` | `-87` |
| `lorawan_snr` | `rx_metadata[0].snr` | `9.5` |
| `lorawan_fport` | `uplink_message.f_port` | `1` |
| `lorawan_sf` | `settings.data_rate.lora.spreading_factor` | `7` |
| `lorawan_bandwidth` | `settings.data_rate.lora.bandwidth` | `125000` |

---

## Configuration Options

### Device ID Mapping

Control how TTN device IDs map to Industrial Cloud device IDs.

#### Default (No Prefix/Suffix)

```json
{
  "deviceIdPrefix": "",
  "deviceIdSuffix": ""
}
```

TTN Device ID: `sensor-001` → Industrial Cloud Device ID: `sensor-001`

#### With Prefix

```json
{
  "deviceIdPrefix": "ttn-"
}
```

TTN Device ID: `sensor-001` → Industrial Cloud Device ID: `ttn-sensor-001`

**Use Case:** Distinguish between different data sources (TTN vs Sigfox vs direct MQTT).

#### With Suffix

```json
{
  "deviceIdSuffix": "-warehouse"
}
```

TTN Device ID: `sensor-001` → Industrial Cloud Device ID: `sensor-001-warehouse`

**Use Case:** Identify device location or group.

#### Both Prefix and Suffix

```json
{
  "deviceIdPrefix": "lora-",
  "deviceIdSuffix": "-building-a"
}
```

TTN Device ID: `temp-01` → Industrial Cloud Device ID: `lora-temp-01-building-a`

---

## Advanced Use Cases

### Multiple TTN Applications

Create separate plugins for each TTN application:

1. **Plugin: ttn-warehouse**
   - Configuration: `{"deviceIdPrefix": "warehouse-"}`
   - Webhook URL: `.../webhooks/1/ttn-warehouse`

2. **Plugin: ttn-outdoor**
   - Configuration: `{"deviceIdPrefix": "outdoor-"}`
   - Webhook URL: `.../webhooks/1/ttn-outdoor`

Configure each TTN application with its corresponding webhook URL.

### Custom Payload Decoders

If your TTN application uses complex payload formats, ensure the decoder outputs a flat JSON object:

**Bad (nested):**
```javascript
{
  "data": {
    "sensors": {
      "temp": 23.5,
      "hum": 65.2
    }
  }
}
```

**Good (flat):**
```javascript
{
  "temperature": 23.5,
  "humidity": 65.2
}
```

Industrial Cloud extracts all numeric fields from the top level of `decoded_payload`.

### Handling String Values

Currently, only numeric values are extracted as telemetry variables. String values are ignored.

**Example Payload:**
```json
{
  "temperature": 23.5,
  "humidity": 65.2,
  "status": "OK"
}
```

**Result:**
- ✓ `temperature` = 23.5
- ✓ `humidity` = 65.2
- ✗ `status` ignored (string)

**Workaround:** Convert strings to numbers in TTN decoder:
```javascript
decoded.status_code = (status === "OK") ? 1 : 0;
```

---

## Monitoring and Troubleshooting

### View Execution History

1. Go to **Data Plugins** → **my-lorawan-ttn**
2. Click **History** icon
3. Review execution logs:
   - Green ✓ = SUCCESS
   - Yellow ⚠ = PARTIAL (some records failed)
   - Red ✗ = FAILED

### Common Issues

#### Issue: "Plugin not found"

**Error Message in TTN Webhook Logs:**
```
404 Not Found
```

**Cause:** Plugin name in URL doesn't match Industrial Cloud plugin name

**Solution:**
- Verify plugin name exactly matches (case-sensitive)
- Check organization ID is correct
- Ensure plugin is enabled

#### Issue: "Missing uplink_message in TTN payload"

**Error in Execution History:**
```
Missing uplink_message in TTN payload
```

**Cause:** TTN webhook isn't configured for uplink messages

**Solution:**
1. In TTN Console → Webhooks → Your Webhook
2. Ensure "Uplink message" is checked
3. Ensure other message types (Join accept, etc.) are unchecked

#### Issue: "No telemetry data found in TTN payload"

**Error in Execution History:**
```
No telemetry data found in TTN payload. Please ensure your TTN application includes decoded_payload.
```

**Cause:** TTN application doesn't have a payload decoder configured

**Solution:**
1. Go to TTN Console → Applications → Your App → Payload Formatters
2. Add a JavaScript uplink decoder
3. Ensure it returns numeric values in `data` object
4. Test with a new uplink

#### Issue: Device created but no telemetry data

**Symptoms:**
- Device appears in Industrial Cloud
- Telemetry dashboard shows "No data"
- Execution history shows SUCCESS

**Cause:** All values in `decoded_payload` are strings, not numbers

**Solution:**
Update TTN decoder to return numbers:
```javascript
// Bad
decoded.temperature = "23.5";

// Good
decoded.temperature = 23.5;
```

#### Issue: Wrong timestamp

**Symptoms:**
- Telemetry records show incorrect timestamps

**Cause:** TTN `received_at` field is in wrong format

**Solution:**
- TTN v3 uses ISO 8601 format (correct)
- Verify timezone is UTC
- Industrial Cloud uses `received_at`, not `transmitted_at`

### Debug Webhooks in TTN

1. In TTN Console → Applications → Your App → Integrations → Webhooks
2. Click your webhook → **Live data**
3. Trigger an uplink from your device
4. View webhook request/response

**Successful Response:**
```json
{
  "success": true,
  "executionId": 123,
  "status": "SUCCESS",
  "recordsProcessed": 1,
  "durationMs": 87
}
```

**Failed Response:**
```json
{
  "success": false,
  "executionId": 124,
  "status": "FAILED",
  "recordsProcessed": 0,
  "durationMs": 45,
  "errorMessage": "Missing uplink_message in TTN payload"
}
```

### Enable Debug Logging (Backend)

Add to `application.properties`:
```properties
logging.level.org.indcloud.plugin.impl.LoRaWanTtnPlugin=DEBUG
logging.level.org.indcloud.service.DataPluginService=DEBUG
```

View logs:
```bash
docker logs indcloud-backend 2>&1 | grep -i lorawan
```

---

## Security Best Practices

### 1. Use HTTPS

Always use HTTPS for webhook URLs in production:
```
✗ http://indcloud.example.com/api/v1/webhooks/...
✓ https://indcloud.example.com/api/v1/webhooks/...
```

TTN requires HTTPS for production webhooks.

### 2. Unique Plugin Names

Use hard-to-guess plugin names:
```
✗ my-lorawan-ttn (predictable)
✓ ttn-a7f3d891-4b2c-4e5a-9c8d-f1e4b5a6c7d8 (UUID)
```

### 3. Restrict by IP (Optional)

If using a reverse proxy (nginx), whitelist TTN IP ranges:

```nginx
location /api/v1/webhooks/ {
    # TTN IP ranges (example - verify current ranges)
    allow 52.169.0.0/16;
    allow 168.63.129.16/32;
    deny all;

    proxy_pass http://localhost:8080;
}
```

**Note:** TTN IP ranges may change. Check TTN documentation for current IPs.

### 4. Monitor Execution History

Regularly review execution history for:
- Unexpected spikes in failed executions
- Unknown device IDs
- Unusual telemetry values

---

## Performance and Scalability

### Throughput

The LoRaWAN plugin can handle:
- **Single Device:** Uplinks every few seconds
- **100 Devices:** ~1000 uplinks/hour
- **1000 Devices:** ~10,000 uplinks/hour

Actual performance depends on:
- Database performance (PostgreSQL)
- Number of rules configured
- Number of synthetic variables

### Latency

Typical webhook processing time:
- **Plugin Processing:** 5-15ms
- **Telemetry Ingestion:** 20-50ms
- **Rules Evaluation:** 10-30ms (if rules configured)
- **Total:** ~50-100ms end-to-end

### Optimize for High Volume

If processing >10,000 uplinks/hour:

1. **Disable unused features** (per plugin):
   - No rules? Disable rule evaluation
   - No synthetic variables? Skip calculations

2. **Use read replicas** for analytics queries

3. **Batch uplinks** if possible:
   - Some devices support batching multiple readings
   - Process in a single webhook call

---

## Example Deployment

### Production Setup: Smart Building

**Scenario:**
- 50 LoRaWAN temperature/humidity sensors
- Data sent every 10 minutes
- TTN application: `smart-building-prod`

**Industrial Cloud Configuration:**

```json
{
  "name": "ttn-smart-building",
  "description": "Production smart building sensors",
  "pluginType": "WEBHOOK",
  "provider": "LORAWAN_TTN",
  "enabled": true,
  "configuration": {
    "deviceIdPrefix": "building-",
    "deviceIdSuffix": ""
  }
}
```

**Webhook URL:**
```
https://iot.company.com/api/v1/webhooks/1/ttn-smart-building
```

**TTN Payload Decoder:**
```javascript
function decodeUplink(input) {
  var bytes = input.bytes;

  return {
    data: {
      temperature: ((bytes[0] << 8) | bytes[1]) / 100.0,
      humidity: bytes[2],
      battery_voltage: ((bytes[3] << 8) | bytes[4]) / 1000.0
    }
  };
}
```

**Result:**
- 50 devices in Industrial Cloud with IDs: `building-sensor-001`, `building-sensor-002`, ...
- 3 variables per device: `temperature`, `humidity`, `battery_voltage`
- ~300 telemetry records per hour (50 devices × 6 uplinks/hour)

---

## Limitations

### Current Limitations

1. **No Custom Decoders**
   - Must use TTN payload formatters
   - Cannot define custom decoder in Industrial Cloud
   - **Workaround:** Implement all logic in TTN decoder

2. **Numeric Values Only**
   - String values in `decoded_payload` are ignored
   - Boolean values are ignored
   - **Workaround:** Convert to numbers (0/1)

3. **No Downlink Support**
   - Plugin only handles uplinks
   - Cannot send downlink messages to devices
   - **Roadmap:** Planned for future release

4. **Single Webhook per Plugin**
   - One plugin = one webhook URL
   - Cannot configure multiple sources for one plugin
   - **Workaround:** Create multiple plugins

### Future Enhancements

**Planned for Sprint 4+:**
- ✨ Custom JavaScript/Python decoder functions
- ✨ Downlink message support
- ✨ Webhook authentication (HMAC signatures)
- ✨ IP whitelist configuration per plugin
- ✨ Payload transformation rules

---

## Migration from Other Platforms

### Migrating from Ubidots

Ubidots TTN integration uses:
```
POST https://industrial.api.ubidots.com/api/webhooks/ttn/
X-Auth-Token: YOUR-UBIDOTS-TOKEN
```

**Industrial Cloud equivalent:**
```
POST https://your-domain.com/api/v1/webhooks/{orgId}/{pluginName}
(No authentication required - public webhook)
```

**Key Differences:**
- No API key required (organization ID + plugin name provides isolation)
- Plugin configuration stored in Industrial Cloud (not in webhook headers)
- Execution history tracked automatically

### Migrating from TagoIO

TagoIO uses action scripts for TTN. In Industrial Cloud:
- Create a plugin with provider `LORAWAN_TTN`
- Configure TTN webhook to point to Industrial Cloud
- Industrial Cloud handles all transformation automatically

---

## Support and Resources

### Documentation
- [Data Plugins Guide](DATA_PLUGINS.md)
- [Quick Start Guide](../README.md#quick-start)
- [API Reference](API_REFERENCE.md)

### TTN Resources
- [The Things Network Console](https://console.thethingsnetwork.org/)
- [TTN Documentation](https://www.thethingsindustries.com/docs/)
- [TTN Community Forum](https://www.thethingsnetwork.org/forum/)

### Troubleshooting
- [GitHub Issues](https://github.com/CodeFleck/indcloud/issues)
- [GitHub Discussions](https://github.com/CodeFleck/indcloud/discussions)

---

## Changelog

### v0.1.0 (2025-01-15)
- ✅ Initial LoRaWAN TTN plugin implementation
- ✅ Support for TTN v3 uplink messages
- ✅ Automatic device creation
- ✅ LoRaWAN metadata extraction
- ✅ Configurable device ID prefix/suffix
- ✅ Execution history tracking
- ✅ Frontend UI for plugin management

---

**Status:** ✅ Production Ready

**Last Updated:** 2025-01-15

**Integration Type:** Webhook (HTTP POST)

**Protocol Version:** The Things Network v3
