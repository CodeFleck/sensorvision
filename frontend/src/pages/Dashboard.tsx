import { useEffect, useState, useCallback, useRef } from 'react';
import { Navigate } from 'react-router-dom';
import { DeviceCard } from '../components/DeviceCard';
import { RealTimeChart } from '../components/RealTimeChart';
import { GettingStarted } from '../components/GettingStarted';
import { FleetHealthGauge } from '../components/FleetHealthGauge';
import { ActivityTimeline } from '../components/ActivityTimeline';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { Device, LatestTelemetry, TelemetryPoint } from '../types';
import {
  Activity,
  AlertTriangle,
  RefreshCw,
  BarChart3,
  ChevronDown,
  TrendingUp,
  TrendingDown,
  Zap,
  Clock,
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

  // Telemetry history for sparklines (store last 10 values per device)
  const telemetryHistoryRef = useRef<Record<string, number[]>>({});

  // AbortController ref to cancel in-flight requests on time range change
  const metricsAbortControllerRef = useRef<AbortController | null>(null);
  // Track if initial data has been fetched to prevent duplicate fetches
  const initialFetchDone = useRef(false);

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

    // Cancel any in-flight request
    if (metricsAbortControllerRef.current) {
      metricsAbortControllerRef.current.abort();
    }

    // Create new AbortController for this request
    const abortController = new AbortController();
    metricsAbortControllerRef.current = abortController;

    try {
      setMetricsLoading(true);
      const { start, end } = getTimeRange(timeRange);

      // Initialize metrics with accumulators for proper averaging
      const powerAcc = { sum: 0, min: Number.MAX_VALUE, max: Number.MIN_VALUE, count: 0 };
      const voltageAcc = { sum: 0, min: Number.MAX_VALUE, max: Number.MIN_VALUE, count: 0 };
      const currentAcc = { sum: 0, min: Number.MAX_VALUE, max: Number.MIN_VALUE, count: 0 };

      // Fetch aggregated data for each device and aggregate across all devices
      const devicePromises = deviceList.map(async (device) => {
        if (abortController.signal.aborted) return;

        try {
          const signal = abortController.signal;
          const [avgPower, minPower, maxPower, avgVoltage, minVoltage, maxVoltage, avgCurrent, minCurrent, maxCurrent] = await Promise.all([
            apiService.getAggregatedData(device.externalId, 'kwConsumption', 'AVG', start, end, 'NONE', { signal }),
            apiService.getAggregatedData(device.externalId, 'kwConsumption', 'MIN', start, end, 'NONE', { signal }),
            apiService.getAggregatedData(device.externalId, 'kwConsumption', 'MAX', start, end, 'NONE', { signal }),
            apiService.getAggregatedData(device.externalId, 'voltage', 'AVG', start, end, 'NONE', { signal }),
            apiService.getAggregatedData(device.externalId, 'voltage', 'MIN', start, end, 'NONE', { signal }),
            apiService.getAggregatedData(device.externalId, 'voltage', 'MAX', start, end, 'NONE', { signal }),
            apiService.getAggregatedData(device.externalId, 'current', 'AVG', start, end, 'NONE', { signal }),
            apiService.getAggregatedData(device.externalId, 'current', 'MIN', start, end, 'NONE', { signal }),
            apiService.getAggregatedData(device.externalId, 'current', 'MAX', start, end, 'NONE', { signal }),
          ]);

          if (abortController.signal.aborted) return;

          // Process power metrics
          if (avgPower && avgPower.length > 0 && avgPower[0]?.value !== undefined) {
            powerAcc.sum += avgPower[0].value;
            powerAcc.count++;
          }
          if (minPower && minPower.length > 0 && minPower[0]?.value !== undefined) {
            powerAcc.min = Math.min(powerAcc.min, minPower[0].value);
          }
          if (maxPower && maxPower.length > 0 && maxPower[0]?.value !== undefined) {
            powerAcc.max = Math.max(powerAcc.max, maxPower[0].value);
          }

          // Process voltage metrics
          if (avgVoltage && avgVoltage.length > 0 && avgVoltage[0]?.value !== undefined) {
            voltageAcc.sum += avgVoltage[0].value;
            voltageAcc.count++;
          }
          if (minVoltage && minVoltage.length > 0 && minVoltage[0]?.value !== undefined) {
            voltageAcc.min = Math.min(voltageAcc.min, minVoltage[0].value);
          }
          if (maxVoltage && maxVoltage.length > 0 && maxVoltage[0]?.value !== undefined) {
            voltageAcc.max = Math.max(voltageAcc.max, maxVoltage[0].value);
          }

          // Process current metrics
          if (avgCurrent && avgCurrent.length > 0 && avgCurrent[0]?.value !== undefined) {
            currentAcc.sum += avgCurrent[0].value;
            currentAcc.count++;
          }
          if (minCurrent && minCurrent.length > 0 && minCurrent[0]?.value !== undefined) {
            currentAcc.min = Math.min(currentAcc.min, minCurrent[0].value);
          }
          if (maxCurrent && maxCurrent.length > 0 && maxCurrent[0]?.value !== undefined) {
            currentAcc.max = Math.max(currentAcc.max, maxCurrent[0].value);
          }
        } catch (err) {
          console.warn(`Failed to fetch metrics for device ${device.externalId}:`, err);
        }
      });

      await Promise.allSettled(devicePromises);

      if (abortController.signal.aborted) return;

      const metrics: MetricsData = {
        power: {
          avg: powerAcc.count > 0 ? powerAcc.sum / powerAcc.count : null,
          min: powerAcc.min !== Number.MAX_VALUE ? powerAcc.min : null,
          max: powerAcc.max !== Number.MIN_VALUE ? powerAcc.max : null,
          count: powerAcc.count,
        },
        voltage: {
          avg: voltageAcc.count > 0 ? voltageAcc.sum / voltageAcc.count : null,
          min: voltageAcc.min !== Number.MAX_VALUE ? voltageAcc.min : null,
          max: voltageAcc.max !== Number.MIN_VALUE ? voltageAcc.max : null,
          count: voltageAcc.count,
        },
        current: {
          avg: currentAcc.count > 0 ? currentAcc.sum / currentAcc.count : null,
          min: currentAcc.min !== Number.MAX_VALUE ? currentAcc.min : null,
          max: currentAcc.max !== Number.MIN_VALUE ? currentAcc.max : null,
          count: currentAcc.count,
        },
      };

      setMetricsData(metrics);
    } catch (err) {
      if (err instanceof Error && err.name === 'AbortError') return;
      console.error('Failed to fetch metrics:', err);
    } finally {
      if (!abortController.signal.aborted) {
        setMetricsLoading(false);
      }
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
        fetchMetrics(devicesData, selectedTimeRange);
      }
    } catch (err) {
      console.error('Failed to fetch data:', err);
      setError('Failed to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Initial data fetch
  useEffect(() => {
    if (!initialFetchDone.current) {
      initialFetchDone.current = true;
      fetchData();
    }

    return () => {
      if (metricsAbortControllerRef.current) {
        metricsAbortControllerRef.current.abort();
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Refetch metrics when time range changes
  useEffect(() => {
    if (!initialFetchDone.current) return;
    if (devices.length > 0) {
      fetchMetrics(devices, selectedTimeRange);
    }
  }, [selectedTimeRange, devices, fetchMetrics]);

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

      // Store telemetry history for sparklines
      const deviceId = lastMessage.deviceId;
      const powerValue = lastMessage.kwConsumption || lastMessage.kw_consumption;
      if (typeof powerValue === 'number') {
        const history = telemetryHistoryRef.current[deviceId] || [];
        telemetryHistoryRef.current[deviceId] = [...history.slice(-9), powerValue];
      }
    }
  }, [lastMessage]);

  // Redirect admins to admin dashboard
  if (isAdmin) {
    return <Navigate to="/admin-dashboard" replace />;
  }

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
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-primary">Dashboard</h1>
          <p className="text-secondary mt-1">Real-time IoT monitoring overview</p>
        </div>
        {/* Connection Status */}
        <div className="flex items-center space-x-2 px-3 py-2 bg-secondary rounded-lg border border-default">
          <div className={clsx(
            'w-2 h-2 rounded-full',
            connectionStatus === 'Open' ? 'bg-success animate-pulse' : 'bg-danger'
          )} />
          <span className={clsx(
            'text-sm font-medium font-mono',
            connectionStatus === 'Open' ? 'text-success' : 'text-danger'
          )}>
            {connectionStatus === 'Open' ? 'Connected: Live' : 'Disconnected'}
          </span>
        </div>
      </div>

      {/* Top Section: Fleet Health + Activity Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Fleet Health Panel */}
        <FleetHealthGauge devices={devices} />

        {/* Activity Timeline */}
        <ActivityTimeline maxItems={10} refreshIntervalMs={30000} />
      </div>

      {/* Historical Metrics Panel */}
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
                  <Zap className="h-5 w-5 text-amber-400" />
                  <h3 className="font-medium text-primary">Power Consumption</h3>
                </div>
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm">Average</span>
                    <span className="font-medium text-primary font-mono">
                      {formatMetricValue(metricsData.power.avg, 'kW')}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingDown className="h-3 w-3 text-emerald-400" />
                      Minimum
                    </span>
                    <span className="font-medium text-emerald-400 font-mono">
                      {formatMetricValue(metricsData.power.min, 'kW')}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingUp className="h-3 w-3 text-rose-400" />
                      Maximum
                    </span>
                    <span className="font-medium text-rose-400 font-mono">
                      {formatMetricValue(metricsData.power.max, 'kW')}
                    </span>
                  </div>
                </div>
              </div>

              {/* Voltage Metrics */}
              <div className="bg-hover rounded-lg p-4">
                <div className="flex items-center gap-2 mb-4">
                  <Activity className="h-5 w-5 text-cyan-400" />
                  <h3 className="font-medium text-primary">Voltage</h3>
                </div>
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm">Average</span>
                    <span className="font-medium text-primary font-mono">
                      {formatMetricValue(metricsData.voltage.avg, 'V', 1)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingDown className="h-3 w-3 text-emerald-400" />
                      Minimum
                    </span>
                    <span className="font-medium text-emerald-400 font-mono">
                      {formatMetricValue(metricsData.voltage.min, 'V', 1)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingUp className="h-3 w-3 text-rose-400" />
                      Maximum
                    </span>
                    <span className="font-medium text-rose-400 font-mono">
                      {formatMetricValue(metricsData.voltage.max, 'V', 1)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Current Metrics */}
              <div className="bg-hover rounded-lg p-4">
                <div className="flex items-center gap-2 mb-4">
                  <Clock className="h-5 w-5 text-purple-400" />
                  <h3 className="font-medium text-primary">Current</h3>
                </div>
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm">Average</span>
                    <span className="font-medium text-primary font-mono">
                      {formatMetricValue(metricsData.current.avg, 'A', 3)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingDown className="h-3 w-3 text-emerald-400" />
                      Minimum
                    </span>
                    <span className="font-medium text-emerald-400 font-mono">
                      {formatMetricValue(metricsData.current.min, 'A', 3)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-secondary text-sm flex items-center gap-1">
                      <TrendingUp className="h-3 w-3 text-rose-400" />
                      Maximum
                    </span>
                    <span className="font-medium text-rose-400 font-mono">
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

      {/* Real-time Chart */}
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
              telemetryHistory={telemetryHistoryRef.current[device.externalId] || []}
            />
          ))}
        </div>
      </div>
    </div>
  );
};
