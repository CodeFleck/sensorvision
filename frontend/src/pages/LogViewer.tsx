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
}

interface LogRowComponentProps extends LogRowProps {
  index: number;
  style: CSSProperties;
}

const LogRow = ({ index, style, logs, searchTerm }: LogRowComponentProps): ReactElement => {
  const log = logs[index];

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

  return (
    <div
      style={style}
      className="flex items-start gap-2 px-4 py-1 hover:bg-gray-800/50 border-b border-gray-800/50 font-mono text-sm"
    >
      {/* Timestamp */}
      <span className="text-gray-500 shrink-0 w-24">
        {formatTimestamp(log.timestamp)}
      </span>

      {/* Source badge */}
      <span
        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs border shrink-0 w-24 justify-center ${LOG_SOURCE_COLORS[log.source]}`}
      >
        {LOG_SOURCE_ICONS[log.source]}
        {log.source}
      </span>

      {/* Level */}
      <span className={`shrink-0 w-14 ${LOG_LEVEL_COLORS[log.level]}`}>
        [{log.level}]
      </span>

      {/* Message */}
      <span className="text-gray-200 break-all flex-1 whitespace-pre-wrap">
        {highlightText(log.message, searchTerm)}
      </span>
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

  const listRef = useRef<ListImperativeAPI>(null);
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
            type="text"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            placeholder="Search logs..."
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
              />
            )}
            rowProps={{ logs: filteredLogs, searchTerm }}
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
