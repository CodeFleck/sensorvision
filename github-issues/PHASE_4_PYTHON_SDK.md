# Phase 4: Python SDK for Simplified IoT Integration

**Labels:** `enhancement`, `sdk`, `python`
**Milestone:** Integration Simplification
**Estimated Duration:** 1 week
**Priority:** Medium (after ESP32 SDK)
**Depends on:** Phase 0 (Quick Wins) ✅ COMPLETE

## 🐍 Objectives

Create a production-ready Python SDK that makes SensorVision integration trivial for:
- Raspberry Pi projects
- Python-based IoT applications
- Data science and ML workflows
- Server-side telemetry collection

## ✅ Tasks

### Core SDK Development
- [ ] Create Python package structure (`sensorvision-sdk`)
- [ ] Implement synchronous client with requests
- [ ] Implement async client with asyncio/aiohttp
- [ ] Add MQTT support for real-time data streaming
- [ ] Implement automatic retries and error handling
- [ ] Add connection pooling and rate limiting

### Features
- [ ] Device token authentication
- [ ] Auto-device creation support
- [ ] Type hints for IDE autocomplete
- [ ] Comprehensive error messages
- [ ] Logging integration (stdlib logging)
- [ ] Configuration via environment variables

### Quality & Testing
- [ ] Add type hints throughout
- [ ] Mypy validation passing
- [ ] Unit tests with pytest (>90% coverage)
- [ ] Integration tests with mock server
- [ ] Docstrings for all public APIs

### Examples & Documentation
- [ ] Raspberry Pi sensor example (DHT22, BME280)
- [ ] Async batch data collection example
- [ ] MQTT real-time streaming example
- [ ] Integration with popular sensor libraries
- [ ] API reference documentation
- [ ] Quick start guide

### Distribution
- [ ] Package configuration (setup.py, pyproject.toml)
- [ ] README with installation instructions
- [ ] Publish to PyPI
- [ ] Set up GitHub Actions for CI/CD
- [ ] Semantic versioning

## 📦 Package Structure

```
sensorvision-sdk/
├── sensorvision/
│   ├── __init__.py
│   ├── client.py          # SyncClient, AsyncClient
│   ├── mqtt_client.py     # MQTT streaming support
│   ├── auth.py            # Token authentication
│   ├── exceptions.py      # Custom exceptions
│   ├── models.py          # Data models with type hints
│   └── utils.py           # Helper functions
├── examples/
│   ├── raspberry_pi_dht22.py
│   ├── async_batch_sender.py
│   ├── mqtt_streaming.py
│   └── multi_sensor.py
├── tests/
│   ├── test_client.py
│   ├── test_async_client.py
│   ├── test_mqtt.py
│   └── test_integration.py
├── setup.py
├── pyproject.toml
└── README.md
```

## 💡 Usage Example

```python
from sensorvision import SensorVisionClient

# Simple synchronous usage
client = SensorVisionClient(
    api_url="http://localhost:8080",
    api_key="your-device-token"
)
client.send_data("sensor-001", {
    "temperature": 23.5,
    "humidity": 65.2
})

# Async usage
import asyncio
from sensorvision import AsyncSensorVisionClient

async def send_telemetry():
    async with AsyncSensorVisionClient(
        api_url="http://localhost:8080",
        api_key="token"
    ) as client:
        await client.send_data("sensor-001", {
            "temperature": 23.5,
            "humidity": 65.2
        })

asyncio.run(send_telemetry())

# MQTT streaming
from sensorvision import MQTTClient

mqtt_client = MQTTClient(
    broker_url="mqtt://localhost:1883",
    api_key="token"
)
mqtt_client.stream_data("sensor-001", {
    "temperature": 23.5,
    "humidity": 65.2
})
```

## 🎯 Success Criteria

- [ ] Package published to PyPI
- [ ] >90% test coverage
- [ ] Documentation complete with examples
- [ ] At least 3 working examples
- [ ] Type hints validated with mypy
- [ ] Works on Python 3.8+
- [ ] CI/CD pipeline set up

## 📚 References

- Backend API: `/api/v1/ingest/{deviceId}` in `SimpleIngestionController.java`
- Integration templates: `integration-templates/python-sensor/`
- Phase 0 implementation complete: ✅

## 🔗 Related

- **Depends on:** Phase 0 (Quick Wins) ✅ COMPLETE
- **Related to:** Phase 3 (ESP32/Arduino SDK)
- **Blocks:** Phase 7 (Documentation & Guides)
