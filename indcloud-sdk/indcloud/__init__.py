"""
IndCloud SDK - Python client library for IndCloud IoT platform.

This SDK provides easy-to-use clients for sending telemetry data to IndCloud.

Example:
    Synchronous usage::

        from indcloud import IndCloudClient

        client = IndCloudClient(
            api_url="http://localhost:8080",
            api_key="your-device-token"
        )

        response = client.send_data("sensor-001", {
            "temperature": 23.5,
            "humidity": 65.2
        })

    Asynchronous usage::

        from indcloud import AsyncIndCloudClient
        import asyncio

        async def main():
            async with AsyncIndCloudClient(
                api_url="http://localhost:8080",
                api_key="your-device-token"
            ) as client:
                response = await client.send_data("sensor-001", {
                    "temperature": 23.5,
                    "humidity": 65.2
                })

        asyncio.run(main())
"""

from .client import IndCloudClient, AsyncIndCloudClient
from .models import TelemetryData, IngestionResponse, ClientConfig
from .exceptions import (
    IndCloudError,
    AuthenticationError,
    DeviceNotFoundError,
    ValidationError,
    NetworkError,
    RateLimitError,
    ServerError
)

__version__ = "0.1.0"
__author__ = "IndCloud Team"
__all__ = [
    "IndCloudClient",
    "AsyncIndCloudClient",
    "TelemetryData",
    "IngestionResponse",
    "ClientConfig",
    "IndCloudError",
    "AuthenticationError",
    "DeviceNotFoundError",
    "ValidationError",
    "NetworkError",
    "RateLimitError",
    "ServerError",
]
