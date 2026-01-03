"""
ML Training endpoints.
"""
import logging
from typing import List, Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, BackgroundTasks

from app.core.security import verify_api_key
from app.models.schemas import (
    TrainingJobCreate,
    TrainingJobResponse,
    TrainingJobStatus,
)

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
    """
    logger.info(f"Creating training job for model {job.model_id}")
    # TODO: Implement training job creation
    raise HTTPException(status_code=501, detail="Not implemented yet")


@router.get("/jobs", response_model=List[TrainingJobResponse])
async def list_training_jobs(
    organization_id: int = Query(...),
    model_id: Optional[UUID] = Query(None),
    status: Optional[TrainingJobStatus] = Query(None),
):
    """List training jobs."""
    logger.info(f"Listing training jobs for org {organization_id}")
    # TODO: Implement job listing
    return []


@router.get("/jobs/{job_id}", response_model=TrainingJobResponse)
async def get_training_job(job_id: UUID):
    """Get training job details and progress."""
    logger.info(f"Getting training job {job_id}")
    # TODO: Implement job retrieval
    raise HTTPException(status_code=404, detail="Job not found")


@router.post("/jobs/{job_id}/cancel", response_model=TrainingJobResponse)
async def cancel_training_job(job_id: UUID):
    """Cancel a running training job."""
    logger.info(f"Cancelling training job {job_id}")
    # TODO: Implement job cancellation
    raise HTTPException(status_code=501, detail="Not implemented yet")


@router.get("/jobs/{job_id}/logs")
async def get_training_logs(
    job_id: UUID,
    tail: int = Query(100, ge=1, le=1000),
):
    """Get training job logs."""
    logger.info(f"Getting logs for job {job_id}")
    # TODO: Implement log retrieval
    return {"logs": []}
