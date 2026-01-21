import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Filler,
  Title,
  Tooltip,
  Legend,
  ChartOptions,
  ChartData,
} from 'chart.js';
import { Line } from 'react-chartjs-2';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Filler,
  Title,
  Tooltip,
  Legend
);

interface AreaChartWidgetProps {
  widget: Widget;
  deviceId?: string;
  latestData?: TelemetryPoint;
}

export const AreaChartWidget: React.FC<AreaChartWidgetProps> = ({ widget, deviceId, latestData }) => {
  const [data, setData] = useState<ChartData<'line'> | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  // Trigger refresh when new real-time data arrives
  useEffect(() => {
    if (latestData) {
      setRefreshTrigger(prev => prev + 1);
    }
  }, [latestData]);

  useEffect(() => {
    const fetchData = async () => {
      if (!deviceId || !widget.variableName) {
        setLoading(false);
        return;
      }

      try {
        // Calculate time range
        const now = new Date();
        const minutesAgo = widget.timeRangeMinutes || 60;
        const from = new Date(now.getTime() - minutesAgo * 60000);

        // Fetch historical data
        const telemetryData = await apiService.queryTelemetry(
          deviceId,
          from.toISOString(),
          now.toISOString()
        );

        // Transform data for Chart.js
        const varName = widget.variableName as string;
        // Convert snake_case to camelCase for API property access
        const toCamelCase = (str: string) => str.replace(/_([a-z])/g, (g) => g[1].toUpperCase());
        const accessKey = toCamelCase(varName);
        const labels = telemetryData.map((point) =>
          new Date(point.timestamp).toLocaleTimeString()
        );
        const values = telemetryData.map((point) => (point[accessKey] as number) || 0);

        // Get colors from config or use defaults
        const borderColor = widget.config.colors?.[0] || 'rgb(59, 130, 246)';
        const backgroundColor = widget.config.colors?.[1] || 'rgba(59, 130, 246, 0.3)';

        setData({
          labels,
          datasets: [
            {
              label: widget.variableName,
              data: values,
              borderColor: borderColor,
              backgroundColor: backgroundColor,
              tension: 0.4,
              fill: true,
              pointRadius: (widget.config.showPoints as boolean | undefined) ? 3 : 0,
              pointHoverRadius: 5,
              borderWidth: 2,
            },
          ],
        });
      } catch (error) {
        console.error('Error fetching area chart data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, widget.config.refreshInterval || 30000);
    return () => clearInterval(interval);
  }, [deviceId, widget.variableName, widget.timeRangeMinutes, widget.config.refreshInterval, refreshTrigger]);

  if (loading) {
    return (
      <div className="flex items-center justify-center w-full h-full">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex items-center justify-center w-full h-full text-gray-400">
        <p>No data available</p>
      </div>
    );
  }

  const options: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index',
      intersect: false,
    },
    plugins: {
      legend: {
        display: (widget.config.showLegend as boolean | undefined) ?? true,
        position: 'top' as const,
      },
      title: {
        display: false,
      },
      tooltip: {
        mode: 'index',
        intersect: false,
      },
    },
    scales: {
      y: {
        beginAtZero: (widget.config.beginAtZero as boolean | undefined) ?? true,
        grid: {
          display: (widget.config.showGrid as boolean | undefined) ?? true,
        },
        ...(widget.config.min !== undefined && { min: widget.config.min as number }),
        ...(widget.config.max !== undefined && { max: widget.config.max as number }),
      },
      x: {
        grid: {
          display: (widget.config.showGrid as boolean | undefined) ?? true,
        },
      },
    },
  };

  return (
    <div className="w-full h-full p-2">
      <Line data={data} options={options} />
    </div>
  );
};
