"""
Predictive Maintenance Engine using Random Forest Classifier.

Detects equipment failure 24-48 hours in advance using time-series features
including rolling statistics, trend indicators, and statistical moments.
"""
import logging
from typing import Any, Dict, List, Optional, Tuple
from uuid import UUID

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix
from sklearn.preprocessing import StandardScaler

from app.engines.base import BaseMLEngine

logger = logging.getLogger(__name__)


class PredictiveMaintenanceEngine(BaseMLEngine):
    """
    Predictive Maintenance using Random Forest Classifier.

    Predicts equipment failure probability 24-48 hours in advance based on
    telemetry patterns, rolling statistics, and trend analysis.

    Features are automatically engineered from raw telemetry:
    - Rolling mean, std, max, min over configurable windows
    - Trend indicators (slopes, differences)
    - Statistical moments (skewness, kurtosis)
    - Rate of change metrics
    """

    def __init__(self, model_id: UUID, model_path: Optional[str] = None):
        super().__init__(model_id, model_path)
        self.scaler = StandardScaler()
        self.feature_importance_: Optional[Dict[str, float]] = None
        self.engineered_feature_names_: List[str] = []
        self.failure_probability_threshold: float = 0.5

    def get_default_hyperparameters(self) -> Dict[str, Any]:
        """
        Return default hyperparameters for Random Forest Classifier.

        Returns:
            Dictionary with hyperparameters:
            - n_estimators: Number of trees in the forest
            - max_depth: Maximum depth of trees (None = unlimited)
            - min_samples_split: Minimum samples required to split
            - min_samples_leaf: Minimum samples required at leaf node
            - max_features: Number of features for best split
            - window_sizes: List of rolling window sizes (in records)
            - threshold: Failure probability threshold (0-1)
            - random_state: Random seed for reproducibility
        """
        return {
            "n_estimators": 100,
            "max_depth": 10,
            "min_samples_split": 5,
            "min_samples_leaf": 2,
            "max_features": "sqrt",
            "window_sizes": [5, 10, 20],  # Rolling windows for feature engineering
            "threshold": 0.5,  # Failure probability threshold
            "random_state": 42,
        }

    def _engineer_features(
        self,
        data: pd.DataFrame,
        feature_columns: List[str],
        window_sizes: List[int],
    ) -> pd.DataFrame:
        """
        Engineer time-series features from raw telemetry.

        Creates rolling statistics, trend indicators, and statistical moments
        to capture temporal patterns indicative of equipment degradation.

        Args:
            data: DataFrame with telemetry data (must be time-ordered)
            feature_columns: Raw feature column names
            window_sizes: Window sizes for rolling statistics

        Returns:
            DataFrame with engineered features
        """
        engineered_features = pd.DataFrame(index=data.index)

        for col in feature_columns:
            if col not in data.columns:
                continue

            values = data[col]

            # Basic statistics per window
            for window in window_sizes:
                # Rolling statistics
                engineered_features[f"{col}_mean_{window}"] = values.rolling(
                    window=window, min_periods=1
                ).mean()
                engineered_features[f"{col}_std_{window}"] = values.rolling(
                    window=window, min_periods=1
                ).std().fillna(0)
                engineered_features[f"{col}_max_{window}"] = values.rolling(
                    window=window, min_periods=1
                ).max()
                engineered_features[f"{col}_min_{window}"] = values.rolling(
                    window=window, min_periods=1
                ).min()

                # Range (max - min)
                engineered_features[f"{col}_range_{window}"] = (
                    engineered_features[f"{col}_max_{window}"]
                    - engineered_features[f"{col}_min_{window}"]
                )

            # Trend indicators (rate of change)
            engineered_features[f"{col}_diff_1"] = values.diff(1).fillna(0)
            engineered_features[f"{col}_diff_5"] = values.diff(5).fillna(0)

            # Slope over small window (linear trend)
            for window in [5, 10]:
                def compute_slope(series):
                    if len(series) < 2:
                        return 0
                    x = np.arange(len(series))
                    coeffs = np.polyfit(x, series, 1)
                    return coeffs[0]

                engineered_features[f"{col}_slope_{window}"] = values.rolling(
                    window=window, min_periods=2
                ).apply(compute_slope, raw=True).fillna(0)

            # Statistical moments (for larger windows only)
            for window in [10, 20]:
                if window in window_sizes:
                    # Skewness (asymmetry of distribution)
                    engineered_features[f"{col}_skew_{window}"] = values.rolling(
                        window=window, min_periods=3
                    ).skew().fillna(0)

                    # Kurtosis (tail heaviness)
                    engineered_features[f"{col}_kurt_{window}"] = values.rolling(
                        window=window, min_periods=4
                    ).kurt().fillna(0)

        # Replace any remaining NaN or inf values
        engineered_features = engineered_features.replace([np.inf, -np.inf], 0)
        engineered_features = engineered_features.fillna(0)

        return engineered_features

    def train(
        self,
        data: pd.DataFrame,
        feature_columns: List[str],
        target_column: Optional[str] = None,
        hyperparameters: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """
        Train the predictive maintenance model.

        Args:
            data: Training data with telemetry and failure labels
            feature_columns: List of raw telemetry variable names
            target_column: Column containing failure labels (0=normal, 1=failure)
            hyperparameters: Optional hyperparameter overrides

        Returns:
            Dictionary containing training metrics:
            - training_samples: Number of samples used
            - features: Number of engineered features
            - accuracy: Overall accuracy
            - precision: Precision score
            - recall: Recall score (most critical for maintenance)
            - f1_score: F1 score
            - failure_rate: Proportion of failure cases
            - feature_importance_top_10: Top 10 most important features
        """
        if target_column is None:
            raise ValueError("target_column is required for predictive maintenance")

        self.validate_features(data, feature_columns + [target_column])
        params = {**self.get_default_hyperparameters(), **(hyperparameters or {})}

        # Store threshold
        self.failure_probability_threshold = params["threshold"]

        # Engineer features from raw telemetry
        logger.info(f"Engineering features from {len(feature_columns)} raw variables")
        X_engineered = self._engineer_features(
            data, feature_columns, params["window_sizes"]
        )
        self.engineered_feature_names_ = list(X_engineered.columns)

        y = data[target_column].values

        logger.info(
            f"Training Random Forest on {len(X_engineered)} samples, "
            f"{len(self.engineered_feature_names_)} engineered features"
        )

        # Fit scaler on engineered features
        X_scaled = self.scaler.fit_transform(X_engineered.values)

        # Train Random Forest Classifier
        self.model = RandomForestClassifier(
            n_estimators=params["n_estimators"],
            max_depth=params["max_depth"],
            min_samples_split=params["min_samples_split"],
            min_samples_leaf=params["min_samples_leaf"],
            max_features=params["max_features"],
            random_state=params["random_state"],
            class_weight="balanced",  # Handle imbalanced datasets
            n_jobs=-1,  # Use all CPU cores
        )
        self.model.fit(X_scaled, y)

        # Compute training metrics
        y_pred = self.model.predict(X_scaled)
        proba = self.model.predict_proba(X_scaled)
        # Handle single-class case (when training data has only 0s or only 1s)
        if proba.shape[1] == 1:
            # If only one class, probabilities are either all 0 or all 1
            y_proba = np.zeros(len(y)) if self.model.classes_[0] == 0 else np.ones(len(y))
        else:
            y_proba = proba[:, 1]

        # Classification metrics
        accuracy = accuracy_score(y, y_pred)
        precision = precision_score(y, y_pred, zero_division=0)
        recall = recall_score(y, y_pred, zero_division=0)
        f1 = f1_score(y, y_pred, zero_division=0)

        # Confusion matrix
        cm = confusion_matrix(y, y_pred)
        tn, fp, fn, tp = cm.ravel() if cm.size == 4 else (0, 0, 0, 0)

        # Feature importance
        importances = self.model.feature_importances_
        self.feature_importance_ = {
            name: float(importance)
            for name, importance in zip(self.engineered_feature_names_, importances)
        }

        # Get top 10 features
        top_features = sorted(
            self.feature_importance_.items(), key=lambda x: x[1], reverse=True
        )[:10]

        metrics = {
            "algorithm": "random_forest",
            "training_samples": len(X_engineered),
            "features": len(self.engineered_feature_names_),
            "raw_features": len(feature_columns),
            "accuracy": float(accuracy),
            "precision": float(precision),
            "recall": float(recall),
            "f1_score": float(f1),
            "failure_rate": float(y.mean()),
            "true_positives": int(tp),
            "true_negatives": int(tn),
            "false_positives": int(fp),
            "false_negatives": int(fn),
            "mean_failure_probability": float(y_proba.mean()),
            "threshold": self.failure_probability_threshold,
            "feature_importance_top_10": dict(top_features),
        }

        self.is_loaded = True
        logger.info(f"Training complete: accuracy={accuracy:.3f}, recall={recall:.3f}, f1={f1:.3f}")
        return metrics

    def predict(self, data: pd.DataFrame, feature_columns: List[str]) -> np.ndarray:
        """
        Predict failure labels (0=normal, 1=failure imminent).

        Args:
            data: Telemetry data for prediction
            feature_columns: Raw telemetry variable names

        Returns:
            Array of binary predictions (0 or 1)
        """
        if not self.is_loaded:
            raise ValueError("Model not trained or loaded")

        self.validate_features(data, feature_columns)

        # Engineer features using same window sizes
        params = self.get_default_hyperparameters()
        X_engineered = self._engineer_features(
            data, feature_columns, params["window_sizes"]
        )

        # Ensure feature alignment
        if list(X_engineered.columns) != self.engineered_feature_names_:
            # Reorder or fill missing features
            X_aligned = pd.DataFrame(
                0, index=X_engineered.index, columns=self.engineered_feature_names_
            )
            for col in X_engineered.columns:
                if col in X_aligned.columns:
                    X_aligned[col] = X_engineered[col]
            X_engineered = X_aligned

        X_scaled = self.scaler.transform(X_engineered.values)
        return self.model.predict(X_scaled)

    def predict_with_probability(
        self, data: pd.DataFrame, feature_columns: List[str]
    ) -> Tuple[np.ndarray, np.ndarray, List[Dict[str, Any]]]:
        """
        Predict failure with probability scores and time-to-failure estimates.

        Args:
            data: Telemetry data for prediction
            feature_columns: Raw telemetry variable names

        Returns:
            Tuple of (labels, probabilities, details):
            - labels: Binary predictions (0 or 1)
            - probabilities: Failure probability scores (0-1)
            - details: List of dicts with per-prediction details:
                - failure_probability: 0-1 probability
                - days_to_failure: Estimated days (24-48 hour window)
                - risk_level: LOW/MEDIUM/HIGH/CRITICAL
                - top_risk_factors: Features contributing to failure prediction
        """
        if not self.is_loaded:
            raise ValueError("Model not trained or loaded")

        self.validate_features(data, feature_columns)

        # Engineer features
        params = self.get_default_hyperparameters()
        X_engineered = self._engineer_features(
            data, feature_columns, params["window_sizes"]
        )

        # Ensure feature alignment
        if list(X_engineered.columns) != self.engineered_feature_names_:
            X_aligned = pd.DataFrame(
                0, index=X_engineered.index, columns=self.engineered_feature_names_
            )
            for col in X_engineered.columns:
                if col in X_aligned.columns:
                    X_aligned[col] = X_engineered[col]
            X_engineered = X_aligned

        X_scaled = self.scaler.transform(X_engineered.values)

        # Get predictions and probabilities
        labels = self.model.predict(X_scaled)
        probabilities = self.model.predict_proba(X_scaled)[:, 1]

        # Build detailed results
        details = []
        for i in range(len(labels)):
            probability = float(probabilities[i])

            # Estimate days to failure (1-2 days based on probability)
            # Higher probability = sooner failure
            if probability >= self.failure_probability_threshold:
                days_to_failure = max(1, int(2 - probability))  # 1-2 days
            else:
                days_to_failure = None

            # Determine risk level
            if probability < 0.3:
                risk_level = "LOW"
            elif probability < 0.5:
                risk_level = "MEDIUM"
            elif probability < 0.75:
                risk_level = "HIGH"
            else:
                risk_level = "CRITICAL"

            # Identify top risk factors using feature importance
            # Get feature values for this sample
            sample_features = X_engineered.iloc[i]
            risk_factors = {}

            if self.feature_importance_ and probability >= 0.3:
                # Get top features by importance
                top_features = sorted(
                    self.feature_importance_.items(), key=lambda x: x[1], reverse=True
                )[:5]

                for feature_name, importance in top_features:
                    if feature_name in sample_features.index:
                        # Normalized contribution (importance * abs(scaled value))
                        feature_idx = self.engineered_feature_names_.index(feature_name)
                        scaled_value = abs(X_scaled[i, feature_idx])
                        contribution = importance * scaled_value
                        risk_factors[feature_name] = float(contribution)

            details.append(
                {
                    "failure_probability": probability,
                    "days_to_failure": days_to_failure,
                    "risk_level": risk_level,
                    "top_risk_factors": risk_factors,
                }
            )

        return labels, probabilities, details

    def save_model(self, path: Optional[str] = None) -> str:
        """
        Save model with scaler, feature names, and metadata.

        Args:
            path: Optional custom save path

        Returns:
            Path where model was saved
        """
        if not self.is_loaded:
            raise ValueError("No model to save - train first")

        save_path = path or self._get_model_path()

        model_data = {
            "model": self.model,
            "scaler": self.scaler,
            "engineered_feature_names": self.engineered_feature_names_,
            "feature_importance": self.feature_importance_,
            "failure_probability_threshold": self.failure_probability_threshold,
        }

        import joblib
        from pathlib import Path

        Path(save_path).parent.mkdir(parents=True, exist_ok=True)
        joblib.dump(model_data, save_path)

        self.model_path = save_path
        logger.info(f"Predictive maintenance model saved to {save_path}")
        return save_path

    def load_model(self, path: Optional[str] = None) -> None:
        """
        Load model with scaler, feature names, and metadata.

        Args:
            path: Optional custom load path
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
        self.engineered_feature_names_ = model_data["engineered_feature_names"]
        self.feature_importance_ = model_data["feature_importance"]
        self.failure_probability_threshold = model_data["failure_probability_threshold"]
        self.is_loaded = True

        logger.info(f"Predictive maintenance model loaded from {load_path}")
