"""
Synchronous and asynchronous clients for SensorVision API.
"""
import logging
from typing import Dict, Any, Optional
from urllib.parse import urljoin

try:
    import requests
    REQUESTS_AVAILABLE = True
except ImportError:
    REQUESTS_AVAILABLE = False

try:
    import aiohttp
    import asyncio
    AIOHTTP_AVAILABLE = True
except ImportError:
    AIOHTTP_AVAILABLE = False

from .auth import TokenAuth
from .models import ClientConfig, IngestionResponse, TelemetryData
from .utils import validate_device_id, validate_telemetry_data, retry_on_failure
from .exceptions import (
    AuthenticationError,
    NetworkError,
    ServerError,
    ValidationError,
    RateLimitError
)

logger = logging.getLogger(__name__)


class SensorVisionClient:
    """Synchronous client for SensorVision API."""

    def __init__(
        self,
        api_url: str,
        api_key: str,
        timeout: int = 30,
        retry_attempts: int = 3,
        retry_delay: float = 1.0,
        verify_ssl: bool = True
    ):
        """
        Initialize SensorVision synchronous client.

        Args:
            api_url: Base URL of SensorVision API (e.g., "http://localhost:8080")
            api_key: Device API token for authentication
            timeout: Request timeout in seconds
            retry_attempts: Number of retry attempts for failed requests
            retry_delay: Initial delay between retries in seconds
            verify_ssl: Whether to verify SSL certificates

        Raises:
            ImportError: If requests library is not installed
        """
        if not REQUESTS_AVAILABLE:
            raise ImportError(
                "requests library is required for synchronous client. "
                "Install it with: pip install requests"
            )

        self.config = ClientConfig(
            api_url=api_url.rstrip('/'),
            api_key=api_key,
            timeout=timeout,
            retry_attempts=retry_attempts,
            retry_delay=retry_delay,
            verify_ssl=verify_ssl
        )
        self.auth = TokenAuth(api_key)
        self.session = requests.Session()
        self.session.headers.update(self.auth.get_headers())

    def send_data(
        self,
        device_id: str,
        data: Dict[str, float]
    ) -> IngestionResponse:
        """
        Send telemetry data to SensorVision.

        DYNAMIC VARIABLES: Send ANY variable names in the data dictionary.
        Variables are automatically provisioned on first use - no schema changes needed!

        Args:
            device_id: Unique identifier for the device
            data: Dictionary of variable names to values (must be numeric).
                  Variable names can be anything - they're auto-created on first use.

        Returns:
            IngestionResponse with success status and message

        Raises:
            ValidationError: If device_id or data is invalid
            AuthenticationError: If API key is invalid
            NetworkError: If network request fails
            ServerError: If server returns 5xx error

        Example:
            >>> client = SensorVisionClient("http://localhost:8080", "your-api-key")
            >>> # Standard variables
            >>> response = client.send_data("sensor-001", {
            ...     "temperature": 23.5,
            ...     "humidity": 65.2
            ... })
            >>> # Custom variables are auto-provisioned!
            >>> response = client.send_data("sensor-001", {
            ...     "soil_moisture": 45.0,
            ...     "light_level": 850.0,
            ...     "my_custom_sensor": 100.0
            ... })
            >>> print(response.message)
            Data ingested successfully
        """
        validate_device_id(device_id)
        validate_telemetry_data(data)

        return self._send_data_with_retry(device_id, data)

    def _send_data_with_retry(
        self,
        device_id: str,
        data: Dict[str, float]
    ) -> IngestionResponse:
        """Internal method with retry logic using configured retry parameters."""
        url = urljoin(self.config.api_url, f"/api/v1/ingest/{device_id}")

        last_exception = None
        backoff = self.config.retry_delay

        for attempt in range(self.config.retry_attempts):
            if attempt > 0:
                logger.debug(f"Retry attempt {attempt + 1}/{self.config.retry_attempts} after {backoff}s delay")
                import time
                time.sleep(backoff)
                backoff *= 2  # Exponential backoff

            try:
                logger.debug(f"Sending data to {url}: {data}")
                response = self.session.post(
                    url,
                    json=data,
                    timeout=self.config.timeout,
                    verify=self.config.verify_ssl
                )

                # Handle different HTTP status codes
                if response.status_code == 200 or response.status_code == 201:
                    logger.info(f"Successfully sent data for device {device_id}")
                    return IngestionResponse.from_dict(response.json())

                elif response.status_code == 401 or response.status_code == 403:
                    # Don't retry authentication errors
                    raise AuthenticationError(
                        f"Authentication failed: {response.text}"
                    )

                elif response.status_code == 400:
                    # Don't retry validation errors
                    raise ValidationError(
                        f"Invalid data format: {response.text}"
                    )

                elif response.status_code == 429:
                    # Don't retry rate limit errors
                    raise RateLimitError(
                        f"Rate limit exceeded: {response.text}"
                    )

                elif response.status_code >= 500:
                    # Retry server errors
                    last_exception = ServerError(
                        f"Server error ({response.status_code}): {response.text}"
                    )
                    logger.warning(f"Server error on attempt {attempt + 1}: {last_exception}")
                    continue

                else:
                    raise NetworkError(
                        f"Unexpected response ({response.status_code}): {response.text}"
                    )

            except (AuthenticationError, ValidationError, RateLimitError):
                # Don't retry these - raise immediately
                raise
            except requests.exceptions.Timeout as e:
                # Retry timeout errors
                last_exception = NetworkError(f"Request timeout: {str(e)}")
                logger.warning(f"Timeout on attempt {attempt + 1}: {last_exception}")
                continue
            except requests.exceptions.ConnectionError as e:
                # Retry connection errors
                last_exception = NetworkError(f"Connection error: {str(e)}")
                logger.warning(f"Connection error on attempt {attempt + 1}: {last_exception}")
                continue
            except requests.exceptions.RequestException as e:
                # Retry other request errors
                last_exception = NetworkError(f"Request failed: {str(e)}")
                logger.warning(f"Request error on attempt {attempt + 1}: {last_exception}")
                continue

        # All retries exhausted
        if last_exception:
            raise last_exception
        raise NetworkError("Request failed after all retry attempts")

    def close(self) -> None:
        """Close the HTTP session."""
        self.session.close()

    def __enter__(self):
        """Context manager entry."""
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.close()


class AsyncSensorVisionClient:
    """Asynchronous client for SensorVision API."""

    def __init__(
        self,
        api_url: str,
        api_key: str,
        timeout: int = 30,
        retry_attempts: int = 3,
        retry_delay: float = 1.0,
        verify_ssl: bool = True
    ):
        """
        Initialize SensorVision asynchronous client.

        Args:
            api_url: Base URL of SensorVision API
            api_key: Device API token for authentication
            timeout: Request timeout in seconds
            retry_attempts: Number of retry attempts for failed requests
            retry_delay: Initial delay between retries in seconds
            verify_ssl: Whether to verify SSL certificates

        Raises:
            ImportError: If aiohttp library is not installed
        """
        if not AIOHTTP_AVAILABLE:
            raise ImportError(
                "aiohttp library is required for async client. "
                "Install it with: pip install aiohttp"
            )

        self.config = ClientConfig(
            api_url=api_url.rstrip('/'),
            api_key=api_key,
            timeout=timeout,
            retry_attempts=retry_attempts,
            retry_delay=retry_delay,
            verify_ssl=verify_ssl
        )
        self.auth = TokenAuth(api_key)
        self.session: Optional[aiohttp.ClientSession] = None

    async def _get_session(self) -> aiohttp.ClientSession:
        """Get or create aiohttp session."""
        if self.session is None or self.session.closed:
            connector = aiohttp.TCPConnector(
                ssl=self.config.verify_ssl
            )
            timeout = aiohttp.ClientTimeout(total=self.config.timeout)
            self.session = aiohttp.ClientSession(
                headers=self.auth.get_headers(),
                connector=connector,
                timeout=timeout
            )
        return self.session

    async def send_data(
        self,
        device_id: str,
        data: Dict[str, float]
    ) -> IngestionResponse:
        """
        Send telemetry data to SensorVision asynchronously.

        DYNAMIC VARIABLES: Send ANY variable names in the data dictionary.
        Variables are automatically provisioned on first use - no schema changes needed!

        Args:
            device_id: Unique identifier for the device
            data: Dictionary of variable names to values (must be numeric).
                  Variable names can be anything - they're auto-created on first use.

        Returns:
            IngestionResponse with success status and message

        Raises:
            ValidationError: If device_id or data is invalid
            AuthenticationError: If API key is invalid
            NetworkError: If network request fails
            ServerError: If server returns 5xx error

        Example:
            >>> async with AsyncSensorVisionClient("http://localhost:8080", "key") as client:
            ...     # Standard variables
            ...     response = await client.send_data("sensor-001", {
            ...         "temperature": 23.5,
            ...         "humidity": 65.2
            ...     })
            ...     # Custom variables are auto-provisioned!
            ...     response = await client.send_data("sensor-001", {
            ...         "soil_moisture": 45.0,
            ...         "custom_reading": 100.0
            ...     })
        """
        validate_device_id(device_id)
        validate_telemetry_data(data)

        return await self._send_data_with_retry(device_id, data)

    async def _send_data_with_retry(
        self,
        device_id: str,
        data: Dict[str, float]
    ) -> IngestionResponse:
        """Internal method with retry logic."""
        url = urljoin(self.config.api_url, f"/api/v1/ingest/{device_id}")
        session = await self._get_session()

        last_exception = None
        current_delay = self.config.retry_delay

        for attempt in range(self.config.retry_attempts):
            try:
                logger.debug(f"Sending data to {url}: {data}")
                async with session.post(url, json=data) as response:

                    # Handle different HTTP status codes
                    if response.status == 200 or response.status == 201:
                        logger.info(f"Successfully sent data for device {device_id}")
                        response_data = await response.json()
                        return IngestionResponse.from_dict(response_data)

                    elif response.status == 401 or response.status == 403:
                        text = await response.text()
                        raise AuthenticationError(
                            f"Authentication failed: {text}"
                        )

                    elif response.status == 400:
                        text = await response.text()
                        raise ValidationError(
                            f"Invalid data format: {text}"
                        )

                    elif response.status == 429:
                        text = await response.text()
                        raise RateLimitError(
                            f"Rate limit exceeded: {text}"
                        )

                    elif response.status >= 500:
                        text = await response.text()
                        raise ServerError(
                            f"Server error ({response.status}): {text}"
                        )

                    else:
                        text = await response.text()
                        raise NetworkError(
                            f"Unexpected response ({response.status}): {text}"
                        )

            except Exception as e:
                # Skip SensorVision exceptions (already handled)
                if isinstance(e, (AuthenticationError, ValidationError, RateLimitError, ServerError)):
                    raise

                last_exception = e
                if attempt < self.config.retry_attempts - 1:
                    await asyncio.sleep(current_delay)
                    current_delay *= 2.0
                else:
                    raise NetworkError(
                        f"Failed after {self.config.retry_attempts} attempts: {str(e)}"
                    ) from e

        raise last_exception  # type: ignore

    async def close(self) -> None:
        """Close the HTTP session."""
        if self.session and not self.session.closed:
            await self.session.close()

    async def __aenter__(self):
        """Async context manager entry."""
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        await self.close()
