"""
Training service for managing ML model training jobs.

Provides in-memory job store with thread-safe operations for creating,
tracking, and managing training jobs across all model types.
"""
import logging
import time
from datetime import datetime, timezone
from threading import Lock, Thread
from typing import Any, Dict, List, Optional
from uuid import UUID, uuid4

import numpy as np
import pandas as pd

from app.core.config import settings
from app.engines.anomaly_detection import AnomalyDetectionEngine
from app.engines.energy_forecasting import EnergyForecastingEngine
from app.engines.equipment_rul import EquipmentRULEngine
from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
from app.models.schemas import MLModelType, TrainingJobStatus

logger = logging.getLogger(__name__)


class TrainingJob:
    """Represents a training job with progress tracking."""

    def __init__(
        self,
        job_id: UUID,
        model_id: UUID,
        organization_id: int,
        model_type: MLModelType,
        job_type: str,
        training_config: Dict[str, Any],
        training_data_start: Optional[datetime] = None,
        training_data_end: Optional[datetime] = None,
    ):
        self.id = job_id
        self.model_id = model_id
        self.organization_id = organization_id
        self.model_type = model_type
        self.job_type = job_type
        self.status = TrainingJobStatus.PENDING
        self.training_config = training_config
        self.training_data_start = training_data_start
        self.training_data_end = training_data_end
        self.record_count: Optional[int] = None
        self.device_count: Optional[int] = None
        self.progress_percent: int = 0
        self.current_step: Optional[str] = None
        self.result_metrics: Dict[str, Any] = {}
        self.error_message: Optional[str] = None
        self.started_at: Optional[datetime] = None
        self.completed_at: Optional[datetime] = None
        self.duration_seconds: Optional[int] = None
        self.triggered_by: Optional[UUID] = None
        self.created_at = datetime.now(timezone.utc)
        self.logs: List[str] = []

    def to_dict(self) -> Dict[str, Any]:
        """Convert job to dictionary for API responses."""
        return {
            "id": self.id,
            "model_id": self.model_id,
            "organization_id": self.organization_id,
            "job_type": self.job_type,
            "status": self.status,
            "training_config": self.training_config,
            "training_data_start": self.training_data_start,
            "training_data_end": self.training_data_end,
            "record_count": self.record_count,
            "device_count": self.device_count,
            "progress_percent": self.progress_percent,
            "current_step": self.current_step,
            "result_metrics": self.result_metrics,
            "error_message": self.error_message,
            "started_at": self.started_at,
            "completed_at": self.completed_at,
            "duration_seconds": self.duration_seconds,
            "triggered_by": self.triggered_by,
            "created_at": self.created_at,
        }

    def add_log(self, message: str) -> None:
        """Add a log entry with timestamp."""
        timestamp = datetime.now(timezone.utc).isoformat()
        self.logs.append(f"[{timestamp}] {message}")


class TrainingService:
    """
    Service for managing ML training jobs.

    Thread-safe in-memory job store with support for all model types.
    For MVP, generates mock training data. In production, would fetch
    from database.
    """

    def __init__(self):
        self._jobs: Dict[UUID, TrainingJob] = {}
        self._lock = Lock()
        logger.info("TrainingService initialized")

    def create_job(
        self,
        model_id: UUID,
        organization_id: int,
        model_type: MLModelType,
        job_type: str = "INITIAL_TRAINING",
        training_config: Optional[Dict[str, Any]] = None,
        training_data_start: Optional[datetime] = None,
        training_data_end: Optional[datetime] = None,
        triggered_by: Optional[UUID] = None,
    ) -> TrainingJob:
        """
        Create a new training job.

        Thread-safe operation that adds job to in-memory store.

        Args:
            model_id: UUID of the model to train.
            organization_id: Organization ID.
            model_type: Type of ML model.
            job_type: Type of training job (default: INITIAL_TRAINING).
            training_config: Optional configuration overrides.
            training_data_start: Optional start time for training data window.
            training_data_end: Optional end time for training data window.
            triggered_by: Optional UUID of user who triggered training.

        Returns:
            Created TrainingJob instance.
        """
        job_id = uuid4()
        config = training_config or {}

        job = TrainingJob(
            job_id=job_id,
            model_id=model_id,
            organization_id=organization_id,
            model_type=model_type,
            job_type=job_type,
            training_config=config,
            training_data_start=training_data_start,
            training_data_end=training_data_end,
        )
        job.triggered_by = triggered_by

        with self._lock:
            self._jobs[job_id] = job

        job.add_log(f"Training job created: {job_type} for model {model_id}")
        logger.info(f"Created training job {job_id} for model {model_id}")

        return job

    def get_job(self, job_id: UUID) -> Optional[TrainingJob]:
        """
        Get job by ID.

        Thread-safe read operation.

        Args:
            job_id: UUID of the job.

        Returns:
            TrainingJob if found, None otherwise.
        """
        with self._lock:
            return self._jobs.get(job_id)

    def list_jobs(
        self,
        organization_id: Optional[int] = None,
        model_id: Optional[UUID] = None,
        status: Optional[TrainingJobStatus] = None,
    ) -> List[TrainingJob]:
        """
        List jobs with optional filters.

        Thread-safe operation with consistent snapshot.

        Args:
            organization_id: Filter by organization ID.
            model_id: Filter by model ID.
            status: Filter by job status.

        Returns:
            List of matching jobs, sorted by created_at descending.
        """
        with self._lock:
            jobs = list(self._jobs.values())

        # Apply filters
        if organization_id is not None:
            jobs = [j for j in jobs if j.organization_id == organization_id]
        if model_id is not None:
            jobs = [j for j in jobs if j.model_id == model_id]
        if status is not None:
            jobs = [j for j in jobs if j.status == status]

        # Sort by created_at descending (most recent first)
        jobs.sort(key=lambda j: j.created_at, reverse=True)

        return jobs

    def update_job_progress(
        self,
        job_id: UUID,
        progress_percent: int,
        current_step: str,
    ) -> None:
        """
        Update job progress.

        Thread-safe operation for background training updates.

        Args:
            job_id: UUID of the job.
            progress_percent: Progress percentage (0-100).
            current_step: Description of current training step.
        """
        with self._lock:
            job = self._jobs.get(job_id)
            if job:
                job.progress_percent = progress_percent
                job.current_step = current_step
                job.add_log(f"Progress: {progress_percent}% - {current_step}")

    def cancel_job(self, job_id: UUID) -> bool:
        """
        Cancel a training job.

        Only PENDING or RUNNING jobs can be cancelled.

        Args:
            job_id: UUID of the job to cancel.

        Returns:
            True if job was cancelled, False if job not found or not cancellable.
        """
        with self._lock:
            job = self._jobs.get(job_id)
            if not job:
                return False

            if job.status not in [TrainingJobStatus.PENDING, TrainingJobStatus.RUNNING]:
                logger.warning(f"Cannot cancel job {job_id} with status {job.status}")
                return False

            job.status = TrainingJobStatus.CANCELLED
            job.completed_at = datetime.now(timezone.utc)
            if job.started_at:
                job.duration_seconds = int((job.completed_at - job.started_at).total_seconds())
            job.add_log("Job cancelled")

        logger.info(f"Cancelled training job {job_id}")
        return True

    def run_training(self, job_id: UUID) -> None:
        """
        Execute training job asynchronously.

        This method runs the complete training pipeline:
        1. Generates mock training data (MVP - no database dependency)
        2. Initializes appropriate ML engine based on model_type
        3. Trains model with progress updates
        4. Saves trained model to disk
        5. Updates job with metrics and COMPLETED status
        6. Handles errors gracefully with FAILED status

        Args:
            job_id: UUID of the job to execute.
        """
        # Get job and mark as running
        with self._lock:
            job = self._jobs.get(job_id)
            if not job:
                logger.error(f"Job {job_id} not found")
                return

            if job.status != TrainingJobStatus.PENDING:
                logger.warning(f"Job {job_id} is not in PENDING state: {job.status}")
                return

            job.status = TrainingJobStatus.RUNNING
            job.started_at = datetime.now(timezone.utc)
            job.add_log("Training started")

        try:
            # Step 1: Generate mock training data (10%)
            self.update_job_progress(job_id, 10, "Generating training data")
            training_data, feature_columns, target_column = self._generate_mock_data(
                job.model_type, job.training_config
            )

            with self._lock:
                job.record_count = len(training_data)
                job.device_count = job.training_config.get("device_count", 1)

            # Step 2: Initialize ML engine (20%)
            self.update_job_progress(job_id, 20, f"Initializing {job.model_type.value} engine")
            engine = self._create_engine(job.model_id, job.model_type)

            # Step 3: Train model (30-80%)
            self.update_job_progress(job_id, 30, "Training model")
            hyperparameters = job.training_config.get("hyperparameters", {})

            metrics = engine.train(
                data=training_data,
                feature_columns=feature_columns,
                target_column=target_column,
                hyperparameters=hyperparameters,
            )

            self.update_job_progress(job_id, 80, "Training complete, saving model")

            # Step 4: Save model to disk (90%)
            self.update_job_progress(job_id, 90, "Saving model to disk")
            model_path = engine.save_model()

            # Step 5: Mark as completed (100%)
            with self._lock:
                job.status = TrainingJobStatus.COMPLETED
                job.progress_percent = 100
                job.current_step = "Training complete"
                job.result_metrics = metrics
                job.completed_at = datetime.now(timezone.utc)
                job.duration_seconds = int((job.completed_at - job.started_at).total_seconds())
                job.add_log(f"Training completed successfully. Model saved to {model_path}")
                job.add_log(f"Metrics: {metrics}")

            logger.info(f"Training job {job_id} completed successfully")

        except Exception as e:
            # Handle errors gracefully
            logger.exception(f"Training job {job_id} failed")

            with self._lock:
                job.status = TrainingJobStatus.FAILED
                job.error_message = str(e)
                job.completed_at = datetime.now(timezone.utc)
                if job.started_at:
                    job.duration_seconds = int((job.completed_at - job.started_at).total_seconds())
                job.add_log(f"Training failed: {str(e)}")

    def _generate_mock_data(
        self,
        model_type: MLModelType,
        config: Dict[str, Any],
    ) -> tuple[pd.DataFrame, List[str], Optional[str]]:
        """
        Generate mock training data for MVP.

        In production, this would fetch real telemetry data from the database.

        Args:
            model_type: Type of model to generate data for.
            config: Training configuration with optional overrides.

        Returns:
            Tuple of (DataFrame, feature_columns, target_column).
        """
        n_samples = config.get("n_samples", 1000)
        np.random.seed(42)

        # Generate time series data
        timestamps = pd.date_range(
            start="2024-01-01",
            periods=n_samples,
            freq="1h",
        )

        # Base feature set (common across all model types)
        data = {
            "timestamp": timestamps,
            "temperature": np.random.normal(25, 5, n_samples),
            "pressure": np.random.normal(101.3, 2, n_samples),
            "vibration": np.random.normal(0.5, 0.2, n_samples),
            "current": np.random.normal(10, 2, n_samples),
        }

        # Model-specific features
        if model_type == MLModelType.ENERGY_FORECAST:
            # Energy forecasting needs energy consumption variable
            data["energy_consumption"] = np.random.normal(100, 20, n_samples)
            feature_columns = ["temperature", "pressure", "current"]
            target_column = "energy_consumption"

        elif model_type == MLModelType.PREDICTIVE_MAINTENANCE:
            # Predictive maintenance needs failure labels
            # Simulate degradation: higher vibration/temperature = more likely to fail
            degradation_score = (
                (data["vibration"] - 0.5) / 0.2 +
                (data["temperature"] - 25) / 5
            ) / 2
            failure_prob = 1 / (1 + np.exp(-degradation_score))
            data["failure"] = (failure_prob > 0.6).astype(int)
            feature_columns = ["temperature", "pressure", "vibration", "current"]
            target_column = "failure"

        elif model_type == MLModelType.EQUIPMENT_RUL:
            # RUL estimation needs remaining useful life in days
            # Simulate linear degradation over time
            rul = np.maximum(0, 365 - np.arange(n_samples) * 0.5 + np.random.normal(0, 10, n_samples))
            data["rul_days"] = rul
            feature_columns = ["temperature", "pressure", "vibration", "current"]
            target_column = "rul_days"

        else:  # ANOMALY_DETECTION
            # Anomaly detection is unsupervised - no target
            # Inject some anomalies
            anomaly_indices = np.random.choice(n_samples, size=int(n_samples * 0.05), replace=False)
            data["temperature"][anomaly_indices] = np.random.uniform(50, 100, len(anomaly_indices))
            feature_columns = ["temperature", "pressure", "vibration", "current"]
            target_column = None

        df = pd.DataFrame(data)
        return df, feature_columns, target_column

    def _create_engine(self, model_id: UUID, model_type: MLModelType):
        """
        Create appropriate ML engine based on model type.

        Args:
            model_id: UUID of the model.
            model_type: Type of ML model.

        Returns:
            Initialized ML engine instance.

        Raises:
            ValueError: If model type is not supported.
        """
        if model_type == MLModelType.ANOMALY_DETECTION:
            return AnomalyDetectionEngine(model_id=model_id, algorithm="isolation_forest")
        elif model_type == MLModelType.PREDICTIVE_MAINTENANCE:
            return PredictiveMaintenanceEngine(model_id=model_id, algorithm="random_forest")
        elif model_type == MLModelType.ENERGY_FORECAST:
            return EnergyForecastingEngine(model_id=model_id, algorithm="arima")
        elif model_type == MLModelType.EQUIPMENT_RUL:
            return EquipmentRULEngine(model_id=model_id, algorithm="gradient_boosting")
        else:
            raise ValueError(f"Unsupported model type: {model_type}")


# Global singleton instance
training_service = TrainingService()
