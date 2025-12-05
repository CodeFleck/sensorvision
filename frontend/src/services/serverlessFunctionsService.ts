import axios from 'axios';

const API_URL = '/api/v1/functions';

export interface ServerlessFunction {
  id: number;
  organizationId: number;
  name: string;
  description?: string;
  runtime: FunctionRuntime;
  code: string;
  handler: string;
  enabled: boolean;
  timeoutSeconds: number;
  memoryLimitMb: number;
  environmentVariables?: Record<string, string>;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export enum FunctionRuntime {
  PYTHON_3_11 = 'PYTHON_3_11',
  NODEJS_18 = 'NODEJS_18'
}

export interface FunctionTrigger {
  id: number;
  functionId: number;
  triggerType: FunctionTriggerType;
  triggerConfig: Record<string, any>;
  enabled: boolean;
}

export enum FunctionTriggerType {
  HTTP = 'HTTP',
  MQTT = 'MQTT',
  SCHEDULED = 'SCHEDULED',
  DEVICE_EVENT = 'DEVICE_EVENT'
}

export interface FunctionExecution {
  id: number;
  functionId: number;
  status: FunctionExecutionStatus;
  startedAt: string;
  completedAt?: string;
  durationMs?: number;
  inputData?: any;
  outputData?: any;
  errorMessage?: string;
  errorStack?: string;
  memoryUsedMb?: number;
}

export enum FunctionExecutionStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  TIMEOUT = 'TIMEOUT'
}

export interface CreateFunctionRequest {
  name: string;
  description?: string;
  runtime: FunctionRuntime;
  code: string;
  handler?: string;
  enabled?: boolean;
  timeoutSeconds?: number;
  memoryLimitMb?: number;
  environmentVariables?: Record<string, string>;
}

export interface UpdateFunctionRequest {
  name?: string;
  description?: string;
  code?: string;
  handler?: string;
  enabled?: boolean;
  timeoutSeconds?: number;
  memoryLimitMb?: number;
  environmentVariables?: Record<string, string>;
}

export interface InvokeFunctionRequest {
  input: any;
  sync?: boolean;
}

export interface InvokeFunctionResponse {
  executionId: number;
  status: string;
  output?: any;
  errorMessage?: string;
}

class ServerlessFunctionsService {
  async getAllFunctions(): Promise<ServerlessFunction[]> {
    const response = await axios.get<ServerlessFunction[]>(API_URL);
    return response.data;
  }

  async getFunction(id: number): Promise<ServerlessFunction> {
    const response = await axios.get<ServerlessFunction>(`${API_URL}/${id}`);
    return response.data;
  }

  async createFunction(request: CreateFunctionRequest): Promise<ServerlessFunction> {
    const response = await axios.post<ServerlessFunction>(API_URL, request);
    return response.data;
  }

  async updateFunction(id: number, request: UpdateFunctionRequest): Promise<ServerlessFunction> {
    const response = await axios.put<ServerlessFunction>(`${API_URL}/${id}`, request);
    return response.data;
  }

  async deleteFunction(id: number): Promise<void> {
    await axios.delete(`${API_URL}/${id}`);
  }

  async invokeFunction(id: number, request: InvokeFunctionRequest): Promise<InvokeFunctionResponse> {
    const response = await axios.post<InvokeFunctionResponse>(
      `${API_URL}/${id}/invoke`,
      request
    );
    return response.data;
  }

  async getExecutionHistory(
    id: number,
    page = 0,
    size = 20
  ): Promise<{ content: FunctionExecution[]; totalPages: number; totalElements: number }> {
    const response = await axios.get(`${API_URL}/${id}/executions`, {
      params: { page, size, sort: 'startedAt,desc' }
    });
    return response.data;
  }

  async getTriggers(functionId: number): Promise<FunctionTrigger[]> {
    const response = await axios.get<FunctionTrigger[]>(`${API_URL}/${functionId}/triggers`);
    return response.data;
  }

  async createTrigger(functionId: number, trigger: Omit<FunctionTrigger, 'id' | 'functionId'>): Promise<FunctionTrigger> {
    const response = await axios.post<FunctionTrigger>(
      `${API_URL}/${functionId}/triggers`,
      trigger
    );
    return response.data;
  }

  async deleteTrigger(functionId: number, triggerId: number): Promise<void> {
    await axios.delete(`${API_URL}/${functionId}/triggers/${triggerId}`);
  }
}

export default new ServerlessFunctionsService();
