"""
Example: Training and using the Predictive Maintenance Engine.

This script demonstrates:
1. Creating synthetic equipment telemetry with failure patterns
2. Training a Random Forest classifier for failure prediction
3. Making predictions with probability scores
4. Saving and loading the model
"""
import numpy as np
import pandas as pd
from uuid import uuid4
from datetime import datetime, timedelta

from app.engines.predictive_maintenance import PredictiveMaintenanceEngine


def generate_synthetic_equipment_data(n_normal=800, n_failure=200):
    """
    Generate synthetic equipment telemetry with realistic failure patterns.

    Normal operation:
    - Temperature: ~70°C ± 5°C
    - Vibration: ~0.5 mm/s ± 0.1 mm/s
    - Pressure: ~100 kPa ± 3 kPa

    Pre-failure patterns:
    - Temperature: increasing trend to 85-95°C
    - Vibration: elevated to 1.2-1.5 mm/s
    - Pressure: unstable 95 ± 8 kPa
    """
    np.random.seed(42)

    # Normal operation samples
    normal_temp = np.random.normal(70, 5, n_normal)
    normal_vibration = np.random.normal(0.5, 0.1, n_normal)
    normal_pressure = np.random.normal(100, 3, n_normal)

    # Pre-failure samples (equipment degradation)
    failure_temp = np.random.normal(85, 8, n_failure) + np.linspace(0, 10, n_failure)
    failure_vibration = np.random.normal(1.2, 0.3, n_failure)
    failure_pressure = np.random.normal(95, 8, n_failure)

    # Combine and shuffle
    temperature = np.concatenate([normal_temp, failure_temp])
    vibration = np.concatenate([normal_vibration, failure_vibration])
    pressure = np.concatenate([normal_pressure, failure_pressure])
    labels = np.concatenate([np.zeros(n_normal), np.ones(n_failure)])

    indices = np.random.permutation(len(labels))

    return pd.DataFrame({
        "temperature": temperature[indices],
        "vibration": vibration[indices],
        "pressure": pressure[indices],
        "failure": labels[indices],
    })


def main():
    """Run predictive maintenance example."""

    print("=" * 80)
    print("Predictive Maintenance Engine - Example")
    print("=" * 80)

    # Step 1: Generate training data
    print("\n1. Generating synthetic equipment telemetry...")
    training_data = generate_synthetic_equipment_data(n_normal=800, n_failure=200)

    print(f"   Training samples: {len(training_data)}")
    print(f"   Failure rate: {training_data['failure'].mean():.1%}")
    print(f"   Features: temperature, vibration, pressure")

    # Step 2: Initialize and train the engine
    print("\n2. Training Random Forest Classifier...")
    engine = PredictiveMaintenanceEngine(model_id=uuid4())

    feature_columns = ["temperature", "vibration", "pressure"]
    metrics = engine.train(
        data=training_data,
        feature_columns=feature_columns,
        target_column="failure",
        hyperparameters={
            "n_estimators": 100,
            "max_depth": 10,
            "window_sizes": [5, 10, 20],
            "threshold": 0.5,
        }
    )

    print("\n   Training Metrics:")
    print(f"   - Accuracy:  {metrics['accuracy']:.3f}")
    print(f"   - Precision: {metrics['precision']:.3f}")
    print(f"   - Recall:    {metrics['recall']:.3f}")
    print(f"   - F1 Score:  {metrics['f1_score']:.3f}")
    print(f"   - Engineered features: {metrics['features']}")

    print("\n   Top Risk Indicators:")
    for feature, importance in list(metrics['feature_importance_top_10'].items())[:5]:
        print(f"   - {feature}: {importance:.4f}")

    # Step 3: Make predictions on normal operation
    print("\n3. Testing on normal operation data...")
    normal_data = pd.DataFrame({
        "temperature": np.random.normal(70, 5, 20),
        "vibration": np.random.normal(0.5, 0.1, 20),
        "pressure": np.random.normal(100, 3, 20),
    })

    labels, probabilities, details = engine.predict_with_probability(
        normal_data, feature_columns
    )

    avg_prob = probabilities.mean()
    failures_detected = np.sum(labels == 1)

    print(f"   Samples analyzed: {len(normal_data)}")
    print(f"   Average failure probability: {avg_prob:.2%}")
    print(f"   Failures predicted: {failures_detected}")

    # Step 4: Test on degraded equipment
    print("\n4. Testing on degraded equipment data...")
    degraded_data = pd.DataFrame({
        "temperature": np.random.normal(85, 8, 20) + np.linspace(0, 10, 20),
        "vibration": np.random.normal(1.2, 0.3, 20),
        "pressure": np.random.normal(95, 8, 20),
    })

    labels, probabilities, details = engine.predict_with_probability(
        degraded_data, feature_columns
    )

    print(f"   Samples analyzed: {len(degraded_data)}")
    print(f"   Average failure probability: {probabilities.mean():.2%}")
    print(f"   Failures predicted: {np.sum(labels == 1)}")

    # Show detailed predictions for high-risk cases
    high_risk_indices = np.where(probabilities >= 0.5)[0]
    if len(high_risk_indices) > 0:
        print(f"\n   High-risk predictions ({len(high_risk_indices)} samples):")
        for i in high_risk_indices[:3]:  # Show first 3
            detail = details[i]
            print(f"\n   Sample {i}:")
            print(f"   - Failure Probability: {detail['failure_probability']:.1%}")
            print(f"   - Risk Level: {detail['risk_level']}")
            print(f"   - Estimated Days to Failure: {detail['days_to_failure']}")
            if detail['top_risk_factors']:
                print(f"   - Top Risk Factors:")
                for factor, score in list(detail['top_risk_factors'].items())[:3]:
                    print(f"     • {factor}: {score:.4f}")

    # Step 5: Save and reload model
    print("\n5. Saving and reloading model...")
    model_path = "/tmp/predictive_maintenance_model.joblib"
    engine.save_model(model_path)
    print(f"   Model saved to: {model_path}")

    # Create new engine and load
    new_engine = PredictiveMaintenanceEngine(model_id=uuid4())
    new_engine.load_model(model_path)
    print(f"   Model loaded successfully")

    # Verify predictions are identical
    new_labels = new_engine.predict(degraded_data, feature_columns)
    identical = np.array_equal(labels, new_labels)
    print(f"   Predictions match original: {identical}")

    print("\n" + "=" * 80)
    print("Example completed successfully!")
    print("=" * 80)


if __name__ == "__main__":
    main()
