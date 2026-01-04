# Equipment RUL (Remaining Useful Life) Engine

## Overview

The Equipment RUL Engine is a production-ready machine learning system for predicting the remaining useful life of industrial equipment. It uses Gradient Boosting Regression to estimate how many days remain until maintenance is needed based on equipment condition indicators.

## Algorithm: Gradient Boosting Regressor

### Why Gradient Boosting?

1. **High Accuracy**: Ensemble method that combines weak learners (decision trees) into a strong predictor
2. **Handles Non-Linear Relationships**: Captures complex interactions between degradation indicators
3. **Robust to Outliers**: Huber loss function provides robustness to measurement noise
4. **Feature Importance**: Built-in feature importance scoring for interpretability
5. **Missing Value Tolerance**: Handles missing features gracefully
6. **Production Proven**: Widely used in industrial predictive maintenance

### Hyperparameters

| Parameter | Default | Description | Impact |
|-----------|---------|-------------|--------|
| `n_estimators` | 200 | Number of boosting stages | More trees = better fit but slower training |
| `learning_rate` | 0.1 | Shrinkage factor for each tree | Lower = more conservative, needs more trees |
| `max_depth` | 5 | Maximum tree depth | Higher = more complex patterns but risk of overfitting |
| `min_samples_split` | 10 | Minimum samples to split a node | Higher = more regularization |
| `min_samples_leaf` | 4 | Minimum samples at leaf | Higher = smoother predictions |
| `max_features` | sqrt | Features to consider per split | Lower = more randomization, better generalization |
| `subsample` | 0.8 | Fraction of samples per tree | Lower = more stochastic, prevents overfitting |
| `loss` | huber | Loss function | Huber is robust to outliers |
| `alpha` | 0.9 | Quantile for Huber loss | Controls outlier sensitivity |

## Features

### Input Features

The engine expects features that indicate equipment degradation:

**Operational Metrics:**
- `operating_hours`: Cumulative runtime hours
- `cycle_count`: Number of operational cycles completed
- `start_stop_cycles`: Frequency of start/stop events

**Sensor Readings:**
- `temperature`: Operating temperature (°C or °F)
- `vibration`: Vibration amplitude (mm/s or g)
- `pressure`: Operating pressure (PSI or bar)
- `current`: Electrical current draw (A)
- `voltage`: Electrical voltage (V)

**Degradation Indicators:**
- `wear_indicator`: Calculated wear metric (0-100%)
- `efficiency`: Current vs. design efficiency (%)
- `power_factor`: Electrical power factor
- `oil_quality`: Lubricant quality index
- `corrosion_index`: Corrosion severity metric

### Target Variable

- `rul`: Remaining Useful Life in days until maintenance required

## Performance Metrics

### Training Metrics

1. **MAE (Mean Absolute Error)**: Average prediction error in days
   - Lower is better
   - Direct interpretability (e.g., MAE=5 means ±5 days on average)

2. **RMSE (Root Mean Squared Error)**: Penalizes large errors more
   - Lower is better
   - More sensitive to outliers than MAE

3. **R² Score**: Proportion of variance explained
   - Range: -∞ to 1.0
   - 1.0 = perfect predictions
   - 0.0 = baseline (mean prediction)
   - < 0 = worse than baseline

### Validation Metrics

Uses 20% holdout for validation with same metrics as training.

### Expected Performance

For typical industrial equipment datasets:
- **Val MAE**: 5-15 days (depending on data quality)
- **Val RMSE**: 8-20 days
- **Val R²**: 0.70-0.95

## Usage

### 1. Training

```python
from uuid import uuid4
import pandas as pd
from app.engines.equipment_rul import EquipmentRULEngine

# Initialize engine
engine = EquipmentRULEngine(model_id=uuid4())

# Define features
feature_columns = [
    "operating_hours",
    "cycle_count",
    "temperature",
    "vibration",
    "wear_indicator"
]

# Train model
metrics = engine.train(
    data=training_data,
    feature_columns=feature_columns,
    target_column="rul",
    hyperparameters={
        "n_estimators": 200,
        "learning_rate": 0.1,
        "max_depth": 5
    }
)

print(f"Validation MAE: {metrics['val_mae']:.2f} days")
print(f"Validation R²: {metrics['val_r2']:.4f}")
```

### 2. Prediction

```python
# Simple prediction
predictions = engine.predict(test_data, feature_columns)
print(f"Predicted RUL: {predictions[0]:.1f} days")

# Prediction with confidence intervals
pred, lower, upper = engine.predict_with_confidence(
    test_data,
    feature_columns,
    confidence_level=0.95
)

print(f"RUL: {pred[0]:.1f} days [95% CI: {lower[0]:.1f} - {upper[0]:.1f}]")
```

### 3. Feature Importance

```python
# Get feature importance
importance = engine.get_feature_importance()

# Sort by importance
sorted_features = sorted(
    importance.items(),
    key=lambda x: x[1],
    reverse=True
)

for feature, score in sorted_features:
    print(f"{feature}: {score:.4f}")
```

### 4. Model Persistence

```python
# Save model
model_path = engine.save_model("/path/to/model.joblib")

# Load model
new_engine = EquipmentRULEngine(model_id=uuid4())
new_engine.load_model(model_path)
```

## Confidence Intervals

The `predict_with_confidence()` method provides uncertainty estimates:

### Methodology

1. **Staged Predictions**: Uses the last 50 boosting stages to create an ensemble
2. **Variance Estimation**: Computes standard deviation across stages
3. **Confidence Bounds**: Applies normal approximation with z-scores
   - 95% CI: ±1.96σ
   - 99% CI: ±2.576σ

### Interpretation

```python
pred, lower, upper = engine.predict_with_confidence(data, features, 0.95)

# Interpretation:
# - pred[i]: Most likely RUL
# - [lower[i], upper[i]]: 95% confidence interval
# - Wider intervals = higher uncertainty
```

**Uncertainty Sources:**
- Limited training data
- Feature measurement noise
- Equipment variability
- Unpredictable failure modes

## Data Requirements

### Minimum Dataset Size

- **Minimum**: 100 samples
- **Recommended**: 500+ samples
- **Optimal**: 2000+ samples

### Data Quality

**Required:**
- No missing target values (RUL)
- Non-negative RUL values
- Temporal consistency (features align with RUL)

**Recommended:**
- < 5% missing feature values
- Multiple equipment instances
- Coverage of different degradation stages
- Run-to-failure data for calibration

### Data Preprocessing

The engine handles:
- Feature scaling (StandardScaler)
- Negative RUL clipping to zero
- Train/validation split (80/20)

User should handle:
- Missing value imputation
- Outlier detection
- Feature engineering
- Data augmentation (if needed)

## Production Considerations

### Scalability

**Training:**
- Time Complexity: O(n_features × n_samples × n_estimators × max_depth)
- Space Complexity: O(n_estimators × max_depth × n_features)
- Typical training time: 1-30 seconds (2000 samples, 10 features)

**Inference:**
- Time Complexity: O(n_estimators × max_depth)
- Latency: < 10ms per prediction (CPU)
- Throughput: 1000+ predictions/second

### Model Size

- Typical size: 1-50 MB
- Depends on: n_estimators, max_depth, n_features
- Compression: joblib uses pickle protocol 5 (efficient)

### Monitoring

Monitor these metrics in production:

1. **Prediction Distribution**: Should align with training distribution
2. **Feature Drift**: Monitor feature statistics over time
3. **Prediction Errors**: Track MAE/RMSE on ground truth when available
4. **Confidence Widths**: Sudden increases indicate model uncertainty

### Retraining Strategy

Retrain when:
- Prediction error increases > 20%
- Feature distributions drift significantly
- New equipment types added
- Maintenance procedures change
- Quarterly or after 1000+ new samples

## Edge Cases

### Handled Automatically

1. **Negative RUL predictions**: Clipped to 0
2. **Negative RUL targets**: Clipped to 0 during training
3. **Extreme feature values**: Robust to outliers via Huber loss
4. **Small datasets**: Early stopping prevents overfitting

### User Responsibility

1. **Missing features**: Impute before calling engine
2. **New feature values**: Extrapolation may be unreliable
3. **Equipment type mismatch**: Model trained on Type A won't work for Type B
4. **Catastrophic failures**: Unpredictable events (e.g., lightning) not captured

## Testing

Comprehensive test suite in `ml-service/tests/test_equipment_rul.py`:

```bash
cd ml-service
pytest tests/test_equipment_rul.py -v
```

**Test Coverage:**
- Initialization and configuration
- Training with various hyperparameters
- Prediction accuracy and bounds
- Confidence interval validity
- Feature importance computation
- Model persistence (save/load)
- Edge cases and error handling

## Example Output

```
Training complete - Val MAE: 8.45 days, Val RMSE: 12.33 days, Val R²: 0.8765

Top Features (by importance):
1. wear_indicator        0.3245 (32.5%)
2. operating_hours       0.2187 (21.9%)
3. vibration            0.1876 (18.8%)
4. cycle_count          0.1432 (14.3%)
5. temperature          0.1260 (12.6%)

Predictions with Confidence Intervals:
Scenario         Prediction              95% CI      Width
----------------------------------------------------------------
New Equipment        345.2     [325.8, 364.6]       38.8
Mid-Life            182.7     [168.3, 197.1]       28.8
Near Failure         12.4      [  5.2,  19.6]       14.4
High Stress          45.8      [ 32.1,  59.5]       27.4
```

## Integration with ML Service

The engine integrates with the broader ML service architecture:

```python
# In ml-service/app/services/inference.py
from app.engines.equipment_rul import EquipmentRULEngine

# Load model
engine = EquipmentRULEngine(model_id=model.id)
engine.load_model(model.model_path)

# Make prediction
predictions = engine.predict(telemetry_df, model.feature_columns)
pred, lower, upper = engine.predict_with_confidence(
    telemetry_df,
    model.feature_columns
)

# Return result
return PredictiveMaintenanceResult(
    device_id=device_id,
    model_id=model.id,
    days_to_maintenance=int(predictions[0]),
    confidence_lower=int(lower[0]),
    confidence_upper=int(upper[0]),
    prediction_timestamp=datetime.utcnow()
)
```

## References

- Scikit-learn Gradient Boosting: https://scikit-learn.org/stable/modules/ensemble.html#gradient-boosting
- Predictive Maintenance Overview: https://en.wikipedia.org/wiki/Predictive_maintenance
- RUL Estimation Techniques: IEEE PHM Data Challenge

## License

Part of the Industrial Cloud ML Service. See project LICENSE.
