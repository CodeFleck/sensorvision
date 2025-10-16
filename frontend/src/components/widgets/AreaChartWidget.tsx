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
  latestData?: TelemetryPoint;
}

export const AreaChartWidget: React.FC<AreaChartWidgetProps> = ({ widget, latestData }) => {
  const [data, setData] = useState<{ labels: string[]; datasets: Array<Record<string, unknown>> } | null>(null);
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
      if (!widget.deviceId || !widget.variableName) {
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
          widget.deviceId,
          from.toISOString(),
          now.toISOString()
        );

        // Transform data for Chart.js
        const varName = widget.variableName as string;
        const labels = telemetryData.map((point) =>
          new Date(point.timestamp).toLocaleTimeString()
        );
        const values = telemetryData.map((point) => (point as Record<string, unknown>)[varName] as number || 0);

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
              pointRadius: widget.config.showPoints ? 3 : 0,
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
  }, [widget, refreshTrigger]);

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
        display: widget.config.showLegend ?? true,
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
        beginAtZero: widget.config.beginAtZero ?? true,
        grid: {
          display: widget.config.showGrid ?? true,
        },
        ...(widget.config.min !== undefined && { min: widget.config.min }),
        ...(widget.config.max !== undefined && { max: widget.config.max }),
      },
      x: {
        grid: {
          display: widget.config.showGrid ?? true,
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
