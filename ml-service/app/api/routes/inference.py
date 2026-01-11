"""
ML Inference endpoints.

Provides REST API endpoints for running ML inference on telemetry data.
Supports anomaly detection, predictive maintenance, energy forecasting,
and equipment RUL estimation.
"""
import logging
import math
import re
import time
from datetime import datetime, timezone
from typing import Dict, List, Optional, Tuple, Union
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


def _validate_array_length(
    arrays: List[Tuple[str, np.ndarray]],
    min_length: int = 1,
) -> None:
    """
    Validate that all arrays have at least minimum length.

    Args:
        arrays: List of (name, array) tuples for error messages.
        min_length: Minimum required length (default 1).

    Raises:
        ValueError: If any array is shorter than min_length.
    """
    for name, arr in arrays:
        if len(arr) < min_length:
            raise ValueError(
                f"Engine returned empty or insufficient {name} "
                f"(got {len(arr)}, need at least {min_length})"
            )


def _sanitize_prediction(
    value: float,
    name: str = "prediction",
    replace_invalid: bool = True,
    default: float = 0.0,
) -> float:
    """
    Check prediction value for NaN/Infinity and handle appropriately.

    Args:
        value: The prediction value to check.
        name: Name of the value for logging.
        replace_invalid: If True, replace invalid values with default.
                        If False, raise ValueError.
        default: Default value to use when replacing.

    Returns:
        Sanitized float value.

    Raises:
        ValueError: If value is invalid and replace_invalid is False.
    """
    if math.isnan(value) or math.isinf(value):
        logger.warning(f"Invalid {name} value: {value}")
        if replace_invalid:
            return default
        raise ValueError(f"Invalid {name} value: {value}")
    return value


def _find_energy_column(df: pd.DataFrame) -> Optional[str]:
    """
    Find an energy-related column using word-part matching.

    Splits column names by non-alphanumeric characters (underscores, hyphens, etc.)
    and checks if any part matches an energy keyword. This avoids false positives
    like "disempowerment" matching "power" while still matching "energy_consumption"
    or "power_kw".

    Args:
        df: DataFrame to search for energy columns.

    Returns:
        Name of energy column if found, None otherwise.
    """
    energy_keywords = {"energy", "consumption", "power", "kw", "kwh", "watt"}

    for col in df.columns:
        if col == "timestamp":
            continue

        # Split by non-alphanumeric characters to get word parts
        # e.g., "energy_consumption" -> ["energy", "consumption"]
        # e.g., "disempowerment" -> ["disempowerment"]
        col_parts = set(re.split(r'[^a-zA-Z0-9]+', col.lower()))

        # Check if any part matches an energy keyword
        if col_parts & energy_keywords:  # Set intersection
            return col

    return None


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


def get_feature_columns(df: pd.DataFrame, max_columns: Optional[int] = None) -> List[str]:
    """
    Extract feature columns from DataFrame (exclude timestamp).

    Args:
        df: DataFrame with telemetry data.
        max_columns: Maximum number of feature columns to return.
                    If None, uses settings.MAX_FEATURE_COLUMNS.

    Returns:
        List of feature column names (limited to max_columns).

    Note:
        Columns are returned in their original order, truncated at max.
        Callers should be aware that if data has more columns than the
        limit, only the first N columns will be used for inference.
    """
    limit = max_columns if max_columns is not None else settings.MAX_FEATURE_COLUMNS
    columns = [col for col in df.columns if col != "timestamp"]

    if len(columns) > limit:
        logger.warning(
            f"Feature columns ({len(columns)}) exceed limit ({limit}). "
            f"Using first {limit} columns: {columns[:limit]}"
        )
        return columns[:limit]

    return columns


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

        # Validate arrays have data before indexing
        _validate_array_length([
            ("labels", labels),
            ("scores", scores),
            ("details", np.array(details)),  # details is a list
        ])

        # Use the most recent result (last row)
        idx = -1
        is_anomaly = labels[idx] == -1
        anomaly_score = _sanitize_prediction(
            float(scores[idx]),
            name="anomaly_score",
            replace_invalid=True,
            default=0.5,
        )
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

    except HTTPException:
        # Re-raise HTTP exceptions (e.g., from validation above)
        raise
    except ModelNotFoundError:
        raise HTTPException(status_code=404, detail="Model not found")
    except ModelLoadError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Model load failed", e))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid request", e))
    except (KeyError, TypeError, IndexError) as e:
        # Specific exceptions for data access issues
        logger.error(f"Data processing error in anomaly detection: {type(e).__name__}")
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid telemetry data", e))
    except Exception as e:
        logger.exception("Anomaly detection failed")
        raise HTTPException(status_code=500, detail=_safe_error_message("Inference failed", e))


@router.post("/anomaly/batch", response_model=BatchInferenceResponse)
async def detect_anomalies_batch(request: BatchInferenceRequest):
    """
    Run batch anomaly detection on multiple devices.

    NOT YET IMPLEMENTED: Requires database integration to fetch telemetry
    data for each device. The telemetry data should be fetched from the
    Spring Boot backend via the data service.

    Args:
        request: Batch request with model_id and list of device_ids.

    Returns:
        BatchInferenceResponse with predictions for all devices.

    Raises:
        HTTPException: 501 - This endpoint is not yet implemented.
    """
    logger.warning(
        f"Batch endpoint called but not implemented: "
        f"model={request.model_id}, devices={len(request.device_ids)}"
    )
    raise HTTPException(
        status_code=501,
        detail="Batch inference is not yet implemented. "
               "Use the single-device /anomaly endpoint instead, "
               "or implement database integration to fetch telemetry per device."
    )


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

        # Validate arrays have data before indexing
        _validate_array_length([
            ("labels", labels),
            ("probabilities", probabilities),
            ("details", np.array(details)),
        ])

        # Use the most recent result
        idx = -1
        maintenance_probability = _sanitize_prediction(
            float(probabilities[idx]),
            name="maintenance_probability",
            replace_invalid=True,
            default=0.5,
        )
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

    except HTTPException:
        # Re-raise HTTP exceptions (e.g., from validation above)
        raise
    except ModelNotFoundError:
        raise HTTPException(status_code=404, detail="Model not found")
    except ModelLoadError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Model load failed", e))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid request", e))
    except (KeyError, TypeError, IndexError) as e:
        # Specific exceptions for data access issues
        logger.error(f"Data processing error in maintenance prediction: {type(e).__name__}")
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid telemetry data", e))
    except Exception as e:
        logger.exception("Maintenance prediction failed")
        raise HTTPException(status_code=500, detail=_safe_error_message("Inference failed", e))


def _get_maintenance_recommendations(risk_level: str, risk_factors: Dict[str, float]) -> List[str]:
    """
    Generate maintenance recommendations based on risk level.

    Returns general recommendations based on the risk level. Specific
    factor-based recommendations should be handled by the client UI,
    which has access to the raw risk_factors data.

    Args:
        risk_level: Risk level (LOW, MEDIUM, HIGH, CRITICAL).
        risk_factors: Dictionary of risk factor names to contribution scores.
                     (Passed for completeness but not used - factors are returned
                     separately in the response for client-side processing)

    Returns:
        List of recommended actions based on risk level.
    """
    if risk_level == "CRITICAL":
        return [
            "Immediate inspection required",
            "Schedule emergency maintenance within 24 hours",
            "Prepare backup equipment",
            "Review operational logs for recent anomalies",
        ]
    elif risk_level == "HIGH":
        return [
            "Schedule maintenance within 48 hours",
            "Increase monitoring frequency",
            "Review recent operational changes",
        ]
    elif risk_level == "MEDIUM":
        return [
            "Plan maintenance within 1 week",
            "Monitor key indicators closely",
        ]
    else:
        return [
            "Continue normal operation",
            "Maintain regular maintenance schedule",
        ]


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
        HTTPException: 404 if model not found, 400 if inference fails or no energy column.
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

        # Determine target column - MUST be energy-related
        # Use word boundary matching to avoid false positives (e.g., "disempowerment" for "power")
        target_column = _find_energy_column(df)

        if not target_column:
            available_cols = [c for c in df.columns if c != "timestamp"]
            energy_keywords = ["energy", "consumption", "power", "kw", "kwh", "watt"]
            raise HTTPException(
                status_code=400,
                detail=f"No energy-related column found. Expected column with word matching one of: "
                       f"{energy_keywords}. Available columns: {available_cols}"
            )

        # Run forecast
        forecast_df, predictions = engine.forecast(
            last_known_data=df,
            target_column=target_column,
            periods=periods,
        )

        # Validate predictions array is not empty
        if len(predictions) == 0:
            raise ValueError("Forecast engine returned empty predictions")

        # Calculate summary statistics with sanity checks
        # Use nanmean/nanmin/nanmax to handle potential NaN values gracefully
        mean_forecast = _sanitize_prediction(
            float(np.nanmean(predictions)),
            name="mean_forecast",
            replace_invalid=True,
            default=0.0,
        )
        min_forecast = _sanitize_prediction(
            float(np.nanmin(predictions)),
            name="min_forecast",
            replace_invalid=True,
            default=0.0,
        )
        max_forecast = _sanitize_prediction(
            float(np.nanmax(predictions)),
            name="max_forecast",
            replace_invalid=True,
            default=0.0,
        )
        total_forecast = _sanitize_prediction(
            float(np.nansum(predictions)),
            name="total_forecast",
            replace_invalid=True,
            default=0.0,
        )

        # Get valid until timestamp
        valid_until = forecast_df["timestamp"].max() if not forecast_df.empty else None

        # Truncate forecast values for response, with metadata
        max_values = settings.MAX_FORECAST_VALUES_RETURNED
        total_values = len(predictions)
        is_truncated = total_values > max_values
        # Sanitize individual forecast values (replace NaN/Inf with 0)
        forecast_values = [
            _sanitize_prediction(float(v), "forecast_value", replace_invalid=True, default=0.0)
            for v in predictions[:max_values]
        ]

        return InferenceResponse(
            device_id=request.device_id,
            model_id=request.model_id,
            prediction_type="ENERGY_FORECAST",
            prediction_value=mean_forecast,
            prediction_label=f"{horizon}_FORECAST",
            confidence=settings.DEFAULT_FORECAST_CONFIDENCE,
            prediction_details={
                "horizon": horizon,
                "periods": periods,
                "target_column": target_column,
                "mean_consumption": mean_forecast,
                "min_consumption": min_forecast,
                "max_consumption": max_forecast,
                "total_consumption": total_forecast,
                "forecast_values": forecast_values,
                "forecast_values_count": len(forecast_values),
                "forecast_values_total": total_values,
                "forecast_values_truncated": is_truncated,
            },
            prediction_timestamp=datetime.now(timezone.utc),
            prediction_horizon=horizon,
            valid_until=valid_until,
        )

    except HTTPException:
        # Re-raise HTTP exceptions (e.g., from column validation above)
        raise
    except ModelNotFoundError:
        raise HTTPException(status_code=404, detail="Model not found")
    except ModelLoadError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Model load failed", e))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid request", e))
    except (KeyError, TypeError) as e:
        # Specific exceptions for data access issues
        logger.error(f"Data processing error in energy forecast: {type(e).__name__}")
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid telemetry data", e))
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

        # Validate arrays have data before indexing
        _validate_array_length([
            ("predictions", predictions),
            ("lower_bounds", lower_bounds),
            ("upper_bounds", upper_bounds),
        ])

        # Use the most recent prediction
        idx = -1
        rul_days = _sanitize_prediction(
            float(predictions[idx]),
            name="rul_days",
            replace_invalid=True,
            default=0.0,
        )
        lower_bound = _sanitize_prediction(
            float(lower_bounds[idx]),
            name="lower_bound",
            replace_invalid=True,
            default=0.0,
        )
        upper_bound = _sanitize_prediction(
            float(upper_bounds[idx]),
            name="upper_bound",
            replace_invalid=True,
            default=0.0,
        )

        # Calculate confidence using coefficient of variation approach
        # Confidence is higher when the interval is narrow relative to the prediction
        interval_width = upper_bound - lower_bound
        if rul_days > 0:
            # Use relative precision: 1 - (half_interval / prediction)
            # Normalized so that an interval spanning +/- 50% of prediction gives ~0.5 confidence
            relative_uncertainty = (interval_width / 2) / rul_days
            confidence = max(0.0, min(1.0, 1.0 - relative_uncertainty))
        else:
            # RUL is 0 or negative: confidence based purely on interval width
            # Narrow interval = high confidence the equipment needs attention
            # Scale factor is configurable (default 100 days) - represents the interval
            # width at which confidence would be 0 when RUL is already 0
            scale_days = settings.RUL_CONFIDENCE_SCALE_DAYS
            confidence = max(0.0, min(1.0, 1.0 - (interval_width / scale_days)))

        # Determine label based on RUL using configurable thresholds
        if rul_days <= settings.RUL_CRITICAL_THRESHOLD_DAYS:
            label = "CRITICAL"
        elif rul_days <= settings.RUL_WARNING_THRESHOLD_DAYS:
            label = "WARNING"
        elif rul_days <= settings.RUL_ATTENTION_THRESHOLD_DAYS:
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
            confidence=round(confidence, 3),
            prediction_details={
                "rul_days": rul_days,
                "lower_bound_95": lower_bound,
                "upper_bound_95": upper_bound,
                "confidence_interval": f"[{lower_bound:.1f}, {upper_bound:.1f}]",
                "interval_width_days": round(interval_width, 1),
                "samples_analyzed": len(df),
                "top_factors": dict(list(feature_importance.items())[:5]),
            },
            prediction_timestamp=datetime.now(timezone.utc),
        )

    except HTTPException:
        # Re-raise HTTP exceptions (e.g., from validation above)
        raise
    except ModelNotFoundError:
        raise HTTPException(status_code=404, detail="Model not found")
    except ModelLoadError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Model load failed", e))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid request", e))
    except (KeyError, TypeError) as e:
        # Specific exceptions for data access issues
        logger.error(f"Data processing error in RUL estimation: {type(e).__name__}")
        raise HTTPException(status_code=400, detail=_safe_error_message("Invalid telemetry data", e))
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
