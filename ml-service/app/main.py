"""
Industrial Cloud ML Service
FastAPI-based ML microservice for anomaly detection, predictive maintenance,
energy forecasting, and equipment RUL estimation.
"""
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.core.logging import setup_logging
from app.api.routes import health, models, inference, training

# Setup logging
setup_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler for startup/shutdown."""
    logger.info("Starting ML Service...")
    logger.info(f"Environment: {settings.ENVIRONMENT}")
    logger.info(f"Database: {settings.DATABASE_HOST}:{settings.DATABASE_PORT}")
    yield
    logger.info("Shutting down ML Service...")


app = FastAPI(
    title="Industrial Cloud ML Service",
    description="Machine Learning service for IoT anomaly detection, predictive maintenance, and forecasting",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(health.router, prefix="/health", tags=["Health"])
app.include_router(models.router, prefix="/api/v1/models", tags=["Models"])
app.include_router(inference.router, prefix="/api/v1/inference", tags=["Inference"])
app.include_router(training.router, prefix="/api/v1/training", tags=["Training"])


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": "Industrial Cloud ML Service",
        "version": "1.0.0",
        "status": "running"
    }
