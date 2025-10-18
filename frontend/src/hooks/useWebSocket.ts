import { TelemetryPoint } from '../types';
import { useWebSocketContext } from '../contexts/WebSocketContext';

interface UseWebSocketReturn {
  lastMessage: TelemetryPoint | null;
  connectionStatus: 'Connecting' | 'Open' | 'Closing' | 'Closed';
  sendMessage: (message: string) => void;
}

/**
 * Hook to access WebSocket connection managed by WebSocketProvider.
 * Note: url parameter is deprecated and ignored - connection is managed at the provider level.
 *
 * @deprecated The url parameter is no longer used. WebSocket connection is managed by WebSocketProvider.
 */
export const useWebSocket = (_url?: string): UseWebSocketReturn => {
  const { lastMessage, connectionStatus } = useWebSocketContext();

  // sendMessage is not currently supported in the context-based implementation
  // This is a placeholder for backward compatibility
  const sendMessage = (_message: string) => {
    console.warn('[useWebSocket] sendMessage is not implemented in context-based WebSocket');
  };

  return {
    lastMessage,
    connectionStatus,
    sendMessage,
  };
};