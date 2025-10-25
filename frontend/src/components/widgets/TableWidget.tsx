import React, { useEffect, useState } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { apiService } from '../../services/api';

interface TableWidgetProps {
  widget: Widget;
  latestData?: TelemetryPoint;
}

export const TableWidget: React.FC<TableWidgetProps> = ({ widget, latestData }) => {
  const [data, setData] = useState<TelemetryPoint | null>(null);
  const [loading, setLoading] = useState(true);

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
      if (!widget.deviceId) {
        setLoading(false);
        return;
      }

      try {
        const telemetryData = await apiService.getLatestForDevice(widget.deviceId);
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
  }, [widget]);

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

  // Define which fields to display and their labels
  const fields = [
    { key: 'kwConsumption', label: 'Power (kW)', unit: 'kW', decimals: 2 },
    { key: 'voltage', label: 'Voltage', unit: 'V', decimals: 1 },
    { key: 'current', label: 'Current', unit: 'A', decimals: 2 },
    { key: 'powerFactor', label: 'Power Factor', unit: '', decimals: 3 },
    { key: 'frequency', label: 'Frequency', unit: 'Hz', decimals: 1 },
  ];

  // Filter fields to only show those configured in widget or all if none specified
  const configuredFields = widget.config.fields as string[] | undefined;
  const displayFields = configuredFields
    ? fields.filter(f => configuredFields.includes(f.key))
    : fields;

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
            const value = data[field.key as keyof TelemetryPoint];
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
