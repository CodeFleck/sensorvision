import React, { useState, useEffect } from 'react';
import { 
  ChartBarIcon, 
  CpuChipIcon, 
  CircleStackIcon, 
  ClockIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ArrowPathIcon
} from '@heroicons/react/24/outline';

interface PerformanceMetrics {
  database: {
    activeConnections: number;
    cacheHitRatio: number;
    databaseSizeMB: number;
    tableSizesMB: Record<string, number>;
  };
  redis: {
    usedMemory: string;
    connectedClients: string;
    operationsPerSecond: string;
    cacheHitRatio: number;
  };
  application: {
    jvmMemoryUsedMB: number;
    jvmMemoryMaxMB: number;
    jvmMemoryUsagePercent: number;
    activeThreads: number;
    uptimeHours: number;
  };
  pilot: {
    telemetryIngestionsLastHour: number;
    avgTelemetryProcessingTimeSeconds: number;
    activeDevicesLast24h: number;
    alertsLast24h: number;
  };
  system: {
    availableProcessors: number;
    javaVersion: string;
    osName: string;
    metricsCollectedAt: string;
  };
}

interface SystemHealth {
  healthy: boolean;
  status: string;
  checks: {
    database: { healthy: boolean; message: string };
    redis: { healthy: boolean; message: string };
    memory: { healthy: boolean; message: string };
    performance: { healthy: boolean; message: string };
  };
}

interface CacheStats {
  [cacheName: string]: {
    hitRatio: number;
    size: number;
    evictions: number;
  };
}

const PilotPerformanceDashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<PerformanceMetrics | null>(null);
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [cacheStats, setCacheStats] = useState<CacheStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const fetchMetrics = async () => {
    try {
      const [metricsRes, healthRes, cacheRes] = await Promise.all([
        fetch('/api/v1/pilot/performance/metrics'),
        fetch('/api/v1/pilot/performance/health'),
        fetch('/api/v1/pilot/performance/cache/stats')
      ]);

      if (metricsRes.ok) {
        setMetrics(await metricsRes.json());
      }
      if (healthRes.ok) {
        setHealth(await healthRes.json());
      }
      if (cacheRes.ok) {
        setCacheStats(await cacheRes.json());
      }

      setError(null);
    } catch (err) {
      setError('Failed to fetch performance metrics');
      console.error('Error fetching metrics:', err);
    } finally {
      setLoading(false);
    }
  };

  const clearCache = async (cacheName?: string) => {
    try {
      const url = cacheName 
        ? `/api/v1/pilot/performance/cache/${cacheName}`
        : '/api/v1/pilot/performance/cache';
      
      const response = await fetch(url, { method: 'DELETE' });
      
      if (response.ok) {
        // Refresh cache stats after clearing
        fetchMetrics();
      }
    } catch (err) {
      console.error('Error clearing cache:', err);
    }
  };

  const flushTelemetryBatches = async () => {
    try {
      const response = await fetch('/api/v1/pilot/performance/telemetry/flush', {
        method: 'POST'
      });
      
      if (response.ok) {
        fetchMetrics();
      }
    } catch (err) {
      console.error('Error flushing telemetry batches:', err);
    }
  };

  useEffect(() => {
    fetchMetrics();
  }, []);

  useEffect(() => {
    if (autoRefresh) {
      const interval = setInterval(fetchMetrics, 30000); // Refresh every 30 seconds
      return () => clearInterval(interval);
    }
  }, [autoRefresh]);

  const getHealthStatusColor = (healthy: boolean) => {
    return healthy ? 'text-green-600' : 'text-red-600';
  };

  const getHealthStatusIcon = (healthy: boolean) => {
    return healthy ? CheckCircleIcon : ExclamationTriangleIcon;
  };

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <ArrowPathIcon className="h-8 w-8 animate-spin text-blue-600" />
        <span className="ml-2 text-gray-600">Loading performance metrics...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-md p-4">
        <div className="flex">
          <ExclamationTriangleIcon className="h-5 w-5 text-red-400" />
          <div className="ml-3">
            <h3 className="text-sm font-medium text-red-800">Error</h3>
            <p className="text-sm text-red-700 mt-1">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Pilot Performance Dashboard</h1>
        <div className="flex items-center space-x-4">
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            <span className="ml-2 text-sm text-gray-700">Auto-refresh</span>
          </label>
          <button
            onClick={fetchMetrics}
            className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            <ArrowPathIcon className="h-4 w-4 mr-2" />
            Refresh
          </button>
        </div>
      </div>

      {/* System Health Overview */}
      {health && (
        <div className="bg-white shadow rounded-lg p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-medium text-gray-900">System Health</h2>
            <div className={`flex items-center ${getHealthStatusColor(health.healthy)}`}>
              {React.createElement(getHealthStatusIcon(health.healthy), { className: "h-5 w-5 mr-2" })}
              <span className="font-medium">{health.status}</span>
            </div>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {Object.entries(health.checks).map(([key, check]) => (
              <div key={key} className="border rounded-lg p-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-900 capitalize">{key}</span>
                  {React.createElement(getHealthStatusIcon(check.healthy), { 
                    className: `h-4 w-4 ${getHealthStatusColor(check.healthy)}` 
                  })}
                </div>
                <p className="text-xs text-gray-600 mt-1">{check.message}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Key Performance Metrics */}
      {metrics && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {/* Database Performance */}
          <div className="bg-white shadow rounded-lg p-6">
            <div className="flex items-center">
              <CircleStackIcon className="h-8 w-8 text-blue-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Database</p>
                <p className="text-2xl font-semibold text-gray-900">
                  {metrics.database.cacheHitRatio.toFixed(1)}%
                </p>
                <p className="text-xs text-gray-500">Cache Hit Ratio</p>
              </div>
            </div>
            <div className="mt-4 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Active Connections</span>
                <span className="font-medium">{metrics.database.activeConnections}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Database Size</span>
                <span className="font-medium">{metrics.database.databaseSizeMB} MB</span>
              </div>
            </div>
          </div>

          {/* Memory Usage */}
          <div className="bg-white shadow rounded-lg p-6">
            <div className="flex items-center">
              <CpuChipIcon className="h-8 w-8 text-green-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">JVM Memory</p>
                <p className="text-2xl font-semibold text-gray-900">
                  {metrics.application.jvmMemoryUsagePercent.toFixed(1)}%
                </p>
                <p className="text-xs text-gray-500">
                  {metrics.application.jvmMemoryUsedMB} / {metrics.application.jvmMemoryMaxMB} MB
                </p>
              </div>
            </div>
            <div className="mt-4">
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div 
                  className="bg-green-600 h-2 rounded-full" 
                  style={{ width: `${metrics.application.jvmMemoryUsagePercent}%` }}
                ></div>
              </div>
            </div>
          </div>

          {/* Telemetry Performance */}
          <div className="bg-white shadow rounded-lg p-6">
            <div className="flex items-center">
              <ChartBarIcon className="h-8 w-8 text-purple-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Telemetry</p>
                <p className="text-2xl font-semibold text-gray-900">
                  {metrics.pilot.telemetryIngestionsLastHour.toLocaleString()}
                </p>
                <p className="text-xs text-gray-500">Points/Hour</p>
              </div>
            </div>
            <div className="mt-4 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Avg Processing Time</span>
                <span className="font-medium">{metrics.pilot.avgTelemetryProcessingTimeSeconds.toFixed(3)}s</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Active Devices</span>
                <span className="font-medium">{metrics.pilot.activeDevicesLast24h}</span>
              </div>
            </div>
          </div>

          {/* System Uptime */}
          <div className="bg-white shadow rounded-lg p-6">
            <div className="flex items-center">
              <ClockIcon className="h-8 w-8 text-orange-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">System Uptime</p>
                <p className="text-2xl font-semibold text-gray-900">
                  {metrics.application.uptimeHours}h
                </p>
                <p className="text-xs text-gray-500">
                  {metrics.application.activeThreads} Active Threads
                </p>
              </div>
            </div>
            <div className="mt-4 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">CPU Cores</span>
                <span className="font-medium">{metrics.system.availableProcessors}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Java Version</span>
                <span className="font-medium">{metrics.system.javaVersion}</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Cache Performance */}
      {cacheStats && (
        <div className="bg-white shadow rounded-lg p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-medium text-gray-900">Cache Performance</h2>
            <button
              onClick={() => clearCache()}
              className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
            >
              Clear All Caches
            </button>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {Object.entries(cacheStats).map(([cacheName, stats]) => (
              <div key={cacheName} className="border rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-medium text-gray-900">{cacheName}</h3>
                  <button
                    onClick={() => clearCache(cacheName)}
                    className="text-xs text-red-600 hover:text-red-800"
                  >
                    Clear
                  </button>
                </div>
                <div className="space-y-1">
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Hit Ratio</span>
                    <span className="font-medium">{stats.hitRatio.toFixed(1)}%</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Size</span>
                    <span className="font-medium">{stats.size}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Evictions</span>
                    <span className="font-medium">{stats.evictions}</span>
                  </div>
                </div>
                <div className="mt-2">
                  <div className="w-full bg-gray-200 rounded-full h-1">
                    <div 
                      className="bg-blue-600 h-1 rounded-full" 
                      style={{ width: `${stats.hitRatio}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Database Table Sizes */}
      {metrics?.database.tableSizesMB && (
        <div className="bg-white shadow rounded-lg p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Database Table Sizes</h2>
          <div className="space-y-3">
            {Object.entries(metrics.database.tableSizesMB)
              .sort(([,a], [,b]) => b - a)
              .map(([tableName, sizeMB]) => (
                <div key={tableName} className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-900">{tableName}</span>
                  <div className="flex items-center">
                    <div className="w-32 bg-gray-200 rounded-full h-2 mr-3">
                      <div 
                        className="bg-blue-600 h-2 rounded-full" 
                        style={{ 
                          width: `${Math.min((sizeMB / Math.max(...Object.values(metrics.database.tableSizesMB))) * 100, 100)}%` 
                        }}
                      ></div>
                    </div>
                    <span className="text-sm text-gray-600 w-16 text-right">{sizeMB} MB</span>
                  </div>
                </div>
              ))}
          </div>
        </div>
      )}

      {/* Quick Actions */}
      <div className="bg-white shadow rounded-lg p-6">
        <h2 className="text-lg font-medium text-gray-900 mb-4">Quick Actions</h2>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={flushTelemetryBatches}
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Flush Telemetry Batches
          </button>
          <button
            onClick={() => clearCache()}
            className="inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500"
          >
            Clear All Caches
          </button>
          <a
            href="/api/v1/pilot/performance/recommendations"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500"
          >
            View Recommendations
          </a>
        </div>
      </div>

      {/* Last Updated */}
      {metrics && (
        <div className="text-center text-sm text-gray-500">
          Last updated: {new Date(metrics.system.metricsCollectedAt).toLocaleString()}
        </div>
      )}
    </div>
  );
};

export default PilotPerformanceDashboard;