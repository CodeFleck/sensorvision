import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';

interface MetricCardWidgetProps {
  widget: Widget;
  deviceId?: string;
  latestData?: TelemetryPoint;
}

export const MetricCardWidget: React.FC<MetricCardWidgetProps> = ({ widget, deviceId, latestData }) => {
  const [value, setValue] = useState<number>(0);
  const [previousValue, setPreviousValue] = useState<number>(0);
  const [loading, setLoading] = useState(true);

  const unit = (widget.config.unit as string | undefined) ?? '';
  const decimals = (widget.config.decimals as number | undefined) ?? 1;

  // Update value when real-time data arrives
  useEffect(() => {
    if (latestData && widget.variableName) {
      const varName = widget.variableName as keyof TelemetryPoint;
      const rawValue = latestData[varName];

      // Only update if the variable is actually present in the data and is a number
      // This prevents resetting to 0 when data for other variables arrives
      if (rawValue !== undefined && rawValue !== null && typeof rawValue === 'number') {
        // Use functional update to avoid stale closure issues with previousValue
        setValue(prevValue => {
          setPreviousValue(prevValue);
          return rawValue;
        });
        setLoading(false);
      }
    }
  }, [latestData, widget.variableName]);

  // Initial data fetch and fallback polling
  useEffect(() => {
    const fetchData = async () => {
      if (!deviceId || !widget.variableName) {
        setLoading(false);
        return;
      }

      try {
        const data = await apiService.getLatestForDevice(deviceId);
        const varName = widget.variableName as keyof typeof data;
        const rawValue = data[varName];

        // Only update if the variable has a value and is a number
        if (rawValue !== undefined && rawValue !== null && typeof rawValue === 'number') {
          // Use functional update to avoid stale closure issues with previousValue
          setValue(prevValue => {
            setPreviousValue(prevValue);
            return rawValue;
          });
        }
      } catch (error) {
        console.error('Error fetching metric data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, (widget.config.refreshInterval as number | undefined) ?? 30000);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deviceId, widget.variableName, widget.config.refreshInterval]);

  if (loading) {
    return (
      <div className="flex items-center justify-center w-full h-full">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  const change = value - previousValue;
  const changePercentage = previousValue !== 0 ? ((change / previousValue) * 100) : 0;

  // Determine trend color
  const trendColor = change > 0 ? 'text-green-600' : change < 0 ? 'text-red-600' : 'text-gray-600';
  const trendIcon = change > 0 ? '↑' : change < 0 ? '↓' : '→';

  return (
    <div className="flex flex-col items-center justify-center w-full h-full p-4">
      {/* Main value */}
      <div className="text-5xl font-bold text-gray-800 mb-2">
        {value.toFixed(decimals)}
        {unit && <span className="text-2xl text-gray-500 ml-2">{unit}</span>}
      </div>

      {/* Variable name */}
      <div className="text-sm text-gray-500 mb-3">
        {widget.variableName}
      </div>

      {/* Trend indicator */}
      {previousValue !== 0 && (
        <div className={`flex items-center gap-1 text-sm font-semibold ${trendColor}`}>
          <span className="text-lg">{trendIcon}</span>
          <span>{Math.abs(changePercentage).toFixed(1)}%</span>
        </div>
      )}

      {/* Threshold warning */}
      {widget.config.thresholds && widget.config.thresholds.length > 0 && (
        <div className="mt-3 text-xs">
          {(widget.config.thresholds as Array<{ value: number; color: string }>).map((threshold, idx: number) => {
            if (value >= threshold.value) {
              return (
                <div
                  key={idx}
                  className="px-2 py-1 rounded"
                  style={{ backgroundColor: threshold.color + '20', color: threshold.color }}
                >
                  Above {threshold.value} threshold
                </div>
              );
            }
            return null;
          })}
        </div>
      )}
    </div>
  );
};
