# Energy Forecasting - Quick Reference

## Quick Start (5 minutes)

### 1. Basic Training and Forecasting
```python
from uuid import uuid4
import pandas as pd
from app.engines.energy_forecasting import EnergyForecastingEngine

# Load your data (must have 'timestamp' and consumption columns)
data = pd.read_csv("hourly_consumption.csv")

# Create engine
engine = EnergyForecastingEngine(model_id=uuid4())

# Train
metrics = engine.train(
    data=data,
    feature_columns=["consumption"],
    target_column="consumption"
)

# Forecast next 7 days (168 hours)
forecast_df, predictions = engine.forecast(
    last_known_data=data.tail(200),
    target_column="consumption",
    periods=168
)

# Save results
forecast_df.to_csv("7day_forecast.csv", index=False)
```

### 2. Save and Load Model
```python
# Save
model_path = engine.save_model("/models/energy_forecast_v1.joblib")

# Load later
new_engine = EnergyForecastingEngine(model_id=uuid4())
new_engine.load_model(model_path)

# Use loaded model
forecast_df, _ = new_engine.forecast(recent_data, "consumption", periods=24)
```

## Method Reference

### `__init__(model_id, algorithm="auto", model_path=None)`
- **algorithm**: `"auto"`, `"xgboost"`, or `"gradient_boosting"`
- **Returns**: Engine instance

### `train(data, feature_columns, target_column, hyperparameters=None)`
- **data**: DataFrame with `timestamp` and consumption columns
- **target_column**: Required - name of consumption column
- **Returns**: `{"mae": float, "rmse": float, "mape": float, ...}`

### `forecast(last_known_data, target_column, periods=168, frequency="1H")`
- **periods**: Number of time steps (default: 168 = 7 days)
- **frequency**: `"1H"` (hourly), `"1D"` (daily), etc.
- **Returns**: `(forecast_df, predictions_array)`

### `predict(data, feature_columns)`
- **data**: DataFrame with engineered features
- **feature_columns**: Use `engine.feature_names`
- **Returns**: NumPy array of predictions

### `save_model(path=None)` / `load_model(path=None)`
- Persists/loads model with joblib
- Includes scalers and training statistics

## Feature Engineering

### Automatic Features (11 total)
```python
# Time-based (6)
hour_of_day     # 0-23
day_of_week     # 0-6 (Monday=0)
is_weekend      # 0/1
month           # 1-12
day_of_month    # 1-31
week_of_year    # 1-52

# Lag features (3)
lag_1h          # Consumption 1 hour ago
lag_24h         # Consumption 24 hours ago
lag_7d          # Consumption 168 hours ago

# Rolling statistics (2)
rolling_mean_24h  # 24-hour moving average
rolling_std_24h   # 24-hour standard deviation
```

### Manual Feature Engineering
```python
# Only needed if you want to see/modify features
df_engineered = engine.engineer_features(data, "consumption")
# Now has all 11 features + consumption column
```

## Hyperparameter Tuning

### Default Hyperparameters
```python
params = engine.get_default_hyperparameters()
# XGBoost: n_estimators=100, learning_rate=0.1, max_depth=6
# GradientBoosting: n_estimators=100, learning_rate=0.1, max_depth=6
```

### Custom Hyperparameters
```python
custom_params = {
    "n_estimators": 150,      # More trees = better fit, slower training
    "learning_rate": 0.05,    # Lower = more regularization
    "max_depth": 5,           # Shallower = less overfitting
    "subsample": 0.7,         # Train on 70% of samples per tree
}

metrics = engine.train(data, ["consumption"], "consumption", custom_params)
```

## Performance Metrics

### MAE (Mean Absolute Error)
- **Units**: Same as consumption (kWh)
- **Meaning**: Average prediction error
- **Good**: < 5 kWh for household, < 20 kWh for commercial

### RMSE (Root Mean Squared Error)
- **Units**: Same as consumption (kWh)
- **Meaning**: Penalizes large errors more
- **Property**: Always ≥ MAE

### MAPE (Mean Absolute Percentage Error)
- **Units**: Percentage
- **Meaning**: Average error relative to actual value
- **Good**: < 10% excellent, < 15% good, > 20% needs improvement

## Common Patterns

### Pattern 1: Weekly Retraining
```python
# Every week, retrain with last 90 days
recent_data = get_data_last_90_days()
metrics = engine.train(recent_data, ["consumption"], "consumption")

if metrics["mape"] < 15:  # Acceptable performance
    engine.save_model(f"/models/weekly_{datetime.now():%Y%m%d}.joblib")
else:
    alert("Model performance degraded!")
```

### Pattern 2: Hourly Forecasting Service
```python
def generate_forecast():
    """Run every hour to update forecasts."""
    # Load latest model
    engine = EnergyForecastingEngine(model_id=current_model_id)
    engine.load_model("/models/production_model.joblib")

    # Get recent data
    recent_data = get_last_200_hours()

    # Forecast next 24 hours
    forecast_df, _ = engine.forecast(recent_data, "consumption", periods=24)

    # Store in database
    save_to_database(forecast_df)
```

### Pattern 3: Multi-Building Forecasting
```python
buildings = ["Building_A", "Building_B", "Building_C"]

for building in buildings:
    data = load_building_data(building)

    engine = EnergyForecastingEngine(model_id=uuid4())
    metrics = engine.train(data, ["consumption"], "consumption")

    forecast_df, _ = engine.forecast(data.tail(200), "consumption", periods=168)

    engine.save_model(f"/models/{building}_model.joblib")
    forecast_df.to_csv(f"/forecasts/{building}_7day.csv", index=False)
```

## Data Requirements

### Minimum Requirements
- **Rows**: 168+ hours (7 days) for lag features
- **Columns**: `timestamp` (datetime), consumption (numeric)
- **Frequency**: Regular intervals (hourly recommended)

### Recommended for Good Results
- **Rows**: 720+ hours (30 days), ideally 8760 hours (1 year)
- **Quality**: No large gaps, consistent frequency
- **Outliers**: Clean extreme values (sensor errors)

### Data Format
```python
# CSV
timestamp,consumption
2024-01-01 00:00:00,102.3
2024-01-01 01:00:00,98.5
...

# DataFrame
data = pd.DataFrame({
    "timestamp": pd.date_range("2024-01-01", periods=720, freq="1H"),
    "consumption": [102.3, 98.5, ...]
})
```

## Troubleshooting

### Error: "target_column is required"
```python
# WRONG
engine.train(data, ["consumption"])

# CORRECT
engine.train(data, ["consumption"], target_column="consumption")
```

### Error: "timestamp column not found"
```python
# Ensure your data has a 'timestamp' column
data = data.rename(columns={"date": "timestamp"})

# Or specify timestamp column in engineer_features
df_eng = engine.engineer_features(data, "consumption", timestamp_column="date")
```

### Error: "Model not trained"
```python
# Train before predicting
engine.train(data, ["consumption"], "consumption")

# Or load a trained model
engine.load_model("/models/saved_model.joblib")
```

### Poor Performance (high MAPE)
```python
# 1. Use more training data (30+ days)
data = get_last_60_days()

# 2. Tune hyperparameters
custom_params = {
    "n_estimators": 200,  # More trees
    "learning_rate": 0.05,  # Slower learning
    "max_depth": 7  # Deeper trees
}

# 3. Check data quality
print(data.isna().sum())  # Look for missing values
print(data.describe())     # Look for outliers
```

## Testing

### Run Tests
```bash
# All tests
pytest tests/test_energy_forecasting.py -v

# Specific test
pytest tests/test_energy_forecasting.py::TestEnergyForecastingEngine::TestTraining -v

# Quick validation
python validate_energy_forecasting.py
```

### Example Script
```bash
# Run example with visualization
python example_energy_forecast.py
```

## File Locations

```
ml-service/
├── app/engines/
│   └── energy_forecasting.py      # Main implementation
├── tests/
│   └── test_energy_forecasting.py # Test suite
├── validate_energy_forecasting.py # Quick validation
├── example_energy_forecast.py     # Usage example
├── ENERGY_FORECASTING_README.md   # Full documentation
└── QUICK_REFERENCE_ENERGY_FORECASTING.md  # This file
```

## Key Takeaways

1. **Always provide `target_column`** in `train()`
2. **Need 168+ hours** of data for lag features
3. **Use `forecast()` for multi-step** predictions (not `predict()`)
4. **Check MAPE < 15%** for production use
5. **Retrain regularly** (weekly/monthly) with fresh data
6. **Save models** after successful training
7. **Test with `validate_energy_forecasting.py`** first

---

**Full Documentation**: See `ENERGY_FORECASTING_README.md`
**Implementation**: `app/engines/energy_forecasting.py`
**Tests**: `tests/test_energy_forecasting.py`
