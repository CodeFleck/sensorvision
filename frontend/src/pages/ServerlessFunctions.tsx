import React, { useState, useEffect } from 'react';
import {
  Plus,
  MoreVertical,
  Play,
  Edit,
  Trash2,
  History,
  CheckCircle,
  XCircle
} from 'lucide-react';
import toast from 'react-hot-toast';
import serverlessFunctionsService, {
  ServerlessFunction,
  FunctionRuntime
} from '../services/serverlessFunctionsService';
import FunctionEditor from '../components/serverless/FunctionEditor';
import FunctionInvokeModal from '../components/serverless/FunctionInvokeModal';
import ExecutionHistory from '../components/serverless/ExecutionHistory';

const ServerlessFunctions: React.FC = () => {
  const [functions, setFunctions] = useState<ServerlessFunction[]>([]);
  const [loading, setLoading] = useState(true);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingFunction, setEditingFunction] = useState<ServerlessFunction | null>(null);
  const [invokeModalOpen, setInvokeModalOpen] = useState(false);
  const [invokingFunction, setInvokingFunction] = useState<ServerlessFunction | null>(null);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [historyFunction, setHistoryFunction] = useState<ServerlessFunction | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingFunction, setDeletingFunction] = useState<ServerlessFunction | null>(null);
  const [menuOpen, setMenuOpen] = useState<number | null>(null);

  useEffect(() => {
    loadFunctions();
  }, []);

  const loadFunctions = async () => {
    try {
      setLoading(true);
      const data = await serverlessFunctionsService.getAllFunctions();
      setFunctions(data);
    } catch (error) {
      console.error('Failed to load functions:', error);
      toast.error('Failed to load functions');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateFunction = () => {
    setEditingFunction(null);
    setEditorOpen(true);
  };

  const handleEditFunction = (func: ServerlessFunction) => {
    setEditingFunction(func);
    setEditorOpen(true);
    setMenuOpen(null);
  };

  const handleDeleteClick = (func: ServerlessFunction) => {
    setDeletingFunction(func);
    setDeleteDialogOpen(true);
    setMenuOpen(null);
  };

  const confirmDelete = async () => {
    if (!deletingFunction) return;

    try {
      await serverlessFunctionsService.deleteFunction(deletingFunction.id);
      toast.success(`Function "${deletingFunction.name}" deleted successfully`);
      loadFunctions();
    } catch (error) {
      console.error('Failed to delete function:', error);
      toast.error('Failed to delete function');
    } finally {
      setDeleteDialogOpen(false);
      setDeletingFunction(null);
    }
  };

  const handleInvokeFunction = (func: ServerlessFunction) => {
    setInvokingFunction(func);
    setInvokeModalOpen(true);
    setMenuOpen(null);
  };

  const handleViewHistory = (func: ServerlessFunction) => {
    setHistoryFunction(func);
    setHistoryOpen(true);
    setMenuOpen(null);
  };

  const handleSaveFunction = async () => {
    setEditorOpen(false);
    await loadFunctions();
  };

  const getRuntimeLabel = (runtime: FunctionRuntime): string => {
    switch (runtime) {
      case FunctionRuntime.PYTHON_3_11:
        return 'Python 3.11';
      case FunctionRuntime.NODEJS_18:
        return 'Node.js 18';
      default:
        return runtime;
    }
  };

  const getRuntimeColor = (runtime: FunctionRuntime): string => {
    switch (runtime) {
      case FunctionRuntime.PYTHON_3_11:
        return 'bg-blue-100 text-blue-800';
      case FunctionRuntime.NODEJS_18:
        return 'bg-green-100 text-green-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return (
      <div className="p-6">
        <div className="text-gray-600">Loading functions...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Serverless Functions</h1>
        <button
          onClick={handleCreateFunction}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="w-5 h-5" />
          Create Function
        </button>
      </div>

      {functions.length === 0 ? (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-blue-800">
          No serverless functions yet. Click &quot;Create Function&quot; to get started.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {functions.map((func) => (
            <div key={func.id} className="bg-white rounded-lg shadow border border-gray-200 p-4">
              <div className="flex justify-between items-start mb-3">
                <div className="flex-1">
                  <h3 className="text-lg font-semibold text-gray-900">{func.name}</h3>
                  {func.description && (
                    <p className="text-sm text-gray-600 mt-1">{func.description}</p>
                  )}
                </div>
                <div className="relative">
                  <button
                    onClick={() => setMenuOpen(menuOpen === func.id ? null : func.id)}
                    className="p-1 hover:bg-gray-100 rounded"
                  >
                    <MoreVertical className="w-5 h-5 text-gray-600" />
                  </button>
                  {menuOpen === func.id && (
                    <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 z-10">
                      <button
                        onClick={() => handleEditFunction(func)}
                        className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      >
                        <Edit className="w-4 h-4" />
                        Edit
                      </button>
                      <button
                        onClick={() => handleInvokeFunction(func)}
                        className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      >
                        <Play className="w-4 h-4" />
                        Invoke
                      </button>
                      <button
                        onClick={() => handleViewHistory(func)}
                        className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      >
                        <History className="w-4 h-4" />
                        View History
                      </button>
                      <button
                        onClick={() => handleDeleteClick(func)}
                        className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                      >
                        <Trash2 className="w-4 h-4" />
                        Delete
                      </button>
                    </div>
                  )}
                </div>
              </div>

              <div className="flex flex-wrap gap-2 mb-3">
                <span className={`px-2 py-1 text-xs font-medium rounded ${getRuntimeColor(func.runtime)}`}>
                  {getRuntimeLabel(func.runtime)}
                </span>
                <span className={`px-2 py-1 text-xs font-medium rounded flex items-center gap-1 ${
                  func.enabled
                    ? 'bg-green-100 text-green-800'
                    : 'bg-gray-100 text-gray-800'
                }`}>
                  {func.enabled ? <CheckCircle className="w-3 h-3" /> : <XCircle className="w-3 h-3" />}
                  {func.enabled ? 'Enabled' : 'Disabled'}
                </span>
              </div>

              <div className="text-xs text-gray-600 space-y-1">
                <div>Handler: <span className="font-mono font-semibold">{func.handler}</span></div>
                <div>Timeout: {func.timeoutSeconds}s • Memory: {func.memoryLimitMb}MB</div>
              </div>

              <div className="mt-4 flex gap-2">
                <button
                  onClick={() => handleInvokeFunction(func)}
                  disabled={!func.enabled}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2 text-sm border border-blue-600 text-blue-600 rounded hover:bg-blue-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Play className="w-4 h-4" />
                  Invoke
                </button>
                <button
                  onClick={() => handleViewHistory(func)}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2 text-sm border border-gray-300 text-gray-700 rounded hover:bg-gray-50"
                >
                  <History className="w-4 h-4" />
                  History
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {editorOpen && (
        <FunctionEditor
          open={editorOpen}
          function={editingFunction}
          onClose={() => setEditorOpen(false)}
          onSave={handleSaveFunction}
        />
      )}

      {invokeModalOpen && invokingFunction && (
        <FunctionInvokeModal
          open={invokeModalOpen}
          function={invokingFunction}
          onClose={() => setInvokeModalOpen(false)}
        />
      )}

      {historyOpen && historyFunction && (
        <ExecutionHistory
          open={historyOpen}
          function={historyFunction}
          onClose={() => setHistoryOpen(false)}
        />
      )}

      {deleteDialogOpen && deletingFunction && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h2 className="text-xl font-bold mb-4">Delete Function</h2>
            <p className="text-gray-700 mb-6">
              Are you sure you want to delete the function &quot;{deletingFunction.name}&quot;?
              This action cannot be undone and will also delete all execution history and triggers.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setDeleteDialogOpen(false)}
                className="px-4 py-2 text-gray-700 border border-gray-300 rounded hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={confirmDelete}
                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ServerlessFunctions;
