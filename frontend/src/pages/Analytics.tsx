import { useState, useEffect } from 'react';
import { TrendingUp, BarChart3, Download } from 'lucide-react';
import { Device } from '../types';
import { apiService } from '../services/api';
import { AggregationChart } from '../components/AggregationChart';

export const Analytics = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<string>('');
  const [selectedVariable, setSelectedVariable] = useState<'kwConsumption' | 'voltage' | 'current'>('kwConsumption');
  const [aggregationType, setAggregationType] = useState<'MIN' | 'MAX' | 'AVG' | 'SUM'>('AVG');
  const [dateRange, setDateRange] = useState({
    from: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString().split('T')[0], // 24 hours ago
    to: new Date().toISOString().split('T')[0], // now
  });
  const [aggregatedData, setAggregatedData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchDevices = async () => {
      try {
        const data = await apiService.getDevices();
        setDevices(data);
        if (data.length > 0) {
          setSelectedDevice(data[0].externalId);
        }
      } catch (error) {
        console.error('Failed to fetch devices:', error);
      }
    };

    fetchDevices();
  }, []);

  const handleAnalyze = async () => {
    if (!selectedDevice) return;

    setLoading(true);
    try {
      const data = await apiService.getAggregatedData(
        selectedDevice,
        selectedVariable,
        aggregationType,
        `${dateRange.from}T00:00:00Z`,
        `${dateRange.to}T23:59:59Z`,
        '1h' // hourly intervals
      );
      setAggregatedData(data as any[]);
    } catch (error) {
      console.error('Failed to fetch aggregated data:', error);
    } finally {
      setLoading(false);
    }
  };

  const variableLabels = {
    kwConsumption: 'Power Consumption (kW)',
    voltage: 'Voltage (V)',
    current: 'Current (A)',
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Data Analytics</h1>
        <p className="text-gray-600 mt-1">Analyze historical telemetry data with aggregations</p>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Analysis Parameters</h2>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Device</label>
            <select
              value={selectedDevice}
              onChange={(e) => setSelectedDevice(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              {devices.map((device) => (
                <option key={device.externalId} value={device.externalId}>
                  {device.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Variable</label>
            <select
              value={selectedVariable}
              onChange={(e) => setSelectedVariable(e.target.value as any)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="kwConsumption">Power Consumption</option>
              <option value="voltage">Voltage</option>
              <option value="current">Current</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Aggregation</label>
            <select
              value={aggregationType}
              onChange={(e) => setAggregationType(e.target.value as any)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="AVG">Average</option>
              <option value="MIN">Minimum</option>
              <option value="MAX">Maximum</option>
              <option value="SUM">Sum</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">From Date</label>
            <input
              type="date"
              value={dateRange.from}
              onChange={(e) => setDateRange(prev => ({ ...prev, from: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">To Date</label>
            <input
              type="date"
              value={dateRange.to}
              onChange={(e) => setDateRange(prev => ({ ...prev, to: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
        </div>

        <div className="mt-4">
          <button
            onClick={handleAnalyze}
            disabled={loading || !selectedDevice}
            className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            <BarChart3 className="h-4 w-4" />
            <span>{loading ? 'Analyzing...' : 'Analyze Data'}</span>
          </button>
        </div>
      </div>

      {/* Results */}
      {aggregatedData.length > 0 && (
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">
              {aggregationType} {variableLabels[selectedVariable]} - {selectedDevice}
            </h2>
            <button className="flex items-center space-x-2 text-blue-600 hover:text-blue-800">
              <Download className="h-4 w-4" />
              <span>Export CSV</span>
            </button>
          </div>

          <AggregationChart
            data={aggregatedData}
            variable={selectedVariable}
            aggregation={aggregationType}
          />
        </div>
      )}

      {/* Summary Stats */}
      {aggregatedData.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {['MIN', 'MAX', 'AVG', 'SUM'].map((stat) => (
            <div key={stat} className="bg-white rounded-lg border border-gray-200 p-4">
              <div className="flex items-center">
                <TrendingUp className="h-6 w-6 text-blue-600 mr-3" />
                <div>
                  <div className="text-sm font-medium text-gray-600">{stat}</div>
                  <div className="text-lg font-bold text-gray-900">
                    {/* Calculate stat from aggregatedData */}
                    --
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};