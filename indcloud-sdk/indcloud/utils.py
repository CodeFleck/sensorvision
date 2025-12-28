"""
Utility functions for IndCloud SDK.
"""
import os
import time
from typing import Callable, TypeVar, Optional
from functools import wraps
from .exceptions import (
    NetworkError,
    AuthenticationError,
    ValidationError,
    RateLimitError,
    ServerError,
    IndCloudError
)

T = TypeVar('T')


def get_env_or_raise(var_name: str, default: Optional[str] = None) -> str:
    """
    Get environment variable or raise error if not found.

    Args:
        var_name: Name of environment variable
        default: Default value if not found

    Returns:
        Environment variable value

    Raises:
        ValueError: If environment variable not found and no default
    """
    value = os.getenv(var_name, default)
    if value is None:
        raise ValueError(f"Environment variable {var_name} not set")
    return value


def retry_on_failure(
    max_attempts: int = 3,
    delay: float = 1.0,
    backoff: float = 2.0
) -> Callable[[Callable[..., T]], Callable[..., T]]:
    """
    Decorator to retry function on failure with exponential backoff.

    Args:
        max_attempts: Maximum number of retry attempts
        delay: Initial delay between retries in seconds
        backoff: Backoff multiplier for delay

    Returns:
        Decorated function with retry logic
    """
    def decorator(func: Callable[..., T]) -> Callable[..., T]:
        @wraps(func)
        def wrapper(*args, **kwargs) -> T:
            current_delay = delay
            last_exception = None

            for attempt in range(max_attempts):
                try:
                    return func(*args, **kwargs)
                except (AuthenticationError, ValidationError, RateLimitError, ServerError) as e:
                    # Don't retry these errors, raise immediately
                    raise
                except Exception as e:
                    last_exception = e
                    if attempt < max_attempts - 1:
                        time.sleep(current_delay)
                        current_delay *= backoff
                    else:
                        raise NetworkError(
                            f"Failed after {max_attempts} attempts: {str(e)}"
                        ) from e

            # This should never be reached, but keeps type checker happy
            raise last_exception  # type: ignore

        return wrapper
    return decorator


def validate_device_id(device_id: str) -> None:
    """
    Validate device ID format.

    Args:
        device_id: Device identifier to validate

    Raises:
        ValidationError: If device ID is invalid
    """
    if not device_id or not isinstance(device_id, str):
        raise ValidationError("Device ID must be a non-empty string")

    if len(device_id) > 255:
        raise ValidationError("Device ID must be less than 255 characters")


def validate_telemetry_data(data: dict) -> None:
    """
    Validate telemetry data format.

    Args:
        data: Telemetry data dictionary to validate

    Raises:
        ValidationError: If telemetry data is invalid
    """
    if not isinstance(data, dict):
        raise ValidationError("Telemetry data must be a dictionary")

    if not data:
        raise ValidationError("Telemetry data cannot be empty")

    for key, value in data.items():
        if not isinstance(key, str):
            raise ValidationError(f"Telemetry key must be string, got {type(key)}")

        if not isinstance(value, (int, float, bool)):
            raise ValidationError(
                f"Telemetry value for '{key}' must be numeric or boolean, "
                f"got {type(value)}"
            )
