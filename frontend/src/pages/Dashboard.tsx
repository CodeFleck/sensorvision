import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { DeviceCard } from '../components/DeviceCard';
import { RealTimeChart } from '../components/RealTimeChart';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { Device, LatestTelemetry, TelemetryPoint } from '../types';
import { Activity, Zap, Cpu } from 'lucide-react';

export const Dashboard = () => {
  const { isAdmin } = useAuth();

  // Redirect admins to admin dashboard
  if (isAdmin) {
    return <Navigate to="/admin-dashboard" replace />;
  }
  const [devices, setDevices] = useState<Device[]>([]);
  const [latestTelemetry, setLatestTelemetry] = useState<Record<string, TelemetryPoint>>({});
  const [loading, setLoading] = useState(true);

  // Dynamically construct WebSocket URL based on current host
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${wsProtocol}//${window.location.host}/ws/telemetry`;
  const { lastMessage, connectionStatus } = useWebSocket(wsUrl);

  useEffect(() => {
    const fetchData = async () => {
      try {
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
      } catch (error) {
        console.error('Failed to fetch data:', error);
      } finally {
        setLoading(false);
      }
    };

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

  const onlineDevices = devices.filter(d => d.status === 'ONLINE').length;
  const totalPower = Object.values(latestTelemetry)
    .reduce((sum, reading) => sum + (reading.kwConsumption || 0), 0);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading dashboard...</div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-600 mt-1">Real-time IoT monitoring overview</p>
      </div>

      {/* Connection Status */}
      <div className="bg-white rounded-lg border border-gray-200 p-4">
        <div className="flex items-center space-x-2">
          <Activity className={`h-4 w-4 ${connectionStatus === 'Open' ? 'text-green-500' : 'text-red-500'}`} />
          <span className="text-sm font-medium">
            Real-time Connection: {connectionStatus}
          </span>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center">
            <Cpu className="h-8 w-8 text-blue-600" />
            <div className="ml-4">
              <div className="text-2xl font-bold text-gray-900">{devices.length}</div>
              <div className="text-sm text-gray-600">Total Devices</div>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center">
            <Activity className="h-8 w-8 text-green-600" />
            <div className="ml-4">
              <div className="text-2xl font-bold text-gray-900">{onlineDevices}</div>
              <div className="text-sm text-gray-600">Online Devices</div>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center">
            <Zap className="h-8 w-8 text-yellow-600" />
            <div className="ml-4">
              <div className="text-2xl font-bold text-gray-900">{totalPower.toFixed(1)} kW</div>
              <div className="text-sm text-gray-600">Total Power</div>
            </div>
          </div>
        </div>
      </div>

      {/* Real-time Chart - Only show when there's telemetry data */}
      {Object.keys(latestTelemetry).length > 0 && (
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Real-time Power Consumption</h2>
          <RealTimeChart telemetryData={Object.values(latestTelemetry)} />
        </div>
      )}

      {/* Device Grid */}
      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Device Overview</h2>
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