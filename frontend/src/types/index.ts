export interface Device {
  externalId: string;
  name: string;
  location?: string;
  sensorType?: string;
  firmwareVersion?: string;
  status: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
  lastSeenAt?: string;
}

export interface TelemetryPoint {
  deviceId: string;
  timestamp: string;
  kwConsumption?: number;
  voltage?: number;
  current?: number;
  powerFactor?: number;
  frequency?: number;
}

export interface LatestTelemetry {
  deviceId: string;
  latest: TelemetryPoint | null;
}

export interface Rule {
  id: string;
  name: string;
  description?: string;
  deviceId: string;
  variable: string;
  operator: 'GT' | 'LT' | 'EQ' | 'GTE' | 'LTE';
  threshold: number;
  enabled: boolean;
  createdAt: string;
}

export interface Alert {
  id: string;
  ruleId: string;
  ruleName: string;
  deviceId: string;
  message: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  timestamp: string;
  acknowledged: boolean;
}