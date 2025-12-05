import { apiService } from './api';

export interface DataRetentionPolicy {
  id: number;
  enabled: boolean;
  retentionDays: number;
  archiveEnabled: boolean;
  archiveStorageType: 'LOCAL_FILE' | 'S3' | 'AZURE_BLOB' | 'GCS';
  archiveStorageConfig?: Record<string, any>;
  archiveScheduleCron: string;
  lastArchiveRun?: string;
  lastArchiveStatus?: 'RUNNING' | 'SUCCESS' | 'FAILED';
  lastArchiveError?: string;
  totalRecordsArchived: number;
  totalArchiveSizeBytes: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRetentionPolicyRequest {
  enabled: boolean;
  retentionDays: number;
  archiveEnabled: boolean;
  archiveStorageType: 'LOCAL_FILE' | 'S3' | 'AZURE_BLOB' | 'GCS';
  archiveStorageConfig?: Record<string, any>;
  archiveScheduleCron?: string;
}

export interface ArchiveExecution {
  id: number;
  policyId: number;
  startedAt: string;
  completedAt?: string;
  status: 'RUNNING' | 'SUCCESS' | 'FAILED';
  archiveFromDate: string;
  archiveToDate: string;
  recordsArchived: number;
  archiveFilePath?: string;
  archiveSizeBytes: number;
  durationMs?: number;
  errorMessage?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const dataRetentionService = {
  async getPolicy(): Promise<DataRetentionPolicy> {
    const response = await apiService.get<DataRetentionPolicy>('/retention-policies');
    return response.data;
  },

  async createOrUpdatePolicy(policy: CreateRetentionPolicyRequest): Promise<DataRetentionPolicy> {
    const response = await apiService.put<DataRetentionPolicy>('/retention-policies', policy);
    return response.data;
  },

  async deletePolicy(): Promise<void> {
    await apiService.delete('/retention-policies');
  },

  async executeArchival(): Promise<ArchiveExecution> {
    const response = await apiService.post<ArchiveExecution>('/retention-policies/execute');
    return response.data;
  },

  async getExecutions(page = 0, size = 20): Promise<PageResponse<ArchiveExecution>> {
    const response = await apiService.get<PageResponse<ArchiveExecution>>(
      `/retention-policies/executions?page=${page}&size=${size}`
    );
    return response.data;
  },
};

export default dataRetentionService;
