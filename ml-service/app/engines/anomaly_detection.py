"""
Anomaly Detection Engine using Isolation Forest and Z-Score methods.
"""
import logging
from typing import Any, Dict, List, Optional, Tuple
from uuid import UUID

import numpy as np
import pandas as pd
from scipy import stats
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

from app.engines.base import BaseMLEngine
from app.models.schemas import AnomalySeverity

logger = logging.getLogger(__name__)


class AnomalyDetectionEngine(BaseMLEngine):
    """
    Anomaly detection using Isolation Forest with Z-Score fallback.

    Supports two modes:
    - isolation_forest: ML-based anomaly detection
    - z_score: Statistical anomaly detection based on standard deviations
    """

    def __init__(
        self,
        model_id: UUID,
        algorithm: str = "isolation_forest",
        model_path: Optional[str] = None,
    ):
        super().__init__(model_id, model_path)
        self.algorithm = algorithm
        self.scaler = StandardScaler()
        self.feature_stats: Dict[str, Dict[str, float]] = {}

    def get_default_hyperparameters(self) -> Dict[str, Any]:
        """Return default hyperparameters."""
        if self.algorithm == "isolation_forest":
            return {
                "n_estimators": 100,
                "contamination": 0.1,
                "max_samples": "auto",
                "max_features": 1.0,
                "random_state": 42,
            }
        else:  # z_score
            return {
                "threshold": 3.0,  # Standard deviations
            }

    def train(
        self,
        data: pd.DataFrame,
        feature_columns: List[str],
        target_column: Optional[str] = None,
        hyperparameters: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """
        Train the anomaly detection model.

        For Isolation Forest: Learns normal patterns from data.
        For Z-Score: Computes feature statistics (mean, std).
        """
        self.validate_features(data, feature_columns)
        params = {**self.get_default_hyperparameters(), **(hyperparameters or {})}

        X = data[feature_columns].values
        logger.info(f"Training {self.algorithm} on {len(X)} samples, {len(feature_columns)} features")

        # Fit scaler
        X_scaled = self.scaler.fit_transform(X)

        # Compute feature statistics for both methods
        for i, col in enumerate(feature_columns):
            self.feature_stats[col] = {
                "mean": float(np.mean(X[:, i])),
                "std": float(np.std(X[:, i])),
                "min": float(np.min(X[:, i])),
                "max": float(np.max(X[:, i])),
            }

        if self.algorithm == "isolation_forest":
            self.model = IsolationForest(
                n_estimators=params["n_estimators"],
                contamination=params["contamination"],
                max_samples=params["max_samples"],
                max_features=params["max_features"],
                random_state=params["random_state"],
            )
            self.model.fit(X_scaled)

            # Compute training metrics
            scores = self.model.decision_function(X_scaled)
            predictions = self.model.predict(X_scaled)
            anomaly_ratio = np.sum(predictions == -1) / len(predictions)

            metrics = {
                "algorithm": "isolation_forest",
                "training_samples": len(X),
                "features": len(feature_columns),
                "anomaly_ratio": float(anomaly_ratio),
                "mean_score": float(np.mean(scores)),
                "std_score": float(np.std(scores)),
            }
        else:
            # Z-score doesn't need a trained model, just statistics
            self.model = {"type": "z_score", "threshold": params["threshold"]}
            metrics = {
                "algorithm": "z_score",
                "training_samples": len(X),
                "features": len(feature_columns),
                "threshold": params["threshold"],
            }

        self.is_loaded = True
        logger.info(f"Training complete: {metrics}")
        return metrics

    def predict(self, data: pd.DataFrame, feature_columns: List[str]) -> np.ndarray:
        """
        Predict anomaly labels.

        Returns:
            Array of 1 (normal) or -1 (anomaly).
        """
        if not self.is_loaded:
            raise ValueError("Model not trained or loaded")

        self.validate_features(data, feature_columns)
        X = data[feature_columns].values
        X_scaled = self.scaler.transform(X)

        if self.algorithm == "isolation_forest":
            return self.model.predict(X_scaled)
        else:
            # Z-score based detection
            z_scores = np.abs(X_scaled)
            max_z = np.max(z_scores, axis=1)
            threshold = self.model["threshold"]
            return np.where(max_z > threshold, -1, 1)

    def predict_with_scores(
        self,
        data: pd.DataFrame,
        feature_columns: List[str],
    ) -> Tuple[np.ndarray, np.ndarray, List[Dict[str, Any]]]:
        """
        Predict anomalies with detailed scores and affected variables.

        Returns:
            Tuple of (labels, scores, details per sample).
        """
        if not self.is_loaded:
            raise ValueError("Model not trained or loaded")

        self.validate_features(data, feature_columns)
        X = data[feature_columns].values
        X_scaled = self.scaler.transform(X)

        if self.algorithm == "isolation_forest":
            labels = self.model.predict(X_scaled)
            # Isolation Forest scores: lower = more anomalous
            # We normalize to 0-1 where 1 = most anomalous
            raw_scores = -self.model.decision_function(X_scaled)
            scores = (raw_scores - raw_scores.min()) / (raw_scores.max() - raw_scores.min() + 1e-10)
        else:
            z_scores = np.abs(X_scaled)
            max_z = np.max(z_scores, axis=1)
            threshold = self.model["threshold"]
            labels = np.where(max_z > threshold, -1, 1)
            # Normalize z-scores to 0-1
            scores = np.clip(max_z / (threshold * 2), 0, 1)

        # Build details for each sample
        details = []
        for i in range(len(X)):
            sample_z = np.abs(X_scaled[i])
            affected = []
            expected = {}
            actual = {}

            for j, col in enumerate(feature_columns):
                if sample_z[j] > 2.0:  # Notable deviation
                    affected.append(col)
                    expected[col] = self.feature_stats[col]["mean"]
                    actual[col] = float(X[i, j])

            details.append({
                "affected_variables": affected,
                "expected_values": expected,
                "actual_values": actual,
            })

        return labels, scores, details

    def get_severity(self, score: float, threshold: float = 0.5) -> AnomalySeverity:
        """Determine anomaly severity based on score."""
        if score < threshold:
            return AnomalySeverity.LOW
        elif score < threshold + 0.2:
            return AnomalySeverity.MEDIUM
        elif score < threshold + 0.35:
            return AnomalySeverity.HIGH
        else:
            return AnomalySeverity.CRITICAL

    def save_model(self, path: Optional[str] = None) -> str:
        """Save model with scaler and statistics."""
        if not self.is_loaded:
            raise ValueError("No model to save - train first")

        save_path = path or self._get_model_path()

        model_data = {
            "algorithm": self.algorithm,
            "model": self.model,
            "scaler": self.scaler,
            "feature_stats": self.feature_stats,
        }

        import joblib
        from pathlib import Path
        Path(save_path).parent.mkdir(parents=True, exist_ok=True)
        joblib.dump(model_data, save_path)

        self.model_path = save_path
        logger.info(f"Anomaly detection model saved to {save_path}")
        return save_path

    def load_model(self, path: Optional[str] = None) -> None:
        """Load model with scaler and statistics."""
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
        self.feature_stats = model_data["feature_stats"]
        self.is_loaded = True

        logger.info(f"Anomaly detection model loaded from {load_path}")
