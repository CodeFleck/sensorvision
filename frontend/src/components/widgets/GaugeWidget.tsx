import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';

interface GaugeWidgetProps {
  widget: Widget;
  deviceId?: string;
  latestData?: TelemetryPoint;
}

export const GaugeWidget: React.FC<GaugeWidgetProps> = ({ widget, deviceId, latestData }) => {
  const [value, setValue] = useState<number>(0);
  const [loading, setLoading] = useState(true);

  const min = widget.config.min ?? 0;
  const max = widget.config.max ?? 100;
  const unit = widget.config.unit ?? '';

  // Update value when real-time data arrives
  useEffect(() => {
    if (latestData && widget.variableName) {
      const varName = widget.variableName as keyof TelemetryPoint;
      const rawValue = latestData[varName];

      // Only update if the variable is actually present in the data and is a number
      // This prevents resetting to 0 when data for other variables arrives
      if (rawValue !== undefined && rawValue !== null && typeof rawValue === 'number') {
        setValue(rawValue);
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
          setValue(rawValue);
        }
      } catch (error) {
        console.error('Error fetching gauge data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    // Only poll if we don't have real-time data
    const interval = setInterval(fetchData, widget.config.refreshInterval ?? 30000);
    return () => clearInterval(interval);
  }, [deviceId, widget.variableName, widget.config.refreshInterval]);

  if (loading) {
    return (
      <div className="flex items-center justify-center w-full h-full">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  // Calculate percentage for gauge
  const percentage = Math.min(Math.max(((value - min) / (max - min)) * 100, 0), 100);

  // Determine color based on thresholds
  let color = '#10b981'; // green default
  if (widget.config.thresholds) {
    for (const threshold of widget.config.thresholds) {
      if (value >= threshold.value) {
        color = threshold.color;
      }
    }
  }

  // SVG gauge parameters
  const size = 200;
  const strokeWidth = 20;
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const offset = circumference - (percentage / 100) * circumference;

  return (
    <div className="flex flex-col items-center justify-center w-full h-full">
      <svg width={size} height={size} className="transform -rotate-90">
        {/* Background circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="#e5e7eb"
          strokeWidth={strokeWidth}
        />
        {/* Value circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          className="transition-all duration-500"
        />
      </svg>

      {/* Value display */}
      <div className="absolute flex flex-col items-center">
        <div className="text-3xl font-bold" style={{ color }}>
          {value.toFixed(1)}
        </div>
        {unit && <div className="text-sm text-gray-500">{unit}</div>}
      </div>

      {/* Min/Max labels */}
      <div className="flex justify-between w-full mt-2 px-4 text-xs text-gray-500">
        <span>{min}</span>
        <span>{max}</span>
      </div>
    </div>
  );
};
