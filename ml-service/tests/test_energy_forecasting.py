"""
Tests for Energy Forecasting Engine.
"""
import pytest
import numpy as np
import pandas as pd
from uuid import uuid4
from datetime import datetime, timedelta

from app.engines.energy_forecasting import EnergyForecastingEngine, XGBOOST_AVAILABLE


class TestEnergyForecastingEngine:
    """Tests for EnergyForecastingEngine class."""

    @pytest.fixture
    def xgboost_engine(self):
        """Create an XGBoost engine."""
        return EnergyForecastingEngine(
            model_id=uuid4(),
            algorithm="auto"
        )

    @pytest.fixture
    def gradient_boosting_engine(self):
        """Create a GradientBoosting engine."""
        return EnergyForecastingEngine(
            model_id=uuid4(),
            algorithm="gradient_boosting"
        )

    @pytest.fixture
    def training_data(self):
        """Generate synthetic hourly energy consumption data."""
        np.random.seed(42)

        # Generate 30 days of hourly data
        n_hours = 30 * 24  # 720 hours
        start_time = datetime(2024, 1, 1, 0, 0, 0)

        timestamps = [start_time + timedelta(hours=i) for i in range(n_hours)]

        # Simulate realistic energy consumption patterns
        consumption = []
        for i, ts in enumerate(timestamps):
            # Base load
            base = 100.0

            # Daily seasonality (higher during day, lower at night)
            hour = ts.hour
            daily_pattern = 30 * np.sin((hour - 6) * np.pi / 12)

            # Weekly seasonality (lower on weekends)
            is_weekend = ts.weekday() >= 5
            weekly_pattern = -20 if is_weekend else 0

            # Random noise
            noise = np.random.normal(0, 5)

            consumption.append(base + daily_pattern + weekly_pattern + noise)

        return pd.DataFrame({
            "timestamp": timestamps,
            "consumption": consumption
        })

    @pytest.fixture
    def test_data(self, training_data):
        """Generate test data (last 7 days of training data)."""
        return training_data.tail(7 * 24).copy()

    class TestInitialization:
        """Tests for engine initialization."""

        def test_creates_xgboost_engine_when_available(self, xgboost_engine):
            if XGBOOST_AVAILABLE:
                assert xgboost_engine.algorithm == "xgboost"
            else:
                assert xgboost_engine.algorithm == "gradient_boosting"
            assert xgboost_engine.is_loaded is False

        def test_creates_gradient_boosting_engine(self, gradient_boosting_engine):
            assert gradient_boosting_engine.algorithm == "gradient_boosting"
            assert gradient_boosting_engine.is_loaded is False

        def test_default_hyperparameters_xgboost(self):
            if not XGBOOST_AVAILABLE:
                pytest.skip("XGBoost not available")

            engine = EnergyForecastingEngine(model_id=uuid4(), algorithm="xgboost")
            params = engine.get_default_hyperparameters()

            assert params["n_estimators"] == 100
            assert params["learning_rate"] == 0.1
            assert params["max_depth"] == 6
            assert params["random_state"] == 42

        def test_default_hyperparameters_gradient_boosting(self, gradient_boosting_engine):
            params = gradient_boosting_engine.get_default_hyperparameters()

            assert params["n_estimators"] == 100
            assert params["learning_rate"] == 0.1
            assert params["max_depth"] == 6
            assert params["random_state"] == 42

    class TestFeatureEngineering:
        """Tests for feature engineering."""

        def test_engineer_features_creates_time_features(self, xgboost_engine, training_data):
            df_engineered = xgboost_engine.engineer_features(
                training_data,
                target_column="consumption"
            )

            assert "hour_of_day" in df_engineered.columns
            assert "day_of_week" in df_engineered.columns
            assert "is_weekend" in df_engineered.columns
            assert "month" in df_engineered.columns
            assert "day_of_month" in df_engineered.columns
            assert "week_of_year" in df_engineered.columns

        def test_engineer_features_creates_lag_features(self, xgboost_engine, training_data):
            df_engineered = xgboost_engine.engineer_features(
                training_data,
                target_column="consumption"
            )

            assert "lag_1h" in df_engineered.columns
            assert "lag_24h" in df_engineered.columns
            assert "lag_7d" in df_engineered.columns

        def test_engineer_features_creates_rolling_stats(self, xgboost_engine, training_data):
            df_engineered = xgboost_engine.engineer_features(
                training_data,
                target_column="consumption"
            )

            assert "rolling_mean_24h" in df_engineered.columns
            assert "rolling_std_24h" in df_engineered.columns

        def test_engineer_features_removes_nan_rows(self, xgboost_engine, training_data):
            df_engineered = xgboost_engine.engineer_features(
                training_data,
                target_column="consumption"
            )

            # Should have fewer rows due to lag features
            assert len(df_engineered) < len(training_data)

            # Should not have NaN in lag features
            assert not df_engineered["lag_1h"].isna().any()
            assert not df_engineered["lag_24h"].isna().any()

        def test_engineer_features_hour_of_day_range(self, xgboost_engine, training_data):
            df_engineered = xgboost_engine.engineer_features(
                training_data,
                target_column="consumption"
            )

            assert df_engineered["hour_of_day"].min() >= 0
            assert df_engineered["hour_of_day"].max() <= 23

        def test_engineer_features_is_weekend_binary(self, xgboost_engine, training_data):
            df_engineered = xgboost_engine.engineer_features(
                training_data,
                target_column="consumption"
            )

            assert set(df_engineered["is_weekend"].unique()).issubset({0, 1})

    class TestTraining:
        """Tests for model training."""

        def test_train_with_raw_data(self, xgboost_engine, training_data):
            feature_columns = ["consumption"]  # Will be ignored, features auto-engineered

            metrics = xgboost_engine.train(
                training_data,
                feature_columns,
                target_column="consumption"
            )

            assert xgboost_engine.is_loaded is True
            assert "mae" in metrics
            assert "rmse" in metrics
            assert "mape" in metrics
            assert metrics["training_samples"] > 0
            assert metrics["features"] > 0

        def test_train_gradient_boosting(self, gradient_boosting_engine, training_data):
            feature_columns = ["consumption"]

            metrics = gradient_boosting_engine.train(
                training_data,
                feature_columns,
                target_column="consumption"
            )

            assert gradient_boosting_engine.is_loaded is True
            assert metrics["algorithm"] == "gradient_boosting"

        def test_train_stores_training_stats(self, xgboost_engine, training_data):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            assert "mean" in xgboost_engine.training_stats
            assert "std" in xgboost_engine.training_stats
            assert "min" in xgboost_engine.training_stats
            assert "max" in xgboost_engine.training_stats

            # Validate reasonable values
            assert xgboost_engine.training_stats["mean"] > 0
            assert xgboost_engine.training_stats["std"] > 0

        def test_train_stores_feature_names(self, xgboost_engine, training_data):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            assert len(xgboost_engine.feature_names) > 0
            assert "hour_of_day" in xgboost_engine.feature_names
            assert "lag_1h" in xgboost_engine.feature_names

        def test_train_with_custom_hyperparameters(self, xgboost_engine, training_data):
            custom_params = {
                "n_estimators": 50,
                "learning_rate": 0.05,
                "max_depth": 4
            }

            metrics = xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption",
                hyperparameters=custom_params
            )

            assert xgboost_engine.is_loaded is True

        def test_train_raises_without_target_column(self, xgboost_engine, training_data):
            with pytest.raises(ValueError, match="target_column is required"):
                xgboost_engine.train(training_data, ["consumption"])

        def test_train_raises_without_timestamp(self, xgboost_engine):
            # Data without timestamp column
            bad_data = pd.DataFrame({
                "consumption": [100, 110, 105, 95]
            })

            with pytest.raises(ValueError, match="timestamp"):
                xgboost_engine.train(
                    bad_data,
                    ["consumption"],
                    target_column="consumption"
                )

        def test_training_metrics_are_reasonable(self, xgboost_engine, training_data):
            metrics = xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            # MAE and RMSE should be positive
            assert metrics["mae"] > 0
            assert metrics["rmse"] > 0

            # MAPE should be percentage
            assert 0 <= metrics["mape"] <= 100

            # RMSE should be >= MAE (mathematical property)
            assert metrics["rmse"] >= metrics["mae"]

    class TestPrediction:
        """Tests for making predictions."""

        def test_predict_returns_correct_shape(self, xgboost_engine, training_data):
            # Train model
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            # Engineer features for prediction
            df_engineered = xgboost_engine.engineer_features(
                training_data.tail(100),
                target_column="consumption"
            )

            predictions = xgboost_engine.predict(
                df_engineered,
                xgboost_engine.feature_names
            )

            assert len(predictions) == len(df_engineered)
            assert isinstance(predictions, np.ndarray)

        def test_predict_returns_positive_values(self, xgboost_engine, training_data):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            df_engineered = xgboost_engine.engineer_features(
                training_data.tail(50),
                target_column="consumption"
            )

            predictions = xgboost_engine.predict(
                df_engineered,
                xgboost_engine.feature_names
            )

            # Most predictions should be positive (energy consumption)
            assert np.mean(predictions > 0) > 0.95

        def test_predict_raises_when_not_trained(self, xgboost_engine, training_data):
            df_engineered = xgboost_engine.engineer_features(
                training_data.tail(10),
                target_column="consumption"
            )

            with pytest.raises(ValueError, match="not trained"):
                xgboost_engine.predict(
                    df_engineered,
                    ["hour_of_day", "lag_1h"]
                )

        def test_predict_raises_on_missing_features(self, xgboost_engine, training_data):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            # Data missing required features
            bad_data = pd.DataFrame({
                "hour_of_day": [12, 13, 14]
            })

            with pytest.raises(ValueError, match="Missing features"):
                xgboost_engine.predict(bad_data, xgboost_engine.feature_names)

    class TestForecasting:
        """Tests for multi-step forecasting."""

        def test_forecast_returns_correct_periods(self, xgboost_engine, training_data):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            periods = 24  # 1 day
            forecast_df, predictions = xgboost_engine.forecast(
                training_data.tail(200),
                target_column="consumption",
                periods=periods
            )

            assert len(forecast_df) == periods
            assert len(predictions) == periods
            assert "timestamp" in forecast_df.columns
            assert "predicted_consumption" in forecast_df.columns

        def test_forecast_7_days(self, xgboost_engine, training_data):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            # 7 days * 24 hours = 168 hours
            forecast_df, predictions = xgboost_engine.forecast(
                training_data.tail(200),
                target_column="consumption",
                periods=168
            )

            assert len(predictions) == 168

        def test_forecast_predictions_are_positive(self, xgboost_engine, training_data):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            _, predictions = xgboost_engine.forecast(
                training_data.tail(200),
                target_column="consumption",
                periods=48
            )

            # All predictions should be non-negative
            assert np.all(predictions >= 0)

        def test_forecast_timestamps_are_future(self, xgboost_engine, training_data):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            last_timestamp = training_data["timestamp"].max()

            forecast_df, _ = xgboost_engine.forecast(
                training_data.tail(200),
                target_column="consumption",
                periods=24
            )

            # All forecast timestamps should be after last known timestamp
            assert (forecast_df["timestamp"] > last_timestamp).all()

        def test_forecast_timestamps_are_sequential(self, xgboost_engine, training_data):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            forecast_df, _ = xgboost_engine.forecast(
                training_data.tail(200),
                target_column="consumption",
                periods=24,
                frequency="1H"
            )

            # Check timestamps are 1 hour apart
            time_diffs = forecast_df["timestamp"].diff().dropna()
            assert all(time_diffs == pd.Timedelta("1H"))

        def test_forecast_raises_when_not_trained(self, xgboost_engine, training_data):
            with pytest.raises(ValueError, match="not trained"):
                xgboost_engine.forecast(
                    training_data,
                    target_column="consumption",
                    periods=24
                )

        def test_forecast_predictions_within_reasonable_range(self, xgboost_engine, training_data):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            _, predictions = xgboost_engine.forecast(
                training_data.tail(200),
                target_column="consumption",
                periods=168
            )

            # Predictions should be within reasonable bounds of training data
            train_mean = training_data["consumption"].mean()
            train_std = training_data["consumption"].std()

            # Most predictions should be within 3 standard deviations
            within_bounds = np.abs(predictions - train_mean) <= 3 * train_std
            assert np.mean(within_bounds) > 0.90

    class TestMetrics:
        """Tests for metric computation."""

        def test_compute_metrics_mae(self, xgboost_engine):
            y_true = np.array([100, 110, 105, 95])
            y_pred = np.array([98, 112, 107, 93])

            metrics = xgboost_engine._compute_metrics(y_true, y_pred)

            # MAE = mean(|100-98|, |110-112|, |105-107|, |95-93|) = mean(2,2,2,2) = 2
            assert abs(metrics["mae"] - 2.0) < 0.01

        def test_compute_metrics_rmse(self, xgboost_engine):
            y_true = np.array([100, 110, 105, 95])
            y_pred = np.array([98, 112, 107, 93])

            metrics = xgboost_engine._compute_metrics(y_true, y_pred)

            # RMSE = sqrt(mean((2^2, 2^2, 2^2, 2^2))) = sqrt(4) = 2
            assert abs(metrics["rmse"] - 2.0) < 0.01

        def test_compute_metrics_mape(self, xgboost_engine):
            y_true = np.array([100, 100, 100, 100])
            y_pred = np.array([90, 110, 95, 105])

            metrics = xgboost_engine._compute_metrics(y_true, y_pred)

            # MAPE = mean(|10/100|, |10/100|, |5/100|, |5/100|) * 100 = 7.5%
            assert abs(metrics["mape"] - 7.5) < 0.01

        def test_compute_metrics_handles_zero_values(self, xgboost_engine):
            y_true = np.array([0, 100, 0, 100])
            y_pred = np.array([5, 95, 5, 105])

            metrics = xgboost_engine._compute_metrics(y_true, y_pred)

            # Should not crash with division by zero
            assert "mape" in metrics
            assert metrics["mape"] >= 0

    class TestModelPersistence:
        """Tests for model save/load functionality."""

        def test_save_model(self, xgboost_engine, training_data, tmp_path):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            model_path = str(tmp_path / "test_energy_model.joblib")
            saved_path = xgboost_engine.save_model(model_path)

            assert saved_path == model_path
            import os
            assert os.path.exists(model_path)

        def test_load_model(self, xgboost_engine, training_data, tmp_path):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            model_path = str(tmp_path / "test_energy_model.joblib")
            xgboost_engine.save_model(model_path)

            # Create new engine and load
            new_engine = EnergyForecastingEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            assert new_engine.is_loaded is True
            assert len(new_engine.feature_names) > 0
            assert "mean" in new_engine.training_stats

        def test_loaded_model_can_predict(self, xgboost_engine, training_data, tmp_path):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            # Save and reload
            model_path = str(tmp_path / "test_energy_model.joblib")
            xgboost_engine.save_model(model_path)

            new_engine = EnergyForecastingEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            # Test prediction
            df_engineered = new_engine.engineer_features(
                training_data.tail(100),
                target_column="consumption"
            )

            predictions = new_engine.predict(
                df_engineered,
                new_engine.feature_names
            )

            assert len(predictions) > 0

        def test_loaded_model_can_forecast(self, xgboost_engine, training_data, tmp_path):
            xgboost_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            # Save and reload
            model_path = str(tmp_path / "test_energy_model.joblib")
            xgboost_engine.save_model(model_path)

            new_engine = EnergyForecastingEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            # Test forecast
            forecast_df, predictions = new_engine.forecast(
                training_data.tail(200),
                target_column="consumption",
                periods=24
            )

            assert len(predictions) == 24

        def test_save_raises_when_not_trained(self, xgboost_engine):
            with pytest.raises(ValueError, match="No model to save"):
                xgboost_engine.save_model("/tmp/test.joblib")

        def test_load_raises_when_file_not_found(self, xgboost_engine):
            with pytest.raises(FileNotFoundError):
                xgboost_engine.load_model("/nonexistent/path.joblib")

        def test_saved_model_preserves_algorithm(self, gradient_boosting_engine, training_data, tmp_path):
            gradient_boosting_engine.train(
                training_data,
                ["consumption"],
                target_column="consumption"
            )

            model_path = str(tmp_path / "test_gb_model.joblib")
            gradient_boosting_engine.save_model(model_path)

            new_engine = EnergyForecastingEngine(model_id=uuid4())
            new_engine.load_model(model_path)

            assert new_engine.algorithm == "gradient_boosting"
