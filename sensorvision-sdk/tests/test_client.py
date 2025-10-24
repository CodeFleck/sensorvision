"""
Unit tests for SensorVision synchronous client.
"""
import pytest
from unittest.mock import Mock, patch, MagicMock
from sensorvision import SensorVisionClient, IngestionResponse
from sensorvision.exceptions import (
    AuthenticationError,
    ValidationError,
    NetworkError,
    ServerError,
    RateLimitError
)


@pytest.fixture
def client():
    """Create a test client."""
    with patch('sensorvision.client.requests'):
        return SensorVisionClient(
            api_url="http://test.local:8080",
            api_key="test-key"
        )


@pytest.fixture
def mock_response():
    """Create a mock HTTP response."""
    response = Mock()
    response.status_code = 200
    response.json.return_value = {
        "success": True,
        "message": "Data ingested successfully",
        "deviceId": "test-device",
        "timestamp": "2024-01-01T12:00:00Z"
    }
    return response


class TestClientInitialization:
    """Test client initialization."""

    def test_client_init_success(self):
        """Test successful client initialization."""
        with patch('sensorvision.client.requests'):
            client = SensorVisionClient(
                api_url="http://test.local:8080",
                api_key="test-key"
            )
            assert client.config.api_url == "http://test.local:8080"
            assert client.config.api_key == "test-key"
            assert client.config.timeout == 30
            assert client.config.retry_attempts == 3

    def test_client_init_custom_params(self):
        """Test client initialization with custom parameters."""
        with patch('sensorvision.client.requests'):
            client = SensorVisionClient(
                api_url="http://test.local:8080/",
                api_key="test-key",
                timeout=60,
                retry_attempts=5,
                retry_delay=2.0
            )
            assert client.config.api_url == "http://test.local:8080"
            assert client.config.timeout == 60
            assert client.config.retry_attempts == 5
            assert client.config.retry_delay == 2.0


class TestSendData:
    """Test send_data method."""

    def test_send_data_success(self, client, mock_response):
        """Test successful data sending."""
        client.session.post = Mock(return_value=mock_response)

        response = client.send_data("test-device", {
            "temperature": 23.5,
            "humidity": 65.2
        })

        assert isinstance(response, IngestionResponse)
        assert response.success is True
        assert response.message == "Data ingested successfully"
        assert response.device_id == "test-device"

    def test_send_data_with_201_status(self, client, mock_response):
        """Test successful data sending with 201 status."""
        mock_response.status_code = 201
        client.session.post = Mock(return_value=mock_response)

        response = client.send_data("test-device", {"temperature": 23.5})
        assert response.success is True

    def test_send_data_invalid_device_id(self, client):
        """Test sending data with invalid device ID."""
        with pytest.raises(ValueError):
            client.send_data("", {"temperature": 23.5})

        with pytest.raises(ValueError):
            client.send_data("a" * 256, {"temperature": 23.5})

    def test_send_data_invalid_data(self, client):
        """Test sending invalid telemetry data."""
        with pytest.raises(ValueError):
            client.send_data("test-device", {})

        with pytest.raises(ValueError):
            client.send_data("test-device", {"temp": "not a number"})

        with pytest.raises(ValueError):
            client.send_data("test-device", "not a dict")


class TestErrorHandling:
    """Test error handling."""

    def test_authentication_error(self, client):
        """Test authentication error handling."""
        mock_response = Mock()
        mock_response.status_code = 401
        mock_response.text = "Invalid API key"
        client.session.post = Mock(return_value=mock_response)

        with pytest.raises(AuthenticationError):
            client.send_data("test-device", {"temperature": 23.5})

    def test_forbidden_error(self, client):
        """Test forbidden error handling."""
        mock_response = Mock()
        mock_response.status_code = 403
        mock_response.text = "Access forbidden"
        client.session.post = Mock(return_value=mock_response)

        with pytest.raises(AuthenticationError):
            client.send_data("test-device", {"temperature": 23.5})

    def test_validation_error(self, client):
        """Test validation error handling."""
        mock_response = Mock()
        mock_response.status_code = 400
        mock_response.text = "Invalid data format"
        client.session.post = Mock(return_value=mock_response)

        with pytest.raises(ValidationError):
            client.send_data("test-device", {"temperature": 23.5})

    def test_rate_limit_error(self, client):
        """Test rate limit error handling."""
        mock_response = Mock()
        mock_response.status_code = 429
        mock_response.text = "Rate limit exceeded"
        client.session.post = Mock(return_value=mock_response)

        with pytest.raises(RateLimitError):
            client.send_data("test-device", {"temperature": 23.5})

    def test_server_error(self, client):
        """Test server error handling."""
        mock_response = Mock()
        mock_response.status_code = 500
        mock_response.text = "Internal server error"
        client.session.post = Mock(return_value=mock_response)

        with pytest.raises(ServerError):
            client.send_data("test-device", {"temperature": 23.5})

    def test_unexpected_status_code(self, client):
        """Test handling of unexpected status codes."""
        mock_response = Mock()
        mock_response.status_code = 418  # I'm a teapot
        mock_response.text = "Unexpected error"
        client.session.post = Mock(return_value=mock_response)

        with pytest.raises(NetworkError):
            client.send_data("test-device", {"temperature": 23.5})


class TestContextManager:
    """Test context manager functionality."""

    def test_context_manager(self):
        """Test using client as context manager."""
        with patch('sensorvision.client.requests'):
            with SensorVisionClient(
                api_url="http://test.local:8080",
                api_key="test-key"
            ) as client:
                assert client is not None

    def test_close_method(self, client):
        """Test close method."""
        client.session.close = Mock()
        client.close()
        client.session.close.assert_called_once()
