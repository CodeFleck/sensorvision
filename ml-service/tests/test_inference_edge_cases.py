"""
Edge case tests for inference endpoints.

Tests boundary conditions, error handling, and potential issues.
"""
import pytest
from datetime import datetime, timezone
from uuid import uuid4
from unittest.mock import Mock, patch, PropertyMock
import numpy as np
import pandas as pd
from fastapi.testclient import TestClient

from app.main import app
from app.engines.anomaly_detection import AnomalyDetectionEngine
from app.models.schemas import MLModelType, AnomalySeverity


@pytest.fixture
def client():
    """Create test client with disabled API key verification."""
    with patch("app.core.config.settings.API_KEY_REQUIRED", False):
        with TestClient(app) as c:
            yield c


class TestPayloadSizeLimits:
    """Test payload size validation."""

    def test_max_telemetry_points_accepted(self, client):
        """Test that exactly 10,000 telemetry points is accepted."""
        model_id = str(uuid4())
        device_id = str(uuid4())

        # Create exactly 10,000 points with proper timestamp format
        telemetry = [
            {
                "timestamp": f"2024-01-{(i // 1440) % 28 + 1:02d}T{(i // 60) % 24:02d}:{i % 60:02d}:00Z",
                "variables": {"temp": 25.0}
            }
            for i in range(10000)
        ]

        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.algorithm = "isolation_forest"
        mock_engine.predict_with_scores.return_value = (
            np.array([-1] * 10000),
            np.array([0.8] * 10000),
            [{"affected_variables": [], "expected_values": {}, "actual_values": {}} for _ in range(10000)]
        )
        mock_engine.get_severity.return_value = AnomalySeverity.HIGH

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            response = client.post(
                "/api/v1/inference/anomaly",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": telemetry,
                }
            )

        # Should succeed (at limit)
        assert response.status_code == 200

    def test_telemetry_exceeds_max_rejected(self, client):
        """Test that >10,000 telemetry points is rejected."""
        model_id = str(uuid4())
        device_id = str(uuid4())

        # Create 10,001 points (over limit)
        telemetry = [
            {
                "timestamp": f"2024-01-01T00:00:00Z",
                "variables": {"temp": 25.0}
            }
            for _ in range(10001)
        ]

        response = client.post(
            "/api/v1/inference/anomaly",
            json={
                "device_id": device_id,
                "organization_id": 1,
                "model_id": model_id,
                "telemetry": telemetry,
            }
        )

        # Should be rejected by Pydantic validation
        assert response.status_code == 422

    def test_batch_max_devices_returns_501(self, client):
        """Test that batch endpoint returns 501 Not Implemented."""
        model_id = str(uuid4())
        device_ids = [str(uuid4()) for _ in range(1000)]

        # Batch endpoint returns 501 Not Implemented regardless of device count
        response = client.post(
            "/api/v1/inference/anomaly/batch",
            json={
                "organization_id": 1,
                "model_id": model_id,
                "device_ids": device_ids,
            }
        )

        assert response.status_code == 501
        assert "not yet implemented" in response.json()["detail"].lower()

    def test_batch_exceeds_max_devices_rejected(self, client):
        """Test that >1,000 devices is rejected."""
        model_id = str(uuid4())
        device_ids = [str(uuid4()) for _ in range(1001)]

        response = client.post(
            "/api/v1/inference/anomaly/batch",
            json={
                "organization_id": 1,
                "model_id": model_id,
                "device_ids": device_ids,
            }
        )

        # Should be rejected by Pydantic validation
        assert response.status_code == 422


class TestTelemetryConversion:
    """Test telemetry data conversion edge cases."""

    def test_empty_variables_dict(self, client):
        """Test telemetry point with empty variables."""
        model_id = str(uuid4())
        device_id = str(uuid4())

        telemetry = [
            {"timestamp": "2024-01-01T00:00:00Z", "variables": {}}
        ]

        mock_engine = Mock(spec=AnomalyDetectionEngine)

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            response = client.post(
                "/api/v1/inference/anomaly",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": telemetry,
                }
            )

        # Should return 400 - no feature columns
        assert response.status_code == 400
        assert "feature" in response.json()["detail"].lower() or "column" in response.json()["detail"].lower()

    def test_special_float_values_infinity(self, client):
        """Test handling of Infinity values."""
        model_id = str(uuid4())
        device_id = str(uuid4())

        # Note: NaN and Infinity aren't valid JSON per spec, but Python's json module
        # may handle them differently depending on version. Test with extreme but valid values.
        telemetry = [
            {"timestamp": "2024-01-01T00:00:00Z", "variables": {"temp": 25.0}},
            {"timestamp": "2024-01-01T00:01:00Z", "variables": {"temp": 1e308}},  # Near max float
        ]

        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.algorithm = "isolation_forest"
        mock_engine.predict_with_scores.return_value = (
            np.array([1, 1]),
            np.array([0.1, 0.1]),
            [
                {"affected_variables": [], "expected_values": {}, "actual_values": {}},
                {"affected_variables": [], "expected_values": {}, "actual_values": {}},
            ]
        )
        mock_engine.get_severity.return_value = AnomalySeverity.LOW

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            response = client.post(
                "/api/v1/inference/anomaly",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": telemetry,
                }
            )

        # Should succeed - extreme but valid float values
        assert response.status_code == 200

    def test_very_large_variable_values(self, client):
        """Test handling of very large numeric values."""
        model_id = str(uuid4())
        device_id = str(uuid4())

        telemetry = [
            {"timestamp": "2024-01-01T00:00:00Z", "variables": {"temp": 1e308}}
        ]

        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.algorithm = "isolation_forest"
        mock_engine.predict_with_scores.return_value = (
            np.array([1]),
            np.array([0.1]),
            [{"affected_variables": [], "expected_values": {}, "actual_values": {}}]
        )
        mock_engine.get_severity.return_value = "LOW"

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            response = client.post(
                "/api/v1/inference/anomaly",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": telemetry,
                }
            )

        # Should succeed - engine will handle large values
        assert response.status_code == 200

    def test_unsorted_timestamps(self, client):
        """Test that unsorted timestamps are handled correctly."""
        model_id = str(uuid4())
        device_id = str(uuid4())

        # Deliberately unsorted
        telemetry = [
            {"timestamp": "2024-01-01T00:02:00Z", "variables": {"temp": 27.0}},
            {"timestamp": "2024-01-01T00:00:00Z", "variables": {"temp": 25.0}},
            {"timestamp": "2024-01-01T00:01:00Z", "variables": {"temp": 26.0}},
        ]

        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.algorithm = "isolation_forest"
        mock_engine.predict_with_scores.return_value = (
            np.array([1, 1, 1]),
            np.array([0.1, 0.2, 0.3]),
            [
                {"affected_variables": [], "expected_values": {}, "actual_values": {}},
                {"affected_variables": [], "expected_values": {}, "actual_values": {}},
                {"affected_variables": [], "expected_values": {}, "actual_values": {}},
            ]
        )
        mock_engine.get_severity.return_value = "LOW"

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            response = client.post(
                "/api/v1/inference/anomaly",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": telemetry,
                }
            )

        # Should succeed - telemetry_to_dataframe sorts by timestamp
        assert response.status_code == 200


class TestErrorHandling:
    """Test error handling in production mode."""

    def test_production_error_messages_sanitized(self, client):
        """Test that production errors don't leak details."""
        model_id = str(uuid4())
        device_id = str(uuid4())

        telemetry = [{"timestamp": "2024-01-01T00:00:00Z", "variables": {"temp": 25.0}}]

        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.predict_with_scores.side_effect = Exception("Internal error: database password is 'secret123'")

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            # Patch the ENVIRONMENT to simulate production
            with patch("app.core.config.settings.ENVIRONMENT", "production"):
                response = client.post(
                    "/api/v1/inference/anomaly",
                    json={
                        "device_id": device_id,
                        "organization_id": 1,
                        "model_id": model_id,
                        "telemetry": telemetry,
                    }
                )

        assert response.status_code == 500
        # Should NOT contain the actual exception message
        assert "secret123" not in response.json()["detail"]
        assert response.json()["detail"] == "Inference failed"

    def test_development_error_messages_include_details(self, client):
        """Test that development errors include details for debugging."""
        model_id = str(uuid4())
        device_id = str(uuid4())

        telemetry = [{"timestamp": "2024-01-01T00:00:00Z", "variables": {"temp": 25.0}}]

        mock_engine = Mock(spec=AnomalyDetectionEngine)
        error_msg = "Missing required column: voltage"
        mock_engine.predict_with_scores.side_effect = ValueError(error_msg)

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            # Default ENVIRONMENT is "development" which is already set
            response = client.post(
                "/api/v1/inference/anomaly",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": telemetry,
                }
            )

        assert response.status_code == 400
        # Should contain the actual exception message in dev mode
        assert error_msg in response.json()["detail"]


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
