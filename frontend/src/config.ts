// Environment-aware configuration
// Export functions for testability
export const getApiBaseUrl = (): string => {
  // In production (served from same origin), use relative path
  if (import.meta.env.PROD) {
    return '';
  }

  // In development, use localhost
  return 'http://localhost:8080';
};

export const getBackendUrl = (): string => {
  // Get the full backend URL (for display purposes in IntegrationWizard)
  if (import.meta.env.PROD) {
    // In production, use the same origin as the frontend (nginx reverse proxy handles routing)
    // This ensures generated code snippets work when deployed behind reverse proxy
    return window.location.origin;
  }

  // In development, backend runs on separate port
  return 'http://localhost:8080';
};

export const getWebSocketUrl = (): string => {
  // In production, construct WebSocket URL from current location
  if (import.meta.env.PROD) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    // Use same host as frontend (nginx reverse proxy handles WebSocket routing)
    return `${protocol}//${host}/ws/telemetry`;
  }

  // In development, backend WebSocket runs on separate port
  return 'ws://localhost:8080/ws/telemetry';
};

export const getLogsWebSocketUrl = (): string => {
  // In production, construct WebSocket URL from current location
  if (import.meta.env.PROD) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    return `${protocol}//${host}/ws/logs`;
  }

  // In development, backend WebSocket runs on separate port
  return 'ws://localhost:8080/ws/logs';
};

export const getMqttBrokerHost = (): string => {
  // In production, extract host from current location (strip port)
  if (import.meta.env.PROD) {
    // Extract just the hostname/IP without port
    return window.location.hostname;
  }

  // In development, use localhost
  return 'localhost';
};

export const getMqttBrokerPort = (): number => {
  return 1883; // Standard MQTT port
};

// Feature flags for quick rollback capability
// These can be controlled via environment variables or localStorage for testing
export const featureFlags = {
  /**
   * Enable the new engaging dashboard with Fleet Health, Activity Timeline,
   * sparklines, and trend indicators.
   * Set to false to revert to the classic dashboard layout.
   * Can be overridden via localStorage: localStorage.setItem('FF_ENGAGING_DASHBOARD', 'false')
   */
  get engagingDashboard(): boolean {
    // Check localStorage override first (for quick rollback in production)
    const override = localStorage.getItem('FF_ENGAGING_DASHBOARD');
    if (override !== null) {
      return override === 'true';
    }
    // Default: enabled
    return true;
  },
};

// Config object for convenience - calls functions at access time
export const config = {
  get apiBaseUrl() {
    return getApiBaseUrl();
  },
  get backendUrl() {
    return getBackendUrl();
  },
  get webSocketUrl() {
    return getWebSocketUrl();
  },
  get logsWebSocketUrl() {
    return getLogsWebSocketUrl();
  },
  get mqttBrokerHost() {
    return getMqttBrokerHost();
  },
  get mqttBrokerPort() {
    return getMqttBrokerPort();
  },
  get featureFlags() {
    return featureFlags;
  },
};
