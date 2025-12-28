import asyncio
import socket
from datetime import datetime, timezone

import pytest
import responses
from aiohttp import web

from indcloud import (
    IndCloudClient,
    AsyncIndCloudClient,
    RateLimitError,
    ServerError,
)
from indcloud.models import TelemetryData
from indcloud.utils import validate_device_id, validate_telemetry_data


@pytest.fixture
def sync_client():
    client = IndCloudClient(
        api_url="http://api.indcloud.local",
        api_key="test-token",
        retry_attempts=3,
        retry_delay=0,
        timeout=1,
        verify_ssl=False,
    )
    yield client
    client.close()


@pytest.fixture
def unused_tcp_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    return port

@responses.activate
def test_send_data_success(sync_client):
    responses.add(
        responses.POST,
        "http://api.indcloud.local/api/v1/ingest/device-123",
        json={
            "success": True,
            "message": "Data ingested successfully",
            "deviceId": "device-123",
            "timestamp": "2024-01-01T00:00:00Z",
        },
        status=200,
    )

    response = sync_client.send_data("device-123", {"temperature": 23.4})

    assert response.success is True
    assert response.device_id == "device-123"
    assert response.message == "Data ingested successfully"
    assert len(responses.calls) == 1

@responses.activate
def test_send_data_validation_error(sync_client):
    with pytest.raises(ValueError):
        sync_client.send_data("", {"temperature": 23.4})

    with pytest.raises(ValueError):
        sync_client.send_data("device-123", [])  # type: ignore[arg-type]

@responses.activate
def test_send_data_rate_limit(sync_client):
    responses.add(
        responses.POST,
        "http://api.indcloud.local/api/v1/ingest/device-123",
        status=429,
        json={"message": "Too many requests"},
    )

    with pytest.raises(RateLimitError):
        sync_client.send_data("device-123", {"temperature": 23.4})

    assert len(responses.calls) == 1

@responses.activate
def test_send_data_server_error_retries(sync_client):
    responses.add(
        responses.POST,
        "http://api.indcloud.local/api/v1/ingest/device-123",
        status=500,
        json={"message": "Server error"},
    )

    with pytest.raises(ServerError):
        sync_client.send_data("device-123", {"temperature": 23.4})

    assert len(responses.calls) == sync_client.config.retry_attempts


@pytest.mark.asyncio
async def test_async_send_data_success(unused_tcp_port):
    received_payload = {}

    async def handler(request: web.Request):
        received_payload.update(await request.json())
        return web.json_response({
            "success": True,
            "message": "ok",
            "deviceId": request.match_info["device_id"],
        })

    app = web.Application()
    app.router.add_post("/api/v1/ingest/{device_id}", handler)

    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "127.0.0.1", unused_tcp_port)
    await site.start()

    api_url = f"http://127.0.0.1:{unused_tcp_port}"

    try:
        client = AsyncIndCloudClient(
            api_url=api_url,
            api_key="test-token",
            retry_attempts=1,
            retry_delay=0,
            timeout=2,
            verify_ssl=False,
        )

        async with client:
            response = await client.send_data("device-123", {"temperature": 23.4})

        assert response.success is True
        assert received_payload["temperature"] == 23.4
    finally:
        await runner.cleanup()


@pytest.mark.asyncio
async def test_async_send_data_rate_limit(unused_tcp_port):
    async def handler(request: web.Request):
        return web.Response(status=429, text="Too many requests")

    app = web.Application()
    app.router.add_post("/api/v1/ingest/{device_id}", handler)

    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "127.0.0.1", unused_tcp_port)
    await site.start()

    api_url = f"http://127.0.0.1:{unused_tcp_port}"

    try:
        client = AsyncIndCloudClient(
            api_url=api_url,
            api_key="test-token",
            retry_attempts=2,
            retry_delay=0,
            timeout=2,
            verify_ssl=False,
        )

        async with client:
            with pytest.raises(RateLimitError):
                await client.send_data("device-123", {"temperature": 23.4})
    finally:
        await runner.cleanup()


def test_telemetry_data_to_dict_includes_timestamp():
    ts = datetime(2024, 1, 1, tzinfo=timezone.utc)
    telemetry = TelemetryData(
        device_id="device-123",
        variables={"temperature": 23.4},
        timestamp=ts,
    )

    payload = telemetry.to_dict()

    assert payload["temperature"] == 23.4
    assert payload["timestamp"] == ts.isoformat()


@pytest.mark.parametrize(
    "device_id",
    [None, ""]
)
def test_validate_device_id_invalid(device_id):
    with pytest.raises(ValueError):
        validate_device_id(device_id)  # type: ignore[arg-type]


@pytest.mark.parametrize(
    "data",
    [
        None,
        {},
        {"temperature": "hot"},
        {1: 23.4},
    ],
)
def test_validate_telemetry_data_invalid(data):
    with pytest.raises(ValueError):
        validate_telemetry_data(data)  # type: ignore[arg-type]
