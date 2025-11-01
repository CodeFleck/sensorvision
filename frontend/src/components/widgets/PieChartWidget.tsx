import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  ChartOptions,
  ChartData,
} from 'chart.js';
import { Pie } from 'react-chartjs-2';

ChartJS.register(ArcElement, Tooltip, Legend);

interface PieChartWidgetProps {
  widget: Widget;
  deviceId?: string;
  latestData?: TelemetryPoint;
}

export const PieChartWidget: React.FC<PieChartWidgetProps> = ({ widget, deviceId, latestData }) => {
  const [data, setData] = useState<ChartData<'pie'> | null>(null);
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
        // For pie chart, we'll show the distribution of all variables for a device
        const telemetryData = await apiService.getLatestForDevice(deviceId);

        // Define variables to include in the pie chart
        const variables = [
          { key: 'kwConsumption', label: 'Power (kW)' },
          { key: 'voltage', label: 'Voltage (V)' },
          { key: 'current', label: 'Current (A)' },
          { key: 'powerFactor', label: 'Power Factor' },
          { key: 'frequency', label: 'Frequency (Hz)' },
        ];

        // Filter to only configured variables if specified
        const configuredVariables = widget.config.variables as string[] | undefined;
        const displayVariables = configuredVariables
          ? variables.filter(v => configuredVariables.includes(v.key))
          : variables;

        const labels = displayVariables.map(v => v.label);
        const values = displayVariables.map(v => {
          const val = telemetryData[v.key as keyof TelemetryPoint];
          return typeof val === 'number' ? Math.abs(val) : 0;
        });

        // Define color palette
        const colors = widget.config.colors || [
          'rgba(59, 130, 246, 0.8)',   // blue
          'rgba(16, 185, 129, 0.8)',   // green
          'rgba(245, 158, 11, 0.8)',   // amber
          'rgba(239, 68, 68, 0.8)',    // red
          'rgba(168, 85, 247, 0.8)',   // purple
        ];

        const borderColors = colors.map(c => c.replace('0.8', '1'));

        setData({
          labels,
          datasets: [
            {
              label: 'Values',
              data: values,
              backgroundColor: colors,
              borderColor: borderColors,
              borderWidth: 1,
            },
          ],
        });
      } catch (error) {
        console.error('Error fetching pie chart data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, widget.config.refreshInterval || 30000);
    return () => clearInterval(interval);
  }, [deviceId, widget.config.variables, widget.config.colors, widget.config.refreshInterval, refreshTrigger]);

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

  const options: ChartOptions<'pie'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: widget.config.showLegend ?? true,
        position: 'right' as const,
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || '';
            const value = context.parsed || 0;
            const total = context.dataset.data.reduce((a: number, b: number) => a + b, 0);
            const percentage = ((value / total) * 100).toFixed(1);
            return `${label}: ${value.toFixed(2)} (${percentage}%)`;
          }
        }
      }
    },
  };

  return (
    <div className="w-full h-full p-4 flex items-center justify-center">
      <div className="w-full h-full max-w-md max-h-96">
        <Pie data={data} options={options} />
      </div>
    </div>
  );
};
