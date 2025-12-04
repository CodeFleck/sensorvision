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
};
