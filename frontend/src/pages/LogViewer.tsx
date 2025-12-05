import React, { useState, useEffect, useRef, useMemo, CSSProperties, ReactElement } from 'react';
import { List, ListImperativeAPI } from 'react-window';
import {
  Terminal,
  Wifi,
  WifiOff,
  Trash2,
  Download,
  Pause,
  Play,
  Search,
  Filter,
  Server,
  Database,
  Radio,
  ChevronDown,
  X,
  AlertCircle,
  Copy,
  ChevronRight,
} from 'lucide-react';
import { LogsWebSocketProvider, useLogsWebSocket } from '../contexts/LogsWebSocketContext';
import { LogEntry, LogSource, LogLevel } from '../types';

const LOG_LEVEL_COLORS: Record<LogLevel, string> = {
  DEBUG: 'text-gray-400',
  INFO: 'text-green-400',
  WARN: 'text-yellow-400',
  ERROR: 'text-red-400',
  FATAL: 'text-red-600 font-bold',
};

const LOG_SOURCE_ICONS: Record<LogSource, React.ReactNode> = {
  backend: <Server className="w-4 h-4" />,
  mosquitto: <Radio className="w-4 h-4" />,
  postgres: <Database className="w-4 h-4" />,
};

const LOG_SOURCE_COLORS: Record<LogSource, string> = {
  backend: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  mosquitto: 'bg-purple-500/20 text-purple-400 border-purple-500/30',
  postgres: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
};

interface LogRowProps {
  logs: LogEntry[];
  searchTerm: string;
  expandedRows: Set<number>;
  onToggleExpand: (index: number) => void;
  onCopyLog: (log: LogEntry) => void;
}

interface LogRowComponentProps extends LogRowProps {
  index: number;
  style: CSSProperties;
}

const LogRow = ({ index, style, logs, searchTerm, expandedRows, onToggleExpand, onCopyLog }: LogRowComponentProps): ReactElement => {
  const log = logs[index];
  const isExpanded = expandedRows.has(index);
  const messageRef = useRef<HTMLSpanElement>(null);
  const [isTruncated, setIsTruncated] = useState(false);

  // Check if message is truncated
  useEffect(() => {
    if (messageRef.current && !isExpanded) {
      setIsTruncated(messageRef.current.scrollWidth > messageRef.current.clientWidth);
    }
  }, [log.message, isExpanded]);

  const highlightText = (text: string, term: string) => {
    if (!term) return text;
    // Escape regex special characters to prevent injection
    const escapedTerm = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const parts = text.split(new RegExp(`(${escapedTerm})`, 'gi'));
    return parts.map((part, i) =>
      part.toLowerCase() === term.toLowerCase() ? (
        <mark key={i} className="bg-yellow-500/50 text-white rounded px-0.5">
          {part}
        </mark>
      ) : (
        part
      )
    );
  };

  const formatTimestamp = (timestamp: string) => {
    try {
      const date = new Date(timestamp);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffSec = Math.floor(diffMs / 1000);

      // Handle negative diff (future timestamps due to clock skew) - fall back to absolute time
      if (diffSec < 0) {
        return date.toLocaleTimeString('en-US', {
          hour12: false,
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
        } as Intl.DateTimeFormatOptions);
      }

      // Show relative time for recent logs
      if (diffSec < 60) return `${diffSec}s ago`;
      if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;

      // Show time for today's logs
      return date.toLocaleTimeString('en-US', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      } as Intl.DateTimeFormatOptions);
    } catch {
      return timestamp.substring(0, 12);
    }
  };

  const getFullTimestamp = (timestamp: string) => {
    try {
      return new Date(timestamp).toISOString();
    } catch {
      return timestamp;
    }
  };

  const getLevelBadgeClass = (level: LogLevel) => {
    switch (level) {
      case 'ERROR':
      case 'FATAL':
        return 'bg-red-500/20 text-red-400 border border-red-500/30';
      case 'WARN':
        return 'bg-yellow-500/20 text-yellow-400 border border-yellow-500/30';
      case 'INFO':
        return 'bg-green-500/20 text-green-400 border border-green-500/30';
      default:
        return 'bg-gray-500/20 text-gray-400 border border-gray-500/30';
    }
  };

  return (
    <div
      style={style}
      className={`group flex items-start gap-2 px-4 py-1 hover:bg-gray-700/50 font-mono text-sm transition-colors ${
        index % 2 === 0 ? 'bg-gray-900/40' : 'bg-gray-900/0'
      }`}
    >
      {/* Expand/collapse indicator for truncated messages */}
      <button
        onClick={() => onToggleExpand(index)}
        className={`shrink-0 w-4 h-4 flex items-center justify-center text-gray-500 hover:text-gray-300 focus-visible:text-gray-300 transition-transform focus:outline-none focus-visible:ring-1 focus-visible:ring-cyan-500 rounded ${
          isExpanded ? 'rotate-90' : ''
        } ${isTruncated || isExpanded ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}
        title={isExpanded ? 'Collapse' : 'Expand'}
        aria-label={isExpanded ? 'Collapse log message' : 'Expand log message'}
        aria-expanded={isExpanded}
        tabIndex={isTruncated || isExpanded ? 0 : -1}
      >
        <ChevronRight className="w-3 h-3" aria-hidden="true" />
      </button>

      {/* Timestamp */}
      <span
        className="text-gray-500 shrink-0 w-16 cursor-help"
        title={getFullTimestamp(log.timestamp)}
      >
        {formatTimestamp(log.timestamp)}
      </span>

      {/* Source badge */}
      <span
        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs border shrink-0 w-24 justify-center ${LOG_SOURCE_COLORS[log.source]}`}
      >
        {LOG_SOURCE_ICONS[log.source]}
        {log.source}
      </span>

      {/* Level badge */}
      <span className={`shrink-0 text-xs font-medium px-2 py-0.5 rounded ${getLevelBadgeClass(log.level)}`}>
        {log.level}
      </span>

      {/* Message */}
      <div className="flex-1 min-w-0 relative">
        <span
          ref={messageRef}
          className={`text-gray-200 ${
            isExpanded
              ? 'whitespace-pre-wrap break-all'
              : 'whitespace-nowrap overflow-hidden text-ellipsis block'
          }`}
        >
          {highlightText(log.message, searchTerm)}
        </span>
      </div>

      {/* Copy button */}
      <button
        onClick={() => onCopyLog(log)}
        className="shrink-0 opacity-0 group-hover:opacity-100 focus-visible:opacity-100 p-1 hover:bg-gray-600 focus-visible:bg-gray-600 rounded transition-opacity focus:outline-none focus-visible:ring-1 focus-visible:ring-cyan-500"
        title="Copy log entry"
        aria-label="Copy log entry to clipboard"
      >
        <Copy className="w-3 h-3 text-gray-400" aria-hidden="true" />
      </button>
    </div>
  );
};

const LogViewerContent: React.FC = () => {
  const {
    logs,
    connectionStatus,
    isConnected,
    dockerAvailable,
    subscribedSources,
    subscribe,
    unsubscribe,
    clearLogs,
    error,
  } = useLogsWebSocket();

  const [selectedSources, setSelectedSources] = useState<LogSource[]>(['backend']);
  const [selectedLevels, setSelectedLevels] = useState<LogLevel[]>(['DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL']);
  const [searchTerm, setSearchTerm] = useState('');
  const [autoScroll, setAutoScroll] = useState(true);
  const [isPaused, setIsPaused] = useState(false);
  const [showFilters, setShowFilters] = useState(false);
  const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set());
  const [copyFeedback, setCopyFeedback] = useState<string | null>(null);

  const listRef = useRef<ListImperativeAPI>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [listHeight, setListHeight] = useState(500);

  // Update list height on resize
  useEffect(() => {
    const updateHeight = () => {
      if (containerRef.current) {
        const rect = containerRef.current.getBoundingClientRect();
        setListHeight(rect.height - 4); // Account for border
      }
    };

    updateHeight();
    window.addEventListener('resize', updateHeight);
    return () => window.removeEventListener('resize', updateHeight);
  }, []);

  // Filter logs
  const filteredLogs = useMemo(() => {
    return logs.filter(log => {
      // Source filter
      if (!selectedSources.includes(log.source)) return false;

      // Level filter
      if (!selectedLevels.includes(log.level)) return false;

      // Search filter
      if (searchTerm && !log.message.toLowerCase().includes(searchTerm.toLowerCase())) {
        return false;
      }

      return true;
    });
  }, [logs, selectedSources, selectedLevels, searchTerm]);

  // Clear expanded rows when filters change (indices become stale)
  useEffect(() => {
    setExpandedRows(new Set());
  }, [selectedSources, selectedLevels, searchTerm]);

  // Subscribe to selected sources when they change
  useEffect(() => {
    if (isConnected && selectedSources.length > 0 && !isPaused) {
      subscribe(selectedSources);
    }
  }, [isConnected, selectedSources, subscribe, isPaused]);

  // Auto-scroll to bottom when new logs arrive
  useEffect(() => {
    if (autoScroll && listRef.current && filteredLogs.length > 0) {
      listRef.current.scrollToRow({ index: filteredLogs.length - 1, align: 'end' });
    }
  }, [filteredLogs.length, autoScroll, listRef]);

  const handleSourceToggle = (source: LogSource) => {
    setSelectedSources(prev =>
      prev.includes(source) ? prev.filter(s => s !== source) : [...prev, source]
    );
  };

  const handleLevelToggle = (level: LogLevel) => {
    setSelectedLevels(prev =>
      prev.includes(level) ? prev.filter(l => l !== level) : [...prev, level]
    );
  };

  const handlePauseToggle = () => {
    if (isPaused) {
      // Resume - resubscribe
      subscribe(selectedSources);
    } else {
      // Pause - unsubscribe
      unsubscribe();
    }
    setIsPaused(!isPaused);
  };

  const handleExport = () => {
    const content = filteredLogs
      .map(log => `${log.timestamp} [${log.source}] [${log.level}] ${log.message}`)
      .join('\n');

    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `sensorvision-logs-${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const getConnectionStatusColor = () => {
    switch (connectionStatus) {
      case 'Open':
        return 'text-green-400';
      case 'Connecting':
        return 'text-yellow-400';
      default:
        return 'text-red-400';
    }
  };

  const handleToggleExpand = (index: number) => {
    setExpandedRows(prev => {
      const next = new Set(prev);
      if (next.has(index)) {
        next.delete(index);
      } else {
        next.add(index);
      }
      return next;
    });
  };

  const handleCopyLog = (log: LogEntry) => {
    const text = `${log.timestamp} [${log.source}] [${log.level}] ${log.message}`;
    navigator.clipboard.writeText(text)
      .then(() => {
        setCopyFeedback('Copied!');
        setTimeout(() => setCopyFeedback(null), 2000);
      })
      .catch((err) => {
        console.error('Failed to copy to clipboard:', err);
        setCopyFeedback('Copy failed');
        setTimeout(() => setCopyFeedback(null), 2000);
      });
  };

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      if (e.metaKey || e.ctrlKey) {
        switch (e.key) {
          case 'k': // Cmd+K to focus search
            e.preventDefault();
            searchInputRef.current?.focus();
            break;
        }
      }
      // Only clear search if the search input is focused
      if (e.key === 'Escape' && document.activeElement === searchInputRef.current) {
        setSearchTerm('');
        searchInputRef.current?.blur();
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, []);

  return (
    <div className="space-y-4 h-full flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Terminal className="w-6 h-6 text-cyan-500" />
          <h1 className="text-2xl font-bold text-white">System Logs</h1>
          <span
            className={`flex items-center gap-1 text-sm ${getConnectionStatusColor()}`}
          >
            {isConnected ? (
              <Wifi className="w-4 h-4" />
            ) : (
              <WifiOff className="w-4 h-4" />
            )}
            {connectionStatus}
          </span>
        </div>

        <div className="flex items-center gap-2">
          {/* Pause/Resume */}
          <button
            onClick={handlePauseToggle}
            className={`flex items-center gap-1 px-3 py-1.5 rounded text-sm ${
              isPaused
                ? 'bg-green-600 hover:bg-green-700 text-white'
                : 'bg-yellow-600 hover:bg-yellow-700 text-white'
            }`}
          >
            {isPaused ? <Play className="w-4 h-4" /> : <Pause className="w-4 h-4" />}
            {isPaused ? 'Resume' : 'Pause'}
          </button>

          {/* Export */}
          <button
            onClick={handleExport}
            disabled={filteredLogs.length === 0}
            className="flex items-center gap-1 px-3 py-1.5 rounded bg-gray-700 hover:bg-gray-600 text-white text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Download className="w-4 h-4" />
            Export
          </button>

          {/* Clear */}
          <button
            onClick={clearLogs}
            disabled={logs.length === 0}
            className="flex items-center gap-1 px-3 py-1.5 rounded bg-red-600 hover:bg-red-700 text-white text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Trash2 className="w-4 h-4" />
            Clear
          </button>
        </div>
      </div>

      {/* Error message */}
      {error && (
        <div className="flex items-center gap-2 p-3 bg-red-900/50 border border-red-500/50 rounded text-red-200">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
          <button
            onClick={() => window.location.reload()}
            className="ml-auto px-2 py-1 bg-red-600 hover:bg-red-700 rounded text-sm"
          >
            Reload
          </button>
        </div>
      )}

      {/* Filters bar */}
      <div className="flex flex-wrap items-center gap-4 p-3 bg-gray-800/50 rounded-lg border border-gray-700">
        {/* Source filters */}
        <div className="flex items-center gap-2">
          <span className="text-gray-400 text-sm">Sources:</span>
          {(['backend', 'mosquitto', 'postgres'] as LogSource[]).map(source => (
            <button
              key={source}
              onClick={() => handleSourceToggle(source)}
              disabled={source !== 'backend' && !dockerAvailable}
              className={`flex items-center gap-1 px-2 py-1 rounded text-xs border transition-colors ${
                selectedSources.includes(source)
                  ? LOG_SOURCE_COLORS[source]
                  : 'bg-gray-700/50 text-gray-400 border-gray-600 hover:border-gray-500'
              } ${source !== 'backend' && !dockerAvailable ? 'opacity-50 cursor-not-allowed' : ''}`}
              title={source !== 'backend' && !dockerAvailable ? 'Docker not available' : undefined}
            >
              {LOG_SOURCE_ICONS[source]}
              {source}
            </button>
          ))}
        </div>

        {/* Level filter dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="flex items-center gap-1 px-3 py-1.5 bg-gray-700 hover:bg-gray-600 rounded text-sm text-white"
          >
            <Filter className="w-4 h-4" />
            Levels ({selectedLevels.length})
            <ChevronDown className={`w-4 h-4 transition-transform ${showFilters ? 'rotate-180' : ''}`} />
          </button>

          {showFilters && (
            <div className="absolute top-full left-0 mt-1 p-2 bg-gray-800 border border-gray-700 rounded-lg shadow-xl z-10 min-w-32">
              {(['DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL'] as LogLevel[]).map(level => (
                <label
                  key={level}
                  className="flex items-center gap-2 px-2 py-1 hover:bg-gray-700 rounded cursor-pointer"
                >
                  <input
                    type="checkbox"
                    checked={selectedLevels.includes(level)}
                    onChange={() => handleLevelToggle(level)}
                    className="rounded border-gray-600"
                  />
                  <span className={LOG_LEVEL_COLORS[level]}>{level}</span>
                </label>
              ))}
            </div>
          )}
        </div>

        {/* Search */}
        <div className="flex-1 min-w-48 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            ref={searchInputRef}
            type="text"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            placeholder="Search logs... (Ctrl+K)"
            className="w-full pl-9 pr-8 py-1.5 bg-gray-700 border border-gray-600 rounded text-sm text-white placeholder-gray-400 focus:outline-none focus:border-cyan-500"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Auto-scroll toggle */}
        <label className="flex items-center gap-2 text-sm text-gray-300 cursor-pointer">
          <input
            type="checkbox"
            checked={autoScroll}
            onChange={e => setAutoScroll(e.target.checked)}
            className="rounded border-gray-600"
          />
          Auto-scroll
        </label>

        {/* Stats */}
        <div className="text-sm text-gray-400">
          {filteredLogs.length.toLocaleString()} / {logs.length.toLocaleString()} logs
        </div>
      </div>

      {/* Copy feedback toast */}
      {copyFeedback && (
        <div
          className={`fixed bottom-4 right-4 text-white px-4 py-2 rounded-lg shadow-lg z-50 animate-fadeIn ${
            copyFeedback === 'Copy failed' ? 'bg-red-600' : 'bg-green-600'
          }`}
          role="status"
          aria-live="polite"
        >
          {copyFeedback}
        </div>
      )}

      {/* Log display */}
      <div
        ref={containerRef}
        className="flex-1 bg-gray-900 border border-gray-700 rounded-lg overflow-hidden min-h-96"
      >
        {filteredLogs.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400">
            <Terminal className="w-12 h-12 mb-4 opacity-50" />
            {logs.length === 0 ? (
              <>
                <p className="text-lg">No logs yet</p>
                <p className="text-sm mt-1">
                  {isConnected
                    ? 'Waiting for log entries...'
                    : 'Connecting to log stream...'}
                </p>
              </>
            ) : (
              <>
                <p className="text-lg">No matching logs</p>
                <p className="text-sm mt-1">Try adjusting your filters</p>
              </>
            )}
          </div>
        ) : (
          <List<LogRowProps>
            listRef={listRef}
            defaultHeight={listHeight}
            rowCount={filteredLogs.length}
            rowHeight={32}
            rowComponent={(props) => (
              <LogRow
                {...props}
                logs={filteredLogs}
                searchTerm={searchTerm}
                expandedRows={expandedRows}
                onToggleExpand={handleToggleExpand}
                onCopyLog={handleCopyLog}
              />
            )}
            rowProps={{
              logs: filteredLogs,
              searchTerm,
              expandedRows,
              onToggleExpand: handleToggleExpand,
              onCopyLog: handleCopyLog
            }}
          />
        )}
      </div>

      {/* Footer info */}
      <div className="flex items-center justify-between text-xs text-gray-500">
        <span>
          Subscribed to: {subscribedSources.length > 0 ? subscribedSources.join(', ') : 'none'}
        </span>
        <span>
          {dockerAvailable ? 'Docker available' : 'Docker not available (container logs unavailable)'}
        </span>
      </div>
    </div>
  );
};

const LogViewer: React.FC = () => {
  return (
    <LogsWebSocketProvider>
      <LogViewerContent />
    </LogsWebSocketProvider>
  );
};

export default LogViewer;
