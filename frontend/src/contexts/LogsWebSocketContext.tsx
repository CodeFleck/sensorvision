import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { LogEntry, LogSource, LogsWebSocketMessage } from '../types';
import { config } from '../config';

const MAX_LOG_ENTRIES = 10000;

interface LogsWebSocketContextType {
  logs: LogEntry[];
  connectionStatus: 'Connecting' | 'Open' | 'Closing' | 'Closed';
  isConnected: boolean;
  reconnectAttempts: number;
  availableSources: LogSource[];
  dockerAvailable: boolean;
  subscribedSources: LogSource[];
  subscribe: (sources: LogSource[]) => void;
  unsubscribe: (sources?: LogSource[]) => void;
  clearLogs: () => void;
  requestHistory: (source: LogSource, lines?: number) => void;
  error: string | null;
}

const LogsWebSocketContext = createContext<LogsWebSocketContextType | undefined>(undefined);

interface LogsWebSocketProviderProps {
  children: React.ReactNode;
  maxReconnectAttempts?: number;
  reconnectInterval?: number;
}

export const LogsWebSocketProvider: React.FC<LogsWebSocketProviderProps> = ({
  children,
  maxReconnectAttempts = Infinity,
  reconnectInterval = 3000,
}) => {
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [connectionStatus, setConnectionStatus] = useState<'Connecting' | 'Open' | 'Closing' | 'Closed'>('Closed');
  const [reconnectAttempts, setReconnectAttempts] = useState(0);
  const [availableSources, setAvailableSources] = useState<LogSource[]>([]);
  const [dockerAvailable, setDockerAvailable] = useState(false);
  const [subscribedSources, setSubscribedSources] = useState<LogSource[]>([]);
  const [error, setError] = useState<string | null>(null);

  const ws = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);
  const shouldReconnect = useRef(true);
  const reconnectAttemptCount = useRef(0);

  const addLogEntry = useCallback((entry: LogEntry) => {
    setLogs(prev => {
      const newLogs = [...prev, entry];
      // Keep only the last MAX_LOG_ENTRIES
      if (newLogs.length > MAX_LOG_ENTRIES) {
        return newLogs.slice(-MAX_LOG_ENTRIES);
      }
      return newLogs;
    });
  }, []);

  const handleMessage = useCallback((data: LogsWebSocketMessage) => {
    switch (data.type) {
      case 'connected':
        console.log('[LogsWebSocket] Connected:', data.message);
        if (data.availableSources) {
          setAvailableSources(data.availableSources);
        }
        if (data.dockerAvailable !== undefined) {
          setDockerAvailable(data.dockerAvailable);
        }
        setError(null);
        break;

      case 'subscribed':
        console.log('[LogsWebSocket] Subscribed to:', data.sources);
        if (data.sources) {
          setSubscribedSources(data.sources);
        }
        break;

      case 'unsubscribed':
        console.log('[LogsWebSocket] Unsubscribed, remaining:', data.sources);
        if (data.sources) {
          setSubscribedSources(data.sources);
        }
        break;

      case 'log':
        if (data.entry) {
          addLogEntry(data.entry);
        }
        break;

      case 'history':
        if (data.logs) {
          setLogs(prev => [...data.logs!, ...prev].slice(-MAX_LOG_ENTRIES));
        }
        break;

      case 'error':
        console.error('[LogsWebSocket] Error:', data.message);
        setError(data.message || 'Unknown error');
        break;

      case 'pong':
        // Heartbeat response, nothing to do
        break;

      default:
        console.warn('[LogsWebSocket] Unknown message type:', data);
    }
  }, [addLogEntry]);

  const sendMessage = useCallback((message: object) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify(message));
    } else {
      console.warn('[LogsWebSocket] Cannot send message - not connected');
    }
  }, []);

  const subscribe = useCallback((sources: LogSource[]) => {
    sendMessage({ action: 'subscribe', sources });
  }, [sendMessage]);

  const unsubscribe = useCallback((sources?: LogSource[]) => {
    if (sources) {
      sendMessage({ action: 'unsubscribe', sources });
    } else {
      sendMessage({ action: 'unsubscribe' });
    }
  }, [sendMessage]);

  const clearLogs = useCallback(() => {
    setLogs([]);
  }, []);

  const requestHistory = useCallback((source: LogSource, lines = 100) => {
    sendMessage({ action: 'history', source, lines });
  }, [sendMessage]);

  const connect = useCallback(() => {
    // Don't create a new connection if one already exists
    if (ws.current?.readyState === WebSocket.OPEN || ws.current?.readyState === WebSocket.CONNECTING) {
      return;
    }

    setConnectionStatus('Connecting');
    setError(null);

    // Get JWT token for authentication
    const token = localStorage.getItem('accessToken');
    if (!token) {
      console.error('[LogsWebSocket] No access token available');
      setError('Authentication required');
      setConnectionStatus('Closed');
      return;
    }

    const wsUrl = `${config.logsWebSocketUrl}?token=${encodeURIComponent(token)}`;
    console.log('[LogsWebSocket] Connecting...');

    const websocket = new WebSocket(wsUrl);
    ws.current = websocket;

    websocket.onopen = () => {
      setConnectionStatus('Open');
      reconnectAttemptCount.current = 0;
      setReconnectAttempts(0);
      console.log('[LogsWebSocket] Connected successfully');
    };

    websocket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as LogsWebSocketMessage;
        handleMessage(data);
      } catch (err) {
        console.error('[LogsWebSocket] Failed to parse message:', err);
      }
    };

    websocket.onclose = (event) => {
      setConnectionStatus('Closed');
      setSubscribedSources([]);

      // Check for policy violation (role check failed)
      if (event.code === 1008) {
        console.error('[LogsWebSocket] Access denied - ROLE_DEVELOPER required');
        setError('Access denied - ROLE_DEVELOPER required');
        shouldReconnect.current = false;
        return;
      }

      // Don't log expected closures
      if (event.code !== 1000 && event.code !== 1001) {
        console.log(`[LogsWebSocket] Connection closed (code: ${event.code}, reason: ${event.reason || 'none'})`);
      }

      // Attempt to reconnect
      if (shouldReconnect.current && reconnectAttemptCount.current < maxReconnectAttempts) {
        reconnectAttemptCount.current++;
        setReconnectAttempts(reconnectAttemptCount.current);

        // Exponential backoff: 3s, 6s, 12s, 24s, 30s (max)
        const backoffTime = Math.min(
          reconnectInterval * Math.pow(2, reconnectAttemptCount.current - 1),
          30000
        );

        console.log(`[LogsWebSocket] Reconnecting in ${backoffTime / 1000}s (attempt ${reconnectAttemptCount.current}/${maxReconnectAttempts})...`);

        reconnectTimeoutRef.current = window.setTimeout(() => {
          connect();
        }, backoffTime);
      } else if (reconnectAttemptCount.current >= maxReconnectAttempts) {
        console.error('[LogsWebSocket] Max reconnection attempts reached');
        setError('Connection lost - max reconnection attempts reached');
      }
    };

    websocket.onerror = (err) => {
      console.error('[LogsWebSocket] Error occurred:', err);
    };
  }, [handleMessage, maxReconnectAttempts, reconnectInterval]);

  useEffect(() => {
    // Enable reconnection and connect
    shouldReconnect.current = true;
    connect();

    // Cleanup function
    return () => {
      console.log('[LogsWebSocket] Cleaning up connection...');
      shouldReconnect.current = false;

      // Clear any pending reconnect timeout
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }

      // Close the WebSocket connection
      if (ws.current) {
        if (ws.current.readyState === WebSocket.OPEN || ws.current.readyState === WebSocket.CONNECTING) {
          ws.current.close(1000, 'Component unmounting');
        }
        ws.current = null;
      }
    };
  }, [connect]);

  const value: LogsWebSocketContextType = {
    logs,
    connectionStatus,
    isConnected: connectionStatus === 'Open',
    reconnectAttempts,
    availableSources,
    dockerAvailable,
    subscribedSources,
    subscribe,
    unsubscribe,
    clearLogs,
    requestHistory,
    error,
  };

  return (
    <LogsWebSocketContext.Provider value={value}>
      {children}
    </LogsWebSocketContext.Provider>
  );
};

export const useLogsWebSocket = (): LogsWebSocketContextType => {
  const context = useContext(LogsWebSocketContext);
  if (context === undefined) {
    throw new Error('useLogsWebSocket must be used within a LogsWebSocketProvider');
  }
  return context;
};
