"""
Thread safety test for ModelLoader.

Tests concurrent access to the model cache to verify thread safety.
"""
import pytest
import threading
import time
from uuid import uuid4
from unittest.mock import Mock, patch

from app.services.model_loader import ModelLoader, ModelNotFoundError
from app.models.schemas import MLModelType
from app.engines.anomaly_detection import AnomalyDetectionEngine


def test_concurrent_model_loading():
    """Test that multiple threads can safely load models concurrently."""
    loader = ModelLoader(max_cache_size=5)
    model_id = uuid4()
    results = []
    errors = []

    # Mock the _load_model method to simulate slow loading
    original_load = loader._load_model
    def slow_load(*args, **kwargs):
        time.sleep(0.01)  # Simulate I/O delay
        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.model_id = args[0]
        return mock_engine

    with patch.object(loader, '_load_model', side_effect=slow_load):
        def load_model_thread():
            try:
                engine = loader.get_model(
                    model_id=model_id,
                    model_type=MLModelType.ANOMALY_DETECTION,
                )
                results.append(engine)
            except Exception as e:
                errors.append(e)

        # Launch 10 concurrent threads trying to load the same model
        threads = []
        for _ in range(10):
            t = threading.Thread(target=load_model_thread)
            threads.append(t)
            t.start()

        # Wait for all threads to complete
        for t in threads:
            t.join()

    # Verify no errors occurred
    assert len(errors) == 0, f"Errors occurred: {errors}"

    # All threads should get a result
    assert len(results) == 10

    # All results should be the same engine instance (cached)
    assert all(r is results[0] for r in results)

    # Model should only be loaded once (cache hit for subsequent requests)
    assert loader.get_cache_stats()["cached_models"] == 1


def test_concurrent_different_models():
    """Test loading different models concurrently."""
    loader = ModelLoader(max_cache_size=10)
    model_ids = [uuid4() for _ in range(5)]
    results = {}
    errors = []

    def slow_load(model_id, model_type, model_path=None):
        time.sleep(0.01)  # Simulate I/O delay
        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.model_id = model_id
        return mock_engine

    with patch.object(loader, '_load_model', side_effect=slow_load):
        def load_model_thread(model_id):
            try:
                engine = loader.get_model(
                    model_id=model_id,
                    model_type=MLModelType.ANOMALY_DETECTION,
                )
                results[str(model_id)] = engine
            except Exception as e:
                errors.append(e)

        # Launch threads for different models
        threads = []
        for model_id in model_ids:
            t = threading.Thread(target=load_model_thread, args=(model_id,))
            threads.append(t)
            t.start()

        for t in threads:
            t.join()

    assert len(errors) == 0
    assert len(results) == 5
    assert loader.get_cache_stats()["cached_models"] == 5


def test_lru_eviction_thread_safety():
    """Test that LRU eviction works correctly under concurrent access."""
    loader = ModelLoader(max_cache_size=3)
    model_ids = [uuid4() for _ in range(5)]

    def slow_load(model_id, model_type, model_path=None):
        time.sleep(0.01)
        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.model_id = model_id
        return mock_engine

    with patch.object(loader, '_load_model', side_effect=slow_load):
        # Load 5 models sequentially to trigger eviction
        for model_id in model_ids:
            loader.get_model(
                model_id=model_id,
                model_type=MLModelType.ANOMALY_DETECTION,
            )

    # Only last 3 should be cached
    stats = loader.get_cache_stats()
    assert stats["cached_models"] == 3
    assert stats["max_cache_size"] == 3


def test_invalidate_during_concurrent_access():
    """Test that invalidating a model during concurrent access is safe."""
    loader = ModelLoader(max_cache_size=5)
    model_id = uuid4()
    errors = []
    invalidate_done = threading.Event()

    def slow_load(mid, model_type, model_path=None):
        time.sleep(0.01)
        mock_engine = Mock(spec=AnomalyDetectionEngine)
        mock_engine.model_id = mid
        return mock_engine

    with patch.object(loader, '_load_model', side_effect=slow_load):
        # First load the model
        loader.get_model(model_id=model_id, model_type=MLModelType.ANOMALY_DETECTION)

        def access_model():
            try:
                invalidate_done.wait()  # Wait for invalidation
                time.sleep(0.01)
                loader.get_model(
                    model_id=model_id,
                    model_type=MLModelType.ANOMALY_DETECTION,
                )
            except Exception as e:
                errors.append(e)

        def invalidate_model():
            time.sleep(0.02)
            loader.invalidate(model_id)
            invalidate_done.set()

        threads = []
        # Start access threads
        for _ in range(5):
            t = threading.Thread(target=access_model)
            threads.append(t)
            t.start()

        # Start invalidation thread
        inv_thread = threading.Thread(target=invalidate_model)
        inv_thread.start()
        threads.append(inv_thread)

        for t in threads:
            t.join()

    # Should complete without errors
    assert len(errors) == 0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
