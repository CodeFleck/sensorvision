"""
Unit tests for utility functions.
"""
import pytest
from sensorvision.utils import (
    validate_device_id,
    validate_telemetry_data,
    get_env_or_raise
)


class TestValidateDeviceId:
    """Test device ID validation."""

    def test_valid_device_id(self):
        """Test valid device IDs."""
        validate_device_id("sensor-001")
        validate_device_id("my-device")
        validate_device_id("a" * 255)

    def test_invalid_device_id_empty(self):
        """Test empty device ID."""
        with pytest.raises(ValueError, match="non-empty string"):
            validate_device_id("")

    def test_invalid_device_id_none(self):
        """Test None device ID."""
        with pytest.raises(ValueError):
            validate_device_id(None)

    def test_invalid_device_id_too_long(self):
        """Test device ID that's too long."""
        with pytest.raises(ValueError, match="less than 255"):
            validate_device_id("a" * 256)


class TestValidateTelemetryData:
    """Test telemetry data validation."""

    def test_valid_telemetry_data(self):
        """Test valid telemetry data."""
        validate_telemetry_data({"temperature": 23.5})
        validate_telemetry_data({"temp": 23.5, "humidity": 65.2})
        validate_telemetry_data({"value": 100, "enabled": True})

    def test_invalid_telemetry_data_not_dict(self):
        """Test non-dict telemetry data."""
        with pytest.raises(ValueError, match="must be a dictionary"):
            validate_telemetry_data("not a dict")

        with pytest.raises(ValueError, match="must be a dictionary"):
            validate_telemetry_data([1, 2, 3])

    def test_invalid_telemetry_data_empty(self):
        """Test empty telemetry data."""
        with pytest.raises(ValueError, match="cannot be empty"):
            validate_telemetry_data({})

    def test_invalid_telemetry_data_non_string_key(self):
        """Test telemetry data with non-string key."""
        with pytest.raises(ValueError, match="key must be string"):
            validate_telemetry_data({123: 45.6})

    def test_invalid_telemetry_data_non_numeric_value(self):
        """Test telemetry data with non-numeric value."""
        with pytest.raises(ValueError, match="must be numeric or boolean"):
            validate_telemetry_data({"temp": "23.5"})

        with pytest.raises(ValueError, match="must be numeric or boolean"):
            validate_telemetry_data({"data": {"nested": "value"}})


class TestGetEnvOrRaise:
    """Test environment variable retrieval."""

    def test_get_existing_env(self, monkeypatch):
        """Test getting existing environment variable."""
        monkeypatch.setenv("TEST_VAR", "test_value")
        assert get_env_or_raise("TEST_VAR") == "test_value"

    def test_get_env_with_default(self):
        """Test getting non-existent env with default."""
        value = get_env_or_raise("NON_EXISTENT_VAR", "default_value")
        assert value == "default_value"

    def test_get_env_missing_no_default(self):
        """Test getting non-existent env without default."""
        with pytest.raises(ValueError, match="not set"):
            get_env_or_raise("NON_EXISTENT_VAR_12345")
