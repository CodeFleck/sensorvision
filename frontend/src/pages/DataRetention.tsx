import React, { useEffect, useState } from 'react';
import dataRetentionService, {
  DataRetentionPolicy,
  ArchiveExecution,
  CreateRetentionPolicyRequest,
} from '../services/dataRetentionService';

const formatBytes = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
};

const formatDate = (dateString?: string): string => {
  if (!dateString) return 'Never';
  return new Date(dateString).toLocaleString();
};

const DataRetention: React.FC = () => {
  const [policy, setPolicy] = useState<DataRetentionPolicy | null>(null);
  const [executions, setExecutions] = useState<ArchiveExecution[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [executing, setExecuting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [enabled, setEnabled] = useState(true);
  const [retentionDays, setRetentionDays] = useState(90);
  const [archiveEnabled, setArchiveEnabled] = useState(true);
  const [storageType, setStorageType] = useState<'LOCAL_FILE' | 'S3' | 'AZURE_BLOB' | 'GCS'>('LOCAL_FILE');
  const [scheduleCron, setScheduleCron] = useState('0 2 * * *');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Load policy
      try {
        const policyData = await dataRetentionService.getPolicy();
        setPolicy(policyData);
        setEnabled(policyData.enabled);
        setRetentionDays(policyData.retentionDays);
        setArchiveEnabled(policyData.archiveEnabled);
        setStorageType(policyData.archiveStorageType);
        setScheduleCron(policyData.archiveScheduleCron);
      } catch (err: any) {
        if (err.response?.status !== 404) {
          throw err;
        }
        // No policy exists yet, use defaults
      }

      // Load execution history
      const executionsData = await dataRetentionService.getExecutions(0, 10);
      setExecutions(executionsData.content);
    } catch (err: any) {
      console.error('Error loading data retention data:', err);
      setError(err.response?.data?.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      setError(null);

      const request: CreateRetentionPolicyRequest = {
        enabled,
        retentionDays,
        archiveEnabled,
        archiveStorageType: storageType,
        archiveScheduleCron: scheduleCron,
      };

      const savedPolicy = await dataRetentionService.createOrUpdatePolicy(request);
      setPolicy(savedPolicy);

      alert('Data retention policy saved successfully!');
    } catch (err: any) {
      console.error('Error saving policy:', err);
      setError(err.response?.data?.message || 'Failed to save policy');
    } finally {
      setSaving(false);
    }
  };

  const handleExecute = async () => {
    if (!confirm('Are you sure you want to execute archival now? This will process all eligible records.')) {
      return;
    }

    try {
      setExecuting(true);
      setError(null);

      await dataRetentionService.executeArchival();
      alert('Archival execution started successfully!');

      // Reload data after short delay
      setTimeout(() => loadData(), 2000);
    } catch (err: any) {
      console.error('Error executing archival:', err);
      setError(err.response?.data?.message || 'Failed to execute archival');
    } finally {
      setExecuting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading data retention settings...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Data Retention & Archival</h1>
        <p className="text-gray-600 mt-2">
          Configure automatic archival of old telemetry data to reduce database size and improve performance.
        </p>
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">
          {error}
        </div>
      )}

      {/* Policy Configuration */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Retention Policy</h2>

        <div className="space-y-4">
          {/* Enable/Disable */}
          <div className="flex items-center">
            <input
              type="checkbox"
              id="enabled"
              checked={enabled}
              onChange={(e) => setEnabled(e.target.checked)}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="enabled" className="ml-2 text-sm font-medium text-gray-700">
              Enable data retention policy
            </label>
          </div>

          {/* Retention Days */}
          <div>
            <label htmlFor="retentionDays" className="block text-sm font-medium text-gray-700 mb-1">
              Retention Period (days)
            </label>
            <input
              type="number"
              id="retentionDays"
              min="7"
              max="3650"
              value={retentionDays}
              onChange={(e) => setRetentionDays(parseInt(e.target.value))}
              disabled={!enabled}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
            />
            <p className="mt-1 text-sm text-gray-500">
              Keep telemetry data for this many days. Older data will be archived or deleted.
            </p>
          </div>

          {/* Archive Enable */}
          <div className="flex items-center">
            <input
              type="checkbox"
              id="archiveEnabled"
              checked={archiveEnabled}
              onChange={(e) => setArchiveEnabled(e.target.checked)}
              disabled={!enabled}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="archiveEnabled" className="ml-2 text-sm font-medium text-gray-700">
              Archive old data (if disabled, data will be permanently deleted)
            </label>
          </div>

          {/* Storage Type */}
          <div>
            <label htmlFor="storageType" className="block text-sm font-medium text-gray-700 mb-1">
              Archive Storage Type
            </label>
            <select
              id="storageType"
              value={storageType}
              onChange={(e) => setStorageType(e.target.value as any)}
              disabled={!enabled || !archiveEnabled}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
            >
              <option value="LOCAL_FILE">Local File System</option>
              <option value="S3" disabled>Amazon S3 (Coming Soon)</option>
              <option value="AZURE_BLOB" disabled>Azure Blob Storage (Coming Soon)</option>
              <option value="GCS" disabled>Google Cloud Storage (Coming Soon)</option>
            </select>
          </div>

          {/* Schedule Cron */}
          <div>
            <label htmlFor="scheduleCron" className="block text-sm font-medium text-gray-700 mb-1">
              Archival Schedule (Cron Expression)
            </label>
            <input
              type="text"
              id="scheduleCron"
              value={scheduleCron}
              onChange={(e) => setScheduleCron(e.target.value)}
              disabled={!enabled}
              placeholder="0 2 * * *"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
            />
            <p className="mt-1 text-sm text-gray-500">
              Default: 0 2 * * * (Daily at 2 AM)
            </p>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-3 pt-4">
            <button
              onClick={handleSave}
              disabled={saving || !enabled}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              {saving ? 'Saving...' : 'Save Policy'}
            </button>
            <button
              onClick={handleExecute}
              disabled={executing || !policy || !policy.enabled}
              className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              {executing ? 'Executing...' : 'Execute Now'}
            </button>
          </div>
        </div>
      </div>

      {/* Policy Statistics */}
      {policy && (
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">Statistics</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-gray-50 p-4 rounded">
              <p className="text-sm text-gray-600">Total Records Archived</p>
              <p className="text-2xl font-bold text-gray-900">
                {policy.totalRecordsArchived.toLocaleString()}
              </p>
            </div>
            <div className="bg-gray-50 p-4 rounded">
              <p className="text-sm text-gray-600">Total Archive Size</p>
              <p className="text-2xl font-bold text-gray-900">
                {formatBytes(policy.totalArchiveSizeBytes)}
              </p>
            </div>
            <div className="bg-gray-50 p-4 rounded">
              <p className="text-sm text-gray-600">Last Execution</p>
              <p className="text-lg font-semibold text-gray-900">
                {formatDate(policy.lastArchiveRun)}
              </p>
              {policy.lastArchiveStatus && (
                <p className={`text-sm mt-1 ${
                  policy.lastArchiveStatus === 'SUCCESS' ? 'text-green-600' :
                  policy.lastArchiveStatus === 'FAILED' ? 'text-red-600' :
                  'text-yellow-600'
                }`}>
                  {policy.lastArchiveStatus}
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Execution History */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold mb-4">Execution History</h2>
        {executions.length === 0 ? (
          <p className="text-gray-600">No archival executions yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Started At
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Records
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Size
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Duration
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {executions.map((execution) => (
                  <tr key={execution.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatDate(execution.startedAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-1 text-xs font-semibold rounded ${
                        execution.status === 'SUCCESS' ? 'bg-green-100 text-green-800' :
                        execution.status === 'FAILED' ? 'bg-red-100 text-red-800' :
                        'bg-yellow-100 text-yellow-800'
                      }`}>
                        {execution.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {execution.recordsArchived.toLocaleString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatBytes(execution.archiveSizeBytes)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {execution.durationMs ? `${execution.durationMs}ms` : '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default DataRetention;
