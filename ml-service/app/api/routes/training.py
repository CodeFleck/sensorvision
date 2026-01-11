"""
ML Training endpoints.
"""
import logging
from threading import Thread
from typing import List, Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, BackgroundTasks

from app.core.security import verify_api_key
from app.models.schemas import (
    MLModelType,
    TrainingJobCreate,
    TrainingJobResponse,
    TrainingJobStatus,
)
from app.services.training_service import training_service

router = APIRouter(dependencies=[Depends(verify_api_key)])
logger = logging.getLogger(__name__)


@router.post("/jobs", response_model=TrainingJobResponse, status_code=201)
async def create_training_job(
    job: TrainingJobCreate,
    background_tasks: BackgroundTasks,
):
    """
    Create a new training job.
    Training runs asynchronously in the background.

    Args:
        job: Training job creation request.
        background_tasks: FastAPI background tasks for async execution.

    Returns:
        Created training job with PENDING status.

    Raises:
        HTTPException: 400 if model_type is missing from training_config.
    """
    logger.info(f"Creating training job for model {job.model_id}")

    # Extract model_type from training_config
    model_type_str = job.training_config.get("model_type")
    if not model_type_str:
        raise HTTPException(
            status_code=400,
            detail="model_type is required in training_config"
        )

    try:
        model_type = MLModelType(model_type_str)
    except ValueError:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid model_type: {model_type_str}. "
                   f"Must be one of: {[t.value for t in MLModelType]}"
        )

    # Create job
    training_job = training_service.create_job(
        model_id=job.model_id,
        organization_id=job.organization_id,
        model_type=model_type,
        job_type=job.job_type,
        training_config=job.training_config,
        training_data_start=job.training_data_start,
        training_data_end=job.training_data_end,
    )

    # Start training in background thread
    # Note: Using Thread instead of BackgroundTasks for long-running operations
    # BackgroundTasks are designed for quick cleanup tasks, not long-running work
    # Using daemon=False to allow graceful completion during shutdown
    thread = Thread(
        target=training_service.run_training,
        args=(training_job.id,),
        daemon=False,
        name=f"training-job-{training_job.id}",
    )
    thread.start()

    # Track thread for graceful shutdown
    training_service._active_threads.add(thread)

    logger.info(f"Training job {training_job.id} started in background")

    return TrainingJobResponse(**training_job.to_dict())


@router.get("/jobs", response_model=List[TrainingJobResponse])
async def list_training_jobs(
    organization_id: int = Query(...),
    model_id: Optional[UUID] = Query(None),
    status: Optional[TrainingJobStatus] = Query(None),
):
    """
    List training jobs with optional filters.

    Args:
        organization_id: Filter by organization ID (required).
        model_id: Optional filter by model ID.
        status: Optional filter by job status.

    Returns:
        List of training jobs sorted by created_at descending.
    """
    logger.info(f"Listing training jobs for org {organization_id}")

    jobs = training_service.list_jobs(
        organization_id=organization_id,
        model_id=model_id,
        status=status,
    )

    return [TrainingJobResponse(**job.to_dict()) for job in jobs]


@router.get("/jobs/{job_id}", response_model=TrainingJobResponse)
async def get_training_job(job_id: UUID):
    """
    Get training job details and progress.

    Args:
        job_id: UUID of the training job.

    Returns:
        Training job details with current progress.

    Raises:
        HTTPException: 404 if job not found.
    """
    logger.info(f"Getting training job {job_id}")

    job = training_service.get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    return TrainingJobResponse(**job.to_dict())


@router.post("/jobs/{job_id}/cancel", response_model=TrainingJobResponse)
async def cancel_training_job(job_id: UUID):
    """
    Cancel a running training job.

    Only PENDING or RUNNING jobs can be cancelled.

    Args:
        job_id: UUID of the training job to cancel.

    Returns:
        Updated training job with CANCELLED status.

    Raises:
        HTTPException: 404 if job not found, 400 if job cannot be cancelled.
    """
    logger.info(f"Cancelling training job {job_id}")

    job = training_service.get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    if not training_service.cancel_job(job_id):
        raise HTTPException(
            status_code=400,
            detail=f"Cannot cancel job with status {job.status}. "
                   "Only PENDING or RUNNING jobs can be cancelled."
        )

    # Get updated job
    updated_job = training_service.get_job(job_id)
    return TrainingJobResponse(**updated_job.to_dict())


@router.get("/jobs/{job_id}/logs")
async def get_training_logs(
    job_id: UUID,
    tail: int = Query(100, ge=1, le=1000),
):
    """
    Get training job logs.

    Args:
        job_id: UUID of the training job.
        tail: Maximum number of most recent log entries to return (default 100).

    Returns:
        Dictionary with logs array.

    Raises:
        HTTPException: 404 if job not found.
    """
    logger.info(f"Getting logs for job {job_id}")

    job = training_service.get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    # Return most recent logs (tail)
    logs = job.logs[-tail:] if len(job.logs) > tail else job.logs

    return {"logs": logs}
