/**
 * WebSocket client for real-time telemetry subscriptions
 *
 * Supports both Node.js (using 'ws' package) and browser (native WebSocket) environments.
 */

import {
  WebSocketConfig,
  TelemetryPoint,
  SubscriptionCallback,
  ErrorCallback,
} from './types';
import { WebSocketError } from './errors';

// Type for WebSocket that works in both environments
type CrossPlatformWebSocket = WebSocket | any;

/**
 * Helper to detect if we're in a browser environment
 */
const isBrowser = typeof window !== 'undefined' && typeof window.WebSocket !== 'undefined';

/**
 * Helper to get the appropriate WebSocket constructor
 */
function getWebSocketConstructor(): any {
  if (isBrowser) {
    return WebSocket;
  } else {
    // Node.js environment - require ws dynamically
    try {
      return require('ws');
    } catch (e) {
      throw new Error('WebSocket support requires the "ws" package in Node.js. Install it with: npm install ws');
    }
  }
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
  private ws: CrossPlatformWebSocket | null = null;
  private subscriptions: Map<string, Set<SubscriptionCallback>> = new Map();
  private errorCallbacks: Set<ErrorCallback> = new Set();
  private reconnectAttempts = 0;
  private reconnectTimer: any = null;
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
   * Cross-platform event listener attachment
   * Handles differences between Node.js EventEmitter (.on) and browser WebSocket (addEventListener/properties)
   */
  private attachEventHandlers(ws: CrossPlatformWebSocket): void {
    const onOpen = () => {
      this.isConnecting = false;
      this.reconnectAttempts = 0;
      console.log('WebSocket connected');
    };

    const onMessage = (event: any) => {
      // In browsers, event.data contains the message
      // In Node.js ws, the event itself is the data
      const data = isBrowser ? event.data : event;
      this.handleMessage(data);
    };

    const onError = (error: any) => {
      console.error('WebSocket error:', error);
      const wsError = new WebSocketError(error.message || 'WebSocket error');
      this.notifyError(wsError);
    };

    const onClose = () => {
      console.log('WebSocket disconnected');
      this.ws = null;
      this.isConnecting = false;

      if (!this.isManualClose && this.config.reconnect) {
        this.scheduleReconnect();
      }
    };

    if (isBrowser) {
      // Browser WebSocket API - use addEventListener
      ws.addEventListener('open', onOpen);
      ws.addEventListener('message', onMessage);
      ws.addEventListener('error', onError);
      ws.addEventListener('close', onClose);
    } else {
      // Node.js ws library - use EventEmitter .on()
      ws.on('open', onOpen);
      ws.on('message', onMessage);
      ws.on('error', onError);
      ws.on('close', onClose);
    }
  }

  /**
   * Connect to the WebSocket server
   */
  async connect(): Promise<void> {
    if (this.isConnecting || (this.ws && this.ws.readyState === (isBrowser ? WebSocket.OPEN : 1))) {
      return;
    }

    this.isConnecting = true;
    this.isManualClose = false;

    return new Promise((resolve, reject) => {
      try {
        const WS = getWebSocketConstructor();

        // Create WebSocket with environment-specific options
        if (isBrowser) {
          // Browser: WebSocket doesn't support custom headers in constructor
          // Headers must be sent via query params or after connection
          this.ws = new WS(this.config.wsUrl);
        } else {
          // Node.js: ws library supports headers option
          this.ws = new WS(this.config.wsUrl, {
            headers: {
              'X-API-Key': this.config.apiKey,
            },
          });
        }

        // Set up event handlers before connection completes
        const onOpenOnce = () => {
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          console.log('WebSocket connected');
          resolve();
        };

        const onErrorOnce = (error: any) => {
          this.isConnecting = false;
          const wsError = new WebSocketError(
            `Failed to connect: ${error?.message || 'Unknown error'}`
          );
          this.notifyError(wsError);
          reject(wsError);
        };

        // Attach one-time handlers for connection promise
        if (isBrowser) {
          this.ws!.addEventListener('open', onOpenOnce, { once: true });
          this.ws!.addEventListener('error', onErrorOnce, { once: true });
        } else {
          this.ws!.once('open', onOpenOnce);
          this.ws!.once('error', onErrorOnce);
        }

        // Attach persistent event handlers
        this.attachEventHandlers(this.ws!);

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
   * Subscribe to telemetry updates for a specific device
   *
   * @param deviceId - The device ID to subscribe to
   * @param callback - Callback function to receive telemetry data
   */
  subscribe(deviceId: string, callback: SubscriptionCallback): void {
    if (!this.subscriptions.has(deviceId)) {
      this.subscriptions.set(deviceId, new Set());
    }
    this.subscriptions.get(deviceId)!.add(callback);

    // Send subscription message if connected
    if (this.ws && this.ws.readyState === (isBrowser ? WebSocket.OPEN : 1)) {
      this.ws.send(JSON.stringify({
        type: 'subscribe',
        deviceId,
      }));
    }
  }

  /**
   * Unsubscribe from telemetry updates for a specific device
   *
   * @param deviceId - The device ID to unsubscribe from
   * @param callback - Optional specific callback to remove. If not provided, removes all callbacks for the device.
   */
  unsubscribe(deviceId: string, callback?: SubscriptionCallback): void {
    if (!this.subscriptions.has(deviceId)) {
      return;
    }

    if (callback) {
      this.subscriptions.get(deviceId)!.delete(callback);
      // If no more callbacks for this device, remove the subscription entirely
      if (this.subscriptions.get(deviceId)!.size === 0) {
        this.subscriptions.delete(deviceId);
      }
    } else {
      this.subscriptions.delete(deviceId);
    }

    // Send unsubscribe message if no more subscriptions for this device
    if (this.ws && this.ws.readyState === (isBrowser ? WebSocket.OPEN : 1) && !this.subscriptions.has(deviceId)) {
      this.ws.send(JSON.stringify({
        type: 'unsubscribe',
        deviceId,
      }));
    }
  }

  /**
   * Register an error callback
   *
   * @param callback - Callback function to receive error notifications
   */
  onError(callback: ErrorCallback): void {
    this.errorCallbacks.add(callback);
  }

  /**
   * Handle incoming WebSocket messages
   */
  private handleMessage(data: any): void {
    try {
      // Parse message data
      const messageStr = typeof data === 'string' ? data : data.toString();
      const message = JSON.parse(messageStr);

      // Backend sends TelemetryPointDto with flat structure
      // Transform it to match SDK's TelemetryPoint interface
      if (message.deviceId && message.timestamp) {
        const deviceId = message.deviceId;
        const timestamp = message.timestamp;

        // Extract all properties except deviceId and timestamp into variables object
        const variables: { [key: string]: number | boolean } = {};
        for (const key in message) {
          if (key !== 'deviceId' && key !== 'timestamp' && message[key] !== null && message[key] !== undefined) {
            variables[key] = message[key];
          }
        }

        // Create TelemetryPoint in the format expected by SDK consumers
        const telemetryPoint: TelemetryPoint = {
          deviceId,
          timestamp,
          variables,
        };

        // Notify subscribers for this device
        const callbacks = this.subscriptions.get(deviceId);
        if (callbacks) {
          callbacks.forEach((callback) => {
            try {
              callback(telemetryPoint);
            } catch (error) {
              console.error('Error in subscription callback:', error);
            }
          });
        }
      }
    } catch (error) {
      console.error('Failed to parse WebSocket message:', error);
    }
  }

  /**
   * Notify error callbacks
   */
  private notifyError(error: WebSocketError): void {
    this.errorCallbacks.forEach((callback) => {
      try {
        callback(error);
      } catch (e) {
        console.error('Error in error callback:', e);
      }
    });
  }

  /**
   * Schedule a reconnection attempt
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.config.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      this.notifyError(
        new WebSocketError('Max reconnection attempts reached')
      );
      return;
    }

    this.reconnectAttempts++;
    const delay = this.config.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);

    console.log(
      `Scheduling reconnection attempt ${this.reconnectAttempts}/${this.config.maxReconnectAttempts} in ${delay}ms`
    );

    this.reconnectTimer = setTimeout(() => {
      this.connect().catch((error) => {
        console.error('Reconnection failed:', error);
      });
    }, delay);
  }

  /**
   * Check if WebSocket is connected
   */
  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === (isBrowser ? WebSocket.OPEN : 1);
  }
}
