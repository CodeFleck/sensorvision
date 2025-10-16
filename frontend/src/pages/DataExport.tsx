import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

interface Device {
  id: number;
  externalId: string;
  name: string;
}

const DataExport: React.FC = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDevice, setSelectedDevice] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [exportFormat, setExportFormat] = useState<'csv' | 'json'>('csv');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    loadDevices();
    // Set default date range (last 30 days)
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 30);
    setToDate(to.toISOString().slice(0, 16));
    setFromDate(from.toISOString().slice(0, 16));
  }, []);

  const loadDevices = async () => {
    try {
      const response = await apiService.get('/devices');
      setDevices(response.data as Device[]);
    } catch (err) {
      console.error('Failed to load devices:', err);
      setError('Failed to load devices');
    }
  };

  const handleExport = async () => {
    if (!selectedDevice) {
      setError('Please select a device');
      return;
    }
    if (!fromDate || !toDate) {
      setError('Please select date range');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const from = new Date(fromDate).toISOString();
      const to = new Date(toDate).toISOString();

      const url = `/export/${exportFormat}/${selectedDevice}?from=${from}&to=${to}`;

      // For blob downloads, we need to use fetch directly since our generic API doesn't support responseType
      const token = localStorage.getItem('accessToken');
      const fetchResponse = await fetch(`/api/v1${url}`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {},
      });
      const blob = await fetchResponse.blob();

      // Create download link
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = `${selectedDevice}_telemetry_${new Date().getTime()}.${exportFormat}`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(downloadUrl);

      setSuccess(`Data exported successfully as ${exportFormat.toUpperCase()}`);
    } catch (err: unknown) {
      const message = err instanceof Error && 'response' in err
        ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to export data')
        : 'Failed to export data';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const setQuickRange = (days: number) => {
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - days);
    setToDate(to.toISOString().slice(0, 16));
    setFromDate(from.toISOString().slice(0, 16));
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-8">Data Export</h1>

      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-6">Export Telemetry Data</h2>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4">
              {error}
            </div>
          )}

          {success && (
            <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-4">
              {success}
            </div>
          )}

          <div className="space-y-6">
            {/* Device Selection */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Select Device
              </label>
              <select
                value={selectedDevice}
                onChange={(e) => setSelectedDevice(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              >
                <option value="">-- Select a device --</option>
                {devices.map((device) => (
                  <option key={device.id} value={device.externalId}>
                    {device.name} ({device.externalId})
                  </option>
                ))}
              </select>
            </div>

            {/* Quick Date Range Buttons */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Quick Date Range
              </label>
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => setQuickRange(1)}
                  className="px-3 py-1 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 text-sm"
                >
                  Last 24 hours
                </button>
                <button
                  type="button"
                  onClick={() => setQuickRange(7)}
                  className="px-3 py-1 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 text-sm"
                >
                  Last 7 days
                </button>
                <button
                  type="button"
                  onClick={() => setQuickRange(30)}
                  className="px-3 py-1 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 text-sm"
                >
                  Last 30 days
                </button>
                <button
                  type="button"
                  onClick={() => setQuickRange(90)}
                  className="px-3 py-1 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 text-sm"
                >
                  Last 90 days
                </button>
              </div>
            </div>

            {/* From Date */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                From Date & Time
              </label>
              <input
                type="datetime-local"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
            </div>

            {/* To Date */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                To Date & Time
              </label>
              <input
                type="datetime-local"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
            </div>

            {/* Export Format */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Export Format
              </label>
              <div className="flex gap-4">
                <label className="flex items-center">
                  <input
                    type="radio"
                    value="csv"
                    checked={exportFormat === 'csv'}
                    onChange={() => setExportFormat('csv')}
                    className="mr-2"
                  />
                  <span className="text-gray-700">CSV (Excel compatible)</span>
                </label>
                <label className="flex items-center">
                  <input
                    type="radio"
                    value="json"
                    checked={exportFormat === 'json'}
                    onChange={() => setExportFormat('json')}
                    className="mr-2"
                  />
                  <span className="text-gray-700">JSON</span>
                </label>
              </div>
            </div>

            {/* Export Button */}
            <button
              onClick={handleExport}
              disabled={loading || !selectedDevice}
              className="w-full bg-blue-600 text-white py-3 rounded-md hover:bg-blue-700 disabled:bg-gray-400 font-medium"
            >
              {loading ? (
                <span className="flex items-center justify-center">
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Exporting...
                </span>
              ) : (
                `Export as ${exportFormat.toUpperCase()}`
              )}
            </button>
          </div>
        </div>

        {/* Info Panel */}
        <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h3 className="font-semibold text-blue-900 mb-2">Export Information</h3>
          <ul className="text-sm text-blue-800 space-y-1">
            <li>• CSV format is compatible with Excel and other spreadsheet applications</li>
            <li>• JSON format includes all metadata and is suitable for programmatic processing</li>
            <li>• Large date ranges may take longer to export</li>
            <li>• All timestamps are in UTC timezone</li>
            <li>• Maximum export size may be limited by server configuration</li>
          </ul>
        </div>

        {/* API Documentation Link */}
        <div className="mt-6 text-center">
          <p className="text-gray-600 mb-2">Need to automate exports?</p>
          <a
            href="http://localhost:8080/swagger-ui.html"
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-600 hover:underline font-medium"
          >
            View API Documentation
          </a>
        </div>
      </div>
    </div>
  );
};

export default DataExport;
