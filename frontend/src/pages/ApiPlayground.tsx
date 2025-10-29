import React, { useState } from 'react';
import { Play, Clock, Trash2, Copy, Check, BookOpen, Code } from 'lucide-react';
import toast from 'react-hot-toast';

interface ApiEndpoint {
  name: string;
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  path: string;
  description: string;
  exampleBody?: string;
  requiresAuth: boolean;
}

interface RequestHistory {
  id: string;
  method: string;
  path: string;
  statusCode?: number;
  timestamp: Date;
}

const API_ENDPOINTS: Record<string, ApiEndpoint[]> = {
  'Devices': [
    {
      name: 'List Devices',
      method: 'GET',
      path: '/api/v1/devices',
      description: 'Get all devices in your organization',
      requiresAuth: true,
    },
    {
      name: 'Get Device',
      method: 'GET',
      path: '/api/v1/devices/{deviceId}',
      description: 'Get a specific device by ID',
      requiresAuth: true,
    },
    {
      name: 'Create Device',
      method: 'POST',
      path: '/api/v1/devices',
      description: 'Create a new device',
      exampleBody: JSON.stringify({
        externalId: 'sensor-001',
        name: 'Temperature Sensor',
        description: 'Warehouse temperature monitor',
        type: 'SENSOR',
        metadata: {
          location: 'Warehouse A',
          zone: 'Cold Storage'
        }
      }, null, 2),
      requiresAuth: true,
    },
    {
      name: 'Update Device',
      method: 'PUT',
      path: '/api/v1/devices/{deviceId}',
      description: 'Update an existing device',
      exampleBody: JSON.stringify({
        name: 'Updated Sensor Name',
        description: 'Updated description'
      }, null, 2),
      requiresAuth: true,
    },
    {
      name: 'Delete Device',
      method: 'DELETE',
      path: '/api/v1/devices/{deviceId}',
      description: 'Delete a device',
      requiresAuth: true,
    },
  ],
  'Telemetry': [
    {
      name: 'Query Telemetry',
      method: 'GET',
      path: '/api/v1/data/query?deviceId={deviceId}&from={from}&to={to}',
      description: 'Query telemetry data for a device within a time range',
      requiresAuth: true,
    },
    {
      name: 'Latest Telemetry',
      method: 'GET',
      path: '/api/v1/data/latest?deviceIds={deviceId1,deviceId2}',
      description: 'Get latest telemetry for multiple devices',
      requiresAuth: true,
    },
    {
      name: 'Ingest Telemetry',
      method: 'POST',
      path: '/api/v1/data/ingest',
      description: 'Ingest telemetry data for a device',
      exampleBody: JSON.stringify({
        deviceId: 'sensor-001',
        timestamp: new Date().toISOString(),
        variables: {
          temperature: 25.5,
          humidity: 60.0,
          pressure: 1013.25
        }
      }, null, 2),
      requiresAuth: true,
    },
  ],
  'Rules & Alerts': [
    {
      name: 'List Rules',
      method: 'GET',
      path: '/api/v1/rules',
      description: 'Get all rules in your organization',
      requiresAuth: true,
    },
    {
      name: 'Create Rule',
      method: 'POST',
      path: '/api/v1/rules',
      description: 'Create a new monitoring rule',
      exampleBody: JSON.stringify({
        name: 'High Temperature Alert',
        deviceId: 'sensor-001',
        variable: 'temperature',
        operator: 'GT',
        threshold: 30.0,
        enabled: true
      }, null, 2),
      requiresAuth: true,
    },
    {
      name: 'List Alerts',
      method: 'GET',
      path: '/api/v1/alerts',
      description: 'Get all alerts',
      requiresAuth: true,
    },
    {
      name: 'Acknowledge Alert',
      method: 'POST',
      path: '/api/v1/alerts/{alertId}/acknowledge',
      description: 'Acknowledge an alert',
      requiresAuth: true,
    },
  ],
  'Dashboards & Widgets': [
    {
      name: 'List Dashboards',
      method: 'GET',
      path: '/api/v1/dashboards',
      description: 'Get all dashboards',
      requiresAuth: true,
    },
    {
      name: 'Get Default Dashboard',
      method: 'GET',
      path: '/api/v1/dashboards/default',
      description: 'Get the default dashboard',
      requiresAuth: true,
    },
    {
      name: 'Create Dashboard',
      method: 'POST',
      path: '/api/v1/dashboards',
      description: 'Create a new dashboard',
      exampleBody: JSON.stringify({
        name: 'My Dashboard',
        description: 'Custom dashboard',
        isDefault: false
      }, null, 2),
      requiresAuth: true,
    },
  ],
  'Analytics': [
    {
      name: 'Aggregate Data',
      method: 'GET',
      path: '/api/v1/analytics/aggregate?deviceId={deviceId}&variable={variable}&aggregation={MIN|MAX|AVG|SUM}&from={from}&to={to}',
      description: 'Get aggregated telemetry data',
      requiresAuth: true,
    },
  ],
  'Device Tokens': [
    {
      name: 'Generate Token',
      method: 'POST',
      path: '/api/v1/devices/{deviceId}/token/generate',
      description: 'Generate a new authentication token for a device',
      requiresAuth: true,
    },
    {
      name: 'Rotate Token',
      method: 'POST',
      path: '/api/v1/devices/{deviceId}/token/rotate',
      description: 'Rotate an existing device token',
      requiresAuth: true,
    },
    {
      name: 'Get Token Info',
      method: 'GET',
      path: '/api/v1/devices/{deviceId}/token',
      description: 'Get token information for a device',
      requiresAuth: true,
    },
    {
      name: 'Revoke Token',
      method: 'DELETE',
      path: '/api/v1/devices/{deviceId}/token',
      description: 'Revoke a device token',
      requiresAuth: true,
    },
  ],
};

const ApiPlayground: React.FC = () => {
  const [selectedCategory, setSelectedCategory] = useState<string>('Devices');
  const [selectedEndpoint, setSelectedEndpoint] = useState<ApiEndpoint | null>(null);
  const [method, setMethod] = useState<string>('GET');
  const [path, setPath] = useState<string>('');
  const [headers, setHeaders] = useState<string>(JSON.stringify({
    'Content-Type': 'application/json'
  }, null, 2));
  const [body, setBody] = useState<string>('');
  const [response, setResponse] = useState<any>(null);
  const [statusCode, setStatusCode] = useState<number | null>(null);
  const [duration, setDuration] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [history, setHistory] = useState<RequestHistory[]>(() => {
    const saved = localStorage.getItem('apiPlaygroundHistory');
    return saved ? JSON.parse(saved) : [];
  });
  const [copiedResponse, setCopiedResponse] = useState(false);

  const loadEndpoint = (endpoint: ApiEndpoint) => {
    setSelectedEndpoint(endpoint);
    setMethod(endpoint.method);
    setPath(endpoint.path);
    setBody(endpoint.exampleBody || '');
  };

  const executeRequest = async () => {
    setLoading(true);
    setResponse(null);
    setStatusCode(null);
    setDuration(null);

    const startTime = Date.now();

    try {
      const token = localStorage.getItem('accessToken');
      const requestHeaders: Record<string, string> = {};

      // Parse custom headers
      try {
        const parsedHeaders = JSON.parse(headers);
        Object.assign(requestHeaders, parsedHeaders);
      } catch (e) {
        toast.error('Invalid headers JSON');
        setLoading(false);
        return;
      }

      // Add auth token
      if (token) {
        requestHeaders['Authorization'] = `Bearer ${token}`;
      }

      // Build request options
      const options: RequestInit = {
        method,
        headers: requestHeaders,
      };

      // Add body for POST, PUT, PATCH
      if (['POST', 'PUT', 'PATCH'].includes(method) && body) {
        options.body = body;
      }

      // Make the request
      const res = await fetch(path, options);
      const endTime = Date.now();
      setDuration(endTime - startTime);
      setStatusCode(res.status);

      // Parse response
      const contentType = res.headers.get('content-type');
      let responseData;

      if (contentType?.includes('application/json')) {
        responseData = await res.json();
      } else {
        responseData = await res.text();
      }

      setResponse(responseData);

      // Add to history
      const historyEntry: RequestHistory = {
        id: Date.now().toString(),
        method,
        path,
        statusCode: res.status,
        timestamp: new Date(),
      };

      const newHistory = [historyEntry, ...history].slice(0, 20);
      setHistory(newHistory);
      localStorage.setItem('apiPlaygroundHistory', JSON.stringify(newHistory));

      if (res.ok) {
        toast.success(`Request successful (${res.status})`);
      } else {
        toast.error(`Request failed (${res.status})`);
      }
    } catch (error: any) {
      const endTime = Date.now();
      setDuration(endTime - startTime);
      setResponse({ error: error.message });
      toast.error(`Request failed: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const loadFromHistory = (item: RequestHistory) => {
    setMethod(item.method);
    setPath(item.path);
  };

  const clearHistory = () => {
    setHistory([]);
    localStorage.removeItem('apiPlaygroundHistory');
    toast.success('History cleared');
  };

  const copyResponse = () => {
    const responseText = typeof response === 'string'
      ? response
      : JSON.stringify(response, null, 2);

    navigator.clipboard.writeText(responseText);
    setCopiedResponse(true);
    toast.success('Response copied to clipboard');
    setTimeout(() => setCopiedResponse(false), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">API Request Playground</h1>
          <p className="text-gray-600 mt-1">
            Test and explore SensorVision API endpoints interactively
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <a
            href="https://github.com/CodeFleck/sensorvision"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center space-x-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
          >
            <BookOpen className="h-4 w-4" />
            <span>API Docs</span>
          </a>
        </div>
      </div>

      <div className="grid grid-cols-12 gap-6">
        {/* Left Sidebar - Endpoint Catalog */}
        <div className="col-span-3 space-y-4">
          <div className="bg-white rounded-lg shadow p-4">
            <h2 className="text-sm font-semibold text-gray-900 mb-3">API Endpoints</h2>
            <div className="space-y-2">
              {Object.keys(API_ENDPOINTS).map((category) => (
                <div key={category}>
                  <button
                    onClick={() => setSelectedCategory(category)}
                    className={`w-full text-left px-3 py-2 text-sm font-medium rounded-md transition-colors ${
                      selectedCategory === category
                        ? 'bg-blue-50 text-blue-700'
                        : 'text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    {category}
                  </button>
                  {selectedCategory === category && (
                    <div className="mt-2 ml-3 space-y-1">
                      {API_ENDPOINTS[category].map((endpoint, idx) => (
                        <button
                          key={idx}
                          onClick={() => loadEndpoint(endpoint)}
                          className="w-full text-left px-3 py-1.5 text-xs text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                        >
                          <span className={`inline-block w-12 font-mono font-semibold ${
                            endpoint.method === 'GET' ? 'text-green-600' :
                            endpoint.method === 'POST' ? 'text-blue-600' :
                            endpoint.method === 'PUT' ? 'text-orange-600' :
                            endpoint.method === 'PATCH' ? 'text-purple-600' :
                            'text-red-600'
                          }`}>
                            {endpoint.method}
                          </span>
                          {endpoint.name}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Request History */}
          <div className="bg-white rounded-lg shadow p-4">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-sm font-semibold text-gray-900">Recent Requests</h2>
              {history.length > 0 && (
                <button
                  onClick={clearHistory}
                  className="text-xs text-red-600 hover:text-red-700"
                >
                  <Trash2 className="h-3 w-3" />
                </button>
              )}
            </div>
            <div className="space-y-1 max-h-64 overflow-y-auto">
              {history.length === 0 ? (
                <p className="text-xs text-gray-500">No recent requests</p>
              ) : (
                history.map((item) => (
                  <button
                    key={item.id}
                    onClick={() => loadFromHistory(item)}
                    className="w-full text-left px-2 py-1.5 text-xs hover:bg-gray-50 rounded transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <span className={`font-mono font-semibold ${
                        item.method === 'GET' ? 'text-green-600' :
                        item.method === 'POST' ? 'text-blue-600' :
                        item.method === 'PUT' ? 'text-orange-600' :
                        item.method === 'PATCH' ? 'text-purple-600' :
                        'text-red-600'
                      }`}>
                        {item.method}
                      </span>
                      <span className={`text-xs ${
                        item.statusCode && item.statusCode >= 200 && item.statusCode < 300
                          ? 'text-green-600'
                          : 'text-red-600'
                      }`}>
                        {item.statusCode}
                      </span>
                    </div>
                    <p className="text-gray-600 truncate mt-0.5">{item.path}</p>
                  </button>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Main Content - Request Builder */}
        <div className="col-span-9 space-y-4">
          {/* Endpoint Info */}
          {selectedEndpoint && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div className="flex items-start space-x-3">
                <Code className="h-5 w-5 text-blue-600 mt-0.5" />
                <div>
                  <h3 className="text-sm font-semibold text-blue-900">{selectedEndpoint.name}</h3>
                  <p className="text-sm text-blue-700 mt-1">{selectedEndpoint.description}</p>
                  {selectedEndpoint.requiresAuth && (
                    <span className="inline-block mt-2 text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
                      Requires Authentication
                    </span>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Request Configuration */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Request</h2>

            {/* Method and Path */}
            <div className="grid grid-cols-12 gap-4 mb-4">
              <div className="col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-2">Method</label>
                <select
                  value={method}
                  onChange={(e) => setMethod(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="GET">GET</option>
                  <option value="POST">POST</option>
                  <option value="PUT">PUT</option>
                  <option value="PATCH">PATCH</option>
                  <option value="DELETE">DELETE</option>
                </select>
              </div>
              <div className="col-span-10">
                <label className="block text-sm font-medium text-gray-700 mb-2">Path</label>
                <input
                  type="text"
                  value={path}
                  onChange={(e) => setPath(e.target.value)}
                  placeholder="/api/v1/devices"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 font-mono text-sm"
                />
              </div>
            </div>

            {/* Headers */}
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Headers (JSON)
              </label>
              <textarea
                value={headers}
                onChange={(e) => setHeaders(e.target.value)}
                rows={4}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 font-mono text-sm"
                placeholder='{"Content-Type": "application/json"}'
              />
            </div>

            {/* Body */}
            {['POST', 'PUT', 'PATCH'].includes(method) && (
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Body (JSON)
                </label>
                <textarea
                  value={body}
                  onChange={(e) => setBody(e.target.value)}
                  rows={8}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 font-mono text-sm"
                  placeholder='{"key": "value"}'
                />
              </div>
            )}

            {/* Send Button */}
            <button
              onClick={executeRequest}
              disabled={loading || !path}
              className="w-full flex items-center justify-center space-x-2 px-4 py-3 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                  <span>Sending...</span>
                </>
              ) : (
                <>
                  <Play className="h-5 w-5" />
                  <span>Send Request</span>
                </>
              )}
            </button>
          </div>

          {/* Response */}
          {response !== null && (
            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900">Response</h2>
                <div className="flex items-center space-x-4">
                  {statusCode !== null && (
                    <span className={`text-sm font-semibold ${
                      statusCode >= 200 && statusCode < 300
                        ? 'text-green-600'
                        : statusCode >= 400
                        ? 'text-red-600'
                        : 'text-yellow-600'
                    }`}>
                      Status: {statusCode}
                    </span>
                  )}
                  {duration !== null && (
                    <span className="text-sm text-gray-600 flex items-center space-x-1">
                      <Clock className="h-4 w-4" />
                      <span>{duration}ms</span>
                    </span>
                  )}
                  <button
                    onClick={copyResponse}
                    className="flex items-center space-x-1 text-sm text-gray-600 hover:text-gray-900"
                  >
                    {copiedResponse ? (
                      <>
                        <Check className="h-4 w-4 text-green-600" />
                        <span className="text-green-600">Copied!</span>
                      </>
                    ) : (
                      <>
                        <Copy className="h-4 w-4" />
                        <span>Copy</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
              <pre className="bg-gray-50 rounded-md p-4 overflow-x-auto text-sm font-mono">
                {typeof response === 'string'
                  ? response
                  : JSON.stringify(response, null, 2)}
              </pre>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ApiPlayground;
