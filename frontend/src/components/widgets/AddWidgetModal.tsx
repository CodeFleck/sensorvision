import React, { useState, useEffect } from 'react';
import { WidgetType, WidgetCreateRequest, Device, DeviceVariable } from '../../types';
import { apiService } from '../../services/api';

interface AddWidgetModalProps {
  dashboardId: number;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const AddWidgetModal: React.FC<AddWidgetModalProps> = ({
  dashboardId,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [variables, setVariables] = useState<DeviceVariable[]>([]);
  const [loadingVariables, setLoadingVariables] = useState(false);
  const [secondDeviceVariables, setSecondDeviceVariables] = useState<DeviceVariable[]>([]);
  const [loadingSecondVariables, setLoadingSecondVariables] = useState(false);
  const [enableDualDevice, setEnableDualDevice] = useState(false);
  const [formData, setFormData] = useState<WidgetCreateRequest>({
    name: '',
    type: 'GAUGE',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 4,
    deviceId: '',
    secondDeviceId: undefined,
    variableName: '',
    secondVariableName: undefined,
    deviceLabel: undefined,
    secondDeviceLabel: undefined,
    aggregation: 'LAST',
    timeRangeMinutes: 60,
    config: {},
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      loadDevices();
    }
  }, [isOpen]);

  // Fetch variables when device is selected
  useEffect(() => {
    const loadVariables = async () => {
      if (!formData.deviceId) {
        setVariables([]);
        return;
      }

      setLoadingVariables(true);
      try {
        // Find the device to get its UUID (needed for the variables API)
        const device = devices.find(d => d.externalId === formData.deviceId);
        if (device?.id) {
          const vars = await apiService.getDeviceVariables(device.id);
          setVariables(vars);
          // Auto-select the first variable if none selected
          if (vars.length > 0 && !formData.variableName) {
            setFormData(prev => ({ ...prev, variableName: vars[0].name }));
          }
        } else {
          setVariables([]);
        }
      } catch (error) {
        console.error('Failed to load variables:', error);
        setVariables([]);
      } finally {
        setLoadingVariables(false);
      }
    };

    loadVariables();
  }, [formData.deviceId, devices]);

  // Fetch variables for second device when selected (dual-device mode)
  useEffect(() => {
    const loadSecondVariables = async () => {
      if (!formData.secondDeviceId || !enableDualDevice) {
        setSecondDeviceVariables([]);
        return;
      }

      setLoadingSecondVariables(true);
      try {
        const device = devices.find(d => d.externalId === formData.secondDeviceId);
        if (device?.id) {
          const vars = await apiService.getDeviceVariables(device.id);
          setSecondDeviceVariables(vars);
          // Auto-select the first variable if none selected
          if (vars.length > 0 && !formData.secondVariableName) {
            setFormData(prev => ({ ...prev, secondVariableName: vars[0].name }));
          }
        } else {
          setSecondDeviceVariables([]);
        }
      } catch (error) {
        console.error('Failed to load second device variables:', error);
        setSecondDeviceVariables([]);
      } finally {
        setLoadingSecondVariables(false);
      }
    };

    loadSecondVariables();
  }, [formData.secondDeviceId, devices, enableDualDevice]);

  const loadDevices = async () => {
    try {
      const deviceList = await apiService.getDevices();
      setDevices(deviceList);
      if (deviceList.length > 0) {
        setFormData((prev) => ({ ...prev, deviceId: deviceList[0].externalId }));
      }
    } catch (error) {
      console.error('Failed to load devices:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      await apiService.createWidget(dashboardId, formData);
      onSuccess();
      onClose();
      // Reset form
      setEnableDualDevice(false);
      setFormData({
        name: '',
        type: 'GAUGE',
        positionX: 0,
        positionY: 0,
        width: 4,
        height: 4,
        deviceId: devices[0]?.externalId || '',
        secondDeviceId: undefined,
        variableName: variables[0]?.name || '',
        secondVariableName: undefined,
        deviceLabel: undefined,
        secondDeviceLabel: undefined,
        aggregation: 'LAST',
        timeRangeMinutes: 60,
        config: {},
      });
    } catch (error) {
      console.error('Failed to create widget:', error);
      alert('Failed to create widget');
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

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-gray-800">Add Widget</h2>
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

          {/* Primary Device Selection */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Primary Device *
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

          {/* Primary Variable */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Primary Variable *
            </label>
            <select
              value={formData.variableName}
              onChange={(e) => setFormData({ ...formData, variableName: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              disabled={loadingVariables}
            >
              {loadingVariables ? (
                <option value="">Loading variables...</option>
              ) : variables.length === 0 ? (
                <option value="">No variables available</option>
              ) : (
                variables.map((variable) => (
                  <option key={variable.id} value={variable.name}>
                    {variable.displayName || variable.name}
                    {variable.unit ? ` (${variable.unit})` : ''}
                  </option>
                ))
              )}
            </select>
            {!loadingVariables && variables.length === 0 && formData.deviceId && (
              <p className="text-xs text-yellow-600 mt-1">
                No variables found. Send telemetry data to this device first.
              </p>
            )}
          </div>

          {/* Primary Device Label */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Primary Device Label (optional)
            </label>
            <input
              type="text"
              value={formData.deviceLabel || ''}
              onChange={(e) => setFormData({ ...formData, deviceLabel: e.target.value || undefined })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g., Sensor A, Line 1, etc."
            />
            <p className="text-xs text-gray-500 mt-1">
              Custom label shown in chart legend (defaults to device ID if empty)
            </p>
          </div>

          {/* Dual Device Toggle */}
          {['LINE_CHART', 'BAR_CHART', 'AREA_CHART'].includes(formData.type) && (
            <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={enableDualDevice}
                  onChange={(e) => {
                    setEnableDualDevice(e.target.checked);
                    if (!e.target.checked) {
                      setFormData({
                        ...formData,
                        secondDeviceId: undefined,
                        secondVariableName: undefined,
                        secondDeviceLabel: undefined,
                      });
                    }
                  }}
                  className="w-4 h-4 text-purple-600 border-gray-300 rounded focus:ring-purple-500"
                />
                <div className="ml-3">
                  <span className="text-sm font-medium text-gray-900">
                    Compare two devices
                  </span>
                  <p className="text-xs text-gray-600 mt-0.5">
                    Display data from two different devices in the same chart for comparison
                  </p>
                </div>
              </label>
            </div>
          )}

          {/* Second Device Fields */}
          {enableDualDevice && ['LINE_CHART', 'BAR_CHART', 'AREA_CHART'].includes(formData.type) && (
            <>
              {/* Second Device Selection */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Second Device *
                </label>
                <select
                  value={formData.secondDeviceId || ''}
                  onChange={(e) => setFormData({ ...formData, secondDeviceId: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
                >
                  <option value="">Select a device...</option>
                  {devices.map((device) => (
                    <option key={device.externalId} value={device.externalId}>
                      {device.name} ({device.externalId})
                    </option>
                  ))}
                </select>
              </div>

              {/* Second Variable */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Second Variable *
                </label>
                <select
                  value={formData.secondVariableName || ''}
                  onChange={(e) => setFormData({ ...formData, secondVariableName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
                  disabled={loadingSecondVariables || !formData.secondDeviceId}
                >
                  {!formData.secondDeviceId ? (
                    <option value="">Select a device first</option>
                  ) : loadingSecondVariables ? (
                    <option value="">Loading variables...</option>
                  ) : secondDeviceVariables.length === 0 ? (
                    <option value="">No variables available</option>
                  ) : (
                    secondDeviceVariables.map((variable) => (
                      <option key={variable.id} value={variable.name}>
                        {variable.displayName || variable.name}
                        {variable.unit ? ` (${variable.unit})` : ''}
                      </option>
                    ))
                  )}
                </select>
                {!loadingSecondVariables && secondDeviceVariables.length === 0 && formData.secondDeviceId && (
                  <p className="text-xs text-yellow-600 mt-1">
                    No variables found. Send telemetry data to this device first.
                  </p>
                )}
              </div>

              {/* Second Device Label */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Second Device Label (optional)
                </label>
                <input
                  type="text"
                  value={formData.secondDeviceLabel || ''}
                  onChange={(e) => setFormData({ ...formData, secondDeviceLabel: e.target.value || undefined })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
                  placeholder="e.g., Sensor B, Line 2, etc."
                />
                <p className="text-xs text-gray-500 mt-1">
                  Custom label for second device in chart legend
                </p>
              </div>
            </>
          )}

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
              {loading ? 'Creating...' : 'Create Widget'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
