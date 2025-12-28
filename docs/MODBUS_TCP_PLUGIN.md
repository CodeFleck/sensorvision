# Modbus TCP Plugin

The Modbus TCP Plugin enables Industrial Cloud to poll industrial devices using the Modbus TCP protocol. This is essential for integrating with PLCs, SCADA systems, and other industrial equipment.

## Overview

- **Type**: Polling Plugin (actively fetches data on schedule)
- **Protocol**: Modbus TCP (RFC 2217)
- **Library**: j2mod 3.2.1
- **Use Cases**: Industrial IoT, manufacturing, building automation, energy monitoring

## Features

### Supported Register Types
- **HOLDING Registers** (read/write): Function code 0x03
- **INPUT Registers** (read-only): Function code 0x04

### Supported Data Types
- `INT16` - Signed 16-bit integer (-32,768 to 32,767)
- `UINT16` - Unsigned 16-bit integer (0 to 65,535)
- `INT32` - Signed 32-bit integer (2 registers)
- `UINT32` - Unsigned 32-bit integer (2 registers)
- `FLOAT32` - IEEE 754 32-bit float (2 registers)

### Address Convention Support
The plugin automatically detects and converts Modicon-style addresses:
- **Holding Registers**: 40001-49999 → 0-based (40001 → 0)
- **INPUT Registers**: 30001-39999 → 0-based (30001 → 0)
- **Direct 0-based**: Used as-is

### Data Transformations
- **Scale Factor**: Multiply register value (e.g., 0.1 for tenths)
- **Offset**: Add to scaled value (e.g., temperature offset)

## Configuration

### Example Configuration

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
      "scale": 0.1,
      "offset": 0,
      "dataType": "INT16"
    },
    {
      "type": "INPUT",
      "address": 30001,
      "count": 2,
      "variableName": "flow_rate",
      "scale": 1.0,
      "offset": 0,
      "dataType": "FLOAT32"
    }
  ]
}
```

### Configuration Fields

#### Connection Settings
- **host** (required): IP address of Modbus TCP server
- **port** (optional): Modbus TCP port (default: 502)
- **unitId** (optional): Modbus unit/slave ID (default: 1, range: 0-255)
- **timeout** (optional): Connection timeout in milliseconds (default: 3000)

#### Device Mapping
- **deviceId** (required): Industrial Cloud device ID for this Modbus device

#### Polling Settings
- **pollingIntervalSeconds** (optional): How often to poll (default: 60, range: 1-86400)

#### Register Mappings
- **registers** (required): Array of register configurations
  - **type** (required): "HOLDING" or "INPUT"
  - **address** (required): Modbus register address (0-65535)
  - **count** (required): Number of consecutive registers (1-125)
  - **variableName** (required): Industrial Cloud variable name
  - **scale** (optional): Scale factor (default: 1.0)
  - **offset** (optional): Offset value (default: 0.0)
  - **dataType** (optional): "INT16", "UINT16", "INT32", "UINT32", "FLOAT32" (default: "INT16")

## Architecture

### Backend Components

#### 1. ModbusTcpPlugin.java
**Location**: `src/main/java/org/indcloud/plugin/impl/ModbusTcpPlugin.java`

Core plugin implementation that:
- Connects to Modbus TCP devices using j2mod library
- Reads configured registers
- Parses register values based on data type
- Applies scale and offset transformations
- Returns PollingResult with device ID, timestamp, and variables

**Key Methods**:
- `poll(JsonNode config)` - Main polling logic
- `readRegister()` - Read and parse single register mapping
- `adjustModbusAddress()` - Convert Modicon convention to 0-based
- `parseRegisters()` - Parse register data based on data type

#### 2. BasePollingPlugin.java
**Location**: `src/main/java/org/indcloud/plugin/BasePollingPlugin.java`

Abstract base class for all polling plugins:
- Implements DataPluginProcessor interface
- Provides validation for polling configuration
- Defines PollingResult data structure
- Distinguishes polling plugins from webhook plugins

#### 3. PluginPollingSchedulerService.java
**Location**: `src/main/java/org/indcloud/service/PluginPollingSchedulerService.java`

Manages polling schedules:
- Discovers enabled polling plugins on startup
- Creates scheduled tasks with configurable intervals
- Executes poll() method periodically
- Converts PollingResult to TelemetryPayload
- Ingests data into Industrial Cloud telemetry system
- Supports manual triggering for testing

**Key Features**:
- Thread pool (10 concurrent tasks)
- Dynamic scheduling (add/remove plugins at runtime)
- Graceful shutdown handling
- Individual plugin error isolation

### Frontend Components

#### 1. Data Plugins UI
**Location**: `frontend/src/pages/DataPlugins.tsx`

Lists all configured plugins with:
- POLLING type indicator (indigo badge)
- Enable/disable toggle
- Edit and delete actions
- Execution history access

#### 2. Plugin Form Dialog
**Location**: `frontend/src/components/plugins/PluginFormDialog.tsx`

Configuration form with:
- Modbus TCP provider selection
- JSON configuration editor
- Default configuration template
- Validation and error handling

### Database Schema

#### V34: data_plugins table
```sql
CREATE TABLE data_plugins (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    plugin_type VARCHAR(50) NOT NULL, -- 'POLLING'
    provider VARCHAR(50) NOT NULL,    -- 'MODBUS_TCP'
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### V45: Add POLLING type
```sql
ALTER TABLE data_plugins ADD CONSTRAINT check_plugin_type
    CHECK (plugin_type IN ('PROTOCOL_PARSER', 'WEBHOOK', 'INTEGRATION', 'CSV_IMPORT', 'POLLING'));
```

## Usage Guide

### 1. Create Plugin via UI

1. Navigate to **Data Plugins** page
2. Click **"Create New Plugin"**
3. Configure:
   - **Name**: `my-plc-001`
   - **Provider**: `Modbus TCP`
   - **Configuration**: Use JSON editor to configure connection and registers
4. Click **"Create"**

### 2. Plugin Auto-Starts

Once created and enabled:
- `PluginPollingSchedulerService` automatically picks it up
- Polling starts after 5 seconds
- Runs every `pollingIntervalSeconds` (default: 60)

### 3. Monitor Execution

- Check **Execution History** to see poll results
- View **Device Dashboard** for ingested telemetry data
- Check logs for detailed polling activity

### 4. Testing

To manually trigger a poll for testing:
```java
@Autowired
private PluginPollingSchedulerService scheduler;

// Trigger manual poll
scheduler.triggerManualPoll(pluginId);
```

## Example Use Cases

### 1. Energy Monitoring
```json
{
  "host": "192.168.1.50",
  "port": 502,
  "unitId": 1,
  "deviceId": "meter-001",
  "pollingIntervalSeconds": 30,
  "registers": [
    {
      "type": "HOLDING",
      "address": 40001,
      "count": 2,
      "variableName": "kw_consumption",
      "dataType": "FLOAT32"
    },
    {
      "type": "HOLDING",
      "address": 40003,
      "count": 1,
      "variableName": "voltage",
      "scale": 0.1,
      "dataType": "UINT16"
    },
    {
      "type": "HOLDING",
      "address": 40004,
      "count": 1,
      "variableName": "current",
      "scale": 0.01,
      "dataType": "UINT16"
    }
  ]
}
```

### 2. HVAC System
```json
{
  "host": "192.168.10.20",
  "port": 502,
  "unitId": 1,
  "deviceId": "hvac-main",
  "pollingIntervalSeconds": 120,
  "registers": [
    {
      "type": "INPUT",
      "address": 30001,
      "count": 1,
      "variableName": "room_temperature",
      "scale": 0.1,
      "offset": 0,
      "dataType": "INT16"
    },
    {
      "type": "INPUT",
      "address": 30002,
      "count": 1,
      "variableName": "humidity",
      "scale": 0.1,
      "dataType": "UINT16"
    },
    {
      "type": "HOLDING",
      "address": 40001,
      "count": 1,
      "variableName": "setpoint_temp",
      "scale": 0.1,
      "dataType": "INT16"
    }
  ]
}
```

### 3. Water Treatment
```json
{
  "host": "10.0.5.100",
  "port": 502,
  "unitId": 2,
  "deviceId": "water-plant-main",
  "pollingIntervalSeconds": 60,
  "registers": [
    {
      "type": "INPUT",
      "address": 30001,
      "count": 2,
      "variableName": "flow_rate",
      "dataType": "FLOAT32"
    },
    {
      "type": "INPUT",
      "address": 30003,
      "count": 1,
      "variableName": "ph_level",
      "scale": 0.01,
      "dataType": "UINT16"
    },
    {
      "type": "INPUT",
      "address": 30004,
      "count": 1,
      "variableName": "chlorine_ppm",
      "scale": 0.1,
      "dataType": "UINT16"
    }
  ]
}
```

## Testing with Modbus Simulator

### Using diagslave (Modbus Slave Simulator)

1. **Install diagslave**:
   ```bash
   # Download from https://www.modbusdriver.com/diagslave.html
   ```

2. **Start simulator**:
   ```bash
   diagslave -m tcp -p 5020
   ```

3. **Configure plugin to use simulator**:
   ```json
   {
     "host": "localhost",
     "port": 5020,
     "unitId": 1,
     "deviceId": "test-device",
     "pollingIntervalSeconds": 10,
     "registers": [
       {
         "type": "HOLDING",
         "address": 0,
         "count": 1,
         "variableName": "test_value",
         "dataType": "INT16"
       }
     ]
   }
   ```

### Using pymodbus Simulator

```python
from pymodbus.server.sync import StartTcpServer
from pymodbus.datastore import ModbusSlaveContext, ModbusServerContext
from pymodbus.datastore import ModbusSequentialDataBlock

# Create data blocks
store = ModbusSlaveContext(
    di=ModbusSequentialDataBlock(0, [0]*100),
    co=ModbusSequentialDataBlock(0, [0]*100),
    hr=ModbusSequentialDataBlock(0, [250, 2200, 123, 456]),  # Holding registers
    ir=ModbusSequentialDataBlock(0, [100, 200, 300])  # Input registers
)

context = ModbusServerContext(slaves=store, single=True)

# Start server
StartTcpServer(context, address=("localhost", 502))
```

## Troubleshooting

### Common Issues

#### 1. Connection Refused
**Problem**: `Failed to connect to Modbus device`
**Solutions**:
- Verify host IP and port
- Check firewall rules
- Ensure Modbus device is powered on
- Test with `telnet <host> <port>`

#### 2. Timeout Errors
**Problem**: Reads timing out
**Solutions**:
- Increase `timeout` value
- Check network latency
- Reduce number of registers per poll
- Verify unit ID is correct

#### 3. Invalid Register Values
**Problem**: Garbage data or incorrect values
**Solutions**:
- Check `dataType` matches device configuration
- Verify `address` convention (Modicon vs 0-based)
- Adjust `scale` and `offset` values
- Check register byte order (endianness)

#### 4. No Data Ingested
**Problem**: Plugin polls but no telemetry appears
**Solutions**:
- Check plugin is enabled
- Verify `deviceId` exists in Industrial Cloud
- Check execution history for errors
- Review backend logs

### Logging

Enable debug logging for detailed Modbus activity:

```properties
# application.properties
logging.level.org.indcloud.plugin.impl.ModbusTcpPlugin=DEBUG
logging.level.org.indcloud.service.PluginPollingSchedulerService=DEBUG
```

## Performance Considerations

### Polling Intervals
- **Fast polling** (1-10s): Use for critical real-time monitoring
- **Normal polling** (30-60s): Use for standard monitoring
- **Slow polling** (300-3600s): Use for infrequent data

### Register Optimization
- Group consecutive registers in single read
- Use `count` to read multiple registers at once
- Limit to 125 registers per read (Modbus TCP maximum)

### Concurrent Devices
- Scheduler supports 10 concurrent polling tasks
- Each plugin polls independently
- Failed polls don't affect other plugins

## Future Enhancements

Potential improvements for future versions:

1. **Modbus RTU Support**: Serial communication via RS-485
2. **Write Operations**: Support Modbus write functions
3. **Register Watching**: Only poll when values change
4. **Batch Devices**: Poll multiple unit IDs in single plugin
5. **Connection Pooling**: Reuse connections for efficiency
6. **Custom Function Codes**: Support vendor-specific codes

## References

- [Modbus TCP Specification](https://modbus.org/docs/Modbus_Messaging_Implementation_Guide_V1_0b.pdf)
- [j2mod Library Documentation](https://github.com/steveohara/j2mod)
- [Modbus Register Addressing](https://www.simplymodbus.ca/FAQ.htm)
