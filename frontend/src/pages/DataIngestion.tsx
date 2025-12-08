import React, { useState } from 'react';
import { apiService } from '../services/api';
import { config } from '../config';

interface Variable {
  name: string;
  value: string;
}

const DataIngestion: React.FC = () => {
  const [deviceId, setDeviceId] = useState('');
  const [timestamp, setTimestamp] = useState('');
  const [variables, setVariables] = useState<Variable[]>([{ name: '', value: '' }]);
  const [singleDeviceId, setSingleDeviceId] = useState('');
  const [singleVariableName, setSingleVariableName] = useState('');
  const [singleValue, setSingleValue] = useState('');
  const [response, setResponse] = useState<Record<string, unknown> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const addVariable = () => {
    setVariables([...variables, { name: '', value: '' }]);
  };

  const removeVariable = (index: number) => {
    setVariables(variables.filter((_, i) => i !== index));
  };

  const updateVariable = (index: number, field: 'name' | 'value', value: string) => {
    const updated = [...variables];
    updated[index][field] = value;
    setVariables(updated);
  };

  const handleFullIngestion = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      const variablesObj = variables.reduce((acc, v) => {
        if (v.name && v.value) {
          acc[v.name] = parseFloat(v.value);
        }
        return acc;
      }, {} as Record<string, number>);

      const payload = {
        deviceId,
        timestamp: timestamp || undefined,
        variables: variablesObj,
      };

      const result = await apiService.post('/data/ingest', payload);
      setResponse(result.data as Record<string, unknown>);
    } catch (err: unknown) {
      const message = err instanceof Error && 'response' in err
        ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to ingest data')
        : 'Failed to ingest data';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleSingleVariableIngestion = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      const result = await apiService.post(
        `/data/${singleDeviceId}/${singleVariableName}`,
        parseFloat(singleValue)
      );
      setResponse(result.data as Record<string, unknown>);
    } catch (err: unknown) {
      const message = err instanceof Error && 'response' in err
        ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to ingest data')
        : 'Failed to ingest data';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-8">HTTP Data Ingestion</h1>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Full Telemetry Ingestion */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-4">Full Telemetry Ingestion</h2>
          <form onSubmit={handleFullIngestion}>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Device ID
              </label>
              <input
                type="text"
                value={deviceId}
                onChange={(e) => setDeviceId(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="sensor-001"
                required
              />
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Timestamp (optional)
              </label>
              <input
                type="datetime-local"
                value={timestamp}
                onChange={(e) => setTimestamp(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="mb-4">
              <div className="flex justify-between items-center mb-2">
                <label className="block text-sm font-medium text-gray-700">
                  Variables
                </label>
                <button
                  type="button"
                  onClick={addVariable}
                  className="text-sm text-blue-600 hover:text-blue-800"
                >
                  + Add Variable
                </button>
              </div>
              {variables.map((variable, index) => (
                <div key={index} className="flex gap-2 mb-2">
                  <input
                    type="text"
                    value={variable.name}
                    onChange={(e) => updateVariable(index, 'name', e.target.value)}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Variable name"
                    required
                  />
                  <input
                    type="number"
                    step="any"
                    value={variable.value}
                    onChange={(e) => updateVariable(index, 'value', e.target.value)}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Value"
                    required
                  />
                  {variables.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeVariable(index)}
                      className="px-3 py-2 text-red-600 hover:text-red-800"
                    >
                      ×
                    </button>
                  )}
                </div>
              ))}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 disabled:bg-gray-400"
            >
              {loading ? 'Sending...' : 'Send Telemetry'}
            </button>
          </form>
        </div>

        {/* Single Variable Ingestion */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-4">Single Variable Ingestion</h2>
          <form onSubmit={handleSingleVariableIngestion}>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Device ID
              </label>
              <input
                type="text"
                value={singleDeviceId}
                onChange={(e) => setSingleDeviceId(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="sensor-001"
                required
              />
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Variable Name
              </label>
              <input
                type="text"
                value={singleVariableName}
                onChange={(e) => setSingleVariableName(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="temperature"
                required
              />
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Value
              </label>
              <input
                type="number"
                step="any"
                value={singleValue}
                onChange={(e) => setSingleValue(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="23.5"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-green-600 text-white py-2 rounded-md hover:bg-green-700 disabled:bg-gray-400"
            >
              {loading ? 'Sending...' : 'Send Value'}
            </button>
          </form>
        </div>
      </div>

      {/* Response/Error Display */}
      {(response || error) && (
        <div className="mt-8">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              <strong className="font-semibold">Error: </strong>
              <span>{error}</span>
            </div>
          )}
          {response && (
            <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg">
              <strong className="font-semibold">Success: </strong>
              <pre className="mt-2 text-sm overflow-auto">
                {JSON.stringify(response, null, 2)}
              </pre>
            </div>
          )}
        </div>
      )}

      {/* API Documentation */}
      <div className="mt-8 bg-gray-50 rounded-lg p-6">
        <h2 className="text-xl font-semibold mb-4">API Documentation</h2>
        <p className="text-gray-700 mb-4">
          For complete API documentation, visit:{' '}
          <a
            href={`${config.backendUrl}/swagger-ui.html`}
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-600 hover:underline"
          >
            Swagger UI
          </a>
        </p>
        <div className="space-y-4">
          <div>
            <h3 className="font-semibold text-gray-800">Full Ingestion Endpoint:</h3>
            <code className="block bg-white p-2 rounded text-sm mt-1">
              POST /api/v1/data/ingest
            </code>
          </div>
          <div>
            <h3 className="font-semibold text-gray-800">Single Variable Endpoint:</h3>
            <code className="block bg-white p-2 rounded text-sm mt-1">
              POST /api/v1/data/{'{deviceId}'}/{'{variableName}'}
            </code>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DataIngestion;
