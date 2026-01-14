import { useMemo } from 'react';
import { Device } from '../types';
import { Card, CardBody } from './ui/Card';
import { Activity, Cpu, Wifi, Zap } from 'lucide-react';
import { clsx } from 'clsx';

// Health status configuration
const HEALTH_CONFIG = {
  EXCELLENT: { label: 'Excellent', color: '#10b981', minScore: 80 },
  GOOD: { label: 'Good', color: '#00d4ff', minScore: 60 },
  FAIR: { label: 'Fair', color: '#f59e0b', minScore: 40 },
  POOR: { label: 'Poor', color: '#f97316', minScore: 20 },
  CRITICAL: { label: 'Critical', color: '#f43f5e', minScore: 0 },
};

type HealthStatus = keyof typeof HEALTH_CONFIG;

interface FleetHealthGaugeProps {
  devices: Device[];
  className?: string;
}

export const FleetHealthGauge = ({ devices, className }: FleetHealthGaugeProps) => {
  // Calculate fleet-wide health metrics
  const fleetMetrics = useMemo(() => {
    if (devices.length === 0) {
      return {
        averageHealth: 0,
        healthStatus: 'CRITICAL' as HealthStatus,
        totalDevices: 0,
        onlineDevices: 0,
        totalPower: 0,
      };
    }

    const devicesWithHealth = devices.filter(d => d.healthScore !== undefined);
    const averageHealth = devicesWithHealth.length > 0
      ? devicesWithHealth.reduce((sum, d) => sum + (d.healthScore || 0), 0) / devicesWithHealth.length
      : 0;

    // Determine health status based on average score
    let healthStatus: HealthStatus = 'CRITICAL';
    for (const [status, config] of Object.entries(HEALTH_CONFIG)) {
      if (averageHealth >= config.minScore) {
        healthStatus = status as HealthStatus;
        break;
      }
    }

    const onlineDevices = devices.filter(d => d.status === 'ONLINE').length;

    return {
      averageHealth: Math.round(averageHealth),
      healthStatus,
      totalDevices: devices.length,
      onlineDevices,
      totalPower: 0, // This would come from telemetry data
    };
  }, [devices]);

  // SVG gauge calculations
  const size = 180;
  const strokeWidth = 14;
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const healthPercentage = fleetMetrics.averageHealth / 100;
  const strokeDashoffset = circumference * (1 - healthPercentage);

  const healthConfig = HEALTH_CONFIG[fleetMetrics.healthStatus];

  const healthStatusClass = `health-status-${fleetMetrics.healthStatus.toLowerCase()}`;

  return (
    <Card className={clsx('fleet-health-card', className)}>
      <CardBody>
        {/* Header */}
        <div className="flex items-center gap-2 mb-6">
          <div className="p-2 rounded-lg bg-gradient-to-br from-emerald-500/20 to-cyan-500/20 shadow-lg shadow-emerald-500/10">
            <Activity className="h-5 w-5 text-emerald-400" />
          </div>
          <h2 className="text-lg font-semibold text-primary">Fleet Health</h2>
        </div>

        {/* Circular Gauge */}
        <div className="flex justify-center mb-6">
          <div className="relative fleet-gauge-glow">
            <svg
              width={size}
              height={size}
              className="transform -rotate-90"
              role="img"
              aria-label={`Fleet health: ${fleetMetrics.averageHealth}%`}
            >
              <defs>
                <linearGradient id="fleetHealthGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stopColor="#10b981" />
                  <stop offset="100%" stopColor="#00d4ff" />
                </linearGradient>
                <filter id="glow">
                  <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
                  <feMerge>
                    <feMergeNode in="coloredBlur"/>
                    <feMergeNode in="SourceGraphic"/>
                  </feMerge>
                </filter>
              </defs>
              {/* Background circle */}
              <circle
                cx={size / 2}
                cy={size / 2}
                r={radius}
                fill="none"
                stroke="currentColor"
                strokeWidth={strokeWidth}
                className="text-hover opacity-30"
              />
              {/* Value circle */}
              <circle
                cx={size / 2}
                cy={size / 2}
                r={radius}
                fill="none"
                stroke={fleetMetrics.averageHealth > 60 ? 'url(#fleetHealthGradient)' : healthConfig.color}
                strokeWidth={strokeWidth}
                strokeLinecap="round"
                strokeDasharray={circumference}
                strokeDashoffset={strokeDashoffset}
                className="transition-all duration-1000 ease-out fleet-gauge-animate"
                filter="url(#glow)"
              />
            </svg>
            {/* Center content */}
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span
                className="text-5xl font-bold font-mono fleet-health-value"
                style={{
                  background: fleetMetrics.averageHealth > 60
                    ? 'linear-gradient(135deg, #10b981, #00d4ff)'
                    : healthConfig.color,
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  backgroundClip: 'text',
                }}
              >
                {fleetMetrics.averageHealth}%
              </span>
              <span className={clsx(
                'text-sm font-bold uppercase tracking-widest mt-1',
                healthStatusClass
              )}>
                {healthConfig.label}
              </span>
            </div>
          </div>
        </div>

        {/* Mini Stats */}
        <div className="grid grid-cols-3 gap-3">
          <MiniStat
            icon={<Cpu className="h-4 w-4" />}
            value={fleetMetrics.totalDevices}
            label="Devices"
            iconColor="text-cyan-400"
          />
          <MiniStat
            icon={<Wifi className="h-4 w-4" />}
            value={fleetMetrics.onlineDevices}
            label="Online"
            valueClassName="text-emerald-400"
            iconColor="text-emerald-400"
          />
          <MiniStat
            icon={<Zap className="h-4 w-4" />}
            value={fleetMetrics.totalDevices > 0 ? `${Math.round((fleetMetrics.onlineDevices / fleetMetrics.totalDevices) * 100)}%` : '0%'}
            label="Uptime"
            valueClassName="text-amber-400"
            iconColor="text-amber-400"
          />
        </div>
      </CardBody>
    </Card>
  );
};

// Mini stat component
interface MiniStatProps {
  icon: React.ReactNode;
  value: number | string;
  label: string;
  valueClassName?: string;
  iconColor?: string;
}

const MiniStat = ({ icon, value, label, valueClassName, iconColor }: MiniStatProps) => (
  <div className="mini-stat-card">
    <div className={clsx('flex justify-center mb-2', iconColor || 'text-secondary')}>{icon}</div>
    <div className={clsx('text-2xl font-bold font-mono', valueClassName || 'text-primary')}>
      {value}
    </div>
    <div className="text-xs text-secondary uppercase tracking-wider mt-1">{label}</div>
  </div>
);
