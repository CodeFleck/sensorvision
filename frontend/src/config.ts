// Environment-aware configuration
const getApiBaseUrl = (): string => {
  // In production (served from same origin), use relative path
  if (import.meta.env.PROD) {
    return '';
  }

  // In development, use localhost
  return 'http://localhost:8080';
};

const getBackendUrl = (): string => {
  // Get the full backend URL (for display purposes in IntegrationWizard)
  if (import.meta.env.PROD) {
    const protocol = window.location.protocol;
    const host = window.location.host;
    // If served from nginx on port 80, backend is on port 8080
    const backendHost = host.includes(':3000') || !host.includes(':')
      ? host.replace(':3000', ':8080').replace(/^([^:]+)$/, '$1:8080')
      : host;
    return `${protocol}//${backendHost}`;
  }

  // In development
  return 'http://localhost:8080';
};

const getWebSocketUrl = (): string => {
  // In production, construct WebSocket URL from current location
  if (import.meta.env.PROD) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    // If served from nginx on port 80, connect to backend on port 8080
    const wsHost = host.includes(':3000') || !host.includes(':')
      ? host.replace(':3000', ':8080').replace(/^([^:]+)$/, '$1:8080')
      : host;
    return `${protocol}//${wsHost}/ws/telemetry`;
  }

  // In development
  return 'ws://localhost:8080/ws/telemetry';
};

export const config = {
  apiBaseUrl: getApiBaseUrl(),
  backendUrl: getBackendUrl(),
  webSocketUrl: getWebSocketUrl(),
};
