"""
Health check endpoints.
"""
from fastapi import APIRouter

router = APIRouter()


@router.get("")
async def health_check():
    """Basic health check."""
    return {"status": "healthy"}


@router.get("/ready")
async def readiness_check():
    """Readiness check - verifies service is ready to accept traffic."""
    # TODO: Add database connectivity check
    return {"status": "ready"}


@router.get("/live")
async def liveness_check():
    """Liveness check - verifies service is running."""
    return {"status": "alive"}
