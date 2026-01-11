# ML Training User Guide

**Version**: 1.0.0
**Last Updated**: 2026-01-10
**Audience**: End Users, Data Scientists, System Administrators

---

## Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [Training Your First Model](#training-your-first-model)
4. [Understanding Training Progress](#understanding-training-progress)
5. [Managing Training Jobs](#managing-training-jobs)
6. [Using the Dashboard Time Range Selector](#using-the-dashboard-time-range-selector)
7. [Troubleshooting](#troubleshooting)
8. [FAQ](#faq)

---

## Overview

The **ML Training Pipeline** in SensorVision enables you to train machine learning models on your device telemetry data for:

- **Anomaly Detection** - Identify unusual patterns in your sensor data
- **Predictive Maintenance** - Predict equipment failures before they happen
- **Energy Forecasting** - Forecast energy consumption for the next 7 days
- **Equipment RUL** - Estimate Remaining Useful Life of your equipment

### Key Features

- **Real-time Training Progress** - Monitor your training jobs with live updates
- **One-click Training** - Start training from the ML Models page
- **Automatic Model Management** - Models are automatically updated on successful training
- **Background Processing** - Training continues even if you close the browser
- **Cancellation Support** - Cancel running jobs at any time

---

## Getting Started

### Accessing the ML Pipeline

1. **Login** to SensorVision with your credentials
2. **Navigate** to ML Pipeline:
   - Click **ML Pipeline** in the left sidebar
   - Select **ML Models** to manage models
   - Select **Anomalies** to view detected anomalies

### Prerequisites

Before training a model, ensure you have:
- At least one device sending telemetry data
- Sufficient historical data (recommended: 7+ days of data)
- A created ML model with configured parameters

---

## Training Your First Model

### Step 1: Create a Model

1. Go to **ML Pipeline > ML Models**
2. Click **Create Model** button
3. Fill in the model details:
   - **Name**: A descriptive name (e.g., "Temperature Anomaly Detector")
   - **Type**: Select the model type (Anomaly Detection, Predictive Maintenance, etc.)
   - **Algorithm**: Choose the algorithm (e.g., Isolation Forest for anomaly detection)
   - **Device Scope**: Select which devices to include in training
   - **Feature Columns**: Select which variables to use for training
4. Click **Save** to create the model

### Step 2: Start Training

1. In the ML Models list, find your model
2. Click the **Train** button (play icon)
3. The Training Progress Modal will open automatically

### Step 3: Monitor Progress

The Training Progress Modal shows:
- **Progress Bar**: Visual indicator of training completion (0-100%)
- **Current Step**: What the training is currently doing (e.g., "Feature engineering", "Model fitting")
- **Data Statistics**: Number of records and devices used for training
- **Timing**: When training started and estimated completion

---

## Understanding Training Progress

### Training Stages

Training typically goes through these stages:

1. **Initializing** (0-5%)
   - Validating model configuration
   - Connecting to ML service

2. **Data Loading** (5-20%)
   - Fetching telemetry data from database
   - Filtering by device scope

3. **Feature Engineering** (20-40%)
   - Creating features from raw data
   - Calculating rolling statistics
   - Handling missing values

4. **Model Training** (40-90%)
   - Fitting the algorithm to data
   - Cross-validation (if enabled)
   - Hyperparameter optimization

5. **Model Validation** (90-95%)
   - Calculating performance metrics
   - Generating validation reports

6. **Saving Model** (95-100%)
   - Persisting trained model to storage
   - Updating database records

### Status Indicators

| Icon | Status | Meaning |
|------|--------|---------|
| Clock (Yellow) | PENDING | Job queued, waiting to start |
| Spinner (Blue) | RUNNING | Training in progress |
| Checkmark (Green) | COMPLETED | Training finished successfully |
| X (Red) | FAILED | Training encountered an error |
| Stop (Gray) | CANCELLED | Job was cancelled by user |

---

## Managing Training Jobs

### Cancelling a Training Job

If you need to stop a training job:

1. In the Training Progress Modal, click **Cancel Training**
2. Confirm the cancellation when prompted
3. The job will be marked as CANCELLED
4. Your model will revert to its previous state

**Note**: Cancelled jobs cannot be resumed. You'll need to start a new training job.

### Closing the Modal

You can close the Training Progress Modal while training is still running:

1. Click the **X** button or **Close (Continue in Background)**
2. Confirm that you want to close
3. Training will continue in the background
4. Return to ML Models to check status later

The training job will complete even if your browser is closed.

### Viewing Past Training Jobs

To see the history of training jobs for a model:

1. Go to **ML Pipeline > ML Models**
2. Click on a model row to view details
3. The **Training History** section shows all past jobs
4. Click on any job to see detailed information

---

## Using the Dashboard Time Range Selector

The Dashboard now includes a **Historical Metrics Panel** at the top that displays aggregated statistics for your device data.

### How to Use

1. Go to the **Dashboard**
2. Look for the **Historical Metrics** panel at the top
3. Use the dropdown to select a time range:
   - **1 hour** - Last 60 minutes
   - **6 hours** - Last 6 hours
   - **12 hours** - Last 12 hours
   - **24 hours** - Last 24 hours

### Metrics Displayed

The panel shows aggregated statistics for:

| Metric | Description |
|--------|-------------|
| **Power (kW)** | Average, minimum, and maximum power consumption |
| **Voltage (V)** | Average, minimum, and maximum voltage levels |
| **Current (A)** | Average, minimum, and maximum current readings |

These metrics help you quickly understand the operating characteristics of your devices over the selected time period.

---

## Troubleshooting

### Training Fails to Start

**Symptoms**: Clicking "Train" shows an error immediately

**Possible Causes**:
- Model already has an active training job
- ML Service is not running
- Insufficient data for training

**Solutions**:
1. Wait for any existing training to complete or cancel it
2. Contact your administrator to check ML service status
3. Ensure devices have been sending data for at least 24 hours

### Training Gets Stuck

**Symptoms**: Progress stuck at same percentage for a long time

**Possible Causes**:
- ML Service is processing a large dataset
- Network connectivity issues
- Service timeout

**Solutions**:
1. Wait a few more minutes (large datasets take longer)
2. Click **Refresh** button to force a status update
3. If stuck for >1 hour, consider cancelling and retrying

### Training Fails with Error

**Symptoms**: Status shows FAILED with error message

**Common Errors**:

| Error | Cause | Solution |
|-------|-------|----------|
| "Insufficient data" | Not enough historical data | Wait for more data to accumulate |
| "Feature extraction failed" | Missing or invalid variables | Check that feature columns exist |
| "Model fitting failed" | Algorithm cannot converge | Try different hyperparameters |
| "Connection timeout" | ML service unavailable | Contact administrator |

### Model Not Updating After Training

**Symptoms**: Training completes but model still shows old status

**Solutions**:
1. Refresh the page
2. Wait a few seconds for database sync
3. Clear browser cache and reload

---

## FAQ

### How long does training take?

Training time depends on:
- Amount of data (more data = longer training)
- Model complexity (simple algorithms are faster)
- Number of features (more features = longer training)

Typical training times:
- Small dataset (<10,000 records): 1-5 minutes
- Medium dataset (10,000-100,000 records): 5-15 minutes
- Large dataset (>100,000 records): 15-60 minutes

### Can I train multiple models at once?

Yes, but with limitations:
- Each model can only have one active training job
- The system queues multiple training requests
- Jobs are processed based on available resources

### What happens if I lose internet connection during training?

Training continues on the server regardless of your connection. When you reconnect:
1. Go back to ML Models page
2. Find your model
3. The status will show the current state (RUNNING, COMPLETED, or FAILED)

### How often should I retrain my models?

Recommendations:
- **Anomaly Detection**: Weekly or when significant operational changes occur
- **Predictive Maintenance**: Monthly or after maintenance events
- **Energy Forecasting**: Weekly to capture seasonal patterns
- **Equipment RUL**: After maintenance or calibration

### Can I schedule automatic retraining?

Not currently available. This feature is planned for a future release. For now, you need to manually trigger retraining when needed.

### Where are trained models stored?

Trained models are stored:
- Model files: On the ML service server (`/models/{modelId}.joblib`)
- Metadata: In the PostgreSQL database
- Training logs: Retained for 30 days

---

## Related Documentation

- **Developer Guide**: See `docs/ML_PIPELINE.md` for technical architecture
- **API Reference**: See `/api/v1/ml/training-jobs/*` endpoints
- **Model Configuration**: See `docs/ML_MODELS.md` for model parameters

---

**Need Help?**

If you encounter issues not covered in this guide:
1. Check the [Troubleshooting](#troubleshooting) section
2. Contact your system administrator
3. Submit a support ticket via the Support page

---

**Last Updated**: 2026-01-10
**Version**: 1.0.0
