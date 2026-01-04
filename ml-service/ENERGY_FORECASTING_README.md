# Energy Forecasting ML Engine

Production-ready energy consumption forecasting engine using gradient boosting regression for 7-day ahead predictions.

## Overview

The `EnergyForecastingEngine` implements time-series forecasting for energy consumption data using:
- **XGBoost Regressor** (preferred, when available)
- **Sklearn GradientBoostingRegressor** (fallback)

It follows the exact architectural patterns established in `AnomalyDetectionEngine`, extending `BaseMLEngine` with robust feature engineering, multi-step forecasting, and comprehensive error handling.

## Features

### Time-Based Features
- `hour_of_day`: 0-23 (captures daily seasonality)
- `day_of_week`: 0-6 (captures weekly patterns)
- `is_weekend`: 0/1 (weekend vs weekday consumption)
- `month`: 1-12 (seasonal variations)
- `day_of_month`: 1-31
- `week_of_year`: 1-52

### Lag Features (Historical Consumption)
- `lag_1h`: Consumption 1 hour ago
- `lag_24h`: Consumption 24 hours ago (daily pattern)
- `lag_7d`: Consumption 7 days ago (weekly pattern)

### Rolling Statistics
- `rolling_mean_24h`: 24-hour moving average
- `rolling_std_24h`: 24-hour rolling standard deviation

## Implementation

### File Structure
```
ml-service/
├── app/
│   └── engines/
│       ├── base.py                      # BaseMLEngine interface
│       ├── energy_forecasting.py        # ⭐ NEW: Energy forecasting implementation
│       └── __init__.py                  # Updated exports
└── tests/
    ├── test_energy_forecasting.py       # ⭐ NEW: Comprehensive test suite
    └── conftest.py
```

### Class: `EnergyForecastingEngine`

**Location**: `ml-service/app/engines/energy_forecasting.py`

**Extends**: `BaseMLEngine`

**Constructor**:
```python
EnergyForecastingEngine(
    model_id: UUID,
    algorithm: str = "auto",  # "auto", "xgboost", or "gradient_boosting"
    model_path: Optional[str] = None
)
```

### Core Methods

#### 1. `get_default_hyperparameters() -> Dict[str, Any]`
Returns sensible default hyperparameters for the selected algorithm.

**XGBoost defaults**:
```python
{
    "n_estimators": 100,
    "learning_rate": 0.1,
    "max_depth": 6,
    "min_child_weight": 1,
    "subsample": 0.8,
    "colsample_bytree": 0.8,
    "gamma": 0,
    "reg_alpha": 0,
    "reg_lambda": 1,
    "random_state": 42
}
```

**GradientBoosting defaults**:
```python
{
    "n_estimators": 100,
    "learning_rate": 0.1,
    "max_depth": 6,
    "min_samples_split": 2,
    "min_samples_leaf": 1,
    "subsample": 0.8,
    "max_features": "sqrt",
    "random_state": 42
}
```

#### 2. `engineer_features(data, target_column, timestamp_column="timestamp") -> pd.DataFrame`
Transforms raw telemetry into ML-ready features.

**Input**:
```python
data = pd.DataFrame({
    "timestamp": ["2024-01-01 00:00:00", "2024-01-01 01:00:00", ...],
    "consumption": [105.2, 98.3, ...]
})
```

**Output**: DataFrame with 11 engineered features + target column

**Behavior**:
- Drops initial rows with NaN lag features (requires 168 hours minimum for `lag_7d`)
- Forward-fills and backward-fills remaining NaNs
- Sorts data by timestamp automatically

#### 3. `train(data, feature_columns, target_column, hyperparameters=None) -> Dict[str, Any]`
Trains the forecasting model and returns performance metrics.

**Parameters**:
- `data`: DataFrame with `timestamp` and consumption columns
- `feature_columns`: List of feature names (auto-engineered if raw data)
- `target_column`: **Required** - name of energy consumption column
- `hyperparameters`: Optional custom hyperparameters (merged with defaults)

**Returns**:
```python
{
    "algorithm": "xgboost",
    "training_samples": 696,
    "features": 11,
    "mae": 3.42,          # Mean Absolute Error
    "rmse": 4.81,         # Root Mean Squared Error
    "mape": 3.15,         # Mean Absolute Percentage Error (%)
    "target_mean": 105.3,
    "target_std": 18.7
}
```

**Example**:
```python
engine = EnergyForecastingEngine(model_id=uuid4())

metrics = engine.train(
    data=hourly_telemetry_df,
    feature_columns=["consumption"],  # Ignored, features auto-engineered
    target_column="consumption",
    hyperparameters={"n_estimators": 150, "learning_rate": 0.05}
)

print(f"MAE: {metrics['mae']:.2f} kWh")
print(f"MAPE: {metrics['mape']:.2f}%")
```

#### 4. `predict(data, feature_columns) -> np.ndarray`
Makes predictions on data with engineered features.

**Parameters**:
- `data`: DataFrame with engineered features
- `feature_columns`: List of feature names (typically `engine.feature_names`)

**Returns**: NumPy array of predictions

**Example**:
```python
# Engineer features first
df_engineered = engine.engineer_features(test_data, "consumption")

# Make predictions
predictions = engine.predict(df_engineered, engine.feature_names)
# array([102.3, 108.5, 115.2, ...])
```

#### 5. `forecast(last_known_data, target_column, periods=168, frequency="1H") -> Tuple[pd.DataFrame, np.ndarray]`
Generates multi-step ahead forecasts using recursive prediction.

**Parameters**:
- `last_known_data`: Recent historical data (min 168 hours for lag features)
- `target_column`: Energy consumption column name
- `periods`: Number of time steps to forecast (default: 168 = 7 days)
- `frequency`: Pandas frequency string (default: "1H" for hourly)

**Returns**: `(forecast_df, predictions_array)`
- `forecast_df`: DataFrame with `timestamp` and `predicted_consumption` columns
- `predictions_array`: NumPy array of predictions

**Recursive Prediction Strategy**:
1. Use last 168 hours of history for initial lag features
2. Predict next hour's consumption
3. Add prediction to history window
4. Use updated history to predict next hour
5. Repeat for `periods` time steps

**Example**:
```python
# Forecast next 7 days (168 hours)
forecast_df, predictions = engine.forecast(
    last_known_data=hourly_data.tail(200),  # At least 168 rows needed
    target_column="consumption",
    periods=168,
    frequency="1H"
)

print(forecast_df.head())
#           timestamp  predicted_consumption
# 0 2024-01-31 01:00:00                103.2
# 1 2024-01-31 02:00:00                 98.5
# 2 2024-01-31 03:00:00                 95.1
# ...

print(f"Average forecast: {predictions.mean():.2f} kWh")
```

**Production Safety**:
- Predictions clipped to non-negative values (energy can't be negative)
- History window maintained at 168 rows to prevent memory growth
- Missing lag features filled with training mean as fallback

#### 6. `save_model(path=None) -> str`
Persists model with scalers and statistics using joblib.

**Saves**:
- Trained model (XGBoost or GradientBoosting)
- Feature scaler (StandardScaler)
- Target scaler (StandardScaler)
- Feature names list
- Training statistics (mean, std, min, max)

**Example**:
```python
saved_path = engine.save_model("/models/energy_forecast_v1.joblib")
# Returns: "/models/energy_forecast_v1.joblib"
```

#### 7. `load_model(path=None) -> None`
Loads a previously saved model.

**Example**:
```python
engine = EnergyForecastingEngine(model_id=uuid4())
engine.load_model("/models/energy_forecast_v1.joblib")

# Model ready for predictions
forecast_df, _ = engine.forecast(data, "consumption", periods=24)
```

## Performance Metrics

### Mean Absolute Error (MAE)
Average absolute difference between predictions and actual values.
- **Units**: Same as target (e.g., kWh)
- **Interpretation**: Lower is better
- **Typical range**: 2-10 kWh for household consumption

### Root Mean Squared Error (RMSE)
Square root of average squared errors (penalizes large errors).
- **Units**: Same as target
- **Interpretation**: Lower is better
- **Property**: Always ≥ MAE

### Mean Absolute Percentage Error (MAPE)
Average percentage error relative to actual values.
- **Units**: Percentage
- **Interpretation**: Lower is better
- **Typical range**: 3-15% for good forecasts

## Testing

### Test Suite
**File**: `ml-service/tests/test_energy_forecasting.py`

**Coverage**: 50+ tests organized into 9 test classes:
1. `TestInitialization` - Engine creation, algorithm selection, default hyperparameters
2. `TestFeatureEngineering` - Time features, lag features, rolling stats, NaN handling
3. `TestTraining` - Raw data training, custom hyperparameters, statistics storage
4. `TestPrediction` - Shape validation, positive values, error handling
5. `TestForecasting` - 7-day forecasts, recursive prediction, timestamp validation
6. `TestMetrics` - MAE, RMSE, MAPE computation accuracy
7. `TestModelPersistence` - Save/load, model preservation, reusability

### Running Tests
```bash
# All energy forecasting tests
cd ml-service
pytest tests/test_energy_forecasting.py -v

# Specific test class
pytest tests/test_energy_forecasting.py::TestEnergyForecastingEngine::TestForecasting -v

# With coverage
pytest tests/test_energy_forecasting.py --cov=app.engines.energy_forecasting

# Quick validation script (no pytest needed)
python validate_energy_forecasting.py
```

### Test Data
Tests use synthetic hourly consumption data with realistic patterns:
- **Base load**: 100 kWh
- **Daily seasonality**: ±30 kWh (sine wave, peak at noon)
- **Weekly seasonality**: -20 kWh on weekends
- **Random noise**: σ=5 kWh
- **Duration**: 30 days (720 hours)

## Usage Examples

### Example 1: Basic Training and Forecasting
```python
from uuid import uuid4
import pandas as pd
from app.engines.energy_forecasting import EnergyForecastingEngine

# Load hourly consumption data
data = pd.read_csv("hourly_consumption.csv")
# Columns: timestamp, consumption

# Create and train engine
engine = EnergyForecastingEngine(model_id=uuid4())
metrics = engine.train(
    data=data,
    feature_columns=["consumption"],
    target_column="consumption"
)

print(f"Model trained: MAE={metrics['mae']:.2f}, MAPE={metrics['mape']:.2f}%")

# Forecast next 7 days
forecast_df, predictions = engine.forecast(
    last_known_data=data.tail(300),
    target_column="consumption",
    periods=168
)

forecast_df.to_csv("7day_forecast.csv", index=False)
```

### Example 2: Custom Hyperparameters
```python
engine = EnergyForecastingEngine(model_id=uuid4(), algorithm="xgboost")

# Optimize for faster training with more regularization
custom_params = {
    "n_estimators": 150,
    "learning_rate": 0.05,
    "max_depth": 5,
    "reg_alpha": 0.1,    # L1 regularization
    "reg_lambda": 1.5    # L2 regularization
}

metrics = engine.train(
    data=training_data,
    feature_columns=["consumption"],
    target_column="consumption",
    hyperparameters=custom_params
)
```

### Example 3: Model Deployment Workflow
```python
# Training phase
engine = EnergyForecastingEngine(model_id=model_uuid)
metrics = engine.train(historical_data, ["consumption"], "consumption")

# Save model
model_path = engine.save_model(f"/models/{model_uuid}.joblib")
print(f"Model saved: {model_path}")

# ------- Later: Inference phase -------

# Load model in production
production_engine = EnergyForecastingEngine(model_id=model_uuid)
production_engine.load_model(model_path)

# Generate hourly forecasts
forecast_df, _ = production_engine.forecast(
    recent_data.tail(200),
    "consumption",
    periods=24  # Next 24 hours
)

# Send forecasts to API/dashboard
api.post("/forecasts", forecast_df.to_dict(orient="records"))
```

### Example 4: Daily Forecasts (Instead of Hourly)
```python
# For daily predictions, use daily aggregated data
daily_data = hourly_data.resample('D', on='timestamp').agg({
    'consumption': 'sum'
}).reset_index()

engine = EnergyForecastingEngine(model_id=uuid4())
engine.train(daily_data, ["consumption"], "consumption")

# Forecast next 7 days
forecast_df, predictions = engine.forecast(
    daily_data.tail(30),
    "consumption",
    periods=7,
    frequency="1D"  # Daily frequency
)
```

## Algorithm Selection

### When to Use XGBoost
- **Pros**: Faster training, better accuracy, built-in regularization
- **Cons**: Extra dependency, larger model files
- **Use cases**: Production deployments, large datasets (>10k samples)

### When to Use GradientBoosting
- **Pros**: No extra dependencies, sklearn-standard, smaller models
- **Cons**: Slower training, slightly lower accuracy
- **Use cases**: Development/testing, small datasets, minimal dependencies

### Auto Selection
```python
# Uses XGBoost if available, falls back to GradientBoosting
engine = EnergyForecastingEngine(model_id=uuid4(), algorithm="auto")
```

## Production Considerations

### Training Data Requirements
- **Minimum**: 168 hours (7 days) for lag features
- **Recommended**: 720+ hours (30 days) for robust training
- **Optimal**: 8760 hours (1 year) to capture seasonal patterns

### Forecast Horizon Limits
- **Short-term (1-24h)**: High accuracy, MAPE typically 3-8%
- **Medium-term (1-7d)**: Good accuracy, MAPE typically 5-15%
- **Long-term (>7d)**: Decreasing accuracy, recursive errors compound

### Memory Usage
- **Training**: O(n_samples × n_features) - scales linearly
- **Forecasting**: Fixed O(168) - history window capped at 168 rows
- **Model size**: ~500KB - 2MB depending on n_estimators

### Retraining Strategy
- **Frequency**: Weekly or monthly
- **Data**: Rolling window (last 90-365 days)
- **Trigger**: When MAPE exceeds threshold (e.g., >15%)

### Monitoring Metrics
- **MAE drift**: Track MAE on validation set over time
- **Forecast bias**: Monitor mean(predictions - actuals)
- **Peak detection**: Verify model captures peak hours correctly

## Error Handling

The engine includes robust error handling:

```python
# Missing target column
try:
    engine.train(data, ["consumption"], target_column=None)
except ValueError as e:
    # "target_column is required for energy forecasting"

# Missing timestamp column
try:
    engine.train(data_no_timestamp, ["consumption"], "consumption")
except ValueError as e:
    # "Data must contain 'timestamp' column for feature engineering"

# Prediction before training
try:
    engine.predict(data, ["hour_of_day"])
except ValueError as e:
    # "Model not trained or loaded"

# Missing features
try:
    engine.predict(incomplete_data, engine.feature_names)
except ValueError as e:
    # "Missing features: {'lag_1h', 'lag_24h'}"
```

## Integration with ML Service API

### Training Endpoint
```python
# POST /api/v1/models/train
{
  "model_id": "uuid",
  "organization_id": 1,
  "model_type": "ENERGY_FORECAST",
  "algorithm": "xgboost",
  "hyperparameters": {
    "n_estimators": 100,
    "learning_rate": 0.1
  },
  "feature_columns": ["consumption"],
  "target_column": "consumption",
  "training_data_start": "2024-01-01T00:00:00Z",
  "training_data_end": "2024-01-31T23:59:59Z"
}
```

### Inference Endpoint
```python
# POST /api/v1/models/{model_id}/forecast
{
  "device_id": "uuid",
  "organization_id": 1,
  "periods": 168,  # 7 days
  "frequency": "1H"
}

# Response
{
  "forecast": [
    {"timestamp": "2024-02-01T00:00:00Z", "predicted_consumption": 103.2},
    {"timestamp": "2024-02-01T01:00:00Z", "predicted_consumption": 98.5},
    ...
  ],
  "summary": {
    "mean": 105.3,
    "min": 85.2,
    "max": 128.7,
    "total_predicted": 17690.4
  }
}
```

## Architecture Alignment

This implementation follows the exact patterns from `AnomalyDetectionEngine`:

| Aspect | Pattern |
|--------|---------|
| Base class | Extends `BaseMLEngine` |
| Initialization | `model_id`, `algorithm`, `model_path` |
| Hyperparameters | `get_default_hyperparameters()` method |
| Training | Returns metrics dict, sets `is_loaded = True` |
| Prediction | Validates features, transforms with scaler |
| Persistence | `save_model()` / `load_model()` with joblib |
| Scalers | StandardScaler for features and target |
| Logging | Uses `logger = logging.getLogger(__name__)` |
| Error handling | Raises ValueError with descriptive messages |

## Future Enhancements

### Potential Improvements
1. **Exogenous variables**: Weather, holidays, special events
2. **Quantile regression**: Prediction intervals (confidence bounds)
3. **Multi-target**: Forecast multiple variables simultaneously
4. **Automated hyperparameter tuning**: Grid search / Bayesian optimization
5. **Online learning**: Incremental updates without full retraining
6. **Ensemble methods**: Combine multiple forecasts (XGBoost + LSTM)

## References

- **XGBoost Documentation**: https://xgboost.readthedocs.io/
- **Scikit-learn GradientBoosting**: https://scikit-learn.org/stable/modules/ensemble.html#gradient-boosting
- **Time Series Forecasting**: https://otexts.com/fpp3/

---

**Implementation Date**: 2026-01-02
**Version**: 1.0.0
**Author**: VelocityX ML Team
**Status**: Production-Ready ✅
