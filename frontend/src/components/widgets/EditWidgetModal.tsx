import React, { useState, useEffect } from 'react';
import { WidgetType, Widget, Device } from '../../types';
import { apiService } from '../../services/api';

interface EditWidgetModalProps {
  dashboardId: number;
  widget: Widget;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const EditWidgetModal: React.FC<EditWidgetModalProps> = ({
  dashboardId,
  widget,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [formData, setFormData] = useState({
    name: widget.name,
    type: widget.type,
    positionX: widget.positionX,
    positionY: widget.positionY,
    width: widget.width,
    height: widget.height,
    deviceId: widget.deviceId || '',
    variableName: widget.variableName || 'kwConsumption',
    useContextDevice: widget.useContextDevice || false,
    deviceLabel: widget.deviceLabel,
    aggregation: widget.aggregation,
    timeRangeMinutes: widget.timeRangeMinutes || 60,
    config: widget.config,
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      loadDevices();
      // Reset form data when widget changes
      setFormData({
        name: widget.name,
        type: widget.type,
        positionX: widget.positionX,
        positionY: widget.positionY,
        width: widget.width,
        height: widget.height,
        deviceId: widget.deviceId || '',
        variableName: widget.variableName || 'kwConsumption',
        useContextDevice: widget.useContextDevice || false,
        deviceLabel: widget.deviceLabel,
        aggregation: widget.aggregation,
        timeRangeMinutes: widget.timeRangeMinutes || 60,
        config: widget.config,
      });
    }
  }, [isOpen, widget]);

  const loadDevices = async () => {
    try {
      const deviceList = await apiService.getDevices();
      setDevices(deviceList);
    } catch (error) {
      console.error('Failed to load devices:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      await apiService.updateWidget(dashboardId, widget.id, formData);
      onSuccess();
      onClose();
    } catch (error) {
      console.error('Failed to update widget:', error);
      alert('Failed to update widget');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  const widgetTypes: WidgetType[] = [
    'GAUGE',
    'METRIC_CARD',
    'LINE_CHART',
    'BAR_CHART',
    'PIE_CHART',
    'AREA_CHART',
    'TABLE',
  ];

  const variables = ['kwConsumption', 'voltage', 'current', 'powerFactor', 'frequency'];

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-gray-800">Edit Widget</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Widget Name */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Widget Name *
            </label>
            <input
              type="text"
              required
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g., Power Consumption"
            />
          </div>

          {/* Widget Type */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Widget Type *
            </label>
            <select
              value={formData.type}
              onChange={(e) => setFormData({ ...formData, type: e.target.value as WidgetType })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {widgetTypes.map((type) => (
                <option key={type} value={type}>
                  {type.replace(/_/g, ' ')}
                </option>
              ))}
            </select>
          </div>

          {/* Use Context Device Toggle */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <label className="flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={formData.useContextDevice || false}
                onChange={(e) => setFormData({
                  ...formData,
                  useContextDevice: e.target.checked,
                  deviceId: e.target.checked ? '' : (devices[0]?.externalId || widget.deviceId || '')
                })}
                className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <div className="ml-3">
                <span className="text-sm font-medium text-gray-900">
                  Use dashboard's selected device
                </span>
                <p className="text-xs text-gray-600 mt-0.5">
                  This widget will display data from the device selected in the dashboard dropdown
                </p>
              </div>
            </label>
          </div>

          {/* Device Selection */}
          {!formData.useContextDevice && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Device *
              </label>
              <select
                value={formData.deviceId}
                onChange={(e) => setFormData({ ...formData, deviceId: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {devices.map((device) => (
                  <option key={device.externalId} value={device.externalId}>
                    {device.name} ({device.externalId})
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Variable */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Variable *
            </label>
            <select
              value={formData.variableName}
              onChange={(e) => setFormData({ ...formData, variableName: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {variables.map((variable) => (
                <option key={variable} value={variable}>
                  {variable}
                </option>
              ))}
            </select>
          </div>

          {/* Size */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Width (columns)
              </label>
              <input
                type="number"
                min="1"
                max="12"
                value={formData.width}
                onChange={(e) => setFormData({ ...formData, width: parseInt(e.target.value) })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Height (rows)
              </label>
              <input
                type="number"
                min="1"
                max="10"
                value={formData.height}
                onChange={(e) => setFormData({ ...formData, height: parseInt(e.target.value) })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Time Range for Charts */}
          {['LINE_CHART', 'BAR_CHART', 'AREA_CHART'].includes(formData.type) && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Time Range (minutes)
              </label>
              <input
                type="number"
                min="5"
                value={formData.timeRangeMinutes || 60}
                onChange={(e) => setFormData({ ...formData, timeRangeMinutes: parseInt(e.target.value) })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          )}

          {/* Min/Max for Gauge */}
          {formData.type === 'GAUGE' && (
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Min Value
                </label>
                <input
                  type="number"
                  value={formData.config.min ?? 0}
                  onChange={(e) => setFormData({
                    ...formData,
                    config: { ...formData.config, min: parseFloat(e.target.value) }
                  })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Max Value
                </label>
                <input
                  type="number"
                  value={formData.config.max ?? 100}
                  onChange={(e) => setFormData({
                    ...formData,
                    config: { ...formData.config, max: parseFloat(e.target.value) }
                  })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          )}

          {/* Buttons */}
          <div className="flex justify-end gap-3 mt-6">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50"
              disabled={loading}
            >
              {loading ? 'Updating...' : 'Update Widget'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
