"""
Unit tests for IndCloud asynchronous client.
"""
import pytest
from unittest.mock import Mock, patch, AsyncMock, MagicMock
from indcloud import AsyncIndCloudClient
from indcloud.exceptions import (
    AuthenticationError,
    ValidationError,
    NetworkError,
    ServerError
)


@pytest.fixture
def async_client():
    """Create a test async client."""
    with patch('indcloud.client.AIOHTTP_AVAILABLE', True):
        client = AsyncIndCloudClient(
            api_url="http://test.local:8080",
            api_key="test-key"
        )
        # Pre-set a mock session to avoid aiohttp initialization
        client.session = AsyncMock()
        client.session.closed = False
        return client


@pytest.fixture
def mock_response():
    """Create a mock aiohttp response."""
    response = AsyncMock()
    response.status = 200
    response.json = AsyncMock(return_value={
        "success": True,
        "message": "Data ingested successfully",
        "deviceId": "test-device",
        "timestamp": "2024-01-01T12:00:00Z"
    })
    return response


class TestAsyncClientInitialization:
    """Test async client initialization."""

    def test_async_client_init(self):
        """Test async client initialization."""
        with patch('indcloud.client.AIOHTTP_AVAILABLE', True), \
             patch('indcloud.client.aiohttp', create=True):
            client = AsyncIndCloudClient(
                api_url="http://test.local:8080",
                api_key="test-key"
            )
            assert client.config.api_url == "http://test.local:8080"
            assert client.config.api_key == "test-key"

    def test_async_client_custom_params(self):
        """Test async client with custom parameters."""
        with patch('indcloud.client.AIOHTTP_AVAILABLE', True), \
             patch('indcloud.client.aiohttp', create=True):
            client = AsyncIndCloudClient(
                api_url="http://test.local:8080/",
                api_key="test-key",
                timeout=60,
                retry_attempts=5
            )
            assert client.config.api_url == "http://test.local:8080"
            assert client.config.timeout == 60
            assert client.config.retry_attempts == 5


@pytest.mark.asyncio
class TestAsyncSendData:
    """Test async send_data method."""

    async def test_send_data_success(self, async_client, mock_response):
        """Test successful async data sending."""
        mock_session = AsyncMock()
        mock_session.post = MagicMock()
        mock_session.post.return_value.__aenter__.return_value = mock_response

        # Mock the _get_session method to return our mock session
        async_client._get_session = AsyncMock(return_value=mock_session)

        response = await async_client.send_data("test-device", {
            "temperature": 23.5,
            "humidity": 65.2
        })

        assert response.success is True
        assert response.message == "Data ingested successfully"

    async def test_send_data_invalid_device_id(self, async_client):
        """Test async sending with invalid device ID."""
        with pytest.raises(ValueError):
            await async_client.send_data("", {"temperature": 23.5})

    async def test_send_data_invalid_data(self, async_client):
        """Test async sending with invalid data."""
        with pytest.raises(ValueError):
            await async_client.send_data("test-device", {})


@pytest.mark.asyncio
class TestAsyncErrorHandling:
    """Test async error handling."""

    async def test_authentication_error(self, async_client):
        """Test async authentication error."""
        mock_response = AsyncMock()
        mock_response.status = 401
        mock_response.text = AsyncMock(return_value="Invalid API key")

        mock_session = AsyncMock()
        mock_session.post = MagicMock()
        mock_session.post.return_value.__aenter__.return_value = mock_response

        # Mock the _get_session method to return our mock session
        async_client._get_session = AsyncMock(return_value=mock_session)

        with pytest.raises(AuthenticationError):
            await async_client.send_data("test-device", {"temperature": 23.5})

    async def test_server_error(self, async_client):
        """Test async server error."""
        mock_response = AsyncMock()
        mock_response.status = 500
        mock_response.text = AsyncMock(return_value="Server error")

        mock_session = AsyncMock()
        mock_session.post = MagicMock()
        mock_session.post.return_value.__aenter__.return_value = mock_response

        # Mock the _get_session method to return our mock session
        async_client._get_session = AsyncMock(return_value=mock_session)

        with pytest.raises(ServerError):
            await async_client.send_data("test-device", {"temperature": 23.5})


@pytest.mark.asyncio
class TestAsyncContextManager:
    """Test async context manager."""

    async def test_async_context_manager(self):
        """Test using async client as context manager."""
        with patch('indcloud.client.AIOHTTP_AVAILABLE', True), \
             patch('indcloud.client.aiohttp', create=True):
            async with AsyncIndCloudClient(
                api_url="http://test.local:8080",
                api_key="test-key"
            ) as client:
                assert client is not None

    async def test_async_close(self, async_client):
        """Test async close method."""
        async_client.session = AsyncMock()
        async_client.session.closed = False
        async_client.session.close = AsyncMock()

        await async_client.close()
        async_client.session.close.assert_called_once()
