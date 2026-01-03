-- ML Pipeline Database Schema
-- Supports: Anomaly Detection, Predictive Maintenance, Energy Forecasting, Equipment RUL

-- ML Models metadata and versioning
CREATE TABLE IF NOT EXISTS ml_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Model identification
    name VARCHAR(255) NOT NULL,
    model_type VARCHAR(50) NOT NULL,  -- ANOMALY_DETECTION, PREDICTIVE_MAINTENANCE, ENERGY_FORECAST, EQUIPMENT_RUL
    version VARCHAR(50) NOT NULL DEFAULT '1.0.0',

    -- Model configuration
    algorithm VARCHAR(100) NOT NULL,  -- isolation_forest, gradient_boosting, lstm, prophet, arima, survival
    hyperparameters JSONB DEFAULT '{}',
    feature_columns JSONB DEFAULT '[]',
    target_column VARCHAR(100),

    -- Model status
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',  -- DRAFT, TRAINING, TRAINED, DEPLOYED, ARCHIVED, FAILED

    -- Model storage
    model_path VARCHAR(500),
    model_size_bytes BIGINT,

    -- Performance metrics
    training_metrics JSONB DEFAULT '{}',
    validation_metrics JSONB DEFAULT '{}',

    -- Device scope
    device_scope VARCHAR(30) DEFAULT 'ALL',  -- ALL, SELECTED, GROUP
    device_ids JSONB DEFAULT '[]',
    device_group_id BIGINT,

    -- Scheduling
    inference_schedule VARCHAR(100) DEFAULT '0 0 * * * *',  -- Cron expression (default: hourly)
    last_inference_at TIMESTAMP WITH TIME ZONE,
    next_inference_at TIMESTAMP WITH TIME ZONE,

    -- Thresholds
    confidence_threshold DECIMAL(5, 4) DEFAULT 0.8,
    anomaly_threshold DECIMAL(10, 6) DEFAULT 0.5,

    -- Audit
    created_by UUID,
    trained_by UUID,
    trained_at TIMESTAMP WITH TIME ZONE,
    deployed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ML Predictions storage
CREATE TABLE IF NOT EXISTS ml_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id UUID NOT NULL REFERENCES ml_models(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Prediction details
    prediction_type VARCHAR(50) NOT NULL,  -- ANOMALY, MAINTENANCE, ENERGY, RUL
    prediction_value DECIMAL(20, 6),
    prediction_label VARCHAR(100),  -- NORMAL, ANOMALY, HIGH_RISK, NEEDS_MAINTENANCE, etc.
    confidence DECIMAL(5, 4),  -- 0.0000 to 1.0000

    -- Additional prediction data
    prediction_details JSONB DEFAULT '{}',

    -- Time context
    prediction_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    prediction_horizon VARCHAR(50),  -- For forecasts: 1h, 24h, 7d
    valid_until TIMESTAMP WITH TIME ZONE,

    -- Feedback for model improvement
    feedback_label VARCHAR(100),
    feedback_at TIMESTAMP WITH TIME ZONE,
    feedback_by UUID,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ML Anomalies (specialized table for anomaly detection results)
CREATE TABLE IF NOT EXISTS ml_anomalies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prediction_id UUID NOT NULL REFERENCES ml_predictions(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Anomaly details
    anomaly_score DECIMAL(10, 6) NOT NULL,
    anomaly_type VARCHAR(100) DEFAULT 'POINT_ANOMALY',  -- POINT_ANOMALY, CONTEXTUAL, COLLECTIVE
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',  -- LOW, MEDIUM, HIGH, CRITICAL

    -- Affected variables
    affected_variables JSONB DEFAULT '[]',
    expected_values JSONB DEFAULT '{}',
    actual_values JSONB DEFAULT '{}',

    -- Status tracking
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',  -- NEW, ACKNOWLEDGED, INVESTIGATING, RESOLVED, FALSE_POSITIVE
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by UUID,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID,
    resolution_note TEXT,

    -- Alert reference (if alert was triggered)
    global_alert_id UUID,

    -- Time context
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ML Training Jobs (for tracking training history)
CREATE TABLE IF NOT EXISTS ml_training_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id UUID REFERENCES ml_models(id) ON DELETE SET NULL,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Job details
    job_type VARCHAR(50) NOT NULL,  -- INITIAL_TRAINING, RETRAINING, HYPERPARAMETER_TUNING
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',  -- PENDING, RUNNING, COMPLETED, FAILED, CANCELLED

    -- Configuration
    training_config JSONB NOT NULL DEFAULT '{}',

    -- Data scope
    training_data_start TIMESTAMP WITH TIME ZONE,
    training_data_end TIMESTAMP WITH TIME ZONE,
    record_count BIGINT,
    device_count INTEGER,

    -- Progress tracking
    progress_percent INTEGER DEFAULT 0,
    current_step VARCHAR(100),

    -- Results
    result_metrics JSONB DEFAULT '{}',
    error_message TEXT,
    error_stack_trace TEXT,

    -- Timing
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,

    -- Audit
    triggered_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_ml_models_org ON ml_models(organization_id);
CREATE INDEX IF NOT EXISTS idx_ml_models_type ON ml_models(model_type);
CREATE INDEX IF NOT EXISTS idx_ml_models_status ON ml_models(status);
CREATE INDEX IF NOT EXISTS idx_ml_models_next_inference ON ml_models(next_inference_at) WHERE status = 'DEPLOYED';

CREATE INDEX IF NOT EXISTS idx_ml_predictions_model ON ml_predictions(model_id);
CREATE INDEX IF NOT EXISTS idx_ml_predictions_device ON ml_predictions(device_id);
CREATE INDEX IF NOT EXISTS idx_ml_predictions_timestamp ON ml_predictions(prediction_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_ml_predictions_type ON ml_predictions(prediction_type);
CREATE INDEX IF NOT EXISTS idx_ml_predictions_device_time ON ml_predictions(device_id, prediction_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_ml_predictions_org ON ml_predictions(organization_id);

CREATE INDEX IF NOT EXISTS idx_ml_anomalies_device ON ml_anomalies(device_id);
CREATE INDEX IF NOT EXISTS idx_ml_anomalies_status ON ml_anomalies(status);
CREATE INDEX IF NOT EXISTS idx_ml_anomalies_severity ON ml_anomalies(severity);
CREATE INDEX IF NOT EXISTS idx_ml_anomalies_detected ON ml_anomalies(detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_ml_anomalies_org ON ml_anomalies(organization_id);
CREATE INDEX IF NOT EXISTS idx_ml_anomalies_new ON ml_anomalies(organization_id, status) WHERE status = 'NEW';

CREATE INDEX IF NOT EXISTS idx_ml_training_jobs_model ON ml_training_jobs(model_id);
CREATE INDEX IF NOT EXISTS idx_ml_training_jobs_status ON ml_training_jobs(status);
CREATE INDEX IF NOT EXISTS idx_ml_training_jobs_org ON ml_training_jobs(organization_id);
CREATE INDEX IF NOT EXISTS idx_ml_training_jobs_created ON ml_training_jobs(created_at DESC);

-- Comments
COMMENT ON TABLE ml_models IS 'ML model metadata, versioning, and deployment tracking';
COMMENT ON TABLE ml_predictions IS 'Predictions from ML models with confidence scores';
COMMENT ON TABLE ml_anomalies IS 'Detected anomalies with status tracking and resolution workflow';
COMMENT ON TABLE ml_training_jobs IS 'Training job history with progress tracking and results';

COMMENT ON COLUMN ml_models.model_type IS 'Type of ML model: ANOMALY_DETECTION, PREDICTIVE_MAINTENANCE, ENERGY_FORECAST, EQUIPMENT_RUL';
COMMENT ON COLUMN ml_models.algorithm IS 'ML algorithm: isolation_forest, z_score, gradient_boosting, lstm, prophet, arima, survival';
COMMENT ON COLUMN ml_models.device_scope IS 'Scope of devices: ALL (org-wide), SELECTED (specific devices), GROUP (device group)';
COMMENT ON COLUMN ml_anomalies.anomaly_type IS 'Type of anomaly: POINT_ANOMALY (single point), CONTEXTUAL (context-dependent), COLLECTIVE (pattern)';
