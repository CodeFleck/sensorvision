"""
Data models for SensorVision SDK.
"""
from dataclasses import dataclass
from typing import Dict, Any, Optional
from datetime import datetime


@dataclass
class TelemetryData:
    """Represents telemetry data to be sent to SensorVision."""

    device_id: str
    variables: Dict[str, float]
    timestamp: Optional[datetime] = None

    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for JSON serialization."""
        data = {**self.variables}
        if self.timestamp:
            data["timestamp"] = self.timestamp.isoformat()
        return data


@dataclass
class IngestionResponse:
    """Response from telemetry ingestion."""

    success: bool
    message: str
    device_id: Optional[str] = None
    timestamp: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "IngestionResponse":
        """Create from API response dictionary."""
        return cls(
            success=data.get("success", True),
            message=data.get("message", "Data ingested successfully"),
            device_id=data.get("deviceId"),
            timestamp=data.get("timestamp")
        )


@dataclass
class ClientConfig:
    """Configuration for SensorVision client."""

    api_url: str
    api_key: str
    timeout: int = 30
    retry_attempts: int = 3
    retry_delay: float = 1.0
    verify_ssl: bool = True
