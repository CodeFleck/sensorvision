# IndCloud Python SDK

[![Python Version](https://img.shields.io/badge/python-3.8+-blue.svg)](https://www.python.org/downloads/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

Official Python SDK for [IndCloud](https://github.com/CodeFleck/indcloud) - The IoT platform that scales with you.

Build IoT applications with enterprise-grade infrastructure and developer-friendly tools. This SDK provides simple, production-ready clients for sending telemetry data from IoT devices, Raspberry Pi projects, Python applications, and more.

## Features

- 🚀 **Simple API** - Send telemetry data with just a few lines of code
- 🔮 **Dynamic Variables** - Send ANY variable name - auto-provisioned on first use!
- ⚡ **Async Support** - Built-in asyncio support for high-performance applications
- 🔄 **Auto-Retry** - Automatic retry logic with exponential backoff
- 🛡️ **Type Safety** - Full type hints for IDE autocomplete and type checking
- 📝 **Comprehensive Logging** - Built-in logging for debugging and monitoring
- 🔌 **Flexible** - Works with any Python-based IoT device or application
- 🌐 **Well-Tested** - >90% test coverage

## Installation

### Install from PyPI (Recommended)

```bash
# Basic installation
pip install indcloud-sdk

# With async support
pip install indcloud-sdk[async]

# For Raspberry Pi projects
pip install indcloud-sdk[raspberry-pi]
```

### Install from GitHub (Development)

```bash
# Basic installation
pip install git+https://github.com/CodeFleck/indcloud.git#subdirectory=indcloud-sdk

# With async support
pip install "git+https://github.com/CodeFleck/indcloud.git#subdirectory=indcloud-sdk[async]"

# For Raspberry Pi projects
pip install "git+https://github.com/CodeFleck/indcloud.git#subdirectory=indcloud-sdk[raspberry-pi]"
```

### Development Installation

```bash
# Clone the repository
git clone https://github.com/CodeFleck/indcloud.git
cd indcloud/indcloud-sdk

# Install in editable mode with dev dependencies
pip install -e ".[dev]"
```

## Quick Start

### Synchronous Client

```python
from indcloud import IndCloudClient

# Initialize client
client = IndCloudClient(
    api_url="http://localhost:8080",
    api_key="your-device-token"
)

# Send telemetry data
response = client.send_data("my-sensor", {
    "temperature": 23.5,
    "humidity": 65.2,
    "pressure": 1013.25
})

print(f"Success! {response.message}")

# Close the client
client.close()
```

### Context Manager (Recommended)

```python
from indcloud import IndCloudClient

with IndCloudClient(
    api_url="http://localhost:8080",
    api_key="your-device-token"
) as client:
    response = client.send_data("my-sensor", {
        "temperature": 23.5,
        "humidity": 65.2
    })
```

### Asynchronous Client

```python
import asyncio
from indcloud import AsyncIndCloudClient

async def main():
    async with AsyncIndCloudClient(
        api_url="http://localhost:8080",
        api_key="your-device-token"
    ) as client:
        response = await client.send_data("my-sensor", {
            "temperature": 23.5,
            "humidity": 65.2
        })
        print(response.message)

asyncio.run(main())
```

## Configuration

### Client Parameters

```python
IndCloudClient(
    api_url="http://localhost:8080",     # IndCloud API URL
    api_key="your-device-token",          # Device authentication token
    timeout=30,                           # Request timeout in seconds
    retry_attempts=3,                     # Number of retry attempts
    retry_delay=1.0,                      # Initial retry delay in seconds
    verify_ssl=True                       # SSL certificate verification
)
```

### Environment Variables

You can also configure the SDK using environment variables:

```python
import os
from indcloud import IndCloudClient
from indcloud.utils import get_env_or_raise

client = IndCloudClient(
    api_url=get_env_or_raise("SENSORVISION_API_URL"),
    api_key=get_env_or_raise("SENSORVISION_API_KEY")
)
```

## Examples

### Raspberry Pi DHT22 Sensor

```python
import time
import Adafruit_DHT
from indcloud import IndCloudClient

# Setup
sensor = Adafruit_DHT.DHT22
pin = 4
client = IndCloudClient(
    api_url="http://localhost:8080",
    api_key="your-token"
)

# Read and send data every 60 seconds
while True:
    humidity, temperature = Adafruit_DHT.read_retry(sensor, pin)

    if humidity and temperature:
        client.send_data("raspberry-pi-dht22", {
            "temperature": round(temperature, 2),
            "humidity": round(humidity, 2)
        })

    time.sleep(60)
```

### Async Batch Processing

```python
import asyncio
from indcloud import AsyncIndCloudClient

async def send_batch(client, device_ids, data):
    tasks = [
        client.send_data(device_id, data[device_id])
        for device_id in device_ids
    ]
    results = await asyncio.gather(*tasks, return_exceptions=True)
    return results

async def main():
    async with AsyncIndCloudClient(
        api_url="http://localhost:8080",
        api_key="your-token"
    ) as client:
        device_data = {
            "sensor-001": {"temperature": 23.5},
            "sensor-002": {"temperature": 24.1},
            "sensor-003": {"temperature": 22.8},
        }

        results = await send_batch(client, device_data.keys(), device_data)
        print(f"Sent data for {len(results)} devices")

asyncio.run(main())
```

### Multi-Sensor Monitoring

```python
import time
from indcloud import IndCloudClient

def read_all_sensors():
    """Read from multiple sensor types."""
    return {
        # Environmental sensors
        "temperature": 23.5,
        "humidity": 65.2,
        "pressure": 1013.25,

        # Air quality sensors
        "co2_ppm": 450,
        "pm25": 12.5,

        # Power monitoring
        "voltage": 220.5,
        "current": 0.85,
        "power_kw": 0.187
    }

with IndCloudClient(
    api_url="http://localhost:8080",
    api_key="your-token"
) as client:
    while True:
        data = read_all_sensors()
        client.send_data("multi-sensor-station", data)
        time.sleep(30)
```

### Dynamic Variables (Custom Sensors)

IndCloud supports **dynamic variable provisioning** - send ANY variable name and it's automatically created!

```python
from indcloud import IndCloudClient

with IndCloudClient(
    api_url="http://localhost:8080",
    api_key="your-token"
) as client:
    # Standard variables work as expected
    client.send_data("my-device", {
        "temperature": 23.5,
        "humidity": 65.2
    })

    # Custom variables are auto-provisioned on first use!
    # No schema changes needed - just send them!
    client.send_data("my-device", {
        "soil_moisture": 45.0,        # Custom sensor
        "light_level_lux": 850.0,     # Custom sensor
        "battery_voltage": 3.7,        # Device telemetry
        "my_special_reading": 100.0    # Any variable name works!
    })

    # Variables appear in the API and dashboard automatically
    # GET /api/v1/devices/{deviceId}/variables - lists all auto-provisioned variables
    # GET /api/v1/devices/{deviceId}/variables/latest - get latest values
```

**Variable Naming:**
- Use snake_case or camelCase
- Avoid special characters
- Display names are auto-generated (e.g., "soil_moisture" → "Soil Moisture")

## Error Handling

The SDK provides specific exception types for different error scenarios:

```python
from indcloud import IndCloudClient
from indcloud.exceptions import (
    AuthenticationError,
    ValidationError,
    NetworkError,
    ServerError,
    RateLimitError
)

client = IndCloudClient(
    api_url="http://localhost:8080",
    api_key="your-token"
)

try:
    response = client.send_data("my-sensor", {
        "temperature": 23.5
    })
except AuthenticationError:
    print("Invalid API key")
except ValidationError:
    print("Invalid data format")
except RateLimitError:
    print("Rate limit exceeded, slow down")
except NetworkError:
    print("Network connection failed")
except ServerError:
    print("Server error, try again later")
```

## API Reference

### IndCloudClient

#### `send_data(device_id: str, data: Dict[str, float]) -> IngestionResponse`

Send telemetry data to IndCloud.

**Parameters:**
- `device_id` (str): Unique identifier for the device
- `data` (Dict[str, float]): Dictionary of variable names to numeric values

**Returns:**
- `IngestionResponse`: Response object with success status and message

**Raises:**
- `ValueError`: If device_id or data is invalid
- `AuthenticationError`: If API key is invalid
- `NetworkError`: If network request fails
- `ServerError`: If server returns 5xx error

### AsyncIndCloudClient

Same as `IndCloudClient` but with async/await support.

#### `async send_data(device_id: str, data: Dict[str, float]) -> IngestionResponse`

Asynchronous version of `send_data()`.

## Testing

Run tests with pytest:

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=indcloud --cov-report=html

# Run specific test file
pytest tests/test_client.py

# Run with verbose output
pytest -v
```

## Development

```bash
# Clone the repository
git clone https://github.com/CodeFleck/indcloud.git
cd indcloud/indcloud-sdk

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install in development mode
pip install -e ".[dev]"

# Run tests
pytest

# Run type checking
mypy indcloud

# Format code
black indcloud tests examples
isort indcloud tests examples

# Lint code
flake8 indcloud tests
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

- **Documentation**: [GitHub Wiki](https://github.com/CodeFleck/indcloud/wiki)
- **Issues**: [GitHub Issues](https://github.com/CodeFleck/indcloud/issues)
- **Repository**: [GitHub](https://github.com/CodeFleck/indcloud)

## Related Projects

- [IndCloud](https://github.com/CodeFleck/indcloud) - Main IoT monitoring platform
- [JavaScript/TypeScript SDK](../indcloud-sdk-js/) - JavaScript/TypeScript SDK for IndCloud
- [Java SDK](../indcloud-sdk-java/) - Java SDK for IndCloud
- ESP32/Arduino - See Integration Wizard in the IndCloud dashboard

## Changelog

### Version 0.1.0 (2024-01-01)

- Initial release
- Synchronous and asynchronous clients
- Device token authentication
- Automatic retry logic
- Comprehensive error handling
- Full type hints support
- Example code for Raspberry Pi and batch processing
