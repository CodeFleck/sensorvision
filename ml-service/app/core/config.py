"""
Application configuration using Pydantic Settings.
"""
from typing import List
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # Environment
    ENVIRONMENT: str = "development"
    DEBUG: bool = False

    # Database
    DATABASE_HOST: str = "localhost"
    DATABASE_PORT: int = 5432
    DATABASE_NAME: str = "indcloud"
    DATABASE_USER: str = "indcloud"
    DATABASE_PASSWORD: str = "indcloud"

    # Backend API
    BACKEND_URL: str = "http://localhost:8080"
    BACKEND_API_KEY: str = ""

    # ML Configuration
    MODEL_STORAGE_PATH: str = "/app/models"
    MAX_BATCH_SIZE: int = 1000
    INFERENCE_TIMEOUT_SECONDS: int = 300

    # CORS
    CORS_ORIGINS: List[str] = ["http://localhost:3001", "http://localhost:8080"]

    @property
    def database_url(self) -> str:
        """Construct database URL."""
        return f"postgresql://{self.DATABASE_USER}:{self.DATABASE_PASSWORD}@{self.DATABASE_HOST}:{self.DATABASE_PORT}/{self.DATABASE_NAME}"

    @property
    def async_database_url(self) -> str:
        """Construct async database URL."""
        return f"postgresql+asyncpg://{self.DATABASE_USER}:{self.DATABASE_PASSWORD}@{self.DATABASE_HOST}:{self.DATABASE_PORT}/{self.DATABASE_NAME}"

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
