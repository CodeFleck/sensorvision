import { useEffect, useState, useCallback } from 'react';
import { Event, EventType, EventSeverity } from '../types';
import { apiService } from '../services/api';
import { formatTimeAgo } from '../utils/timeUtils';
import { Card, CardBody } from './ui/Card';
import {
  Activity,
  Wifi,
  WifiOff,
  AlertTriangle,
  Bell,
  Layers,
  RefreshCw,
  Circle,
  Info,
} from 'lucide-react';
import { clsx } from 'clsx';

// Event type to icon and color mapping
const EVENT_CONFIG: Record<string, { icon: React.ReactNode; colorClass: string; bgClass: string }> = {
  DEVICE_CONNECTED: {
    icon: <Wifi className="h-4 w-4" />,
    colorClass: 'text-emerald-400',
    bgClass: 'bg-emerald-400/15',
  },
  DEVICE_DISCONNECTED: {
    icon: <WifiOff className="h-4 w-4" />,
    colorClass: 'text-gray-400',
    bgClass: 'bg-gray-400/15',
  },
  DEVICE_OFFLINE: {
    icon: <WifiOff className="h-4 w-4" />,
    colorClass: 'text-gray-500',
    bgClass: 'bg-gray-500/15',
  },
  ALERT_CREATED: {
    icon: <AlertTriangle className="h-4 w-4" />,
    colorClass: 'text-rose-400',
    bgClass: 'bg-rose-400/15',
  },
  ALERT_ACKNOWLEDGED: {
    icon: <Bell className="h-4 w-4" />,
    colorClass: 'text-amber-400',
    bgClass: 'bg-amber-400/15',
  },
  RULE_TRIGGERED: {
    icon: <Layers className="h-4 w-4" />,
    colorClass: 'text-purple-400',
    bgClass: 'bg-purple-400/15',
  },
  RULE_CREATED: {
    icon: <Layers className="h-4 w-4" />,
    colorClass: 'text-cyan-400',
    bgClass: 'bg-cyan-400/15',
  },
  DEFAULT: {
    icon: <Circle className="h-4 w-4" />,
    colorClass: 'text-gray-400',
    bgClass: 'bg-gray-400/15',
  },
};

// Severity color mapping
const SEVERITY_COLORS: Record<EventSeverity, string> = {
  INFO: 'border-l-cyan-400',
  WARNING: 'border-l-amber-400',
  ERROR: 'border-l-rose-400',
  CRITICAL: 'border-l-red-500',
};

interface ActivityTimelineProps {
  maxItems?: number;
  refreshIntervalMs?: number;
  className?: string;
}

export const ActivityTimeline = ({
  maxItems = 10,
  refreshIntervalMs = 30000,
  className,
}: ActivityTimelineProps) => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchEvents = useCallback(async () => {
    try {
      setError(null);
      const data = await apiService.getRecentEvents(24, maxItems);
      // Handle null/undefined response gracefully
      setEvents(data || []);
    } catch (err) {
      console.error('Failed to fetch events:', err);
      setError('Failed to load activity');
    } finally {
      setLoading(false);
    }
  }, [maxItems]);

  useEffect(() => {
    fetchEvents();
    const interval = setInterval(fetchEvents, refreshIntervalMs);
    return () => clearInterval(interval);
  }, [fetchEvents, refreshIntervalMs]);

  const getEventConfig = (eventType: EventType) => {
    return EVENT_CONFIG[eventType] || EVENT_CONFIG.DEFAULT;
  };

  const formatEventTitle = (event: Event): React.ReactNode => {
    const deviceId = event.deviceId || event.entityId;

    switch (event.eventType) {
      case 'DEVICE_CONNECTED':
        return (
          <>
            Device <strong className="text-cyan-400">{deviceId}</strong> came online
          </>
        );
      case 'DEVICE_DISCONNECTED':
      case 'DEVICE_OFFLINE':
        return (
          <>
            Device <strong className="text-cyan-400">{deviceId}</strong> went offline
          </>
        );
      case 'ALERT_CREATED':
        return (
          <>
            Alert: <strong className="text-rose-400">{event.title}</strong>
          </>
        );
      case 'RULE_TRIGGERED':
        return (
          <>
            Rule <strong className="text-purple-400">{event.title}</strong> triggered
          </>
        );
      default:
        return event.title;
    }
  };

  if (loading) {
    return (
      <Card className={className}>
        <CardBody>
          <div className="flex items-center gap-2 mb-4">
            <Activity className="h-5 w-5 text-link" />
            <h2 className="text-lg font-semibold text-primary">Activity Feed</h2>
          </div>
          <div className="flex items-center justify-center py-8">
            <RefreshCw className="h-5 w-5 animate-spin text-link mr-2" />
            <span className="text-secondary">Loading activity...</span>
          </div>
        </CardBody>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className={className}>
        <CardBody>
          <div className="flex items-center gap-2 mb-4">
            <Activity className="h-5 w-5 text-link" />
            <h2 className="text-lg font-semibold text-primary">Activity Feed</h2>
          </div>
          <div className="flex items-center justify-center py-8 text-danger">
            <AlertTriangle className="h-5 w-5 mr-2" />
            <span>{error}</span>
          </div>
        </CardBody>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <CardBody>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Activity className="h-5 w-5 text-link" />
            <h2 className="text-lg font-semibold text-primary">Activity Feed</h2>
          </div>
          <button
            onClick={fetchEvents}
            className="p-2 text-secondary hover:text-primary hover:bg-hover rounded-lg transition-colors"
            title="Refresh"
          >
            <RefreshCw className="h-4 w-4" />
          </button>
        </div>

        {events.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-secondary">
            <Info className="h-8 w-8 mb-2 opacity-50" />
            <p>No recent activity</p>
          </div>
        ) : (
          <div className="space-y-2 max-h-80 overflow-y-auto pr-1 activity-timeline-scroll">
            {events.map((event, index) => {
              const config = getEventConfig(event.eventType);
              return (
                <div
                  key={event.id}
                  className={clsx(
                    'flex items-start gap-3 p-3 rounded-lg border-l-2 transition-all duration-200',
                    'bg-hover/50 hover:bg-hover',
                    SEVERITY_COLORS[event.severity],
                    'activity-item-animate'
                  )}
                  style={{ animationDelay: `${index * 50}ms` }}
                >
                  <div
                    className={clsx(
                      'flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center',
                      config.bgClass,
                      config.colorClass
                    )}
                  >
                    {config.icon}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-primary leading-relaxed">
                      {formatEventTitle(event)}
                    </p>
                    <p className="text-xs text-tertiary mt-1 font-mono">
                      {formatTimeAgo(event.createdAt)}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardBody>
    </Card>
  );
};
