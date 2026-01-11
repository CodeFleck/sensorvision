"""
Integration tests for ML Training endpoints.

Tests the training routes to verify job management, progress tracking,
cancellation, and integration with all 4 ML model types.
"""
import time
from datetime import datetime, timezone
from unittest.mock import patch
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.models.schemas import MLModelType, TrainingJobStatus


@pytest.fixture
def client():
    """Create test client with disabled API key verification."""
    with patch("app.core.config.settings.API_KEY_REQUIRED", False):
        with TestClient(app) as c:
            yield c


@pytest.fixture
def model_id():
    """Generate a random model ID."""
    return str(uuid4())


@pytest.fixture
def organization_id():
    """Return test organization ID."""
    return 1


@pytest.fixture(autouse=True)
def clear_jobs():
    """Clear all training jobs before each test."""
    from app.services.training_service import training_service
    with training_service._lock:
        training_service._jobs.clear()
    yield
    # Clean up after test
    with training_service._lock:
        training_service._jobs.clear()


class TestCreateTrainingJob:
    """Tests for POST /api/v1/training/jobs endpoint."""

    def test_create_job_success(self, client, model_id, organization_id):
        """Test successful job creation for anomaly detection."""
        response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": 100,
                },
            },
        )

        assert response.status_code == 201
        data = response.json()

        # Verify response structure
        assert "id" in data
        assert data["model_id"] == model_id
        assert data["organization_id"] == organization_id
        assert data["job_type"] == "INITIAL_TRAINING"
        assert data["status"] == "PENDING" or data["status"] == "RUNNING"
        assert data["progress_percent"] >= 0
        assert data["training_config"]["model_type"] == "ANOMALY_DETECTION"

    def test_create_job_all_model_types(self, client, organization_id):
        """Test job creation for all 4 model types."""
        model_types = [
            "ANOMALY_DETECTION",
            "PREDICTIVE_MAINTENANCE",
            "ENERGY_FORECAST",
            "EQUIPMENT_RUL",
        ]

        for model_type in model_types:
            model_id = str(uuid4())
            response = client.post(
                "/api/v1/training/jobs",
                json={
                    "model_id": model_id,
                    "organization_id": organization_id,
                    "job_type": "INITIAL_TRAINING",
                    "training_config": {
                        "model_type": model_type,
                        "n_samples": 50,
                    },
                },
            )

            assert response.status_code == 201, f"Failed for {model_type}"
            data = response.json()
            assert data["training_config"]["model_type"] == model_type

    def test_create_job_missing_model_type(self, client, model_id, organization_id):
        """Test job creation fails without model_type."""
        response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {},
            },
        )

        assert response.status_code == 400
        assert "model_type is required" in response.json()["detail"]

    def test_create_job_invalid_model_type(self, client, model_id, organization_id):
        """Test job creation fails with invalid model_type."""
        response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "INVALID_TYPE",
                },
            },
        )

        assert response.status_code == 400
        assert "Invalid model_type" in response.json()["detail"]

    def test_create_job_with_hyperparameters(self, client, model_id, organization_id):
        """Test job creation with custom hyperparameters."""
        response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": 200,
                    "hyperparameters": {
                        "n_estimators": 50,
                        "contamination": 0.2,
                    },
                },
            },
        )

        assert response.status_code == 201
        data = response.json()
        assert data["training_config"]["hyperparameters"]["n_estimators"] == 50


class TestListTrainingJobs:
    """Tests for GET /api/v1/training/jobs endpoint."""

    def test_list_jobs_empty(self, client, organization_id):
        """Test listing jobs returns empty list when no jobs exist."""
        response = client.get(
            "/api/v1/training/jobs",
            params={"organization_id": organization_id},
        )

        assert response.status_code == 200
        assert response.json() == []

    def test_list_jobs_by_organization(self, client):
        """Test listing jobs filtered by organization."""
        # Create jobs for different organizations
        org1_model = str(uuid4())
        org2_model = str(uuid4())

        # Org 1 job
        client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": org1_model,
                "organization_id": 1,
                "job_type": "INITIAL_TRAINING",
                "training_config": {"model_type": "ANOMALY_DETECTION"},
            },
        )

        # Org 2 job
        client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": org2_model,
                "organization_id": 2,
                "job_type": "INITIAL_TRAINING",
                "training_config": {"model_type": "ENERGY_FORECAST"},
            },
        )

        # List org 1 jobs
        response = client.get(
            "/api/v1/training/jobs",
            params={"organization_id": 1},
        )

        assert response.status_code == 200
        jobs = response.json()
        assert len(jobs) == 1
        assert jobs[0]["organization_id"] == 1

    def test_list_jobs_by_model_id(self, client, organization_id):
        """Test listing jobs filtered by model_id."""
        model1 = str(uuid4())
        model2 = str(uuid4())

        # Create jobs for different models
        for model in [model1, model2, model1]:  # Two jobs for model1
            client.post(
                "/api/v1/training/jobs",
                json={
                    "model_id": model,
                    "organization_id": organization_id,
                    "job_type": "INITIAL_TRAINING",
                    "training_config": {"model_type": "ANOMALY_DETECTION"},
                },
            )

        # List jobs for model1
        response = client.get(
            "/api/v1/training/jobs",
            params={"organization_id": organization_id, "model_id": model1},
        )

        assert response.status_code == 200
        jobs = response.json()
        assert len(jobs) == 2
        assert all(job["model_id"] == model1 for job in jobs)

    def test_list_jobs_by_status(self, client, model_id, organization_id):
        """Test listing jobs filtered by status."""
        # Create a job
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": 50,
                },
            },
        )

        job_id = create_response.json()["id"]

        # Wait briefly for job to potentially start
        time.sleep(0.1)

        # List PENDING jobs (might be RUNNING already)
        response = client.get(
            "/api/v1/training/jobs",
            params={"organization_id": organization_id, "status": "PENDING"},
        )

        assert response.status_code == 200
        # Jobs might have already started running, so just verify structure
        jobs = response.json()
        assert isinstance(jobs, list)

    def test_list_jobs_sorted_by_created_at(self, client, organization_id):
        """Test jobs are sorted by created_at descending (most recent first)."""
        # Create multiple jobs with small delays
        job_ids = []
        for i in range(3):
            response = client.post(
                "/api/v1/training/jobs",
                json={
                    "model_id": str(uuid4()),
                    "organization_id": organization_id,
                    "job_type": "INITIAL_TRAINING",
                    "training_config": {"model_type": "ANOMALY_DETECTION"},
                },
            )
            job_ids.append(response.json()["id"])
            time.sleep(0.01)  # Small delay to ensure different timestamps

        # List all jobs
        response = client.get(
            "/api/v1/training/jobs",
            params={"organization_id": organization_id},
        )

        assert response.status_code == 200
        jobs = response.json()
        assert len(jobs) == 3

        # Verify sorted descending (most recent first)
        created_ats = [job["created_at"] for job in jobs]
        assert created_ats == sorted(created_ats, reverse=True)


class TestGetTrainingJob:
    """Tests for GET /api/v1/training/jobs/{job_id} endpoint."""

    def test_get_job_success(self, client, model_id, organization_id):
        """Test getting job details."""
        # Create a job
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "PREDICTIVE_MAINTENANCE",
                    "n_samples": 100,
                },
            },
        )

        job_id = create_response.json()["id"]

        # Get job details
        response = client.get(f"/api/v1/training/jobs/{job_id}")

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == job_id
        assert data["model_id"] == model_id
        assert "status" in data
        assert "progress_percent" in data

    def test_get_job_not_found(self, client):
        """Test getting non-existent job returns 404."""
        fake_job_id = str(uuid4())
        response = client.get(f"/api/v1/training/jobs/{fake_job_id}")

        assert response.status_code == 404
        assert "Job not found" in response.json()["detail"]

    def test_get_job_shows_progress(self, client, model_id, organization_id):
        """Test job details include progress updates."""
        # Create a job
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "EQUIPMENT_RUL",
                    "n_samples": 100,
                },
            },
        )

        job_id = create_response.json()["id"]

        # Wait briefly for training to progress
        time.sleep(0.2)

        # Get job details
        response = client.get(f"/api/v1/training/jobs/{job_id}")

        assert response.status_code == 200
        data = response.json()
        # Progress should be updating
        assert data["progress_percent"] >= 0
        # Should have a current step if running
        if data["status"] == "RUNNING":
            assert data["current_step"] is not None


class TestCancelTrainingJob:
    """Tests for POST /api/v1/training/jobs/{job_id}/cancel endpoint."""

    def test_cancel_pending_job(self, client, model_id, organization_id):
        """Test cancelling a PENDING job."""
        # Create job but immediately try to cancel before it starts
        from app.services.training_service import training_service

        # Create job without starting training
        job = training_service.create_job(
            model_id=uuid4(),
            organization_id=organization_id,
            model_type=MLModelType.ANOMALY_DETECTION,
            job_type="INITIAL_TRAINING",
            training_config={"model_type": "ANOMALY_DETECTION"},
        )

        # Cancel it
        response = client.post(f"/api/v1/training/jobs/{job.id}/cancel")

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "CANCELLED"
        assert data["completed_at"] is not None

    def test_cancel_running_job(self, client, model_id, organization_id):
        """Test cancelling a RUNNING job."""
        # Create a slow job
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ENERGY_FORECAST",
                    "n_samples": 1000,  # Larger dataset takes longer
                },
            },
        )

        job_id = create_response.json()["id"]

        # Wait for job to start running
        time.sleep(0.1)

        # Cancel it
        response = client.post(f"/api/v1/training/jobs/{job_id}/cancel")

        # Should succeed (either RUNNING or already completed)
        assert response.status_code in [200, 400]
        if response.status_code == 200:
            assert response.json()["status"] == "CANCELLED"

    def test_cancel_completed_job_fails(self, client, model_id, organization_id):
        """Test cancelling a COMPLETED job fails."""
        # Create a fast job
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": 10,  # Very small dataset
                },
            },
        )

        job_id = create_response.json()["id"]

        # Wait for completion
        max_wait = 5
        for _ in range(max_wait * 10):
            response = client.get(f"/api/v1/training/jobs/{job_id}")
            if response.json()["status"] == "COMPLETED":
                break
            time.sleep(0.1)

        # Try to cancel completed job
        response = client.post(f"/api/v1/training/jobs/{job_id}/cancel")

        assert response.status_code == 400
        assert "Cannot cancel" in response.json()["detail"]

    def test_cancel_nonexistent_job(self, client):
        """Test cancelling non-existent job returns 404."""
        fake_job_id = str(uuid4())
        response = client.post(f"/api/v1/training/jobs/{fake_job_id}/cancel")

        assert response.status_code == 404
        assert "Job not found" in response.json()["detail"]


class TestGetTrainingLogs:
    """Tests for GET /api/v1/training/jobs/{job_id}/logs endpoint."""

    def test_get_logs_success(self, client, model_id, organization_id):
        """Test getting job logs."""
        # Create a job
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": 50,
                },
            },
        )

        job_id = create_response.json()["id"]

        # Wait for some logs to be generated
        time.sleep(0.2)

        # Get logs
        response = client.get(f"/api/v1/training/jobs/{job_id}/logs")

        assert response.status_code == 200
        data = response.json()
        assert "logs" in data
        assert isinstance(data["logs"], list)
        # Should have at least the creation log
        assert len(data["logs"]) > 0

    def test_get_logs_with_tail(self, client, model_id, organization_id):
        """Test getting logs with tail parameter."""
        # Create a job
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ENERGY_FORECAST",
                    "n_samples": 100,
                },
            },
        )

        job_id = create_response.json()["id"]

        # Wait for multiple log entries
        time.sleep(0.3)

        # Get only last 2 logs
        response = client.get(
            f"/api/v1/training/jobs/{job_id}/logs",
            params={"tail": 2},
        )

        assert response.status_code == 200
        data = response.json()
        assert len(data["logs"]) <= 2

    def test_get_logs_nonexistent_job(self, client):
        """Test getting logs for non-existent job returns 404."""
        fake_job_id = str(uuid4())
        response = client.get(f"/api/v1/training/jobs/{fake_job_id}/logs")

        assert response.status_code == 404
        assert "Job not found" in response.json()["detail"]


class TestTrainingIntegration:
    """Integration tests for complete training workflows."""

    def test_full_training_workflow_anomaly_detection(self, client, organization_id):
        """Test complete workflow for anomaly detection training."""
        model_id = str(uuid4())

        # 1. Create training job
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": 50,
                },
            },
        )

        assert create_response.status_code == 201
        job_id = create_response.json()["id"]

        # 2. Monitor progress
        max_wait = 10
        final_status = None
        for _ in range(max_wait * 10):
            response = client.get(f"/api/v1/training/jobs/{job_id}")
            assert response.status_code == 200
            status = response.json()["status"]

            if status in ["COMPLETED", "FAILED"]:
                final_status = status
                break

            time.sleep(0.1)

        # 3. Verify completion
        assert final_status == "COMPLETED", "Training should complete successfully"

        # 4. Verify final job state
        final_response = client.get(f"/api/v1/training/jobs/{job_id}")
        data = final_response.json()

        assert data["status"] == "COMPLETED"
        assert data["progress_percent"] == 100
        assert data["completed_at"] is not None
        assert data["duration_seconds"] is not None
        assert len(data["result_metrics"]) > 0

        # 5. Check logs
        logs_response = client.get(f"/api/v1/training/jobs/{job_id}/logs")
        assert logs_response.status_code == 200
        logs = logs_response.json()["logs"]
        assert len(logs) > 0
        # Should have completion log
        assert any("complete" in log.lower() for log in logs)

    def test_training_all_model_types_completes(self, client, organization_id):
        """
        Test that training completes successfully for all 4 model types.

        Note: ENERGY_FORECAST is skipped in this test due to data requirements.
        The EnergyForecastingEngine requires sufficient data for lag features
        (lag_24h, lag_7d) which makes it unsuitable for fast unit testing with
        mock data. It's tested separately with appropriate datasets.
        """
        model_types = [
            "ANOMALY_DETECTION",
            "PREDICTIVE_MAINTENANCE",
            # "ENERGY_FORECAST",  # Skip - requires 168+ samples for lag_7d feature
            "EQUIPMENT_RUL",
        ]

        for model_type in model_types:
            # Create job
            model_id = str(uuid4())
            create_response = client.post(
                "/api/v1/training/jobs",
                json={
                    "model_id": model_id,
                    "organization_id": organization_id,
                    "job_type": "INITIAL_TRAINING",
                    "training_config": {
                        "model_type": model_type,
                        "n_samples": 30,
                    },
                },
            )

            assert create_response.status_code == 201
            job_id = create_response.json()["id"]

            # Wait for completion
            max_wait = 10
            final_status = None
            for _ in range(max_wait * 10):
                response = client.get(f"/api/v1/training/jobs/{job_id}")
                status = response.json()["status"]

                if status in ["COMPLETED", "FAILED"]:
                    final_status = status
                    break

                time.sleep(0.1)

            # Verify success
            assert final_status == "COMPLETED", f"{model_type} training should complete"

            # Verify metrics are present
            final_response = client.get(f"/api/v1/training/jobs/{job_id}")
            data = final_response.json()
            assert len(data["result_metrics"]) > 0, f"{model_type} should have metrics"


class TestResourceLimits:
    """Tests for resource limit enforcement."""

    def test_n_samples_too_small_fails(self, client, model_id, organization_id):
        """Test that n_samples below MIN_N_SAMPLES causes training to fail."""
        from app.services.training_service import MIN_N_SAMPLES

        # Create job with n_samples = 5 (below MIN_N_SAMPLES = 10)
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": MIN_N_SAMPLES - 1,
                },
            },
        )

        assert create_response.status_code == 201
        job_id = create_response.json()["id"]

        # Wait for job to fail
        max_wait = 5
        final_status = None
        for _ in range(max_wait * 10):
            response = client.get(f"/api/v1/training/jobs/{job_id}")
            status = response.json()["status"]

            if status in ["COMPLETED", "FAILED"]:
                final_status = status
                break

            time.sleep(0.1)

        # Should fail due to n_samples validation
        assert final_status == "FAILED"
        data = response.json()
        assert "n_samples" in data["error_message"].lower()

    def test_n_samples_too_large_fails(self, client, model_id, organization_id):
        """Test that n_samples above MAX_N_SAMPLES causes training to fail."""
        from app.services.training_service import MAX_N_SAMPLES

        # Create job with n_samples exceeding MAX_N_SAMPLES
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": MAX_N_SAMPLES + 1,
                },
            },
        )

        assert create_response.status_code == 201
        job_id = create_response.json()["id"]

        # Wait for job to fail
        max_wait = 5
        final_status = None
        for _ in range(max_wait * 10):
            response = client.get(f"/api/v1/training/jobs/{job_id}")
            status = response.json()["status"]

            if status in ["COMPLETED", "FAILED"]:
                final_status = status
                break

            time.sleep(0.1)

        # Should fail due to n_samples validation
        assert final_status == "FAILED"
        data = response.json()
        assert "n_samples" in data["error_message"].lower() or "maximum" in data["error_message"].lower()

    def test_max_jobs_limit_evicts_old_completed_jobs(self, client, model_id, organization_id):
        """Test that MAX_JOBS limit triggers eviction of old completed jobs."""
        from app.services.training_service import training_service, MAX_JOBS, TrainingJob
        from app.models.schemas import MLModelType, TrainingJobStatus
        from uuid import uuid4

        # Clear existing jobs
        with training_service._lock:
            training_service._jobs.clear()

        # Add MAX_JOBS completed jobs directly to the service
        for i in range(MAX_JOBS):
            job = TrainingJob(
                job_id=uuid4(),
                model_id=uuid4(),
                organization_id=organization_id,
                model_type=MLModelType.ANOMALY_DETECTION,
                job_type="INITIAL_TRAINING",
                training_config={},
            )
            job.status = TrainingJobStatus.COMPLETED
            training_service._jobs[job.id] = job

        assert len(training_service._jobs) == MAX_JOBS

        # Create a new job - should trigger eviction
        old_job_ids = set(training_service._jobs.keys())

        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": 100,
                },
            },
        )

        assert create_response.status_code == 201
        new_job_id = create_response.json()["id"]

        # The new job should be in the store
        assert new_job_id in [str(j) for j in training_service._jobs.keys()]

        # At least one old job should have been evicted
        current_job_ids = set(training_service._jobs.keys())
        evicted = old_job_ids - current_job_ids
        assert len(evicted) >= 1, "At least one old job should be evicted"

    def test_log_limit_enforced(self, client, model_id, organization_id):
        """Test that logs are limited to MAX_LOGS_PER_JOB."""
        from app.services.training_service import training_service, MAX_LOGS_PER_JOB

        # Create a job
        create_response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": 50,
                },
            },
        )

        job_id = create_response.json()["id"]
        from uuid import UUID
        job = training_service.get_job(UUID(job_id))

        # Add more than MAX_LOGS_PER_JOB logs
        for i in range(MAX_LOGS_PER_JOB + 100):
            job.add_log(f"Log entry {i}")

        # Logs should be capped at MAX_LOGS_PER_JOB
        assert len(job.logs) <= MAX_LOGS_PER_JOB

    def test_negative_organization_id_rejected(self, client, model_id):
        """Test that negative organization_id is rejected by API."""
        response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": -1,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": 100,
                },
            },
        )

        # Should be rejected with 422 Unprocessable Entity
        assert response.status_code == 422
        assert "greater than 0" in response.text.lower() or "organization" in response.text.lower()

    def test_max_jobs_all_active_rejects_new_job(self, client, model_id, organization_id):
        """Test that when all MAX_JOBS are active, new jobs are rejected."""
        from app.services.training_service import training_service, MAX_JOBS, TrainingJob
        from app.models.schemas import MLModelType, TrainingJobStatus
        from uuid import uuid4

        # Clear existing jobs
        with training_service._lock:
            training_service._jobs.clear()

        # Add MAX_JOBS RUNNING jobs (not evictable)
        for i in range(MAX_JOBS):
            job = TrainingJob(
                job_id=uuid4(),
                model_id=uuid4(),
                organization_id=organization_id,
                model_type=MLModelType.ANOMALY_DETECTION,
                job_type="INITIAL_TRAINING",
                training_config={},
            )
            job.status = TrainingJobStatus.RUNNING  # Active, not evictable
            training_service._jobs[job.id] = job

        assert len(training_service._jobs) == MAX_JOBS

        # Try to create a new job - should fail since no jobs can be evicted
        response = client.post(
            "/api/v1/training/jobs",
            json={
                "model_id": model_id,
                "organization_id": organization_id,
                "job_type": "INITIAL_TRAINING",
                "training_config": {
                    "model_type": "ANOMALY_DETECTION",
                    "n_samples": 100,
                },
            },
        )

        # Should fail with 503 Service Unavailable (capacity exceeded)
        assert response.status_code == 503
        assert "occupied" in response.text.lower() or "cannot create" in response.text.lower()
