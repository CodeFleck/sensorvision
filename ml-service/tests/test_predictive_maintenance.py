"""
Tests for Predictive Maintenance Engine.
"""
import pytest
import numpy as np
import pandas as pd
from uuid import uuid4

from app.engines.predictive_maintenance import PredictiveMaintenanceEngine


class TestPredictiveMaintenanceEngine:
    """Tests for PredictiveMaintenanceEngine class."""

    @pytest.fixture
    def engine(self):
        """Create a predictive maintenance engine."""
        return PredictiveMaintenanceEngine(model_id=uuid4())

    @pytest.fixture
    def training_data(self):
        """
        Generate synthetic training data with failure patterns.

        Simulates equipment degradation:
        - Normal operation: stable values
        - Pre-failure: increasing trend, higher variance
        """
        np.random.seed(42)
        n_normal = 800
        n_failure = 200

        # Normal operation samples
        normal_temp = np.random.normal(70, 5, n_normal)
        normal_vibration = np.random.normal(0.5, 0.1, n_normal)
        normal_pressure = np.random.normal(100, 3, n_normal)

        # Pre-failure samples (degraded patterns)
        # Increasing temperature, higher vibration, unstable pressure
        failure_temp = np.random.normal(85, 8, n_failure) + np.linspace(0, 10, n_failure)
        failure_vibration = np.random.normal(1.2, 0.3, n_failure)
        failure_pressure = np.random.normal(95, 8, n_failure)

        # Combine datasets
        temperature = np.concatenate([normal_temp, failure_temp])
        vibration = np.concatenate([normal_vibration, failure_vibration])
        pressure = np.concatenate([normal_pressure, failure_pressure])
        labels = np.concatenate([np.zeros(n_normal), np.ones(n_failure)])

        # Shuffle to mix normal and failure cases
        indices = np.random.permutation(len(labels))

        return pd.DataFrame(
            {
                "temperature": temperature[indices],
                "vibration": vibration[indices],
                "pressure": pressure[indices],
                "failure": labels[indices],
            }
        )

    @pytest.fixture
    def test_data_normal(self):
        """Generate test data representing normal operation."""
        np.random.seed(100)
        n_samples = 50

        return pd.DataFrame(
            {
                "temperature": np.random.normal(70, 5, n_samples),
                "vibration": np.random.normal(0.5, 0.1, n_samples),
                "pressure": np.random.normal(100, 3, n_samples),
            }
        )

    @pytest.fixture
    def test_data_degraded(self):
        """Generate test data representing degraded equipment."""
        np.random.seed(200)
        n_samples = 50

        # Add degradation trend
        return pd.DataFrame(
            {
                "temperature": np.random.normal(85, 8, n_samples)
                + np.linspace(0, 10, n_samples),
                "vibration": np.random.normal(1.2, 0.3, n_samples),
                "pressure": np.random.normal(95, 8, n_samples),
            }
        )

    class TestInitialization:
        """Tests for engine initialization."""

        def test_creates_engine(self, engine):
            assert engine.is_loaded is False
            assert engine.model is None

        def test_default_hyperparameters(self, engine):
            params = engine.get_default_hyperparameters()

            assert params["n_estimators"] == 100
            assert params["max_depth"] == 10
            assert params["min_samples_split"] == 5
            assert params["min_samples_leaf"] == 2
            assert params["max_features"] == "sqrt"
            assert params["window_sizes"] == [5, 10, 20]
            assert params["threshold"] == 0.5
            assert params["random_state"] == 42

        def test_hyperparameters_are_sensible(self, engine):
            """Verify hyperparameters follow ML best practices."""
            params = engine.get_default_hyperparameters()

            # Ensemble size
            assert 50 <= params["n_estimators"] <= 200, "n_estimators should be reasonable"

            # Tree depth (prevent overfitting)
            assert params["max_depth"] is None or params["max_depth"] >= 5

            # Sample split requirements
            assert params["min_samples_split"] >= 2
            assert params["min_samples_leaf"] >= 1

            # Window sizes for time-series
            assert len(params["window_sizes"]) >= 2
            assert all(w > 0 for w in params["window_sizes"])

            # Threshold
            assert 0 <= params["threshold"] <= 1

    class TestFeatureEngineering:
        """Tests for time-series feature engineering."""

        def test_engineer_features_creates_rolling_stats(self, engine, training_data):
            feature_columns = ["temperature", "vibration"]
            window_sizes = [5, 10]

            X_engineered = engine._engineer_features(
                training_data, feature_columns, window_sizes
            )

            # Check rolling mean exists for each window
            assert "temperature_mean_5" in X_engineered.columns
            assert "temperature_mean_10" in X_engineered.columns
            assert "vibration_mean_5" in X_engineered.columns

            # Check rolling std
            assert "temperature_std_5" in X_engineered.columns
            assert "vibration_std_10" in X_engineered.columns

            # Check rolling max/min
            assert "temperature_max_5" in X_engineered.columns
            assert "temperature_min_5" in X_engineered.columns

            # Check range
            assert "temperature_range_5" in X_engineered.columns

        def test_engineer_features_creates_trend_indicators(self, engine, training_data):
            feature_columns = ["temperature"]
            window_sizes = [5, 10]

            X_engineered = engine._engineer_features(
                training_data, feature_columns, window_sizes
            )

            # Check differences (rate of change)
            assert "temperature_diff_1" in X_engineered.columns
            assert "temperature_diff_5" in X_engineered.columns

            # Check slopes (trend)
            assert "temperature_slope_5" in X_engineered.columns
            assert "temperature_slope_10" in X_engineered.columns

        def test_engineer_features_creates_statistical_moments(self, engine, training_data):
            feature_columns = ["temperature"]
            window_sizes = [10, 20]

            X_engineered = engine._engineer_features(
                training_data, feature_columns, window_sizes
            )

            # Check skewness and kurtosis
            assert "temperature_skew_10" in X_engineered.columns
            assert "temperature_kurt_10" in X_engineered.columns
            assert "temperature_skew_20" in X_engineered.columns

        def test_engineer_features_handles_missing_columns(self, engine, training_data):
            # Request feature that doesn't exist
            feature_columns = ["temperature", "nonexistent"]
            window_sizes = [5]

            X_engineered = engine._engineer_features(
                training_data, feature_columns, window_sizes
            )

            # Should only create features for existing columns
            assert "temperature_mean_5" in X_engineered.columns
            assert "nonexistent_mean_5" not in X_engineered.columns

        def test_engineer_features_returns_no_nan(self, engine, training_data):
            feature_columns = ["temperature", "vibration"]
            window_sizes = [5, 10, 20]

            X_engineered = engine._engineer_features(
                training_data, feature_columns, window_sizes
            )

            # No NaN values should remain
            assert not X_engineered.isnull().any().any()

        def test_engineer_features_returns_no_inf(self, engine, training_data):
            feature_columns = ["temperature", "vibration"]
            window_sizes = [5, 10]

            X_engineered = engine._engineer_features(
                training_data, feature_columns, window_sizes
            )

            # No infinite values
            assert not np.isinf(X_engineered.values).any()

    class TestTraining:
        """Tests for model training."""

        def test_train_requires_target_column(self, engine, training_data):
            with pytest.raises(ValueError, match="target_column is required"):
                engine.train(training_data, ["temperature", "vibration"])

        def test_train_succeeds(self, engine, training_data):
            feature_columns = ["temperature", "vibration", "pressure"]

            metrics = engine.train(
                training_data, feature_columns, target_column="failure"
            )

            assert engine.is_loaded is True
            assert engine.model is not None

            # Check metrics structure
            assert "algorithm" in metrics
            assert metrics["algorithm"] == "random_forest"
            assert "training_samples" in metrics
            assert "features" in metrics
            assert "accuracy" in metrics
            assert "precision" in metrics
            assert "recall" in metrics
            assert "f1_score" in metrics

        def test_train_computes_classification_metrics(self, engine, training_data):
            feature_columns = ["temperature", "vibration"]

            metrics = engine.train(
                training_data, feature_columns, target_column="failure"
            )

            # Metrics should be in valid range
            assert 0 <= metrics["accuracy"] <= 1
            assert 0 <= metrics["precision"] <= 1
            assert 0 <= metrics["recall"] <= 1
            assert 0 <= metrics["f1_score"] <= 1

            # Should have reasonable accuracy on synthetic data
            assert metrics["accuracy"] > 0.6, "Model should learn synthetic patterns"

        def test_train_computes_confusion_matrix_components(self, engine, training_data):
            feature_columns = ["temperature", "vibration"]

            metrics = engine.train(
                training_data, feature_columns, target_column="failure"
            )

            assert "true_positives" in metrics
            assert "true_negatives" in metrics
            assert "false_positives" in metrics
            assert "false_negatives" in metrics

            # All should be non-negative integers
            assert metrics["true_positives"] >= 0
            assert metrics["true_negatives"] >= 0
            assert metrics["false_positives"] >= 0
            assert metrics["false_negatives"] >= 0

        def test_train_computes_feature_importance(self, engine, training_data):
            feature_columns = ["temperature", "vibration"]

            metrics = engine.train(
                training_data, feature_columns, target_column="failure"
            )

            assert engine.feature_importance_ is not None
            assert len(engine.feature_importance_) > 0

            # Check top features are included in metrics
            assert "feature_importance_top_10" in metrics
            top_features = metrics["feature_importance_top_10"]
            assert len(top_features) > 0

            # Importance values should be between 0 and 1
            for importance in top_features.values():
                assert 0 <= importance <= 1

        def test_train_stores_engineered_feature_names(self, engine, training_data):
            feature_columns = ["temperature", "vibration"]

            engine.train(training_data, feature_columns, target_column="failure")

            assert len(engine.engineered_feature_names_) > 0
            # Should have more features than raw inputs
            assert len(engine.engineered_feature_names_) > len(feature_columns)

        def test_train_with_custom_hyperparameters(self, engine, training_data):
            feature_columns = ["temperature"]
            custom_params = {
                "n_estimators": 50,
                "max_depth": 5,
                "window_sizes": [5, 10],
            }

            metrics = engine.train(
                training_data,
                feature_columns,
                target_column="failure",
                hyperparameters=custom_params,
            )

            assert engine.is_loaded is True

        def test_train_raises_on_missing_target(self, engine, training_data):
            with pytest.raises(ValueError, match="Missing features"):
                engine.train(
                    training_data, ["temperature"], target_column="nonexistent"
                )

        def test_train_raises_on_missing_features(self, engine, training_data):
            with pytest.raises(ValueError, match="Missing features"):
                engine.train(
                    training_data,
                    ["temperature", "nonexistent"],
                    target_column="failure",
                )

    class TestPrediction:
        """Tests for failure prediction."""

        def test_predict_returns_labels(
            self, engine, training_data, test_data_normal
        ):
            feature_columns = ["temperature", "vibration", "pressure"]
            engine.train(training_data, feature_columns, target_column="failure")

            labels = engine.predict(test_data_normal, feature_columns)

            assert len(labels) == len(test_data_normal)
            assert set(labels).issubset({0, 1})  # Binary labels

        def test_predict_detects_normal_operation(
            self, engine, training_data, test_data_normal
        ):
            feature_columns = ["temperature", "vibration", "pressure"]
            engine.train(training_data, feature_columns, target_column="failure")

            labels = engine.predict(test_data_normal, feature_columns)

            # Most should be classified as normal (0)
            normal_ratio = np.sum(labels == 0) / len(labels)
            assert normal_ratio > 0.5, "Should mostly predict normal operation"

        def test_predict_detects_degradation(
            self, engine, training_data, test_data_degraded
        ):
            feature_columns = ["temperature", "vibration", "pressure"]
            engine.train(training_data, feature_columns, target_column="failure")

            labels = engine.predict(test_data_degraded, feature_columns)

            # Should detect some failures
            failure_ratio = np.sum(labels == 1) / len(labels)
            assert failure_ratio > 0.1, "Should detect degradation patterns"

        def test_predict_raises_when_not_trained(self, engine, test_data_normal):
            with pytest.raises(ValueError, match="not trained"):
                engine.predict(test_data_normal, ["temperature"])

        def test_predict_raises_on_missing_features(
            self, engine, training_data, test_data_normal
        ):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            with pytest.raises(ValueError, match="Missing features"):
                engine.predict(test_data_normal, ["temperature", "nonexistent"])

    class TestPredictWithProbability:
        """Tests for prediction with probability scores."""

        def test_predict_with_probability_returns_all_outputs(
            self, engine, training_data, test_data_normal
        ):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            labels, probabilities, details = engine.predict_with_probability(
                test_data_normal, feature_columns
            )

            assert len(labels) == len(test_data_normal)
            assert len(probabilities) == len(test_data_normal)
            assert len(details) == len(test_data_normal)

        def test_probabilities_are_normalized(
            self, engine, training_data, test_data_normal
        ):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            _, probabilities, _ = engine.predict_with_probability(
                test_data_normal, feature_columns
            )

            assert all(0 <= p <= 1 for p in probabilities)

        def test_details_contain_required_fields(
            self, engine, training_data, test_data_normal
        ):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            _, _, details = engine.predict_with_probability(
                test_data_normal, feature_columns
            )

            for detail in details:
                assert "failure_probability" in detail
                assert "days_to_failure" in detail
                assert "risk_level" in detail
                assert "top_risk_factors" in detail

        def test_days_to_failure_estimation(
            self, engine, training_data, test_data_degraded
        ):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            _, _, details = engine.predict_with_probability(
                test_data_degraded, feature_columns
            )

            # Check that high-risk predictions have days_to_failure
            high_risk_details = [
                d for d in details if d["failure_probability"] >= 0.5
            ]

            if len(high_risk_details) > 0:
                for detail in high_risk_details:
                    assert detail["days_to_failure"] is not None
                    assert 1 <= detail["days_to_failure"] <= 2

        def test_risk_level_classification(
            self, engine, training_data, test_data_normal
        ):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            _, _, details = engine.predict_with_probability(
                test_data_normal, feature_columns
            )

            valid_risk_levels = {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
            for detail in details:
                assert detail["risk_level"] in valid_risk_levels

        def test_risk_factors_are_provided(
            self, engine, training_data, test_data_degraded
        ):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            _, _, details = engine.predict_with_probability(
                test_data_degraded, feature_columns
            )

            # At least some predictions should have risk factors
            has_risk_factors = any(
                len(d["top_risk_factors"]) > 0 for d in details
            )
            assert has_risk_factors, "Should identify risk factors for some predictions"

        def test_high_probability_yields_critical_risk(
            self, engine, training_data, test_data_degraded
        ):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            _, probabilities, details = engine.predict_with_probability(
                test_data_degraded, feature_columns
            )

            # Find high probability predictions
            for i, prob in enumerate(probabilities):
                if prob >= 0.75:
                    assert details[i]["risk_level"] == "CRITICAL"

    class TestModelPersistence:
        """Tests for model save/load functionality."""

        def test_save_model(self, engine, training_data, tmp_path):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            model_path = str(tmp_path / "test_pm_model.joblib")
            saved_path = engine.save_model(model_path)

            assert saved_path == model_path
            import os

            assert os.path.exists(model_path)

        def test_load_model(self, engine, training_data, tmp_path):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            model_path = str(tmp_path / "test_pm_model.joblib")
            engine.save_model(model_path)

            # Create new engine and load
            new_engine = PredictiveMaintenanceEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            assert new_engine.is_loaded is True
            assert new_engine.model is not None
            assert len(new_engine.engineered_feature_names_) > 0
            assert new_engine.feature_importance_ is not None

        def test_save_preserves_feature_names(self, engine, training_data, tmp_path):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            original_features = engine.engineered_feature_names_.copy()

            model_path = str(tmp_path / "test_pm_model.joblib")
            engine.save_model(model_path)

            new_engine = PredictiveMaintenanceEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            assert new_engine.engineered_feature_names_ == original_features

        def test_save_preserves_threshold(self, engine, training_data, tmp_path):
            feature_columns = ["temperature"]
            custom_params = {"threshold": 0.7}

            engine.train(
                training_data,
                feature_columns,
                target_column="failure",
                hyperparameters=custom_params,
            )

            model_path = str(tmp_path / "test_pm_model.joblib")
            engine.save_model(model_path)

            new_engine = PredictiveMaintenanceEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            assert new_engine.failure_probability_threshold == 0.7

        def test_loaded_model_can_predict(
            self, engine, training_data, test_data_normal, tmp_path
        ):
            feature_columns = ["temperature", "vibration"]
            engine.train(training_data, feature_columns, target_column="failure")

            # Save original predictions
            original_labels = engine.predict(test_data_normal, feature_columns)

            # Save and reload
            model_path = str(tmp_path / "test_pm_model.joblib")
            engine.save_model(model_path)

            new_engine = PredictiveMaintenanceEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            # Predictions should be identical
            loaded_labels = new_engine.predict(test_data_normal, feature_columns)
            assert np.array_equal(original_labels, loaded_labels)

        def test_save_raises_when_not_trained(self, engine):
            with pytest.raises(ValueError, match="No model to save"):
                engine.save_model("/tmp/test.joblib")

        def test_load_raises_when_file_not_found(self, engine):
            with pytest.raises(FileNotFoundError):
                engine.load_model("/nonexistent/path.joblib")

    class TestEdgeCases:
        """Tests for edge cases and error handling."""

        def test_handles_single_feature(self, engine, training_data):
            feature_columns = ["temperature"]

            metrics = engine.train(
                training_data, feature_columns, target_column="failure"
            )

            assert engine.is_loaded is True
            assert metrics["features"] > 1  # Should engineer multiple features

        def test_handles_imbalanced_data(self, engine):
            """Test with heavily imbalanced dataset (common in maintenance)."""
            np.random.seed(42)
            n_normal = 950
            n_failure = 50

            data = pd.DataFrame(
                {
                    "temperature": np.concatenate(
                        [
                            np.random.normal(70, 5, n_normal),
                            np.random.normal(90, 10, n_failure),
                        ]
                    ),
                    "failure": np.concatenate([np.zeros(n_normal), np.ones(n_failure)]),
                }
            )

            metrics = engine.train(data, ["temperature"], target_column="failure")

            # Should still train successfully
            assert engine.is_loaded is True
            assert metrics["failure_rate"] < 0.1

        def test_handles_all_normal_data(self, engine):
            """Test with dataset containing no failures."""
            np.random.seed(42)
            n_samples = 500

            data = pd.DataFrame(
                {
                    "temperature": np.random.normal(70, 5, n_samples),
                    "failure": np.zeros(n_samples),  # All normal
                }
            )

            metrics = engine.train(data, ["temperature"], target_column="failure")

            assert engine.is_loaded is True
            assert metrics["failure_rate"] == 0.0

        def test_handles_small_dataset(self, engine):
            """Test with minimal dataset."""
            np.random.seed(42)
            n_samples = 50

            data = pd.DataFrame(
                {
                    "temperature": np.random.normal(70, 5, n_samples),
                    "failure": (np.random.random(n_samples) > 0.8).astype(int),
                }
            )

            metrics = engine.train(data, ["temperature"], target_column="failure")

            assert engine.is_loaded is True
