import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
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
  Title,
  Tooltip,
  Legend
);

interface LineChartWidgetProps {
  widget: Widget;
  deviceId?: string;
  latestData?: TelemetryPoint;
}

export const LineChartWidget: React.FC<LineChartWidgetProps> = ({ widget, deviceId, latestData }) => {
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
        const labels = telemetryData.map((point) =>
          new Date(point.timestamp).toLocaleTimeString()
        );
        const values = telemetryData.map((point) => (point[varName] as number) || 0);

        setData({
          labels,
          datasets: [
            {
              label: widget.variableName,
              data: values,
              borderColor: widget.config.colors?.[0] || 'rgb(59, 130, 246)',
              backgroundColor: 'rgba(59, 130, 246, 0.1)',
              tension: 0.4,
              fill: true,
            },
          ],
        });
      } catch (error) {
        console.error('Error fetching chart data:', error);
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
    plugins: {
      legend: {
        display: (widget.config.showLegend as boolean | undefined) ?? true,
        position: 'top' as const,
      },
      title: {
        display: false,
      },
    },
    scales: {
      y: {
        beginAtZero: false,
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
