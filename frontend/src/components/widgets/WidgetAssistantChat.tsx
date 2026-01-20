import React, { useState, useRef, useEffect } from 'react';
import { clsx } from 'clsx';
import {
  MessageSquare,
  Send,
  Loader2,
  Sparkles,
  X,
  ChevronDown,
  Check,
  LayoutGrid,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { widgetAssistantApi, ChatResponse, WidgetSuggestion } from '../../services/llmService';
import { generateUUID } from '../../utils/uuid';

interface WidgetAssistantChatProps {
  dashboardId: number;
  onWidgetCreated?: () => void;
}

interface Message {
  id: string;
  type: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  suggestion?: WidgetSuggestion;
  metadata?: {
    tokensUsed?: number;
    latencyMs?: number;
    provider?: string;
  };
}

export const WidgetAssistantChat: React.FC<WidgetAssistantChatProps> = ({
  dashboardId,
  onWidgetCreated,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [pendingSuggestion, setPendingSuggestion] = useState<WidgetSuggestion | null>(null);
  const [creatingWidget, setCreatingWidget] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen && !isMinimized) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isOpen, isMinimized]);

  const handleSend = async () => {
    if (!inputValue.trim() || loading) return;

    const userMessage: Message = {
      id: generateUUID(),
      type: 'user',
      content: inputValue,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputValue('');
    setLoading(true);
    setPendingSuggestion(null);

    try {
      const response: ChatResponse = await widgetAssistantApi.chat({
        message: inputValue,
        dashboardId,
        conversationId: conversationId || undefined,
      });

      if (response.conversationId) {
        setConversationId(response.conversationId);
      }

      const assistantMessage: Message = {
        id: generateUUID(),
        type: 'assistant',
        content: response.response,
        timestamp: new Date(),
        suggestion: response.widgetSuggestion || undefined,
        metadata: {
          tokensUsed: response.tokensUsed || undefined,
          latencyMs: response.latencyMs || undefined,
          provider: response.provider || undefined,
        },
      };

      setMessages((prev) => [...prev, assistantMessage]);

      if (response.widgetSuggestion) {
        setPendingSuggestion(response.widgetSuggestion);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to send message';
      toast.error(errorMessage);
      setMessages((prev) => [
        ...prev,
        {
          id: generateUUID(),
          type: 'assistant',
          content: `Error: ${errorMessage}`,
          timestamp: new Date(),
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateWidget = async () => {
    if (!conversationId || !pendingSuggestion) return;

    setCreatingWidget(true);
    try {
      const response = await widgetAssistantApi.confirmWidget({
        conversationId,
        dashboardId,
        confirmed: true,
      });

      if (response.success) {
        toast.success(response.message || 'Widget created successfully!');
        setPendingSuggestion(null);
        setConversationId(null);
        setMessages((prev) => [
          ...prev,
          {
            id: generateUUID(),
            type: 'assistant',
            content: `Widget "${pendingSuggestion.name}" has been created on your dashboard.`,
            timestamp: new Date(),
          },
        ]);
        onWidgetCreated?.();
      } else {
        toast.error(response.message || 'Failed to create widget');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to create widget';
      toast.error(errorMessage);
    } finally {
      setCreatingWidget(false);
    }
  };

  const handleCancelWidget = () => {
    setPendingSuggestion(null);
    setMessages((prev) => [
      ...prev,
      {
        id: generateUUID(),
        type: 'assistant',
        content: 'Widget creation cancelled. Feel free to describe another widget you\'d like to create.',
        timestamp: new Date(),
      },
    ]);
  };

  const examplePrompts = [
    'Show me a gauge for temperature',
    'Create a line chart for power consumption',
    'Add a metric card showing voltage',
  ];

  const getWidgetTypeIcon = (type: string) => {
    const icons: Record<string, string> = {
      LINE_CHART: 'chart-line',
      GAUGE: 'gauge',
      METRIC_CARD: 'hash',
      BAR_CHART: 'bar-chart',
      AREA_CHART: 'area-chart',
      PIE_CHART: 'pie-chart',
    };
    return icons[type] || 'layout-grid';
  };

  return (
    <>
      {/* Floating Chat Bubble */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="fixed bottom-6 right-6 z-50 flex items-center justify-center w-14 h-14 bg-violet-600 text-white rounded-full shadow-lg hover:bg-violet-700 transition-all hover:scale-105"
          title="AI Widget Assistant"
        >
          <Sparkles className="h-6 w-6" />
        </button>
      )}

      {/* Chat Panel */}
      {isOpen && (
        <div
          className={clsx(
            'fixed z-50 bg-primary border border-default rounded-lg shadow-2xl transition-all duration-300',
            isMinimized
              ? 'bottom-6 right-6 w-72'
              : 'bottom-6 right-6 w-96 max-h-[600px]'
          )}
        >
          {/* Header */}
          <div className="flex items-center justify-between p-3 border-b border-default bg-violet-600 text-white rounded-t-lg">
            <div className="flex items-center gap-2">
              <Sparkles className="h-5 w-5" />
              <span className="font-medium">Widget Assistant</span>
            </div>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setIsMinimized(!isMinimized)}
                className="p-1 hover:bg-violet-500 rounded transition-colors"
              >
                <ChevronDown
                  className={clsx(
                    'h-4 w-4 transition-transform',
                    isMinimized && 'rotate-180'
                  )}
                />
              </button>
              <button
                onClick={() => {
                  setIsOpen(false);
                  setIsMinimized(false);
                }}
                className="p-1 hover:bg-violet-500 rounded transition-colors"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>

          {!isMinimized && (
            <>
              {/* Messages */}
              <div className="h-80 overflow-y-auto p-3 space-y-3">
                {messages.length === 0 ? (
                  <div className="text-center py-6">
                    <MessageSquare className="h-10 w-10 text-violet-400 mx-auto mb-3" />
                    <p className="text-sm text-primary font-medium mb-2">
                      Create widgets with AI
                    </p>
                    <p className="text-xs text-secondary mb-4">
                      Describe the widget you want to create
                    </p>
                    <div className="space-y-2">
                      {examplePrompts.map((prompt, idx) => (
                        <button
                          key={idx}
                          onClick={() => setInputValue(prompt)}
                          className="block w-full text-left text-xs p-2 border border-default rounded hover:border-violet-300 hover:bg-violet-50 dark:hover:bg-violet-900/20 transition-colors"
                        >
                          {prompt}
                        </button>
                      ))}
                    </div>
                  </div>
                ) : (
                  <>
                    {messages.map((message) => (
                      <div
                        key={message.id}
                        className={clsx(
                          'flex',
                          message.type === 'user' ? 'justify-end' : 'justify-start'
                        )}
                      >
                        <div
                          className={clsx(
                            'max-w-[85%] rounded-lg p-2.5 text-sm',
                            message.type === 'user'
                              ? 'bg-violet-600 text-white'
                              : 'bg-hover text-primary'
                          )}
                        >
                          <p className="whitespace-pre-wrap">{message.content}</p>

                          {/* Widget Suggestion Preview */}
                          {message.suggestion && (
                            <div className="mt-2 p-2 bg-white/10 rounded border border-white/20">
                              <div className="flex items-center gap-2 mb-1">
                                <LayoutGrid className="h-4 w-4" />
                                <span className="font-medium text-xs">
                                  {message.suggestion.name}
                                </span>
                              </div>
                              <div className="text-xs opacity-80">
                                <p>Type: {message.suggestion.type}</p>
                                <p>Device: {message.suggestion.deviceName || message.suggestion.deviceId}</p>
                                <p>Variable: {message.suggestion.variableName}</p>
                              </div>
                            </div>
                          )}

                          {message.metadata?.latencyMs && (
                            <p className="text-xs opacity-60 mt-1">
                              {message.metadata.latencyMs}ms
                            </p>
                          )}
                        </div>
                      </div>
                    ))}

                    {/* Loading indicator */}
                    {loading && (
                      <div className="flex justify-start">
                        <div className="bg-hover rounded-lg p-2.5 flex items-center gap-2">
                          <Loader2 className="h-4 w-4 animate-spin text-violet-500" />
                          <span className="text-xs text-secondary">Thinking...</span>
                        </div>
                      </div>
                    )}

                    <div ref={messagesEndRef} />
                  </>
                )}
              </div>

              {/* Widget Creation Actions */}
              {pendingSuggestion && (
                <div className="px-3 py-2 border-t border-default bg-violet-50 dark:bg-violet-900/20">
                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleCreateWidget}
                      disabled={creatingWidget}
                      className="flex-1 flex items-center justify-center gap-2 px-3 py-2 bg-violet-600 text-white text-sm rounded-lg hover:bg-violet-700 disabled:opacity-50 transition-colors"
                    >
                      {creatingWidget ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Check className="h-4 w-4" />
                      )}
                      Create Widget
                    </button>
                    <button
                      onClick={handleCancelWidget}
                      disabled={creatingWidget}
                      className="px-3 py-2 text-sm border border-default rounded-lg hover:bg-hover transition-colors"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              )}

              {/* Input */}
              <div className="p-3 border-t border-default">
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
                    placeholder="Describe a widget..."
                    className="flex-1 px-3 py-2 text-sm border border-default rounded-lg bg-primary text-primary focus:ring-2 focus:ring-violet-500 focus:border-violet-500"
                    disabled={loading || creatingWidget}
                  />
                  <button
                    onClick={handleSend}
                    disabled={loading || !inputValue.trim() || creatingWidget}
                    className="p-2 bg-violet-600 text-white rounded-lg hover:bg-violet-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                    <Send className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      )}
    </>
  );
};

export default WidgetAssistantChat;
