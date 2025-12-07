import React, { useState, useEffect, useCallback } from 'react';
import { apiService } from '../services/api';
import { DeviceVariable } from '../types';

interface DeviceVariablesPanelProps {
  deviceId: string;
  deviceExternalId: string;
  onClose?: () => void;
}

/**
 * Panel component that displays all dynamic variables for a device.
 * Variables are auto-provisioned when telemetry is received (EAV pattern).
 */
export const DeviceVariablesPanel: React.FC<DeviceVariablesPanelProps> = ({
  deviceId,
  deviceExternalId,
  onClose,
}) => {
  const [variables, setVariables] = useState<DeviceVariable[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadVariables = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiService.getDeviceVariables(deviceId);
      setVariables(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load variables');
    } finally {
      setLoading(false);
    }
  }, [deviceId]);

  useEffect(() => {
    loadVariables();
  }, [loadVariables]);

  const formatValue = (value: number | undefined, decimalPlaces: number, unit?: string): string => {
    if (value === undefined || value === null) return '-';
    const formatted = value.toFixed(decimalPlaces);
    return unit ? `${formatted} ${unit}` : formatted;
  };

  const formatTimestamp = (timestamp: string | undefined): string => {
    if (!timestamp) return 'Never';
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  const getSourceBadgeColor = (source: string): string => {
    switch (source) {
      case 'AUTO': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
      case 'MANUAL': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
      case 'SYNTHETIC': return 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200';
    }
  };

  if (loading) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
        <div className="flex items-center justify-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
          <span className="ml-3 text-gray-600 dark:text-gray-400">Loading variables...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
        <div className="text-red-500 dark:text-red-400 text-center py-4">
          <p>{error}</p>
          <button
            onClick={loadVariables}
            className="mt-2 text-blue-500 hover:text-blue-600 underline"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Device Variables
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {deviceExternalId} • {variables.length} variable{variables.length !== 1 ? 's' : ''}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={loadVariables}
            className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
            title="Refresh"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
          {onClose && (
            <button
              onClick={onClose}
              className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          )}
        </div>
      </div>

      {/* Variables List */}
      {variables.length === 0 ? (
        <div className="p-8 text-center">
          <div className="text-gray-400 dark:text-gray-500 mb-2">
            <svg className="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          <p className="text-gray-600 dark:text-gray-400">No variables yet</p>
          <p className="text-sm text-gray-500 dark:text-gray-500 mt-1">
            Variables will appear automatically when the device sends telemetry data
          </p>
        </div>
      ) : (
        <div className="divide-y divide-gray-200 dark:divide-gray-700">
          {variables.map((variable) => (
            <div
              key={variable.id}
              className="p-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-900 dark:text-white">
                      {variable.displayName}
                    </span>
                    <span className={`text-xs px-2 py-0.5 rounded-full ${getSourceBadgeColor(variable.dataSource)}`}>
                      {variable.dataSource.toLowerCase()}
                    </span>
                  </div>
                  <div className="flex items-center gap-3 mt-1 text-sm text-gray-500 dark:text-gray-400">
                    <code className="text-xs bg-gray-100 dark:bg-gray-700 px-1.5 py-0.5 rounded">
                      {variable.name}
                    </code>
                    {variable.unit && (
                      <span>Unit: {variable.unit}</span>
                    )}
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-2xl font-semibold text-gray-900 dark:text-white">
                    {formatValue(variable.lastValue, variable.decimalPlaces, variable.unit)}
                  </div>
                  <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    {formatTimestamp(variable.lastValueAt)}
                  </div>
                </div>
              </div>
              {variable.description && (
                <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
                  {variable.description}
                </p>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Footer with info */}
      {variables.length > 0 && (
        <div className="p-3 bg-gray-50 dark:bg-gray-700/50 border-t border-gray-200 dark:border-gray-700 text-xs text-gray-500 dark:text-gray-400">
          <div className="flex items-center gap-1">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>Variables are auto-created when new telemetry data is received</span>
          </div>
        </div>
      )}
    </div>
  );
};

export default DeviceVariablesPanel;
