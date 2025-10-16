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

export const ScatterChartWidget: React.FC<ScatterChartWidgetProps> = ({ widget, latestData }) => {
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
      if (!widget.deviceId) {
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

        // For scatter chart, we need X and Y variables
        // X-axis variable from config or default to timestamp
        const xVar = widget.config.xVariable || 'timestamp';
        const yVar = widget.variableName || widget.config.yVariable || 'kwConsumption';

        // Transform data for scatter plot
        const scatterData = (telemetryData as Array<Record<string, unknown>>).map((point) => {
          let xValue: number;

          if (xVar === 'timestamp') {
            xValue = new Date(point.timestamp as string).getTime();
          } else {
            xValue = (point[xVar] as number) || 0;
          }

          const yValue = (point[yVar] as number) || 0;

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
              pointRadius: widget.config.pointSize || 5,
              pointHoverRadius: widget.config.pointSize ? widget.config.pointSize + 2 : 7,
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

  const xVar = widget.config.xVariable || 'timestamp';

  const options: ChartOptions<'scatter'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: widget.config.showLegend ?? true,
        position: 'top' as const,
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const point = context.parsed;
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
          text: xVar === 'timestamp' ? 'Time' : xVar,
        },
        grid: {
          display: widget.config.showGrid ?? true,
        },
        ticks: xVar === 'timestamp' ? {
          callback: function(value) {
            return new Date(value).toLocaleTimeString();
          }
        } : undefined,
      },
      y: {
        title: {
          display: true,
          text: widget.variableName || widget.config.yVariable || 'Value',
        },
        grid: {
          display: widget.config.showGrid ?? true,
        },
        ...(widget.config.min !== undefined && { min: widget.config.min }),
        ...(widget.config.max !== undefined && { max: widget.config.max }),
      },
    },
  };

  return (
    <div className="w-full h-full p-2">
      <Scatter data={data} options={options} />
    </div>
  );
};
