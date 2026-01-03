"""
Base ML engine interface.
"""
import logging
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any, Dict, List, Optional
from uuid import UUID

import joblib
import numpy as np
import pandas as pd

from app.core.config import settings

logger = logging.getLogger(__name__)


class BaseMLEngine(ABC):
    """Abstract base class for ML engines."""

    def __init__(self, model_id: UUID, model_path: Optional[str] = None):
        self.model_id = model_id
        self.model_path = model_path
        self.model = None
        self.is_loaded = False

    @abstractmethod
    def train(
        self,
        data: pd.DataFrame,
        feature_columns: List[str],
        target_column: Optional[str] = None,
        hyperparameters: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """
        Train the model on the provided data.

        Returns:
            Dictionary containing training metrics.
        """
        pass

    @abstractmethod
    def predict(self, data: pd.DataFrame, feature_columns: List[str]) -> np.ndarray:
        """
        Make predictions on the provided data.

        Returns:
            Array of predictions.
        """
        pass

    @abstractmethod
    def get_default_hyperparameters(self) -> Dict[str, Any]:
        """Return default hyperparameters for this engine."""
        pass

    def save_model(self, path: Optional[str] = None) -> str:
        """Save the trained model to disk."""
        if self.model is None:
            raise ValueError("No model to save - train first")

        save_path = path or self._get_model_path()
        Path(save_path).parent.mkdir(parents=True, exist_ok=True)

        joblib.dump(self.model, save_path)
        logger.info(f"Model saved to {save_path}")

        self.model_path = save_path
        return save_path

    def load_model(self, path: Optional[str] = None) -> None:
        """Load a trained model from disk."""
        load_path = path or self.model_path
        if not load_path:
            raise ValueError("No model path specified")

        if not Path(load_path).exists():
            raise FileNotFoundError(f"Model file not found: {load_path}")

        self.model = joblib.load(load_path)
        self.is_loaded = True
        logger.info(f"Model loaded from {load_path}")

    def _get_model_path(self) -> str:
        """Generate default model path."""
        return f"{settings.MODEL_STORAGE_PATH}/{self.model_id}.joblib"

    def validate_features(self, data: pd.DataFrame, feature_columns: List[str]) -> None:
        """Validate that all required features are present."""
        missing = set(feature_columns) - set(data.columns)
        if missing:
            raise ValueError(f"Missing features: {missing}")

    def preprocess_data(self, data: pd.DataFrame, feature_columns: List[str]) -> np.ndarray:
        """Basic preprocessing - extract and validate features."""
        self.validate_features(data, feature_columns)
        return data[feature_columns].values
