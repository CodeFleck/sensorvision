"""
Model loading and caching service.

Handles loading trained ML models from disk with LRU caching
to optimize inference performance.

Thread Safety:
    The ModelLoader cache is thread-safe for concurrent access. The underlying
    sklearn/xgboost models are thread-safe for inference (predict) operations
    as they don't modify internal state during prediction. However, models
    MUST NOT be retrained while cached - use invalidate() first.

    If you add a new engine type, ensure its predict() method is thread-safe
    (no mutable instance state during inference).

Race Condition Note:
    When loading a model that isn't cached, concurrent requests for the same
    model may each load it from disk before one caches it. This wastes I/O
    but is acceptable for simplicity. The duplicate_loads counter tracks this.
    For high-concurrency scenarios, consider using per-model loading locks.
"""
import logging
import signal
from collections import OrderedDict
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
from contextlib import contextmanager
from pathlib import Path
from threading import Lock
from typing import Any, Dict, Optional
from uuid import UUID

from app.core.config import settings
from app.engines.anomaly_detection import AnomalyDetectionEngine
from app.engines.base import BaseMLEngine
from app.engines.energy_forecasting import EnergyForecastingEngine
from app.engines.equipment_rul import EquipmentRULEngine
from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
from app.models.schemas import MLModelType

logger = logging.getLogger(__name__)

# Configurable cache size - override via ML_MODEL_CACHE_SIZE env var
DEFAULT_CACHE_SIZE = 20  # Conservative default (~4GB assuming 200MB avg model)
DEFAULT_LOAD_TIMEOUT_SECONDS = 30  # Timeout for loading a single model


class ModelNotFoundError(Exception):
    """Raised when a model file cannot be found."""
    pass


class ModelLoadError(Exception):
    """Raised when a model fails to load."""
    pass


class ModelLoadTimeoutError(ModelLoadError):
    """Raised when model loading times out."""
    pass


class InvalidModelPathError(Exception):
    """Raised when a model path is invalid (e.g., path traversal attempt)."""
    pass


class ModelTypeMismatchError(Exception):
    """Raised when loaded model type doesn't match expected type."""
    pass


# Map model types to expected engine classes for validation
MODEL_TYPE_TO_ENGINE = {
    MLModelType.ANOMALY_DETECTION: AnomalyDetectionEngine,
    MLModelType.PREDICTIVE_MAINTENANCE: PredictiveMaintenanceEngine,
    MLModelType.ENERGY_FORECAST: EnergyForecastingEngine,
    MLModelType.EQUIPMENT_RUL: EquipmentRULEngine,
}


class ModelLoader:
    """
    Loads and caches ML models from disk.

    Uses LRU (Least Recently Used) eviction strategy to keep
    memory usage bounded while minimizing disk I/O.

    Thread Safety:
        This class is thread-safe. All cache and statistics operations
        are protected by a lock. The returned engine instances are safe
        for concurrent inference calls.

    Attributes:
        cache_hits: Number of cache hits since initialization.
        cache_misses: Number of cache misses since initialization.
        duplicate_loads: Number of times a model was loaded but already cached
                        by another thread (wasted work due to race condition).
    """

    # Singleton tracking to warn about multiple instances
    _instance_count = 0
    _instance_lock = Lock()

    def __init__(
        self,
        max_cache_size: Optional[int] = None,
        load_timeout_seconds: Optional[int] = None,
    ):
        """
        Initialize the model loader.

        Args:
            max_cache_size: Maximum number of models to keep in cache.
                           Defaults to ML_MODEL_CACHE_SIZE env var or 20.
            load_timeout_seconds: Timeout for loading a single model.
                                 Defaults to 30 seconds.
        """
        # Warn if multiple instances created (should use singleton)
        with ModelLoader._instance_lock:
            ModelLoader._instance_count += 1
            if ModelLoader._instance_count > 1:
                logger.warning(
                    f"Multiple ModelLoader instances created ({ModelLoader._instance_count}). "
                    "Consider using the global 'model_loader' singleton to share cache."
                )

        # Use provided size, or env var, or default
        if max_cache_size is not None:
            self._max_cache_size = max_cache_size
        else:
            self._max_cache_size = getattr(
                settings, 'ML_MODEL_CACHE_SIZE', DEFAULT_CACHE_SIZE
            )

        self._load_timeout = load_timeout_seconds or DEFAULT_LOAD_TIMEOUT_SECONDS
        self._cache: OrderedDict[str, BaseMLEngine] = OrderedDict()
        self._lock = Lock()
        self._storage_path = Path(settings.MODEL_STORAGE_PATH)

        # Metrics - all protected by _lock
        self._cache_hits = 0
        self._cache_misses = 0
        self._duplicate_loads = 0

        logger.info(
            f"ModelLoader initialized: max_cache_size={self._max_cache_size}, "
            f"load_timeout={self._load_timeout}s, storage_path={self._storage_path}"
        )

    @property
    def cache_hits(self) -> int:
        """Thread-safe access to cache hits counter."""
        with self._lock:
            return self._cache_hits

    @property
    def cache_misses(self) -> int:
        """Thread-safe access to cache misses counter."""
        with self._lock:
            return self._cache_misses

    @property
    def duplicate_loads(self) -> int:
        """Thread-safe access to duplicate loads counter."""
        with self._lock:
            return self._duplicate_loads

    def get_model(
        self,
        model_id: UUID,
        model_type: MLModelType,
        model_path: Optional[str] = None,
    ) -> BaseMLEngine:
        """
        Get a model, loading from disk if not cached.

        Thread Safety:
            Safe to call from multiple threads. Returns the same cached
            instance to all callers. The returned engine is safe for
            concurrent inference calls.

        Args:
            model_id: UUID of the model.
            model_type: Type of the ML model (used to create correct engine).
            model_path: Optional custom path to model file.

        Returns:
            Loaded ML engine instance (may be shared across threads).

        Raises:
            ModelNotFoundError: If model file doesn't exist.
            InvalidModelPathError: If path escapes storage directory.
            ModelLoadError: If model fails to load.
            ModelLoadTimeoutError: If loading takes too long.
            ModelTypeMismatchError: If loaded model doesn't match expected type.
        """
        cache_key = str(model_id)

        with self._lock:
            # Check cache first
            if cache_key in self._cache:
                # Move to end (most recently used)
                self._cache.move_to_end(cache_key)
                self._cache_hits += 1
                logger.debug(f"Cache hit for model {model_id}")
                return self._cache[cache_key]

            self._cache_misses += 1

        # Load outside lock to avoid blocking other cache reads
        # Note: Multiple threads may load the same model concurrently.
        # This is intentional for simplicity - we track it with duplicate_loads.
        engine = self._load_model_with_timeout(model_id, model_type, model_path)

        with self._lock:
            # Double-check: another thread might have loaded it while we were loading
            if cache_key in self._cache:
                self._cache.move_to_end(cache_key)
                self._duplicate_loads += 1
                logger.debug(
                    f"Duplicate load detected for model {model_id} "
                    "(another thread cached it first)"
                )
                # Return the cached instance, let our loaded instance be garbage collected
                return self._cache[cache_key]

            # Add to cache with LRU eviction
            self._cache[cache_key] = engine
            while len(self._cache) > self._max_cache_size:
                evicted_id, _ = self._cache.popitem(last=False)
                logger.debug(f"Evicted model {evicted_id} from cache")

            return engine

    def _load_model_with_timeout(
        self,
        model_id: UUID,
        model_type: MLModelType,
        model_path: Optional[str] = None,
    ) -> BaseMLEngine:
        """
        Load a model with timeout protection.

        Args:
            model_id: UUID of the model.
            model_type: Type of the ML model.
            model_path: Optional custom path to model file.

        Returns:
            Loaded ML engine instance.

        Raises:
            ModelLoadTimeoutError: If loading exceeds timeout.
            Other exceptions from _load_model.
        """
        # Use ThreadPoolExecutor for timeout (works cross-platform)
        with ThreadPoolExecutor(max_workers=1) as executor:
            future = executor.submit(self._load_model, model_id, model_type, model_path)
            try:
                return future.result(timeout=self._load_timeout)
            except FuturesTimeoutError:
                logger.error(
                    f"Model loading timed out after {self._load_timeout}s: {model_id}"
                )
                raise ModelLoadTimeoutError(
                    f"Model loading timed out after {self._load_timeout} seconds"
                )

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
            ModelNotFoundError: If model file doesn't exist.
            InvalidModelPathError: If path escapes storage directory.
            ModelLoadError: If model fails to load.
            ModelTypeMismatchError: If model type doesn't match.
        """
        # Determine model path
        if model_path:
            path = Path(model_path)
        else:
            path = self._storage_path / f"{model_id}.joblib"

        # Validate and resolve path
        resolved_path = self._validate_path(path)

        if not resolved_path.exists():
            raise ModelNotFoundError(f"Model not found: {model_id}")

        # Create appropriate engine based on type
        engine = self._create_engine(model_id, model_type)

        try:
            engine.load_model(str(resolved_path))
            logger.info(f"Loaded {model_type.value} model {model_id}")

            # Validate the loaded model type matches expected
            self._validate_model_type(engine, model_type, model_id)

            return engine
        except FileNotFoundError:
            raise ModelNotFoundError(f"Model not found: {model_id}")
        except (OSError, IOError) as e:
            logger.error(f"IO error loading model {model_id}: {type(e).__name__}")
            raise ModelLoadError("Failed to read model file") from e
        except ModelTypeMismatchError:
            raise  # Re-raise type mismatch errors
        except Exception as e:
            # Log type but not message (may contain sensitive path info)
            logger.error(f"Failed to load model {model_id}: {type(e).__name__}")
            raise ModelLoadError("Failed to load model") from e

    def _validate_model_type(
        self,
        engine: BaseMLEngine,
        expected_type: MLModelType,
        model_id: UUID,
    ) -> None:
        """
        Validate that loaded model matches expected type.

        Args:
            engine: The loaded engine instance.
            expected_type: The expected model type.
            model_id: Model ID for error messages.

        Raises:
            ModelTypeMismatchError: If types don't match.
        """
        expected_class = MODEL_TYPE_TO_ENGINE.get(expected_type)
        if expected_class is None:
            raise ValueError(f"Unknown model type: {expected_type}")

        if not isinstance(engine, expected_class):
            actual_type = type(engine).__name__
            expected_name = expected_class.__name__
            logger.error(
                f"Model type mismatch for {model_id}: "
                f"expected {expected_name}, got {actual_type}"
            )
            raise ModelTypeMismatchError(
                f"Model type mismatch: expected {expected_type.value}, "
                f"but loaded model is {actual_type}"
            )

    def _validate_path(self, path: Path) -> Path:
        """
        Validate that a path is within the storage directory.

        Security: Prevents path traversal attacks by ensuring the resolved
        path is within the allowed storage directory.

        Args:
            path: Path to validate.

        Returns:
            Resolved absolute path.

        Raises:
            InvalidModelPathError: If path escapes storage directory.
        """
        try:
            resolved_path = path.resolve()
            storage_resolved = self._storage_path.resolve()
            # This raises ValueError if resolved_path is not relative to storage
            resolved_path.relative_to(storage_resolved)
            return resolved_path
        except ValueError:
            # SECURITY: Don't log the actual path - it may contain attack payload
            logger.warning(
                f"Path traversal attempt detected for model load "
                f"(storage: {storage_resolved})"
            )
            raise InvalidModelPathError("Invalid model path")

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
        engine_class = MODEL_TYPE_TO_ENGINE.get(model_type)
        if engine_class is None:
            raise ValueError(f"Unsupported model type: {model_type}")

        return engine_class(model_id=model_id)

    def invalidate(self, model_id: UUID) -> bool:
        """
        Remove a model from cache.

        Use this before retraining a model to ensure the new version
        is loaded on next access.

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

    def get_cache_stats(self) -> Dict[str, Any]:
        """
        Get cache statistics for monitoring.

        Thread Safety:
            Returns a consistent snapshot of all statistics.

        Returns:
            Dictionary with cache stats including hit rate.
        """
        with self._lock:
            total_requests = self._cache_hits + self._cache_misses
            hit_rate = (
                self._cache_hits / total_requests if total_requests > 0 else 0.0
            )
            return {
                "cached_models": len(self._cache),
                "max_cache_size": self._max_cache_size,
                "cache_hits": self._cache_hits,
                "cache_misses": self._cache_misses,
                "duplicate_loads": self._duplicate_loads,
                "hit_rate": round(hit_rate, 3),
                "load_timeout_seconds": self._load_timeout,
            }


# Global singleton instance
model_loader = ModelLoader()
