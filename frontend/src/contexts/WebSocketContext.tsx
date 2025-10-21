import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { TelemetryPoint } from '../types';

interface WebSocketContextType {
  lastMessage: TelemetryPoint | null;
  connectionStatus: 'Connecting' | 'Open' | 'Closing' | 'Closed';
  isConnected: boolean;
  reconnectAttempts: number;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

interface WebSocketProviderProps {
  children: React.ReactNode;
  url: string;
  maxReconnectAttempts?: number;
  reconnectInterval?: number;
}

export const WebSocketProvider: React.FC<WebSocketProviderProps> = ({
  children,
  url,
  maxReconnectAttempts = Infinity,
  reconnectInterval = 3000, // Start with 3 seconds
}) => {
  const [lastMessage, setLastMessage] = useState<TelemetryPoint | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<'Connecting' | 'Open' | 'Closing' | 'Closed'>('Closed');
  const [reconnectAttempts, setReconnectAttempts] = useState(0);

  const ws = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);
  const shouldReconnect = useRef(true);
  const reconnectAttemptCount = useRef(0);

  const connect = useCallback(() => {
    // Don't create a new connection if one already exists
    if (ws.current?.readyState === WebSocket.OPEN || ws.current?.readyState === WebSocket.CONNECTING) {
      return;
    }

    setConnectionStatus('Connecting');

    // Append JWT token to WebSocket URL for authentication
    const token = localStorage.getItem('accessToken');
    const wsUrl = token ? `${url}?token=${encodeURIComponent(token)}` : url;

    console.log(`[WebSocket] Connecting to ${url}${token ? ' (authenticated)' : ' (unauthenticated)'}...`);

    const websocket = new WebSocket(wsUrl);
    ws.current = websocket;

    websocket.onopen = () => {
      setConnectionStatus('Open');
      reconnectAttemptCount.current = 0;
      setReconnectAttempts(0);
      console.log('[WebSocket] Connected successfully');
    };

    websocket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as TelemetryPoint;
        setLastMessage(data);
      } catch (error) {
        console.error('[WebSocket] Failed to parse message:', error);
      }
    };

    websocket.onclose = (event) => {
      setConnectionStatus('Closed');
      console.log(`[WebSocket] Connection closed (code: ${event.code}, reason: ${event.reason || 'none'})`);

      // Only attempt to reconnect if we should and haven't exceeded max attempts
      if (shouldReconnect.current && reconnectAttemptCount.current < maxReconnectAttempts) {
        reconnectAttemptCount.current++;
        setReconnectAttempts(reconnectAttemptCount.current);

        // Exponential backoff: 3s, 6s, 12s, 24s, 30s (max)
        const backoffTime = Math.min(
          reconnectInterval * Math.pow(2, reconnectAttemptCount.current - 1),
          30000
        );

        console.log(`[WebSocket] Reconnecting in ${backoffTime / 1000}s (attempt ${reconnectAttemptCount.current}/${maxReconnectAttempts})...`);

        reconnectTimeoutRef.current = setTimeout(() => {
          connect();
        }, backoffTime);
      } else if (reconnectAttemptCount.current >= maxReconnectAttempts) {
        console.error('[WebSocket] Max reconnection attempts reached');
      }
    };

    websocket.onerror = (error) => {
      console.error('[WebSocket] Error occurred:', error);
    };
  }, [url, maxReconnectAttempts, reconnectInterval]);

  useEffect(() => {
    // Enable reconnection and connect
    shouldReconnect.current = true;
    connect();

    // Cleanup function
    return () => {
      console.log('[WebSocket] Cleaning up connection...');
      shouldReconnect.current = false;

      // Clear any pending reconnect timeout
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }

      // Close the WebSocket connection
      if (ws.current) {
        // Set readyState check to avoid closing already closed connections
        if (ws.current.readyState === WebSocket.OPEN || ws.current.readyState === WebSocket.CONNECTING) {
          ws.current.close(1000, 'Component unmounting');
        }
        ws.current = null;
      }
    };
  }, [connect]);

  const value: WebSocketContextType = {
    lastMessage,
    connectionStatus,
    isConnected: connectionStatus === 'Open',
    reconnectAttempts,
  };

  return (
    <WebSocketContext.Provider value={value}>
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocketContext = (): WebSocketContextType => {
  const context = useContext(WebSocketContext);
  if (context === undefined) {
    throw new Error('useWebSocketContext must be used within a WebSocketProvider');
  }
  return context;
};
