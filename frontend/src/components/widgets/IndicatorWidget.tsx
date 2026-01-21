import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';

interface IndicatorWidgetProps {
  widget: Widget;
  deviceId?: string;
  latestData?: TelemetryPoint;
}

export const IndicatorWidget: React.FC<IndicatorWidgetProps> = ({ widget, deviceId, latestData }) => {
  const [currentValue, setCurrentValue] = useState<number | null>(null);
  const [status, setStatus] = useState<'off' | 'normal' | 'warning' | 'critical'>('off');
  const [loading, setLoading] = useState(true);

  // Convert snake_case to camelCase for API property access
  const toCamelCase = (str: string) => str.replace(/_([a-z])/g, (g) => g[1].toUpperCase());

  // Update value when real-time data arrives via WebSocket
  useEffect(() => {
    if (latestData && widget.variableName) {
      // Try both snake_case and camelCase property names
      const varName = widget.variableName as string;
      const camelName = toCamelCase(varName);
      const rawValue = (latestData[camelName as keyof TelemetryPoint] ?? latestData[varName as keyof TelemetryPoint]) as number | undefined;

      // Only update if the variable is actually present in the data
      // This prevents resetting when data for other variables arrives
      if (rawValue !== undefined && rawValue !== null && typeof rawValue === 'number') {
        setCurrentValue(rawValue);
        calculateStatus(rawValue);
        setLoading(false);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [latestData, widget.variableName]);

  // Initial data fetch and fallback polling
  useEffect(() => {
    const fetchData = async () => {
      if (!deviceId || !widget.variableName) {
        setLoading(false);
        return;
      }

      try {
        // Fetch latest value for the device
        const telemetryData = await apiService.getLatestForDevice(deviceId);
        // Try both snake_case and camelCase property names
        const varName = widget.variableName as string;
        const camelName = toCamelCase(varName);
        const value = (telemetryData[camelName as keyof typeof telemetryData] ?? telemetryData[varName as keyof typeof telemetryData]) as number | undefined;

        if (value !== null && value !== undefined) {
          setCurrentValue(value);
          calculateStatus(value);
        }
      } catch (error) {
        console.error('Error fetching indicator data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, (widget.config.refreshInterval as number | undefined) || 5000);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deviceId, widget.variableName, widget.config.refreshInterval]);

  const calculateStatus = (value: number) => {
    // Status calculation based on thresholds in config
    const statusThresholds = widget.config.statusThresholds as {
      critical?: { min: number | null; max: number | null };
      warning?: { min: number | null; max: number | null };
      normal?: { min: number | null; max: number | null };
    } | undefined;

    if (!statusThresholds) {
      setStatus('normal');
      return;
    }

    // Check critical range
    if (statusThresholds.critical) {
      const { min, max } = statusThresholds.critical;
      if ((min !== null && value < min) || (max !== null && value > max)) {
        setStatus('critical');
        return;
      }
    }

    // Check warning range
    if (statusThresholds.warning) {
      const { min, max } = statusThresholds.warning;
      if ((min !== null && value < min) || (max !== null && value > max)) {
        setStatus('warning');
        return;
      }
    }

    // Normal range
    setStatus('normal');
  };

  const getStatusColor = () => {
    switch (status) {
      case 'critical':
        return 'bg-red-500 shadow-red-500/50';
      case 'warning':
        return 'bg-yellow-500 shadow-yellow-500/50';
      case 'normal':
        return 'bg-green-500 shadow-green-500/50';
      case 'off':
      default:
        return 'bg-gray-400 shadow-gray-400/50';
    }
  };

  const getStatusText = () => {
    switch (status) {
      case 'critical':
        return 'CRITICAL';
      case 'warning':
        return 'WARNING';
      case 'normal':
        return 'NORMAL';
      case 'off':
      default:
        return 'OFF';
    }
  };

  const getTextColor = () => {
    switch (status) {
      case 'critical':
        return 'text-red-500';
      case 'warning':
        return 'text-yellow-500';
      case 'normal':
        return 'text-green-500';
      case 'off':
      default:
        return 'text-gray-400';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center w-full h-full">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  const indicatorSize = (widget.config.size as string | undefined) || 'large';
  const sizeClasses = {
    small: 'w-8 h-8',
    medium: 'w-16 h-16',
    large: 'w-24 h-24',
  };

  const showLabel = (widget.config.showLabel as boolean | undefined) ?? true;
  const showValue = (widget.config.showValue as boolean | undefined) ?? true;

  return (
    <div className="flex flex-col items-center justify-center w-full h-full space-y-4">
      {/* Status Light */}
      <div
        className={`${sizeClasses[indicatorSize as keyof typeof sizeClasses]} rounded-full ${getStatusColor()}
                    shadow-lg animate-pulse transition-all duration-300`}
      />

      {/* Status Label */}
      {showLabel && (
        <div className={`text-lg font-bold ${getTextColor()} tracking-wider`}>
          {getStatusText()}
        </div>
      )}

      {/* Current Value */}
      {showValue && currentValue !== null && (
        <div className="text-2xl font-mono text-gray-300">
          {currentValue.toFixed(2)}
          {widget.config.unit && <span className="text-sm ml-1 text-gray-400">{widget.config.unit as string}</span>}
        </div>
      )}

      {/* Variable Name */}
      {(widget.config.showVariableName as boolean | undefined) && (
        <div className="text-sm text-gray-500">
          {widget.variableName}
        </div>
      )}
    </div>
  );
};
