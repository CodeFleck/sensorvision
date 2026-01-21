import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';
import {
  Chart as ChartJS,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ChartOptions,
} from 'chart.js';
import { Scatter } from 'react-chartjs-2';

ChartJS.register(LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

interface ScatterChartWidgetProps {
  widget: Widget;
  deviceId?: string;
  latestData?: TelemetryPoint;
}

interface ScatterChartData {
  datasets: Array<{
    label: string;
    data: Array<{ x: number; y: number }>;
    backgroundColor: string;
    borderColor: string;
    pointRadius: number;
    pointHoverRadius: number;
  }>;
}

export const ScatterChartWidget: React.FC<ScatterChartWidgetProps> = ({ widget, deviceId, latestData }) => {
  const [data, setData] = useState<ScatterChartData | null>(null);
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
      if (!deviceId) {
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

        // For scatter chart, we need X and Y variables
        // X-axis variable from config or default to timestamp
        const xVar = (widget.config.xVariable as string | undefined) || 'timestamp';
        const yVar = widget.variableName || (widget.config.yVariable as string | undefined) || 'kwConsumption';

        // Convert snake_case to camelCase for API property access
        const toCamelCase = (str: string) => str.replace(/_([a-z])/g, (g) => g[1].toUpperCase());
        const xAccessKey = toCamelCase(xVar);
        const yAccessKey = toCamelCase(yVar);

        // Transform data for scatter plot
        const scatterData = telemetryData.map((point) => {
          let xValue: number;

          if (xVar === 'timestamp') {
            xValue = new Date(point.timestamp).getTime();
          } else {
            xValue = (point[xAccessKey] as number) || 0;
          }

          const yValue = (point[yAccessKey] as number) || 0;

          return {
            x: xValue,
            y: yValue,
          };
        });

        const borderColor = widget.config.colors?.[0] || 'rgb(59, 130, 246)';
        const backgroundColor = widget.config.colors?.[1] || 'rgba(59, 130, 246, 0.5)';

        setData({
          datasets: [
            {
              label: `${yVar} vs ${xVar}`,
              data: scatterData,
              backgroundColor: backgroundColor,
              borderColor: borderColor,
              pointRadius: (widget.config.pointSize as number | undefined) || 5,
              pointHoverRadius: (widget.config.pointSize as number | undefined) ? (widget.config.pointSize as number) + 2 : 7,
            },
          ],
        });
      } catch (error) {
        console.error('Error fetching scatter chart data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, widget.config.refreshInterval || 30000);
    return () => clearInterval(interval);
  }, [deviceId, widget.variableName, widget.timeRangeMinutes, widget.config.xVariable, widget.config.yVariable, widget.config.refreshInterval, refreshTrigger]);

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

  const xVar = (widget.config.xVariable as string | undefined) || 'timestamp';

  const options: ChartOptions<'scatter'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: (widget.config.showLegend as boolean | undefined) ?? true,
        position: 'top' as const,
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const point = context.parsed;
            if (!point || point.x == null || point.y == null) return '';

            let xLabel = '';

            if (xVar === 'timestamp') {
              xLabel = new Date(point.x).toLocaleTimeString();
            } else {
              xLabel = point.x.toFixed(2);
            }

            return `${context.dataset.label}: (${xLabel}, ${point.y.toFixed(2)})`;
          }
        }
      }
    },
    scales: {
      x: {
        type: 'linear',
        position: 'bottom',
        title: {
          display: true,
          text: (xVar === 'timestamp' ? 'Time' : xVar) as string,
        },
        grid: {
          display: (widget.config.showGrid as boolean | undefined) ?? true,
        },
        ticks: xVar === 'timestamp' ? {
          callback: function(value) {
            return new Date(value as number).toLocaleTimeString();
          }
        } : undefined,
      },
      y: {
        title: {
          display: true,
          text: (widget.variableName || (widget.config.yVariable as string | undefined) || 'Value') as string,
        },
        grid: {
          display: (widget.config.showGrid as boolean | undefined) ?? true,
        },
        ...(widget.config.min !== undefined && { min: widget.config.min as number }),
        ...(widget.config.max !== undefined && { max: widget.config.max as number }),
      },
    },
  };

  return (
    <div className="w-full h-full p-2">
      <Scatter data={data} options={options} />
    </div>
  );
};
