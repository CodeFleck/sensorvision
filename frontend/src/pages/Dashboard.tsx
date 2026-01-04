import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { DeviceCard } from '../components/DeviceCard';
import { RealTimeChart } from '../components/RealTimeChart';
import { GettingStarted } from '../components/GettingStarted';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { Device, LatestTelemetry, TelemetryPoint } from '../types';
import { Activity, Zap, Cpu, AlertTriangle, RefreshCw } from 'lucide-react';
import { Card, CardBody } from '../components/ui/Card';
import { Button } from '../components/ui/Button';

export const Dashboard = () => {
  const { isAdmin } = useAuth();
  const [devices, setDevices] = useState<Device[]>([]);
  const [latestTelemetry, setLatestTelemetry] = useState<Record<string, TelemetryPoint>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Dynamically construct WebSocket URL based on current host
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${wsProtocol}//${window.location.host}/ws/telemetry`;
  const { lastMessage, connectionStatus } = useWebSocket(wsUrl);

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
     
  }, []);

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