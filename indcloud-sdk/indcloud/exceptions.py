"""
Custom exceptions for IndCloud SDK.
"""


class IndCloudError(Exception):
    """Base exception for all IndCloud SDK errors."""
    pass


class AuthenticationError(IndCloudError):
    """Raised when authentication fails (invalid API key)."""
    pass


class DeviceNotFoundError(IndCloudError):
    """Raised when the specified device is not found."""
    pass


class ValidationError(IndCloudError):
    """Raised when data validation fails."""
    pass


class NetworkError(IndCloudError):
    """Raised when network communication fails."""
    pass


class RateLimitError(IndCloudError):
    """Raised when rate limit is exceeded."""
    pass


class ServerError(IndCloudError):
    """Raised when server returns 5xx errors."""
    pass
