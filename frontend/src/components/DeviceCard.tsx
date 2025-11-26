import { Device, TelemetryPoint } from '../types';
import { Cpu, MapPin, Zap, Clock } from 'lucide-react';
import { clsx } from 'clsx';
import { formatTimeAgo } from '../utils/timeUtils';

interface DeviceCardProps {
  device: Device;
  latestTelemetry?: TelemetryPoint | null;
}

const statusColors = {
  ONLINE: 'bg-green-100 text-green-800',
  OFFLINE: 'bg-red-100 text-red-800',
  UNKNOWN: 'bg-gray-100 text-gray-800',
};

export const DeviceCard = ({ device, latestTelemetry }: DeviceCardProps) => {
  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-3">
          <Cpu className="h-8 w-8 text-blue-600" />
          <div>
            <h3 className="text-lg font-semibold text-gray-900">{device.name}</h3>
            <p className="text-sm text-gray-500">{device.externalId}</p>
          </div>
        </div>
        <div className="flex flex-col items-end gap-1">
          <span
            className={clsx(
              'px-2 py-1 text-xs font-medium rounded-full',
              statusColors[device.status]
            )}
          >
            {device.status}
          </span>
          {device.lastSeenAt && (
            <div className="flex items-center text-xs text-gray-500">
              <Clock className="h-3 w-3 mr-1" />
              <span title={new Date(device.lastSeenAt).toLocaleString()}>
                {formatTimeAgo(device.lastSeenAt)}
              </span>
            </div>
          )}
        </div>
      </div>

      <div className="space-y-3">
        {device.location && (
          <div className="flex items-center text-sm text-gray-600">
            <MapPin className="h-4 w-4 mr-2" />
            {device.location}
          </div>
        )}

        {device.sensorType && (
          <div className="text-sm text-gray-600">
            Type: {device.sensorType}
          </div>
        )}

        {latestTelemetry && (
          <div className="border-t pt-3 mt-3">
            <h4 className="text-sm font-medium text-gray-700 mb-2">Latest Reading</h4>
            <div className="grid grid-cols-2 gap-3 text-sm">
              {latestTelemetry.kwConsumption && (
                <div>
                  <div className="flex items-center text-gray-600">
                    <Zap className="h-3 w-3 mr-1" />
                    Power
                  </div>
                  <div className="font-medium">{latestTelemetry.kwConsumption} kW</div>
                </div>
              )}
              {latestTelemetry.voltage && (
                <div>
                  <div className="text-gray-600">Voltage</div>
                  <div className="font-medium">{latestTelemetry.voltage} V</div>
                </div>
              )}
            </div>

            {latestTelemetry.timestamp && (
              <div className="flex items-center text-xs text-gray-500 mt-2">
                <Clock className="h-3 w-3 mr-1" />
                {new Date(latestTelemetry.timestamp).toLocaleString()}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};