import { useEffect, useState } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  TimeScale,
} from 'chart.js';
import 'chartjs-adapter-date-fns';
import { Line } from 'react-chartjs-2';
import { TelemetryPoint } from '../types';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  TimeScale
);

interface RealTimeChartProps {
  telemetryData: TelemetryPoint[];
}

interface DataPoint {
  timestamp: string;
  deviceId: string;
  value: number;
}

export const RealTimeChart = ({ telemetryData }: RealTimeChartProps) => {
  const [chartData, setChartData] = useState<DataPoint[]>([]);
  const [timeWindow] = useState(300); // 5 minutes in seconds

  useEffect(() => {
    const now = new Date();
    const cutoffTime = new Date(now.getTime() - timeWindow * 1000);

    const newDataPoints: DataPoint[] = telemetryData
      .filter(point => point.kwConsumption !== undefined)
      .map(point => ({
        timestamp: point.timestamp,
        deviceId: point.deviceId,
        value: point.kwConsumption || 0,
      }));

    setChartData(prevData => {
      // Filter old entries from prevData FIRST to prevent memory buildup
      const recentPrevData = prevData.filter(point =>
        new Date(point.timestamp) > cutoffTime
      );

      // Combine filtered previous data with new data
      const allData = [...recentPrevData, ...newDataPoints];

      // Sort by timestamp
      return allData.sort((a, b) =>
        new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
      );
    });
  }, [telemetryData, timeWindow]);

  // Group data by device for chart datasets
  const deviceData = chartData.reduce((acc, point) => {
    if (!acc[point.deviceId]) {
      acc[point.deviceId] = [];
    }
    acc[point.deviceId].push(point);
    return acc;
  }, {} as Record<string, DataPoint[]>);

  // Generate colors for each device
  const colors = [
    'rgb(59, 130, 246)',  // blue
    'rgb(16, 185, 129)',  // green
    'rgb(245, 158, 11)',  // yellow
    'rgb(239, 68, 68)',   // red
    'rgb(139, 92, 246)',  // purple
    'rgb(236, 72, 153)',  // pink
  ];

  const datasets = Object.entries(deviceData).map(([deviceId, points], index) => ({
    label: deviceId,
    data: points.map(point => ({
      x: point.timestamp,
      y: point.value,
    })),
    borderColor: colors[index % colors.length],
    backgroundColor: colors[index % colors.length] + '20',
    borderWidth: 2,
    fill: false,
    tension: 0.1,
  }));

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index' as const,
      intersect: false,
    },
    plugins: {
      title: {
        display: true,
        text: 'Real-time Power Consumption (kW)',
      },
      legend: {
        position: 'top' as const,
      },
    },
    scales: {
      x: {
        type: 'time' as const,
        time: {
          displayFormats: {
            minute: 'HH:mm',
            second: 'HH:mm:ss',
          },
        },
        title: {
          display: true,
          text: 'Time',
        },
      },
      y: {
        title: {
          display: true,
          text: 'Power (kW)',
        },
        beginAtZero: true,
      },
    },
  };

  return (
    <div className="h-96">
      {datasets.length > 0 ? (
        <Line data={{ datasets }} options={options} />
      ) : (
        <div className="flex items-center justify-center h-full text-gray-500">
          No telemetry data available
        </div>
      )}
    </div>
  );
};