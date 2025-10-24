"""
SensorVision SDK - Python client library for SensorVision IoT platform.

This SDK provides easy-to-use clients for sending telemetry data to SensorVision.

Example:
    Synchronous usage::

        from sensorvision import SensorVisionClient

        client = SensorVisionClient(
            api_url="http://localhost:8080",
            api_key="your-device-token"
        )

        response = client.send_data("sensor-001", {
            "temperature": 23.5,
            "humidity": 65.2
        })

    Asynchronous usage::

        from sensorvision import AsyncSensorVisionClient
        import asyncio

        async def main():
            async with AsyncSensorVisionClient(
                api_url="http://localhost:8080",
                api_key="your-device-token"
            ) as client:
                response = await client.send_data("sensor-001", {
                    "temperature": 23.5,
                    "humidity": 65.2
                })

        asyncio.run(main())
"""

from .client import SensorVisionClient, AsyncSensorVisionClient
from .models import TelemetryData, IngestionResponse, ClientConfig
from .exceptions import (
    SensorVisionError,
    AuthenticationError,
    DeviceNotFoundError,
    ValidationError,
    NetworkError,
    RateLimitError,
    ServerError
)

__version__ = "0.1.0"
__author__ = "SensorVision Team"
__all__ = [
    "SensorVisionClient",
    "AsyncSensorVisionClient",
    "TelemetryData",
    "IngestionResponse",
    "ClientConfig",
    "SensorVisionError",
    "AuthenticationError",
    "DeviceNotFoundError",
    "ValidationError",
    "NetworkError",
    "RateLimitError",
    "ServerError",
]
