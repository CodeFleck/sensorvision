import { useEffect, useState, useMemo } from 'react';
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
import { ChevronDown } from 'lucide-react';

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

// Set default font to match dashboard UI
ChartJS.defaults.font.family = "'Satoshi', system-ui, sans-serif";

interface RealTimeChartProps {
  telemetryData: TelemetryPoint[];
}

interface DataPoint {
  timestamp: string;
  deviceId: string;
  value: number;
}

// Common variable display names and units
const VARIABLE_CONFIG: Record<string, { label: string; unit: string }> = {
  kwConsumption: { label: 'Power Consumption', unit: 'kW' },
  kw_consumption: { label: 'Power Consumption', unit: 'kW' },
  voltage: { label: 'Voltage', unit: 'V' },
  current: { label: 'Current', unit: 'A' },
  powerFactor: { label: 'Power Factor', unit: '' },
  power_factor: { label: 'Power Factor', unit: '' },
  frequency: { label: 'Frequency', unit: 'Hz' },
  temperature: { label: 'Temperature', unit: 'C' },
  humidity: { label: 'Humidity', unit: '%' },
  pressure: { label: 'Pressure', unit: 'bar' },
  flow_rate: { label: 'Flow Rate', unit: 'L/min' },
  vibration: { label: 'Vibration', unit: 'mm/s' },
};

// Get display name for a variable
const getVariableLabel = (varName: string): string => {
  if (VARIABLE_CONFIG[varName]) {
    return VARIABLE_CONFIG[varName].label;
  }
  // Convert snake_case or camelCase to Title Case
  return varName
    .replace(/_/g, ' ')
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, str => str.toUpperCase())
    .trim();
};

// Get unit for a variable
const getVariableUnit = (varName: string): string => {
  if (VARIABLE_CONFIG[varName]) {
    return VARIABLE_CONFIG[varName].unit;
  }
  return '';
};

export const RealTimeChart = ({ telemetryData }: RealTimeChartProps) => {
  const [chartData, setChartData] = useState<DataPoint[]>([]);
  const [timeWindow] = useState(300); // 5 minutes in seconds
  const [selectedVariable, setSelectedVariable] = useState<string>('');

  // Extract all available numeric variables from telemetry data
  const availableVariables = useMemo(() => {
    const variables = new Set<string>();
    const excludeKeys = ['deviceId', 'timestamp', 'latitude', 'longitude', 'altitude'];

    telemetryData.forEach(point => {
      Object.entries(point).forEach(([key, value]) => {
        if (!excludeKeys.includes(key) && typeof value === 'number') {
          variables.add(key);
        }
      });
    });

    return Array.from(variables).sort();
  }, [telemetryData]);

  // Auto-select first available variable if none selected
  useEffect(() => {
    if (!selectedVariable && availableVariables.length > 0) {
      // Prefer common variables
      const preferred = ['kwConsumption', 'kw_consumption', 'temperature', 'voltage', 'power'];
      const found = preferred.find(v => availableVariables.includes(v));
      setSelectedVariable(found || availableVariables[0]);
    }
  }, [availableVariables, selectedVariable]);

  useEffect(() => {
    if (!selectedVariable) return;

    const now = new Date();
    const cutoffTime = new Date(now.getTime() - timeWindow * 1000);

    const newDataPoints: DataPoint[] = telemetryData
      .filter(point => {
        const value = point[selectedVariable];
        return typeof value === 'number' && value !== undefined;
      })
      .map(point => ({
        timestamp: point.timestamp,
        deviceId: point.deviceId,
        value: point[selectedVariable] as number,
      }));

    setChartData(prevData => {
      // Filter old entries from prevData FIRST to prevent memory buildup
      const recentPrevData = prevData.filter(point =>
        new Date(point.timestamp) > cutoffTime
      );

      // Create a Set of existing keys for O(1) deduplication lookup
      const existingKeys = new Set(
        recentPrevData.map(p => `${p.deviceId}-${p.timestamp}`)
      );

      // Only add new points that don't already exist (prevents duplicates from WebSocket reconnections)
      const uniqueNewPoints = newDataPoints.filter(
        p => !existingKeys.has(`${p.deviceId}-${p.timestamp}`)
      );

      // Combine filtered previous data with unique new data
      const allData = [...recentPrevData, ...uniqueNewPoints];

      // Sort by timestamp
      return allData.sort((a, b) =>
        new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
      );
    });
  }, [telemetryData, timeWindow, selectedVariable]);

  // Clear chart data when variable changes
  useEffect(() => {
    setChartData([]);
  }, [selectedVariable]);

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

  const variableLabel = getVariableLabel(selectedVariable);
  const variableUnit = getVariableUnit(selectedVariable);
  const yAxisLabel = variableUnit ? `${variableLabel} (${variableUnit})` : variableLabel;

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
        text: `Real-time ${variableLabel}${variableUnit ? ` (${variableUnit})` : ''}`,
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
          text: yAxisLabel,
        },
        beginAtZero: true,
      },
    },
  };

  return (
    <div>
      {/* Variable Selector */}
      {availableVariables.length > 1 && (
        <div className="mb-4 flex items-center justify-end">
          <label className="text-sm text-secondary mr-2">Variable:</label>
          <div className="relative">
            <select
              value={selectedVariable}
              onChange={(e) => setSelectedVariable(e.target.value)}
              className="appearance-none bg-primary border border-default rounded-lg px-3 py-1.5 pr-8 text-sm text-primary focus:ring-2 focus:ring-link focus:border-link cursor-pointer"
            >
              {availableVariables.map(varName => (
                <option key={varName} value={varName}>
                  {getVariableLabel(varName)}
                </option>
              ))}
            </select>
            <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-secondary pointer-events-none" />
          </div>
        </div>
      )}

      <div className="h-96">
        {datasets.length > 0 ? (
          <Line data={{ datasets }} options={options} />
        ) : (
          <div className="flex items-center justify-center h-full text-secondary">
            {availableVariables.length === 0
              ? 'No telemetry data available'
              : `No data available for ${variableLabel}`}
          </div>
        )}
      </div>
    </div>
  );
};
