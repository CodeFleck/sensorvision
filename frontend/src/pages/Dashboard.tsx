import { useEffect, useState, useCallback, useMemo } from 'react';
import { Navigate } from 'react-router-dom';
import { DeviceCard } from '../components/DeviceCard';
import { RealTimeChart } from '../components/RealTimeChart';
import { GettingStarted } from '../components/GettingStarted';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { Device, LatestTelemetry, TelemetryPoint } from '../types';
import {
  Activity,
  Zap,
  Cpu,
  AlertTriangle,
  RefreshCw,
  Clock,
  TrendingUp,
  TrendingDown,
  BarChart3,
  ChevronDown,
} from 'lucide-react';
import { Card, CardBody } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { clsx } from 'clsx';

// Time range options for metrics panel
const TIME_RANGES = [
  { label: '1 Hour', value: '1h', hours: 1 },
  { label: '6 Hours', value: '6h', hours: 6 },
  { label: '12 Hours', value: '12h', hours: 12 },
  { label: '24 Hours', value: '24h', hours: 24 },
] as const;

type TimeRangeValue = typeof TIME_RANGES[number]['value'];

interface AggregatedMetrics {
  avg: number | null;
  min: number | null;
  max: number | null;
  count: number;
}

interface MetricsData {
  power: AggregatedMetrics;
  voltage: AggregatedMetrics;
  current: AggregatedMetrics;
}

export const Dashboard = () => {
  const { isAdmin } = useAuth();
  const [devices, setDevices] = useState<Device[]>([]);
  const [latestTelemetry, setLatestTelemetry] = useState<Record<string, TelemetryPoint>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Metrics panel state
  const [selectedTimeRange, setSelectedTimeRange] = useState<TimeRangeValue>('1h');
  const [metricsData, setMetricsData] = useState<MetricsData | null>(null);
  const [metricsLoading, setMetricsLoading] = useState(false);

  // Dynamically construct WebSocket URL based on current host
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${wsProtocol}//${window.location.host}/ws/telemetry`;
  const { lastMessage, connectionStatus } = useWebSocket(wsUrl);

  // Calculate time range based on selection
  const getTimeRange = useCallback((range: TimeRangeValue) => {
    const hours = TIME_RANGES.find(r => r.value === range)?.hours || 1;
    const now = new Date();
    const start = new Date(now.getTime() - hours * 60 * 60 * 1000);
    return {
      start: start.toISOString(),
      end: now.toISOString(),
    };
  }, []);

  // Fetch aggregated metrics for the selected time range
  const fetchMetrics = useCallback(async (deviceList: Device[], timeRange: TimeRangeValue) => {
    if (deviceList.length === 0) return;

    try {
      setMetricsLoading(true);
      const { start, end } = getTimeRange(timeRange);

      // Initialize metrics
      const metrics: MetricsData = {
        power: { avg: null, min: null, max: null, count: 0 },
        voltage: { avg: null, min: null, max: null, count: 0 },
        current: { avg: null, min: null, max: null, count: 0 },
      };

      // Fetch aggregated data for each device and aggregate across all devices
      // We'll use the first device that has data for simplicity
      // In a real implementation, you might want to aggregate across all devices
      for (const device of deviceList) {
        try {
          // Fetch power metrics
          const [avgPower, minPower, maxPower] = await Promise.all([
            apiService.getAggregatedData(device.externalId, 'kwConsumption', 'AVG', start, end, 'NONE'),
            apiService.getAggregatedData(device.externalId, 'kwConsumption', 'MIN', start, end, 'NONE'),
            apiService.getAggregatedData(device.externalId, 'kwConsumption', 'MAX', start, end, 'NONE'),
          ]);

          // Parse the aggregation results
          if (avgPower && avgPower.length > 0) {
            const avgVal = avgPower[0]?.value;
            const minVal = minPower[0]?.value;
            const maxVal = maxPower[0]?.value;

            if (avgVal !== undefined) {
              metrics.power.avg = metrics.power.avg === null
                ? avgVal
                : (metrics.power.avg + avgVal) / 2;
            }
            if (minVal !== undefined) {
              metrics.power.min = metrics.power.min === null
                ? minVal
                : Math.min(metrics.power.min, minVal);
            }
            if (maxVal !== undefined) {
              metrics.power.max = metrics.power.max === null
                ? maxVal
                : Math.max(metrics.power.max, maxVal);
            }
            metrics.power.count++;
          }

          // Fetch voltage metrics
          const [avgVoltage, minVoltage, maxVoltage] = await Promise.all([
            apiService.getAggregatedData(device.externalId, 'voltage', 'AVG', start, end, 'NONE'),
            apiService.getAggregatedData(device.externalId, 'voltage', 'MIN', start, end, 'NONE'),
            apiService.getAggregatedData(device.externalId, 'voltage', 'MAX', start, end, 'NONE'),
          ]);

          if (avgVoltage && avgVoltage.length > 0) {
            const avgVal = avgVoltage[0]?.value;
            const minVal = minVoltage[0]?.value;
            const maxVal = maxVoltage[0]?.value;

            if (avgVal !== undefined) {
              metrics.voltage.avg = metrics.voltage.avg === null
                ? avgVal
                : (metrics.voltage.avg + avgVal) / 2;
            }
            if (minVal !== undefined) {
              metrics.voltage.min = metrics.voltage.min === null
                ? minVal
                : Math.min(metrics.voltage.min, minVal);
            }
            if (maxVal !== undefined) {
              metrics.voltage.max = metrics.voltage.max === null
                ? maxVal
                : Math.max(metrics.voltage.max, maxVal);
            }
            metrics.voltage.count++;
          }

          // Fetch current metrics
          const [avgCurrent, minCurrent, maxCurrent] = await Promise.all([
            apiService.getAggregatedData(device.externalId, 'current', 'AVG', start, end, 'NONE'),
            apiService.getAggregatedData(device.externalId, 'current', 'MIN', start, end, 'NONE'),
            apiService.getAggregatedData(device.externalId, 'current', 'MAX', start, end, 'NONE'),
          ]);

          if (avgCurrent && avgCurrent.length > 0) {
            const avgVal = avgCurrent[0]?.value;
            const minVal = minCurrent[0]?.value;
            const maxVal = maxCurrent[0]?.value;

            if (avgVal !== undefined) {
              metrics.current.avg = metrics.current.avg === null
                ? avgVal
                : (metrics.current.avg + avgVal) / 2;
            }
            if (minVal !== undefined) {
              metrics.current.min = metrics.current.min === null
                ? minVal
                : Math.min(metrics.current.min, minVal);
            }
            if (maxVal !== undefined) {
              metrics.current.max = metrics.current.max === null
                ? maxVal
                : Math.max(metrics.current.max, maxVal);
            }
            metrics.current.count++;
          }
        } catch (err) {
          // Skip this device if metrics fetch fails
          console.warn(`Failed to fetch metrics for device ${device.externalId}:`, err);
        }
      }

      setMetricsData(metrics);
    } catch (err) {
      console.error('Failed to fetch metrics:', err);
    } finally {
      setMetricsLoading(false);
    }
  }, [getTimeRange]);

  const fetchData = async () => {
    try {
      setError(null);
      const devicesData = await apiService.getDevices();
      setDevices(devicesData);

      if (devicesData.length > 0) {
        const telemetryData = await apiService.getLatestTelemetry(
          devicesData.map(d => d.externalId)
        );

        const telemetryMap = telemetryData.reduce((acc: Record<string, TelemetryPoint>, item: LatestTelemetry) => {
          if (item.latest) {
            acc[item.deviceId] = item.latest;
          }
          return acc;
        }, {});

        setLatestTelemetry(telemetryMap);

        // Fetch metrics for the selected time range
        fetchMetrics(devicesData, selectedTimeRange);
      }
    } catch (err) {
      console.error('Failed to fetch data:', err);
      setError('Failed to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Refetch metrics when time range changes
  useEffect(() => {
    if (devices.length > 0) {
      fetchMetrics(devices, selectedTimeRange);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedTimeRange]);

  // Handle time range change
  const handleTimeRangeChange = (value: TimeRangeValue) => {
    setSelectedTimeRange(value);
  };

  // Format metric value for display
  const formatMetricValue = (value: number | null, unit: string, decimals = 2): string => {
    if (value === null) return '-';
    return `${value.toFixed(decimals)} ${unit}`;
  };

  // Update telemetry when new WebSocket message arrives
  useEffect(() => {
    if (lastMessage) {
      setLatestTelemetry(prev => ({
        ...prev,
        [lastMessage.deviceId]: lastMessage,
      }));
    }
  }, [lastMessage]);

  // Redirect admins to admin dashboard - placed after all hooks to comply with Rules of Hooks
  if (isAdmin) {
    return <Navigate to="/admin-dashboard" replace />;
  }

  const onlineDevices = devices.filter(d => d.status === 'ONLINE').length;
  const totalPower = Object.values(latestTelemetry)
    .reduce((sum, reading) => sum + (reading.kwConsumption || 0), 0);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-secondary">Loading dashboard...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <AlertTriangle className="mx-auto h-12 w-12 text-danger mb-4" />
          <p className="text-danger mb-4">{error}</p>
          <Button
            onClick={() => {
              setLoading(true);
              fetchData();
            }}
            variant="primary"
          >
            <RefreshCw className="h-4 w-4 mr-2" />
            Try Again
          </Button>
        </div>
      </div>
    );
  }

  // Show Getting Started when user has no devices
  if (devices.length === 0) {
    return (
      <div className="space-y-8">
        <div>
          <h1 className="text-2xl font-bold text-primary">Dashboard</h1>
          <p className="text-secondary mt-1">Real-time IoT monitoring overview</p>
        </div>
        <GettingStarted />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-primary">Dashboard</h1>
        <p className="text-secondary mt-1">Real-time IoT monitoring overview</p>
      </div>

      {/* Connection Status */}
      <Card>
        <CardBody>
          <div className="flex items-center space-x-2">
            <Activity className={`h-4 w-4 ${connectionStatus === 'Open' ? 'text-success' : 'text-danger'}`} />
            <span className="text-sm font-medium text-primary">
              Real-time Connection: {connectionStatus}
            </span>
          </div>
        </CardBody>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card>
          <CardBody>
            <div className="flex items-center">
              <Cpu className="h-8 w-8 text-link" />
              <div className="ml-4">
                <div className="text-2xl font-bold text-primary">{devices.length}</div>
                <div className="text-sm text-secondary">Total Devices</div>
              </div>
            </div>
          </CardBody>
        </Card>

        <Card>
          <CardBody>
            <div className="flex items-center">
              <Activity className="h-8 w-8 text-success" />
              <div className="ml-4">
                <div className="text-2xl font-bold text-primary">{onlineDevices}</div>
                <div className="text-sm text-secondary">Online Devices</div>
              </div>
            </div>
          </CardBody>
        </Card>

        <Card>
          <CardBody>
            <div className="flex items-center">
              <Zap className="h-8 w-8 text-warning" />
              <div className="ml-4">
                <div className="text-2xl font-bold text-primary">{totalPower.toFixed(1)} kW</div>
                <div className="text-sm text-secondary">Total Power</div>
              </div>
            </div>
          </CardBody>
        </Card>
      </div>

      {/* Historical Metrics Panel - Always Visible */}
      <Card>
        <CardBody>
          {/* Header with Time Range Dropdown */}
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5 text-link" />
              <h2 className="text-lg font-semibold text-primary">Historical Metrics</h2>
            </div>
            <div className="flex items-center gap-3">
              {/* Time Range Dropdown */}
              <div className="relative">
                <select
                  value={selectedTimeRange}
                  onChange={(e) => handleTimeRangeChange(e.target.value as TimeRangeValue)}
                  className="appearance-none pl-3 pr-8 py-2 border border-default rounded-lg bg-primary text-primary text-sm focus:ring-2 focus:ring-link focus:border-link cursor-pointer"
                  disabled={metricsLoading}
                >
                  {TIME_RANGES.map((range) => (
                    <option key={range.value} value={range.value}>
                      {range.label}
                    </option>
                  ))}
                </select>
                <ChevronDown className="absolute right-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-secondary pointer-events-none" />
              </div>
              {/* Refresh button */}
              <button
                onClick={() => fetchMetrics(devices, selectedTimeRange)}
                disabled={metricsLoading}
                className="p-2 text-secondary hover:text-primary hover:bg-hover rounded-lg transition-colors disabled:opacity-50"
                title="Refresh metrics"
              >
                <RefreshCw className={clsx('h-4 w-4', metricsLoading && 'animate-spin')} />
              </button>
            </div>
          </div>

          {/* Loading state */}
          {metricsLoading && !metricsData && (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-6 w-6 animate-spin text-link mr-2" />
              <span className="text-secondary">Loading metrics...</span>
            </div>
          )}

          {/* Metrics Grid */}
          {metricsData && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Power Consumption Metrics */}
              <div className="bg-hover rounded-lg p-4">
                <div className="flex items-center gap-2 mb-4">
                  <Zap className="h-5 w-5 text-yellow-500" />
                  <h3 className="font-medium text-primary">Power Consumption</h3>
                </div>
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm">Average</span>
                    <span className="font-medium text-primary">
                      {formatMetricValue(metricsData.power.avg, 'kW')}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingDown className="h-3 w-3 text-green-500" />
                      Minimum
                    </span>
                    <span className="font-medium text-green-600">
                      {formatMetricValue(metricsData.power.min, 'kW')}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingUp className="h-3 w-3 text-red-500" />
                      Maximum
                    </span>
                    <span className="font-medium text-red-600">
                      {formatMetricValue(metricsData.power.max, 'kW')}
                    </span>
                  </div>
                </div>
              </div>

              {/* Voltage Metrics */}
              <div className="bg-hover rounded-lg p-4">
                <div className="flex items-center gap-2 mb-4">
                  <Activity className="h-5 w-5 text-blue-500" />
                  <h3 className="font-medium text-primary">Voltage</h3>
                </div>
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm">Average</span>
                    <span className="font-medium text-primary">
                      {formatMetricValue(metricsData.voltage.avg, 'V', 1)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingDown className="h-3 w-3 text-green-500" />
                      Minimum
                    </span>
                    <span className="font-medium text-green-600">
                      {formatMetricValue(metricsData.voltage.min, 'V', 1)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingUp className="h-3 w-3 text-red-500" />
                      Maximum
                    </span>
                    <span className="font-medium text-red-600">
                      {formatMetricValue(metricsData.voltage.max, 'V', 1)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Current Metrics */}
              <div className="bg-hover rounded-lg p-4">
                <div className="flex items-center gap-2 mb-4">
                  <Clock className="h-5 w-5 text-purple-500" />
                  <h3 className="font-medium text-primary">Current</h3>
                </div>
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm">Average</span>
                    <span className="font-medium text-primary">
                      {formatMetricValue(metricsData.current.avg, 'A', 3)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingDown className="h-3 w-3 text-green-500" />
                      Minimum
                    </span>
                    <span className="font-medium text-green-600">
                      {formatMetricValue(metricsData.current.min, 'A', 3)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingUp className="h-3 w-3 text-red-500" />
                      Maximum
                    </span>
                    <span className="font-medium text-red-600">
                      {formatMetricValue(metricsData.current.max, 'A', 3)}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* No data state */}
          {!metricsLoading && !metricsData && (
            <div className="text-center py-8 text-secondary">
              <BarChart3 className="h-8 w-8 mx-auto mb-2 opacity-50" />
              <p>No historical data available for the selected time range.</p>
            </div>
          )}
        </CardBody>
      </Card>

      {/* Real-time Chart - Only show when there's telemetry data */}
      {Object.keys(latestTelemetry).length > 0 && (
        <Card>
          <CardBody>
            <h2 className="text-lg font-semibold text-primary mb-4">Real-time Telemetry</h2>
            <RealTimeChart telemetryData={Object.values(latestTelemetry)} />
          </CardBody>
        </Card>
      )}

      {/* Device Grid */}
      <div>
        <h2 className="text-lg font-semibold text-primary mb-4">Device Overview</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {devices.map((device) => (
            <DeviceCard
              key={device.externalId}
              device={device}
              latestTelemetry={latestTelemetry[device.externalId]}
            />
          ))}
        </div>
      </div>
    </div>
  );
};