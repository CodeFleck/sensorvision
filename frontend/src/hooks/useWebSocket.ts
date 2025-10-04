import { useEffect, useRef, useState } from 'react';
import { TelemetryPoint } from '../types';

interface UseWebSocketReturn {
  lastMessage: TelemetryPoint | null;
  connectionStatus: 'Connecting' | 'Open' | 'Closing' | 'Closed';
  sendMessage: (message: string) => void;
}

export const useWebSocket = (url: string): UseWebSocketReturn => {
  const [lastMessage, setLastMessage] = useState<TelemetryPoint | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<'Connecting' | 'Open' | 'Closing' | 'Closed'>('Closed');
  const ws = useRef<WebSocket | null>(null);

  const sendMessage = (message: string) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(message);
    }
  };

  useEffect(() => {
    const websocket = new WebSocket(url);
    ws.current = websocket;

    websocket.onopen = () => {
      setConnectionStatus('Open');
      console.log('WebSocket connected');
    };

    websocket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as TelemetryPoint;
        setLastMessage(data);
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    websocket.onclose = () => {
      setConnectionStatus('Closed');
      console.log('WebSocket disconnected');
    };

    websocket.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    return () => {
      websocket.close();
    };
  }, [url]);

  return {
    lastMessage,
    connectionStatus,
    sendMessage,
  };
};