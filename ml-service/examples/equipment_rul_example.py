"""
Example usage of Equipment RUL Engine.

This script demonstrates how to:
1. Generate synthetic equipment degradation data
2. Train an RUL prediction model
3. Make predictions with confidence intervals
4. Analyze feature importance
5. Save and load models
"""
import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

import numpy as np
import pandas as pd
from uuid import uuid4

from app.engines.equipment_rul import EquipmentRULEngine


def generate_equipment_data(n_samples: int = 1000) -> pd.DataFrame:
    """
    Generate synthetic equipment data with degradation patterns.

    Simulates realistic equipment behavior where RUL decreases with:
    - Operating hours
    - Cycle counts
    - Temperature stress
    - Vibration levels
    - Wear accumulation
    """
    np.random.seed(42)

    # Generate features
    operating_hours = np.random.uniform(0, 10000, n_samples)
    cycle_count = np.random.uniform(0, 5000, n_samples)
    temperature = np.random.normal(75, 10, n_samples)
    vibration = np.random.uniform(0.1, 2.0, n_samples)
    wear_indicator = np.random.uniform(0, 100, n_samples)
    pressure = np.random.normal(101, 5, n_samples)

    # Generate RUL with realistic degradation
    base_rul = 365  # 1 year baseline
    rul = (
        base_rul
        - (operating_hours / 50)
        - (cycle_count / 25)
        - (temperature - 75) * 0.5
        - vibration * 10
        - wear_indicator * 2
        - (pressure - 101) * 0.3
        + np.random.normal(0, 10, n_samples)
    )

    # Ensure non-negative RUL
    rul = np.clip(rul, 0, 365)

    return pd.DataFrame({
        "operating_hours": operating_hours,
        "cycle_count": cycle_count,
        "temperature": temperature,
        "vibration": vibration,
        "wear_indicator": wear_indicator,
        "pressure": pressure,
        "rul": rul
    })


def main():
    print("=" * 80)
    print("Equipment RUL Prediction Engine - Example")
    print("=" * 80)

    # Step 1: Generate training data
    print("\n1. Generating synthetic equipment data...")
    data = generate_equipment_data(n_samples=2000)
    print(f"   Generated {len(data)} samples")
    print(f"   RUL statistics: mean={data['rul'].mean():.1f}, "
          f"std={data['rul'].std():.1f}, "
          f"min={data['rul'].min():.1f}, "
          f"max={data['rul'].max():.1f}")

    # Step 2: Initialize engine
    print("\n2. Initializing RUL engine...")
    engine = EquipmentRULEngine(model_id=uuid4())

    # Step 3: Define features and target
    feature_columns = [
        "operating_hours",
        "cycle_count",
        "temperature",
        "vibration",
        "wear_indicator",
        "pressure"
    ]
    target_column = "rul"

    # Step 4: Train model
    print(f"\n3. Training model with {len(feature_columns)} features...")
    print(f"   Features: {', '.join(feature_columns)}")

    metrics = engine.train(
        data=data,
        feature_columns=feature_columns,
        target_column=target_column
    )

    print("\n   Training Results:")
    print(f"   - Algorithm: {metrics['algorithm']}")
    print(f"   - Training samples: {metrics['training_samples']}")
    print(f"   - Validation samples: {metrics['validation_samples']}")
    print(f"\n   Training Metrics:")
    print(f"   - MAE: {metrics['train_mae']:.2f} days")
    print(f"   - RMSE: {metrics['train_rmse']:.2f} days")
    print(f"   - R²: {metrics['train_r2']:.4f}")
    print(f"\n   Validation Metrics:")
    print(f"   - MAE: {metrics['val_mae']:.2f} days")
    print(f"   - RMSE: {metrics['val_rmse']:.2f} days")
    print(f"   - R²: {metrics['val_r2']:.4f}")
    print(f"\n   Model Configuration:")
    print(f"   - Estimators used: {metrics['n_estimators_used']}")

    # Step 5: Feature importance
    print("\n4. Analyzing feature importance...")
    importance = engine.get_feature_importance()
    sorted_features = sorted(importance.items(), key=lambda x: x[1], reverse=True)

    print("\n   Top Features (by importance):")
    for i, (feature, score) in enumerate(sorted_features, 1):
        print(f"   {i}. {feature:20s} {score:.4f} ({score*100:.1f}%)")

    # Step 6: Make predictions
    print("\n5. Making predictions on test samples...")

    # Create test scenarios
    test_scenarios = pd.DataFrame({
        "scenario": ["New Equipment", "Mid-Life", "Near Failure", "High Stress"],
        "operating_hours": [100, 5000, 9500, 3000],
        "cycle_count": [50, 2500, 4800, 3500],
        "temperature": [70, 75, 85, 95],
        "vibration": [0.2, 0.5, 1.8, 1.5],
        "wear_indicator": [5, 50, 95, 70],
        "pressure": [101, 101, 105, 110],
    })

    predictions = engine.predict(
        test_scenarios[feature_columns],
        feature_columns
    )

    print("\n   Predictions:")
    for i, row in test_scenarios.iterrows():
        print(f"   {row['scenario']:15s}: {predictions[i]:6.1f} days")

    # Step 7: Predictions with confidence intervals
    print("\n6. Making predictions with 95% confidence intervals...")

    pred, lower, upper = engine.predict_with_confidence(
        test_scenarios[feature_columns],
        feature_columns,
        confidence_level=0.95
    )

    print("\n   Predictions with Confidence Intervals:")
    print(f"   {'Scenario':<15s} {'Prediction':>10s} {'95% CI':>20s} {'Width':>10s}")
    print("   " + "-" * 60)
    for i, row in test_scenarios.iterrows():
        ci_width = upper[i] - lower[i]
        print(f"   {row['scenario']:15s} {pred[i]:10.1f} "
              f"[{lower[i]:6.1f}, {upper[i]:6.1f}] {ci_width:10.1f}")

    # Step 8: Save model
    print("\n7. Saving trained model...")
    model_path = "/tmp/equipment_rul_model.joblib"
    saved_path = engine.save_model(model_path)
    print(f"   Model saved to: {saved_path}")

    # Step 9: Load model
    print("\n8. Loading saved model...")
    new_engine = EquipmentRULEngine(model_id=uuid4())
    new_engine.load_model(model_path)
    print("   Model loaded successfully")

    # Verify loaded model works
    test_pred = new_engine.predict(
        test_scenarios[feature_columns].iloc[[0]],
        feature_columns
    )
    print(f"   Verification prediction: {test_pred[0]:.1f} days")

    print("\n" + "=" * 80)
    print("Example completed successfully!")
    print("=" * 80)


if __name__ == "__main__":
    main()
