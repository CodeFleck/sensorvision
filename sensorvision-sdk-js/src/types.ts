/**
 * Type definitions for SensorVision SDK
 */

/**
 * Configuration options for SensorVision client
 */
export interface ClientConfig {
  /** Base URL of the SensorVision API */
  apiUrl: string;
  /** API key for authentication */
  apiKey: string;
  /** Request timeout in milliseconds */
  timeout?: number;
  /** Number of retry attempts for failed requests */
  retryAttempts?: number;
  /** Initial delay between retries in milliseconds */
  retryDelay?: number;
}

/**
 * Telemetry data to be sent to SensorVision
 */
export interface TelemetryData {
  [key: string]: number | boolean;
}

/**
 * Response from telemetry ingestion
 */
export interface IngestionResponse {
  success: boolean;
  message: string;
  deviceId?: string;
  timestamp?: string;
}

/**
 * WebSocket configuration options
 */
export interface WebSocketConfig {
  /** WebSocket URL of the SensorVision server */
  wsUrl: string;
  /** API key for authentication */
  apiKey: string;
  /** Reconnection settings */
  reconnect?: boolean;
  /** Reconnection delay in milliseconds */
  reconnectDelay?: number;
  /** Maximum reconnection attempts */
  maxReconnectAttempts?: number;
}

/**
 * Telemetry data point received from WebSocket
 */
export interface TelemetryPoint {
  deviceId: string;
  timestamp: string;
  variables: TelemetryData;
}

/**
 * WebSocket message types
 */
export type WebSocketMessage =
  | { type: 'telemetry'; data: TelemetryPoint }
  | { type: 'error'; error: string }
  | { type: 'connected'; message: string }
  | { type: 'disconnected'; message: string };

/**
 * Subscription callback for WebSocket data
 */
export type SubscriptionCallback = (data: TelemetryPoint) => void;

/**
 * Error callback for WebSocket errors
 */
export type ErrorCallback = (error: Error) => void;
