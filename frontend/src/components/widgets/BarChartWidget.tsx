import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ChartOptions,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend);

interface BarChartWidgetProps {
  widget: Widget;
  latestData?: TelemetryPoint;
}

export const BarChartWidget: React.FC<BarChartWidgetProps> = ({ widget, latestData }) => {
  const [data, setData] = useState<any>(null);
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
        const now = new Date();
        const minutesAgo = widget.timeRangeMinutes || 60;
        const from = new Date(now.getTime() - minutesAgo * 60000);

        const telemetryData = await apiService.queryTelemetry(
          widget.deviceId,
          from.toISOString(),
          now.toISOString()
        );

        const varName = widget.variableName as string;
        const labels = telemetryData.map((point) =>
          new Date(point.timestamp).toLocaleTimeString()
        );
        const values = telemetryData.map((point) => (point as any)[varName] || 0);

        setData({
          labels,
          datasets: [
            {
              label: widget.variableName,
              data: values,
              backgroundColor: widget.config.colors?.[0] || 'rgba(59, 130, 246, 0.5)',
              borderColor: widget.config.colors?.[0] || 'rgb(59, 130, 246)',
              borderWidth: 1,
            },
          ],
        });
      } catch (error) {
        console.error('Error fetching bar chart data:', error);
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

  const options: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: widget.config.showLegend ?? true,
        position: 'top' as const,
      },
    },
    scales: {
      y: {
        beginAtZero: true,
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
      <Bar data={data} options={options} />
    </div>
  );
};
