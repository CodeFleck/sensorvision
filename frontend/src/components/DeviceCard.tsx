import { useState, useMemo } from 'react';
import { Device, TelemetryPoint } from '../types';
import { Cpu, MapPin, Clock, Activity } from 'lucide-react';
import { formatTimeAgo } from '../utils/timeUtils';
import { Card, CardBody } from './ui/Card';
import { Badge } from './ui/Badge';
import { Sparkline, calculateTrend, TrendDirection } from './Sparkline';
import { TrendIndicator } from './TrendIndicator';
import { clsx } from 'clsx';

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

// Health status configuration
const HEALTH_STATUS_CONFIG = {
  EXCELLENT: { color: 'bg-emerald-400', textColor: 'text-emerald-400', borderColor: 'border-emerald-400/30', bgColor: 'bg-emerald-400/10' },
  GOOD: { color: 'bg-cyan-400', textColor: 'text-cyan-400', borderColor: 'border-cyan-400/30', bgColor: 'bg-cyan-400/10' },
  FAIR: { color: 'bg-amber-400', textColor: 'text-amber-400', borderColor: 'border-amber-400/30', bgColor: 'bg-amber-400/10' },
  POOR: { color: 'bg-orange-400', textColor: 'text-orange-400', borderColor: 'border-orange-400/30', bgColor: 'bg-orange-400/10' },
  CRITICAL: { color: 'bg-rose-400', textColor: 'text-rose-400', borderColor: 'border-rose-400/30', bgColor: 'bg-rose-400/10' },
};

interface DeviceCardProps {
  device: Device;
  latestTelemetry?: TelemetryPoint | null;
  /** Historical telemetry for sparkline (optional) */
  telemetryHistory?: number[];
  /** Previous telemetry value for trend calculation */
  previousValue?: number;
}

const statusVariantMap = {
  ONLINE: 'success' as const,
  OFFLINE: 'danger' as const,
  UNKNOWN: 'default' as const,
};

export const DeviceCard = ({ device, latestTelemetry, telemetryHistory = [], previousValue }: DeviceCardProps) => {
  const [isHovered, setIsHovered] = useState(false);

  // Get health status config
  const healthConfig = device.healthStatus
    ? HEALTH_STATUS_CONFIG[device.healthStatus]
    : HEALTH_STATUS_CONFIG.FAIR;

  // Get primary metric (prefer kwConsumption/power)
  const primaryMetric = useMemo(() => {
    if (!latestTelemetry) return null;

    const priorityVars = ['kwConsumption', 'kw_consumption', 'power', 'temperature', 'voltage'];
    for (const varName of priorityVars) {
      const value = latestTelemetry[varName];
      if (typeof value === 'number') {
        return { name: varName, value, info: getVariableInfo(varName) };
      }
    }

    // Fall back to first numeric value
    const entries = Object.entries(latestTelemetry).filter(
      ([key, val]) => !['deviceId', 'timestamp', 'latitude', 'longitude', 'altitude'].includes(key) && typeof val === 'number'
    );
    if (entries.length > 0) {
      const [name, value] = entries[0];
      return { name, value: value as number, info: getVariableInfo(name) };
    }

    return null;
  }, [latestTelemetry]);

  // Calculate trend
  const trend = useMemo(() => {
    if (telemetryHistory.length >= 2) {
      return calculateTrend(telemetryHistory);
    }
    if (primaryMetric && previousValue !== undefined && previousValue !== 0) {
      const percentChange = ((primaryMetric.value - previousValue) / Math.abs(previousValue)) * 100;
      let direction: TrendDirection = 'stable';
      if (percentChange > 1) direction = 'up';
      else if (percentChange < -1) direction = 'down';
      return { direction, percentChange };
    }
    return { direction: 'stable' as TrendDirection, percentChange: 0 };
  }, [telemetryHistory, primaryMetric, previousValue]);

  return (
    <Card
      variant="elevated"
      className={clsx(
        'device-card-hover transition-all duration-300',
        isHovered && 'ring-1 ring-link/30 shadow-lg'
      )}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <CardBody>
        {/* Header with device info and health badge */}
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-lg bg-gradient-to-br from-link/15 to-link/5">
              <Cpu className="h-6 w-6 text-link" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-primary">{device.name}</h3>
              <p className="text-xs text-tertiary font-mono">{device.externalId}</p>
            </div>
          </div>

          {/* Health Badge */}
          <div className="flex flex-col items-end gap-1.5">
            {device.healthScore !== undefined && (
              <div
                className={clsx(
                  'flex items-center gap-1.5 px-2 py-1 rounded-full border',
                  healthConfig.bgColor,
                  healthConfig.borderColor
                )}
              >
                <div className={clsx('w-2 h-2 rounded-full', healthConfig.color, device.healthStatus === 'CRITICAL' && 'animate-pulse')} />
                <span className={clsx('text-xs font-semibold font-mono', healthConfig.textColor)}>
                  {device.healthScore}%
                </span>
              </div>
            )}
            <Badge variant={statusVariantMap[device.status]} className="text-xs">
              {device.status}
            </Badge>
          </div>
        </div>

        {/* Primary Metric with Sparkline */}
        {primaryMetric && (
          <div className="bg-hover rounded-lg p-3 mb-3">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-3xl font-bold font-mono text-primary">
                  {primaryMetric.value.toFixed(1)}
                </span>
                <span className="text-sm text-secondary ml-1">{primaryMetric.info.unit}</span>
              </div>

              {/* Sparkline */}
              {telemetryHistory.length >= 2 && (
                <Sparkline
                  data={telemetryHistory}
                  width={90}
                  height={35}
                  strokeWidth={2}
                  animate={true}
                />
              )}
            </div>

            {/* Trend Indicator */}
            <div className="mt-2">
              <TrendIndicator
                direction={trend.direction}
                percentChange={trend.percentChange}
                periodLabel="vs 1h ago"
                size="sm"
                showBackground={false}
              />
            </div>
          </div>
        )}

        {/* Additional readings */}
        {latestTelemetry && (
          <div className="space-y-2">
            {device.location && (
              <div className="flex items-center text-sm text-secondary">
                <MapPin className="h-3.5 w-3.5 mr-2 flex-shrink-0" />
                <span className="truncate">{device.location}</span>
              </div>
            )}

            <div className="border-t border-muted pt-2 mt-2">
              <div className="flex items-center text-xs text-secondary mb-2">
                <Activity className="h-3 w-3 mr-1" />
                Latest Readings
              </div>
              <div className="grid grid-cols-2 gap-2 text-sm">
                {Object.entries(latestTelemetry)
                  .filter(([key, value]) =>
                    !['deviceId', 'timestamp', 'latitude', 'longitude', 'altitude'].includes(key) &&
                    typeof value === 'number' &&
                    key !== primaryMetric?.name
                  )
                  .slice(0, 4)
                  .map(([key, value]) => {
                    const info = getVariableInfo(key);
                    const numValue = typeof value === 'number' ? value : 0;
                    return (
                      <div key={key} className="bg-hover/50 rounded px-2 py-1">
                        <div className="text-tertiary text-xs">{info.label}</div>
                        <div className="font-medium text-primary font-mono text-sm">
                          {numValue.toFixed(1)}{info.unit && ` ${info.unit}`}
                        </div>
                      </div>
                    );
                  })}
              </div>
            </div>
          </div>
        )}

        {/* Last seen timestamp */}
        {device.lastSeenAt && (
          <div className="flex items-center justify-center text-xs text-tertiary mt-3 pt-3 border-t border-muted">
            <Clock className="h-3 w-3 mr-1" />
            <span title={new Date(device.lastSeenAt).toLocaleString()}>
              Last seen {formatTimeAgo(device.lastSeenAt)}
            </span>
          </div>
        )}
      </CardBody>
    </Card>
  );
};
