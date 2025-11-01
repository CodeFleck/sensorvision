import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';

// Fix for default marker icon issues with Webpack
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';
import iconRetina from 'leaflet/dist/images/marker-icon-2x.png';

L.Icon.Default.mergeOptions({
  iconRetinaUrl: iconRetina,
  iconUrl: icon,
  shadowUrl: iconShadow,
});

interface MapWidgetProps {
  widget: Widget;
  deviceId?: string;
  latestData?: TelemetryPoint;
}

interface DeviceLocation {
  deviceId: string;
  name: string;
  latitude: number;
  longitude: number;
  altitude?: number;
  lastSeen?: string;
  status?: string;
}

// Custom component to handle map recenter when devices change
function MapRecenter({ center }: { center: [number, number] }) {
  const map = useMap();
  useEffect(() => {
    map.setView(center, map.getZoom());
  }, [center, map]);
  return null;
}

export const MapWidget: React.FC<MapWidgetProps> = ({ widget, deviceId, latestData }) => {
  const [devices, setDevices] = useState<DeviceLocation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Default center (San Francisco)
  const defaultCenter: [number, number] = [37.7749, -122.4194];

  // Calculate map center based on devices
  const mapCenter: [number, number] = devices.length > 0
    ? [
        devices.reduce((sum, d) => sum + d.latitude, 0) / devices.length,
        devices.reduce((sum, d) => sum + d.longitude, 0) / devices.length,
      ]
    : defaultCenter;

  useEffect(() => {
    const fetchDeviceLocations = async () => {
      try {
        setError(null);

        if (deviceId) {
          // Single device mode
          const telemetry = await apiService.getLatestForDevice(deviceId);

          if (telemetry.latitude !== null && telemetry.longitude !== null) {
            setDevices([{
              deviceId: deviceId,
              name: deviceId,
              latitude: telemetry.latitude || 0,
              longitude: telemetry.longitude || 0,
              altitude: telemetry.altitude,
              lastSeen: telemetry.timestamp,
              status: 'online',
            }]);
          } else {
            setDevices([]);
          }
        } else {
          // Multi-device mode - show all devices with location data
          try {
            const allDevices = await apiService.getDevices();
            const devicesWithLocation = allDevices
              .filter(d => d.latitude != null && d.longitude != null)
              .map(d => ({
                deviceId: d.externalId,
                name: d.name || d.externalId,
                latitude: d.latitude as number,
                longitude: d.longitude as number,
                altitude: d.altitude,
                lastSeen: d.lastSeenAt,
                status: d.status,
              }));

            setDevices(devicesWithLocation);
          } catch (err) {
            console.warn('Multi-device map endpoint not available, showing placeholder');
            setDevices([]);
          }
        }
      } catch (error) {
        console.error('Error fetching device locations:', error);
        setError('Failed to load device locations');
      } finally {
        setLoading(false);
      }
    };

    fetchDeviceLocations();
    const interval = setInterval(fetchDeviceLocations, widget.config.refreshInterval || 30000);
    return () => clearInterval(interval);
  }, [deviceId, widget.config.refreshInterval, latestData]);

  if (loading) {
    return (
      <div className="flex items-center justify-center w-full h-full bg-gray-900">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center w-full h-full text-red-400 bg-gray-900">
        <svg className="w-16 h-16 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
        <p>{error}</p>
      </div>
    );
  }

  if (devices.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center w-full h-full text-gray-400 bg-gray-900">
        <svg className="w-16 h-16 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"
          />
        </svg>
        <p>No location data available</p>
        <p className="text-xs mt-2">Devices must report latitude/longitude in telemetry</p>
      </div>
    );
  }

  return (
    <div className="relative w-full h-full">
      <MapContainer
        center={mapCenter}
        zoom={13}
        style={{ height: '100%', width: '100%' }}
        className="z-0"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <MapRecenter center={mapCenter} />

        {devices.map((device) => (
          <Marker
            key={device.deviceId}
            position={[device.latitude, device.longitude]}
          >
            <Popup>
              <div className="text-sm">
                <h3 className="font-bold text-lg mb-2">{device.name}</h3>
                <div className="space-y-1">
                  <p>
                    <span className="font-semibold">Device ID:</span> {device.deviceId}
                  </p>
                  <p>
                    <span className="font-semibold">Location:</span>{' '}
                    {device.latitude.toFixed(6)}, {device.longitude.toFixed(6)}
                  </p>
                  {device.altitude && (
                    <p>
                      <span className="font-semibold">Altitude:</span> {device.altitude.toFixed(2)}m
                    </p>
                  )}
                  {device.status && (
                    <p>
                      <span className="font-semibold">Status:</span>{' '}
                      <span
                        className={`font-medium ${
                          device.status === 'ONLINE'
                            ? 'text-green-600'
                            : device.status === 'OFFLINE'
                            ? 'text-red-600'
                            : 'text-yellow-600'
                        }`}
                      >
                        {device.status}
                      </span>
                    </p>
                  )}
                  {device.lastSeen && (
                    <p className="text-xs text-gray-600 mt-2">
                      Last seen: {new Date(device.lastSeen).toLocaleString()}
                    </p>
                  )}
                </div>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      {/* Legend */}
      <div className="absolute bottom-4 right-4 bg-white rounded-lg shadow-lg p-3 z-[1000]">
        <h4 className="text-xs font-bold mb-2">Devices ({devices.length})</h4>
        <div className="space-y-1 text-xs">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
            <span>Active Devices</span>
          </div>
        </div>
      </div>
    </div>
  );
};
