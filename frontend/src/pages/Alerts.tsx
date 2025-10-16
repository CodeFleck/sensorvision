import { useEffect, useState } from 'react';
import { AlertTriangle, CheckCircle, Clock, Filter } from 'lucide-react';
import { Alert } from '../types';
import { apiService } from '../services/api';
import { clsx } from 'clsx';

export const Alerts = () => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'unacknowledged' | 'acknowledged'>('all');
  const [severityFilter, setSeverityFilter] = useState<string>('all');

  useEffect(() => {
    fetchAlerts();
  }, []);

  const fetchAlerts = async () => {
    try {
      const data = await apiService.getAlerts().catch(() => []); // Handle if endpoint doesn't exist yet
      setAlerts(data);
    } catch (error) {
      console.error('Failed to fetch alerts:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAcknowledge = async (id: string) => {
    try {
      await apiService.acknowledgeAlert(id);
      setAlerts(prev =>
        prev.map(alert =>
          alert.id === id ? { ...alert, acknowledged: true } : alert
        )
      );
    } catch (error) {
      console.error('Failed to acknowledge alert:', error);
    }
  };

  const filteredAlerts = alerts.filter(alert => {
    if (filter === 'acknowledged' && !alert.acknowledged) return false;
    if (filter === 'unacknowledged' && alert.acknowledged) return false;
    if (severityFilter !== 'all' && alert.severity !== severityFilter) return false;
    return true;
  });

  const severityColors = {
    LOW: 'bg-blue-100 text-blue-800',
    MEDIUM: 'bg-yellow-100 text-yellow-800',
    HIGH: 'bg-orange-100 text-orange-800',
    CRITICAL: 'bg-red-100 text-red-800',
  };

  const severityIcons = {
    LOW: 'bg-blue-500',
    MEDIUM: 'bg-yellow-500',
    HIGH: 'bg-orange-500',
    CRITICAL: 'bg-red-500',
  };

  const unacknowledgedCount = alerts.filter(alert => !alert.acknowledged).length;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading alerts...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Alert Management</h1>
          <p className="text-gray-600 mt-1">
            Monitor and manage alerts from your IoT devices
            {unacknowledgedCount > 0 && (
              <span className="ml-2 bg-red-100 text-red-800 text-xs px-2 py-1 rounded-full">
                {unacknowledgedCount} unacknowledged
              </span>
            )}
          </p>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg border border-gray-200 p-4">
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            <Filter className="h-4 w-4 text-gray-500" />
            <span className="text-sm font-medium text-gray-700">Filters:</span>
          </div>

          <div className="flex space-x-2">
            {['all', 'unacknowledged', 'acknowledged'].map((filterOption) => (
              <button
                key={filterOption}
                onClick={() => setFilter(filterOption as 'all' | 'unacknowledged' | 'acknowledged')}
                className={clsx(
                  'px-3 py-1 text-xs font-medium rounded-full transition-colors',
                  filter === filterOption
                    ? 'bg-blue-100 text-blue-800'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                )}
              >
                {filterOption.charAt(0).toUpperCase() + filterOption.slice(1)}
              </button>
            ))}
          </div>

          <div className="flex items-center space-x-2">
            <span className="text-sm text-gray-600">Severity:</span>
            <select
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value)}
              className="text-xs border border-gray-300 rounded px-2 py-1"
            >
              <option value="all">All</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>
        </div>
      </div>

      {/* Alerts List */}
      <div className="space-y-3">
        {filteredAlerts.map((alert) => (
          <div
            key={alert.id}
            className={clsx(
              'bg-white rounded-lg border p-4 transition-colors',
              alert.acknowledged
                ? 'border-gray-200'
                : 'border-l-4',
              !alert.acknowledged && {
                'border-l-blue-500': alert.severity === 'LOW',
                'border-l-yellow-500': alert.severity === 'MEDIUM',
                'border-l-orange-500': alert.severity === 'HIGH',
                'border-l-red-500': alert.severity === 'CRITICAL',
              }
            )}
          >
            <div className="flex items-start justify-between">
              <div className="flex items-start space-x-3">
                <div className={clsx('w-2 h-2 rounded-full mt-2', severityIcons[alert.severity])} />
                <div className="flex-1">
                  <div className="flex items-center space-x-2 mb-1">
                    <h3 className="text-sm font-medium text-gray-900">{alert.ruleName}</h3>
                    <span
                      className={clsx(
                        'px-2 py-1 text-xs font-medium rounded-full',
                        severityColors[alert.severity]
                      )}
                    >
                      {alert.severity}
                    </span>
                    {alert.acknowledged && (
                      <span className="flex items-center text-xs text-green-600">
                        <CheckCircle className="h-3 w-3 mr-1" />
                        Acknowledged
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-600 mb-2">{alert.message}</p>
                  <div className="flex items-center text-xs text-gray-500 space-x-4">
                    <span>Device: {alert.deviceId}</span>
                    <span className="flex items-center">
                      <Clock className="h-3 w-3 mr-1" />
                      {new Date(alert.timestamp).toLocaleString()}
                    </span>
                  </div>
                </div>
              </div>

              {!alert.acknowledged && (
                <button
                  onClick={() => handleAcknowledge(alert.id)}
                  className="flex items-center space-x-1 text-blue-600 hover:text-blue-800 text-xs font-medium"
                >
                  <CheckCircle className="h-4 w-4" />
                  <span>Acknowledge</span>
                </button>
              )}
            </div>
          </div>
        ))}

        {filteredAlerts.length === 0 && (
          <div className="text-center py-8">
            <AlertTriangle className="h-12 w-12 text-gray-300 mx-auto mb-4" />
            <h3 className="text-sm font-medium text-gray-900 mb-2">No alerts found</h3>
            <p className="text-sm text-gray-500">
              {filter === 'all'
                ? 'No alerts have been triggered yet.'
                : `No ${filter} alerts found.`}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};