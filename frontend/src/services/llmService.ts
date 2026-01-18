/**
 * LLM Service - API client for AI/LLM features
 * Handles anomaly explanation, natural language queries, report generation, and root cause analysis
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

export interface AnomalyExplanation {
  anomalyId: string;
  deviceId: string;
  deviceName: string;
  anomalyScore: number;
  severity: string;
  explanation: string;
  success: boolean;
  errorMessage?: string;
  provider?: string;
  modelId?: string;
  tokensUsed?: number;
  latencyMs?: number;
  generatedAt: string;
}

export interface NaturalLanguageQueryRequest {
  query: string;
  deviceIds?: string[];
  fromTime?: string;
  toTime?: string;
}

export interface DataPoint {
  deviceId: string;
  deviceName: string;
  variableName: string;
  value: unknown;
  unit?: string;
  timestamp: string;
}

export interface NaturalLanguageQueryResponse {
  query: string;
  deviceIds?: string[];
  fromTime?: string;
  toTime?: string;
  response: string;
  supportingData?: DataPoint[];
  success: boolean;
  errorMessage?: string;
  provider?: string;
  modelId?: string;
  tokensUsed?: number;
  latencyMs?: number;
  generatedAt: string;
}

export type ReportType =
  | 'DAILY_SUMMARY'
  | 'WEEKLY_REVIEW'
  | 'MONTHLY_ANALYSIS'
  | 'ANOMALY_REPORT'
  | 'DEVICE_HEALTH'
  | 'ENERGY_ANALYSIS'
  | 'CUSTOM';

export interface ReportGenerationRequest {
  reportType: ReportType;
  deviceIds?: string[];
  periodStart?: string;
  periodEnd?: string;
  customPrompt?: string;
}

export interface ReportGenerationResponse {
  reportId: string;
  reportType: ReportType;
  title: string;
  executiveSummary?: string;
  content: string;
  keyFindings?: string[];
  recommendations?: string[];
  periodStart?: string;
  periodEnd?: string;
  deviceIds?: string[];
  success: boolean;
  errorMessage?: string;
  provider?: string;
  modelId?: string;
  tokensUsed?: number;
  latencyMs?: number;
  generatedAt: string;
}

export type RootCauseSourceType =
  | 'ALERT'
  | 'ANOMALY'
  | 'DEVICE_FAILURE'
  | 'PERFORMANCE_DEGRADATION';

export interface RootCauseAnalysisRequest {
  sourceId: string;
  sourceType: RootCauseSourceType;
  additionalContext?: string;
  lookbackHours?: number;
}

export interface RootCause {
  description: string;
  likelihoodPercent: number;
  category: string;
  evidence: string;
}

export interface TimelineEvent {
  timestamp: string;
  event: string;
  significance: string;
}

export interface CorrectiveAction {
  priority: number;
  action: string;
  expectedOutcome: string;
  urgency: string;
}

export interface RootCauseAnalysisResponse {
  analysisId: string;
  sourceId: string;
  sourceType: RootCauseSourceType;
  deviceId?: string;
  deviceName?: string;
  issueSummary: string;
  rootCauses: RootCause[];
  contributingFactors: string[];
  timeline?: TimelineEvent[];
  correctiveActions: CorrectiveAction[];
  preventiveMeasures: string[];
  fullAnalysis?: string;
  confidenceLevel: number;
  success: boolean;
  errorMessage?: string;
  provider?: string;
  modelId?: string;
  tokensUsed?: number;
  latencyMs?: number;
  generatedAt: string;
}

export interface LLMUsageStats {
  totalTokensUsed: number;
  totalRequests: number;
  byFeature: Record<string, number>;
  byProvider: Record<string, number>;
  periodStart: string;
  periodEnd: string;
}

// ============================================================================
// LLM API
// ============================================================================

export const llmApi = {
  /**
   * Get AI explanation for an anomaly
   */
  async explainAnomaly(anomalyId: string): Promise<AnomalyExplanation> {
    return request<AnomalyExplanation>(`/llm/anomalies/${anomalyId}/explain`, {
      method: 'POST',
    });
  },

  /**
   * Query data using natural language
   */
  async naturalLanguageQuery(req: NaturalLanguageQueryRequest): Promise<NaturalLanguageQueryResponse> {
    return request<NaturalLanguageQueryResponse>('/llm/query', {
      method: 'POST',
      body: JSON.stringify(req),
    });
  },

  /**
   * Generate an AI-powered report
   */
  async generateReport(req: ReportGenerationRequest): Promise<ReportGenerationResponse> {
    return request<ReportGenerationResponse>('/llm/reports/generate', {
      method: 'POST',
      body: JSON.stringify(req),
    });
  },

  /**
   * Perform root cause analysis
   */
  async analyzeRootCause(req: RootCauseAnalysisRequest): Promise<RootCauseAnalysisResponse> {
    return request<RootCauseAnalysisResponse>('/llm/root-cause/analyze', {
      method: 'POST',
      body: JSON.stringify(req),
    });
  },

  /**
   * Get LLM usage statistics
   */
  async getUsageStats(daysBack = 30): Promise<LLMUsageStats> {
    return request<LLMUsageStats>(`/llm/usage/stats?daysBack=${daysBack}`);
  },
};

// ============================================================================
// Helper Functions
// ============================================================================

export const getReportTypeLabel = (type: ReportType): string => {
  const labels: Record<ReportType, string> = {
    DAILY_SUMMARY: 'Daily Summary',
    WEEKLY_REVIEW: 'Weekly Review',
    MONTHLY_ANALYSIS: 'Monthly Analysis',
    ANOMALY_REPORT: 'Anomaly Report',
    DEVICE_HEALTH: 'Device Health',
    ENERGY_ANALYSIS: 'Energy Analysis',
    CUSTOM: 'Custom Report',
  };
  return labels[type] || type;
};

export const getReportTypeDescription = (type: ReportType): string => {
  const descriptions: Record<ReportType, string> = {
    DAILY_SUMMARY: 'Daily operational summary with key metrics and events',
    WEEKLY_REVIEW: 'Weekly performance review and trend analysis',
    MONTHLY_ANALYSIS: 'Comprehensive monthly analysis with insights',
    ANOMALY_REPORT: 'Summary and analysis of detected anomalies',
    DEVICE_HEALTH: 'Device health and status assessment',
    ENERGY_ANALYSIS: 'Energy consumption analysis and optimization suggestions',
    CUSTOM: 'Custom report based on your specific requirements',
  };
  return descriptions[type] || '';
};

export const getSourceTypeLabel = (type: RootCauseSourceType): string => {
  const labels: Record<RootCauseSourceType, string> = {
    ALERT: 'Alert',
    ANOMALY: 'Anomaly',
    DEVICE_FAILURE: 'Device Failure',
    PERFORMANCE_DEGRADATION: 'Performance Degradation',
  };
  return labels[type] || type;
};
