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
    // In production, use the same origin as the frontend (nginx reverse proxy handles routing)
    // This ensures generated code snippets work when deployed behind reverse proxy
    return window.location.origin;
  }

  // In development, backend runs on separate port
  return 'http://localhost:8080';
};

const getWebSocketUrl = (): string => {
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

export const config = {
  apiBaseUrl: getApiBaseUrl(),
  backendUrl: getBackendUrl(),
  webSocketUrl: getWebSocketUrl(),
};
