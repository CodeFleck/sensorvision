import api from './api';

export interface Secret {
  id: number;
  secretKey: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSecretRequest {
  secretKey: string;
  secretValue: string;
}

class SecretsService {
  /**
   * Get all secrets for a function (values are not returned).
   */
  async getSecrets(functionId: number): Promise<Secret[]> {
    const response = await api.get(`/api/v1/functions/${functionId}/secrets`);
    return response.data;
  }

  /**
   * Create or update a secret.
   */
  async createSecret(functionId: number, secretKey: string, request: CreateSecretRequest): Promise<Secret> {
    const response = await api.put(`/api/v1/functions/${functionId}/secrets/${secretKey}`, request);
    return response.data;
  }

  /**
   * Delete a secret.
   */
  async deleteSecret(functionId: number, secretKey: string): Promise<void> {
    await api.delete(`/api/v1/functions/${functionId}/secrets/${secretKey}`);
  }

  /**
   * Check if a secret exists.
   */
  async secretExists(functionId: number, secretKey: string): Promise<boolean> {
    const response = await api.get(`/api/v1/functions/${functionId}/secrets/${secretKey}/exists`);
    return response.data;
  }
}

export default new SecretsService();
