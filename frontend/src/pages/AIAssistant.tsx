import { useState, useEffect, useRef } from 'react';
import toast from 'react-hot-toast';
import {
  MessageSquare,
  FileText,
  Search,
  Send,
  Loader2,
  Sparkles,
  Clock,
  Zap,
  Calendar,
  Cpu,
  AlertTriangle,
  TrendingUp,
  Copy,
  Check,
} from 'lucide-react';
import { clsx } from 'clsx';
import {
  llmApi,
  NaturalLanguageQueryResponse,
  ReportGenerationResponse,
  RootCauseAnalysisResponse,
  ReportType,
  RootCauseSourceType,
  getReportTypeLabel,
  getReportTypeDescription,
  getSourceTypeLabel,
} from '../services/llmService';
import { apiService } from '../services/api';
import { Device, Alert } from '../types';
import { mlAnomaliesApi, MLAnomaly } from '../services/mlService';
import { SafeLLMContent } from '../components/SafeLLMContent';
import { MultiSelect } from '../components/MultiSelect';

type TabType = 'query' | 'reports' | 'root-cause';

interface Message {
  id: string;
  type: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  metadata?: {
    tokensUsed?: number;
    latencyMs?: number;
    provider?: string;
  };
}

export const AIAssistant = () => {
  const [activeTab, setActiveTab] = useState<TabType>('query');
  const [loading, setLoading] = useState(false);

  // Natural Language Query state
  const [queryInput, setQueryInput] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [selectedDevices, setSelectedDevices] = useState<string[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Report Generation state
  const [reportType, setReportType] = useState<ReportType>('DAILY_SUMMARY');
  const [reportDevices, setReportDevices] = useState<string[]>([]);
  const [reportPeriodStart, setReportPeriodStart] = useState('');
  const [reportPeriodEnd, setReportPeriodEnd] = useState('');
  const [customPrompt, setCustomPrompt] = useState('');
  const [generatedReport, setGeneratedReport] = useState<ReportGenerationResponse | null>(null);

  // Root Cause Analysis state
  const [sourceType, setSourceType] = useState<RootCauseSourceType>('ANOMALY');
  const [sourceId, setSourceId] = useState('');
  const [additionalContext, setAdditionalContext] = useState('');
  const [lookbackHours, setLookbackHours] = useState(24);
  const [rcaResult, setRcaResult] = useState<RootCauseAnalysisResponse | null>(null);
  const [anomalies, setAnomalies] = useState<MLAnomaly[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);

  // Copy state
  const [copiedId, setCopiedId] = useState<string | null>(null);

  useEffect(() => {
    loadDevices();
    loadAnomaliesAndAlerts();
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const loadDevices = async () => {
    try {
      const data = await apiService.getDevices();
      setDevices(data);
    } catch (error) {
      console.error('Failed to load devices:', error);
    }
  };

  const loadAnomaliesAndAlerts = async () => {
    try {
      const [anomalyData, alertData] = await Promise.all([
        mlAnomaliesApi.list({ page: 0, size: 50 }),
        apiService.getAlerts(),
      ]);
      setAnomalies(anomalyData.content);
      setAlerts(alertData);
    } catch (error) {
      console.error('Failed to load anomalies/alerts:', error);
    }
  };

  const handleSendQuery = async () => {
    if (!queryInput.trim() || loading) return;

    const userMessage: Message = {
      id: crypto.randomUUID(),
      type: 'user',
      content: queryInput,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setQueryInput('');
    setLoading(true);

    try {
      const response: NaturalLanguageQueryResponse = await llmApi.naturalLanguageQuery({
        query: queryInput,
        deviceIds: selectedDevices.length > 0 ? selectedDevices : undefined,
      });

      const assistantMessage: Message = {
        id: crypto.randomUUID(),
        type: 'assistant',
        content: response.success ? response.response : `Error: ${response.errorMessage}`,
        timestamp: new Date(),
        metadata: {
          tokensUsed: response.tokensUsed,
          latencyMs: response.latencyMs,
          provider: response.provider,
        },
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to process query';
      toast.error(errorMessage);
      setMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          type: 'assistant',
          content: `Error: ${errorMessage}`,
          timestamp: new Date(),
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateReport = async () => {
    if (loading) return;
    setLoading(true);

    try {
      const response = await llmApi.generateReport({
        reportType,
        deviceIds: reportDevices.length > 0 ? reportDevices : undefined,
        periodStart: reportPeriodStart || undefined,
        periodEnd: reportPeriodEnd || undefined,
        customPrompt: reportType === 'CUSTOM' ? customPrompt : undefined,
      });

      if (response.success) {
        setGeneratedReport(response);
        toast.success('Report generated successfully');
      } else {
        toast.error(response.errorMessage || 'Failed to generate report');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to generate report';
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleAnalyzeRootCause = async () => {
    if (!sourceId || loading) return;
    setLoading(true);

    try {
      const response = await llmApi.analyzeRootCause({
        sourceId,
        sourceType,
        additionalContext: additionalContext || undefined,
        lookbackHours,
      });

      if (response.success) {
        setRcaResult(response);
        toast.success('Root cause analysis completed');
      } else {
        toast.error(response.errorMessage || 'Failed to analyze root cause');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to analyze root cause';
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = async (text: string, id: string) => {
    await navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const reportTypes: ReportType[] = [
    'DAILY_SUMMARY',
    'WEEKLY_REVIEW',
    'MONTHLY_ANALYSIS',
    'ANOMALY_REPORT',
    'DEVICE_HEALTH',
    'ENERGY_ANALYSIS',
    'CUSTOM',
  ];

  const sourceTypes: RootCauseSourceType[] = ['ANOMALY', 'ALERT', 'DEVICE_FAILURE', 'PERFORMANCE_DEGRADATION'];

  const exampleQueries = [
    'What is the average temperature across all sensors today?',
    'Show me devices with the highest power consumption this week',
    'Are there any anomalies in the last 24 hours?',
    'Compare energy usage between Monday and Tuesday',
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-primary flex items-center gap-2">
            <Sparkles className="h-7 w-7 text-violet-500" />
            AI Assistant
          </h1>
          <p className="text-secondary mt-1">
            Query your IoT data using natural language, generate reports, and analyze issues
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-default">
        <nav className="flex space-x-8">
          {[
            { id: 'query' as TabType, label: 'Natural Language Query', icon: MessageSquare },
            { id: 'reports' as TabType, label: 'Report Generation', icon: FileText },
            { id: 'root-cause' as TabType, label: 'Root Cause Analysis', icon: Search },
          ].map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={clsx(
                  'flex items-center gap-2 py-4 px-1 border-b-2 font-medium text-sm transition-colors',
                  activeTab === tab.id
                    ? 'border-violet-500 text-violet-600'
                    : 'border-transparent text-secondary hover:text-primary hover:border-gray-300'
                )}
              >
                <Icon className="h-4 w-4" />
                {tab.label}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Tab Content */}
      <div className="bg-primary rounded-lg border border-default">
        {/* Natural Language Query Tab */}
        {activeTab === 'query' && (
          <div className="flex flex-col h-[600px]">
            {/* Device Filter */}
            <div className="p-4 border-b border-default">
              <div className="flex items-center gap-4">
                <div className="flex items-center gap-2 flex-shrink-0">
                  <Cpu className="h-4 w-4 text-secondary" />
                  <span className="text-sm text-secondary">Filter by devices:</span>
                </div>
                <MultiSelect
                  options={devices.map((d) => ({ value: d.id, label: d.name }))}
                  selected={selectedDevices}
                  onChange={setSelectedDevices}
                  placeholder="All devices"
                  searchPlaceholder="Search devices..."
                  className="flex-1 max-w-md"
                />
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {messages.length === 0 ? (
                <div className="text-center py-12">
                  <Sparkles className="h-12 w-12 text-violet-400 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-primary mb-2">Ask anything about your data</h3>
                  <p className="text-secondary mb-6">
                    Use natural language to query your IoT data. Try one of these examples:
                  </p>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3 max-w-2xl mx-auto">
                    {exampleQueries.map((query, idx) => (
                      <button
                        key={idx}
                        onClick={() => setQueryInput(query)}
                        className="text-left p-3 border border-default rounded-lg hover:border-violet-300 hover:bg-violet-50 dark:hover:bg-violet-900/20 transition-colors"
                      >
                        <p className="text-sm text-primary">{query}</p>
                      </button>
                    ))}
                  </div>
                </div>
              ) : (
                messages.map((message) => (
                  <div
                    key={message.id}
                    className={clsx(
                      'flex',
                      message.type === 'user' ? 'justify-end' : 'justify-start'
                    )}
                  >
                    <div
                      className={clsx(
                        'max-w-[80%] rounded-lg p-4',
                        message.type === 'user'
                          ? 'bg-violet-600 text-white'
                          : 'bg-hover text-primary'
                      )}
                    >
                      {message.type === 'user' ? (
                        <p className="whitespace-pre-wrap">{message.content}</p>
                      ) : (
                        <SafeLLMContent content={message.content} />
                      )}
                      {message.metadata && (
                        <div className="mt-2 pt-2 border-t border-white/20 flex items-center gap-4 text-xs opacity-70">
                          {message.metadata.tokensUsed && (
                            <span>{message.metadata.tokensUsed} tokens</span>
                          )}
                          {message.metadata.latencyMs && (
                            <span>{message.metadata.latencyMs}ms</span>
                          )}
                          {message.metadata.provider && (
                            <span>{message.metadata.provider}</span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                ))
              )}
              {loading && (
                <div className="flex justify-start">
                  <div className="bg-hover rounded-lg p-4 flex items-center gap-2">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span className="text-secondary">Thinking...</span>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className="p-4 border-t border-default">
              <div className="flex items-center gap-3">
                <input
                  type="text"
                  value={queryInput}
                  onChange={(e) => setQueryInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSendQuery()}
                  placeholder="Ask a question about your IoT data..."
                  className="flex-1 px-4 py-3 border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-violet-500 focus:border-violet-500"
                  disabled={loading}
                />
                <button
                  onClick={handleSendQuery}
                  disabled={loading || !queryInput.trim()}
                  className="px-4 py-3 bg-violet-600 text-white rounded-lg hover:bg-violet-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  <Send className="h-5 w-5" />
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Report Generation Tab */}
        {activeTab === 'reports' && (
          <div className="p-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {/* Configuration */}
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-semibold text-primary mb-4 flex items-center gap-2">
                    <FileText className="h-5 w-5 text-violet-500" />
                    Configure Report
                  </h3>

                  {/* Report Type */}
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-primary mb-2">
                        Report Type
                      </label>
                      <div className="grid grid-cols-1 gap-2">
                        {reportTypes.map((type) => (
                          <button
                            key={type}
                            onClick={() => setReportType(type)}
                            className={clsx(
                              'text-left p-3 border rounded-lg transition-colors',
                              reportType === type
                                ? 'border-violet-500 bg-violet-50 dark:bg-violet-900/20'
                                : 'border-default hover:border-gray-300'
                            )}
                          >
                            <p className="font-medium text-primary">{getReportTypeLabel(type)}</p>
                            <p className="text-xs text-secondary mt-1">
                              {getReportTypeDescription(type)}
                            </p>
                          </button>
                        ))}
                      </div>
                    </div>

                    {/* Time Period */}
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-primary mb-2">
                          <Calendar className="inline h-4 w-4 mr-1" />
                          Start Date
                        </label>
                        <input
                          type="datetime-local"
                          value={reportPeriodStart}
                          onChange={(e) => setReportPeriodStart(e.target.value)}
                          className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-primary mb-2">
                          <Calendar className="inline h-4 w-4 mr-1" />
                          End Date
                        </label>
                        <input
                          type="datetime-local"
                          value={reportPeriodEnd}
                          onChange={(e) => setReportPeriodEnd(e.target.value)}
                          className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary"
                        />
                      </div>
                    </div>

                    {/* Devices */}
                    <div>
                      <label className="block text-sm font-medium text-primary mb-2">
                        <Cpu className="inline h-4 w-4 mr-1" />
                        Devices (optional)
                      </label>
                      <MultiSelect
                        options={devices.map((d) => ({ value: d.id, label: d.name }))}
                        selected={reportDevices}
                        onChange={setReportDevices}
                        placeholder="All devices"
                        searchPlaceholder="Search devices..."
                      />
                    </div>

                    {/* Custom Prompt */}
                    {reportType === 'CUSTOM' && (
                      <div>
                        <label className="block text-sm font-medium text-primary mb-2">
                          Custom Prompt
                        </label>
                        <textarea
                          value={customPrompt}
                          onChange={(e) => setCustomPrompt(e.target.value)}
                          placeholder="Describe what you want in this report..."
                          rows={4}
                          className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary"
                        />
                      </div>
                    )}

                    <button
                      onClick={handleGenerateReport}
                      disabled={loading}
                      className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-violet-600 text-white rounded-lg hover:bg-violet-700 disabled:opacity-50 transition-colors"
                    >
                      {loading ? (
                        <Loader2 className="h-5 w-5 animate-spin" />
                      ) : (
                        <Zap className="h-5 w-5" />
                      )}
                      Generate Report
                    </button>
                  </div>
                </div>
              </div>

              {/* Generated Report */}
              <div>
                <h3 className="text-lg font-semibold text-primary mb-4">Generated Report</h3>
                {generatedReport ? (
                  <div className="border border-default rounded-lg p-4 space-y-4 max-h-[600px] overflow-y-auto">
                    <div className="flex items-start justify-between">
                      <div>
                        <h4 className="font-bold text-lg text-primary">{generatedReport.title}</h4>
                        <p className="text-xs text-secondary mt-1">
                          Generated at {new Date(generatedReport.generatedAt).toLocaleString()}
                        </p>
                      </div>
                      <button
                        onClick={() => copyToClipboard(generatedReport.content, 'report')}
                        className="p-2 text-secondary hover:text-primary transition-colors"
                      >
                        {copiedId === 'report' ? (
                          <Check className="h-4 w-4 text-green-500" />
                        ) : (
                          <Copy className="h-4 w-4" />
                        )}
                      </button>
                    </div>

                    {generatedReport.executiveSummary && (
                      <div className="bg-violet-50 dark:bg-violet-900/20 rounded-lg p-3">
                        <p className="text-sm font-medium text-violet-800 dark:text-violet-200 mb-1">
                          Executive Summary
                        </p>
                        <p className="text-sm text-primary">{generatedReport.executiveSummary}</p>
                      </div>
                    )}

                    <SafeLLMContent content={generatedReport.content} />

                    {generatedReport.keyFindings && generatedReport.keyFindings.length > 0 && (
                      <div>
                        <p className="font-medium text-primary mb-2">Key Findings</p>
                        <ul className="list-disc list-inside space-y-1">
                          {generatedReport.keyFindings.map((finding, idx) => (
                            <li key={idx} className="text-sm text-secondary">
                              {finding}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {generatedReport.recommendations && generatedReport.recommendations.length > 0 && (
                      <div>
                        <p className="font-medium text-primary mb-2">Recommendations</p>
                        <ul className="list-disc list-inside space-y-1">
                          {generatedReport.recommendations.map((rec, idx) => (
                            <li key={idx} className="text-sm text-secondary">
                              {rec}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    <div className="pt-3 border-t border-default flex items-center gap-4 text-xs text-secondary">
                      {generatedReport.tokensUsed && <span>{generatedReport.tokensUsed} tokens</span>}
                      {generatedReport.latencyMs && <span>{generatedReport.latencyMs}ms</span>}
                      {generatedReport.provider && <span>{generatedReport.provider}</span>}
                    </div>
                  </div>
                ) : (
                  <div className="border border-dashed border-default rounded-lg p-8 text-center">
                    <FileText className="h-12 w-12 text-gray-300 mx-auto mb-4" />
                    <p className="text-secondary">
                      Configure and generate a report to see results here
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Root Cause Analysis Tab */}
        {activeTab === 'root-cause' && (
          <div className="p-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {/* Configuration */}
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-semibold text-primary mb-4 flex items-center gap-2">
                    <Search className="h-5 w-5 text-violet-500" />
                    Analyze Root Cause
                  </h3>

                  <div className="space-y-4">
                    {/* Source Type */}
                    <div>
                      <label className="block text-sm font-medium text-primary mb-2">
                        Source Type
                      </label>
                      <div className="flex gap-2">
                        {sourceTypes.map((type) => (
                          <button
                            key={type}
                            onClick={() => {
                              setSourceType(type);
                              setSourceId('');
                            }}
                            className={clsx(
                              'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                              sourceType === type
                                ? 'bg-violet-600 text-white'
                                : 'bg-hover text-secondary hover:bg-gray-200 dark:hover:bg-gray-700'
                            )}
                          >
                            {getSourceTypeLabel(type)}
                          </button>
                        ))}
                      </div>
                    </div>

                    {/* Source Selection */}
                    <div>
                      <label className="block text-sm font-medium text-primary mb-2">
                        Select {getSourceTypeLabel(sourceType)}
                      </label>
                      {sourceType === 'ANOMALY' ? (
                        <select
                          value={sourceId}
                          onChange={(e) => setSourceId(e.target.value)}
                          className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary"
                        >
                          <option value="">Select an anomaly...</option>
                          {anomalies.map((anomaly) => (
                            <option key={anomaly.id} value={anomaly.id}>
                              {anomaly.anomalyType} - {anomaly.deviceName} ({anomaly.severity})
                            </option>
                          ))}
                        </select>
                      ) : sourceType === 'ALERT' ? (
                        <select
                          value={sourceId}
                          onChange={(e) => setSourceId(e.target.value)}
                          className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary"
                        >
                          <option value="">Select an alert...</option>
                          {alerts.map((alert) => (
                            <option key={alert.id} value={alert.id}>
                              {alert.message || `Alert ${alert.id}`} ({alert.severity})
                            </option>
                          ))}
                        </select>
                      ) : (
                        <input
                          type="text"
                          value={sourceId}
                          onChange={(e) => setSourceId(e.target.value)}
                          placeholder="Enter source ID..."
                          className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary"
                        />
                      )}
                    </div>

                    {/* Lookback Hours */}
                    <div>
                      <label className="block text-sm font-medium text-primary mb-2">
                        <Clock className="inline h-4 w-4 mr-1" />
                        Lookback Period (hours)
                      </label>
                      <input
                        type="number"
                        value={lookbackHours}
                        onChange={(e) => setLookbackHours(parseInt(e.target.value) || 24)}
                        min={1}
                        max={168}
                        className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary"
                      />
                    </div>

                    {/* Additional Context */}
                    <div>
                      <label className="block text-sm font-medium text-primary mb-2">
                        Additional Context (optional)
                      </label>
                      <textarea
                        value={additionalContext}
                        onChange={(e) => setAdditionalContext(e.target.value)}
                        placeholder="Any additional information that might help the analysis..."
                        rows={3}
                        className="w-full px-3 py-2 border border-default rounded-lg bg-primary text-primary"
                      />
                    </div>

                    <button
                      onClick={handleAnalyzeRootCause}
                      disabled={loading || !sourceId}
                      className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-violet-600 text-white rounded-lg hover:bg-violet-700 disabled:opacity-50 transition-colors"
                    >
                      {loading ? (
                        <Loader2 className="h-5 w-5 animate-spin" />
                      ) : (
                        <TrendingUp className="h-5 w-5" />
                      )}
                      Analyze Root Cause
                    </button>
                  </div>
                </div>
              </div>

              {/* Analysis Results */}
              <div>
                <h3 className="text-lg font-semibold text-primary mb-4">Analysis Results</h3>
                {rcaResult ? (
                  <div className="border border-default rounded-lg p-4 space-y-4 max-h-[600px] overflow-y-auto">
                    <div className="flex items-start justify-between">
                      <div>
                        <h4 className="font-bold text-lg text-primary">Root Cause Analysis</h4>
                        {rcaResult.deviceName && (
                          <p className="text-sm text-secondary">Device: {rcaResult.deviceName}</p>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <span
                          className={clsx(
                            'px-2 py-1 rounded text-xs font-medium',
                            rcaResult.confidenceLevel >= 80
                              ? 'bg-green-100 text-green-800'
                              : rcaResult.confidenceLevel >= 50
                              ? 'bg-yellow-100 text-yellow-800'
                              : 'bg-red-100 text-red-800'
                          )}
                        >
                          {rcaResult.confidenceLevel}% confidence
                        </span>
                        <button
                          onClick={() =>
                            copyToClipboard(rcaResult.fullAnalysis || rcaResult.issueSummary, 'rca')
                          }
                          className="p-2 text-secondary hover:text-primary transition-colors"
                        >
                          {copiedId === 'rca' ? (
                            <Check className="h-4 w-4 text-green-500" />
                          ) : (
                            <Copy className="h-4 w-4" />
                          )}
                        </button>
                      </div>
                    </div>

                    {rcaResult.issueSummary && (
                      <div className="bg-orange-50 dark:bg-orange-900/20 rounded-lg p-3">
                        <p className="text-sm font-medium text-orange-800 dark:text-orange-200 mb-1">
                          <AlertTriangle className="inline h-4 w-4 mr-1" />
                          Issue Summary
                        </p>
                        <p className="text-sm text-primary">{rcaResult.issueSummary}</p>
                      </div>
                    )}

                    {rcaResult.fullAnalysis && (
                      <SafeLLMContent content={rcaResult.fullAnalysis} />
                    )}

                    {rcaResult.rootCauses && rcaResult.rootCauses.length > 0 && (
                      <div>
                        <p className="font-medium text-primary mb-2">Root Causes</p>
                        <div className="space-y-2">
                          {rcaResult.rootCauses.map((cause, idx) => (
                            <div key={idx} className="p-3 bg-hover rounded-lg">
                              <div className="flex items-center justify-between mb-1">
                                <span className="font-medium text-primary">
                                  {idx + 1}. {cause.description}
                                </span>
                                <span className="text-sm text-violet-600">
                                  {cause.likelihoodPercent}% likely
                                </span>
                              </div>
                              <p className="text-xs text-secondary">Category: {cause.category}</p>
                              <p className="text-sm text-secondary mt-1">{cause.evidence}</p>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {rcaResult.correctiveActions && rcaResult.correctiveActions.length > 0 && (
                      <div>
                        <p className="font-medium text-primary mb-2">Corrective Actions</p>
                        <div className="space-y-2">
                          {rcaResult.correctiveActions.map((action, idx) => (
                            <div key={idx} className="flex items-start gap-3 p-2 bg-hover rounded">
                              <span
                                className={clsx(
                                  'px-2 py-0.5 text-xs font-medium rounded',
                                  action.urgency === 'IMMEDIATE'
                                    ? 'bg-red-100 text-red-800'
                                    : action.urgency === 'SHORT_TERM'
                                    ? 'bg-yellow-100 text-yellow-800'
                                    : 'bg-blue-100 text-blue-800'
                                )}
                              >
                                {action.urgency}
                              </span>
                              <div>
                                <p className="text-sm text-primary">{action.action}</p>
                                <p className="text-xs text-secondary">{action.expectedOutcome}</p>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {rcaResult.preventiveMeasures && rcaResult.preventiveMeasures.length > 0 && (
                      <div>
                        <p className="font-medium text-primary mb-2">Preventive Measures</p>
                        <ul className="list-disc list-inside space-y-1">
                          {rcaResult.preventiveMeasures.map((measure, idx) => (
                            <li key={idx} className="text-sm text-secondary">
                              {measure}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    <div className="pt-3 border-t border-default flex items-center gap-4 text-xs text-secondary">
                      {rcaResult.tokensUsed && <span>{rcaResult.tokensUsed} tokens</span>}
                      {rcaResult.latencyMs && <span>{rcaResult.latencyMs}ms</span>}
                      {rcaResult.provider && <span>{rcaResult.provider}</span>}
                    </div>
                  </div>
                ) : (
                  <div className="border border-dashed border-default rounded-lg p-8 text-center">
                    <Search className="h-12 w-12 text-gray-300 mx-auto mb-4" />
                    <p className="text-secondary">
                      Select a source and run analysis to see results here
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AIAssistant;
