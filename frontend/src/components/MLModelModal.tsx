import { useState, useEffect } from 'react';
import { X, Brain, AlertCircle } from 'lucide-react';
import {
  mlModelsApi,
  MLModel,
  MLModelType,
  MLModelCreateRequest,
  MLModelUpdateRequest,
  DeviceScope,
  getModelTypeLabel,
} from '../services/mlService';

interface MLModelModalProps {
  model: MLModel | null;
  onClose: () => void;
}

export const MLModelModal = ({ model, onClose }: MLModelModalProps) => {
  const isEditing = !!model;

  const [formData, setFormData] = useState({
    name: '',
    modelType: 'ANOMALY_DETECTION' as MLModelType,
    algorithm: '',
    version: '1.0.0',
    deviceScope: 'ALL' as DeviceScope,
    deviceIds: [] as string[],
    deviceGroupId: '',
    inferenceSchedule: '0 */5 * * * *', // Every 5 minutes
    confidenceThreshold: 0.8,
    anomalyThreshold: 0.9,
    featureColumns: [] as string[],
    targetColumn: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (model) {
      setFormData({
        name: model.name,
        modelType: model.modelType,
        algorithm: model.algorithm || '',
        version: model.version,
        deviceScope: model.deviceScope,
        deviceIds: model.deviceIds || [],
        deviceGroupId: model.deviceGroupId || '',
        inferenceSchedule: model.inferenceSchedule || '0 */5 * * * *',
        confidenceThreshold: model.confidenceThreshold,
        anomalyThreshold: model.anomalyThreshold,
        featureColumns: model.featureColumns || [],
        targetColumn: model.targetColumn || '',
      });
    }
  }, [model]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (isEditing) {
        const updateRequest: MLModelUpdateRequest = {
          name: formData.name,
          hyperparameters: {},
          featureColumns: formData.featureColumns,
          targetColumn: formData.targetColumn,
          deviceScope: formData.deviceScope,
          deviceIds: formData.deviceScope === 'SPECIFIC' ? formData.deviceIds : undefined,
          deviceGroupId: formData.deviceScope === 'GROUP' ? formData.deviceGroupId : undefined,
          inferenceSchedule: formData.inferenceSchedule,
          confidenceThreshold: formData.confidenceThreshold,
          anomalyThreshold: formData.anomalyThreshold,
        };
        if (model) await mlModelsApi.update(model.id, updateRequest);
      } else {
        const createRequest: MLModelCreateRequest = {
          name: formData.name,
          modelType: formData.modelType,
          algorithm: formData.algorithm || undefined,
          version: formData.version,
          hyperparameters: {},
          featureColumns: formData.featureColumns,
          targetColumn: formData.targetColumn,
          deviceScope: formData.deviceScope,
          deviceIds: formData.deviceScope === 'SPECIFIC' ? formData.deviceIds : undefined,
          deviceGroupId: formData.deviceScope === 'GROUP' ? formData.deviceGroupId : undefined,
          inferenceSchedule: formData.inferenceSchedule,
          confidenceThreshold: formData.confidenceThreshold,
          anomalyThreshold: formData.anomalyThreshold,
        };
        await mlModelsApi.create(createRequest);
      }
      onClose();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to save model';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const modelTypes: { value: MLModelType; description: string }[] = [
    { value: 'ANOMALY_DETECTION', description: 'Detect unusual patterns in telemetry data' },
    { value: 'PREDICTIVE_MAINTENANCE', description: 'Predict equipment failures before they occur' },
    { value: 'ENERGY_FORECAST', description: 'Forecast energy consumption patterns' },
    { value: 'EQUIPMENT_RUL', description: 'Estimate remaining useful life of equipment' },
  ];

  const algorithms: Record<MLModelType, string[]> = {
    ANOMALY_DETECTION: ['Isolation Forest', 'One-Class SVM', 'Autoencoder', 'LSTM Autoencoder'],
    PREDICTIVE_MAINTENANCE: ['Random Forest', 'Gradient Boosting', 'LSTM', 'Transformer'],
    ENERGY_FORECAST: ['Prophet', 'ARIMA', 'LSTM', 'XGBoost'],
    EQUIPMENT_RUL: ['Survival Analysis', 'LSTM', 'Gradient Boosting', 'Deep Neural Network'],
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-primary rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-default">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-100 rounded-lg">
              <Brain className="h-6 w-6 text-blue-600" />
            </div>
            <div>
              <h2 className="text-xl font-semibold text-primary">
                {isEditing ? 'Edit Model' : 'Create New Model'}
              </h2>
              <p className="text-sm text-secondary">
                {isEditing ? 'Update model configuration' : 'Configure a new ML model for your data'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-secondary hover:text-primary p-1 rounded transition-colors"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto max-h-[calc(90vh-200px)]">
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start space-x-3">
              <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
              <div className="text-sm text-red-800">{error}</div>
            </div>
          )}

          <div className="space-y-6">
            {/* Basic Info */}
            <div>
              <h3 className="text-sm font-medium text-primary mb-4">Basic Information</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-secondary mb-1">
                    Model Name *
                  </label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-link focus:border-link"
                    placeholder="e.g., Temperature Anomaly Detector"
                    required
                  />
                </div>

                <div className="col-span-2">
                  <label className="block text-sm font-medium text-secondary mb-1">
                    Model Type *
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    {modelTypes.map((type) => (
                      <label
                        key={type.value}
                        className={`relative flex items-start p-3 border rounded-lg cursor-pointer transition-colors ${
                          formData.modelType === type.value
                            ? 'border-link bg-blue-50'
                            : 'border-default hover:border-muted'
                        } ${isEditing ? 'opacity-60 cursor-not-allowed' : ''}`}
                      >
                        <input
                          type="radio"
                          name="modelType"
                          value={type.value}
                          checked={formData.modelType === type.value}
                          onChange={(e) => setFormData({ ...formData, modelType: e.target.value as MLModelType, algorithm: '' })}
                          disabled={isEditing}
                          className="sr-only"
                        />
                        <div>
                          <div className="text-sm font-medium text-primary">
                            {getModelTypeLabel(type.value)}
                          </div>
                          <div className="text-xs text-secondary mt-0.5">
                            {type.description}
                          </div>
                        </div>
                      </label>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-secondary mb-1">
                    Algorithm
                  </label>
                  <select
                    value={formData.algorithm}
                    onChange={(e) => setFormData({ ...formData, algorithm: e.target.value })}
                    className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-link focus:border-link"
                  >
                    <option value="">Auto-select (Recommended)</option>
                    {algorithms[formData.modelType].map((algo) => (
                      <option key={algo} value={algo}>{algo}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-secondary mb-1">
                    Version
                  </label>
                  <input
                    type="text"
                    value={formData.version}
                    onChange={(e) => setFormData({ ...formData, version: e.target.value })}
                    className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-link focus:border-link"
                    placeholder="1.0.0"
                  />
                </div>
              </div>
            </div>

            {/* Device Scope */}
            <div>
              <h3 className="text-sm font-medium text-primary mb-4">Device Scope</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-secondary mb-1">
                    Apply to Devices
                  </label>
                  <select
                    value={formData.deviceScope}
                    onChange={(e) => setFormData({ ...formData, deviceScope: e.target.value as DeviceScope })}
                    className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-link focus:border-link"
                  >
                    <option value="ALL">All Devices</option>
                    <option value="SPECIFIC">Specific Devices</option>
                    <option value="GROUP">Device Group</option>
                  </select>
                </div>

                {formData.deviceScope === 'SPECIFIC' && (
                  <div>
                    <label className="block text-sm font-medium text-secondary mb-1">
                      Device IDs (comma-separated)
                    </label>
                    <input
                      type="text"
                      value={formData.deviceIds.join(', ')}
                      onChange={(e) => setFormData({
                        ...formData,
                        deviceIds: e.target.value.split(',').map(s => s.trim()).filter(Boolean)
                      })}
                      className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-link focus:border-link"
                      placeholder="device-001, device-002"
                    />
                  </div>
                )}

                {formData.deviceScope === 'GROUP' && (
                  <div>
                    <label className="block text-sm font-medium text-secondary mb-1">
                      Device Group ID
                    </label>
                    <input
                      type="text"
                      value={formData.deviceGroupId}
                      onChange={(e) => setFormData({ ...formData, deviceGroupId: e.target.value })}
                      className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-link focus:border-link"
                      placeholder="Enter device group ID"
                    />
                  </div>
                )}
              </div>
            </div>

            {/* Thresholds */}
            <div>
              <h3 className="text-sm font-medium text-primary mb-4">Thresholds</h3>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-secondary mb-1">
                    Confidence Threshold
                  </label>
                  <div className="flex items-center space-x-3">
                    <input
                      type="range"
                      min="0"
                      max="1"
                      step="0.05"
                      value={formData.confidenceThreshold}
                      onChange={(e) => setFormData({ ...formData, confidenceThreshold: parseFloat(e.target.value) })}
                      className="flex-1"
                    />
                    <span className="text-sm font-mono text-primary w-12 text-right">
                      {(formData.confidenceThreshold * 100).toFixed(0)}%
                    </span>
                  </div>
                  <p className="text-xs text-secondary mt-1">
                    Minimum confidence for predictions
                  </p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-secondary mb-1">
                    Anomaly Threshold
                  </label>
                  <div className="flex items-center space-x-3">
                    <input
                      type="range"
                      min="0"
                      max="1"
                      step="0.05"
                      value={formData.anomalyThreshold}
                      onChange={(e) => setFormData({ ...formData, anomalyThreshold: parseFloat(e.target.value) })}
                      className="flex-1"
                    />
                    <span className="text-sm font-mono text-primary w-12 text-right">
                      {(formData.anomalyThreshold * 100).toFixed(0)}%
                    </span>
                  </div>
                  <p className="text-xs text-secondary mt-1">
                    Score threshold for flagging anomalies
                  </p>
                </div>
              </div>
            </div>

            {/* Schedule */}
            <div>
              <h3 className="text-sm font-medium text-primary mb-4">Inference Schedule</h3>
              <div>
                <label className="block text-sm font-medium text-secondary mb-1">
                  Cron Expression
                </label>
                <input
                  type="text"
                  value={formData.inferenceSchedule}
                  onChange={(e) => setFormData({ ...formData, inferenceSchedule: e.target.value })}
                  className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-link focus:border-link font-mono"
                  placeholder="0 */5 * * * *"
                />
                <p className="text-xs text-secondary mt-1">
                  How often to run inference (default: every 5 minutes)
                </p>
              </div>
            </div>
          </div>
        </form>

        {/* Footer */}
        <div className="flex items-center justify-end space-x-3 p-6 border-t border-default bg-hover">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 border border-default rounded-lg text-secondary hover:bg-primary transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading || !formData.name}
            className="px-4 py-2 bg-link text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
          >
            {loading ? (
              <>
                <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                <span>Saving...</span>
              </>
            ) : (
              <span>{isEditing ? 'Update Model' : 'Create Model'}</span>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
