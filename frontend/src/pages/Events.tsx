import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';
import { Event, EventType, EventSeverity } from '../types';
import {
  InformationCircleIcon,
  ExclamationTriangleIcon,
  XCircleIcon,
  FireIcon
} from '@heroicons/react/24/outline';

export const Events: React.FC = () => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters
  const [selectedEventType, setSelectedEventType] = useState<EventType | ''>('');
  const [selectedSeverity, setSelectedSeverity] = useState<EventSeverity | ''>('');
  const [selectedDeviceId, setSelectedDeviceId] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    fetchEvents();
  }, [selectedEventType, selectedSeverity, selectedDeviceId, page]);

  const fetchEvents = async () => {
    try {
      setLoading(true);
      const params: any = {
        page,
        size: 50,
      };

      if (selectedEventType) params.eventType = selectedEventType;
      if (selectedSeverity) params.severity = selectedSeverity;
      if (selectedDeviceId) params.deviceId = selectedDeviceId;

      const response = await apiService.getEvents(params);
      setEvents(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
      setError('');
    } catch (err) {
      setError('Failed to fetch events');
      console.error('Error fetching events:', err);
    } finally {
      setLoading(false);
    }
  };

  const getSeverityIcon = (severity: EventSeverity) => {
    switch (severity) {
      case 'INFO':
        return <InformationCircleIcon className="h-5 w-5 text-blue-500" />;
      case 'WARNING':
        return <ExclamationTriangleIcon className="h-5 w-5 text-yellow-500" />;
      case 'ERROR':
        return <XCircleIcon className="h-5 w-5 text-red-500" />;
      case 'CRITICAL':
        return <FireIcon className="h-5 w-5 text-red-700" />;
    }
  };

  const getSeverityBadgeClass = (severity: EventSeverity) => {
    switch (severity) {
      case 'INFO':
        return 'bg-blue-100 text-blue-800';
      case 'WARNING':
        return 'bg-yellow-100 text-yellow-800';
      case 'ERROR':
        return 'bg-red-100 text-red-800';
      case 'CRITICAL':
        return 'bg-red-200 text-red-900 font-bold';
    }
  };

  const formatEventType = (type: EventType) => {
    return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  };

  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  const eventTypes: EventType[] = [
    'DEVICE_CREATED', 'DEVICE_UPDATED', 'DEVICE_DELETED', 'DEVICE_CONNECTED',
    'DEVICE_DISCONNECTED', 'DEVICE_OFFLINE', 'TELEMETRY_RECEIVED', 'TELEMETRY_ANOMALY',
    'RULE_CREATED', 'RULE_UPDATED', 'RULE_DELETED', 'RULE_TRIGGERED',
    'ALERT_CREATED', 'ALERT_ACKNOWLEDGED', 'ALERT_RESOLVED',
    'DASHBOARD_CREATED', 'DASHBOARD_UPDATED', 'DASHBOARD_DELETED',
    'WIDGET_CREATED', 'WIDGET_UPDATED', 'WIDGET_DELETED',
    'USER_LOGIN', 'USER_LOGOUT', 'USER_REGISTERED', 'USER_UPDATED', 'USER_DELETED',
    'SYSTEM_ERROR', 'SYSTEM_WARNING', 'SYSTEM_INFO',
    'SYNTHETIC_VARIABLE_CREATED', 'SYNTHETIC_VARIABLE_UPDATED',
    'SYNTHETIC_VARIABLE_DELETED', 'SYNTHETIC_VARIABLE_CALCULATED'
  ];

  const severities: EventSeverity[] = ['INFO', 'WARNING', 'ERROR', 'CRITICAL'];

  return (
    <div className="space-y-6">
      <div className="sm:flex sm:items-center sm:justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">Events</h1>
        <div className="mt-4 sm:mt-0 text-sm text-gray-500">
          {totalElements} total events
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white shadow rounded-lg p-4">
        <h2 className="text-lg font-medium text-gray-900 mb-4">Filters</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div>
            <label htmlFor="eventType" className="block text-sm font-medium text-gray-700">
              Event Type
            </label>
            <select
              id="eventType"
              value={selectedEventType}
              onChange={(e) => {
                setSelectedEventType(e.target.value as EventType | '');
                setPage(0);
              }}
              className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-md"
            >
              <option value="">All Types</option>
              {eventTypes.map((type) => (
                <option key={type} value={type}>
                  {formatEventType(type)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="severity" className="block text-sm font-medium text-gray-700">
              Severity
            </label>
            <select
              id="severity"
              value={selectedSeverity}
              onChange={(e) => {
                setSelectedSeverity(e.target.value as EventSeverity | '');
                setPage(0);
              }}
              className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-md"
            >
              <option value="">All Severities</option>
              {severities.map((severity) => (
                <option key={severity} value={severity}>
                  {severity}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="deviceId" className="block text-sm font-medium text-gray-700">
              Device ID
            </label>
            <input
              type="text"
              id="deviceId"
              value={selectedDeviceId}
              onChange={(e) => {
                setSelectedDeviceId(e.target.value);
                setPage(0);
              }}
              placeholder="Filter by device..."
              className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-md"
            />
          </div>
        </div>
      </div>

      {/* Events List */}
      {error && (
        <div className="rounded-md bg-red-50 p-4">
          <p className="text-sm text-red-800">{error}</p>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      ) : (
        <>
          <div className="bg-white shadow overflow-hidden sm:rounded-lg">
            <ul className="divide-y divide-gray-200">
              {events.length === 0 ? (
                <li className="px-6 py-12 text-center text-gray-500">
                  No events found
                </li>
              ) : (
                events.map((event) => (
                  <li key={event.id} className="px-6 py-4 hover:bg-gray-50">
                    <div className="flex items-start space-x-3">
                      <div className="flex-shrink-0 mt-1">
                        {getSeverityIcon(event.severity)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-2">
                            <p className="text-sm font-medium text-gray-900">
                              {event.title}
                            </p>
                            <span
                              className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getSeverityBadgeClass(
                                event.severity
                              )}`}
                            >
                              {event.severity}
                            </span>
                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                              {formatEventType(event.eventType)}
                            </span>
                          </div>
                          <p className="text-sm text-gray-500">
                            {formatTimestamp(event.createdAt)}
                          </p>
                        </div>
                        {event.description && (
                          <p className="mt-1 text-sm text-gray-600">
                            {event.description}
                          </p>
                        )}
                        {event.deviceId && (
                          <p className="mt-1 text-xs text-gray-500">
                            Device: {event.deviceId}
                          </p>
                        )}
                      </div>
                    </div>
                  </li>
                ))
              )}
            </ul>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 sm:px-6 rounded-lg">
              <div className="flex flex-1 justify-between sm:hidden">
                <button
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                  className="relative inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                  className="relative ml-3 inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                >
                  Next
                </button>
              </div>
              <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm text-gray-700">
                    Showing page <span className="font-medium">{page + 1}</span> of{' '}
                    <span className="font-medium">{totalPages}</span>
                  </p>
                </div>
                <div>
                  <nav className="isolate inline-flex -space-x-px rounded-md shadow-sm">
                    <button
                      onClick={() => setPage(Math.max(0, page - 1))}
                      disabled={page === 0}
                      className="relative inline-flex items-center rounded-l-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 disabled:opacity-50"
                    >
                      Previous
                    </button>
                    <button
                      onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                      disabled={page >= totalPages - 1}
                      className="relative inline-flex items-center rounded-r-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 disabled:opacity-50"
                    >
                      Next
                    </button>
                  </nav>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};
