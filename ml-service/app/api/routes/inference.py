"""
ML Inference endpoints.

Provides REST API endpoints for running ML inference on telemetry data.
Supports anomaly detection, predictive maintenance, energy forecasting,
and equipment RUL estimation.
"""
import logging
import time
from datetime import datetime, timezone
from typing import Dict, List, Optional
from uuid import UUID

import numpy as np
import pandas as pd
from fastapi import APIRouter, Depends, HTTPException, Query

from app.core.config import settings
from app.core.security import verify_api_key
from app.engines.anomaly_detection import AnomalyDetectionEngine
from app.engines.energy_forecasting import EnergyForecastingEngine
from app.engines.equipment_rul import EquipmentRULEngine
from app.engines.predictive_maintenance import PredictiveMaintenanceEngine
from app.models.schemas import (
    AnomalyDetectionResult,
    AnomalySeverity,
    BatchInferenceRequest,
    BatchInferenceResponse,
    InferenceRequest,
    InferenceResponse,
    MLModelType,
    PredictiveMaintenanceResult,
    TelemetryPoint,
)
from app.services.model_loader import model_loader, ModelLoadError, ModelNotFoundError

router = APIRouter(dependencies=[Depends(verify_api_key)])
logger = logging.getLogger(__name__)

# Use the global singleton model loader instance
_model_loader = model_loader


def _safe_error_message(message: str, exception: Exception) -> str:
    """
    Return error message, sanitizing details in production.

    In development, includes exception details for debugging.
    In production, returns only the generic message to prevent information disclosure.
    """
    if settings.is_production:
        return message
    return f"{message}: {str(exception)}"


def telemetry_to_dataframe(telemetry: List[TelemetryPoint]) -> pd.DataFrame:
    """
    Convert list of TelemetryPoint to pandas DataFrame.

    Args:
        telemetry: List of telemetry points with timestamp and variables.

    Returns:
        DataFrame with timestamp column and variable columns.
    """
    rows = []
    for point in telemetry:
        row = {"timestamp": point.timestamp, **point.variables}
        rows.append(row)

    df = pd.DataFrame(rows)

    # Ensure timestamp is datetime
    if "timestamp" in df.columns:
        df["timestamp"] = pd.to_datetime(df["timestamp"])
        df = df.sort_values("timestamp").reset_index(drop=True)

    return df


def get_feature_columns(df: pd.DataFrame) -> List[str]:
    """
    Extract feature columns from DataFrame (exclude timestamp).

    Args:
        df: DataFrame with telemetry data.

    Returns:
        List of feature column names.
    """
    return [col for col in df.columns if col != "timestamp"]


@router.post("/anomaly", response_model=AnomalyDetectionResult)
async def detect_anomaly(request: InferenceRequest):
    """
    Run anomaly detection on telemetry data.

    Takes telemetry data and returns anomaly score, classification,
    severity level, and affected variables.

    Args:
        request: Inference request with device_id, model_id, and telemetry data.

    Returns:
        AnomalyDetectionResult with anomaly details.

    Raises:
        HTTPException: 404 if model not found, 400 if inference fails.
    """
    logger.info(f"Anomaly detection for device {request.device_id}")

    if not request.model_id:
        raise HTTPException(status_code=400, detail="model_id is required")

    try:
        # Load model
        engine: AnomalyDetectionEngine = _model_loader.get_model(
            model_id=request.model_id,
            model_type=MLModelType.ANOMALY_DETECTION,
        )

        # Convert telemetry to DataFrame
        df = telemetry_to_dataframe(request.telemetry)
        feature_columns = get_feature_columns(df)

        if not feature_columns:
            raise HTTPException(status_code=400, detail="No feature columns in telemetry")

        # Run inference - use the last data point for single prediction
        labels, scores, details = engine.predict_with_scores(df, feature_columns)

        # Use the most recent result (last row)
        idx = -1
        is_anomaly = labels[idx] == -1
        anomaly_score = float(scores[idx])
        severity = engine.get_severity(anomaly_score)
        detail = details[idx]

        return AnomalyDetectionResult(
            device_id=request.device_id,
            model_id=request.model_id,
            prediction_type="ANOMALY_DETECTION",
            prediction_value=anomaly_score,
            prediction_label="ANOMALY" if is_anomaly else "NORMAL",
            confidence=1.0 - anomaly_score if not is_anomaly else anomaly_score,
            prediction_details={
                "algorithm": engine.algorithm,
                "samples_analyzed": len(df),
            },
            prediction_timestamp=datetime.now(timezone.utc),
            anomaly_score=anomaly_score,
            is_anomaly=is_anomaly,
            severity=severity,
            affected_variables=detail.get("affected_variables", []),
            expected_values=detail.get("expected_values", {}),
            actual_values=detail.get("actual_values", {}),
        )

    except ModelNotFoundError:
        raise HTTPException(status_code=404, detail="Model not found")
    except ModelLoadError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Model load failed", e))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid request", e))
    except Exception as e:
        logger.exception("Anomaly detection failed")
        raise HTTPException(status_code=500, detail=_safe_error_message("Inference failed", e))


@router.post("/anomaly/batch", response_model=BatchInferenceResponse)
async def detect_anomalies_batch(request: BatchInferenceRequest):
    """
    Run batch anomaly detection on multiple devices.

    Optimized for scheduled inference across many devices.
    Each device's telemetry should be provided separately.

    Args:
        request: Batch request with model_id and list of device_ids.

    Returns:
        BatchInferenceResponse with predictions for all devices.

    Raises:
        HTTPException: 404 if model not found.
    """
    logger.info(f"Batch anomaly detection for {len(request.device_ids)} devices")
    start_time = time.time()

    try:
        # Load model once for all devices
        engine: AnomalyDetectionEngine = _model_loader.get_model(
            model_id=request.model_id,
            model_type=MLModelType.ANOMALY_DETECTION,
        )

        predictions: List[InferenceResponse] = []
        errors: List[Dict] = []

        for device_id in request.device_ids:
            try:
                # Note: In a full implementation, we would fetch telemetry
                # from the database for each device. For now, we create
                # a placeholder response indicating the device was processed.
                # The actual telemetry would come from the Spring Boot backend.

                predictions.append(
                    AnomalyDetectionResult(
                        device_id=device_id,
                        model_id=request.model_id,
                        prediction_type="ANOMALY_DETECTION",
                        prediction_value=0.0,
                        prediction_label="PENDING",
                        confidence=0.0,
                        prediction_details={"status": "requires_telemetry_data"},
                        prediction_timestamp=datetime.now(timezone.utc),
                        anomaly_score=0.0,
                        is_anomaly=False,
                        severity=AnomalySeverity.LOW,
                        affected_variables=[],
                        expected_values={},
                        actual_values={},
                    )
                )
            except Exception as e:
                errors.append({
                    "device_id": str(device_id),
                    "error": str(e),
                })

        processing_time = int((time.time() - start_time) * 1000)

        return BatchInferenceResponse(
            model_id=request.model_id,
            total_devices=len(request.device_ids),
            processed_devices=len(predictions),
            predictions=predictions,
            errors=errors,
            processing_time_ms=processing_time,
        )

    except ModelNotFoundError:
        raise HTTPException(status_code=404, detail="Model not found")
    except ModelLoadError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Model load failed", e))
    except Exception as e:
        logger.exception("Batch anomaly detection failed")
        raise HTTPException(status_code=500, detail=_safe_error_message("Batch inference failed", e))


@router.post("/maintenance", response_model=PredictiveMaintenanceResult)
async def predict_maintenance(request: InferenceRequest):
    """
    Predict maintenance needs for a device.

    Analyzes telemetry patterns to predict equipment failure
    probability within 24-48 hours.

    Args:
        request: Inference request with device_id, model_id, and telemetry data.

    Returns:
        PredictiveMaintenanceResult with failure probability and recommendations.

    Raises:
        HTTPException: 404 if model not found, 400 if inference fails.
    """
    logger.info(f"Maintenance prediction for device {request.device_id}")

    if not request.model_id:
        raise HTTPException(status_code=400, detail="model_id is required")

    try:
        # Load model
        engine: PredictiveMaintenanceEngine = _model_loader.get_model(
            model_id=request.model_id,
            model_type=MLModelType.PREDICTIVE_MAINTENANCE,
        )

        # Convert telemetry to DataFrame
        df = telemetry_to_dataframe(request.telemetry)
        feature_columns = get_feature_columns(df)

        if not feature_columns:
            raise HTTPException(status_code=400, detail="No feature columns in telemetry")

        # Run inference
        labels, probabilities, details = engine.predict_with_probability(df, feature_columns)

        # Use the most recent result
        idx = -1
        maintenance_probability = float(probabilities[idx])
        detail = details[idx]
        is_failure_imminent = labels[idx] == 1

        # Generate recommended actions based on risk level
        risk_level = detail.get("risk_level", "LOW")
        recommended_actions = _get_maintenance_recommendations(risk_level, detail.get("top_risk_factors", {}))

        return PredictiveMaintenanceResult(
            device_id=request.device_id,
            model_id=request.model_id,
            prediction_type="PREDICTIVE_MAINTENANCE",
            prediction_value=maintenance_probability,
            prediction_label="FAILURE_IMMINENT" if is_failure_imminent else "NORMAL",
            confidence=maintenance_probability if is_failure_imminent else (1.0 - maintenance_probability),
            prediction_details={
                "risk_level": risk_level,
                "samples_analyzed": len(df),
            },
            prediction_timestamp=datetime.now(timezone.utc),
            maintenance_probability=maintenance_probability,
            days_to_maintenance=detail.get("days_to_failure"),
            recommended_actions=recommended_actions,
            risk_factors=detail.get("top_risk_factors", {}),
        )

    except ModelNotFoundError:
        raise HTTPException(status_code=404, detail="Model not found")
    except ModelLoadError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Model load failed", e))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid request", e))
    except Exception as e:
        logger.exception("Maintenance prediction failed")
        raise HTTPException(status_code=500, detail=_safe_error_message("Inference failed", e))


def _get_maintenance_recommendations(risk_level: str, risk_factors: Dict[str, float]) -> List[str]:
    """
    Generate maintenance recommendations based on risk level and factors.

    Args:
        risk_level: Risk level (LOW, MEDIUM, HIGH, CRITICAL).
        risk_factors: Dictionary of risk factor names to contribution scores.

    Returns:
        List of recommended actions.
    """
    recommendations = []

    if risk_level == "CRITICAL":
        recommendations.append("Immediate inspection required")
        recommendations.append("Schedule emergency maintenance within 24 hours")
        recommendations.append("Prepare backup equipment")
    elif risk_level == "HIGH":
        recommendations.append("Schedule maintenance within 48 hours")
        recommendations.append("Increase monitoring frequency")
        recommendations.append("Review recent operational changes")
    elif risk_level == "MEDIUM":
        recommendations.append("Plan maintenance within 1 week")
        recommendations.append("Monitor key indicators closely")
    else:
        recommendations.append("Continue normal operation")
        recommendations.append("Maintain regular maintenance schedule")

    # Add specific recommendations based on risk factors
    for factor in list(risk_factors.keys())[:3]:
        if "temperature" in factor.lower():
            recommendations.append("Check cooling system")
        elif "vibration" in factor.lower():
            recommendations.append("Inspect bearings and alignment")
        elif "pressure" in factor.lower():
            recommendations.append("Check for leaks and blockages")

    return recommendations


@router.post("/energy", response_model=InferenceResponse)
async def forecast_energy(
    request: InferenceRequest,
    horizon: str = Query("24h", description="Forecast horizon (1h, 24h, 7d)"),
):
    """
    Forecast energy consumption for a device.

    Uses historical telemetry patterns to predict future
    energy consumption over the specified horizon.

    Args:
        request: Inference request with device_id, model_id, and telemetry data.
        horizon: Forecast horizon - 1h (1 hour), 24h (24 hours), or 7d (7 days).

    Returns:
        InferenceResponse with forecasted consumption values.

    Raises:
        HTTPException: 404 if model not found, 400 if inference fails.
    """
    logger.info(f"Energy forecast for device {request.device_id}, horizon={horizon}")

    if not request.model_id:
        raise HTTPException(status_code=400, detail="model_id is required")

    # Parse horizon to periods (assuming hourly data)
    horizon_periods = {
        "1h": 1,
        "24h": 24,
        "7d": 168,
    }

    if horizon not in horizon_periods:
        raise HTTPException(status_code=400, detail=f"Invalid horizon. Use: {list(horizon_periods.keys())}")

    periods = horizon_periods[horizon]

    try:
        # Load model
        engine: EnergyForecastingEngine = _model_loader.get_model(
            model_id=request.model_id,
            model_type=MLModelType.ENERGY_FORECAST,
        )

        # Convert telemetry to DataFrame
        df = telemetry_to_dataframe(request.telemetry)

        # Determine target column (energy/consumption related)
        target_column = None
        for col in df.columns:
            if any(keyword in col.lower() for keyword in ["energy", "consumption", "power", "kw"]):
                target_column = col
                break

        if not target_column:
            # Use first numeric column if no energy column found
            numeric_cols = df.select_dtypes(include=[np.number]).columns.tolist()
            if numeric_cols:
                target_column = numeric_cols[0]
            else:
                raise HTTPException(status_code=400, detail="No numeric columns for forecasting")

        # Run forecast
        forecast_df, predictions = engine.forecast(
            last_known_data=df,
            target_column=target_column,
            periods=periods,
        )

        # Calculate summary statistics
        mean_forecast = float(np.mean(predictions))
        min_forecast = float(np.min(predictions))
        max_forecast = float(np.max(predictions))
        total_forecast = float(np.sum(predictions))

        # Get valid until timestamp
        valid_until = forecast_df["timestamp"].max() if not forecast_df.empty else None

        return InferenceResponse(
            device_id=request.device_id,
            model_id=request.model_id,
            prediction_type="ENERGY_FORECAST",
            prediction_value=mean_forecast,
            prediction_label=f"{horizon}_FORECAST",
            confidence=0.85,  # Default confidence for forecasts
            prediction_details={
                "horizon": horizon,
                "periods": periods,
                "target_column": target_column,
                "mean_consumption": mean_forecast,
                "min_consumption": min_forecast,
                "max_consumption": max_forecast,
                "total_consumption": total_forecast,
                "forecast_values": predictions.tolist()[:48],  # Limit to first 48 values
            },
            prediction_timestamp=datetime.now(timezone.utc),
            prediction_horizon=horizon,
            valid_until=valid_until,
        )

    except ModelNotFoundError:
        raise HTTPException(status_code=404, detail="Model not found")
    except ModelLoadError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Model load failed", e))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid request", e))
    except Exception as e:
        logger.exception("Energy forecast failed")
        raise HTTPException(status_code=500, detail=_safe_error_message("Inference failed", e))


@router.post("/rul", response_model=InferenceResponse)
async def estimate_rul(request: InferenceRequest):
    """
    Estimate Remaining Useful Life for equipment.

    Analyzes degradation indicators to predict how many days
    until maintenance is required.

    Args:
        request: Inference request with device_id, model_id, and telemetry data.

    Returns:
        InferenceResponse with RUL in days and confidence interval.

    Raises:
        HTTPException: 404 if model not found, 400 if inference fails.
    """
    logger.info(f"RUL estimation for device {request.device_id}")

    if not request.model_id:
        raise HTTPException(status_code=400, detail="model_id is required")

    try:
        # Load model
        engine: EquipmentRULEngine = _model_loader.get_model(
            model_id=request.model_id,
            model_type=MLModelType.EQUIPMENT_RUL,
        )

        # Convert telemetry to DataFrame
        df = telemetry_to_dataframe(request.telemetry)
        feature_columns = get_feature_columns(df)

        if not feature_columns:
            raise HTTPException(status_code=400, detail="No feature columns in telemetry")

        # Run inference with confidence intervals
        predictions, lower_bounds, upper_bounds = engine.predict_with_confidence(
            df, feature_columns, confidence_level=0.95
        )

        # Use the most recent prediction
        idx = -1
        rul_days = float(predictions[idx])
        lower_bound = float(lower_bounds[idx])
        upper_bound = float(upper_bounds[idx])

        # Calculate confidence based on interval width
        interval_width = upper_bound - lower_bound
        confidence = max(0.5, 1.0 - (interval_width / (rul_days + 1)))

        # Determine label based on RUL
        if rul_days <= 7:
            label = "CRITICAL"
        elif rul_days <= 30:
            label = "WARNING"
        elif rul_days <= 90:
            label = "ATTENTION"
        else:
            label = "HEALTHY"

        # Get feature importance if available
        feature_importance = engine.get_feature_importance() or {}

        return InferenceResponse(
            device_id=request.device_id,
            model_id=request.model_id,
            prediction_type="EQUIPMENT_RUL",
            prediction_value=rul_days,
            prediction_label=label,
            confidence=confidence,
            prediction_details={
                "rul_days": rul_days,
                "lower_bound_95": lower_bound,
                "upper_bound_95": upper_bound,
                "confidence_interval": f"[{lower_bound:.1f}, {upper_bound:.1f}]",
                "samples_analyzed": len(df),
                "top_factors": dict(list(feature_importance.items())[:5]),
            },
            prediction_timestamp=datetime.now(timezone.utc),
        )

    except ModelNotFoundError:
        raise HTTPException(status_code=404, detail="Model not found")
    except ModelLoadError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Model load failed", e))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid request", e))
    except Exception as e:
        logger.exception("RUL estimation failed")
        raise HTTPException(status_code=500, detail=_safe_error_message("Inference failed", e))


@router.get("/predictions/{device_id}")
async def get_device_predictions(
    device_id: UUID,
    prediction_type: Optional[str] = Query(None),
    limit: int = Query(10, ge=1, le=100),
):
    """
    Get recent predictions for a device.

    Note: This endpoint requires database integration to retrieve
    stored predictions. Currently returns empty list.

    Args:
        device_id: UUID of the device.
        prediction_type: Optional filter by prediction type.
        limit: Maximum number of predictions to return.

    Returns:
        List of recent predictions.
    """
    logger.info(f"Getting predictions for device {device_id}")
    # TODO: Implement database query for stored predictions
    # This would query the ml_predictions table in PostgreSQL
    return []
