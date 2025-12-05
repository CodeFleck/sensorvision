import { apiService } from './api';

export interface EmailTemplateRequest {
  name: string;
  description?: string;
  templateType: string;
  subject: string;
  body: string;
  variables?: any;
  isDefault: boolean;
  active: boolean;
}

export interface EmailTemplateResponse {
  id: number;
  name: string;
  description?: string;
  templateType: string;
  subject: string;
  body: string;
  variables?: any;
  isDefault: boolean;
  active: boolean;
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const emailTemplateService = {
  async createTemplate(request: EmailTemplateRequest): Promise<EmailTemplateResponse> {
    const response = await apiService.post<EmailTemplateResponse>('/email-templates', request);
    return response.data;
  },

  async getTemplates(
    page = 0,
    size = 20,
    templateType?: string,
    active?: boolean
  ): Promise<PageResponse<EmailTemplateResponse>> {
    let url = `/email-templates?page=${page}&size=${size}`;
    if (templateType) url += `&templateType=${templateType}`;
    if (active !== undefined) url += `&active=${active}`;
    
    const response = await apiService.get<PageResponse<EmailTemplateResponse>>(url);
    return response.data;
  },

  async getTemplate(id: number): Promise<EmailTemplateResponse> {
    const response = await apiService.get<EmailTemplateResponse>(`/email-templates/${id}`);
    return response.data;
  },

  async updateTemplate(id: number, request: EmailTemplateRequest): Promise<EmailTemplateResponse> {
    const response = await apiService.put<EmailTemplateResponse>(`/email-templates/${id}`, request);
    return response.data;
  },

  async deleteTemplate(id: number): Promise<void> {
    await apiService.delete(`/email-templates/${id}`);
  },

  async previewTemplate(id: number, sampleData: Record<string, any>): Promise<{ subject: string; body: string }> {
    const response = await apiService.post<{ subject: string; body: string }>(`/email-templates/${id}/preview`, sampleData);
    return response.data;
  },
};

export default emailTemplateService;
