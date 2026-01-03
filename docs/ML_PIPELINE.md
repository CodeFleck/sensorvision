# Machine Learning Pipeline

Comprehensive ML pipeline for predictive analytics, anomaly detection, and intelligent monitoring.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    SensorVision ML Architecture                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │   Frontend   │───▶│  Spring Boot │───▶│  ML Service  │       │
│  │   (React)    │    │   Backend    │    │  (FastAPI)   │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│                             │                    │               │
│                             ▼                    ▼               │
│                    ┌──────────────┐    ┌──────────────┐         │
│                    │  PostgreSQL  │    │ Model Store  │         │
│                    │  (ML Tables) │    │  (joblib)    │         │
│                    └──────────────┘    └──────────────┘         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Components

### 1. Python ML Service (`ml-service/`)

FastAPI microservice for model training and inference.

**Directory Structure:**
```
ml-service/
├── app/
│   ├── api/routes/
│   │   ├── health.py      # Health check endpoints
│   │   ├── inference.py   # Prediction endpoints
│   │   ├── models.py      # Model management
│   │   └── training.py    # Training endpoints
│   ├── core/
│   │   ├── config.py      # Configuration settings
│   │   ├── logging.py     # Logging setup
│   │   └── security.py    # API key auth
│   ├── engines/
│   │   ├── base.py        # BaseMLEngine abstract class
│   │   └── anomaly_detection.py  # Anomaly detection
│   ├── models/
│   │   └── schemas.py     # Pydantic models
│   └── main.py            # FastAPI app
├── tests/                  # Pytest tests
├── requirements.txt
└── Dockerfile
```

**Running the ML Service:**
```bash
cd ml-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

### 2. Java Backend Services

**MLModelService** (`src/main/java/io/indcloud/service/ml/MLModelService.java`)
- Model CRUD operations
- Training lifecycle management (DRAFT → TRAINING → TRAINED → DEPLOYED)
- Version management with auto-increment on retrain
- Deployment and archival

**MLAnomalyService** (`src/main/java/io/indcloud/service/ml/MLAnomalyService.java`)
- Anomaly management and tracking
- Severity classification (LOW, MEDIUM, HIGH, CRITICAL)
- Workflow: NEW → ACKNOWLEDGED → INVESTIGATING → RESOLVED/FALSE_POSITIVE
- False positive feedback for model improvement

**MLServiceClient** (`src/main/java/io/indcloud/service/ml/MLServiceClient.java`)
- HTTP client for Python ML service
- Training job orchestration
- Inference requests

### 3. Database Entities

**MLModel** - ML model configuration and metadata
```java
@Entity
public class MLModel {
    UUID id;
    String name;
    MLModelType modelType;      // ANOMALY_DETECTION, PREDICTIVE_MAINTENANCE, etc.
    String algorithm;           // isolation_forest, z_score, etc.
    String version;             // Semantic versioning
    MLModelStatus status;       // DRAFT, TRAINING, TRAINED, DEPLOYED, ARCHIVED, FAILED
    Map<String, Object> hyperparameters;
    List<String> featureColumns;
    String targetColumn;
    Map<String, Object> trainingMetrics;
    Map<String, Object> validationMetrics;
    String modelPath;           // Path to saved model file
    Long modelSizeBytes;
    String deviceScope;         // ALL, SPECIFIC, GROUP
    List<UUID> deviceIds;
    Long deviceGroupId;
    String inferenceSchedule;   // Cron expression
    Instant lastInferenceAt;
    Instant nextInferenceAt;
    BigDecimal confidenceThreshold;
    BigDecimal anomalyThreshold;
}
```

**MLAnomaly** - Detected anomalies
```java
@Entity
public class MLAnomaly {
    UUID id;
    MLPrediction prediction;
    Device device;
    Organization organization;
    BigDecimal anomalyScore;
    String anomalyType;
    MLAnomalySeverity severity;
    MLAnomalyStatus status;
    List<String> affectedVariables;
    Map<String, Object> expectedValues;
    Map<String, Object> actualValues;
    Instant detectedAt;
    Instant acknowledgedAt;
    Instant resolvedAt;
    String resolutionNote;
}
```

**MLPrediction** - Inference results
```java
@Entity
public class MLPrediction {
    UUID id;
    MLModel model;
    Device device;
    Map<String, Object> inputData;
    Map<String, Object> outputData;
    BigDecimal confidenceScore;
    Instant predictionTimestamp;
    Long inferenceTimeMs;
    String feedbackLabel;       // For model improvement
}
```

## ML Engines

### 1. Anomaly Detection Engine (✅ COMPLETE)

**Algorithms:**
- **Isolation Forest** - ML-based, learns normal patterns
- **Z-Score** - Statistical, threshold-based

**Usage:**
```python
from app.engines.anomaly_detection import AnomalyDetectionEngine
from uuid import uuid4

# Initialize
engine = AnomalyDetectionEngine(
    model_id=uuid4(),
    algorithm="isolation_forest"
)

# Train
metrics = engine.train(
    data=df,
    feature_columns=["temperature", "pressure", "vibration"],
    hyperparameters={"contamination": 0.1}
)

# Predict with scores
labels, scores, details = engine.predict_with_scores(
    data=new_data,
    feature_columns=["temperature", "pressure", "vibration"]
)

# Save model
engine.save_model("/models/anomaly_v1.joblib")
```

### 2. Predictive Maintenance Engine (🚧 PLANNED)

Detect equipment failure 24-48 hours in advance.

**Algorithms:**
- Random Forest Classifier
- Gradient Boosting
- LSTM for time-series patterns

**Features:**
- Rolling statistics (mean, std, max)
- Trend indicators
- Cycle detection

### 3. Energy Forecasting Engine (🚧 PLANNED)

Predict energy consumption for next 7 days.

**Algorithms:**
- Prophet (Facebook)
- ARIMA/SARIMA
- XGBoost with time features

**Features:**
- Hourly/daily patterns
- Weekly seasonality
- Holiday effects

### 4. Equipment RUL Engine (🚧 PLANNED)

Estimate Remaining Useful Life in days.

**Algorithms:**
- Survival Analysis (Cox PH)
- Weibull Distribution
- Deep learning (LSTM)

**Features:**
- Degradation curves
- Failure history
- Operating conditions

## REST API Endpoints

### Model Management

```
GET    /api/v1/ml/models                 # List models
POST   /api/v1/ml/models                 # Create model
GET    /api/v1/ml/models/{id}            # Get model
PUT    /api/v1/ml/models/{id}            # Update model
DELETE /api/v1/ml/models/{id}            # Delete model
POST   /api/v1/ml/models/{id}/train      # Start training
POST   /api/v1/ml/models/{id}/deploy     # Deploy model
POST   /api/v1/ml/models/{id}/archive    # Archive model
```

### Anomaly Management

```
GET    /api/v1/ml/anomalies              # List anomalies
GET    /api/v1/ml/anomalies/{id}         # Get anomaly
GET    /api/v1/ml/devices/{deviceId}/anomalies  # Device anomalies
POST   /api/v1/ml/anomalies/{id}/acknowledge    # Acknowledge
POST   /api/v1/ml/anomalies/{id}/investigate    # Start investigation
POST   /api/v1/ml/anomalies/{id}/resolve        # Resolve
POST   /api/v1/ml/anomalies/{id}/false-positive # Mark false positive
```

## Configuration

### Application Properties
```properties
# ML Service Configuration
ml.service.url=${ML_SERVICE_URL:http://localhost:8000}
ml.service.timeout=${ML_SERVICE_TIMEOUT:30}
ml.service.api-key=${ML_SERVICE_API_KEY:}

# ML Feature Flags
ml.anomaly-detection.enabled=${ML_ANOMALY_DETECTION_ENABLED:true}
ml.predictive-maintenance.enabled=${ML_PREDICTIVE_MAINTENANCE_ENABLED:true}
ml.energy-forecast.enabled=${ML_ENERGY_FORECAST_ENABLED:true}
ml.equipment-rul.enabled=${ML_EQUIPMENT_RUL_ENABLED:true}

# ML Inference Settings
ml.inference.batch-size=${ML_INFERENCE_BATCH_SIZE:100}
ml.inference.schedule.enabled=${ML_INFERENCE_SCHEDULE_ENABLED:true}
```

### ML Service Config (`ml-service/app/core/config.py`)
```python
class Settings(BaseSettings):
    API_KEY: str = ""
    MODEL_STORAGE_PATH: str = "./models"
    LOG_LEVEL: str = "INFO"
    MAX_TRAINING_SAMPLES: int = 100000
    INFERENCE_TIMEOUT: int = 30
```

## Model Lifecycle

```
┌──────┐    ┌──────────┐    ┌─────────┐    ┌──────────┐
│DRAFT │───▶│ TRAINING │───▶│ TRAINED │───▶│ DEPLOYED │
└──────┘    └──────────┘    └─────────┘    └──────────┘
    │              │                            │
    │              ▼                            ▼
    │         ┌────────┐                  ┌──────────┐
    └────────▶│ FAILED │                  │ ARCHIVED │
              └────────┘                  └──────────┘
```

1. **DRAFT** - Model configured but not trained
2. **TRAINING** - Training job in progress
3. **TRAINED** - Training complete, ready for deployment
4. **DEPLOYED** - Active, running inference on schedule
5. **ARCHIVED** - Inactive, preserved for reference
6. **FAILED** - Training failed

## Anomaly Workflow

```
┌─────┐    ┌──────────────┐    ┌───────────────┐    ┌──────────┐
│ NEW │───▶│ ACKNOWLEDGED │───▶│ INVESTIGATING │───▶│ RESOLVED │
└─────┘    └──────────────┘    └───────────────┘    └──────────┘
    │                                  │
    │                                  ▼
    │                          ┌───────────────┐
    └─────────────────────────▶│FALSE_POSITIVE │
                               └───────────────┘
```

## Testing

### Running ML Service Tests
```bash
cd ml-service
pytest                              # Run all tests
pytest -v                           # Verbose output
pytest --cov=app                    # With coverage
pytest tests/test_anomaly_detection.py  # Specific test
```

### Running Java Tests
```bash
./gradlew test --tests "*ML*"       # All ML tests
./gradlew test --tests "MLModelServiceTest"
./gradlew test --tests "MLAnomalyServiceTest"
```

## Deployment

### Docker Compose
```yaml
services:
  ml-service:
    build: ./ml-service
    ports:
      - "8000:8000"
    environment:
      - ML_SERVICE_API_KEY=${ML_SERVICE_API_KEY}
      - MODEL_STORAGE_PATH=/models
    volumes:
      - ml-models:/models

volumes:
  ml-models:
```

### Environment Variables
```bash
# Required
ML_SERVICE_URL=http://ml-service:8000
ML_SERVICE_API_KEY=your-secure-api-key

# Optional
ML_ANOMALY_DETECTION_ENABLED=true
ML_PREDICTIVE_MAINTENANCE_ENABLED=true
ML_INFERENCE_BATCH_SIZE=100
```

## Next Steps

1. **Sprint 2**: Implement remaining ML engines
   - Predictive Maintenance (Random Forest + LSTM)
   - Energy Forecasting (Prophet + XGBoost)
   - Equipment RUL (Survival Analysis)

2. **Sprint 3**: Frontend UI
   - ML Models management page
   - Training workflow wizard
   - Anomaly dashboard with charts
   - Prediction insights panel

3. **Future Enhancements**
   - AutoML for hyperparameter tuning
   - Model versioning and A/B testing
   - Real-time streaming inference
   - Edge deployment (TensorFlow Lite)

---

**Last Updated**: 2026-01-03
**Status**: Sprint 1 Complete (Foundation)
