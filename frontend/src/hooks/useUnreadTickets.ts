import { useState, useEffect, useCallback } from 'react';
import { apiService } from '../services/api';

export const useUnreadTickets = () => {
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const fetchUnreadCount = useCallback(async () => {
    try {
      setLoading(true);
      const result = await apiService.getUnreadTicketCount();
      setUnreadCount(result.unreadCount);
    } catch (error) {
      console.error('Failed to fetch unread ticket count:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUnreadCount();

    // Poll every 30 seconds for unread count updates
    const interval = setInterval(fetchUnreadCount, 30000);

    return () => clearInterval(interval);
  }, [fetchUnreadCount]);

  const refresh = useCallback(() => {
    fetchUnreadCount();
  }, [fetchUnreadCount]);

  return { unreadCount, loading, refresh };
};
