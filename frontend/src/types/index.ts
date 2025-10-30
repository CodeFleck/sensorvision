export interface Device {
  externalId: string;
  name: string;
  location?: string;
  sensorType?: string;
  firmwareVersion?: string;
  status: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
  lastSeenAt?: string;
  latitude?: number;
  longitude?: number;
  altitude?: number;
  healthScore?: number;
  healthStatus?: 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR' | 'CRITICAL';
  lastHealthCheckAt?: string;
}

export interface DeviceTokenResponse {
  deviceId?: string;
  token?: string;          // Full token (only shown once on generation/rotation)
  maskedToken?: string;    // Masked token (e.g., "550e8400...0000")
  expiresAt?: string | null; // Token expiration date (if applicable)
  message: string;
  success: boolean;
  tokenCreatedAt?: string;
  tokenLastUsedAt?: string;
}

export interface TelemetryPoint {
  deviceId: string;
  timestamp: string;
  kwConsumption?: number;
  voltage?: number;
  current?: number;
  powerFactor?: number;
  frequency?: number;
  latitude?: number;
  longitude?: number;
  altitude?: number;
  // Allow dynamic property access for variable names
  [key: string]: string | number | undefined;
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

export type WidgetType =
  | 'LINE_CHART'
  | 'BAR_CHART'
  | 'GAUGE'
  | 'INDICATOR'
  | 'TABLE'
  | 'MAP'
  | 'CONTROL_BUTTON'
  | 'METRIC_CARD'
  | 'PIE_CHART'
  | 'AREA_CHART'
  | 'SCATTER_CHART';

export type WidgetAggregation = 'NONE' | 'MIN' | 'MAX' | 'AVG' | 'SUM' | 'COUNT' | 'LAST';

export interface WidgetConfig {
  [key: string]: unknown;
  // Common config options
  min?: number;
  max?: number;
  unit?: string;
  thresholds?: Array<{ value: number; color: string }>;
  colors?: string[];
  refreshInterval?: number;
  // Chart-specific
  showLegend?: boolean;
  showGrid?: boolean;
  // Gauge-specific
  segments?: Array<{ min: number; max: number; color: string }>;
}

export interface Widget {
  id: number;
  dashboardId: number;
  name: string;
  type: WidgetType;
  positionX: number;
  positionY: number;
  width: number;
  height: number;
  deviceId?: string;
  variableName?: string;
  useContextDevice?: boolean;
  deviceLabel?: string;
  aggregation: WidgetAggregation;
  timeRangeMinutes?: number;
  config: WidgetConfig;
  createdAt: string;
  updatedAt: string;
}

export interface Dashboard {
  id: number;
  name: string;
  description?: string;
  isDefault: boolean;
  defaultDeviceId?: string;
  layoutConfig: {
    cols: number;
    rowHeight: number;
  };
  widgets: Widget[];
  createdAt: string;
  updatedAt: string;
}

export interface WidgetCreateRequest {
  name: string;
  type: WidgetType;
  positionX?: number;
  positionY?: number;
  width?: number;
  height?: number;
  deviceId?: string;
  variableName?: string;
  useContextDevice?: boolean;
  deviceLabel?: string;
  aggregation?: WidgetAggregation;
  timeRangeMinutes?: number;
  config?: WidgetConfig;
}

export interface DashboardCreateRequest {
  name: string;
  description?: string;
  isDefault?: boolean;
  layoutConfig?: {
    cols: number;
    rowHeight: number;
  };
}

// Authentication types
export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  organizationId: number;
  organizationName: string;
  roles: string[];
  enabled: boolean;
  avatarUrl?: string;
  avatarVersion?: number;
  themePreference?: 'light' | 'dark' | 'system';
  createdAt?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  organizationName?: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  userId: number;
  username: string;
  email: string;
  organizationId: number;
}

// Event types
export type EventType =
  | 'DEVICE_CREATED'
  | 'DEVICE_UPDATED'
  | 'DEVICE_DELETED'
  | 'DEVICE_CONNECTED'
  | 'DEVICE_DISCONNECTED'
  | 'DEVICE_OFFLINE'
  | 'TELEMETRY_RECEIVED'
  | 'TELEMETRY_ANOMALY'
  | 'RULE_CREATED'
  | 'RULE_UPDATED'
  | 'RULE_DELETED'
  | 'RULE_TRIGGERED'
  | 'ALERT_CREATED'
  | 'ALERT_ACKNOWLEDGED'
  | 'ALERT_RESOLVED'
  | 'DASHBOARD_CREATED'
  | 'DASHBOARD_UPDATED'
  | 'DASHBOARD_DELETED'
  | 'WIDGET_CREATED'
  | 'WIDGET_UPDATED'
  | 'WIDGET_DELETED'
  | 'USER_LOGIN'
  | 'USER_LOGOUT'
  | 'USER_REGISTERED'
  | 'USER_UPDATED'
  | 'USER_DELETED'
  | 'SYSTEM_ERROR'
  | 'SYSTEM_WARNING'
  | 'SYSTEM_INFO'
  | 'SYNTHETIC_VARIABLE_CREATED'
  | 'SYNTHETIC_VARIABLE_UPDATED'
  | 'SYNTHETIC_VARIABLE_DELETED'
  | 'SYNTHETIC_VARIABLE_CALCULATED';

export type EventSeverity = 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';

export interface Event {
  id: number;
  eventType: EventType;
  severity: EventSeverity;
  entityType?: string;
  entityId?: string;
  title: string;
  description?: string;
  metadata?: Record<string, unknown>;
  userId?: number;
  deviceId?: string;
  createdAt: string;
}

// Notification types
export type NotificationChannel = 'EMAIL' | 'SMS' | 'WEBHOOK' | 'IN_APP';

export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface NotificationPreference {
  id: number;
  channel: NotificationChannel;
  enabled: boolean;
  destination?: string;
  minSeverity: AlertSeverity;
  immediate: boolean;
  digestIntervalMinutes?: number;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationPreferenceRequest {
  channel: NotificationChannel;
  enabled?: boolean;
  destination?: string;
  minSeverity?: AlertSeverity;
  immediate?: boolean;
  digestIntervalMinutes?: number;
}

export interface NotificationLog {
  id: number;
  alertId?: string;
  channel: NotificationChannel;
  destination: string;
  subject?: string;
  message: string;
  status: 'PENDING' | 'SENT' | 'FAILED' | 'RETRYING';
  errorMessage?: string;
  sentAt?: string;
  createdAt: string;
}

export interface NotificationStats {
  total: number;
  sent: number;
  failed: number;
}

// Issue Submission types
export type IssueCategory = 'BUG' | 'FEATURE_REQUEST' | 'QUESTION' | 'OTHER';

export type IssueSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type IssueStatus = 'SUBMITTED' | 'IN_REVIEW' | 'RESOLVED' | 'CLOSED';

export interface IssueSubmissionRequest {
  title: string;
  description: string;
  category: IssueCategory;
  severity: IssueSeverity;
  screenshotBase64?: string;
  screenshotFilename?: string;
  browserInfo?: string;
  pageUrl?: string;
  userAgent?: string;
  screenResolution?: string;
}

export interface IssueSubmission {
  id: number;
  title: string;
  description: string;
  category: IssueCategory;
  severity: IssueSeverity;
  status: IssueStatus;
  screenshotFilename?: string;
  hasScreenshot: boolean;
  browserInfo?: string;
  pageUrl?: string;
  userAgent?: string;
  screenResolution?: string;
  username: string;
  organizationName: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminIssue {
  id: number;
  title: string;
  description: string;
  category: IssueCategory;
  severity: IssueSeverity;
  status: IssueStatus;
  hasScreenshot: boolean;
  username: string;
  userEmail: string;
  userId: number;
  organizationName: string;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface IssueComment {
  id: number;
  issueId: number;
  authorId: number;
  authorName: string;
  message: string;
  internal: boolean;
  createdAt: string;
}

export interface IssueCommentRequest {
  message: string;
  internal?: boolean;
}