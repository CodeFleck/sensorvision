"""
Integration tests for ML Inference endpoints.

Tests the inference routes with mocked models to verify
endpoint behavior, error handling, and response formats.
"""
import pytest
from datetime import datetime, timezone
from uuid import uuid4
from unittest.mock import Mock, patch, MagicMock

import numpy as np
import pandas as pd
from fastapi.testclient import TestClient

from app.main import app
from app.engines.anomaly_detection import AnomalyDetectionEngine
from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
from app.engines.energy_forecasting import EnergyForecastingEngine
from app.engines.equipment_rul import EquipmentRULEngine
from app.models.schemas import AnomalySeverity, MLModelType
from app.services.model_loader import ModelNotFoundError, ModelLoadError


@pytest.fixture
def client():
    """Create test client with disabled API key verification."""
    with patch("app.core.config.settings.API_KEY_REQUIRED", False):
        with TestClient(app) as c:
            yield c


@pytest.fixture
def sample_telemetry():
    """Generate sample telemetry data for testing."""
    return [
        {
            "timestamp": "2024-01-01T00:00:00Z",
            "variables": {"temperature": 25.0, "pressure": 101.3, "vibration": 0.5}
        },
        {
            "timestamp": "2024-01-01T00:01:00Z",
            "variables": {"temperature": 25.5, "pressure": 101.2, "vibration": 0.6}
        },
        {
            "timestamp": "2024-01-01T00:02:00Z",
            "variables": {"temperature": 26.0, "pressure": 101.1, "vibration": 0.55}
        },
    ]


@pytest.fixture
def model_id():
    """Generate a random model ID."""
    return str(uuid4())


@pytest.fixture
def device_id():
    """Generate a random device ID."""
    return str(uuid4())


class TestAnomalyEndpoint:
    """Tests for /api/v1/inference/anomaly endpoint."""

    def test_anomaly_detection_success(self, client, sample_telemetry, model_id, device_id):
        """Test successful anomaly detection."""
        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.algorithm = "isolation_forest"
        mock_engine.predict_with_scores.return_value = (
            np.array([1, 1, -1]),  # labels: last is anomaly
            np.array([0.2, 0.3, 0.8]),  # scores
            [
                {"affected_variables": [], "expected_values": {}, "actual_values": {}},
                {"affected_variables": [], "expected_values": {}, "actual_values": {}},
                {
                    "affected_variables": ["temperature"],
                    "expected_values": {"temperature": 25.0},
                    "actual_values": {"temperature": 26.0}
                },
            ]
        )
        mock_engine.get_severity.return_value = AnomalySeverity.HIGH

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine) as mock_get_model:
            response = client.post(
                "/api/v1/inference/anomaly",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": sample_telemetry,
                }
            )

            # Verify model loader was called correctly
            mock_get_model.assert_called_once()
            call_args = mock_get_model.call_args
            from uuid import UUID
            assert call_args.kwargs["model_id"] == UUID(model_id)
            assert call_args.kwargs["model_type"] == MLModelType.ANOMALY_DETECTION

        assert response.status_code == 200
        data = response.json()
        assert data["device_id"] == device_id
        assert data["model_id"] == model_id
        assert data["prediction_type"] == "ANOMALY_DETECTION"
        assert data["is_anomaly"] is True
        assert data["anomaly_score"] == 0.8
        assert data["severity"] == "HIGH"
        assert "temperature" in data["affected_variables"]

        # Verify engine methods were called
        mock_engine.predict_with_scores.assert_called_once()
        mock_engine.get_severity.assert_called_once_with(0.8)

    def test_anomaly_detection_model_not_found(self, client, sample_telemetry, model_id, device_id):
        """Test 404 when model not found."""
        with patch(
            "app.api.routes.inference._model_loader.get_model",
            side_effect=ModelNotFoundError(f"Model {model_id} not found")
        ):
            response = client.post(
                "/api/v1/inference/anomaly",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": sample_telemetry,
                }
            )

        assert response.status_code == 404
        assert response.json()["detail"] == "Model not found"

    def test_anomaly_detection_missing_model_id(self, client, sample_telemetry, device_id):
        """Test 400 when model_id is missing."""
        response = client.post(
            "/api/v1/inference/anomaly",
            json={
                "device_id": device_id,
                "organization_id": 1,
                "telemetry": sample_telemetry,
            }
        )

        assert response.status_code == 400
        assert "model_id" in response.json()["detail"].lower()

    def test_anomaly_detection_empty_telemetry(self, client, model_id, device_id):
        """Test 422 when telemetry is empty."""
        response = client.post(
            "/api/v1/inference/anomaly",
            json={
                "device_id": device_id,
                "organization_id": 1,
                "model_id": model_id,
                "telemetry": [],
            }
        )

        # FastAPI validation should reject empty telemetry
        assert response.status_code == 422


class TestBatchAnomalyEndpoint:
    """Tests for /api/v1/inference/anomaly/batch endpoint."""

    def test_batch_anomaly_returns_501_not_implemented(self, client, model_id):
        """Test batch endpoint returns 501 Not Implemented."""
        device_ids = [str(uuid4()) for _ in range(3)]

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

    def test_batch_anomaly_provides_alternative_guidance(self, client, model_id):
        """Test batch endpoint suggests using single-device endpoint."""
        device_ids = [str(uuid4())]

        response = client.post(
            "/api/v1/inference/anomaly/batch",
            json={
                "organization_id": 1,
                "model_id": model_id,
                "device_ids": device_ids,
            }
        )

        assert response.status_code == 501
        detail = response.json()["detail"]
        # Should provide guidance on alternative
        assert "/anomaly" in detail or "single-device" in detail.lower()


class TestMaintenanceEndpoint:
    """Tests for /api/v1/inference/maintenance endpoint."""

    def test_maintenance_prediction_success(self, client, sample_telemetry, model_id, device_id):
        """Test successful maintenance prediction."""
        mock_engine = Mock(spec=PredictiveMaintenanceEngine)
        mock_engine.predict_with_probability.return_value = (
            np.array([0, 0, 1]),  # labels: last predicts failure
            np.array([0.2, 0.3, 0.85]),  # probabilities
            [
                {"failure_probability": 0.2, "days_to_failure": None, "risk_level": "LOW", "top_risk_factors": {}},
                {"failure_probability": 0.3, "days_to_failure": None, "risk_level": "MEDIUM", "top_risk_factors": {}},
                {
                    "failure_probability": 0.85,
                    "days_to_failure": 1,
                    "risk_level": "CRITICAL",
                    "top_risk_factors": {"temperature_mean_5": 0.3, "vibration_std_10": 0.25}
                },
            ]
        )

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine) as mock_get_model:
            response = client.post(
                "/api/v1/inference/maintenance",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": sample_telemetry,
                }
            )

            # Verify model loader was called correctly
            mock_get_model.assert_called_once()
            call_args = mock_get_model.call_args
            from uuid import UUID
            assert call_args.kwargs["model_id"] == UUID(model_id)
            assert call_args.kwargs["model_type"] == MLModelType.PREDICTIVE_MAINTENANCE

        assert response.status_code == 200
        data = response.json()
        assert data["device_id"] == device_id
        assert data["prediction_type"] == "PREDICTIVE_MAINTENANCE"
        assert data["maintenance_probability"] == 0.85
        assert data["days_to_maintenance"] == 1
        assert len(data["recommended_actions"]) > 0
        assert "Immediate inspection required" in data["recommended_actions"]

        # Verify engine predict method was called
        mock_engine.predict_with_probability.assert_called_once()

    def test_maintenance_prediction_model_not_found(self, client, sample_telemetry, model_id, device_id):
        """Test 404 when model not found."""
        with patch(
            "app.api.routes.inference._model_loader.get_model",
            side_effect=ModelNotFoundError(f"Model {model_id} not found")
        ):
            response = client.post(
                "/api/v1/inference/maintenance",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": sample_telemetry,
                }
            )

        assert response.status_code == 404


class TestEnergyEndpoint:
    """Tests for /api/v1/inference/energy endpoint."""

    def test_energy_forecast_success(self, client, model_id, device_id):
        """Test successful energy forecast."""
        # Telemetry with energy consumption
        telemetry = [
            {"timestamp": f"2024-01-01T{i:02d}:00:00Z", "variables": {"energy_consumption": 50.0 + i * 2}}
            for i in range(24)
        ]

        mock_engine = Mock(spec=EnergyForecastingEngine)
        forecast_df = pd.DataFrame({
            "timestamp": pd.date_range(start="2024-01-02", periods=24, freq="1H"),
            "predicted_consumption": np.random.uniform(45, 65, 24),
        })
        predictions = forecast_df["predicted_consumption"].values
        mock_engine.forecast.return_value = (forecast_df, predictions)

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine) as mock_get_model:
            response = client.post(
                "/api/v1/inference/energy?horizon=24h",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": telemetry,
                }
            )

            # Verify model loader was called correctly
            mock_get_model.assert_called_once()
            call_args = mock_get_model.call_args
            from uuid import UUID
            assert call_args.kwargs["model_id"] == UUID(model_id)
            assert call_args.kwargs["model_type"] == MLModelType.ENERGY_FORECAST

        assert response.status_code == 200
        data = response.json()
        assert data["device_id"] == device_id
        assert data["prediction_type"] == "ENERGY_FORECAST"
        assert data["prediction_horizon"] == "24h"
        assert "forecast_values" in data["prediction_details"]
        assert "mean_consumption" in data["prediction_details"]
        # Verify truncation metadata is included
        assert "forecast_values_count" in data["prediction_details"]
        assert "forecast_values_total" in data["prediction_details"]
        assert "forecast_values_truncated" in data["prediction_details"]

        # Verify engine forecast method was called
        mock_engine.forecast.assert_called_once()

    def test_energy_forecast_no_energy_column_fails(self, client, model_id, device_id):
        """Test 400 when telemetry has no energy-related column."""
        # Telemetry without energy-related column names
        telemetry = [
            {"timestamp": "2024-01-01T00:00:00Z", "variables": {"temperature": 25.0, "humidity": 60.0}}
        ]

        mock_engine = Mock(spec=EnergyForecastingEngine)

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            response = client.post(
                "/api/v1/inference/energy?horizon=24h",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": telemetry,
                }
            )

        assert response.status_code == 400
        detail = response.json()["detail"]
        assert "energy-related column" in detail.lower()
        # Should list available columns
        assert "temperature" in detail or "humidity" in detail

    def test_energy_forecast_invalid_horizon(self, client, model_id, device_id):
        """Test 400 for invalid horizon."""
        telemetry = [
            {"timestamp": "2024-01-01T00:00:00Z", "variables": {"energy": 50.0}}
        ]

        response = client.post(
            "/api/v1/inference/energy?horizon=invalid",
            json={
                "device_id": device_id,
                "organization_id": 1,
                "model_id": model_id,
                "telemetry": telemetry,
            }
        )

        assert response.status_code == 400
        assert "Invalid horizon" in response.json()["detail"]

    def test_energy_forecast_model_not_found(self, client, model_id, device_id):
        """Test 404 when model not found."""
        telemetry = [
            {"timestamp": "2024-01-01T00:00:00Z", "variables": {"energy": 50.0}}
        ]

        with patch(
            "app.api.routes.inference._model_loader.get_model",
            side_effect=ModelNotFoundError(f"Model {model_id} not found")
        ):
            response = client.post(
                "/api/v1/inference/energy?horizon=24h",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": telemetry,
                }
            )

        assert response.status_code == 404


class TestRULEndpoint:
    """Tests for /api/v1/inference/rul endpoint."""

    def test_rul_estimation_success(self, client, sample_telemetry, model_id, device_id):
        """Test successful RUL estimation."""
        mock_engine = Mock(spec=EquipmentRULEngine)
        mock_engine.predict_with_confidence.return_value = (
            np.array([45.0, 42.0, 38.0]),  # predictions (days)
            np.array([30.0, 28.0, 25.0]),  # lower bounds
            np.array([60.0, 56.0, 51.0]),  # upper bounds
        )
        mock_engine.get_feature_importance.return_value = {
            "operating_hours": 0.35,
            "temperature_mean": 0.25,
            "vibration_std": 0.15,
        }

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine) as mock_get_model:
            response = client.post(
                "/api/v1/inference/rul",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": sample_telemetry,
                }
            )

            # Verify model loader was called correctly
            mock_get_model.assert_called_once()
            call_args = mock_get_model.call_args
            from uuid import UUID
            assert call_args.kwargs["model_id"] == UUID(model_id)
            assert call_args.kwargs["model_type"] == MLModelType.EQUIPMENT_RUL

        assert response.status_code == 200
        data = response.json()
        assert data["device_id"] == device_id
        assert data["prediction_type"] == "EQUIPMENT_RUL"
        assert data["prediction_value"] == 38.0  # Last prediction
        assert data["prediction_label"] == "ATTENTION"  # 38 days falls in ATTENTION range (8-90 days)
        assert "rul_days" in data["prediction_details"]
        assert "lower_bound_95" in data["prediction_details"]
        assert "upper_bound_95" in data["prediction_details"]
        assert "interval_width_days" in data["prediction_details"]

        # Verify engine methods were called
        mock_engine.predict_with_confidence.assert_called_once()
        mock_engine.get_feature_importance.assert_called_once()

        # Verify confidence is calculated and within valid range
        assert 0.0 <= data["confidence"] <= 1.0

    def test_rul_estimation_critical_label(self, client, sample_telemetry, model_id, device_id):
        """Test RUL estimation returns CRITICAL for low RUL."""
        mock_engine = Mock(spec=EquipmentRULEngine)
        mock_engine.predict_with_confidence.return_value = (
            np.array([5.0]),  # Very low RUL
            np.array([2.0]),
            np.array([8.0]),
        )
        mock_engine.get_feature_importance.return_value = {}

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            response = client.post(
                "/api/v1/inference/rul",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": [sample_telemetry[0]],  # Single point
                }
            )

        assert response.status_code == 200
        data = response.json()
        assert data["prediction_label"] == "CRITICAL"

    def test_rul_estimation_warning_label(self, client, sample_telemetry, model_id, device_id):
        """Test RUL estimation returns WARNING for moderate RUL."""
        mock_engine = Mock(spec=EquipmentRULEngine)
        mock_engine.predict_with_confidence.return_value = (
            np.array([20.0]),  # Between 7 and 30 days
            np.array([15.0]),
            np.array([25.0]),
        )
        mock_engine.get_feature_importance.return_value = {}

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            response = client.post(
                "/api/v1/inference/rul",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": [sample_telemetry[0]],
                }
            )

        assert response.status_code == 200
        data = response.json()
        assert data["prediction_label"] == "WARNING"

    def test_rul_estimation_healthy_label(self, client, sample_telemetry, model_id, device_id):
        """Test RUL estimation returns HEALTHY for high RUL."""
        mock_engine = Mock(spec=EquipmentRULEngine)
        mock_engine.predict_with_confidence.return_value = (
            np.array([150.0]),  # More than 90 days
            np.array([120.0]),
            np.array([180.0]),
        )
        mock_engine.get_feature_importance.return_value = {}

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            response = client.post(
                "/api/v1/inference/rul",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": [sample_telemetry[0]],
                }
            )

        assert response.status_code == 200
        data = response.json()
        assert data["prediction_label"] == "HEALTHY"

    def test_rul_estimation_model_not_found(self, client, sample_telemetry, model_id, device_id):
        """Test 404 when model not found."""
        with patch(
            "app.api.routes.inference._model_loader.get_model",
            side_effect=ModelNotFoundError(f"Model {model_id} not found")
        ):
            response = client.post(
                "/api/v1/inference/rul",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": sample_telemetry,
                }
            )

        assert response.status_code == 404


class TestGetDevicePredictions:
    """Tests for GET /api/v1/inference/predictions/{device_id}."""

    def test_get_predictions_returns_empty_list(self, client, device_id):
        """Test that endpoint returns empty list (pending implementation)."""
        response = client.get(f"/api/v1/inference/predictions/{device_id}")

        assert response.status_code == 200
        assert response.json() == []

    def test_get_predictions_with_filters(self, client, device_id):
        """Test endpoint with query parameters."""
        response = client.get(
            f"/api/v1/inference/predictions/{device_id}",
            params={"prediction_type": "ANOMALY_DETECTION", "limit": 5}
        )

        assert response.status_code == 200
        assert response.json() == []


class TestModelLoaderIntegration:
    """Tests for ModelLoader service integration."""

    def test_model_load_error_returns_400(self, client, sample_telemetry, model_id, device_id):
        """Test 400 when model fails to load."""
        with patch(
            "app.api.routes.inference._model_loader.get_model",
            side_effect=ModelLoadError("Failed to load model: corrupted file")
        ):
            response = client.post(
                "/api/v1/inference/anomaly",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": sample_telemetry,
                }
            )

        assert response.status_code == 400
        assert "Model load failed" in response.json()["detail"]

    def test_value_error_returns_400(self, client, sample_telemetry, model_id, device_id):
        """Test 400 when inference raises ValueError."""
        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.predict_with_scores.side_effect = ValueError("Missing features: unknown_feature")

        with patch("app.api.routes.inference._model_loader.get_model", return_value=mock_engine):
            response = client.post(
                "/api/v1/inference/anomaly",
                json={
                    "device_id": device_id,
                    "organization_id": 1,
                    "model_id": model_id,
                    "telemetry": sample_telemetry,
                }
            )

        assert response.status_code == 400
        assert "Invalid request" in response.json()["detail"]
