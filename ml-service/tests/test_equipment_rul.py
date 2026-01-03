"""
Tests for Equipment RUL Engine.
"""
import pytest
import numpy as np
import pandas as pd
from uuid import uuid4

from app.engines.equipment_rul import EquipmentRULEngine


class TestEquipmentRULEngine:
    """Tests for EquipmentRULEngine class."""

    @pytest.fixture
    def rul_engine(self):
        """Create an Equipment RUL engine."""
        return EquipmentRULEngine(model_id=uuid4())

    @pytest.fixture
    def training_data(self):
        """
        Generate synthetic training data for RUL prediction.

        Simulates equipment degradation over time with multiple features:
        - operating_hours: Cumulative runtime
        - cycle_count: Number of operational cycles
        - temperature: Operating temperature
        - vibration: Vibration amplitude
        - wear_indicator: Simulated wear metric
        - rul: Remaining useful life in days (target)
        """
        np.random.seed(42)
        n_samples = 1000

        # Simulate equipment degradation patterns
        operating_hours = np.random.uniform(0, 10000, n_samples)
        cycle_count = np.random.uniform(0, 5000, n_samples)
        temperature = np.random.normal(75, 10, n_samples)
        vibration = np.random.uniform(0.1, 2.0, n_samples)
        wear_indicator = np.random.uniform(0, 100, n_samples)

        # Generate RUL with realistic degradation relationship
        # RUL decreases with operating hours, cycles, temperature, vibration, wear
        base_rul = 365  # 1 year baseline
        rul = (
            base_rul
            - (operating_hours / 50)
            - (cycle_count / 25)
            - (temperature - 75) * 0.5
            - vibration * 10
            - wear_indicator * 2
            + np.random.normal(0, 10, n_samples)  # Add noise
        )

        # Ensure non-negative RUL
        rul = np.clip(rul, 0, 365)

        return pd.DataFrame({
            "operating_hours": operating_hours,
            "cycle_count": cycle_count,
            "temperature": temperature,
            "vibration": vibration,
            "wear_indicator": wear_indicator,
            "rul": rul
        })

    @pytest.fixture
    def test_data(self):
        """Generate test data for predictions."""
        np.random.seed(123)
        n_samples = 100

        operating_hours = np.random.uniform(0, 10000, n_samples)
        cycle_count = np.random.uniform(0, 5000, n_samples)
        temperature = np.random.normal(75, 10, n_samples)
        vibration = np.random.uniform(0.1, 2.0, n_samples)
        wear_indicator = np.random.uniform(0, 100, n_samples)

        return pd.DataFrame({
            "operating_hours": operating_hours,
            "cycle_count": cycle_count,
            "temperature": temperature,
            "vibration": vibration,
            "wear_indicator": wear_indicator,
        })

    class TestInitialization:
        """Tests for engine initialization."""

        def test_creates_rul_engine(self, rul_engine):
            assert rul_engine.is_loaded is False
            assert rul_engine.model is None
            assert rul_engine.feature_importance_ is None

        def test_default_hyperparameters(self, rul_engine):
            params = rul_engine.get_default_hyperparameters()

            assert params["n_estimators"] == 200
            assert params["learning_rate"] == 0.1
            assert params["max_depth"] == 5
            assert params["min_samples_split"] == 10
            assert params["min_samples_leaf"] == 4
            assert params["max_features"] == "sqrt"
            assert params["subsample"] == 0.8
            assert params["loss"] == "huber"
            assert params["random_state"] == 42

        def test_stores_model_id(self):
            model_id = uuid4()
            engine = EquipmentRULEngine(model_id=model_id)
            assert engine.model_id == model_id

    class TestTraining:
        """Tests for model training."""

        def test_train_success(self, rul_engine, training_data):
            feature_columns = [
                "operating_hours", "cycle_count", "temperature",
                "vibration", "wear_indicator"
            ]
            target_column = "rul"

            metrics = rul_engine.train(
                training_data,
                feature_columns,
                target_column
            )

            assert rul_engine.is_loaded is True
            assert metrics["algorithm"] == "gradient_boosting_regressor"
            assert metrics["training_samples"] > 0
            assert metrics["validation_samples"] > 0
            assert metrics["features"] == len(feature_columns)

        def test_train_computes_metrics(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "temperature", "vibration"]
            target_column = "rul"

            metrics = rul_engine.train(
                training_data,
                feature_columns,
                target_column
            )

            # Training metrics
            assert "train_mae" in metrics
            assert "train_rmse" in metrics
            assert "train_r2" in metrics

            # Validation metrics
            assert "val_mae" in metrics
            assert "val_rmse" in metrics
            assert "val_r2" in metrics

            # Should have reasonable R² score
            assert metrics["val_r2"] > 0

        def test_train_computes_feature_importance(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "temperature", "vibration"]
            target_column = "rul"

            metrics = rul_engine.train(
                training_data,
                feature_columns,
                target_column
            )

            assert "feature_importance" in metrics
            assert len(metrics["feature_importance"]) <= len(feature_columns)

            # Feature importance should sum to 1.0
            importance_values = list(rul_engine.feature_importance_.values())
            assert abs(sum(importance_values) - 1.0) < 0.01

        def test_train_stores_feature_columns(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)

            assert rul_engine.feature_columns_ == feature_columns

        def test_train_with_custom_hyperparameters(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"
            custom_params = {
                "n_estimators": 50,
                "learning_rate": 0.05,
                "max_depth": 3
            }

            metrics = rul_engine.train(
                training_data,
                feature_columns,
                target_column,
                hyperparameters=custom_params
            )

            assert rul_engine.is_loaded is True
            assert "val_mae" in metrics

        def test_train_raises_without_target_column(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "temperature"]

            with pytest.raises(ValueError, match="target_column is required"):
                rul_engine.train(training_data, feature_columns)

        def test_train_raises_on_missing_target(self, rul_engine, training_data):
            feature_columns = ["operating_hours"]
            target_column = "nonexistent_column"

            with pytest.raises(ValueError, match="Target column.*not found"):
                rul_engine.train(training_data, feature_columns, target_column)

        def test_train_raises_on_missing_features(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "nonexistent_feature"]
            target_column = "rul"

            with pytest.raises(ValueError, match="Missing features"):
                rul_engine.train(training_data, feature_columns, target_column)

        def test_train_handles_negative_rul_values(self, rul_engine, training_data):
            # Add some negative RUL values
            training_data_copy = training_data.copy()
            training_data_copy.loc[0:5, "rul"] = -10

            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            # Should not raise, should clip to 0
            metrics = rul_engine.train(
                training_data_copy,
                feature_columns,
                target_column
            )

            assert metrics["min_rul"] >= 0

        def test_train_includes_rul_statistics(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            metrics = rul_engine.train(
                training_data,
                feature_columns,
                target_column
            )

            assert "mean_rul" in metrics
            assert "std_rul" in metrics
            assert "min_rul" in metrics
            assert "max_rul" in metrics

    class TestPrediction:
        """Tests for RUL prediction."""

        def test_predict_returns_array(self, rul_engine, training_data, test_data):
            feature_columns = ["operating_hours", "temperature", "vibration"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)
            predictions = rul_engine.predict(test_data, feature_columns)

            assert len(predictions) == len(test_data)
            assert isinstance(predictions, np.ndarray)

        def test_predict_returns_non_negative(self, rul_engine, training_data, test_data):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)
            predictions = rul_engine.predict(test_data, feature_columns)

            assert np.all(predictions >= 0)

        def test_predict_raises_when_not_trained(self, rul_engine, test_data):
            feature_columns = ["operating_hours", "temperature"]

            with pytest.raises(ValueError, match="not trained"):
                rul_engine.predict(test_data, feature_columns)

        def test_predict_raises_on_missing_features(self, rul_engine, training_data, test_data):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)

            with pytest.raises(ValueError, match="Missing features"):
                rul_engine.predict(test_data, ["nonexistent_feature"])

        def test_predict_single_sample(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)

            # Single sample prediction
            single_sample = training_data[feature_columns].iloc[[0]]
            predictions = rul_engine.predict(single_sample, feature_columns)

            assert len(predictions) == 1
            assert predictions[0] >= 0

    class TestPredictWithConfidence:
        """Tests for prediction with confidence intervals."""

        def test_predict_with_confidence_returns_three_arrays(
            self, rul_engine, training_data, test_data
        ):
            feature_columns = ["operating_hours", "temperature", "vibration"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)
            predictions, lower, upper = rul_engine.predict_with_confidence(
                test_data, feature_columns
            )

            assert len(predictions) == len(test_data)
            assert len(lower) == len(test_data)
            assert len(upper) == len(test_data)

        def test_confidence_intervals_are_valid(
            self, rul_engine, training_data, test_data
        ):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)
            predictions, lower, upper = rul_engine.predict_with_confidence(
                test_data, feature_columns
            )

            # Lower bound should be <= prediction <= upper bound
            assert np.all(lower <= predictions)
            assert np.all(predictions <= upper)

        def test_confidence_intervals_are_non_negative(
            self, rul_engine, training_data, test_data
        ):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)
            predictions, lower, upper = rul_engine.predict_with_confidence(
                test_data, feature_columns
            )

            assert np.all(lower >= 0)
            assert np.all(predictions >= 0)
            assert np.all(upper >= 0)

        def test_confidence_level_parameter(
            self, rul_engine, training_data, test_data
        ):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)

            # 95% confidence
            pred_95, lower_95, upper_95 = rul_engine.predict_with_confidence(
                test_data, feature_columns, confidence_level=0.95
            )

            # 99% confidence should have wider intervals
            pred_99, lower_99, upper_99 = rul_engine.predict_with_confidence(
                test_data, feature_columns, confidence_level=0.99
            )

            # Predictions should be the same
            assert np.allclose(pred_95, pred_99)

            # 99% intervals should be wider (on average)
            width_95 = np.mean(upper_95 - lower_95)
            width_99 = np.mean(upper_99 - lower_99)
            assert width_99 >= width_95

        def test_predict_with_confidence_raises_when_not_trained(
            self, rul_engine, test_data
        ):
            feature_columns = ["operating_hours", "temperature"]

            with pytest.raises(ValueError, match="not trained"):
                rul_engine.predict_with_confidence(test_data, feature_columns)

    class TestFeatureImportance:
        """Tests for feature importance."""

        def test_get_feature_importance_after_training(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "temperature", "vibration"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)
            importance = rul_engine.get_feature_importance()

            assert importance is not None
            assert len(importance) == len(feature_columns)
            assert all(feat in importance for feat in feature_columns)

        def test_get_feature_importance_before_training(self, rul_engine):
            importance = rul_engine.get_feature_importance()
            assert importance is None

        def test_feature_importance_sums_to_one(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "temperature", "vibration"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)
            importance = rul_engine.get_feature_importance()

            total_importance = sum(importance.values())
            assert abs(total_importance - 1.0) < 0.01

    class TestModelPersistence:
        """Tests for model save/load functionality."""

        def test_save_model(self, rul_engine, training_data, tmp_path):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)

            model_path = str(tmp_path / "test_rul_model.joblib")
            saved_path = rul_engine.save_model(model_path)

            assert saved_path == model_path
            import os
            assert os.path.exists(model_path)

        def test_load_model(self, rul_engine, training_data, test_data, tmp_path):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            # Train and save
            rul_engine.train(training_data, feature_columns, target_column)
            original_predictions = rul_engine.predict(test_data, feature_columns)
            original_importance = rul_engine.get_feature_importance()

            model_path = str(tmp_path / "test_rul_model.joblib")
            rul_engine.save_model(model_path)

            # Create new engine and load
            new_engine = EquipmentRULEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            assert new_engine.is_loaded is True
            assert new_engine.feature_columns_ == feature_columns

            # Predictions should match
            new_predictions = new_engine.predict(test_data, feature_columns)
            assert np.allclose(original_predictions, new_predictions)

            # Feature importance should match
            new_importance = new_engine.get_feature_importance()
            assert original_importance.keys() == new_importance.keys()
            for key in original_importance:
                assert abs(original_importance[key] - new_importance[key]) < 1e-6

        def test_save_raises_when_not_trained(self, rul_engine):
            with pytest.raises(ValueError, match="No model to save"):
                rul_engine.save_model("/tmp/test.joblib")

        def test_load_raises_when_file_not_found(self, rul_engine):
            with pytest.raises(FileNotFoundError):
                rul_engine.load_model("/nonexistent/path.joblib")

        def test_load_raises_when_no_path_specified(self, rul_engine):
            with pytest.raises(ValueError, match="No model path specified"):
                rul_engine.load_model()

        def test_save_and_load_preserves_scaler(
            self, rul_engine, training_data, test_data, tmp_path
        ):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            # Train
            rul_engine.train(training_data, feature_columns, target_column)
            original_mean = rul_engine.scaler.mean_
            original_scale = rul_engine.scaler.scale_

            # Save and load
            model_path = str(tmp_path / "test_rul_model.joblib")
            rul_engine.save_model(model_path)

            new_engine = EquipmentRULEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            # Scaler parameters should match
            assert np.allclose(original_mean, new_engine.scaler.mean_)
            assert np.allclose(original_scale, new_engine.scaler.scale_)

    class TestEdgeCases:
        """Tests for edge cases and error handling."""

        def test_train_with_small_dataset(self, rul_engine):
            # Very small dataset
            small_data = pd.DataFrame({
                "feature1": [1, 2, 3, 4, 5],
                "rul": [100, 90, 80, 70, 60]
            })

            # Should still work but may have poor metrics
            metrics = rul_engine.train(small_data, ["feature1"], "rul")
            assert rul_engine.is_loaded is True

        def test_train_with_constant_target(self, rul_engine):
            # All RUL values are the same
            constant_data = pd.DataFrame({
                "feature1": np.random.rand(100),
                "feature2": np.random.rand(100),
                "rul": [100] * 100
            })

            metrics = rul_engine.train(
                constant_data,
                ["feature1", "feature2"],
                "rul"
            )

            # R² should be poor but training should complete
            assert rul_engine.is_loaded is True

        def test_predict_with_extreme_values(self, rul_engine, training_data):
            feature_columns = ["operating_hours", "temperature"]
            target_column = "rul"

            rul_engine.train(training_data, feature_columns, target_column)

            # Create test data with extreme values
            extreme_data = pd.DataFrame({
                "operating_hours": [1e6, 0],
                "temperature": [200, -50]
            })

            predictions = rul_engine.predict(extreme_data, feature_columns)

            # Should still produce non-negative predictions
            assert np.all(predictions >= 0)
            assert len(predictions) == 2
