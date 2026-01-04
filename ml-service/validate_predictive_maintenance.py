#!/usr/bin/env python
"""
Validation script for Predictive Maintenance Engine.

Tests:
1. Import validation
2. Initialization
3. Default hyperparameters
4. Basic training and prediction
"""
import sys
import numpy as np
import pandas as pd
from uuid import uuid4


def validate_imports():
    """Test that all imports work."""
    print("Testing imports...")
    try:
        from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
        from app.engines import PredictiveMaintenanceEngine as PMEngine
        print("✓ Imports successful")
        return True
    except ImportError as e:
        print(f"✗ Import failed: {e}")
        return False


def validate_initialization():
    """Test engine initialization."""
    print("\nTesting initialization...")
    try:
        from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
        engine = PredictiveMaintenanceEngine(model_id=uuid4())

        assert engine.model is None
        assert engine.is_loaded is False
        assert engine.scaler is not None

        print("✓ Initialization successful")
        return True
    except Exception as e:
        print(f"✗ Initialization failed: {e}")
        return False


def validate_hyperparameters():
    """Test default hyperparameters."""
    print("\nTesting hyperparameters...")
    try:
        from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
        engine = PredictiveMaintenanceEngine(model_id=uuid4())
        params = engine.get_default_hyperparameters()

        required_params = [
            "n_estimators", "max_depth", "min_samples_split",
            "min_samples_leaf", "max_features", "window_sizes",
            "threshold", "random_state"
        ]

        for param in required_params:
            assert param in params, f"Missing parameter: {param}"

        assert params["n_estimators"] == 100
        assert params["threshold"] == 0.5
        assert params["window_sizes"] == [5, 10, 20]

        print("✓ Hyperparameters valid")
        return True
    except Exception as e:
        print(f"✗ Hyperparameter validation failed: {e}")
        return False


def validate_feature_engineering():
    """Test feature engineering."""
    print("\nTesting feature engineering...")
    try:
        from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
        engine = PredictiveMaintenanceEngine(model_id=uuid4())

        # Create simple test data
        data = pd.DataFrame({
            "temp": np.random.normal(70, 5, 100),
            "vib": np.random.normal(0.5, 0.1, 100),
        })

        X_eng = engine._engineer_features(
            data, ["temp", "vib"], window_sizes=[5, 10]
        )

        assert len(X_eng) == len(data)
        assert len(X_eng.columns) > len(data.columns)
        assert not X_eng.isnull().any().any()
        assert not np.isinf(X_eng.values).any()

        # Check expected features exist
        assert "temp_mean_5" in X_eng.columns
        assert "temp_std_10" in X_eng.columns
        assert "vib_diff_1" in X_eng.columns
        assert "vib_slope_5" in X_eng.columns

        print(f"✓ Feature engineering successful ({len(X_eng.columns)} features)")
        return True
    except Exception as e:
        print(f"✗ Feature engineering failed: {e}")
        return False


def validate_training():
    """Test model training."""
    print("\nTesting training...")
    try:
        from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
        engine = PredictiveMaintenanceEngine(model_id=uuid4())

        # Create synthetic data
        np.random.seed(42)
        n_samples = 500
        data = pd.DataFrame({
            "temp": np.concatenate([
                np.random.normal(70, 5, 400),
                np.random.normal(90, 10, 100)
            ]),
            "vib": np.concatenate([
                np.random.normal(0.5, 0.1, 400),
                np.random.normal(1.2, 0.3, 100)
            ]),
            "failure": np.concatenate([
                np.zeros(400),
                np.ones(100)
            ])
        })

        metrics = engine.train(
            data=data,
            feature_columns=["temp", "vib"],
            target_column="failure",
            hyperparameters={"n_estimators": 50, "window_sizes": [5, 10]}
        )

        assert engine.is_loaded is True
        assert engine.model is not None

        # Check metrics
        required_metrics = [
            "accuracy", "precision", "recall", "f1_score",
            "training_samples", "features", "feature_importance_top_10"
        ]
        for metric in required_metrics:
            assert metric in metrics, f"Missing metric: {metric}"

        assert 0 <= metrics["accuracy"] <= 1
        assert 0 <= metrics["recall"] <= 1
        assert metrics["training_samples"] == n_samples

        print(f"✓ Training successful (accuracy={metrics['accuracy']:.3f})")
        return True
    except Exception as e:
        print(f"✗ Training failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def validate_prediction():
    """Test prediction."""
    print("\nTesting prediction...")
    try:
        from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
        engine = PredictiveMaintenanceEngine(model_id=uuid4())

        # Train first
        np.random.seed(42)
        train_data = pd.DataFrame({
            "temp": np.concatenate([
                np.random.normal(70, 5, 400),
                np.random.normal(90, 10, 100)
            ]),
            "failure": np.concatenate([np.zeros(400), np.ones(100)])
        })

        engine.train(
            train_data,
            ["temp"],
            target_column="failure",
            hyperparameters={"window_sizes": [5]}
        )

        # Predict
        test_data = pd.DataFrame({
            "temp": np.random.normal(70, 5, 50)
        })

        labels = engine.predict(test_data, ["temp"])

        assert len(labels) == len(test_data)
        assert set(labels).issubset({0, 1})

        print(f"✓ Basic prediction successful")
        return True
    except Exception as e:
        print(f"✗ Prediction failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def validate_prediction_with_probability():
    """Test prediction with probability."""
    print("\nTesting prediction with probability...")
    try:
        from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
        engine = PredictiveMaintenanceEngine(model_id=uuid4())

        # Train
        np.random.seed(42)
        train_data = pd.DataFrame({
            "temp": np.concatenate([
                np.random.normal(70, 5, 400),
                np.random.normal(90, 10, 100)
            ]),
            "failure": np.concatenate([np.zeros(400), np.ones(100)])
        })

        engine.train(
            train_data,
            ["temp"],
            target_column="failure",
            hyperparameters={"window_sizes": [5]}
        )

        # Predict with probability
        test_data = pd.DataFrame({
            "temp": np.random.normal(70, 5, 20)
        })

        labels, probabilities, details = engine.predict_with_probability(
            test_data, ["temp"]
        )

        assert len(labels) == len(test_data)
        assert len(probabilities) == len(test_data)
        assert len(details) == len(test_data)
        assert all(0 <= p <= 1 for p in probabilities)

        # Check details structure
        for detail in details:
            assert "failure_probability" in detail
            assert "days_to_failure" in detail
            assert "risk_level" in detail
            assert "top_risk_factors" in detail
            assert detail["risk_level"] in ["LOW", "MEDIUM", "HIGH", "CRITICAL"]

        print(f"✓ Probability prediction successful")
        return True
    except Exception as e:
        print(f"✗ Probability prediction failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def validate_persistence():
    """Test model save/load."""
    print("\nTesting model persistence...")
    try:
        from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
        import tempfile
        import os

        engine = PredictiveMaintenanceEngine(model_id=uuid4())

        # Train
        np.random.seed(42)
        train_data = pd.DataFrame({
            "temp": np.concatenate([
                np.random.normal(70, 5, 400),
                np.random.normal(90, 10, 100)
            ]),
            "failure": np.concatenate([np.zeros(400), np.ones(100)])
        })

        engine.train(
            train_data,
            ["temp"],
            target_column="failure",
            hyperparameters={"window_sizes": [5], "threshold": 0.6}
        )

        # Save
        with tempfile.TemporaryDirectory() as tmpdir:
            model_path = os.path.join(tmpdir, "test_model.joblib")
            engine.save_model(model_path)

            assert os.path.exists(model_path)

            # Load in new engine
            new_engine = PredictiveMaintenanceEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            assert new_engine.is_loaded is True
            assert new_engine.model is not None
            assert new_engine.failure_probability_threshold == 0.6

            # Test predictions match
            test_data = pd.DataFrame({"temp": [70, 75, 85, 95]})
            labels1 = engine.predict(test_data, ["temp"])
            labels2 = new_engine.predict(test_data, ["temp"])

            assert np.array_equal(labels1, labels2)

        print(f"✓ Model persistence successful")
        return True
    except Exception as e:
        print(f"✗ Model persistence failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def main():
    """Run all validations."""
    print("=" * 70)
    print("Predictive Maintenance Engine - Validation")
    print("=" * 70)

    tests = [
        validate_imports,
        validate_initialization,
        validate_hyperparameters,
        validate_feature_engineering,
        validate_training,
        validate_prediction,
        validate_prediction_with_probability,
        validate_persistence,
    ]

    results = []
    for test in tests:
        results.append(test())

    print("\n" + "=" * 70)
    passed = sum(results)
    total = len(results)

    if passed == total:
        print(f"✓ ALL TESTS PASSED ({passed}/{total})")
        print("=" * 70)
        return 0
    else:
        print(f"✗ SOME TESTS FAILED ({passed}/{total} passed)")
        print("=" * 70)
        return 1


if __name__ == "__main__":
    sys.exit(main())
