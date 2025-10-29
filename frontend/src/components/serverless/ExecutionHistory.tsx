import React, { useState, useEffect } from 'react';
import {
  X,
  ChevronDown,
  ChevronRight,
  CheckCircle,
  AlertCircle,
  Clock,
  Timer,
  Loader
} from 'lucide-react';
import toast from 'react-hot-toast';
import serverlessFunctionsService, {
  ServerlessFunction,
  FunctionExecution,
  FunctionExecutionStatus
} from '../../services/serverlessFunctionsService';

interface ExecutionHistoryProps {
  open: boolean;
  function: ServerlessFunction;
  onClose: () => void;
}

interface ExecutionRowProps {
  execution: FunctionExecution;
}

const ExecutionRow: React.FC<ExecutionRowProps> = ({ execution }) => {
  const [expanded, setExpanded] = useState(false);

  const getStatusIcon = (status: FunctionExecutionStatus) => {
    switch (status) {
      case FunctionExecutionStatus.SUCCESS:
        return <CheckCircle className="w-4 h-4" />;
      case FunctionExecutionStatus.FAILED:
        return <AlertCircle className="w-4 h-4" />;
      case FunctionExecutionStatus.TIMEOUT:
        return <Timer className="w-4 h-4" />;
      case FunctionExecutionStatus.PENDING:
      case FunctionExecutionStatus.RUNNING:
        return <Clock className="w-4 h-4" />;
      default:
        return null;
    }
  };

  const getStatusColor = (status: FunctionExecutionStatus): string => {
    switch (status) {
      case FunctionExecutionStatus.SUCCESS:
        return 'bg-green-100 text-green-800';
      case FunctionExecutionStatus.FAILED:
        return 'bg-red-100 text-red-800';
      case FunctionExecutionStatus.TIMEOUT:
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  const formatJson = (obj: any): string => {
    try {
      return JSON.stringify(obj, null, 2);
    } catch (e) {
      return String(obj);
    }
  };

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden">
      <div
        className="flex items-center gap-4 p-4 cursor-pointer hover:bg-gray-50"
        onClick={() => setExpanded(!expanded)}
      >
        <button className="p-1">
          {expanded ? (
            <ChevronDown className="w-4 h-4 text-gray-600" />
          ) : (
            <ChevronRight className="w-4 h-4 text-gray-600" />
          )}
        </button>

        <span className={`flex items-center gap-1 px-2 py-1 text-xs font-medium rounded ${getStatusColor(execution.status)}`}>
          {getStatusIcon(execution.status)}
          {execution.status}
        </span>

        <span className="text-sm text-gray-700 flex-1">
          {formatTimestamp(execution.startedAt)}
        </span>

        <span className="text-sm text-gray-600">
          {execution.durationMs !== undefined && execution.durationMs !== null
            ? `${execution.durationMs}ms`
            : '-'}
        </span>

        <span className="text-sm text-gray-600">
          {execution.memoryUsedMb !== undefined && execution.memoryUsedMb !== null
            ? `${execution.memoryUsedMb}MB`
            : '-'}
        </span>
      </div>

      {expanded && (
        <div className="border-t border-gray-200 bg-gray-50 p-4 space-y-3">
          {execution.inputData && (
            <div>
              <h4 className="text-xs font-medium text-gray-700 mb-1">Input Data</h4>
              <div className="border border-gray-200 bg-white rounded p-2">
                <pre className="text-xs font-mono overflow-x-auto">
                  {formatJson(execution.inputData)}
                </pre>
              </div>
            </div>
          )}

          {execution.outputData && (
            <div>
              <h4 className="text-xs font-medium text-gray-700 mb-1">Output Data</h4>
              <div className="border border-gray-200 bg-white rounded p-2">
                <pre className="text-xs font-mono overflow-x-auto">
                  {formatJson(execution.outputData)}
                </pre>
              </div>
            </div>
          )}

          {execution.errorMessage && (
            <div>
              <h4 className="text-xs font-medium text-red-800 mb-1">Error Message</h4>
              <div className="bg-red-50 border border-red-200 rounded p-2">
                <pre className="text-xs text-red-700 whitespace-pre-wrap">
                  {execution.errorMessage}
                </pre>
              </div>
            </div>
          )}

          {execution.errorStack && (
            <div>
              <h4 className="text-xs font-medium text-gray-700 mb-1">Stack Trace</h4>
              <div className="border border-gray-200 bg-gray-100 rounded p-2">
                <pre className="text-xs font-mono whitespace-pre-wrap overflow-x-auto">
                  {execution.errorStack}
                </pre>
              </div>
            </div>
          )}

          {execution.completedAt && (
            <div className="text-xs text-gray-600">
              Completed at: {formatTimestamp(execution.completedAt)}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

const ExecutionHistory: React.FC<ExecutionHistoryProps> = ({
  open,
  function: func,
  onClose
}) => {
  const [executions, setExecutions] = useState<FunctionExecution[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (open) {
      loadExecutions();
    }
  }, [open, page, rowsPerPage]);

  const loadExecutions = async () => {
    try {
      setLoading(true);
      const response = await serverlessFunctionsService.getExecutionHistory(
        func.id,
        page,
        rowsPerPage
      );
      setExecutions(response.content);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Failed to load execution history:', error);
      toast.error('Failed to load execution history');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setPage(0);
    onClose();
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-5xl w-full max-h-[90vh] flex flex-col">
        <div className="bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center">
          <h2 className="text-xl font-bold text-gray-900">
            Execution History: {func.name}
          </h2>
          <button
            onClick={handleClose}
            className="p-1 hover:bg-gray-100 rounded"
          >
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
              No execution history yet. Invoke the function to see results here.
            </div>
          ) : (
            <div className="space-y-2">
              {executions.map((execution) => (
                <ExecutionRow key={execution.id} execution={execution} />
              ))}
            </div>
          )}
        </div>

        {!loading && totalElements > 0 && (
          <div className="border-t border-gray-200 px-6 py-4 flex items-center justify-between bg-gray-50">
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-700">
                Rows per page:
              </span>
              <select
                value={rowsPerPage}
                onChange={(e) => {
                  setRowsPerPage(Number(e.target.value));
                  setPage(0);
                }}
                className="px-2 py-1 text-sm border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500"
              >
                <option value={5}>5</option>
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
              </select>
            </div>

            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-700">
                {page * rowsPerPage + 1}-{Math.min((page + 1) * rowsPerPage, totalElements)} of {totalElements}
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
          </div>
        )}

        <div className="border-t border-gray-200 px-6 py-4 flex justify-end bg-gray-50">
          <button
            onClick={handleClose}
            className="px-4 py-2 text-gray-700 border border-gray-300 rounded-md hover:bg-gray-50"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default ExecutionHistory;
