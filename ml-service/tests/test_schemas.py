"""
Tests for Pydantic schemas.
"""
import pytest
from datetime import datetime, timezone
from uuid import uuid4
from pydantic import ValidationError

from app.models.schemas import (
    MLModelType,
    MLModelStatus,
    TrainingJobStatus,
    AnomalySeverity,
    MLModelCreate,
    MLModelUpdate,
    InferenceRequest,
    TelemetryPoint,
    BatchInferenceRequest,
    TrainingJobCreate,
    AnomalyDetectionResult,
)


class TestEnums:
    """Tests for enum types."""

    def test_ml_model_type_values(self):
        assert MLModelType.ANOMALY_DETECTION.value == "ANOMALY_DETECTION"
        assert MLModelType.PREDICTIVE_MAINTENANCE.value == "PREDICTIVE_MAINTENANCE"
        assert MLModelType.ENERGY_FORECAST.value == "ENERGY_FORECAST"
        assert MLModelType.EQUIPMENT_RUL.value == "EQUIPMENT_RUL"

    def test_ml_model_status_values(self):
        assert MLModelStatus.DRAFT.value == "DRAFT"
        assert MLModelStatus.TRAINING.value == "TRAINING"
        assert MLModelStatus.TRAINED.value == "TRAINED"
        assert MLModelStatus.DEPLOYED.value == "DEPLOYED"

    def test_anomaly_severity_values(self):
        assert AnomalySeverity.LOW.value == "LOW"
        assert AnomalySeverity.MEDIUM.value == "MEDIUM"
        assert AnomalySeverity.HIGH.value == "HIGH"
        assert AnomalySeverity.CRITICAL.value == "CRITICAL"


class TestMLModelCreate:
    """Tests for MLModelCreate schema."""

    def test_create_valid_model(self):
        model = MLModelCreate(
            organization_id=1,
            name="Test Model",
            model_type=MLModelType.ANOMALY_DETECTION,
            algorithm="isolation_forest"
        )

        assert model.name == "Test Model"
        assert model.organization_id == 1
        assert model.version == "1.0.0"  # default
        assert model.confidence_threshold == 0.8  # default

    def test_create_with_all_fields(self):
        model = MLModelCreate(
            organization_id=1,
            name="Full Model",
            model_type=MLModelType.PREDICTIVE_MAINTENANCE,
            algorithm="gradient_boosting",
            version="2.0.0",
            hyperparameters={"n_estimators": 100},
            feature_columns=["temp", "pressure"],
            device_scope="SELECTED",
            device_ids=[uuid4(), uuid4()],
            confidence_threshold=0.9,
            anomaly_threshold=0.3
        )

        assert model.version == "2.0.0"
        assert len(model.device_ids) == 2
        assert model.confidence_threshold == 0.9

    def test_create_fails_without_required_fields(self):
        with pytest.raises(ValidationError):
            MLModelCreate(name="Test")  # missing organization_id, model_type, algorithm


class TestMLModelUpdate:
    """Tests for MLModelUpdate schema."""

    def test_update_partial_fields(self):
        update = MLModelUpdate(
            name="Updated Name",
            confidence_threshold=0.95
        )

        assert update.name == "Updated Name"
        assert update.hyperparameters is None

    def test_update_all_fields(self):
        update = MLModelUpdate(
            name="Updated",
            hyperparameters={"key": "value"},
            feature_columns=["new_feature"],
            inference_schedule="0 */5 * * * *",
            confidence_threshold=0.85,
            anomaly_threshold=0.4
        )

        assert update.inference_schedule == "0 */5 * * * *"


class TestInferenceRequest:
    """Tests for InferenceRequest schema."""

    def test_create_valid_request(self):
        request = InferenceRequest(
            device_id=uuid4(),
            organization_id=1,
            telemetry=[
                TelemetryPoint(
                    timestamp=datetime.now(),
                    variables={"temperature": 25.0, "pressure": 101.3}
                )
            ]
        )

        assert request.model_id is None
        assert len(request.telemetry) == 1

    def test_create_with_model_id(self):
        model_id = uuid4()
        request = InferenceRequest(
            device_id=uuid4(),
            organization_id=1,
            model_id=model_id,
            telemetry=[
                TelemetryPoint(
                    timestamp=datetime.now(),
                    variables={"temp": 20.0}
                )
            ]
        )

        assert request.model_id == model_id

    def test_fails_with_empty_telemetry(self):
        with pytest.raises(ValidationError):
            InferenceRequest(
                device_id=uuid4(),
                organization_id=1,
                telemetry=[]
            )


class TestTelemetryPoint:
    """Tests for TelemetryPoint schema."""

    def test_create_telemetry_point(self):
        # Use timezone-aware datetime since the validator enforces UTC
        now = datetime.now(timezone.utc)
        point = TelemetryPoint(
            timestamp=now,
            variables={"temp": 25.5, "humidity": 60.0}
        )

        assert point.timestamp == now
        assert len(point.variables) == 2

    def test_naive_timestamp_converted_to_utc(self):
        """Test that naive timestamps are converted to UTC."""
        naive_dt = datetime(2024, 1, 15, 12, 30, 0)
        point = TelemetryPoint(
            timestamp=naive_dt,
            variables={"temp": 25.0}
        )

        # Should be converted to UTC
        assert point.timestamp.tzinfo == timezone.utc
        # The time value should remain the same (just with UTC timezone added)
        assert point.timestamp.year == 2024
        assert point.timestamp.month == 1
        assert point.timestamp.day == 15
        assert point.timestamp.hour == 12
        assert point.timestamp.minute == 30

    def test_timezone_aware_timestamp_converted_to_utc(self):
        """Test that non-UTC timezone-aware timestamps are converted to UTC."""
        from datetime import timedelta

        # Create a timestamp with UTC+2 timezone
        plus_two = timezone(timedelta(hours=2))
        aware_dt = datetime(2024, 1, 15, 14, 30, 0, tzinfo=plus_two)  # 14:30 UTC+2 = 12:30 UTC

        point = TelemetryPoint(
            timestamp=aware_dt,
            variables={"temp": 25.0}
        )

        # Should be converted to UTC
        assert point.timestamp.tzinfo == timezone.utc
        # 14:30 UTC+2 should become 12:30 UTC
        assert point.timestamp.hour == 12
        assert point.timestamp.minute == 30

    def test_fails_without_timestamp(self):
        with pytest.raises(ValidationError):
            TelemetryPoint(variables={"temp": 25.0})

    def test_fails_with_too_many_variables(self):
        """Test that more than MAX_VARIABLES_PER_POINT variables are rejected."""
        from app.models.schemas import MAX_VARIABLES_PER_POINT

        # Create variables dict with too many entries
        too_many_vars = {f"var_{i}": float(i) for i in range(MAX_VARIABLES_PER_POINT + 1)}

        with pytest.raises(ValidationError) as exc_info:
            TelemetryPoint(
                timestamp=datetime.now(timezone.utc),
                variables=too_many_vars
            )

        assert "Too many variables" in str(exc_info.value)


class TestBatchInferenceRequest:
    """Tests for BatchInferenceRequest schema."""

    def test_create_batch_request(self):
        request = BatchInferenceRequest(
            organization_id=1,
            model_id=uuid4(),
            device_ids=[uuid4(), uuid4(), uuid4()],
            time_range_hours=24
        )

        assert len(request.device_ids) == 3
        assert request.time_range_hours == 24

    def test_fails_with_empty_device_ids(self):
        with pytest.raises(ValidationError):
            BatchInferenceRequest(
                organization_id=1,
                model_id=uuid4(),
                device_ids=[],
                time_range_hours=24
            )


class TestTrainingJobCreate:
    """Tests for TrainingJobCreate schema."""

    def test_create_training_job(self):
        job = TrainingJobCreate(
            model_id=uuid4(),
            organization_id=1
        )

        assert job.job_type == "INITIAL_TRAINING"  # default
        assert job.training_config == {}

    def test_create_with_config(self):
        job = TrainingJobCreate(
            model_id=uuid4(),
            organization_id=1,
            job_type="RETRAINING",
            training_config={"epochs": 100, "batch_size": 32},
            training_data_start=datetime(2024, 1, 1),
            training_data_end=datetime(2024, 1, 31)
        )

        assert job.job_type == "RETRAINING"
        assert job.training_config["epochs"] == 100


class TestAnomalyDetectionResult:
    """Tests for AnomalyDetectionResult schema."""

    def test_create_result(self):
        result = AnomalyDetectionResult(
            device_id=uuid4(),
            model_id=uuid4(),
            prediction_type="ANOMALY",
            confidence=0.95,
            prediction_timestamp=datetime.now(),
            anomaly_score=0.85,
            is_anomaly=True,
            severity=AnomalySeverity.HIGH,
            affected_variables=["temperature", "pressure"],
            expected_values={"temperature": 25.0},
            actual_values={"temperature": 85.0}
        )

        assert result.is_anomaly is True
        assert result.severity == AnomalySeverity.HIGH
        assert len(result.affected_variables) == 2

    def test_inherits_from_inference_response(self):
        result = AnomalyDetectionResult(
            device_id=uuid4(),
            model_id=uuid4(),
            prediction_type="ANOMALY",
            prediction_value=0.85,
            prediction_label="HIGH_RISK",
            confidence=0.95,
            prediction_timestamp=datetime.now(),
            anomaly_score=0.85,
            is_anomaly=True,
            severity=AnomalySeverity.HIGH
        )

        # Check inherited fields
        assert result.prediction_value == 0.85
        assert result.prediction_label == "HIGH_RISK"
