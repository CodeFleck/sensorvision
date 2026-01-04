"""
Quick validation script for EnergyForecastingEngine.
Run this to validate the implementation without full pytest setup.
"""
import sys
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from uuid import uuid4

# Add app to path
sys.path.insert(0, 'app')

from engines.energy_forecasting import EnergyForecastingEngine

def generate_test_data():
    """Generate synthetic hourly energy consumption data."""
    np.random.seed(42)
    n_hours = 30 * 24  # 30 days
    start_time = datetime(2024, 1, 1, 0, 0, 0)

    timestamps = [start_time + timedelta(hours=i) for i in range(n_hours)]

    consumption = []
    for ts in timestamps:
        base = 100.0
        hour = ts.hour
        daily_pattern = 30 * np.sin((hour - 6) * np.pi / 12)
        is_weekend = ts.weekday() >= 5
        weekly_pattern = -20 if is_weekend else 0
        noise = np.random.normal(0, 5)
        consumption.append(base + daily_pattern + weekly_pattern + noise)

    return pd.DataFrame({
        "timestamp": timestamps,
        "consumption": consumption
    })

def main():
    print("=" * 60)
    print("Energy Forecasting Engine Validation")
    print("=" * 60)

    # Create engine
    print("\n1. Creating engine...")
    engine = EnergyForecastingEngine(model_id=uuid4(), algorithm="auto")
    print(f"   Algorithm: {engine.algorithm}")
    print(f"   Is loaded: {engine.is_loaded}")

    # Generate data
    print("\n2. Generating training data...")
    data = generate_test_data()
    print(f"   Generated {len(data)} hourly samples")
    print(f"   Date range: {data['timestamp'].min()} to {data['timestamp'].max()}")

    # Train model
    print("\n3. Training model...")
    metrics = engine.train(
        data,
        feature_columns=["consumption"],
        target_column="consumption"
    )
    print(f"   MAE: {metrics['mae']:.2f}")
    print(f"   RMSE: {metrics['rmse']:.2f}")
    print(f"   MAPE: {metrics['mape']:.2f}%")
    print(f"   Training samples: {metrics['training_samples']}")
    print(f"   Features: {metrics['features']}")

    # Test prediction
    print("\n4. Testing prediction...")
    df_engineered = engine.engineer_features(data.tail(100), "consumption")
    predictions = engine.predict(df_engineered, engine.feature_names)
    print(f"   Predictions shape: {predictions.shape}")
    print(f"   Predictions range: [{predictions.min():.2f}, {predictions.max():.2f}]")

    # Test 7-day forecast
    print("\n5. Testing 7-day forecast (168 hours)...")
    forecast_df, forecast_predictions = engine.forecast(
        data.tail(200),
        target_column="consumption",
        periods=168
    )
    print(f"   Forecast length: {len(forecast_predictions)}")
    print(f"   Forecast range: [{forecast_predictions.min():.2f}, {forecast_predictions.max():.2f}]")
    print(f"   First timestamp: {forecast_df['timestamp'].iloc[0]}")
    print(f"   Last timestamp: {forecast_df['timestamp'].iloc[-1]}")

    # Test model persistence
    print("\n6. Testing model save/load...")
    import tempfile
    import os
    with tempfile.TemporaryDirectory() as tmpdir:
        model_path = os.path.join(tmpdir, "test_model.joblib")
        saved_path = engine.save_model(model_path)
        print(f"   Saved to: {saved_path}")

        new_engine = EnergyForecastingEngine(model_id=uuid4())
        new_engine.load_model(model_path)
        print(f"   Loaded successfully")
        print(f"   Feature names preserved: {len(new_engine.feature_names)} features")

        # Test loaded model can forecast
        forecast_df2, _ = new_engine.forecast(
            data.tail(200),
            target_column="consumption",
            periods=24
        )
        print(f"   Loaded model can forecast: {len(forecast_df2)} predictions")

    print("\n" + "=" * 60)
    print("All validations passed!")
    print("=" * 60)

if __name__ == "__main__":
    main()
