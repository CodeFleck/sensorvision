"""
Tests for Anomaly Detection Engine.
"""
import pytest
import numpy as np
import pandas as pd
from uuid import uuid4
from decimal import Decimal

from app.engines.anomaly_detection import AnomalyDetectionEngine
from app.models.schemas import AnomalySeverity


class TestAnomalyDetectionEngine:
    """Tests for AnomalyDetectionEngine class."""

    @pytest.fixture
    def isolation_forest_engine(self):
        """Create an Isolation Forest engine."""
        return AnomalyDetectionEngine(
            model_id=uuid4(),
            algorithm="isolation_forest"
        )

    @pytest.fixture
    def z_score_engine(self):
        """Create a Z-Score engine."""
        return AnomalyDetectionEngine(
            model_id=uuid4(),
            algorithm="z_score"
        )

    @pytest.fixture
    def training_data(self):
        """Generate synthetic training data."""
        np.random.seed(42)
        n_samples = 1000

        # Normal data
        temperature = np.random.normal(25, 2, n_samples)
        pressure = np.random.normal(101.3, 1, n_samples)
        vibration = np.random.normal(0.5, 0.1, n_samples)

        return pd.DataFrame({
            "temperature": temperature,
            "pressure": pressure,
            "vibration": vibration
        })

    @pytest.fixture
    def test_data_with_anomalies(self, training_data):
        """Generate test data with known anomalies."""
        test_data = training_data.copy()

        # Add some anomalies
        test_data.loc[0, "temperature"] = 85.0  # Extreme temperature
        test_data.loc[1, "pressure"] = 150.0  # Extreme pressure
        test_data.loc[2, "vibration"] = 2.0  # Extreme vibration

        return test_data.head(10)

    class TestInitialization:
        """Tests for engine initialization."""

        def test_creates_isolation_forest_engine(self, isolation_forest_engine):
            assert isolation_forest_engine.algorithm == "isolation_forest"
            assert isolation_forest_engine.is_loaded is False

        def test_creates_z_score_engine(self, z_score_engine):
            assert z_score_engine.algorithm == "z_score"
            assert z_score_engine.is_loaded is False

        def test_default_hyperparameters_isolation_forest(self, isolation_forest_engine):
            params = isolation_forest_engine.get_default_hyperparameters()
            assert params["n_estimators"] == 100
            assert params["contamination"] == 0.1
            assert params["random_state"] == 42

        def test_default_hyperparameters_z_score(self, z_score_engine):
            params = z_score_engine.get_default_hyperparameters()
            assert params["threshold"] == 3.0

    class TestTraining:
        """Tests for model training."""

        def test_train_isolation_forest(self, isolation_forest_engine, training_data):
            feature_columns = ["temperature", "pressure", "vibration"]

            metrics = isolation_forest_engine.train(
                training_data,
                feature_columns
            )

            assert isolation_forest_engine.is_loaded is True
            assert metrics["algorithm"] == "isolation_forest"
            assert metrics["training_samples"] == len(training_data)
            assert metrics["features"] == len(feature_columns)
            assert "anomaly_ratio" in metrics

        def test_train_z_score(self, z_score_engine, training_data):
            feature_columns = ["temperature", "pressure"]

            metrics = z_score_engine.train(
                training_data,
                feature_columns
            )

            assert z_score_engine.is_loaded is True
            assert metrics["algorithm"] == "z_score"
            assert metrics["threshold"] == 3.0

        def test_train_stores_feature_statistics(self, isolation_forest_engine, training_data):
            feature_columns = ["temperature", "pressure"]

            isolation_forest_engine.train(training_data, feature_columns)

            assert "temperature" in isolation_forest_engine.feature_stats
            assert "pressure" in isolation_forest_engine.feature_stats

            temp_stats = isolation_forest_engine.feature_stats["temperature"]
            assert "mean" in temp_stats
            assert "std" in temp_stats
            assert "min" in temp_stats
            assert "max" in temp_stats

        def test_train_with_custom_hyperparameters(self, isolation_forest_engine, training_data):
            feature_columns = ["temperature"]
            custom_params = {"n_estimators": 50, "contamination": 0.05}

            metrics = isolation_forest_engine.train(
                training_data,
                feature_columns,
                hyperparameters=custom_params
            )

            assert isolation_forest_engine.is_loaded is True

        def test_train_raises_on_missing_features(self, isolation_forest_engine, training_data):
            with pytest.raises(ValueError, match="Missing features"):
                isolation_forest_engine.train(
                    training_data,
                    ["temperature", "nonexistent_column"]
                )

    class TestPrediction:
        """Tests for anomaly prediction."""

        def test_predict_returns_labels(self, isolation_forest_engine, training_data, test_data_with_anomalies):
            feature_columns = ["temperature", "pressure", "vibration"]
            isolation_forest_engine.train(training_data, feature_columns)

            labels = isolation_forest_engine.predict(test_data_with_anomalies, feature_columns)

            assert len(labels) == len(test_data_with_anomalies)
            assert set(labels).issubset({-1, 1})  # -1 = anomaly, 1 = normal

        def test_predict_detects_anomalies(self, isolation_forest_engine, training_data, test_data_with_anomalies):
            feature_columns = ["temperature", "pressure", "vibration"]
            isolation_forest_engine.train(training_data, feature_columns)

            labels = isolation_forest_engine.predict(test_data_with_anomalies, feature_columns)

            # First 3 samples have anomalies
            assert labels[0] == -1 or labels[1] == -1 or labels[2] == -1

        def test_predict_raises_when_not_trained(self, isolation_forest_engine, training_data):
            with pytest.raises(ValueError, match="not trained"):
                isolation_forest_engine.predict(training_data, ["temperature"])

        def test_z_score_predict(self, z_score_engine, training_data, test_data_with_anomalies):
            feature_columns = ["temperature", "pressure"]
            z_score_engine.train(training_data, feature_columns)

            labels = z_score_engine.predict(test_data_with_anomalies, feature_columns)

            assert len(labels) == len(test_data_with_anomalies)

    class TestPredictWithScores:
        """Tests for prediction with detailed scores."""

        def test_predict_with_scores_returns_all_outputs(self, isolation_forest_engine, training_data, test_data_with_anomalies):
            feature_columns = ["temperature", "pressure", "vibration"]
            isolation_forest_engine.train(training_data, feature_columns)

            labels, scores, details = isolation_forest_engine.predict_with_scores(
                test_data_with_anomalies, feature_columns
            )

            assert len(labels) == len(test_data_with_anomalies)
            assert len(scores) == len(test_data_with_anomalies)
            assert len(details) == len(test_data_with_anomalies)

        def test_scores_are_normalized(self, isolation_forest_engine, training_data, test_data_with_anomalies):
            feature_columns = ["temperature", "pressure"]
            isolation_forest_engine.train(training_data, feature_columns)

            _, scores, _ = isolation_forest_engine.predict_with_scores(
                test_data_with_anomalies, feature_columns
            )

            assert all(0 <= s <= 1 for s in scores)

        def test_details_contain_affected_variables(self, isolation_forest_engine, training_data, test_data_with_anomalies):
            feature_columns = ["temperature", "pressure", "vibration"]
            isolation_forest_engine.train(training_data, feature_columns)

            _, _, details = isolation_forest_engine.predict_with_scores(
                test_data_with_anomalies, feature_columns
            )

            for detail in details:
                assert "affected_variables" in detail
                assert "expected_values" in detail
                assert "actual_values" in detail

    class TestSeverity:
        """Tests for severity determination."""

        def test_get_severity_low(self, isolation_forest_engine):
            severity = isolation_forest_engine.get_severity(0.4, threshold=0.5)
            assert severity == AnomalySeverity.LOW

        def test_get_severity_medium(self, isolation_forest_engine):
            severity = isolation_forest_engine.get_severity(0.55, threshold=0.5)
            assert severity == AnomalySeverity.MEDIUM

        def test_get_severity_high(self, isolation_forest_engine):
            severity = isolation_forest_engine.get_severity(0.75, threshold=0.5)
            assert severity == AnomalySeverity.HIGH

        def test_get_severity_critical(self, isolation_forest_engine):
            severity = isolation_forest_engine.get_severity(0.9, threshold=0.5)
            assert severity == AnomalySeverity.CRITICAL

    class TestModelPersistence:
        """Tests for model save/load functionality."""

        def test_save_model(self, isolation_forest_engine, training_data, tmp_path):
            feature_columns = ["temperature", "pressure"]
            isolation_forest_engine.train(training_data, feature_columns)

            model_path = str(tmp_path / "test_model.joblib")
            saved_path = isolation_forest_engine.save_model(model_path)

            assert saved_path == model_path
            import os
            assert os.path.exists(model_path)

        def test_load_model(self, isolation_forest_engine, training_data, tmp_path):
            feature_columns = ["temperature", "pressure"]
            isolation_forest_engine.train(training_data, feature_columns)

            model_path = str(tmp_path / "test_model.joblib")
            isolation_forest_engine.save_model(model_path)

            # Create new engine and load
            new_engine = AnomalyDetectionEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            assert new_engine.is_loaded is True
            assert new_engine.algorithm == "isolation_forest"
            assert "temperature" in new_engine.feature_stats

        def test_save_raises_when_not_trained(self, isolation_forest_engine):
            with pytest.raises(ValueError, match="No model to save"):
                isolation_forest_engine.save_model("/tmp/test.joblib")

        def test_load_raises_when_file_not_found(self, isolation_forest_engine):
            with pytest.raises(FileNotFoundError):
                isolation_forest_engine.load_model("/nonexistent/path.joblib")
