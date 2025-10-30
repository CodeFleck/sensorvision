import React, { useState, useEffect } from 'react';
import webhookTestService, { WebhookTestRequest, WebhookTestResponse } from '../services/webhookTestService';

const PAYLOAD_TEMPLATES = {
  'HTTP Webhook (Generic)': {
    url: 'http://localhost:8080/api/v1/webhooks/1/test-webhook',
    body: JSON.stringify({
      deviceId: 'sensor-001',
      timestamp: new Date().toISOString(),
      variables: {
        temperature: 25.5,
        humidity: 60.0
      }
    }, null, 2),
    headers: '{"Content-Type": "application/json"}'
  },
  'LoRaWAN TTN': {
    url: 'http://localhost:8080/api/v1/webhooks/1/lorawan-ttn',
    body: JSON.stringify({
      end_device_ids: {
        device_id: 'dev-001'
      },
      uplink_message: {
        decoded_payload: {
          temperature: 22.5,
          humidity: 55.0
        }
      }
    }, null, 2),
    headers: '{"Content-Type": "application/json"}'
  },
};

const WebhookTester: React.FC = () => {
  const [url, setUrl] = useState('');
  const [method, setMethod] = useState<'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'>('POST');
  const [headers, setHeaders] = useState('{"Content-Type": "application/json"}');
  const [body, setBody] = useState('');
  const [testName, setTestName] = useState('');
  const [testing, setTesting] = useState(false);
  const [result, setResult] = useState<WebhookTestResponse | null>(null);
  const [history, setHistory] = useState<WebhookTestResponse[]>([]);
  const [showHistory, setShowHistory] = useState(false);

  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = async () => {
    try {
      const data = await webhookTestService.getHistory(0, 10);
      setHistory(data.content);
    } catch (err) {
      console.error('Failed to load history:', err);
    }
  };

  const handleTemplateSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const templateName = e.target.value;
    if (templateName && PAYLOAD_TEMPLATES[templateName as keyof typeof PAYLOAD_TEMPLATES]) {
      const template = PAYLOAD_TEMPLATES[templateName as keyof typeof PAYLOAD_TEMPLATES];
      setUrl(template.url);
      setBody(template.body);
      setHeaders(template.headers);
    }
  };

  const handleTest = async () => {
    try {
      setTesting(true);
      setResult(null);

      let parsedHeaders = {};
      try {
        parsedHeaders = headers ? JSON.parse(headers) : {};
      } catch (e) {
        alert('Invalid JSON in headers');
        setTesting(false);
        return;
      }

      const request: WebhookTestRequest = {
        name: testName || undefined,
        url,
        httpMethod: method,
        headers: parsedHeaders,
        requestBody: body || undefined,
      };

      const response = await webhookTestService.executeTest(request);
      setResult(response);
      await loadHistory();
    } catch (err: any) {
      console.error('Test failed:', err);
      alert(`Test failed: ${err.response?.data?.message || err.message}`);
    } finally {
      setTesting(false);
    }
  };

  const formatStatus = (statusCode?: number) => {
    if (!statusCode) return 'N/A';
    if (statusCode >= 200 && statusCode < 300) return `${statusCode} OK`;
    if (statusCode >= 400 && statusCode < 500) return `${statusCode} Client Error`;
    if (statusCode >= 500) return `${statusCode} Server Error`;
    return statusCode.toString();
  };

  return (
    <div className="max-w-7xl mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Webhook Testing Tool</h1>
        <p className="text-gray-600 mt-2">
          Test webhook endpoints with custom payloads and view responses.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left: Test Configuration */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold mb-4">Request Configuration</h2>

          {/* Template Selector */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Load Template
            </label>
            <select
              onChange={handleTemplateSelect}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              defaultValue=""
            >
              <option value="">-- Select a template --</option>
              {Object.keys(PAYLOAD_TEMPLATES).map((name) => (
                <option key={name} value={name}>{name}</option>
              ))}
            </select>
          </div>

          {/* Test Name */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Test Name (Optional)
            </label>
            <input
              type="text"
              value={testName}
              onChange={(e) => setTestName(e.target.value)}
              placeholder="My webhook test"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* URL */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              URL *
            </label>
            <input
              type="text"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://example.com/webhook"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>

          {/* HTTP Method */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              HTTP Method
            </label>
            <select
              value={method}
              onChange={(e) => setMethod(e.target.value as any)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="POST">POST</option>
              <option value="GET">GET</option>
              <option value="PUT">PUT</option>
              <option value="PATCH">PATCH</option>
              <option value="DELETE">DELETE</option>
            </select>
          </div>

          {/* Headers */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Headers (JSON)
            </label>
            <textarea
              value={headers}
              onChange={(e) => setHeaders(e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Body */}
          {method !== 'GET' && (
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Request Body (JSON)
              </label>
              <textarea
                value={body}
                onChange={(e) => setBody(e.target.value)}
                rows={8}
                className="w-full px-3 py-2 border border-gray-300 rounded-md font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder='{"key": "value"}'
              />
            </div>
          )}

          {/* Send Button */}
          <button
            onClick={handleTest}
            disabled={testing || !url}
            className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {testing ? 'Sending...' : 'Send Request'}
          </button>
        </div>

        {/* Right: Response */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-semibold">Response</h2>
            <button
              onClick={() => setShowHistory(!showHistory)}
              className="text-sm text-blue-600 hover:text-blue-800"
            >
              {showHistory ? 'Hide History' : 'Show History'}
            </button>
          </div>

          {!showHistory ? (
            result ? (
              <div>
                {/* Status */}
                <div className="mb-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium text-gray-700">Status</span>
                    <span className={`px-2 py-1 text-sm font-semibold rounded ${
                      result.statusCode && result.statusCode >= 200 && result.statusCode < 300
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {formatStatus(result.statusCode)}
                    </span>
                  </div>
                  <div className="text-sm text-gray-600 mt-1">
                    Duration: {result.durationMs}ms
                  </div>
                </div>

                {/* Error Message */}
                {result.errorMessage && (
                  <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded">
                    <p className="text-sm font-medium text-red-800">Error</p>
                    <p className="text-sm text-red-600 mt-1">{result.errorMessage}</p>
                  </div>
                )}

                {/* Response Body */}
                {result.responseBody && (
                  <div className="mb-4">
                    <p className="text-sm font-medium text-gray-700 mb-2">Response Body</p>
                    <pre className="p-3 bg-gray-50 border border-gray-200 rounded text-xs overflow-auto max-h-96">
                      {result.responseBody}
                    </pre>
                  </div>
                )}

                {/* Response Headers */}
                {result.responseHeaders && (
                  <div>
                    <p className="text-sm font-medium text-gray-700 mb-2">Response Headers</p>
                    <pre className="p-3 bg-gray-50 border border-gray-200 rounded text-xs overflow-auto max-h-48">
                      {JSON.stringify(result.responseHeaders, null, 2)}
                    </pre>
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center text-gray-500 py-12">
                <p>No response yet.</p>
                <p className="text-sm mt-2">Send a request to see the response here.</p>
              </div>
            )
          ) : (
            <div>
              <h3 className="text-lg font-semibold mb-3">Test History</h3>
              {history.length === 0 ? (
                <p className="text-gray-500">No test history yet.</p>
              ) : (
                <div className="space-y-2">
                  {history.map((test) => (
                    <div
                      key={test.id}
                      onClick={() => { setResult(test); setShowHistory(false); }}
                      className="p-3 border border-gray-200 rounded hover:bg-gray-50 cursor-pointer"
                    >
                      <div className="flex justify-between items-start">
                        <div className="flex-1">
                          <p className="text-sm font-medium text-gray-900">
                            {test.name || 'Unnamed Test'}
                          </p>
                          <p className="text-xs text-gray-600 mt-1">
                            {test.httpMethod} {test.url}
                          </p>
                          <p className="text-xs text-gray-500 mt-1">
                            {new Date(test.createdAt).toLocaleString()}
                          </p>
                        </div>
                        <span className={`ml-2 px-2 py-1 text-xs font-semibold rounded ${
                          test.statusCode && test.statusCode >= 200 && test.statusCode < 300
                            ? 'bg-green-100 text-green-800'
                            : 'bg-red-100 text-red-800'
                        }`}>
                          {test.statusCode || 'ERR'}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default WebhookTester;
