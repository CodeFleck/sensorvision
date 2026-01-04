import { Device, TelemetryPoint } from '../types';
import { Cpu, MapPin, Clock, Activity } from 'lucide-react';
import { formatTimeAgo } from '../utils/timeUtils';
import { Card, CardBody } from './ui/Card';
import { Badge } from './ui/Badge';

// Common variable display names and units
const VARIABLE_CONFIG: Record<string, { label: string; unit: string }> = {
  kwConsumption: { label: 'Power', unit: 'kW' },
  kw_consumption: { label: 'Power', unit: 'kW' },
  voltage: { label: 'Voltage', unit: 'V' },
  current: { label: 'Current', unit: 'A' },
  powerFactor: { label: 'PF', unit: '' },
  power_factor: { label: 'PF', unit: '' },
  frequency: { label: 'Freq', unit: 'Hz' },
  temperature: { label: 'Temp', unit: 'C' },
  humidity: { label: 'Humidity', unit: '%' },
  pressure: { label: 'Pressure', unit: 'bar' },
  flow_rate: { label: 'Flow', unit: 'L/min' },
  vibration: { label: 'Vibration', unit: 'mm/s' },
};

// Get display info for a variable
const getVariableInfo = (varName: string): { label: string; unit: string } => {
  if (VARIABLE_CONFIG[varName]) {
    return VARIABLE_CONFIG[varName];
  }
  // Convert snake_case or camelCase to short label
  const label = varName
    .replace(/_/g, ' ')
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, str => str.toUpperCase())
    .trim()
    .split(' ')
    .slice(0, 2)
    .join(' ');
  return { label, unit: '' };
};

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
              <h4 className="text-sm font-medium text-primary mb-2 flex items-center">
                <Activity className="h-3 w-3 mr-1" />
                Latest Reading
              </h4>
              <div className="grid grid-cols-2 gap-2 text-sm">
                {Object.entries(latestTelemetry)
                  .filter(([key, value]) =>
                    !['deviceId', 'timestamp', 'latitude', 'longitude', 'altitude'].includes(key) &&
                    typeof value === 'number'
                  )
                  .slice(0, 6) // Show max 6 variables to keep card compact
                  .map(([key, value]) => {
                    const info = getVariableInfo(key);
                    const numValue = typeof value === 'number' ? value : 0;
                    return (
                      <div key={key}>
                        <div className="text-secondary text-xs">{info.label}</div>
                        <div className="font-medium text-primary">
                          {numValue.toFixed(1)}{info.unit && ` ${info.unit}`}
                        </div>
                      </div>
                    );
                  })}
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