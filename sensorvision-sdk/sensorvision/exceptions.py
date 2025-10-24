"""
Custom exceptions for SensorVision SDK.
"""


class SensorVisionError(Exception):
    """Base exception for all SensorVision SDK errors."""
    pass


class AuthenticationError(SensorVisionError):
    """Raised when authentication fails (invalid API key)."""
    pass


class DeviceNotFoundError(SensorVisionError):
    """Raised when the specified device is not found."""
    pass


class ValidationError(SensorVisionError):
    """Raised when data validation fails."""
    pass


class NetworkError(SensorVisionError):
    """Raised when network communication fails."""
    pass


class RateLimitError(SensorVisionError):
    """Raised when rate limit is exceeded."""
    pass


class ServerError(SensorVisionError):
    """Raised when server returns 5xx errors."""
    pass
