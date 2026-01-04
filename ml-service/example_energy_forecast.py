"""
Example: Energy Forecasting with EnergyForecastingEngine

This example demonstrates:
1. Loading hourly energy consumption data
2. Training a forecasting model
3. Generating 7-day ahead forecasts
4. Saving and loading the model
5. Visualizing results (optional, requires matplotlib)
"""
import sys
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from uuid import uuid4

# Add app to path
sys.path.insert(0, 'app')

from engines.energy_forecasting import EnergyForecastingEngine


def generate_sample_data(days=30):
    """
    Generate sample hourly energy consumption data.

    Simulates realistic patterns:
    - Base load: 100 kWh
    - Daily seasonality: Higher during day, lower at night
    - Weekly seasonality: Lower on weekends
    - Random noise
    """
    np.random.seed(42)
    n_hours = days * 24
    start_time = datetime(2024, 1, 1, 0, 0, 0)

    timestamps = [start_time + timedelta(hours=i) for i in range(n_hours)]
    consumption = []

    for ts in timestamps:
        # Base load
        base = 100.0

        # Daily pattern (peak around 2pm, low around 4am)
        hour = ts.hour
        daily_pattern = 30 * np.sin((hour - 6) * np.pi / 12)

        # Weekly pattern (lower on weekends)
        is_weekend = ts.weekday() >= 5
        weekly_pattern = -20 if is_weekend else 0

        # Random noise
        noise = np.random.normal(0, 5)

        consumption.append(base + daily_pattern + weekly_pattern + noise)

    return pd.DataFrame({
        "timestamp": timestamps,
        "consumption": consumption
    })


def main():
    print("=" * 70)
    print("Energy Forecasting Example")
    print("=" * 70)

    # -------------------------------------------------------------------------
    # 1. Generate training data (30 days)
    # -------------------------------------------------------------------------
    print("\n[Step 1] Generating training data...")
    data = generate_sample_data(days=30)
    print(f"Generated {len(data)} hourly samples")
    print(f"Date range: {data['timestamp'].min()} to {data['timestamp'].max()}")
    print(f"\nData preview:")
    print(data.head())
    print(f"\nConsumption statistics:")
    print(f"  Mean: {data['consumption'].mean():.2f} kWh")
    print(f"  Std:  {data['consumption'].std():.2f} kWh")
    print(f"  Min:  {data['consumption'].min():.2f} kWh")
    print(f"  Max:  {data['consumption'].max():.2f} kWh")

    # -------------------------------------------------------------------------
    # 2. Create and train forecasting model
    # -------------------------------------------------------------------------
    print("\n[Step 2] Training forecasting model...")
    engine = EnergyForecastingEngine(
        model_id=uuid4(),
        algorithm="auto"  # Uses XGBoost if available, else GradientBoosting
    )

    print(f"Selected algorithm: {engine.algorithm}")

    # Optional: Customize hyperparameters
    custom_params = {
        "n_estimators": 100,
        "learning_rate": 0.1,
        "max_depth": 6
    }

    metrics = engine.train(
        data=data,
        feature_columns=["consumption"],  # Will be auto-engineered
        target_column="consumption",
        hyperparameters=custom_params
    )

    print(f"\nTraining complete!")
    print(f"  Algorithm:        {metrics['algorithm']}")
    print(f"  Training samples: {metrics['training_samples']}")
    print(f"  Features:         {metrics['features']}")
    print(f"\nModel performance:")
    print(f"  MAE:  {metrics['mae']:.2f} kWh")
    print(f"  RMSE: {metrics['rmse']:.2f} kWh")
    print(f"  MAPE: {metrics['mape']:.2f}%")

    # -------------------------------------------------------------------------
    # 3. Generate 7-day forecast
    # -------------------------------------------------------------------------
    print("\n[Step 3] Generating 7-day forecast (168 hours)...")

    forecast_df, predictions = engine.forecast(
        last_known_data=data.tail(200),  # Use last 200 hours for context
        target_column="consumption",
        periods=168,  # 7 days
        frequency="1H"
    )

    print(f"Forecast generated: {len(predictions)} hourly predictions")
    print(f"Forecast range: {forecast_df['timestamp'].min()} to {forecast_df['timestamp'].max()}")
    print(f"\nForecast statistics:")
    print(f"  Mean: {predictions.mean():.2f} kWh")
    print(f"  Min:  {predictions.min():.2f} kWh")
    print(f"  Max:  {predictions.max():.2f} kWh")

    print(f"\nFirst 24 hours of forecast:")
    print(forecast_df.head(24))

    # -------------------------------------------------------------------------
    # 4. Save model for later use
    # -------------------------------------------------------------------------
    print("\n[Step 4] Saving model...")
    import tempfile
    import os

    with tempfile.TemporaryDirectory() as tmpdir:
        model_path = os.path.join(tmpdir, "energy_forecast_model.joblib")
        saved_path = engine.save_model(model_path)
        print(f"Model saved to: {saved_path}")

        # File size
        file_size = os.path.getsize(saved_path)
        print(f"Model size: {file_size / 1024:.2f} KB")

        # -------------------------------------------------------------------------
        # 5. Load model and test inference
        # -------------------------------------------------------------------------
        print("\n[Step 5] Loading model and testing inference...")
        new_engine = EnergyForecastingEngine(model_id=uuid4())
        new_engine.load_model(saved_path)
        print("Model loaded successfully!")

        # Test with loaded model
        test_forecast_df, test_predictions = new_engine.forecast(
            last_known_data=data.tail(200),
            target_column="consumption",
            periods=24  # Next 24 hours
        )

        print(f"Loaded model generated {len(test_predictions)}-hour forecast")
        print(f"Predictions match original: {np.allclose(predictions[:24], test_predictions)}")

    # -------------------------------------------------------------------------
    # 6. Optional: Visualize results (requires matplotlib)
    # -------------------------------------------------------------------------
    try:
        import matplotlib.pyplot as plt

        print("\n[Step 6] Visualizing forecast...")

        fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(14, 10))

        # Plot 1: Last 7 days of history + 7-day forecast
        history_7d = data.tail(168)

        ax1.plot(history_7d['timestamp'], history_7d['consumption'],
                 label='Historical (Last 7 days)', color='blue', linewidth=2)
        ax1.plot(forecast_df['timestamp'], forecast_df['predicted_consumption'],
                 label='Forecast (Next 7 days)', color='red', linestyle='--', linewidth=2)
        ax1.axvline(data['timestamp'].max(), color='green', linestyle=':', label='Forecast Start')
        ax1.set_xlabel('Timestamp')
        ax1.set_ylabel('Energy Consumption (kWh)')
        ax1.set_title('Energy Consumption: Historical vs Forecast')
        ax1.legend()
        ax1.grid(True, alpha=0.3)

        # Plot 2: Daily consumption pattern (aggregated by hour)
        forecast_df['hour'] = forecast_df['timestamp'].dt.hour
        hourly_avg = forecast_df.groupby('hour')['predicted_consumption'].mean()

        ax2.bar(hourly_avg.index, hourly_avg.values, color='orange', alpha=0.7)
        ax2.set_xlabel('Hour of Day')
        ax2.set_ylabel('Average Consumption (kWh)')
        ax2.set_title('Forecasted Daily Consumption Pattern')
        ax2.set_xticks(range(24))
        ax2.grid(True, alpha=0.3, axis='y')

        plt.tight_layout()
        plt.savefig('energy_forecast_visualization.png', dpi=150)
        print("Visualization saved to: energy_forecast_visualization.png")
        print("Opening plot...")
        plt.show()

    except ImportError:
        print("\n[Step 6] Matplotlib not installed, skipping visualization")
        print("Install with: pip install matplotlib")

    # -------------------------------------------------------------------------
    # Summary
    # -------------------------------------------------------------------------
    print("\n" + "=" * 70)
    print("Example completed successfully!")
    print("=" * 70)
    print(f"\nKey results:")
    print(f"  - Trained model with MAE={metrics['mae']:.2f} kWh, MAPE={metrics['mape']:.2f}%")
    print(f"  - Generated 7-day forecast with mean={predictions.mean():.2f} kWh")
    print(f"  - Model saved and successfully reloaded")
    print(f"\nNext steps:")
    print(f"  - Use this model in production for automated forecasting")
    print(f"  - Retrain weekly/monthly with new data")
    print(f"  - Monitor forecast accuracy and adjust hyperparameters")


if __name__ == "__main__":
    main()
