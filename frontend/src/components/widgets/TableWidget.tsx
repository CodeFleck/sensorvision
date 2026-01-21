import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint, DeviceVariable } from '../../types';
import { apiService } from '../../services/api';

interface TableWidgetProps {
  widget: Widget;
  deviceId?: string;
  latestData?: TelemetryPoint;
}

export const TableWidget: React.FC<TableWidgetProps> = ({ widget, deviceId, latestData }) => {
  const [data, setData] = useState<TelemetryPoint | null>(null);
  const [loading, setLoading] = useState(true);
  const [variables, setVariables] = useState<DeviceVariable[]>([]);

  // Fetch device variables
  useEffect(() => {
    const loadVariables = async () => {
      if (!deviceId) return;

      try {
        // deviceId in props is externalId, we need to find the UUID
        const devices = await apiService.getDevices();
        const device = devices.find(d => d.externalId === deviceId);
        if (device?.id) {
          const vars = await apiService.getDeviceVariables(device.id);
          setVariables(vars);
        }
      } catch (error) {
        console.error('Failed to load variables for table:', error);
      }
    };

    loadVariables();
  }, [deviceId]);

  // Update value when real-time data arrives
  useEffect(() => {
    if (latestData) {
      setData(latestData);
      setLoading(false);
    }
  }, [latestData]);

  // Initial data fetch and fallback polling
  useEffect(() => {
    const fetchData = async () => {
      if (!deviceId) {
        setLoading(false);
        return;
      }

      try {
        const telemetryData = await apiService.getLatestForDevice(deviceId);
        setData(telemetryData);
      } catch (error) {
        console.error('Error fetching table data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, (widget.config.refreshInterval as number | undefined) ?? 30000);
    return () => clearInterval(interval);
  }, [deviceId, widget.config.refreshInterval]);

  if (loading) {
    return (
      <div className="flex items-center justify-center w-full h-full">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex items-center justify-center w-full h-full text-gray-400">
        <p>No data available</p>
      </div>
    );
  }

  // Build display fields from dynamic variables
  const displayFields = variables.length > 0
    ? variables.map(v => ({
        key: v.name,
        label: v.displayName || v.name,
        unit: v.unit || '',
        decimals: v.decimalPlaces ?? 2,
      }))
    : // Fallback to widget config fields if variables not loaded yet
      (widget.config.fields as Array<{ key: string; label: string; unit: string; decimals: number }> | undefined) || [];

  return (
    <div className="w-full h-full overflow-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50 sticky top-0">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Variable
            </th>
            <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
              Value
            </th>
            <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
              Unit
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {displayFields.map((field) => {
            // Convert snake_case to camelCase for API property access
            const toCamelCase = (str: string) => str.replace(/_([a-z])/g, (g) => g[1].toUpperCase());
            const camelKey = toCamelCase(field.key);
            const value = data[camelKey as keyof TelemetryPoint] ?? data[field.key as keyof TelemetryPoint];
            const displayValue = typeof value === 'number'
              ? value.toFixed(field.decimals)
              : value?.toString() ?? 'N/A';

            return (
              <tr key={field.key} className="hover:bg-gray-50">
                <td className="px-4 py-3 text-sm font-medium text-gray-900">
                  {field.label}
                </td>
                <td className="px-4 py-3 text-sm text-gray-700 text-right font-mono">
                  {displayValue}
                </td>
                <td className="px-4 py-3 text-sm text-gray-500 text-right">
                  {field.unit}
                </td>
              </tr>
            );
          })}

          {/* Device ID and Timestamp */}
          <tr className="bg-gray-50">
            <td className="px-4 py-3 text-sm font-medium text-gray-900">
              Device ID
            </td>
            <td className="px-4 py-3 text-sm text-gray-700 text-right font-mono" colSpan={2}>
              {data.deviceId}
            </td>
          </tr>
          <tr className="bg-gray-50">
            <td className="px-4 py-3 text-sm font-medium text-gray-900">
              Last Update
            </td>
            <td className="px-4 py-3 text-sm text-gray-700 text-right" colSpan={2}>
              {new Date(data.timestamp).toLocaleString()}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
};
