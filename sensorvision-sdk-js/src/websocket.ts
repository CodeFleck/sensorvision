/**
 * WebSocket client for real-time telemetry subscriptions
 */

import {
  WebSocketConfig,
  TelemetryPoint,
  SubscriptionCallback,
  ErrorCallback,
} from './types';
import { WebSocketError } from './errors';

// Environment-aware WebSocket import
// In Node.js, use the 'ws' package; in browsers, use native WebSocket
let WebSocketImpl: typeof WebSocket;
if (typeof window === 'undefined') {
  // Node.js environment
  try {
    // Dynamic import for Node.js 'ws' package
    WebSocketImpl = require('ws');
  } catch (e) {
    throw new Error('WebSocket support requires the "ws" package in Node.js. Install it with: npm install ws');
  }
} else {
  // Browser environment - use native WebSocket
  if (typeof WebSocket === 'undefined') {
    throw new Error('WebSocket is not supported in this browser');
  }
  WebSocketImpl = WebSocket;
}

/**
 * SensorVision WebSocket Client
 *
 * Provides real-time subscriptions to telemetry data via WebSocket.
 *
 * @example
 * ```typescript
 * const wsClient = new WebSocketClient({
 *   wsUrl: 'ws://localhost:8080/ws/telemetry',
 *   apiKey: 'your-api-key'
 * });
 *
 * wsClient.subscribe('sensor-001', (data) => {
 *   console.log('Received:', data);
 * });
 *
 * wsClient.connect();
 * ```
 */
export class WebSocketClient {
  private config: Required<WebSocketConfig>;
  private ws: WebSocket | null = null;
  private subscriptions: Map<string, Set<SubscriptionCallback>> = new Map();
  private errorCallbacks: Set<ErrorCallback> = new Set();
  private reconnectAttempts = 0;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private isConnecting = false;
  private isManualClose = false;

  /**
   * Create a new WebSocket client
   *
   * @param config - WebSocket configuration options
   */
  constructor(config: WebSocketConfig) {
    this.config = {
      wsUrl: config.wsUrl,
      apiKey: config.apiKey,
      reconnect: config.reconnect ?? true,
      reconnectDelay: config.reconnectDelay ?? 5000,
      maxReconnectAttempts: config.maxReconnectAttempts ?? 10,
    };
  }

  /**
   * Connect to the WebSocket server
   */
  async connect(): Promise<void> {
    if (this.isConnecting || (this.ws && this.ws.readyState === WebSocket.OPEN)) {
      return;
    }

    this.isConnecting = true;
    this.isManualClose = false;

    return new Promise((resolve, reject) => {
      try {
        // Use environment-appropriate WebSocket implementation
        this.ws = new WebSocketImpl(this.config.wsUrl, {
          headers: {
            'X-API-Key': this.config.apiKey,
          },
        }) as WebSocket;

        this.ws.on('open', () => {
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          console.log('WebSocket connected');
          resolve();
        });

        this.ws.on('message', (data: WebSocket.Data) => {
          this.handleMessage(data);
        });

        this.ws.on('error', (error: Error) => {
          console.error('WebSocket error:', error);
          this.notifyError(new WebSocketError(error.message));
        });

        this.ws.on('close', () => {
          console.log('WebSocket disconnected');
          this.ws = null;
          this.isConnecting = false;

          if (!this.isManualClose && this.config.reconnect) {
            this.scheduleReconnect();
          }
        });
      } catch (error) {
        this.isConnecting = false;
        const wsError = new WebSocketError(
          `Failed to connect: ${error instanceof Error ? error.message : String(error)}`
        );
        this.notifyError(wsError);
        reject(wsError);
      }
    });
  }

  /**
   * Disconnect from the WebSocket server
   */
  disconnect(): void {
    this.isManualClose = true;

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  /**
   * Subscribe to telemetry data for a specific device
   *
   * @param deviceId - Device ID to subscribe to
   * @param callback - Callback function to receive telemetry data
   *
   * @example
   * ```typescript
   * wsClient.subscribe('sensor-001', (data) => {
   *   console.log(`Temperature: ${data.variables.temperature}`);
   * });
   * ```
   */
  subscribe(deviceId: string, callback: SubscriptionCallback): void {
    if (!this.subscriptions.has(deviceId)) {
      this.subscriptions.set(deviceId, new Set());
    }
    this.subscriptions.get(deviceId)!.add(callback);

    // Send subscription message if connected
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        type: 'subscribe',
        deviceId,
      }));
    }
  }

  /**
   * Unsubscribe from telemetry data for a specific device
   *
   * @param deviceId - Device ID to unsubscribe from
   * @param callback - Optional specific callback to remove
   */
  unsubscribe(deviceId: string, callback?: SubscriptionCallback): void {
    if (!this.subscriptions.has(deviceId)) {
      return;
    }

    if (callback) {
      this.subscriptions.get(deviceId)!.delete(callback);
      if (this.subscriptions.get(deviceId)!.size === 0) {
        this.subscriptions.delete(deviceId);
      }
    } else {
      this.subscriptions.delete(deviceId);
    }

    // Send unsubscribe message if connected
    if (this.ws && this.ws.readyState === WebSocket.OPEN && this.subscriptions.get(deviceId)?.size === 0) {
      this.ws.send(JSON.stringify({
        type: 'unsubscribe',
        deviceId,
      }));
    }
  }

  /**
   * Register an error callback
   *
   * @param callback - Error callback function
   */
  onError(callback: ErrorCallback): void {
    this.errorCallbacks.add(callback);
  }

  /**
   * Get connection status
   */
  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }

  /**
   * Handle incoming WebSocket messages
   */
  private handleMessage(data: WebSocket.Data): void {
    try {
      const message = JSON.parse(data.toString());

      if (message.type === 'telemetry' && message.data) {
        const telemetry: TelemetryPoint = message.data;
        const callbacks = this.subscriptions.get(telemetry.deviceId);

        if (callbacks) {
          callbacks.forEach(callback => {
            try {
              callback(telemetry);
            } catch (error) {
              console.error('Error in subscription callback:', error);
            }
          });
        }
      }
    } catch (error) {
      console.error('Failed to parse WebSocket message:', error);
      this.notifyError(new WebSocketError('Failed to parse message'));
    }
  }

  /**
   * Notify error callbacks
   */
  private notifyError(error: Error): void {
    this.errorCallbacks.forEach(callback => {
      try {
        callback(error);
      } catch (err) {
        console.error('Error in error callback:', err);
      }
    });
  }

  /**
   * Schedule reconnection attempt
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.config.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      this.notifyError(new WebSocketError('Max reconnection attempts reached'));
      return;
    }

    this.reconnectAttempts++;
    const delay = this.config.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);

    console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.config.maxReconnectAttempts})`);

    this.reconnectTimer = setTimeout(() => {
      this.connect().catch(error => {
        console.error('Reconnection failed:', error);
      });
    }, delay);
  }
}
