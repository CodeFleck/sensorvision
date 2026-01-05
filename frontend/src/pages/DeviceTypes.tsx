import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

// Types matching backend DTOs
interface DeviceTypeVariable {
  id?: number;
  name: string;
  label: string;
  unit: string;
  dataType: 'NUMBER' | 'BOOLEAN' | 'STRING' | 'LOCATION' | 'DATETIME' | 'JSON';
  minValue?: number;
  maxValue?: number;
  required: boolean;
  defaultValue?: string;
  description?: string;
  displayOrder?: number;
}

interface DeviceTypeRuleTemplate {
  id?: number;
  name: string;
  description?: string;
  variableName: string;
  operator: 'GT' | 'GTE' | 'LT' | 'LTE' | 'EQ';
  thresholdValue: number;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  notificationMessage?: string;
  enabled: boolean;
  displayOrder?: number;
}

interface DeviceTypeDashboardTemplate {
  id?: number;
  widgetType: 'LINE_CHART' | 'GAUGE' | 'METRIC' | 'TABLE' | 'MAP' | 'STATUS_INDICATOR' | 'CONTROL_BUTTON' | 'THERMOMETER' | 'TANK' | 'IMAGE' | 'TEXT';
  title: string;
  variableName?: string;
  config?: Record<string, unknown>;
  gridX: number;
  gridY: number;
  gridWidth: number;
  gridHeight: number;
  displayOrder?: number;
}

interface DeviceType {
  id: number;
  name: string;
  description?: string;
  icon?: string;
  color?: string;
  templateCategory?: 'ENERGY' | 'ENVIRONMENTAL' | 'INDUSTRIAL' | 'SMART_HOME' | 'CUSTOM';
  isSystemTemplate: boolean;
  isActive: boolean;
  variables: DeviceTypeVariable[];
  ruleTemplates: DeviceTypeRuleTemplate[];
  dashboardTemplates: DeviceTypeDashboardTemplate[];
  createdAt: string;
  updatedAt?: string;
}

interface Device {
  id: number;
  externalId: string;
  name: string;
}

const CATEGORY_COLORS: Record<string, string> = {
  ENERGY: 'bg-yellow-100 text-yellow-800',
  ENVIRONMENTAL: 'bg-green-100 text-green-800',
  INDUSTRIAL: 'bg-gray-100 text-gray-800',
  SMART_HOME: 'bg-blue-100 text-blue-800',
  CUSTOM: 'bg-purple-100 text-purple-800',
};

const CATEGORY_ICONS: Record<string, string> = {
  ENERGY: 'zap',
  ENVIRONMENTAL: 'thermometer',
  INDUSTRIAL: 'settings',
  SMART_HOME: 'home',
  CUSTOM: 'box',
};

const DeviceTypes: React.FC = () => {
  const [deviceTypes, setDeviceTypes] = useState<DeviceType[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [showApplyModal, setShowApplyModal] = useState(false);
  const [selectedType, setSelectedType] = useState<DeviceType | null>(null);
  const [selectedDeviceId, setSelectedDeviceId] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'details' | 'variables' | 'rules' | 'dashboard'>('details');
  const [applyLoading, setApplyLoading] = useState(false);
  const [applySuccess, setApplySuccess] = useState<string | null>(null);

  // Form state for creating new device type
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    icon: '',
    color: '#3B82F6',
    category: 'CUSTOM' as DeviceType['templateCategory'],
  });
  const [formVariables, setFormVariables] = useState<DeviceTypeVariable[]>([]);

  useEffect(() => {
    loadDeviceTypes();
    loadDevices();
  }, []);

  const loadDeviceTypes = async () => {
    try {
      setLoading(true);
      const response = await apiService.get<DeviceType[]>('/device-types');
      setDeviceTypes(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to load device types');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadDevices = async () => {
    try {
      const response = await apiService.get<Device[]>('/devices');
      setDevices(response.data);
    } catch (err) {
      console.error('Failed to load devices:', err);
    }
  };

  const handleViewDetails = (deviceType: DeviceType) => {
    setSelectedType(deviceType);
    setActiveTab('details');
    setShowModal(true);
  };

  const handleApplyTemplate = (deviceType: DeviceType) => {
    setSelectedType(deviceType);
    setSelectedDeviceId('');
    setApplySuccess(null);
    setShowApplyModal(true);
  };

  const handleApplyConfirm = async () => {
    if (!selectedType || !selectedDeviceId) return;

    setApplyLoading(true);
    setError(null);

    try {
      const response = await apiService.post(`/device-types/${selectedType.id}/apply/${selectedDeviceId}`, {});
      setApplySuccess(`Template applied successfully! Created ${(response.data as { variablesCreated: number }).variablesCreated} variables, ${(response.data as { rulesCreated: number }).rulesCreated} rules${(response.data as { dashboardCreated: boolean }).dashboardCreated ? ', and a dashboard' : ''}.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to apply template');
    } finally {
      setApplyLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this device type?')) return;

    try {
      await apiService.delete(`/device-types/${id}`);
      await loadDeviceTypes();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete device type');
    }
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateLoading(true);
    setError(null);

    try {
      await apiService.post('/device-types', {
        name: formData.name,
        description: formData.description,
        icon: formData.icon || undefined,
        color: formData.color,
        category: formData.category,
        variables: formVariables.length > 0 ? formVariables : undefined,
      });
      await loadDeviceTypes();
      setShowCreateModal(false);
      resetCreateForm();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create device type');
    } finally {
      setCreateLoading(false);
    }
  };

  const resetCreateForm = () => {
    setFormData({
      name: '',
      description: '',
      icon: '',
      color: '#3B82F6',
      category: 'CUSTOM',
    });
    setFormVariables([]);
  };

  const addVariable = () => {
    setFormVariables([
      ...formVariables,
      {
        name: '',
        label: '',
        unit: '',
        dataType: 'NUMBER',
        required: false,
        displayOrder: formVariables.length,
      },
    ]);
  };

  const updateVariable = (index: number, field: keyof DeviceTypeVariable, value: unknown) => {
    const updated = [...formVariables];
    updated[index] = { ...updated[index], [field]: value };
    setFormVariables(updated);
  };

  const removeVariable = (index: number) => {
    setFormVariables(formVariables.filter((_, i) => i !== index));
  };

  const renderCategoryBadge = (category?: string) => {
    if (!category) return null;
    return (
      <span className={`px-2 py-1 text-xs font-medium rounded-full ${CATEGORY_COLORS[category] || 'bg-gray-100 text-gray-800'}`}>
        {category.replace('_', ' ')}
      </span>
    );
  };

  const renderSystemBadge = (isSystem: boolean) => {
    if (!isSystem) return null;
    return (
      <span className="px-2 py-1 text-xs font-medium rounded-full bg-indigo-100 text-indigo-800">
        System Template
      </span>
    );
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-primary">Device Types</h1>
          <p className="text-secondary mt-1">Manage device type templates for auto-provisioning</p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition"
        >
          + Create Device Type
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4">
          {error}
          <button onClick={() => setError(null)} className="float-right font-bold">&times;</button>
        </div>
      )}

      {loading ? (
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {deviceTypes.map((deviceType) => (
            <div
              key={deviceType.id}
              className="bg-card rounded-lg shadow-md hover:shadow-lg transition cursor-pointer border border-primary/10"
              onClick={() => handleViewDetails(deviceType)}
            >
              <div className="p-6">
                <div className="flex justify-between items-start mb-3">
                  <div className="flex items-center gap-3">
                    <div
                      className="w-12 h-12 rounded-lg flex items-center justify-center text-white text-xl"
                      style={{ backgroundColor: deviceType.color || '#3B82F6' }}
                    >
                      {deviceType.icon || CATEGORY_ICONS[deviceType.templateCategory || 'CUSTOM']?.charAt(0).toUpperCase() || 'D'}
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold text-primary">{deviceType.name}</h3>
                      <div className="flex gap-2 mt-1">
                        {renderCategoryBadge(deviceType.templateCategory)}
                        {renderSystemBadge(deviceType.isSystemTemplate)}
                      </div>
                    </div>
                  </div>
                </div>

                {deviceType.description && (
                  <p className="text-secondary text-sm mb-4 line-clamp-2">{deviceType.description}</p>
                )}

                <div className="flex justify-between items-center text-sm text-secondary border-t border-primary/10 pt-4 mt-4">
                  <div className="flex gap-4">
                    <span>{deviceType.variables?.length || 0} variables</span>
                    <span>{deviceType.ruleTemplates?.length || 0} rules</span>
                    <span>{deviceType.dashboardTemplates?.length || 0} widgets</span>
                  </div>
                </div>

                <div className="flex gap-2 mt-4" onClick={(e) => e.stopPropagation()}>
                  <button
                    onClick={() => handleApplyTemplate(deviceType)}
                    className="flex-1 bg-green-600 text-white py-2 px-3 rounded text-sm hover:bg-green-700 transition"
                  >
                    Apply to Device
                  </button>
                  {!deviceType.isSystemTemplate && (
                    <button
                      onClick={() => handleDelete(deviceType.id)}
                      className="bg-red-100 text-red-700 py-2 px-3 rounded text-sm hover:bg-red-200 transition"
                    >
                      Delete
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {deviceTypes.length === 0 && !loading && (
        <div className="text-center py-12 bg-card rounded-lg border border-primary/10">
          <p className="text-secondary mb-4">No device types found</p>
          <button
            onClick={() => setShowCreateModal(true)}
            className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700"
          >
            Create Your First Device Type
          </button>
        </div>
      )}

      {/* Detail Modal */}
      {showModal && selectedType && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-card rounded-lg max-w-4xl w-full max-h-[90vh] overflow-hidden">
            <div className="p-6 border-b border-primary/10">
              <div className="flex justify-between items-start">
                <div className="flex items-center gap-4">
                  <div
                    className="w-14 h-14 rounded-lg flex items-center justify-center text-white text-2xl"
                    style={{ backgroundColor: selectedType.color || '#3B82F6' }}
                  >
                    {selectedType.icon || 'D'}
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-primary">{selectedType.name}</h2>
                    <div className="flex gap-2 mt-1">
                      {renderCategoryBadge(selectedType.templateCategory)}
                      {renderSystemBadge(selectedType.isSystemTemplate)}
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => setShowModal(false)}
                  className="text-secondary hover:text-primary text-2xl"
                >
                  &times;
                </button>
              </div>
            </div>

            {/* Tabs */}
            <div className="border-b border-primary/10">
              <nav className="flex">
                {(['details', 'variables', 'rules', 'dashboard'] as const).map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    className={`px-6 py-3 text-sm font-medium border-b-2 transition ${
                      activeTab === tab
                        ? 'border-blue-600 text-blue-600'
                        : 'border-transparent text-secondary hover:text-primary'
                    }`}
                  >
                    {tab.charAt(0).toUpperCase() + tab.slice(1)}
                    {tab === 'variables' && ` (${selectedType.variables?.length || 0})`}
                    {tab === 'rules' && ` (${selectedType.ruleTemplates?.length || 0})`}
                    {tab === 'dashboard' && ` (${selectedType.dashboardTemplates?.length || 0})`}
                  </button>
                ))}
              </nav>
            </div>

            {/* Tab Content */}
            <div className="p-6 overflow-y-auto max-h-[60vh]">
              {activeTab === 'details' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-secondary">Description</label>
                    <p className="mt-1 text-primary">{selectedType.description || 'No description'}</p>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-secondary">Created</label>
                      <p className="mt-1 text-primary">{new Date(selectedType.createdAt).toLocaleString()}</p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-secondary">Last Updated</label>
                      <p className="mt-1 text-primary">{selectedType.updatedAt ? new Date(selectedType.updatedAt).toLocaleString() : 'Never'}</p>
                    </div>
                  </div>
                </div>
              )}

              {activeTab === 'variables' && (
                <div className="space-y-3">
                  {selectedType.variables?.length > 0 ? (
                    selectedType.variables.map((variable, index) => (
                      <div key={index} className="bg-surface p-4 rounded-lg border border-primary/10">
                        <div className="flex justify-between items-start">
                          <div>
                            <h4 className="font-medium text-primary">{variable.label || variable.name}</h4>
                            <p className="text-sm text-secondary">{variable.name}</p>
                          </div>
                          <div className="flex gap-2">
                            <span className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded">{variable.dataType}</span>
                            {variable.required && <span className="px-2 py-1 bg-red-100 text-red-800 text-xs rounded">Required</span>}
                          </div>
                        </div>
                        <div className="mt-2 text-sm text-secondary">
                          {variable.unit && <span>Unit: {variable.unit}</span>}
                          {variable.minValue !== undefined && variable.maxValue !== undefined && (
                            <span className="ml-4">Range: {variable.minValue} - {variable.maxValue}</span>
                          )}
                        </div>
                      </div>
                    ))
                  ) : (
                    <p className="text-secondary text-center py-8">No variables defined</p>
                  )}
                </div>
              )}

              {activeTab === 'rules' && (
                <div className="space-y-3">
                  {selectedType.ruleTemplates?.length > 0 ? (
                    selectedType.ruleTemplates.map((rule, index) => (
                      <div key={index} className="bg-surface p-4 rounded-lg border border-primary/10">
                        <div className="flex justify-between items-start">
                          <div>
                            <h4 className="font-medium text-primary">{rule.name}</h4>
                            <p className="text-sm text-secondary">{rule.description}</p>
                          </div>
                          <span className={`px-2 py-1 text-xs rounded ${
                            rule.severity === 'CRITICAL' ? 'bg-red-100 text-red-800' :
                            rule.severity === 'WARNING' ? 'bg-yellow-100 text-yellow-800' :
                            'bg-blue-100 text-blue-800'
                          }`}>
                            {rule.severity}
                          </span>
                        </div>
                        <div className="mt-2 text-sm font-mono bg-card p-2 rounded">
                          {rule.variableName} {rule.operator} {rule.thresholdValue}
                        </div>
                      </div>
                    ))
                  ) : (
                    <p className="text-secondary text-center py-8">No rule templates defined</p>
                  )}
                </div>
              )}

              {activeTab === 'dashboard' && (
                <div className="space-y-3">
                  {selectedType.dashboardTemplates?.length > 0 ? (
                    selectedType.dashboardTemplates.map((widget, index) => (
                      <div key={index} className="bg-surface p-4 rounded-lg border border-primary/10">
                        <div className="flex justify-between items-start">
                          <div>
                            <h4 className="font-medium text-primary">{widget.title}</h4>
                            <p className="text-sm text-secondary">Variable: {widget.variableName || 'N/A'}</p>
                          </div>
                          <span className="px-2 py-1 bg-purple-100 text-purple-800 text-xs rounded">
                            {widget.widgetType.replace('_', ' ')}
                          </span>
                        </div>
                        <div className="mt-2 text-sm text-secondary">
                          Position: ({widget.gridX}, {widget.gridY}) | Size: {widget.gridWidth}x{widget.gridHeight}
                        </div>
                      </div>
                    ))
                  ) : (
                    <p className="text-secondary text-center py-8">No dashboard widgets defined</p>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Apply Template Modal */}
      {showApplyModal && selectedType && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-card rounded-lg max-w-md w-full p-6">
            <h2 className="text-xl font-bold text-primary mb-4">Apply Template to Device</h2>
            <p className="text-secondary mb-4">
              Apply <strong>{selectedType.name}</strong> template to a device. This will create:
            </p>
            <ul className="list-disc list-inside text-secondary mb-4 text-sm">
              <li>{selectedType.variables?.length || 0} variables</li>
              <li>{selectedType.ruleTemplates?.length || 0} alert rules</li>
              <li>{selectedType.dashboardTemplates?.length || 0 > 0 ? 'A custom dashboard' : 'No dashboard'}</li>
            </ul>

            {applySuccess ? (
              <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-4">
                {applySuccess}
              </div>
            ) : (
              <div className="mb-4">
                <label className="block text-sm font-medium text-primary mb-2">Select Device</label>
                <select
                  value={selectedDeviceId}
                  onChange={(e) => setSelectedDeviceId(e.target.value)}
                  className="w-full px-3 py-2 border border-primary/20 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 bg-card text-primary"
                >
                  <option value="">Select a device...</option>
                  {devices.map((device) => (
                    <option key={device.id} value={device.externalId}>
                      {device.name} ({device.externalId})
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="flex gap-3">
              <button
                onClick={() => {
                  setShowApplyModal(false);
                  setApplySuccess(null);
                }}
                className="flex-1 bg-secondary/20 text-primary py-2 rounded-md hover:bg-secondary/30 transition"
              >
                {applySuccess ? 'Close' : 'Cancel'}
              </button>
              {!applySuccess && (
                <button
                  onClick={handleApplyConfirm}
                  disabled={!selectedDeviceId || applyLoading}
                  className="flex-1 bg-green-600 text-white py-2 rounded-md hover:bg-green-700 disabled:bg-gray-400 transition"
                >
                  {applyLoading ? 'Applying...' : 'Apply Template'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Create Device Type Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-card rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-primary/10">
              <div className="flex justify-between items-center">
                <h2 className="text-xl font-bold text-primary">Create Device Type</h2>
                <button onClick={() => setShowCreateModal(false)} className="text-secondary hover:text-primary text-2xl">
                  &times;
                </button>
              </div>
            </div>

            <form onSubmit={handleCreateSubmit} className="p-6 space-y-6">
              {/* Basic Info */}
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-primary mb-1">Name *</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border border-primary/20 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 bg-card text-primary"
                    placeholder="My Device Type"
                    required
                  />
                </div>
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-primary mb-1">Description</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-3 py-2 border border-primary/20 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 bg-card text-primary"
                    placeholder="Description of this device type..."
                    rows={2}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-primary mb-1">Category</label>
                  <select
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value as DeviceType['templateCategory'] })}
                    className="w-full px-3 py-2 border border-primary/20 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 bg-card text-primary"
                  >
                    <option value="ENERGY">Energy</option>
                    <option value="ENVIRONMENTAL">Environmental</option>
                    <option value="INDUSTRIAL">Industrial</option>
                    <option value="SMART_HOME">Smart Home</option>
                    <option value="CUSTOM">Custom</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-primary mb-1">Color</label>
                  <input
                    type="color"
                    value={formData.color}
                    onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                    className="w-full h-10 px-1 py-1 border border-primary/20 rounded-md cursor-pointer"
                  />
                </div>
              </div>

              {/* Variables Section */}
              <div>
                <div className="flex justify-between items-center mb-3">
                  <h3 className="text-lg font-medium text-primary">Variables</h3>
                  <button
                    type="button"
                    onClick={addVariable}
                    className="text-blue-600 hover:text-blue-800 text-sm"
                  >
                    + Add Variable
                  </button>
                </div>

                {formVariables.length === 0 ? (
                  <p className="text-secondary text-sm text-center py-4 bg-surface rounded-lg">
                    No variables yet. Click &quot;Add Variable&quot; to define sensor data points.
                  </p>
                ) : (
                  <div className="space-y-3">
                    {formVariables.map((variable, index) => (
                      <div key={index} className="bg-surface p-4 rounded-lg border border-primary/10">
                        <div className="grid grid-cols-4 gap-3">
                          <div>
                            <label className="block text-xs text-secondary mb-1">Name *</label>
                            <input
                              type="text"
                              value={variable.name}
                              onChange={(e) => updateVariable(index, 'name', e.target.value)}
                              className="w-full px-2 py-1 border border-primary/20 rounded text-sm bg-card text-primary"
                              placeholder="temperature"
                              required
                            />
                          </div>
                          <div>
                            <label className="block text-xs text-secondary mb-1">Label</label>
                            <input
                              type="text"
                              value={variable.label}
                              onChange={(e) => updateVariable(index, 'label', e.target.value)}
                              className="w-full px-2 py-1 border border-primary/20 rounded text-sm bg-card text-primary"
                              placeholder="Temperature"
                            />
                          </div>
                          <div>
                            <label className="block text-xs text-secondary mb-1">Unit</label>
                            <input
                              type="text"
                              value={variable.unit}
                              onChange={(e) => updateVariable(index, 'unit', e.target.value)}
                              className="w-full px-2 py-1 border border-primary/20 rounded text-sm bg-card text-primary"
                              placeholder="°C"
                            />
                          </div>
                          <div>
                            <label className="block text-xs text-secondary mb-1">Type</label>
                            <select
                              value={variable.dataType}
                              onChange={(e) => updateVariable(index, 'dataType', e.target.value)}
                              className="w-full px-2 py-1 border border-primary/20 rounded text-sm bg-card text-primary"
                            >
                              <option value="NUMBER">Number</option>
                              <option value="BOOLEAN">Boolean</option>
                              <option value="STRING">String</option>
                              <option value="JSON">JSON</option>
                            </select>
                          </div>
                        </div>
                        <div className="flex justify-between items-center mt-2">
                          <label className="flex items-center text-sm text-secondary">
                            <input
                              type="checkbox"
                              checked={variable.required}
                              onChange={(e) => updateVariable(index, 'required', e.target.checked)}
                              className="mr-2"
                            />
                            Required field
                          </label>
                          <button
                            type="button"
                            onClick={() => removeVariable(index)}
                            className="text-red-600 hover:text-red-800 text-sm"
                          >
                            Remove
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Actions */}
              <div className="flex gap-3 pt-4 border-t border-primary/10">
                <button
                  type="button"
                  onClick={() => {
                    setShowCreateModal(false);
                    resetCreateForm();
                  }}
                  className="flex-1 bg-secondary/20 text-primary py-2 rounded-md hover:bg-secondary/30 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createLoading || !formData.name}
                  className="flex-1 bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 disabled:bg-gray-400 transition"
                >
                  {createLoading ? 'Creating...' : 'Create Device Type'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default DeviceTypes;
