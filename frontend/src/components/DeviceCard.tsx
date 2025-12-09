import { Device, TelemetryPoint } from '../types';
import { Cpu, MapPin, Zap, Clock } from 'lucide-react';
import { clsx } from 'clsx';
import { formatTimeAgo } from '../utils/timeUtils';
import { Card, CardBody } from './ui/Card';
import { Badge } from './ui/Badge';

interface DeviceCardProps {
  device: Device;
  latestTelemetry?: TelemetryPoint | null;
}

const statusVariantMap = {
  ONLINE: 'success' as const,
  OFFLINE: 'danger' as const,
  UNKNOWN: 'default' as const,
};

export const DeviceCard = ({ device, latestTelemetry }: DeviceCardProps) => {
  return (
    <Card variant="elevated">
      <CardBody>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <Cpu className="h-8 w-8 text-link" />
            <div>
              <h3 className="text-lg font-semibold text-primary">{device.name}</h3>
              <p className="text-sm text-secondary">{device.externalId}</p>
            </div>
          </div>
          <div className="flex flex-col items-end gap-1">
            <Badge variant={statusVariantMap[device.status]}>
              {device.status}
            </Badge>
            {device.lastSeenAt && (
              <div className="flex items-center text-xs text-secondary">
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
            <div className="flex items-center text-sm text-secondary">
              <MapPin className="h-4 w-4 mr-2" />
              {device.location}
            </div>
          )}

          {device.sensorType && (
            <div className="text-sm text-secondary">
              Type: {device.sensorType}
            </div>
          )}

          {latestTelemetry && (
            <div className="border-t border-muted pt-3 mt-3">
              <h4 className="text-sm font-medium text-primary mb-2">Latest Reading</h4>
              <div className="grid grid-cols-2 gap-3 text-sm">
                {latestTelemetry.kwConsumption && (
                  <div>
                    <div className="flex items-center text-secondary">
                      <Zap className="h-3 w-3 mr-1" />
                      Power
                    </div>
                    <div className="font-medium text-primary">{latestTelemetry.kwConsumption} kW</div>
                  </div>
                )}
                {latestTelemetry.voltage && (
                  <div>
                    <div className="text-secondary">Voltage</div>
                    <div className="font-medium text-primary">{latestTelemetry.voltage} V</div>
                  </div>
                )}
              </div>

              {latestTelemetry.timestamp && (
                <div className="flex items-center text-xs text-secondary mt-2">
                  <Clock className="h-3 w-3 mr-1" />
                  {new Date(latestTelemetry.timestamp).toLocaleString()}
                </div>
              )}
            </div>
          )}
        </div>
      </CardBody>
    </Card>
  );
};