import React, { useState } from 'react';

interface ImportResult {
  success: boolean;
  message?: string;
  totalRecords?: number;
  successCount?: number;
  failureCount?: number;
  errors?: string[];
}

const DataImport: React.FC = () => {
  const [importType, setImportType] = useState<'telemetry' | 'devices'>('telemetry');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [importFormat, setImportFormat] = useState<'csv' | 'json'>('csv');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [dragActive, setDragActive] = useState(false);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
      setResult(null);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setSelectedFile(e.dataTransfer.files[0]);
      setResult(null);
    }
  };

  const handleImport = async () => {
    if (!selectedFile) {
      setResult({
        success: false,
        message: 'Please select a file first',
      });
      return;
    }

    setLoading(true);
    setResult(null);

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);

      const endpoint = `/import/${importType}/${importFormat}`;
      const token = localStorage.getItem('accessToken');

      const response = await fetch(`/api/v1${endpoint}`, {
        method: 'POST',
        headers: token ? { 'Authorization': `Bearer ${token}` } : {},
        body: formData,
      });

      const data = await response.json();
      setResult(data);

      if (data.success && data.failureCount === 0) {
        setTimeout(() => {
          setSelectedFile(null);
          setResult(null);
        }, 5000);
      }
    } catch (err: any) {
      setResult({
        success: false,
        message: err.response?.data?.message || 'Import failed. Please check your file format.',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      const endpoint = `/import/${importType}/${importFormat}/template`;
      const token = localStorage.getItem('accessToken');

      const response = await fetch(`/api/v1${endpoint}`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {},
      });

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${importType}_import_template.${importFormat}`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download template:', err);
    }
  };

  const getFormatExamples = () => {
    if (importType === 'telemetry') {
      if (importFormat === 'csv') {
        return {
          example: 'deviceId,timestamp,kwConsumption,voltage,current\nmeter-001,2024-01-01T12:00:00Z,50.5,220.1,0.57',
          rules: [
            "First column must be 'deviceId'",
            "Second column must be 'timestamp' (ISO 8601 format)",
            'Remaining columns are variable names and their values',
            'All timestamps must be in UTC timezone',
            'Use comma as delimiter'
          ]
        };
      } else {
        return {
          example: '[\n  {\n    "deviceId": "meter-001",\n    "timestamp": "2024-01-01T12:00:00Z",\n    "variables": {\n      "kwConsumption": 50.5,\n      "voltage": 220.1\n    }\n  }\n]',
          rules: [
            'Must be a JSON array of objects',
            "Each object must have 'deviceId', 'timestamp', and 'variables' fields",
            "'variables' is an object with variable names as keys and numeric values",
            'All timestamps must be in ISO 8601 format (UTC)'
          ]
        };
      }
    } else {
      if (importFormat === 'csv') {
        return {
          example: 'externalId,name,location,sensorType,firmwareVersion,status,latitude,longitude,altitude\nmeter-001,Smart Meter 001,Building A,SMART_METER,v2.1.0,ACTIVE,40.7128,-74.0060,10.5',
          rules: [
            "First column must be 'externalId'",
            "Second column must be 'name'",
            'Optional columns: location, sensorType, firmwareVersion, status, latitude, longitude, altitude',
            'Status values: ACTIVE, INACTIVE, MAINTENANCE, ERROR, UNKNOWN',
            'Geolocation: latitude (-90 to 90), longitude (-180 to 180), altitude (meters)'
          ]
        };
      } else {
        return {
          example: '[\n  {\n    "externalId": "meter-001",\n    "name": "Smart Meter 001",\n    "location": "Building A",\n    "sensorType": "SMART_METER",\n    "firmwareVersion": "v2.1.0",\n    "status": "ACTIVE",\n    "latitude": 40.7128,\n    "longitude": -74.0060,\n    "altitude": 10.5\n  }\n]',
          rules: [
            'Must be a JSON array of device objects',
            "Required fields: 'externalId', 'name'",
            'Optional fields: location, sensorType, firmwareVersion, status, latitude, longitude, altitude',
            'Devices with existing externalId will be updated'
          ]
        };
      }
    }
  };

  const formatInfo = getFormatExamples();

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-8">Bulk Data Import</h1>

      <div className="max-w-4xl mx-auto">
        {/* Import Type & Format Selection */}
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">Select Import Type</h2>
          <div className="flex gap-4 mb-6">
            <label className="flex items-center cursor-pointer">
              <input
                type="radio"
                value="telemetry"
                checked={importType === 'telemetry'}
                onChange={() => {
                  setImportType('telemetry');
                  setSelectedFile(null);
                  setResult(null);
                }}
                className="mr-2"
              />
              <span className="text-gray-700 font-medium">Telemetry Data</span>
            </label>
            <label className="flex items-center cursor-pointer">
              <input
                type="radio"
                value="devices"
                checked={importType === 'devices'}
                onChange={() => {
                  setImportType('devices');
                  setSelectedFile(null);
                  setResult(null);
                }}
                className="mr-2"
              />
              <span className="text-gray-700 font-medium">Devices</span>
            </label>
          </div>

          <h2 className="text-xl font-semibold mb-4">Select File Format</h2>
          <div className="flex gap-4">
            <label className="flex items-center cursor-pointer">
              <input
                type="radio"
                value="csv"
                checked={importFormat === 'csv'}
                onChange={() => {
                  setImportFormat('csv');
                  setSelectedFile(null);
                  setResult(null);
                }}
                className="mr-2"
              />
              <span className="text-gray-700 font-medium">CSV (Comma-separated values)</span>
            </label>
            <label className="flex items-center cursor-pointer">
              <input
                type="radio"
                value="json"
                checked={importFormat === 'json'}
                onChange={() => {
                  setImportFormat('json');
                  setSelectedFile(null);
                  setResult(null);
                }}
                className="mr-2"
              />
              <span className="text-gray-700 font-medium">JSON</span>
            </label>
          </div>
        </div>

        {/* File Upload */}
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">Upload File</h2>

          <div
            className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
              dragActive
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-300 hover:border-gray-400'
            }`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            {selectedFile ? (
              <div className="space-y-4">
                <div className="flex items-center justify-center">
                  <svg
                    className="w-12 h-12 text-green-500"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                    />
                  </svg>
                </div>
                <div>
                  <p className="text-lg font-medium text-gray-900">{selectedFile.name}</p>
                  <p className="text-sm text-gray-500">
                    {(selectedFile.size / 1024).toFixed(2)} KB
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setSelectedFile(null);
                    setResult(null);
                  }}
                  className="text-sm text-red-600 hover:text-red-800"
                >
                  Remove file
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                <svg
                  className="mx-auto h-12 w-12 text-gray-400"
                  stroke="currentColor"
                  fill="none"
                  viewBox="0 0 48 48"
                >
                  <path
                    d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
                    strokeWidth={2}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
                <div className="space-y-1">
                  <label htmlFor="file-upload" className="cursor-pointer">
                    <span className="text-blue-600 hover:text-blue-700 font-medium">
                      Click to upload
                    </span>
                    <span className="text-gray-600"> or drag and drop</span>
                  </label>
                  <p className="text-sm text-gray-500">
                    {importFormat.toUpperCase()} file (max 10 MB)
                  </p>
                </div>
                <input
                  id="file-upload"
                  type="file"
                  accept={`.${importFormat}`}
                  onChange={handleFileSelect}
                  className="hidden"
                />
              </div>
            )}
          </div>

          <div className="mt-6 flex gap-4">
            <button
              onClick={handleImport}
              disabled={loading || !selectedFile}
              className="flex-1 bg-blue-600 text-white py-3 rounded-md hover:bg-blue-700 disabled:bg-gray-400 font-medium"
            >
              {loading ? (
                <span className="flex items-center justify-center">
                  <svg
                    className="animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                    ></circle>
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    ></path>
                  </svg>
                  Importing...
                </span>
              ) : (
                `Import ${importType === 'telemetry' ? 'Telemetry' : 'Devices'}`
              )}
            </button>
            <button
              onClick={handleDownloadTemplate}
              className="px-6 py-3 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 font-medium"
            >
              Download Template
            </button>
          </div>
        </div>

        {/* Results Display */}
        {result && (
          <div className="bg-white rounded-lg shadow p-6 mb-6">
            <h2 className="text-xl font-semibold mb-4">Import Results</h2>

            {result.success ? (
              <div>
                <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-4">
                  <div className="flex items-start">
                    <svg
                      className="w-6 h-6 text-green-600 mr-3 flex-shrink-0"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                    <div className="flex-1">
                      <p className="font-semibold text-green-900">
                        {result.message || 'Import completed'}
                      </p>
                      {result.totalRecords !== undefined && (
                        <div className="mt-2 text-sm text-green-800">
                          <p>Total records: {result.totalRecords}</p>
                          <p>Successfully imported: {result.successCount}</p>
                          {result.failureCount! > 0 && (
                            <p className="text-orange-600">
                              Failed: {result.failureCount}
                            </p>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {result.errors && result.errors.length > 0 && (
                  <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
                    <p className="font-semibold text-orange-900 mb-2">
                      Errors ({result.errors.length}):
                    </p>
                    <div className="max-h-60 overflow-y-auto">
                      <ul className="text-sm text-orange-800 space-y-1">
                        {result.errors.map((error, index) => (
                          <li key={index} className="font-mono text-xs">
                            {error}
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                <div className="flex items-start">
                  <svg
                    className="w-6 h-6 text-red-600 mr-3 flex-shrink-0"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </svg>
                  <div>
                    <p className="font-semibold text-red-900">Import Failed</p>
                    <p className="text-sm text-red-800 mt-1">{result.message}</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Format Information */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h3 className="font-semibold text-blue-900 mb-3">
            {importType === 'telemetry' ? 'Telemetry' : 'Device'} {importFormat.toUpperCase()} Format
          </h3>

          <div className="text-sm text-blue-800 space-y-3">
            <div>
              <p className="font-medium mb-1">Expected Format:</p>
              <code className="block bg-white p-2 rounded text-xs overflow-x-auto whitespace-pre">
                {formatInfo.example}
              </code>
            </div>
            <ul className="space-y-1">
              {formatInfo.rules.map((rule, index) => (
                <li key={index}>• {rule}</li>
              ))}
            </ul>
          </div>

          <div className="mt-4 pt-4 border-t border-blue-300">
            <p className="text-sm text-blue-800">
              <strong>Tip:</strong> Download the template to see the exact format expected.
              {importType === 'telemetry' && ' Devices will be created automatically if they don\'t exist.'}
              {importType === 'devices' && ' Existing devices will be updated based on externalId.'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DataImport;
