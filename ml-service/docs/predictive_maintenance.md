# Predictive Maintenance Engine

## Overview

The Predictive Maintenance Engine uses Random Forest Classification to detect equipment failure 24-48 hours in advance. It analyzes time-series telemetry patterns, automatically engineers features from rolling statistics and trend indicators, and provides actionable predictions with confidence scores.

## Algorithm: Random Forest Classifier

**Why Random Forest?**
- Handles non-linear relationships in equipment degradation
- Resistant to overfitting with proper hyperparameter tuning
- Provides feature importance for root cause analysis
- Robust to outliers and missing values
- Works well with imbalanced datasets (few failures vs. many normal samples)

**Time Complexity:**
- Training: O(n_trees × n_samples × log(n_samples) × n_features)
- Prediction: O(n_trees × log(n_samples) × n_features)

**Space Complexity:**
- O(n_trees × n_nodes × n_features)

## Feature Engineering

The engine automatically transforms raw telemetry into rich time-series features:

### Rolling Statistics (per window)
- **Mean**: Average value over window
- **Std**: Variance/stability indicator
- **Max/Min**: Extreme value detection
- **Range**: Max - Min, volatility measure

### Trend Indicators
- **Diff_1**: Short-term rate of change
- **Diff_5**: Medium-term rate of change
- **Slope_5/10**: Linear trend over window
- **Skew**: Distribution asymmetry (for larger windows)
- **Kurt**: Tail heaviness (for larger windows)

### Default Window Sizes
- 5 samples: Detect immediate changes
- 10 samples: Capture short-term patterns
- 20 samples: Identify long-term trends

**Example:** For 3 raw variables and windows [5, 10, 20], the engine creates:
- 3 variables × 5 features per window × 3 windows = 45 rolling features
- 3 variables × 2 diff features = 6 trend features
- 3 variables × 2 slope features = 6 slope features
- 3 variables × 2 moments × 2 windows = 12 statistical features
- **Total: ~70 engineered features** from 3 raw inputs

## Hyperparameters

### Default Configuration

```python
{
    "n_estimators": 100,        # Number of trees in forest
    "max_depth": 10,            # Maximum tree depth (prevents overfitting)
    "min_samples_split": 5,     # Minimum samples to split node
    "min_samples_leaf": 2,      # Minimum samples at leaf
    "max_features": "sqrt",     # Features considered per split
    "window_sizes": [5, 10, 20],# Rolling window sizes
    "threshold": 0.5,           # Failure probability threshold
    "random_state": 42,         # Reproducibility seed
}
```

### Tuning Guidelines

**For High Precision (minimize false alarms):**
```python
{
    "n_estimators": 150,
    "max_depth": 8,
    "threshold": 0.65,  # Higher threshold = fewer false positives
}
```

**For High Recall (catch all failures):**
```python
{
    "n_estimators": 100,
    "max_depth": 12,
    "threshold": 0.35,  # Lower threshold = catch more failures
}
```

**For Imbalanced Data (1-5% failure rate):**
```python
{
    "n_estimators": 200,
    "max_depth": 10,
    "min_samples_leaf": 1,
    # Note: Engine automatically uses class_weight="balanced"
}
```

## API Usage

### Training

```python
from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
from uuid import uuid4

# Initialize engine
engine = PredictiveMaintenanceEngine(model_id=uuid4())

# Prepare training data
# Required columns: raw telemetry + failure label (0=normal, 1=failure)
training_data = pd.DataFrame({
    "temperature": [...],
    "vibration": [...],
    "pressure": [...],
    "failure": [0, 0, 0, 1, 1, ...],  # Binary labels
})

# Train model
metrics = engine.train(
    data=training_data,
    feature_columns=["temperature", "vibration", "pressure"],
    target_column="failure",
    hyperparameters={
        "n_estimators": 100,
        "window_sizes": [5, 10, 20],
        "threshold": 0.5,
    }
)

# Review metrics
print(f"Accuracy: {metrics['accuracy']:.3f}")
print(f"Recall: {metrics['recall']:.3f}")  # Most critical for maintenance
print(f"F1 Score: {metrics['f1_score']:.3f}")
```

### Prediction with Probability Scores

```python
# Prepare telemetry data (no labels needed)
test_data = pd.DataFrame({
    "temperature": [...],
    "vibration": [...],
    "pressure": [...],
})

# Get predictions with detailed analysis
labels, probabilities, details = engine.predict_with_probability(
    data=test_data,
    feature_columns=["temperature", "vibration", "pressure"]
)

# Process results
for i, (label, prob, detail) in enumerate(zip(labels, probabilities, details)):
    if label == 1:  # Failure predicted
        print(f"⚠️  Equipment {i}: FAILURE IMMINENT")
        print(f"   Probability: {detail['failure_probability']:.1%}")
        print(f"   Risk Level: {detail['risk_level']}")
        print(f"   Days to Failure: {detail['days_to_failure']}")
        print(f"   Top Risk Factors: {detail['top_risk_factors']}")
```

### Model Persistence

```python
# Save trained model
model_path = engine.save_model("/path/to/model.joblib")

# Load model later
new_engine = PredictiveMaintenanceEngine(model_id=uuid4())
new_engine.load_model("/path/to/model.joblib")

# Model is ready for predictions
labels = new_engine.predict(test_data, feature_columns)
```

## Training Metrics Explained

### Classification Metrics

**Accuracy**: Overall correctness (TP + TN) / Total
- Good for balanced datasets
- Can be misleading with imbalanced data

**Precision**: TP / (TP + FP)
- "When I predict failure, how often am I right?"
- High precision = fewer false alarms
- Important for maintenance resource allocation

**Recall**: TP / (TP + FN)
- "Of all actual failures, how many did I catch?"
- High recall = fewer missed failures
- **MOST CRITICAL for safety-critical equipment**
- Prioritize recall over precision for critical assets

**F1 Score**: Harmonic mean of precision and recall
- Balanced metric when both false alarms and missed failures matter
- Good for comparing models

### Confusion Matrix

```
                Predicted
              Normal  Failure
Actual Normal   TN      FP     (False Positive = false alarm)
     Failure    FN      TP     (False Negative = missed failure)
```

**True Positives (TP)**: Correctly predicted failures
**True Negatives (TN)**: Correctly predicted normal operation
**False Positives (FP)**: False alarms (predicted failure, but equipment OK)
**False Negatives (FN)**: Missed failures (predicted OK, but failed) - **WORST CASE**

### Feature Importance

Indicates which engineered features contribute most to predictions:

```python
metrics['feature_importance_top_10']
# {
#   'temperature_mean_20': 0.156,
#   'vibration_std_10': 0.142,
#   'pressure_slope_5': 0.098,
#   ...
# }
```

**Use for:**
- Root cause analysis: "What variables indicate failure?"
- Sensor prioritization: Focus monitoring on important variables
- Feature selection: Remove low-importance features
- Domain insight: Validate against engineering knowledge

## Prediction Output Details

### Failure Probability
- 0.0 - 0.3: Low risk, normal operation
- 0.3 - 0.5: Medium risk, monitor closely
- 0.5 - 0.75: High risk, schedule inspection
- 0.75 - 1.0: Critical risk, immediate action required

### Days to Failure Estimation
- Based on probability score
- High probability (≥0.5): 1-2 days
- Represents prediction horizon, not exact failure time
- Use for maintenance scheduling window

### Risk Level Classification
- **LOW**: probability < 0.3
- **MEDIUM**: 0.3 ≤ probability < 0.5
- **HIGH**: 0.5 ≤ probability < 0.75
- **CRITICAL**: probability ≥ 0.75

### Top Risk Factors
- Engineered features with highest contribution
- Combination of feature importance and current value
- Use for diagnostic investigation

## Production Considerations

### Data Requirements

**Minimum Training Data:**
- At least 500 samples for reliable training
- 100+ failure cases (if available)
- For imbalanced data: use class_weight="balanced" (automatic)

**Data Quality:**
- Time-ordered samples (for rolling statistics)
- No excessive missing values (< 10% per variable)
- Representative of operational conditions
- Include both normal and pre-failure patterns

**Label Quality:**
- Label 24-48 hours **before** actual failure (not at failure time)
- Consistent labeling criteria across equipment
- Consider gradual degradation vs. sudden failures

### Handling Imbalanced Data

The engine automatically uses `class_weight="balanced"` to handle:
- Typical failure rates: 1-10% of samples
- Prevents model from just predicting "normal" for everything
- Adjusts decision boundary to catch more failures

**If recall is still too low:**
```python
hyperparameters = {
    "threshold": 0.3,  # Lower threshold catches more failures
    "n_estimators": 200,  # More trees for rare pattern detection
}
```

### Monitoring Model Performance

**Retraining Triggers:**
- Recall drops below 80% (missing too many failures)
- Precision drops below 30% (too many false alarms)
- Equipment modifications or operational changes
- New failure modes observed
- At least quarterly for production systems

**Validation Strategy:**
- Time-based split: Train on old data, validate on recent
- Never shuffle time-series data randomly
- Monitor false negatives closely (missed failures)

### Scalability Limits

**Memory:**
- ~1 MB per 100 trees with 100 features
- Example: 200 trees × 70 features ≈ 1.5 MB model

**Prediction Latency:**
- < 10 ms for batch of 1000 samples
- Suitable for real-time monitoring dashboards

**Training Time:**
- ~10 seconds for 10k samples, 100 trees (8 cores)
- ~1 minute for 100k samples

## Integration with ML Service

### Model Type
```python
MLModelType.PREDICTIVE_MAINTENANCE
```

### REST API Endpoints

**Training:**
```
POST /api/v1/training/jobs
{
  "model_id": "uuid",
  "organization_id": 1,
  "training_config": {
    "feature_columns": ["temperature", "vibration"],
    "target_column": "failure",
    "hyperparameters": {...}
  }
}
```

**Inference:**
```
POST /api/v1/inference
{
  "device_id": "uuid",
  "model_id": "uuid",
  "telemetry": [
    {"timestamp": "2024-01-01T00:00:00Z", "variables": {"temperature": 70, ...}},
    ...
  ]
}
```

**Response:**
```json
{
  "device_id": "uuid",
  "model_id": "uuid",
  "prediction_type": "PREDICTIVE_MAINTENANCE",
  "prediction_label": "FAILURE",
  "confidence": 0.85,
  "prediction_details": {
    "maintenance_probability": 0.85,
    "days_to_maintenance": 1,
    "recommended_actions": ["Inspect bearings", "Check temperature sensor"],
    "risk_factors": {
      "temperature_mean_20": 0.234,
      "vibration_std_10": 0.198
    }
  }
}
```

## Troubleshooting

### Model Always Predicts Normal (Low Recall)

**Cause:** Imbalanced data, model learned to ignore failures

**Solutions:**
1. Lower threshold: `{"threshold": 0.3}`
2. Check failure label quality (labeled at right time?)
3. Verify failure samples have distinct patterns
4. Increase tree depth: `{"max_depth": 15}`

### Too Many False Alarms (Low Precision)

**Cause:** Model too sensitive, threshold too low

**Solutions:**
1. Raise threshold: `{"threshold": 0.6}`
2. Reduce max_depth: `{"max_depth": 8}`
3. Increase min_samples_leaf: `{"min_samples_leaf": 5}`
4. Add more training data

### Poor Performance on New Equipment

**Cause:** Distribution shift, different operational patterns

**Solutions:**
1. Retrain with new equipment data
2. Use ensemble of models (per equipment type)
3. Implement online learning (periodic retraining)

### Engineered Features Cause Overfitting

**Symptoms:** High training accuracy, low test accuracy

**Solutions:**
1. Reduce window sizes: `{"window_sizes": [5, 10]}`
2. Limit tree depth: `{"max_depth": 6}`
3. Increase min_samples_split: `{"min_samples_split": 10}`
4. Reduce max_features: `{"max_features": 0.5}`

## References

- Scikit-learn Random Forest Documentation
- "Predictive Maintenance for Industry 4.0" (2019)
- Time-Series Feature Engineering Best Practices
- Class Imbalance in ML: Strategies and Techniques
