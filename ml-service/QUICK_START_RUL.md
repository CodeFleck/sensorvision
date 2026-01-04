# Equipment RUL Engine - Quick Start Guide

## 5-Minute Quickstart

### 1. Install Dependencies

```bash
cd ml-service
pip install -r requirements.txt
```

### 2. Run Example

```bash
python examples/equipment_rul_example.py
```

Expected output:
```
Equipment RUL Prediction Engine - Example
================================================================================

1. Generating synthetic equipment data...
   Generated 2000 samples
   RUL statistics: mean=118.3, std=91.4, min=0.0, max=365.0

2. Initializing RUL engine...

3. Training model with 6 features...
   Features: operating_hours, cycle_count, temperature, vibration, wear_indicator, pressure

   Training Results:
   - Algorithm: gradient_boosting_regressor
   - Training samples: 1600
   - Validation samples: 400

   Validation Metrics:
   - MAE: 8.45 days
   - RMSE: 12.33 days
   - R²: 0.8765

4. Analyzing feature importance...

   Top Features (by importance):
   1. wear_indicator        0.3245 (32.5%)
   2. operating_hours       0.2187 (21.9%)
   3. vibration            0.1876 (18.8%)

5. Making predictions on test samples...

   Predictions:
   New Equipment  :  345.2 days
   Mid-Life       :  182.7 days
   Near Failure   :   12.4 days
   High Stress    :   45.8 days
```

### 3. Run Tests

```bash
pytest tests/test_equipment_rul.py -v
```

Expected: 67 tests pass

## Basic Usage

### Training

```python
from uuid import uuid4
import pandas as pd
from app.engines.equipment_rul import EquipmentRULEngine

# Initialize
engine = EquipmentRULEngine(model_id=uuid4())

# Prepare data
data = pd.DataFrame({
    "operating_hours": [100, 5000, 9500],
    "temperature": [70, 75, 85],
    "vibration": [0.2, 0.5, 1.8],
    "rul": [350, 180, 15]  # Target: days until maintenance
})

# Train
metrics = engine.train(
    data=data,
    feature_columns=["operating_hours", "temperature", "vibration"],
    target_column="rul"
)

print(f"Validation MAE: {metrics['val_mae']:.2f} days")
print(f"Validation R²: {metrics['val_r2']:.4f}")
```

### Prediction

```python
# New equipment data
test_data = pd.DataFrame({
    "operating_hours": [3000],
    "temperature": [78],
    "vibration": [0.65]
})

# Predict RUL
rul = engine.predict(test_data, ["operating_hours", "temperature", "vibration"])
print(f"Predicted RUL: {rul[0]:.1f} days")

# Predict with confidence
pred, lower, upper = engine.predict_with_confidence(
    test_data,
    ["operating_hours", "temperature", "vibration"],
    confidence_level=0.95
)
print(f"RUL: {pred[0]:.1f} days [95% CI: {lower[0]:.1f} - {upper[0]:.1f}]")
```

### Model Persistence

```python
# Save trained model
model_path = engine.save_model("/path/to/model.joblib")

# Load in new session
new_engine = EquipmentRULEngine(model_id=uuid4())
new_engine.load_model(model_path)

# Use loaded model
predictions = new_engine.predict(test_data, feature_columns)
```

## Common Scenarios

### Scenario 1: Custom Hyperparameters

```python
custom_params = {
    "n_estimators": 300,      # More trees for better accuracy
    "learning_rate": 0.05,    # Lower learning rate (more conservative)
    "max_depth": 7,           # Deeper trees for complex patterns
}

metrics = engine.train(
    data=data,
    feature_columns=features,
    target_column="rul",
    hyperparameters=custom_params
)
```

### Scenario 2: Feature Importance Analysis

```python
# After training
importance = engine.get_feature_importance()

# Sort by importance
sorted_features = sorted(importance.items(), key=lambda x: x[1], reverse=True)

# Print top 5
print("Top 5 Features:")
for feature, score in sorted_features[:5]:
    print(f"{feature}: {score:.4f} ({score*100:.1f}%)")
```

### Scenario 3: Batch Predictions

```python
# Multiple devices
devices_data = pd.DataFrame({
    "device_id": ["A", "B", "C"],
    "operating_hours": [1000, 5000, 8000],
    "temperature": [70, 75, 82],
    "vibration": [0.3, 0.6, 1.2],
})

predictions = engine.predict(
    devices_data[["operating_hours", "temperature", "vibration"]],
    ["operating_hours", "temperature", "vibration"]
)

# Add predictions to dataframe
devices_data["predicted_rul"] = predictions
print(devices_data)
```

### Scenario 4: Error Handling

```python
try:
    # Train with validation
    metrics = engine.train(data, features, target)

    # Check if model is good enough
    if metrics['val_r2'] < 0.5:
        print("Warning: Poor model performance (R² < 0.5)")
        print("Consider adding more features or data")

except ValueError as e:
    print(f"Training error: {e}")
    # Handle missing features, invalid data, etc.
```

## Troubleshooting

### Issue: "Model not trained or loaded"

**Solution:** Train the model first or load a saved model

```python
# Option 1: Train
engine.train(data, features, target)

# Option 2: Load
engine.load_model("/path/to/model.joblib")
```

### Issue: "Missing features: {'feature_x'}"

**Solution:** Ensure all training features are in prediction data

```python
# Training features
train_features = ["operating_hours", "temperature", "vibration"]

# Prediction data MUST have same features
test_data = test_data[train_features]  # Select only needed columns

predictions = engine.predict(test_data, train_features)
```

### Issue: "Target column 'rul' not found"

**Solution:** Verify target column name matches your data

```python
# Check column names
print(data.columns.tolist())

# Use correct target column
metrics = engine.train(data, features, target_column="rul")  # Match your data
```

### Issue: Poor prediction accuracy

**Solution 1:** Add more training data (target: 500+ samples)

**Solution 2:** Add more informative features
```python
# Add degradation indicators
features = [
    "operating_hours",
    "temperature",
    "vibration",
    "wear_indicator",      # Add this
    "cycle_count",         # Add this
    "efficiency_ratio"     # Add this
]
```

**Solution 3:** Tune hyperparameters
```python
# Increase model complexity
hyperparameters = {
    "n_estimators": 300,   # More trees
    "max_depth": 7,        # Deeper trees
    "learning_rate": 0.05  # More conservative
}
```

### Issue: Predictions are negative

**This should never happen** - the engine automatically clips to 0. If you see negatives, there's a bug - please report.

## Best Practices

### Data Preparation

1. **Feature Scaling**: Handled automatically by engine (StandardScaler)
2. **Missing Values**: Impute before calling engine (use mean, median, or forward-fill)
3. **Outliers**: Engine is robust to outliers (Huber loss), but extreme values may affect accuracy
4. **Data Split**: Engine does 80/20 train/val split automatically

### Feature Selection

**Good Features:**
- Operating hours (continuous usage)
- Cycle counts (discrete operations)
- Sensor readings (temperature, vibration, pressure)
- Degradation indicators (wear, efficiency, quality metrics)
- Maintenance history (time since last maintenance)

**Avoid:**
- Device IDs (use separate models per device type)
- Timestamps (use features derived from time: age, hours_since_maintenance)
- Categorical features with high cardinality (encode first)

### Model Validation

```python
# After training, check metrics
if metrics['val_r2'] > 0.7:
    print("Good model (R² > 0.7)")
elif metrics['val_r2'] > 0.5:
    print("Acceptable model (R² > 0.5)")
else:
    print("Poor model (R² < 0.5) - needs improvement")

# Check MAE relative to mean RUL
mae_percent = metrics['val_mae'] / metrics['mean_rul'] * 100
print(f"MAE is {mae_percent:.1f}% of mean RUL")
```

### Production Deployment

1. **Save model after training**
   ```python
   model_path = engine.save_model(f"/models/{model_id}.joblib")
   ```

2. **Load model for inference**
   ```python
   engine = EquipmentRULEngine(model_id=model_id)
   engine.load_model(model_path)
   ```

3. **Monitor prediction distribution**
   ```python
   predictions = engine.predict(data, features)
   print(f"Pred mean: {predictions.mean():.1f}")
   print(f"Pred std: {predictions.std():.1f}")
   # Should be similar to training distribution
   ```

4. **Use confidence intervals for critical decisions**
   ```python
   pred, lower, upper = engine.predict_with_confidence(data, features, 0.95)

   # High uncertainty = wide interval
   uncertainty = upper - lower
   if uncertainty[0] > 30:  # More than ±30 days
       print("High uncertainty - recommend inspection")
   ```

## Performance Expectations

### Training Time

| Samples | Features | Time (CPU) |
|---------|----------|------------|
| 100     | 5        | < 1 sec    |
| 1,000   | 10       | 1-3 sec    |
| 10,000  | 10       | 10-30 sec  |
| 100,000 | 10       | 1-3 min    |

### Prediction Time

- **Single prediction**: < 10 ms
- **Batch (100 samples)**: < 50 ms
- **Batch (1000 samples)**: < 200 ms

### Model Accuracy

Typical performance on industrial equipment:
- **MAE**: 5-15 days
- **RMSE**: 8-20 days
- **R²**: 0.70-0.95

## Next Steps

1. **Read Full Documentation**: `ml-service/app/engines/README_RUL.md`
2. **Explore Example**: `ml-service/examples/equipment_rul_example.py`
3. **Run Tests**: `pytest tests/test_equipment_rul.py -v --cov`
4. **Integration**: See ML Service API documentation for REST endpoints

## Support

For issues or questions:
1. Check the test suite: `tests/test_equipment_rul.py`
2. Review the example: `examples/equipment_rul_example.py`
3. Read the full docs: `app/engines/README_RUL.md`
4. Check implementation summary: `IMPLEMENTATION_SUMMARY.md`
