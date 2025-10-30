import React, { useState, useEffect } from 'react';
import { X, CheckCircle, AlertCircle, AlertTriangle, Loader } from 'lucide-react';
import toast from 'react-hot-toast';
import dataPluginsService, {
  DataPlugin,
  PluginExecution,
  ExecutionStatus,
} from '../../services/dataPluginsService';

interface ExecutionHistoryDialogProps {
  open: boolean;
  plugin: DataPlugin;
  onClose: () => void;
}

const ExecutionHistoryDialog: React.FC<ExecutionHistoryDialogProps> = ({
  open,
  plugin,
  onClose,
}) => {
  const [executions, setExecutions] = useState<PluginExecution[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    if (open) {
      loadExecutions();
    }
  }, [open, page]);

  const loadExecutions = async () => {
    try {
      setLoading(true);
      const response = await dataPluginsService.getExecutions(plugin.id, page, 20);
      setExecutions(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (error) {
      console.error('Failed to load execution history:', error);
      toast.error('Failed to load execution history');
    } finally {
      setLoading(false);
    }
  };

  const getStatusIcon = (status: ExecutionStatus) => {
    switch (status) {
      case ExecutionStatus.SUCCESS:
        return <CheckCircle className="w-4 h-4 text-green-600" />;
      case ExecutionStatus.FAILED:
        return <AlertCircle className="w-4 h-4 text-red-600" />;
      case ExecutionStatus.PARTIAL:
        return <AlertTriangle className="w-4 h-4 text-yellow-600" />;
    }
  };

  const getStatusColor = (status: ExecutionStatus): string => {
    switch (status) {
      case ExecutionStatus.SUCCESS:
        return 'bg-green-100 text-green-800';
      case ExecutionStatus.FAILED:
        return 'bg-red-100 text-red-800';
      case ExecutionStatus.PARTIAL:
        return 'bg-yellow-100 text-yellow-800';
    }
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-5xl w-full max-h-[90vh] flex flex-col">
        <div className="bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center">
          <h2 className="text-xl font-bold text-gray-900">
            Execution History: {plugin.name}
          </h2>
          <button onClick={onClose} className="p-1 hover:bg-gray-100 rounded">
            <X className="w-6 h-6 text-gray-600" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="flex justify-center items-center py-12">
              <Loader className="w-8 h-8 text-blue-600 animate-spin" />
            </div>
          ) : executions.length === 0 ? (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-blue-800">
              No execution history yet. This plugin has not been executed.
            </div>
          ) : (
            <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Executed At
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Records
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Duration
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Error
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {executions.map((execution) => (
                    <tr key={execution.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                        {formatTimestamp(execution.executedAt)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-1 text-xs font-medium rounded ${getStatusColor(
                            execution.status
                          )}`}
                        >
                          {getStatusIcon(execution.status)}
                          {execution.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-700">
                        {execution.recordsProcessed}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-700">
                        {execution.durationMs !== undefined && execution.durationMs !== null
                          ? `${execution.durationMs}ms`
                          : '-'}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-700 max-w-md truncate">
                        {execution.errorMessage || '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {!loading && totalPages > 1 && (
          <div className="border-t border-gray-200 px-6 py-4 flex items-center justify-between bg-gray-50">
            <span className="text-sm text-gray-700">
              Showing {page * 20 + 1} to {Math.min((page + 1) * 20, totalElements)} of{' '}
              {totalElements} executions
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="px-3 py-1 text-sm border border-gray-300 rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <button
                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1 text-sm border border-gray-300 rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          </div>
        )}

        <div className="border-t border-gray-200 px-6 py-4 flex justify-end bg-gray-50">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-700 border border-gray-300 rounded-md hover:bg-gray-50"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default ExecutionHistoryDialog;
