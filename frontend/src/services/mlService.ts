/**
 * ML Service - API client for Machine Learning features
 * Handles ML models, anomalies, predictions, and training jobs
 */

const API_BASE = '/api/v1';

async function request<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const token = localStorage.getItem('accessToken');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options?.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    headers,
    ...options,
  });

  if (!response.ok) {
    if (response.status === 401) {
      localStorage.removeItem('accessToken');
      window.location.href = '/login';
      throw new Error('Session expired. Please login again.');
    }

    try {
      const errorData = await response.json();
      throw new Error(errorData.message || errorData.detail || response.statusText);
    } catch (parseError) {
      if (parseError instanceof Error && parseError.message !== 'Session expired. Please login again.') {
        throw parseError;
      }
      throw new Error(`API Error: ${response.status} ${response.statusText}`);
    }
  }

  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T;
  }

  return response.json();
}

// ============================================================================
// Types
// ============================================================================

export type MLModelType =
  | 'ANOMALY_DETECTION'
  | 'PREDICTIVE_MAINTENANCE'
  | 'ENERGY_FORECAST'
  | 'EQUIPMENT_RUL';

export type MLModelStatus =
  | 'DRAFT'
  | 'TRAINING'
  | 'TRAINED'
  | 'DEPLOYED'
  | 'FAILED'
  | 'ARCHIVED';

export type MLAnomalyStatus =
  | 'NEW'
  | 'ACKNOWLEDGED'
  | 'INVESTIGATING'
  | 'RESOLVED'
  | 'FALSE_POSITIVE';

export type TrainingJobStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export type TrainingJobType =
  | 'INITIAL_TRAINING'
  | 'RETRAINING'
  | 'HYPERPARAMETER_TUNING';

export type MLAnomalySeverity =
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH'
  | 'CRITICAL';

export type DeviceScope =
  | 'ALL'
  | 'SPECIFIC'
  | 'GROUP';

export interface MLModel {
  id: string;
  organizationId: number;
  name: string;
  modelType: MLModelType;
  version: string;
  algorithm: string;
  hyperparameters: Record<string, unknown>;
  featureColumns: string[];
  targetColumn: string;
  status: MLModelStatus;
  modelPath?: string;
  modelSizeBytes?: number;
  trainingMetrics?: Record<string, unknown>;
  validationMetrics?: Record<string, unknown>;
  deviceScope: DeviceScope;
  deviceIds?: string[];
  deviceGroupId?: string;
  inferenceSchedule?: string;
  lastInferenceAt?: string;
  nextInferenceAt?: string;
  confidenceThreshold: number;
  anomalyThreshold: number;
  createdBy?: number;
  trainedBy?: number;
  trainedAt?: string;
  deployedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface MLModelCreateRequest {
  name: string;
  modelType: MLModelType;
  algorithm?: string;
  version?: string;
  hyperparameters?: Record<string, unknown>;
  featureColumns?: string[];
  targetColumn?: string;
  deviceScope: DeviceScope;
  deviceIds?: string[];
  deviceGroupId?: string;
  inferenceSchedule?: string;
  confidenceThreshold?: number;
  anomalyThreshold?: number;
}

export interface MLModelUpdateRequest {
  name?: string;
  hyperparameters?: Record<string, unknown>;
  featureColumns?: string[];
  targetColumn?: string;
  deviceScope?: DeviceScope;
  deviceIds?: string[];
  deviceGroupId?: string;
  inferenceSchedule?: string;
  confidenceThreshold?: number;
  anomalyThreshold?: number;
}

export interface MLAnomaly {
  id: string;
  predictionId?: string;
  deviceId?: string;
  deviceName?: string;
  organizationId?: number;
  anomalyScore: number;
  severity: MLAnomalySeverity;
  status: MLAnomalyStatus;
  anomalyType?: string;
  affectedVariables?: string[];
  expectedValues?: Record<string, unknown>;
  actualValues?: Record<string, unknown>;
  contextWindow?: unknown;
  detectedAt: string;
  acknowledgedBy?: number;
  acknowledgedAt?: string;
  resolvedBy?: number;
  resolvedAt?: string;
  resolutionNote?: string;
  globalAlertId?: string;
  createdAt: string;
  predictionType?: string;
  predictionConfidence?: number;
}

export interface MLAnomalyResolveRequest {
  resolutionNote?: string;
}

export interface TrainingJob {
  id: string;
  modelId: string;
  organizationId: number;
  jobType: TrainingJobType;
  status: TrainingJobStatus;
  trainingConfig: Record<string, unknown>;
  trainingDataStart?: string;
  trainingDataEnd?: string;
  recordCount?: number;
  deviceCount?: number;
  progressPercent: number;
  currentStep?: string;
  resultMetrics?: Record<string, unknown>;
  errorMessage?: string;
  startedAt?: string;
  completedAt?: string;
  durationSeconds?: number;
  triggeredBy?: string;
  createdAt: string;
}

export interface TrainingStartResponse {
  model: MLModel;
  trainingJob: TrainingJob;
}

export interface ModelStatsResponse {
  deployedCount: number;
}

export interface AnomalyStatsResponse {
  newCount: number;
  bySeverity: Record<MLAnomalySeverity, number>;
  periodHours: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// ============================================================================
// ML Models API
// ============================================================================

export const mlModelsApi = {
  /**
   * List all ML models with optional filtering
   */
  async list(params?: {
    page?: number;
    size?: number;
    type?: MLModelType;
    status?: MLModelStatus
  }): Promise<PageResponse<MLModel>> {
    const queryParams = new URLSearchParams();
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());
    if (params?.type) queryParams.append('type', params.type);
    if (params?.status) queryParams.append('status', params.status);

    const query = queryParams.toString();
    return request<PageResponse<MLModel>>(`/ml/models${query ? '?' + query : ''}`);
  },

  /**
   * Get a specific model by ID
   */
  async get(id: string): Promise<MLModel> {
    return request<MLModel>(`/ml/models/${id}`);
  },

  /**
   * Create a new ML model
   */
  async create(req: MLModelCreateRequest): Promise<MLModel> {
    return request<MLModel>('/ml/models', {
      method: 'POST',
      body: JSON.stringify(req),
    });
  },

  /**
   * Update an existing model
   */
  async update(id: string, req: MLModelUpdateRequest): Promise<MLModel> {
    return request<MLModel>(`/ml/models/${id}`, {
      method: 'PUT',
      body: JSON.stringify(req),
    });
  },

  /**
   * Delete a model
   */
  async delete(id: string): Promise<void> {
    return request<void>(`/ml/models/${id}`, { method: 'DELETE' });
  },

  /**
   * Deploy a model for production inference
   */
  async deploy(id: string): Promise<MLModel> {
    return request<MLModel>(`/ml/models/${id}/deploy`, { method: 'POST' });
  },

  /**
   * Archive a model
   */
  async archive(id: string): Promise<MLModel> {
    return request<MLModel>(`/ml/models/${id}/archive`, { method: 'POST' });
  },

  /**
   * Start training for a model.
   * Returns both the updated model and the training job details.
   */
  async train(id: string): Promise<TrainingStartResponse> {
    return request<TrainingStartResponse>(`/ml/models/${id}/train`, { method: 'POST' });
  },

  /**
   * Get deployed models
   */
  async getDeployed(): Promise<MLModel[]> {
    return request<MLModel[]>('/ml/models/deployed');
  },

  /**
   * Get model statistics
   */
  async getStats(): Promise<ModelStatsResponse> {
    return request<ModelStatsResponse>('/ml/models/stats');
  },
};

// ============================================================================
// ML Anomalies API
// ============================================================================

export const mlAnomaliesApi = {
  /**
   * List all anomalies with optional filtering
   */
  async list(params?: {
    page?: number;
    size?: number;
    status?: MLAnomalyStatus;
    severity?: MLAnomalySeverity;
  }): Promise<PageResponse<MLAnomaly>> {
    const queryParams = new URLSearchParams();
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());
    if (params?.status) queryParams.append('status', params.status);
    if (params?.severity) queryParams.append('severity', params.severity);

    const query = queryParams.toString();
    return request<PageResponse<MLAnomaly>>(`/ml/anomalies${query ? '?' + query : ''}`);
  },

  /**
   * Get anomalies for a specific device
   */
  async listByDevice(deviceId: string, params?: { page?: number; size?: number }): Promise<PageResponse<MLAnomaly>> {
    const queryParams = new URLSearchParams();
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());

    const query = queryParams.toString();
    return request<PageResponse<MLAnomaly>>(`/ml/anomalies/device/${deviceId}${query ? '?' + query : ''}`);
  },

  /**
   * Get a specific anomaly by ID
   */
  async get(id: string): Promise<MLAnomaly> {
    return request<MLAnomaly>(`/ml/anomalies/${id}`);
  },

  /**
   * Get new (unacknowledged) anomalies
   */
  async getNew(): Promise<MLAnomaly[]> {
    return request<MLAnomaly[]>('/ml/anomalies/new');
  },

  /**
   * Get critical anomalies
   */
  async getCritical(): Promise<MLAnomaly[]> {
    return request<MLAnomaly[]>('/ml/anomalies/critical');
  },

  /**
   * Acknowledge an anomaly
   */
  async acknowledge(id: string): Promise<MLAnomaly> {
    return request<MLAnomaly>(`/ml/anomalies/${id}/acknowledge`, { method: 'POST' });
  },

  /**
   * Start investigation on an anomaly
   */
  async investigate(id: string): Promise<MLAnomaly> {
    return request<MLAnomaly>(`/ml/anomalies/${id}/investigate`, { method: 'POST' });
  },

  /**
   * Resolve an anomaly
   */
  async resolve(id: string, req?: MLAnomalyResolveRequest): Promise<MLAnomaly> {
    return request<MLAnomaly>(`/ml/anomalies/${id}/resolve`, {
      method: 'POST',
      body: req ? JSON.stringify(req) : undefined,
    });
  },

  /**
   * Mark anomaly as false positive
   */
  async markFalsePositive(id: string, req?: MLAnomalyResolveRequest): Promise<MLAnomaly> {
    return request<MLAnomaly>(`/ml/anomalies/${id}/false-positive`, {
      method: 'POST',
      body: req ? JSON.stringify(req) : undefined,
    });
  },

  /**
   * Link anomaly to a global alert
   */
  async linkToAlert(id: string, alertId: string): Promise<MLAnomaly> {
    return request<MLAnomaly>(`/ml/anomalies/${id}/link-alert/${alertId}`, { method: 'POST' });
  },

  /**
   * Get anomaly statistics
   */
  async getStats(hours = 24): Promise<AnomalyStatsResponse> {
    return request<AnomalyStatsResponse>(`/ml/anomalies/stats?hours=${hours}`);
  },
};

// ============================================================================
// Training Jobs API
// ============================================================================

export const trainingJobsApi = {
  /**
   * Get a specific training job by ID
   */
  async get(jobId: string): Promise<TrainingJob> {
    return request<TrainingJob>(`/ml/training-jobs/${jobId}`);
  },

  /**
   * List all training jobs for the organization
   */
  async list(params?: {
    page?: number;
    size?: number;
  }): Promise<PageResponse<TrainingJob>> {
    const queryParams = new URLSearchParams();
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());

    const query = queryParams.toString();
    return request<PageResponse<TrainingJob>>(`/ml/training-jobs${query ? '?' + query : ''}`);
  },

  /**
   * Get training jobs for a specific model
   */
  async listForModel(modelId: string, params?: {
    page?: number;
    size?: number;
  }): Promise<PageResponse<TrainingJob>> {
    const queryParams = new URLSearchParams();
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());

    const query = queryParams.toString();
    return request<PageResponse<TrainingJob>>(`/ml/training-jobs/model/${modelId}${query ? '?' + query : ''}`);
  },

  /**
   * Get the latest training job for a model
   */
  async getLatestForModel(modelId: string): Promise<TrainingJob> {
    return request<TrainingJob>(`/ml/training-jobs/model/${modelId}/latest`);
  },

  /**
   * Cancel a training job
   */
  async cancel(jobId: string): Promise<TrainingJob> {
    return request<TrainingJob>(`/ml/training-jobs/${jobId}/cancel`, { method: 'POST' });
  },

  /**
   * Refresh job status from ML service
   */
  async refresh(jobId: string): Promise<TrainingJob> {
    return request<TrainingJob>(`/ml/training-jobs/${jobId}/refresh`, { method: 'POST' });
  },

  /**
   * Start training for a model (alternative endpoint)
   */
  async start(modelId: string, jobType?: TrainingJobType): Promise<TrainingJob> {
    return request<TrainingJob>(`/ml/training-jobs/start/${modelId}`, {
      method: 'POST',
      body: jobType ? JSON.stringify({ jobType }) : undefined,
    });
  },
};

// ============================================================================
// Helper Functions
// ============================================================================

export const getModelTypeLabel = (type: MLModelType): string => {
  const labels: Record<MLModelType, string> = {
    ANOMALY_DETECTION: 'Anomaly Detection',
    PREDICTIVE_MAINTENANCE: 'Predictive Maintenance',
    ENERGY_FORECAST: 'Energy Forecast',
    EQUIPMENT_RUL: 'Equipment RUL',
  };
  return labels[type] || type;
};

export const getModelStatusLabel = (status: MLModelStatus): string => {
  const labels: Record<MLModelStatus, string> = {
    DRAFT: 'Draft',
    TRAINING: 'Training',
    TRAINED: 'Trained',
    DEPLOYED: 'Deployed',
    FAILED: 'Failed',
    ARCHIVED: 'Archived',
  };
  return labels[status] || status;
};

export const getModelStatusColor = (status: MLModelStatus): string => {
  const colors: Record<MLModelStatus, string> = {
    DRAFT: 'gray',
    TRAINING: 'blue',
    TRAINED: 'green',
    DEPLOYED: 'emerald',
    FAILED: 'red',
    ARCHIVED: 'slate',
  };
  return colors[status] || 'gray';
};

export const getAnomalySeverityLabel = (severity: MLAnomalySeverity): string => {
  const labels: Record<MLAnomalySeverity, string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High',
    CRITICAL: 'Critical',
  };
  return labels[severity] || severity;
};

export const getAnomalySeverityColor = (severity: MLAnomalySeverity): string => {
  const colors: Record<MLAnomalySeverity, string> = {
    LOW: 'blue',
    MEDIUM: 'yellow',
    HIGH: 'orange',
    CRITICAL: 'red',
  };
  return colors[severity] || 'gray';
};

export const getAnomalyStatusLabel = (status: MLAnomalyStatus): string => {
  const labels: Record<MLAnomalyStatus, string> = {
    NEW: 'New',
    ACKNOWLEDGED: 'Acknowledged',
    INVESTIGATING: 'Investigating',
    RESOLVED: 'Resolved',
    FALSE_POSITIVE: 'False Positive',
  };
  return labels[status] || status;
};

export const getAnomalyStatusColor = (status: MLAnomalyStatus): string => {
  const colors: Record<MLAnomalyStatus, string> = {
    NEW: 'red',
    ACKNOWLEDGED: 'yellow',
    INVESTIGATING: 'blue',
    RESOLVED: 'green',
    FALSE_POSITIVE: 'gray',
  };
  return colors[status] || 'gray';
};

export const getTrainingJobStatusLabel = (status: TrainingJobStatus): string => {
  const labels: Record<TrainingJobStatus, string> = {
    PENDING: 'Pending',
    RUNNING: 'Running',
    COMPLETED: 'Completed',
    FAILED: 'Failed',
    CANCELLED: 'Cancelled',
  };
  return labels[status] || status;
};

export const getTrainingJobStatusColor = (status: TrainingJobStatus): string => {
  const colors: Record<TrainingJobStatus, string> = {
    PENDING: 'yellow',
    RUNNING: 'blue',
    COMPLETED: 'green',
    FAILED: 'red',
    CANCELLED: 'gray',
  };
  return colors[status] || 'gray';
};

export const getTrainingJobTypeLabel = (type: TrainingJobType): string => {
  const labels: Record<TrainingJobType, string> = {
    INITIAL_TRAINING: 'Initial Training',
    RETRAINING: 'Retraining',
    HYPERPARAMETER_TUNING: 'Hyperparameter Tuning',
  };
  return labels[type] || type;
};

export const isTrainingJobActive = (status: TrainingJobStatus): boolean => {
  return status === 'PENDING' || status === 'RUNNING';
};

export const isTrainingJobTerminal = (status: TrainingJobStatus): boolean => {
  return status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED';
};
