"""
Energy Forecasting Engine using XGBoost/GradientBoosting for time-series prediction.
"""
import logging
from typing import Any, Dict, List, Optional, Tuple
from uuid import UUID

import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.preprocessing import StandardScaler

from app.engines.base import BaseMLEngine

logger = logging.getLogger(__name__)

# Try to import XGBoost, fall back to sklearn if not available
try:
    from xgboost import XGBRegressor
    XGBOOST_AVAILABLE = True
except ImportError:
    XGBOOST_AVAILABLE = False
    logger.warning("XGBoost not available, will use sklearn GradientBoostingRegressor")


class EnergyForecastingEngine(BaseMLEngine):
    """
    Energy consumption forecasting using gradient boosting regression.

    Supports two algorithms:
    - xgboost: XGBoost regressor (preferred, higher performance)
    - gradient_boosting: sklearn GradientBoostingRegressor (fallback)

    Features:
    - Time-based: hour_of_day, day_of_week, is_weekend, month
    - Lag features: 1h, 24h, 7d historical consumption
    - Multi-step forecasting: Recursive predictions for 7-day horizon

    Metrics:
    - MAE: Mean Absolute Error
    - RMSE: Root Mean Squared Error
    - MAPE: Mean Absolute Percentage Error
    """

    def __init__(
        self,
        model_id: UUID,
        algorithm: str = "auto",
        model_path: Optional[str] = None,
    ):
        super().__init__(model_id, model_path)

        # Auto-select algorithm based on availability
        if algorithm == "auto":
            self.algorithm = "xgboost" if XGBOOST_AVAILABLE else "gradient_boosting"
        else:
            self.algorithm = algorithm

        # Validate algorithm selection
        if self.algorithm == "xgboost" and not XGBOOST_AVAILABLE:
            logger.warning("XGBoost requested but not available, falling back to gradient_boosting")
            self.algorithm = "gradient_boosting"

        self.scaler = StandardScaler()
        self.target_scaler = StandardScaler()
        self.feature_names: List[str] = []
        self.training_stats: Dict[str, float] = {}

    def get_default_hyperparameters(self) -> Dict[str, Any]:
        """Return default hyperparameters."""
        if self.algorithm == "xgboost":
            return {
                "n_estimators": 100,
                "learning_rate": 0.1,
                "max_depth": 6,
                "min_child_weight": 1,
                "subsample": 0.8,
                "colsample_bytree": 0.8,
                "gamma": 0,
                "reg_alpha": 0,
                "reg_lambda": 1,
                "random_state": 42,
            }
        else:  # gradient_boosting
            return {
                "n_estimators": 100,
                "learning_rate": 0.1,
                "max_depth": 6,
                "min_samples_split": 2,
                "min_samples_leaf": 1,
                "subsample": 0.8,
                "max_features": "sqrt",
                "random_state": 42,
            }

    def engineer_features(
        self,
        data: pd.DataFrame,
        target_column: str,
        timestamp_column: str = "timestamp",
    ) -> pd.DataFrame:
        """
        Engineer time-based and lag features from raw telemetry data.

        Args:
            data: DataFrame with timestamp and consumption columns
            target_column: Name of the energy consumption column
            timestamp_column: Name of the timestamp column

        Returns:
            DataFrame with engineered features
        """
        df = data.copy()

        # Ensure timestamp is datetime
        if not pd.api.types.is_datetime64_any_dtype(df[timestamp_column]):
            df[timestamp_column] = pd.to_datetime(df[timestamp_column])

        # Sort by timestamp
        df = df.sort_values(timestamp_column).reset_index(drop=True)

        # Time-based features
        df["hour_of_day"] = df[timestamp_column].dt.hour
        df["day_of_week"] = df[timestamp_column].dt.dayofweek
        df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)
        df["month"] = df[timestamp_column].dt.month
        df["day_of_month"] = df[timestamp_column].dt.day
        df["week_of_year"] = df[timestamp_column].dt.isocalendar().week.astype(int)

        # Lag features (historical consumption)
        # 1-hour lag
        df["lag_1h"] = df[target_column].shift(1)

        # 24-hour lag (daily seasonality)
        df["lag_24h"] = df[target_column].shift(24)

        # 7-day lag (weekly seasonality) - assumes hourly data
        df["lag_7d"] = df[target_column].shift(168)

        # Rolling statistics (past 24 hours)
        df["rolling_mean_24h"] = df[target_column].rolling(window=24, min_periods=1).mean()
        df["rolling_std_24h"] = df[target_column].rolling(window=24, min_periods=1).std()

        # Drop rows with NaN lag features (initial rows)
        # Keep at least some data for training
        df = df.dropna(subset=["lag_1h", "lag_24h"])

        # Fill remaining NaNs with forward fill then backward fill
        df = df.ffill().bfill()

        return df

    def train(
        self,
        data: pd.DataFrame,
        feature_columns: List[str],
        target_column: Optional[str] = None,
        hyperparameters: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """
        Train the energy forecasting model.

        Args:
            data: Training data with timestamp and consumption columns
            feature_columns: List of feature column names (can be raw or engineered)
            target_column: Energy consumption column name (required)
            hyperparameters: Optional custom hyperparameters

        Returns:
            Dictionary containing training metrics (MAE, RMSE, MAPE)
        """
        if target_column is None:
            raise ValueError("target_column is required for energy forecasting")

        # Check if features are already engineered or need engineering
        needs_engineering = "hour_of_day" not in data.columns

        if needs_engineering:
            # Assume data has timestamp and target columns
            if "timestamp" not in data.columns:
                raise ValueError("Data must contain 'timestamp' column for feature engineering")

            logger.info("Engineering features from raw telemetry data")
            df_engineered = self.engineer_features(data, target_column)

            # Define standard feature set
            self.feature_names = [
                "hour_of_day", "day_of_week", "is_weekend", "month",
                "day_of_month", "week_of_year",
                "lag_1h", "lag_24h", "lag_7d",
                "rolling_mean_24h", "rolling_std_24h"
            ]
        else:
            # Features already engineered
            df_engineered = data.copy()
            self.feature_names = feature_columns

        # Validate features
        self.validate_features(df_engineered, self.feature_names)

        # Extract features and target
        X = df_engineered[self.feature_names].values
        y = df_engineered[target_column].values.reshape(-1, 1)

        logger.info(
            f"Training {self.algorithm} on {len(X)} samples, "
            f"{len(self.feature_names)} features"
        )

        # Store training statistics
        self.training_stats = {
            "mean": float(np.mean(y)),
            "std": float(np.std(y)),
            "min": float(np.min(y)),
            "max": float(np.max(y)),
        }

        # Scale features and target
        X_scaled = self.scaler.fit_transform(X)
        y_scaled = self.target_scaler.fit_transform(y).ravel()

        # Merge default and custom hyperparameters
        params = {**self.get_default_hyperparameters(), **(hyperparameters or {})}

        # Create and train model
        if self.algorithm == "xgboost":
            self.model = XGBRegressor(
                n_estimators=params["n_estimators"],
                learning_rate=params["learning_rate"],
                max_depth=params["max_depth"],
                min_child_weight=params["min_child_weight"],
                subsample=params["subsample"],
                colsample_bytree=params["colsample_bytree"],
                gamma=params["gamma"],
                reg_alpha=params["reg_alpha"],
                reg_lambda=params["reg_lambda"],
                random_state=params["random_state"],
                verbosity=0,
            )
        else:  # gradient_boosting
            self.model = GradientBoostingRegressor(
                n_estimators=params["n_estimators"],
                learning_rate=params["learning_rate"],
                max_depth=params["max_depth"],
                min_samples_split=params["min_samples_split"],
                min_samples_leaf=params["min_samples_leaf"],
                subsample=params["subsample"],
                max_features=params["max_features"],
                random_state=params["random_state"],
                verbose=0,
            )

        self.model.fit(X_scaled, y_scaled)
        self.is_loaded = True

        # Compute training metrics
        y_pred_scaled = self.model.predict(X_scaled)
        y_pred = self.target_scaler.inverse_transform(y_pred_scaled.reshape(-1, 1)).ravel()

        metrics = self._compute_metrics(y.ravel(), y_pred)
        metrics.update({
            "algorithm": self.algorithm,
            "training_samples": len(X),
            "features": len(self.feature_names),
            "target_mean": self.training_stats["mean"],
            "target_std": self.training_stats["std"],
        })

        logger.info(f"Training complete: MAE={metrics['mae']:.2f}, RMSE={metrics['rmse']:.2f}, MAPE={metrics['mape']:.2f}%")
        return metrics

    def predict(
        self,
        data: pd.DataFrame,
        feature_columns: List[str]
    ) -> np.ndarray:
        """
        Make predictions on provided data.

        Args:
            data: DataFrame with engineered features
            feature_columns: List of feature column names

        Returns:
            Array of energy consumption predictions
        """
        if not self.is_loaded:
            raise ValueError("Model not trained or loaded")

        self.validate_features(data, feature_columns)

        X = data[feature_columns].values

        # Handle NaN values (can occur in lag features for new data)
        if np.isnan(X).any():
            X = np.nan_to_num(X, nan=0.0)

        X_scaled = self.scaler.transform(X)

        y_pred_scaled = self.model.predict(X_scaled)
        y_pred = self.target_scaler.inverse_transform(y_pred_scaled.reshape(-1, 1)).ravel()

        return y_pred

    def forecast(
        self,
        last_known_data: pd.DataFrame,
        target_column: str,
        periods: int = 168,  # 7 days * 24 hours
        frequency: str = "1H",
    ) -> Tuple[pd.DataFrame, np.ndarray]:
        """
        Generate multi-step ahead forecast.

        Uses recursive prediction: each prediction becomes a feature for the next step.

        Args:
            last_known_data: Recent historical data with timestamp and consumption
            target_column: Energy consumption column name
            periods: Number of time steps to forecast (default: 168 hours = 7 days)
            frequency: Time frequency for forecast (default: 1H for hourly)

        Returns:
            Tuple of (forecast_df, predictions_array)
            - forecast_df: DataFrame with timestamp and forecasted consumption
            - predictions_array: NumPy array of predictions
        """
        if not self.is_loaded:
            raise ValueError("Model not trained or loaded")

        # Engineer features from last known data
        df_hist = self.engineer_features(last_known_data, target_column)

        # Get the last known timestamp
        last_timestamp = pd.to_datetime(last_known_data["timestamp"].max())

        # Generate future timestamps
        future_timestamps = pd.date_range(
            start=last_timestamp + pd.Timedelta(frequency),
            periods=periods,
            freq=frequency
        )

        predictions = []

        # Start with the most recent historical data for lag features
        # We need enough history for lag_7d (168 hours)
        history_window = df_hist.tail(168).copy()

        for future_timestamp in future_timestamps:
            # Create a row for the next timestamp
            next_row = {
                "timestamp": future_timestamp,
                "hour_of_day": future_timestamp.hour,
                "day_of_week": future_timestamp.dayofweek,
                "is_weekend": 1 if future_timestamp.dayofweek >= 5 else 0,
                "month": future_timestamp.month,
                "day_of_month": future_timestamp.day,
                "week_of_year": future_timestamp.isocalendar().week,
            }

            # Add lag features from history
            if len(history_window) >= 1:
                next_row["lag_1h"] = history_window.iloc[-1][target_column]
            else:
                next_row["lag_1h"] = self.training_stats["mean"]

            if len(history_window) >= 24:
                next_row["lag_24h"] = history_window.iloc[-24][target_column]
            else:
                next_row["lag_24h"] = self.training_stats["mean"]

            if len(history_window) >= 168:
                next_row["lag_7d"] = history_window.iloc[-168][target_column]
            else:
                next_row["lag_7d"] = self.training_stats["mean"]

            # Rolling statistics from recent history
            recent_values = history_window.tail(24)[target_column].values
            next_row["rolling_mean_24h"] = np.mean(recent_values)
            next_row["rolling_std_24h"] = np.std(recent_values)

            # Create DataFrame for prediction
            next_df = pd.DataFrame([next_row])

            # Make prediction
            X = next_df[self.feature_names].values
            X_scaled = self.scaler.transform(X)
            y_pred_scaled = self.model.predict(X_scaled)
            y_pred = self.target_scaler.inverse_transform(y_pred_scaled.reshape(-1, 1))[0, 0]

            # Ensure non-negative predictions (energy consumption can't be negative)
            y_pred = max(0.0, y_pred)

            predictions.append(y_pred)

            # Add prediction to history for next iteration
            next_row[target_column] = y_pred
            history_window = pd.concat([
                history_window,
                pd.DataFrame([next_row])
            ], ignore_index=True)

            # Keep only the last 168 rows to prevent memory growth
            history_window = history_window.tail(168)

        # Create forecast DataFrame
        forecast_df = pd.DataFrame({
            "timestamp": future_timestamps,
            "predicted_consumption": predictions,
        })

        logger.info(f"Generated {periods}-step forecast: mean={np.mean(predictions):.2f}, range=[{np.min(predictions):.2f}, {np.max(predictions):.2f}]")

        return forecast_df, np.array(predictions)

    def _compute_metrics(self, y_true: np.ndarray, y_pred: np.ndarray) -> Dict[str, float]:
        """
        Compute regression metrics.

        Args:
            y_true: Ground truth values
            y_pred: Predicted values

        Returns:
            Dictionary with MAE, RMSE, MAPE
        """
        # Mean Absolute Error
        mae = float(np.mean(np.abs(y_true - y_pred)))

        # Root Mean Squared Error
        rmse = float(np.sqrt(np.mean((y_true - y_pred) ** 2)))

        # Mean Absolute Percentage Error (avoid division by zero)
        mask = y_true != 0
        mape = float(np.mean(np.abs((y_true[mask] - y_pred[mask]) / y_true[mask])) * 100) if np.any(mask) else 0.0

        return {
            "mae": mae,
            "rmse": rmse,
            "mape": mape,
        }

    def save_model(self, path: Optional[str] = None) -> str:
        """Save model with scalers and training statistics."""
        if not self.is_loaded:
            raise ValueError("No model to save - train first")

        save_path = path or self._get_model_path()

        model_data = {
            "algorithm": self.algorithm,
            "model": self.model,
            "scaler": self.scaler,
            "target_scaler": self.target_scaler,
            "feature_names": self.feature_names,
            "training_stats": self.training_stats,
        }

        import joblib
        from pathlib import Path
        Path(save_path).parent.mkdir(parents=True, exist_ok=True)
        joblib.dump(model_data, save_path)

        self.model_path = save_path
        logger.info(f"Energy forecasting model saved to {save_path}")
        return save_path

    def load_model(self, path: Optional[str] = None) -> None:
        """Load model with scalers and training statistics."""
        load_path = path or self.model_path
        if not load_path:
            raise ValueError("No model path specified")

        import joblib
        from pathlib import Path
        if not Path(load_path).exists():
            raise FileNotFoundError(f"Model file not found: {load_path}")

        model_data = joblib.load(load_path)
        self.algorithm = model_data["algorithm"]
        self.model = model_data["model"]
        self.scaler = model_data["scaler"]
        self.target_scaler = model_data["target_scaler"]
        self.feature_names = model_data["feature_names"]
        self.training_stats = model_data["training_stats"]
        self.is_loaded = True

        logger.info(f"Energy forecasting model loaded from {load_path}")
