"""
Model loading and caching service.

Handles loading trained ML models from disk with LRU caching
to optimize inference performance.
"""
import logging
from collections import OrderedDict
from pathlib import Path
from threading import Lock
from typing import Dict, Optional
from uuid import UUID

from app.core.config import settings
from app.engines.anomaly_detection import AnomalyDetectionEngine
from app.engines.base import BaseMLEngine
from app.engines.energy_forecasting import EnergyForecastingEngine
from app.engines.equipment_rul import EquipmentRULEngine
from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
from app.models.schemas import MLModelType

logger = logging.getLogger(__name__)


class ModelNotFoundError(Exception):
    """Raised when a model file cannot be found."""

    pass


class ModelLoadError(Exception):
    """Raised when a model fails to load."""

    pass


class ModelLoader:
    """
    Loads and caches ML models from disk.

    Uses LRU (Least Recently Used) eviction strategy to keep
    memory usage bounded while minimizing disk I/O.
    """

    def __init__(self, max_cache_size: int = 50):
        """
        Initialize the model loader.

        Args:
            max_cache_size: Maximum number of models to keep in cache.
        """
        self._cache: OrderedDict[str, BaseMLEngine] = OrderedDict()
        self._max_cache_size = max_cache_size
        self._lock = Lock()
        self._storage_path = Path(settings.MODEL_STORAGE_PATH)

    def get_model(
        self,
        model_id: UUID,
        model_type: MLModelType,
        model_path: Optional[str] = None,
    ) -> BaseMLEngine:
        """
        Get a model, loading from disk if not cached.

        Args:
            model_id: UUID of the model.
            model_type: Type of the ML model.
            model_path: Optional custom path to model file.

        Returns:
            Loaded ML engine instance.

        Raises:
            ModelNotFoundError: If model file doesn't exist.
            ModelLoadError: If model fails to load.
        """
        cache_key = str(model_id)

        with self._lock:
            # Check cache first
            if cache_key in self._cache:
                # Move to end (most recently used)
                self._cache.move_to_end(cache_key)
                logger.debug(f"Model {model_id} loaded from cache")
                return self._cache[cache_key]

            # Load from disk
            engine = self._load_model(model_id, model_type, model_path)

            # Add to cache with LRU eviction
            self._cache[cache_key] = engine
            if len(self._cache) > self._max_cache_size:
                # Remove least recently used (first item)
                evicted_id, _ = self._cache.popitem(last=False)
                logger.debug(f"Evicted model {evicted_id} from cache")

            return engine

    def _load_model(
        self,
        model_id: UUID,
        model_type: MLModelType,
        model_path: Optional[str] = None,
    ) -> BaseMLEngine:
        """
        Load a model from disk.

        Args:
            model_id: UUID of the model.
            model_type: Type of the ML model.
            model_path: Optional custom path to model file.

        Returns:
            Loaded ML engine instance.

        Raises:
            ModelNotFoundError: If model file doesn't exist or path is invalid.
            ModelLoadError: If model fails to load.
        """
        # Determine model path
        if model_path:
            path = Path(model_path)
        else:
            path = self._storage_path / f"{model_id}.joblib"

        # SECURITY: Validate path doesn't escape storage directory (prevent path traversal)
        try:
            resolved_path = path.resolve()
            storage_resolved = self._storage_path.resolve()
            resolved_path.relative_to(storage_resolved)
        except ValueError:
            logger.warning(f"Path traversal attempt detected: {path}")
            raise ModelNotFoundError(f"Invalid model path")

        if not resolved_path.exists():
            raise ModelNotFoundError(f"Model not found: {model_id}")

        # Create appropriate engine based on type
        engine = self._create_engine(model_id, model_type)

        try:
            engine.load_model(str(resolved_path))
            logger.info(f"Loaded {model_type.value} model {model_id}")
            return engine
        except Exception as e:
            logger.error(f"Failed to load model {model_id}")
            raise ModelLoadError(f"Failed to load model") from e

    def _create_engine(self, model_id: UUID, model_type: MLModelType) -> BaseMLEngine:
        """
        Create an engine instance based on model type.

        Args:
            model_id: UUID of the model.
            model_type: Type of the ML model.

        Returns:
            Uninitialized ML engine instance.

        Raises:
            ValueError: If model type is not supported.
        """
        if model_type == MLModelType.ANOMALY_DETECTION:
            return AnomalyDetectionEngine(model_id=model_id)
        elif model_type == MLModelType.PREDICTIVE_MAINTENANCE:
            return PredictiveMaintenanceEngine(model_id=model_id)
        elif model_type == MLModelType.ENERGY_FORECAST:
            return EnergyForecastingEngine(model_id=model_id)
        elif model_type == MLModelType.EQUIPMENT_RUL:
            return EquipmentRULEngine(model_id=model_id)
        else:
            raise ValueError(f"Unsupported model type: {model_type}")

    def invalidate(self, model_id: UUID) -> bool:
        """
        Remove a model from cache.

        Useful when a model is retrained or deleted.

        Args:
            model_id: UUID of the model to invalidate.

        Returns:
            True if model was in cache and removed, False otherwise.
        """
        cache_key = str(model_id)
        with self._lock:
            if cache_key in self._cache:
                del self._cache[cache_key]
                logger.info(f"Invalidated model {model_id} from cache")
                return True
            return False

    def clear_cache(self) -> int:
        """
        Clear all cached models.

        Returns:
            Number of models cleared.
        """
        with self._lock:
            count = len(self._cache)
            self._cache.clear()
            logger.info(f"Cleared {count} models from cache")
            return count

    def get_cache_stats(self) -> Dict[str, int]:
        """
        Get cache statistics.

        Returns:
            Dictionary with cache stats.
        """
        with self._lock:
            return {
                "cached_models": len(self._cache),
                "max_cache_size": self._max_cache_size,
            }


# Global singleton instance
model_loader = ModelLoader()
