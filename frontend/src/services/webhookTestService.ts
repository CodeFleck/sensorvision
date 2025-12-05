import { apiService } from './api';

export interface WebhookTestRequest {
  name?: string;
  url: string;
  httpMethod: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  headers?: Record<string, any>;
  requestBody?: string;
}

export interface WebhookTestResponse {
  id: number;
  name?: string;
  url: string;
  httpMethod: string;
  headers?: Record<string, any>;
  requestBody?: string;
  statusCode?: number;
  responseBody?: string;
  responseHeaders?: Record<string, any>;
  durationMs?: number;
  errorMessage?: string;
  createdBy?: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const webhookTestService = {
  async executeTest(request: WebhookTestRequest): Promise<WebhookTestResponse> {
    const response = await apiService.post<WebhookTestResponse>('/webhook-tests', request);
    return response.data;
  },

  async getHistory(page = 0, size = 20): Promise<PageResponse<WebhookTestResponse>> {
    const response = await apiService.get<PageResponse<WebhookTestResponse>>(
      `/webhook-tests?page=${page}&size=${size}`
    );
    return response.data;
  },

  async getTest(id: number): Promise<WebhookTestResponse> {
    const response = await apiService.get<WebhookTestResponse>(`/webhook-tests/${id}`);
    return response.data;
  },

  async deleteTest(id: number): Promise<void> {
    await apiService.delete(`/webhook-tests/${id}`);
  },
};

export default webhookTestService;
