import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';
import { LineChart } from '@tremor/react';

interface LineChartWidgetProps {
  widget: Widget;
  latestData?: TelemetryPoint;
}

interface TremorDataPoint {
  timestamp: string;
  [key: string]: string | number;
}

export const LineChartWidget: React.FC<LineChartWidgetProps> = ({ widget, latestData }) => {
  const [data, setData] = useState<TremorDataPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [categories, setCategories] = useState<string[]>([]);
  const [colors, setColors] = useState<string[]>([]);

  // Trigger refresh when new real-time data arrives
  useEffect(() => {
    if (latestData) {
      setRefreshTrigger(prev => prev + 1);
    }
  }, [latestData]);

  useEffect(() => {
    const fetchData = async () => {
      if (!widget.deviceId || !widget.variableName) {
        setLoading(false);
        return;
      }

      try {
        // Calculate time range
        const now = new Date();
        const minutesAgo = widget.timeRangeMinutes || 60;
        const from = new Date(now.getTime() - minutesAgo * 60000);

        // Fetch data for primary device
        const telemetryData1 = await apiService.queryTelemetry(
          widget.deviceId,
          from.toISOString(),
          now.toISOString()
        );

        // Check if dual device mode is enabled
        const isDualDevice = widget.secondDeviceId && widget.secondVariableName;
        let telemetryData2: TelemetryPoint[] = [];

        if (isDualDevice) {
          telemetryData2 = await apiService.queryTelemetry(
            widget.secondDeviceId!,
            from.toISOString(),
            now.toISOString()
          );
        }

        // Create labels for the series
        const label1 = widget.deviceLabel || widget.deviceId;
        const label2 = isDualDevice ? (widget.secondDeviceLabel || widget.secondDeviceId!) : '';

        // Build a map of timestamps for dual device merging
        const dataMap = new Map<string, TremorDataPoint>();

        // Add data from first device
        const varName1 = widget.variableName as string;
        telemetryData1.forEach((point) => {
          const timestamp = new Date(point.timestamp).toLocaleTimeString();
          dataMap.set(timestamp, {
            timestamp,
            [label1]: (point[varName1] as number) || 0,
          });
        });

        // Add data from second device (if exists)
        if (isDualDevice && telemetryData2.length > 0) {
          const varName2 = widget.secondVariableName as string;
          telemetryData2.forEach((point) => {
            const timestamp = new Date(point.timestamp).toLocaleTimeString();
            const existing = dataMap.get(timestamp) || { timestamp };
            dataMap.set(timestamp, {
              ...existing,
              [label2]: (point[varName2] as number) || 0,
            });
          });
        }

        // Convert map to array and sort by time
        const tremorData = Array.from(dataMap.values()).sort((a, b) =>
          a.timestamp.localeCompare(b.timestamp)
        );

        setData(tremorData);
        setCategories(isDualDevice ? [label1, label2] : [label1]);
        setColors(isDualDevice
          ? [widget.config.colors?.[0] || 'blue', widget.config.colors?.[1] || 'green']
          : [widget.config.colors?.[0] || 'blue']
        );
      } catch (error) {
        console.error('Error fetching chart data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, widget.config.refreshInterval || 30000);
    return () => clearInterval(interval);
  }, [widget.deviceId, widget.secondDeviceId, widget.variableName, widget.secondVariableName, widget.timeRangeMinutes, widget.config.refreshInterval, refreshTrigger]);

  if (loading) {
    return (
      <div className="flex items-center justify-center w-full h-full">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center w-full h-full text-gray-400">
        <p>No data available</p>
      </div>
    );
  }

  return (
    <div className="w-full h-full p-4">
      <LineChart
        data={data}
        index="timestamp"
        categories={categories}
        colors={colors}
        valueFormatter={(value) => value.toFixed(2)}
        showLegend={(widget.config.showLegend as boolean | undefined) ?? true}
        showGridLines={(widget.config.showGrid as boolean | undefined) ?? true}
        showAnimation={true}
        minValue={widget.config.min as number | undefined}
        maxValue={widget.config.max as number | undefined}
        className="h-full"
      />
    </div>
  );
};
