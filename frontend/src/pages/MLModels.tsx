import { useEffect, useState, useCallback } from 'react';
import toast from 'react-hot-toast';
import {
  Plus,
  Search,
  Brain,
  Play,
  Rocket,
  Archive,
  Trash2,
  Edit,
  RefreshCw,
  Filter,
  ChevronDown,
} from 'lucide-react';
import { clsx } from 'clsx';
import {
  mlModelsApi,
  MLModel,
  MLModelType,
  MLModelStatus,
  TrainingJob,
  getModelTypeLabel,
  getModelStatusLabel,
  getModelStatusColor,
  PageResponse,
} from '../services/mlService';
import { MLModelModal } from '../components/MLModelModal';
import { TrainingProgressModal } from '../components/ml/TrainingProgressModal';
import { formatTimeAgo } from '../utils/timeUtils';

export const MLModels = () => {
  const [models, setModels] = useState<MLModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState<MLModelType | ''>('');
  const [statusFilter, setStatusFilter] = useState<MLModelStatus | ''>('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedModel, setSelectedModel] = useState<MLModel | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  // Training progress modal state
  const [trainingJobId, setTrainingJobId] = useState<string | null>(null);
  const [trainingModelName, setTrainingModelName] = useState<string>('');
  const [isTrainingModalOpen, setIsTrainingModalOpen] = useState(false);

  // Memoized fetch function to avoid recreating on every render
  const fetchModels = useCallback(async () => {
    try {
      setLoading(true);
      const response: PageResponse<MLModel> = await mlModelsApi.list({
        page,
        size: 10,
        type: typeFilter || undefined,
        status: statusFilter || undefined,
      });
      setModels(response.content);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Failed to fetch ML models:', error);
    } finally {
      setLoading(false);
    }
  }, [page, typeFilter, statusFilter]);

  // Load models when filters change
  useEffect(() => {
    fetchModels();
  }, [fetchModels]);

  const handleCreate = () => {
    setSelectedModel(null);
    setIsModalOpen(true);
  };

  const handleEdit = (model: MLModel) => {
    setSelectedModel(model);
    setIsModalOpen(true);
  };

  const handleTrain = async (model: MLModel) => {
    if (actionLoading) return;
    try {
      setActionLoading(model.id);
      const response = await mlModelsApi.train(model.id);

      // Open training progress modal
      setTrainingJobId(response.trainingJob.id);
      setTrainingModelName(model.name);
      setIsTrainingModalOpen(true);

      toast.success(`Training started for "${model.name}"`);
      await fetchModels();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to start training';
      toast.error(message);
      console.error('Failed to start training:', error);
    } finally {
      setActionLoading(null);
    }
  };

  // Memoized handler for training completion - refreshes model list
  const handleTrainingComplete = useCallback((_job: TrainingJob) => {
    // Refresh models list when training completes
    // Note: We don't need to use the job parameter currently,
    // but it's available if needed for future enhancements
    fetchModels();
  }, [fetchModels]);

  const handleTrainingModalClose = useCallback(() => {
    setIsTrainingModalOpen(false);
    setTrainingJobId(null);
    setTrainingModelName('');
    // Refresh models to show updated status
    fetchModels();
  }, [fetchModels]);

  const handleDeploy = async (model: MLModel) => {
    if (actionLoading) return;
    try {
      setActionLoading(model.id);
      await mlModelsApi.deploy(model.id);
      toast.success(`Model "${model.name}" deployed successfully`);
      await fetchModels();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to deploy model';
      toast.error(message);
      console.error('Failed to deploy model:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleArchive = async (model: MLModel) => {
    if (actionLoading) return;
    if (!window.confirm(`Are you sure you want to archive "${model.name}"?`)) {
      return;
    }
    try {
      setActionLoading(model.id);
      await mlModelsApi.archive(model.id);
      toast.success(`Model "${model.name}" archived`);
      await fetchModels();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to archive model';
      toast.error(message);
      console.error('Failed to archive model:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleDelete = async (model: MLModel) => {
    if (actionLoading) return;
    if (!window.confirm(`Are you sure you want to delete "${model.name}"? This action cannot be undone.`)) {
      return;
    }
    try {
      setActionLoading(model.id);
      await mlModelsApi.delete(model.id);
      toast.success(`Model "${model.name}" deleted`);
      await fetchModels();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to delete model';
      toast.error(message);
      console.error('Failed to delete model:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedModel(null);
    fetchModels();
  };

  const filteredModels = models.filter(model =>
    model.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const statusColorMap: Record<string, string> = {
    gray: 'bg-gray-100 text-gray-800',
    blue: 'bg-blue-100 text-blue-800',
    green: 'bg-green-100 text-green-800',
    emerald: 'bg-emerald-100 text-emerald-800',
    red: 'bg-red-100 text-red-800',
    slate: 'bg-slate-100 text-slate-800',
  };

  const typeOptions: MLModelType[] = ['ANOMALY_DETECTION', 'PREDICTIVE_MAINTENANCE', 'ENERGY_FORECAST', 'EQUIPMENT_RUL'];
  const statusOptions: MLModelStatus[] = ['DRAFT', 'TRAINING', 'TRAINED', 'DEPLOYED', 'FAILED', 'ARCHIVED'];

  if (loading && models.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-secondary">Loading ML models...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-primary">ML Models</h1>
          <p className="text-secondary mt-1">
            Manage machine learning models for anomaly detection, predictive maintenance, and forecasting
          </p>
        </div>
        <button
          onClick={handleCreate}
          className="flex items-center space-x-2 bg-link text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="h-4 w-4" />
          <span>New Model</span>
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-4">
        {/* Search */}
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-secondary" />
          <input
            type="text"
            placeholder="Search models..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-link focus:border-link"
          />
        </div>

        {/* Type Filter */}
        <div className="relative">
          <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-secondary" />
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value as MLModelType | '')}
            className="pl-10 pr-8 py-2 border border-default rounded-lg bg-primary text-primary appearance-none cursor-pointer focus:ring-2 focus:ring-link focus:border-link"
          >
            <option value="">All Types</option>
            {typeOptions.map(type => (
              <option key={type} value={type}>{getModelTypeLabel(type)}</option>
            ))}
          </select>
          <ChevronDown className="absolute right-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-secondary pointer-events-none" />
        </div>

        {/* Status Filter */}
        <div className="relative">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as MLModelStatus | '')}
            className="pl-4 pr-8 py-2 border border-default rounded-lg bg-primary text-primary appearance-none cursor-pointer focus:ring-2 focus:ring-link focus:border-link"
          >
            <option value="">All Statuses</option>
            {statusOptions.map(status => (
              <option key={status} value={status}>{getModelStatusLabel(status)}</option>
            ))}
          </select>
          <ChevronDown className="absolute right-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-secondary pointer-events-none" />
        </div>

        {/* Refresh */}
        <button
          onClick={() => fetchModels()}
          disabled={loading}
          className="flex items-center space-x-2 px-4 py-2 border border-default rounded-lg bg-primary text-secondary hover:bg-hover transition-colors disabled:opacity-50"
        >
          <RefreshCw className={clsx('h-4 w-4', loading && 'animate-spin')} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Models Table */}
      <div className="bg-primary rounded-lg border border-default overflow-hidden">
        <table className="w-full">
          <thead className="bg-hover">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                Model
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                Type
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                Version
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                Last Trained
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-secondary uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-primary divide-y divide-default">
            {filteredModels.map((model) => {
              const statusColor = getModelStatusColor(model.status);
              const isLoading = actionLoading === model.id;

              return (
                <tr key={model.id} className="hover:bg-hover">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="flex-shrink-0 h-10 w-10 bg-blue-100 rounded-lg flex items-center justify-center">
                        <Brain className="h-5 w-5 text-blue-600" />
                      </div>
                      <div className="ml-4">
                        <div className="text-sm font-medium text-primary">{model.name}</div>
                        <div className="text-sm text-secondary">{model.algorithm || 'Auto-selected'}</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className="text-sm text-primary">{getModelTypeLabel(model.modelType)}</span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className={clsx(
                        'px-2 py-1 text-xs font-medium rounded-full',
                        statusColorMap[statusColor] || 'bg-gray-100 text-gray-800'
                      )}
                    >
                      {getModelStatusLabel(model.status)}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-primary">
                    v{model.version}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary">
                    {model.trainedAt ? formatTimeAgo(model.trainedAt) : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="flex items-center justify-end space-x-1">
                      {/* Train button - show for DRAFT or TRAINED models */}
                      {(model.status === 'DRAFT' || model.status === 'TRAINED') && (
                        <button
                          onClick={() => handleTrain(model)}
                          disabled={isLoading}
                          className="text-blue-600 hover:text-blue-900 p-1.5 rounded hover:bg-blue-50 transition-colors disabled:opacity-50"
                          title="Train Model"
                        >
                          <Play className="h-4 w-4" />
                        </button>
                      )}

                      {/* Deploy button - show for TRAINED models */}
                      {model.status === 'TRAINED' && (
                        <button
                          onClick={() => handleDeploy(model)}
                          disabled={isLoading}
                          className="text-green-600 hover:text-green-900 p-1.5 rounded hover:bg-green-50 transition-colors disabled:opacity-50"
                          title="Deploy Model"
                        >
                          <Rocket className="h-4 w-4" />
                        </button>
                      )}

                      {/* Edit button */}
                      <button
                        onClick={() => handleEdit(model)}
                        disabled={isLoading}
                        className="text-secondary hover:text-primary p-1.5 rounded hover:bg-hover transition-colors disabled:opacity-50"
                        title="Edit Model"
                      >
                        <Edit className="h-4 w-4" />
                      </button>

                      {/* Archive button - show for DEPLOYED models */}
                      {model.status === 'DEPLOYED' && (
                        <button
                          onClick={() => handleArchive(model)}
                          disabled={isLoading}
                          className="text-orange-600 hover:text-orange-900 p-1.5 rounded hover:bg-orange-50 transition-colors disabled:opacity-50"
                          title="Archive Model"
                        >
                          <Archive className="h-4 w-4" />
                        </button>
                      )}

                      {/* Delete button - show for DRAFT, FAILED, or ARCHIVED models */}
                      {['DRAFT', 'FAILED', 'ARCHIVED'].includes(model.status) && (
                        <button
                          onClick={() => handleDelete(model)}
                          disabled={isLoading}
                          className="text-danger hover:text-red-900 p-1.5 rounded hover:bg-red-50 transition-colors disabled:opacity-50"
                          title="Delete Model"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}

                      {/* Loading indicator */}
                      {isLoading && (
                        <RefreshCw className="h-4 w-4 animate-spin text-secondary ml-1" />
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>

        {/* Empty State */}
        {filteredModels.length === 0 && (
          <div className="text-center py-12">
            {models.length === 0 ? (
              <div className="max-w-md mx-auto">
                <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <Brain className="h-8 w-8 text-blue-600" />
                </div>
                <h3 className="text-lg font-semibold text-primary mb-2">
                  No ML Models Yet
                </h3>
                <p className="text-secondary mb-6">
                  Create your first machine learning model to start detecting anomalies, predicting maintenance needs, or forecasting energy consumption.
                </p>
                <button
                  onClick={handleCreate}
                  className="inline-flex items-center gap-2 px-5 py-2.5 bg-link text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
                >
                  <Plus className="h-4 w-4" />
                  Create Your First Model
                </button>
              </div>
            ) : (
              <div className="text-secondary">
                <Search className="h-8 w-8 mx-auto mb-2 opacity-50" />
                <p>No models match your search criteria</p>
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

      {/* Model Modal */}
      {isModalOpen && (
        <MLModelModal
          model={selectedModel}
          onClose={handleModalClose}
        />
      )}

      {/* Training Progress Modal */}
      {trainingJobId && (
        <TrainingProgressModal
          jobId={trainingJobId}
          modelName={trainingModelName}
          isOpen={isTrainingModalOpen}
          onClose={handleTrainingModalClose}
          onComplete={handleTrainingComplete}
        />
      )}
    </div>
  );
};

export default MLModels;
