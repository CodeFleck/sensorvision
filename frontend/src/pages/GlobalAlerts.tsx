import { useEffect, useState, useCallback } from 'react';
import { AlertTriangle, CheckCircle, Clock, Filter, Users, FileText, X, Loader2 } from 'lucide-react';
import { apiService } from '../services/api';
import { clsx } from 'clsx';
import toast from 'react-hot-toast';

interface GlobalAlert {
  id: string;
  globalRuleId: string;
  globalRuleName: string;
  organizationId: number;
  message: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  triggeredValue: number;
  deviceCount: number;
  affectedDevices: string[];
  triggeredAt: string;
  acknowledged: boolean;
  acknowledgedAt: string | null;
  acknowledgedByName: string | null;
  resolved: boolean;
  resolvedAt: string | null;
  resolvedByName: string | null;
  resolutionNote: string | null;
}

interface PaginatedResponse {
  content: GlobalAlert[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const GlobalAlerts = () => {
  const [alerts, setAlerts] = useState<GlobalAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'unacknowledged' | 'unresolved'>('all');
  const [severityFilter, setSeverityFilter] = useState<string>('all');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [resolveModalOpen, setResolveModalOpen] = useState(false);
  const [selectedAlertId, setSelectedAlertId] = useState<string | null>(null);
  const [resolutionNote, setResolutionNote] = useState('');
  const [expandedDevices, setExpandedDevices] = useState<Set<string>>(new Set());
  const [processingAlertId, setProcessingAlertId] = useState<string | null>(null);
  const [resolving, setResolving] = useState(false);

  const fetchAlerts = useCallback(async () => {
    try {
      setLoading(true);
      const params: { page?: number; size?: number; unacknowledgedOnly?: boolean } = {
        page,
        size: 10,
      };
      if (filter === 'unacknowledged') {
        params.unacknowledgedOnly = true;
      }
      const data: PaginatedResponse = await apiService.getGlobalAlerts(params);
      let filteredContent = data.content;

      // Apply client-side filter for unresolved (API doesn't support this param)
      if (filter === 'unresolved') {
        filteredContent = data.content.filter(a => !a.resolved);
      }

      setAlerts(filteredContent);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (error) {
      console.error('Failed to fetch global alerts:', error);
      toast.error('Failed to load fleet alerts');
    } finally {
      setLoading(false);
    }
  }, [page, filter]);

  useEffect(() => {
    fetchAlerts();
  }, [fetchAlerts]);

  // Handle Escape key to close modal
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && resolveModalOpen) {
        setResolveModalOpen(false);
      }
    };
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [resolveModalOpen]);

  const handleAcknowledge = async (id: string) => {
    setProcessingAlertId(id);
    try {
      await apiService.acknowledgeGlobalAlert(id);
      toast.success('Alert acknowledged');
      setAlerts(prev =>
        prev.map(alert =>
          alert.id === id ? { ...alert, acknowledged: true, acknowledgedAt: new Date().toISOString() } : alert
        )
      );
    } catch (error) {
      console.error('Failed to acknowledge alert:', error);
      toast.error('Failed to acknowledge alert');
    } finally {
      setProcessingAlertId(null);
    }
  };

  const handleOpenResolveModal = (id: string) => {
    setSelectedAlertId(id);
    setResolutionNote('');
    setResolveModalOpen(true);
  };

  const handleResolve = async () => {
    if (!selectedAlertId) return;
    setResolving(true);
    try {
      await apiService.resolveGlobalAlert(selectedAlertId, resolutionNote || undefined);
      toast.success('Alert resolved');
      setAlerts(prev =>
        prev.map(alert =>
          alert.id === selectedAlertId
            ? { ...alert, resolved: true, resolvedAt: new Date().toISOString(), resolutionNote }
            : alert
        )
      );
      setResolveModalOpen(false);
      setSelectedAlertId(null);
      setResolutionNote('');
    } catch (error) {
      console.error('Failed to resolve alert:', error);
      toast.error('Failed to resolve alert');
    } finally {
      setResolving(false);
    }
  };

  const toggleDevicesList = (alertId: string) => {
    setExpandedDevices(prev => {
      const newSet = new Set(prev);
      if (newSet.has(alertId)) {
        newSet.delete(alertId);
      } else {
        newSet.add(alertId);
      }
      return newSet;
    });
  };

  const filteredAlerts = alerts.filter(alert => {
    if (severityFilter !== 'all' && alert.severity !== severityFilter) return false;
    return true;
  });

  const severityColors = {
    LOW: 'bg-blue-100 text-blue-800',
    MEDIUM: 'bg-yellow-100 text-yellow-800',
    HIGH: 'bg-orange-100 text-orange-800',
    CRITICAL: 'bg-red-100 text-red-800',
  };

  const severityBorderColors = {
    LOW: 'border-l-blue-500',
    MEDIUM: 'border-l-yellow-500',
    HIGH: 'border-l-orange-500',
    CRITICAL: 'border-l-red-500',
  };

  const severityIcons = {
    LOW: 'bg-blue-500',
    MEDIUM: 'bg-yellow-500',
    HIGH: 'bg-orange-500',
    CRITICAL: 'bg-red-500',
  };

  const unacknowledgedCount = alerts.filter(alert => !alert.acknowledged).length;
  const unresolvedCount = alerts.filter(alert => !alert.resolved).length;

  if (loading && alerts.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading fleet alerts...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Fleet Alerts</h1>
          <p className="text-gray-600 mt-1">
            Monitor and manage alerts from fleet-wide rules
            {unacknowledgedCount > 0 && (
              <span className="ml-2 bg-red-100 text-red-800 text-xs px-2 py-1 rounded-full">
                {unacknowledgedCount} unacknowledged
              </span>
            )}
            {unresolvedCount > 0 && (
              <span className="ml-2 bg-orange-100 text-orange-800 text-xs px-2 py-1 rounded-full">
                {unresolvedCount} unresolved
              </span>
            )}
          </p>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg border border-gray-200 p-4">
        <div className="flex items-center space-x-4 flex-wrap gap-y-2">
          <div className="flex items-center space-x-2">
            <Filter className="h-4 w-4 text-gray-500" />
            <span className="text-sm font-medium text-gray-700">Status:</span>
          </div>

          <div className="flex space-x-2">
            {[
              { key: 'all', label: 'All' },
              { key: 'unacknowledged', label: 'Unacknowledged' },
              { key: 'unresolved', label: 'Unresolved' },
            ].map((filterOption) => (
              <button
                key={filterOption.key}
                onClick={() => {
                  setFilter(filterOption.key as 'all' | 'unacknowledged' | 'unresolved');
                  setPage(0);
                }}
                className={clsx(
                  'px-3 py-1 text-xs font-medium rounded-full transition-colors',
                  filter === filterOption.key
                    ? 'bg-blue-100 text-blue-800'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                )}
              >
                {filterOption.label}
              </button>
            ))}
          </div>

          <div className="flex items-center space-x-2">
            <label htmlFor="severity-filter" className="text-sm text-gray-600">
              Severity:
            </label>
            <select
              id="severity-filter"
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

          <div className="text-xs text-gray-500">
            Showing {filteredAlerts.length} of {totalElements} alerts
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
              alert.resolved
                ? 'border-gray-200 opacity-75'
                : alert.acknowledged
                ? 'border-gray-200'
                : `border-l-4 ${severityBorderColors[alert.severity]}`
            )}
          >
            <div className="flex items-start justify-between">
              <div className="flex items-start space-x-3 flex-1">
                <div className={clsx('w-2 h-2 rounded-full mt-2', severityIcons[alert.severity])} />
                <div className="flex-1">
                  <div className="flex items-center space-x-2 mb-1 flex-wrap gap-y-1">
                    <h3 className="text-sm font-medium text-gray-900">{alert.globalRuleName || 'Fleet Rule'}</h3>
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
                    {alert.resolved && (
                      <span className="flex items-center text-xs text-blue-600">
                        <FileText className="h-3 w-3 mr-1" />
                        Resolved
                      </span>
                    )}
                  </div>

                  <p className="text-sm text-gray-600 mb-2">{alert.message}</p>

                  {/* Triggered Value */}
                  <div className="text-sm text-gray-700 mb-2">
                    <span className="font-medium">Triggered Value:</span> {alert.triggeredValue}
                  </div>

                  {/* Device Count */}
                  <div className="flex items-center text-sm text-gray-600 mb-2">
                    <Users className="h-4 w-4 mr-1" />
                    <span>{alert.deviceCount} device{alert.deviceCount !== 1 ? 's' : ''} affected</span>
                    {alert.affectedDevices && alert.affectedDevices.length > 0 && (
                      <button
                        onClick={() => toggleDevicesList(alert.id)}
                        className="ml-2 text-blue-600 hover:text-blue-800 text-xs"
                      >
                        {expandedDevices.has(alert.id) ? 'Hide devices' : 'Show devices'}
                      </button>
                    )}
                  </div>

                  {/* Expanded Devices List */}
                  {expandedDevices.has(alert.id) && alert.affectedDevices && (
                    <div className="bg-gray-50 rounded p-2 mb-2 max-h-32 overflow-y-auto">
                      <div className="text-xs text-gray-600 grid grid-cols-2 gap-1">
                        {alert.affectedDevices.map((deviceId) => (
                          <span key={deviceId} className="font-mono truncate">{deviceId}</span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Resolution Note */}
                  {alert.resolved && alert.resolutionNote && (
                    <div className="bg-blue-50 rounded p-2 mb-2 text-sm">
                      <span className="font-medium text-blue-800">Resolution:</span>{' '}
                      <span className="text-blue-700">{alert.resolutionNote}</span>
                    </div>
                  )}

                  <div className="flex items-center text-xs text-gray-500 space-x-4 flex-wrap gap-y-1">
                    <span className="flex items-center">
                      <Clock className="h-3 w-3 mr-1" />
                      Triggered: {new Date(alert.triggeredAt).toLocaleString()}
                    </span>
                    {alert.acknowledgedAt && (
                      <span>Acknowledged: {new Date(alert.acknowledgedAt).toLocaleString()}</span>
                    )}
                    {alert.resolvedAt && (
                      <span>Resolved: {new Date(alert.resolvedAt).toLocaleString()}</span>
                    )}
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex flex-col space-y-2 ml-4">
                {!alert.acknowledged && (
                  <button
                    onClick={() => handleAcknowledge(alert.id)}
                    disabled={processingAlertId === alert.id}
                    className={clsx(
                      'flex items-center space-x-1 text-xs font-medium whitespace-nowrap',
                      processingAlertId === alert.id
                        ? 'text-gray-400 cursor-not-allowed'
                        : 'text-blue-600 hover:text-blue-800'
                    )}
                  >
                    {processingAlertId === alert.id ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <CheckCircle className="h-4 w-4" />
                    )}
                    <span>{processingAlertId === alert.id ? 'Processing...' : 'Acknowledge'}</span>
                  </button>
                )}
                {!alert.resolved && (
                  <button
                    onClick={() => handleOpenResolveModal(alert.id)}
                    className="flex items-center space-x-1 text-green-600 hover:text-green-800 text-xs font-medium whitespace-nowrap"
                  >
                    <FileText className="h-4 w-4" />
                    <span>Resolve</span>
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}

        {filteredAlerts.length === 0 && (
          <div className="text-center py-8">
            <AlertTriangle className="h-12 w-12 text-gray-300 mx-auto mb-4" />
            <h3 className="text-sm font-medium text-gray-900 mb-2">No fleet alerts found</h3>
            <p className="text-sm text-gray-500">
              {filter === 'all'
                ? 'No fleet-wide alerts have been triggered yet.'
                : `No ${filter} fleet alerts found.`}
            </p>
          </div>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center space-x-2">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className={clsx(
              'px-3 py-1 text-sm rounded border',
              page === 0
                ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                : 'bg-white text-gray-700 hover:bg-gray-50'
            )}
          >
            Previous
          </button>
          <span className="text-sm text-gray-600">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className={clsx(
              'px-3 py-1 text-sm rounded border',
              page >= totalPages - 1
                ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                : 'bg-white text-gray-700 hover:bg-gray-50'
            )}
          >
            Next
          </button>
        </div>
      )}

      {/* Resolve Modal */}
      {resolveModalOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
          role="dialog"
          aria-modal="true"
          aria-labelledby="resolve-modal-title"
          onClick={(e) => {
            if (e.target === e.currentTarget) setResolveModalOpen(false);
          }}
        >
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 id="resolve-modal-title" className="text-lg font-semibold text-gray-900">
                Resolve Alert
              </h3>
              <button
                onClick={() => setResolveModalOpen(false)}
                className="text-gray-400 hover:text-gray-600"
                aria-label="Close modal"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="mb-4">
              <label htmlFor="resolution-note" className="block text-sm font-medium text-gray-700 mb-2">
                Resolution Note (optional)
              </label>
              <textarea
                id="resolution-note"
                value={resolutionNote}
                onChange={(e) => setResolutionNote(e.target.value)}
                placeholder="Describe how this alert was resolved..."
                className="w-full border border-gray-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                rows={4}
              />
            </div>
            <div className="flex justify-end space-x-3">
              <button
                onClick={() => setResolveModalOpen(false)}
                className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800"
                disabled={resolving}
              >
                Cancel
              </button>
              <button
                onClick={handleResolve}
                disabled={resolving}
                className={clsx(
                  'px-4 py-2 text-sm text-white rounded-lg flex items-center space-x-2',
                  resolving
                    ? 'bg-green-400 cursor-not-allowed'
                    : 'bg-green-600 hover:bg-green-700'
                )}
              >
                {resolving && <Loader2 className="h-4 w-4 animate-spin" />}
                <span>{resolving ? 'Resolving...' : 'Mark as Resolved'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
