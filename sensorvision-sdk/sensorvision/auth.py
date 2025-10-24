"""
Authentication utilities for SensorVision SDK.
"""
from typing import Dict


class TokenAuth:
    """Handles device token authentication for SensorVision API."""

    def __init__(self, api_key: str):
        """
        Initialize token authentication.

        Args:
            api_key: Device API token for authentication
        """
        self.api_key = api_key

    def get_headers(self) -> Dict[str, str]:
        """
        Get authentication headers for API requests.

        Returns:
            Dictionary of HTTP headers including authentication
        """
        return {
            "X-API-Key": self.api_key,
            "Content-Type": "application/json"
        }
