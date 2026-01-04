import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import {
  Search,
  AlertOctagon,
  CheckCircle,
  Eye,
  XCircle,
  RefreshCw,
  Filter,
  ChevronDown,
  Cpu,
  Clock,
  TrendingUp,
} from 'lucide-react';
import { clsx } from 'clsx';
import {
  mlAnomaliesApi,
  MLAnomaly,
  MLAnomalyStatus,
  MLAnomalySeverity,
  AnomalyStatsResponse,
  getAnomalySeverityLabel,
  getAnomalySeverityColor,
  getAnomalyStatusLabel,
  getAnomalyStatusColor,
  PageResponse,
} from '../services/mlService';
import { formatTimeAgo } from '../utils/timeUtils';

export const MLAnomalies = () => {
  const [anomalies, setAnomalies] = useState<MLAnomaly[]>([]);
  const [stats, setStats] = useState<AnomalyStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<MLAnomalyStatus | ''>('');
  const [severityFilter, setSeverityFilter] = useState<MLAnomalySeverity | ''>('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  useEffect(() => {
    const loadData = async () => {
      await fetchAnomalies();
      await fetchStats();
    };
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, statusFilter, severityFilter]);

  const fetchAnomalies = async () => {
    try {
      setLoading(true);
      const response: PageResponse<MLAnomaly> = await mlAnomaliesApi.list({
        page,
        size: 10,
        status: statusFilter || undefined,
        severity: severityFilter || undefined,
      });
      setAnomalies(response.content);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Failed to fetch anomalies:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchStats = async () => {
    try {
      const data = await mlAnomaliesApi.getStats(24);
      setStats(data);
    } catch (error) {
      console.error('Failed to fetch anomaly stats:', error);
    }
  };

  const handleAcknowledge = async (anomaly: MLAnomaly) => {
    if (actionLoading) return;
    try {
      setActionLoading(anomaly.id);
      await mlAnomaliesApi.acknowledge(anomaly.id);
      toast.success('Anomaly acknowledged');
      await fetchAnomalies();
      await fetchStats();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to acknowledge anomaly';
      toast.error(message);
      console.error('Failed to acknowledge anomaly:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleInvestigate = async (anomaly: MLAnomaly) => {
    if (actionLoading) return;
    try {
      setActionLoading(anomaly.id);
      await mlAnomaliesApi.investigate(anomaly.id);
      toast.success('Investigation started');
      await fetchAnomalies();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to start investigation';
      toast.error(message);
      console.error('Failed to start investigation:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleResolve = async (anomaly: MLAnomaly) => {
    if (actionLoading) return;
    const note = window.prompt('Resolution note (optional):');
    try {
      setActionLoading(anomaly.id);
      await mlAnomaliesApi.resolve(anomaly.id, { resolutionNote: note || undefined });
      toast.success('Anomaly resolved');
      await fetchAnomalies();
      await fetchStats();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to resolve anomaly';
      toast.error(message);
      console.error('Failed to resolve anomaly:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleMarkFalsePositive = async (anomaly: MLAnomaly) => {
    if (actionLoading) return;
    if (!window.confirm('Mark this anomaly as a false positive?')) return;
    const note = window.prompt('Note (optional):');
    try {
      setActionLoading(anomaly.id);
      await mlAnomaliesApi.markFalsePositive(anomaly.id, { resolutionNote: note || undefined });
      toast.success('Marked as false positive');
      await fetchAnomalies();
      await fetchStats();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to mark as false positive';
      toast.error(message);
      console.error('Failed to mark as false positive:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const filteredAnomalies = anomalies.filter(anomaly =>
    (anomaly.deviceName?.toLowerCase().includes(searchTerm.toLowerCase()) || false) ||
    (anomaly.anomalyType?.toLowerCase().includes(searchTerm.toLowerCase()) || false)
  );

  const severityColorMap: Record<string, string> = {
    blue: 'bg-blue-100 text-blue-800 border-blue-200',
    yellow: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    orange: 'bg-orange-100 text-orange-800 border-orange-200',
    red: 'bg-red-100 text-red-800 border-red-200',
    gray: 'bg-gray-100 text-gray-800 border-gray-200',
  };

  const statusColorMap: Record<string, string> = {
    red: 'bg-red-100 text-red-800',
    yellow: 'bg-yellow-100 text-yellow-800',
    blue: 'bg-blue-100 text-blue-800',
    green: 'bg-green-100 text-green-800',
    gray: 'bg-gray-100 text-gray-800',
  };

  const statusOptions: MLAnomalyStatus[] = ['NEW', 'ACKNOWLEDGED', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE'];
  const severityOptions: MLAnomalySeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  if (loading && anomalies.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-secondary">Loading anomalies...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-primary">ML Anomaly Dashboard</h1>
          <p className="text-secondary mt-1">
            Monitor and investigate machine learning detected anomalies
          </p>
        </div>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-primary rounded-lg border border-default p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-secondary">New Anomalies</p>
                <p className="text-2xl font-bold text-primary mt-1">{stats.newCount}</p>
                <p className="text-xs text-secondary mt-1">Last {stats.periodHours}h</p>
              </div>
              <div className="p-3 bg-red-100 rounded-lg">
                <AlertOctagon className="h-6 w-6 text-red-600" />
              </div>
            </div>
          </div>

          <div className="bg-primary rounded-lg border border-default p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-secondary">Critical</p>
                <p className="text-2xl font-bold text-red-600 mt-1">{stats.bySeverity.CRITICAL || 0}</p>
                <p className="text-xs text-secondary mt-1">Requires immediate action</p>
              </div>
              <div className="p-3 bg-red-100 rounded-lg">
                <TrendingUp className="h-6 w-6 text-red-600" />
              </div>
            </div>
          </div>

          <div className="bg-primary rounded-lg border border-default p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-secondary">High Severity</p>
                <p className="text-2xl font-bold text-orange-600 mt-1">{stats.bySeverity.HIGH || 0}</p>
                <p className="text-xs text-secondary mt-1">Needs attention</p>
              </div>
              <div className="p-3 bg-orange-100 rounded-lg">
                <AlertOctagon className="h-6 w-6 text-orange-600" />
              </div>
            </div>
          </div>

          <div className="bg-primary rounded-lg border border-default p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-secondary">Medium/Low</p>
                <p className="text-2xl font-bold text-yellow-600 mt-1">
                  {(stats.bySeverity.MEDIUM || 0) + (stats.bySeverity.LOW || 0)}
                </p>
                <p className="text-xs text-secondary mt-1">Monitor trends</p>
              </div>
              <div className="p-3 bg-yellow-100 rounded-lg">
                <Clock className="h-6 w-6 text-yellow-600" />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap gap-4">
        {/* Search */}
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-secondary" />
          <input
            type="text"
            placeholder="Search by device or type..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-link focus:border-link"
          />
        </div>

        {/* Status Filter */}
        <div className="relative">
          <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-secondary" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as MLAnomalyStatus | '')}
            className="pl-10 pr-8 py-2 border border-default rounded-lg bg-primary text-primary appearance-none cursor-pointer focus:ring-2 focus:ring-link focus:border-link"
          >
            <option value="">All Statuses</option>
            {statusOptions.map(status => (
              <option key={status} value={status}>{getAnomalyStatusLabel(status)}</option>
            ))}
          </select>
          <ChevronDown className="absolute right-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-secondary pointer-events-none" />
        </div>

        {/* Severity Filter */}
        <div className="relative">
          <select
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value as MLAnomalySeverity | '')}
            className="pl-4 pr-8 py-2 border border-default rounded-lg bg-primary text-primary appearance-none cursor-pointer focus:ring-2 focus:ring-link focus:border-link"
          >
            <option value="">All Severities</option>
            {severityOptions.map(severity => (
              <option key={severity} value={severity}>{getAnomalySeverityLabel(severity)}</option>
            ))}
          </select>
          <ChevronDown className="absolute right-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-secondary pointer-events-none" />
        </div>

        {/* Refresh */}
        <button
          onClick={() => { fetchAnomalies(); fetchStats(); }}
          disabled={loading}
          className="flex items-center space-x-2 px-4 py-2 border border-default rounded-lg bg-primary text-secondary hover:bg-hover transition-colors disabled:opacity-50"
        >
          <RefreshCw className={clsx('h-4 w-4', loading && 'animate-spin')} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Anomalies List */}
      <div className="bg-primary rounded-lg border border-default overflow-hidden">
        <div className="divide-y divide-default">
          {filteredAnomalies.map((anomaly) => {
            const severityColor = getAnomalySeverityColor(anomaly.severity);
            const statusColor = getAnomalyStatusColor(anomaly.status);
            const isLoading = actionLoading === anomaly.id;

            return (
              <div key={anomaly.id} className="p-4 hover:bg-hover">
                <div className="flex items-start justify-between">
                  {/* Left side - Anomaly info */}
                  <div className="flex items-start space-x-4">
                    {/* Severity indicator */}
                    <div
                      className={clsx(
                        'w-12 h-12 rounded-lg flex items-center justify-center flex-shrink-0 border',
                        severityColorMap[severityColor] || 'bg-gray-100'
                      )}
                    >
                      <AlertOctagon className="h-6 w-6" />
                    </div>

                    <div className="flex-1">
                      <div className="flex items-center space-x-2 mb-1">
                        <span
                          className={clsx(
                            'px-2 py-0.5 text-xs font-medium rounded-full',
                            severityColorMap[severityColor] || 'bg-gray-100'
                          )}
                        >
                          {getAnomalySeverityLabel(anomaly.severity)}
                        </span>
                        <span
                          className={clsx(
                            'px-2 py-0.5 text-xs font-medium rounded-full',
                            statusColorMap[statusColor] || 'bg-gray-100'
                          )}
                        >
                          {getAnomalyStatusLabel(anomaly.status)}
                        </span>
                      </div>

                      <h3 className="text-sm font-medium text-primary">
                        {anomaly.anomalyType || 'Unknown Anomaly'}
                      </h3>

                      <div className="flex items-center space-x-4 mt-2 text-xs text-secondary">
                        {anomaly.deviceName && (
                          <div className="flex items-center space-x-1">
                            <Cpu className="h-3 w-3" />
                            <span>{anomaly.deviceName}</span>
                          </div>
                        )}
                        <div className="flex items-center space-x-1">
                          <Clock className="h-3 w-3" />
                          <span>{formatTimeAgo(anomaly.detectedAt)}</span>
                        </div>
                        <div>
                          Score: <span className="font-mono">{(anomaly.anomalyScore * 100).toFixed(1)}%</span>
                        </div>
                      </div>

                      {/* Affected variables */}
                      {anomaly.affectedVariables && anomaly.affectedVariables.length > 0 && (
                        <div className="mt-2 flex flex-wrap gap-1">
                          {anomaly.affectedVariables.map((variable, idx) => (
                            <span
                              key={idx}
                              className="px-2 py-0.5 bg-hover text-secondary text-xs rounded"
                            >
                              {variable}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Right side - Actions */}
                  <div className="flex items-center space-x-2">
                    {anomaly.status === 'NEW' && (
                      <button
                        onClick={() => handleAcknowledge(anomaly)}
                        disabled={isLoading}
                        className="px-3 py-1.5 text-xs font-medium text-blue-600 bg-blue-50 rounded-md hover:bg-blue-100 transition-colors disabled:opacity-50"
                        title="Acknowledge"
                      >
                        <CheckCircle className="h-4 w-4" />
                      </button>
                    )}

                    {anomaly.status === 'ACKNOWLEDGED' && (
                      <button
                        onClick={() => handleInvestigate(anomaly)}
                        disabled={isLoading}
                        className="px-3 py-1.5 text-xs font-medium text-yellow-600 bg-yellow-50 rounded-md hover:bg-yellow-100 transition-colors disabled:opacity-50"
                        title="Start Investigation"
                      >
                        <Eye className="h-4 w-4" />
                      </button>
                    )}

                    {(anomaly.status === 'INVESTIGATING' || anomaly.status === 'ACKNOWLEDGED') && (
                      <>
                        <button
                          onClick={() => handleResolve(anomaly)}
                          disabled={isLoading}
                          className="px-3 py-1.5 text-xs font-medium text-green-600 bg-green-50 rounded-md hover:bg-green-100 transition-colors disabled:opacity-50"
                          title="Resolve"
                        >
                          <CheckCircle className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => handleMarkFalsePositive(anomaly)}
                          disabled={isLoading}
                          className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-50 rounded-md hover:bg-gray-100 transition-colors disabled:opacity-50"
                          title="False Positive"
                        >
                          <XCircle className="h-4 w-4" />
                        </button>
                      </>
                    )}

                    {isLoading && (
                      <RefreshCw className="h-4 w-4 animate-spin text-secondary" />
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Empty State */}
        {filteredAnomalies.length === 0 && (
          <div className="text-center py-12">
            {anomalies.length === 0 ? (
              <div className="max-w-md mx-auto">
                <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <CheckCircle className="h-8 w-8 text-green-600" />
                </div>
                <h3 className="text-lg font-semibold text-primary mb-2">
                  No Anomalies Detected
                </h3>
                <p className="text-secondary">
                  Your systems are operating normally. ML models are continuously monitoring for anomalies.
                </p>
              </div>
            ) : (
              <div className="text-secondary">
                <Search className="h-8 w-8 mx-auto mb-2 opacity-50" />
                <p>No anomalies match your search criteria</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <div className="text-sm text-secondary">
            Page {page + 1} of {totalPages}
          </div>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1 border border-default rounded-md text-secondary hover:bg-hover disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <button
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1 border border-default rounded-md text-secondary hover:bg-hover disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default MLAnomalies;
