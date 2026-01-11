"""
Pydantic schemas for ML service API.
"""
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Dict, List, Optional
from uuid import UUID

from pydantic import BaseModel, Field, field_validator

# Limits for request validation
MAX_VARIABLES_PER_POINT = 100  # Prevent memory exhaustion


# Enums
class MLModelType(str, Enum):
    ANOMALY_DETECTION = "ANOMALY_DETECTION"
    PREDICTIVE_MAINTENANCE = "PREDICTIVE_MAINTENANCE"
    ENERGY_FORECAST = "ENERGY_FORECAST"
    EQUIPMENT_RUL = "EQUIPMENT_RUL"


class MLModelStatus(str, Enum):
    DRAFT = "DRAFT"
    TRAINING = "TRAINING"
    TRAINED = "TRAINED"
    DEPLOYED = "DEPLOYED"
    ARCHIVED = "ARCHIVED"
    FAILED = "FAILED"


class TrainingJobStatus(str, Enum):
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


class AnomalySeverity(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


# Model Schemas
class MLModelBase(BaseModel):
    name: str = Field(..., min_length=1, max_length=255)
    model_type: MLModelType
    algorithm: str = Field(..., min_length=1, max_length=100)
    hyperparameters: Dict[str, Any] = Field(default_factory=dict)
    feature_columns: List[str] = Field(default_factory=list)
    target_column: Optional[str] = None
    device_scope: str = Field(default="ALL")
    device_ids: List[UUID] = Field(default_factory=list)
    device_group_id: Optional[int] = None
    inference_schedule: str = Field(default="0 0 * * * *")
    confidence_threshold: float = Field(default=0.8, ge=0, le=1)
    anomaly_threshold: float = Field(default=0.5, ge=0, le=1)


class MLModelCreate(MLModelBase):
    organization_id: int
    version: str = Field(default="1.0.0")


class MLModelUpdate(BaseModel):
    name: Optional[str] = None
    hyperparameters: Optional[Dict[str, Any]] = None
    feature_columns: Optional[List[str]] = None
    device_scope: Optional[str] = None
    device_ids: Optional[List[UUID]] = None
    inference_schedule: Optional[str] = None
    confidence_threshold: Optional[float] = None
    anomaly_threshold: Optional[float] = None


class MLModelResponse(MLModelBase):
    id: UUID
    organization_id: int
    version: str
    status: MLModelStatus
    model_path: Optional[str] = None
    model_size_bytes: Optional[int] = None
    training_metrics: Dict[str, Any] = Field(default_factory=dict)
    validation_metrics: Dict[str, Any] = Field(default_factory=dict)
    last_inference_at: Optional[datetime] = None
    next_inference_at: Optional[datetime] = None
    created_by: Optional[UUID] = None
    trained_by: Optional[UUID] = None
    trained_at: Optional[datetime] = None
    deployed_at: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


# Inference Schemas
class TelemetryPoint(BaseModel):
    timestamp: datetime
    variables: Dict[str, float]

    @field_validator("timestamp")
    @classmethod
    def ensure_utc_timestamp(cls, v: datetime) -> datetime:
        """Ensure timestamp is timezone-aware and convert to UTC."""
        if v.tzinfo is None:
            # Naive datetime - assume UTC
            return v.replace(tzinfo=timezone.utc)
        # Convert to UTC if different timezone
        return v.astimezone(timezone.utc)

    @field_validator("variables")
    @classmethod
    def validate_variables_count(cls, v: Dict[str, float]) -> Dict[str, float]:
        """Limit number of variables to prevent memory exhaustion."""
        if len(v) > MAX_VARIABLES_PER_POINT:
            raise ValueError(
                f"Too many variables: {len(v)} exceeds maximum of {MAX_VARIABLES_PER_POINT}"
            )
        return v


class InferenceRequest(BaseModel):
    device_id: UUID
    organization_id: int
    model_id: Optional[UUID] = None
    telemetry: List[TelemetryPoint] = Field(
        ...,
        min_length=1,
        max_length=10000,
        description="Telemetry data points (max 10,000 points)"
    )


class BatchInferenceRequest(BaseModel):
    organization_id: int
    model_id: UUID
    device_ids: List[UUID] = Field(
        ...,
        min_length=1,
        max_length=1000,
        description="Device IDs to process (max 1,000 devices)"
    )
    time_range_hours: int = Field(default=24, ge=1, le=168)


class InferenceResponse(BaseModel):
    device_id: UUID
    model_id: UUID
    prediction_type: str
    prediction_value: Optional[float] = None
    prediction_label: Optional[str] = None
    confidence: float
    prediction_details: Dict[str, Any] = Field(default_factory=dict)
    prediction_timestamp: datetime
    prediction_horizon: Optional[str] = None
    valid_until: Optional[datetime] = None


class BatchInferenceResponse(BaseModel):
    model_id: UUID
    total_devices: int
    processed_devices: int
    predictions: List[InferenceResponse]
    errors: List[Dict[str, Any]] = Field(default_factory=list)
    processing_time_ms: int


class AnomalyDetectionResult(InferenceResponse):
    anomaly_score: float
    is_anomaly: bool
    severity: AnomalySeverity
    affected_variables: List[str] = Field(default_factory=list)
    expected_values: Dict[str, float] = Field(default_factory=dict)
    actual_values: Dict[str, float] = Field(default_factory=dict)


class PredictiveMaintenanceResult(InferenceResponse):
    maintenance_probability: float
    days_to_maintenance: Optional[int] = None
    recommended_actions: List[str] = Field(default_factory=list)
    risk_factors: Dict[str, float] = Field(default_factory=dict)


# Training Schemas
class TrainingJobCreate(BaseModel):
    model_id: UUID
    organization_id: int = Field(..., gt=0, description="Organization ID must be positive")
    job_type: str = Field(default="INITIAL_TRAINING")
    training_config: Dict[str, Any] = Field(default_factory=dict)
    training_data_start: Optional[datetime] = None
    training_data_end: Optional[datetime] = None


class TrainingJobResponse(BaseModel):
    id: UUID
    model_id: Optional[UUID] = None
    organization_id: int
    job_type: str
    status: TrainingJobStatus
    training_config: Dict[str, Any]
    training_data_start: Optional[datetime] = None
    training_data_end: Optional[datetime] = None
    record_count: Optional[int] = None
    device_count: Optional[int] = None
    progress_percent: int = 0
    current_step: Optional[str] = None
    result_metrics: Dict[str, Any] = Field(default_factory=dict)
    error_message: Optional[str] = None
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    duration_seconds: Optional[int] = None
    triggered_by: Optional[UUID] = None
    created_at: datetime

    class Config:
        from_attributes = True
