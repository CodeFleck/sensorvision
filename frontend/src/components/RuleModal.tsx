import { useState, useEffect } from 'react';
import { X, Plus, Trash2, MessageSquare } from 'lucide-react';
import { Rule, Device, PhoneNumber, DeviceVariable } from '../types';
import { apiService } from '../services/api';

interface RuleModalProps {
  rule: Rule | null;
  devices: Device[];
  onClose: () => void;
}

export const RuleModal = ({ rule, devices, onClose }: RuleModalProps) => {
  const [formData, setFormData] = useState({
    name: rule?.name || '',
    description: rule?.description || '',
    deviceId: rule?.deviceId || (devices[0]?.externalId || ''),
    variable: rule?.variable || '',
    operator: rule?.operator || 'GT',
    threshold: rule?.threshold?.toString() || '',
    enabled: rule?.enabled ?? true,
    sendSms: rule?.sendSms ?? false,
    smsRecipients: rule?.smsRecipients || [],
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [phoneNumbers, setPhoneNumbers] = useState<PhoneNumber[]>([]);
  const [newRecipient, setNewRecipient] = useState('');
  const [variables, setVariables] = useState<DeviceVariable[]>([]);
  const [loadingVariables, setLoadingVariables] = useState(false);

  useEffect(() => {
    fetchPhoneNumbers();
  }, []);

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
          // Auto-select the first variable if none selected or if current is not in list
          if (vars.length > 0) {
            const currentVarExists = vars.some(v => v.name === formData.variable);
            if (!currentVarExists) {
              setFormData(prev => ({ ...prev, variable: vars[0].name }));
            }
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [formData.deviceId, devices]);

  const fetchPhoneNumbers = async () => {
    try {
      const data = await apiService.getPhoneNumbers();
      const verified = data.filter(p => p.verified && p.enabled);
      setPhoneNumbers(verified);
    } catch (error) {
      console.error('Failed to fetch phone numbers:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const ruleData = {
        ...formData,
        threshold: parseFloat(formData.threshold),
        smsRecipients: formData.sendSms ? formData.smsRecipients : [],
      };

      if (rule) {
        await apiService.updateRule(rule.id, ruleData);
      } else {
        await apiService.createRule(ruleData);
      }
      onClose();
    } catch (error) {
      setError(error instanceof Error ? error.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleAddRecipient = (phoneNumber: string) => {
    if (phoneNumber && !formData.smsRecipients.includes(phoneNumber)) {
      setFormData(prev => ({
        ...prev,
        smsRecipients: [...prev.smsRecipients, phoneNumber],
      }));
      setNewRecipient('');
    }
  };

  const handleRemoveRecipient = (phoneNumber: string) => {
    setFormData(prev => ({
      ...prev,
      smsRecipients: prev.smsRecipients.filter(p => p !== phoneNumber),
    }));
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value,
    }));
  };

  const operatorOptions = [
    { value: 'GT', label: 'Greater than (>)' },
    { value: 'GTE', label: 'Greater than or equal (≥)' },
    { value: 'LT', label: 'Less than (<)' },
    { value: 'LTE', label: 'Less than or equal (≤)' },
    { value: 'EQ', label: 'Equal to (=)' },
  ];

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">
            {rule ? 'Edit Rule' : 'Create New Rule'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-300 text-red-700 rounded">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
              Rule Name *
            </label>
            <input
              type="text"
              id="name"
              name="name"
              required
              value={formData.name}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="e.g., High Power Alert"
            />
          </div>

          <div>
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              id="description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Optional description"
              rows={2}
            />
          </div>

          <div>
            <label htmlFor="deviceId" className="block text-sm font-medium text-gray-700 mb-1">
              Device *
            </label>
            <select
              id="deviceId"
              name="deviceId"
              required
              value={formData.deviceId}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              {devices.map((device) => (
                <option key={device.externalId} value={device.externalId}>
                  {device.name} ({device.externalId})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label htmlFor="variable" className="block text-sm font-medium text-gray-700 mb-1">
                Variable *
              </label>
              <select
                id="variable"
                name="variable"
                required
                value={formData.variable}
                onChange={handleChange}
                disabled={loadingVariables}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                {loadingVariables ? (
                  <option value="">Loading...</option>
                ) : variables.length === 0 ? (
                  <option value="">No variables</option>
                ) : (
                  variables.map((variable) => (
                    <option key={variable.id} value={variable.name}>
                      {variable.displayName || variable.name}
                      {variable.unit ? ` (${variable.unit})` : ''}
                    </option>
                  ))
                )}
              </select>
            </div>

            <div>
              <label htmlFor="operator" className="block text-sm font-medium text-gray-700 mb-1">
                Operator *
              </label>
              <select
                id="operator"
                name="operator"
                required
                value={formData.operator}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                {operatorOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="threshold" className="block text-sm font-medium text-gray-700 mb-1">
                Threshold *
              </label>
              <input
                type="text"
                inputMode="decimal"
                pattern="[0-9]*\.?[0-9]*"
                id="threshold"
                name="threshold"
                required
                value={formData.threshold}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="100"
              />
            </div>
          </div>

          <div className="flex items-center">
            <input
              type="checkbox"
              id="enabled"
              name="enabled"
              checked={formData.enabled}
              onChange={handleChange}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="enabled" className="ml-2 block text-sm text-gray-700">
              Enable rule immediately
            </label>
          </div>

          {/* SMS Notification Section */}
          <div className="border-t pt-4 mt-4">
            <div className="flex items-center mb-3">
              <input
                type="checkbox"
                id="sendSms"
                name="sendSms"
                checked={formData.sendSms}
                onChange={handleChange}
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <label htmlFor="sendSms" className="ml-2 flex items-center text-sm font-medium text-gray-700">
                <MessageSquare className="h-4 w-4 mr-1" />
                Send SMS notifications
              </label>
            </div>

            {formData.sendSms && (
              <div className="ml-6 space-y-3">
                {phoneNumbers.length === 0 ? (
                  <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
                    <p className="text-sm text-yellow-800">
                      No verified phone numbers. <a href="/phone-numbers" className="underline font-medium">Add a phone number</a> to receive SMS alerts.
                    </p>
                  </div>
                ) : (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        SMS Recipients
                      </label>
                      <div className="flex space-x-2">
                        <select
                          value={newRecipient}
                          onChange={(e) => setNewRecipient(e.target.value)}
                          className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        >
                          <option value="">Select a phone number...</option>
                          {phoneNumbers.map((phone) => (
                            <option key={phone.id} value={phone.phoneNumber}>
                              {phone.phoneNumber} {phone.isPrimary ? '(Primary)' : ''}
                            </option>
                          ))}
                        </select>
                        <button
                          type="button"
                          onClick={() => handleAddRecipient(newRecipient)}
                          disabled={!newRecipient}
                          className="px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          <Plus className="h-5 w-5" />
                        </button>
                      </div>
                    </div>

                    {formData.smsRecipients.length > 0 && (
                      <div>
                        <p className="text-sm text-gray-600 mb-2">Selected recipients:</p>
                        <div className="space-y-1">
                          {formData.smsRecipients.map((phone) => (
                            <div
                              key={phone}
                              className="flex items-center justify-between bg-gray-50 px-3 py-2 rounded"
                            >
                              <span className="text-sm text-gray-900">{phone}</span>
                              <button
                                type="button"
                                onClick={() => handleRemoveRecipient(phone)}
                                className="text-red-600 hover:text-red-700"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                      <p className="text-xs text-blue-800">
                        SMS alerts will be sent to all selected recipients when this rule triggers. Message and data rates may apply.
                      </p>
                    </div>
                  </>
                )}
              </div>
            )}
          </div>

          <div className="flex space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {loading ? 'Saving...' : rule ? 'Update' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};