"""
Equipment RUL (Remaining Useful Life) Engine using Gradient Boosting Regressor.

Estimates the remaining useful life in days until maintenance is needed based on
degradation indicators, operating hours, cycle counts, and sensor readings.
"""
import logging
from typing import Any, Dict, List, Optional, Tuple
from uuid import UUID

import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

from app.engines.base import BaseMLEngine

logger = logging.getLogger(__name__)


class EquipmentRULEngine(BaseMLEngine):
    """
    Equipment RUL prediction using Gradient Boosting Regressor.

    Predicts remaining useful life (RUL) in days based on equipment condition indicators.
    Features typically include:
    - Degradation indicators (wear, corrosion, fatigue metrics)
    - Operating hours and cycle counts
    - Sensor readings (temperature, vibration, pressure, etc.)
    - Historical maintenance patterns

    The model outputs RUL predictions with confidence intervals to support
    maintenance planning and scheduling decisions.
    """

    def __init__(
        self,
        model_id: UUID,
        model_path: Optional[str] = None,
    ):
        super().__init__(model_id, model_path)
        self.scaler = StandardScaler()
        self.feature_importance_: Optional[Dict[str, float]] = None
        self.feature_columns_: Optional[List[str]] = None

    def get_default_hyperparameters(self) -> Dict[str, Any]:
        """
        Return default hyperparameters for Gradient Boosting Regressor.

        Returns:
            Dictionary with sensible defaults optimized for RUL prediction.
        """
        return {
            "n_estimators": 200,           # Number of boosting stages
            "learning_rate": 0.1,           # Shrinks contribution of each tree
            "max_depth": 5,                 # Maximum depth of trees
            "min_samples_split": 10,        # Minimum samples to split node
            "min_samples_leaf": 4,          # Minimum samples at leaf node
            "max_features": "sqrt",         # Features to consider for splits
            "subsample": 0.8,               # Fraction of samples for fitting trees
            "loss": "huber",                # Robust to outliers
            "alpha": 0.9,                   # Quantile for huber loss
            "random_state": 42,             # Reproducibility
            "validation_fraction": 0.1,     # For early stopping
            "n_iter_no_change": 10,         # Early stopping patience
            "tol": 1e-4,                    # Early stopping tolerance
        }

    def train(
        self,
        data: pd.DataFrame,
        feature_columns: List[str],
        target_column: Optional[str] = None,
        hyperparameters: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """
        Train the RUL prediction model.

        Args:
            data: Training data with features and target
            feature_columns: List of feature column names
            target_column: Name of target column (RUL in days)
            hyperparameters: Optional custom hyperparameters

        Returns:
            Dictionary containing training metrics (MAE, RMSE, R2, feature importance)

        Raises:
            ValueError: If target_column is not provided or features are missing
        """
        if target_column is None:
            raise ValueError("target_column is required for RUL training")

        self.validate_features(data, feature_columns)

        if target_column not in data.columns:
            raise ValueError(f"Target column '{target_column}' not found in data")

        params = {**self.get_default_hyperparameters(), **(hyperparameters or {})}

        # Extract features and target
        X = data[feature_columns].values
        y = data[target_column].values

        logger.info(
            f"Training Equipment RUL model on {len(X)} samples, "
            f"{len(feature_columns)} features"
        )

        # Store feature columns for later use
        self.feature_columns_ = feature_columns

        # Check for invalid RUL values
        if np.any(y < 0):
            logger.warning(f"Found {np.sum(y < 0)} negative RUL values, clipping to 0")
            y = np.clip(y, 0, None)

        # Split for validation
        X_train, X_val, y_train, y_val = train_test_split(
            X, y, test_size=0.2, random_state=params["random_state"]
        )

        # Fit scaler on training data
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_val_scaled = self.scaler.transform(X_val)

        # Build model with early stopping support
        self.model = GradientBoostingRegressor(
            n_estimators=params["n_estimators"],
            learning_rate=params["learning_rate"],
            max_depth=params["max_depth"],
            min_samples_split=params["min_samples_split"],
            min_samples_leaf=params["min_samples_leaf"],
            max_features=params["max_features"],
            subsample=params["subsample"],
            loss=params["loss"],
            alpha=params["alpha"],
            random_state=params["random_state"],
            validation_fraction=params["validation_fraction"],
            n_iter_no_change=params["n_iter_no_change"],
            tol=params["tol"],
            verbose=0,
        )

        # Train model
        self.model.fit(X_train_scaled, y_train)

        # Compute training metrics
        y_train_pred = self.model.predict(X_train_scaled)
        train_mae = mean_absolute_error(y_train, y_train_pred)
        train_rmse = np.sqrt(mean_squared_error(y_train, y_train_pred))
        train_r2 = r2_score(y_train, y_train_pred)

        # Compute validation metrics
        y_val_pred = self.model.predict(X_val_scaled)
        val_mae = mean_absolute_error(y_val, y_val_pred)
        val_rmse = np.sqrt(mean_squared_error(y_val, y_val_pred))
        val_r2 = r2_score(y_val, y_val_pred)

        # Compute feature importance
        self.feature_importance_ = {
            feature: float(importance)
            for feature, importance in zip(feature_columns, self.model.feature_importances_)
        }

        # Sort by importance
        sorted_importance = sorted(
            self.feature_importance_.items(),
            key=lambda x: x[1],
            reverse=True
        )

        metrics = {
            "algorithm": "gradient_boosting_regressor",
            "training_samples": len(X_train),
            "validation_samples": len(X_val),
            "features": len(feature_columns),
            "train_mae": float(train_mae),
            "train_rmse": float(train_rmse),
            "train_r2": float(train_r2),
            "val_mae": float(val_mae),
            "val_rmse": float(val_rmse),
            "val_r2": float(val_r2),
            "n_estimators_used": int(self.model.n_estimators_),
            "feature_importance": dict(sorted_importance[:10]),  # Top 10
            "mean_rul": float(np.mean(y)),
            "std_rul": float(np.std(y)),
            "min_rul": float(np.min(y)),
            "max_rul": float(np.max(y)),
        }

        self.is_loaded = True
        logger.info(
            f"Training complete - Val MAE: {val_mae:.2f} days, "
            f"Val RMSE: {val_rmse:.2f} days, Val R²: {val_r2:.4f}"
        )

        return metrics

    def predict(self, data: pd.DataFrame, feature_columns: List[str]) -> np.ndarray:
        """
        Predict remaining useful life in days.

        Args:
            data: Input data with features
            feature_columns: List of feature column names

        Returns:
            Array of RUL predictions in days

        Raises:
            ValueError: If model not trained or features missing
        """
        if not self.is_loaded:
            raise ValueError("Model not trained or loaded")

        self.validate_features(data, feature_columns)

        X = data[feature_columns].values
        X_scaled = self.scaler.transform(X)

        predictions = self.model.predict(X_scaled)

        # Ensure non-negative predictions
        predictions = np.clip(predictions, 0, None)

        return predictions

    def predict_with_confidence(
        self,
        data: pd.DataFrame,
        feature_columns: List[str],
        confidence_level: float = 0.95,
    ) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
        """
        Predict RUL with confidence intervals using quantile regression approach.

        Uses the staged predictions from gradient boosting to estimate prediction
        uncertainty. The confidence intervals represent the variability in predictions
        across different boosting stages.

        Args:
            data: Input data with features
            feature_columns: List of feature column names
            confidence_level: Confidence level (default 0.95 for 95% CI)

        Returns:
            Tuple of (predictions, lower_bounds, upper_bounds) in days

        Raises:
            ValueError: If model not trained or features missing
        """
        if not self.is_loaded:
            raise ValueError("Model not trained or loaded")

        self.validate_features(data, feature_columns)

        X = data[feature_columns].values
        X_scaled = self.scaler.transform(X)

        # Get predictions from all boosting stages
        staged_predictions = np.array(list(self.model.staged_predict(X_scaled)))

        # Use last 50 stages or all if less than 50
        n_stages = min(50, staged_predictions.shape[0])
        recent_predictions = staged_predictions[-n_stages:]

        # Compute mean and confidence intervals
        predictions = recent_predictions[-1]  # Most recent prediction

        # Estimate uncertainty from variance across stages
        std_predictions = np.std(recent_predictions, axis=0)

        # Calculate confidence intervals
        # Using t-distribution critical value approximation
        alpha = 1 - confidence_level
        z_score = 1.96 if confidence_level == 0.95 else 2.576  # 95% or 99%

        margin = z_score * std_predictions
        lower_bounds = predictions - margin
        upper_bounds = predictions + margin

        # Ensure non-negative bounds
        lower_bounds = np.clip(lower_bounds, 0, None)
        upper_bounds = np.clip(upper_bounds, 0, None)
        predictions = np.clip(predictions, 0, None)

        return predictions, lower_bounds, upper_bounds

    def get_feature_importance(self) -> Optional[Dict[str, float]]:
        """
        Get feature importance scores.

        Returns:
            Dictionary mapping feature names to importance scores,
            or None if model not trained.
        """
        return self.feature_importance_

    def save_model(self, path: Optional[str] = None) -> str:
        """
        Save model with scaler and metadata.

        Args:
            path: Optional path to save model

        Returns:
            Path where model was saved

        Raises:
            ValueError: If model not trained
        """
        if not self.is_loaded:
            raise ValueError("No model to save - train first")

        save_path = path or self._get_model_path()

        model_data = {
            "model": self.model,
            "scaler": self.scaler,
            "feature_importance": self.feature_importance_,
            "feature_columns": self.feature_columns_,
        }

        import joblib
        from pathlib import Path
        Path(save_path).parent.mkdir(parents=True, exist_ok=True)
        joblib.dump(model_data, save_path)

        self.model_path = save_path
        logger.info(f"Equipment RUL model saved to {save_path}")
        return save_path

    def load_model(self, path: Optional[str] = None) -> None:
        """
        Load model with scaler and metadata.

        Args:
            path: Optional path to load model from

        Raises:
            ValueError: If no path specified
            FileNotFoundError: If model file not found
        """
        load_path = path or self.model_path
        if not load_path:
            raise ValueError("No model path specified")

        import joblib
        from pathlib import Path
        if not Path(load_path).exists():
            raise FileNotFoundError(f"Model file not found: {load_path}")

        model_data = joblib.load(load_path)
        self.model = model_data["model"]
        self.scaler = model_data["scaler"]
        self.feature_importance_ = model_data["feature_importance"]
        self.feature_columns_ = model_data["feature_columns"]
        self.is_loaded = True

        logger.info(f"Equipment RUL model loaded from {load_path}")
