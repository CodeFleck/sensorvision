import { useState } from 'react';
import { X } from 'lucide-react';

interface FleetRuleFormData {
  name: string;
  description: string;
  selectorType: string;
  selectorValue: string | null;
  aggregationFunction: string;
  aggregationVariable: string | null;
  aggregationParams: Record<string, unknown>;
  operator: string;
  threshold: number;
  enabled: boolean;
  evaluationInterval: string;
  cooldownMinutes: number;
  sendSms: boolean;
  smsRecipients: string[] | null;
}

interface FleetRuleBuilderModalProps {
  rule?: Partial<FleetRuleFormData> | null;
  onClose: () => void;
  onSave: (rule: FleetRuleFormData) => Promise<void>;
}

const SELECTOR_TYPES = [
  { value: 'ORGANIZATION', label: 'All Devices in Organization' },
  { value: 'TAG', label: 'Devices with Tag' },
  { value: 'GROUP', label: 'Devices in Group' },
  { value: 'CUSTOM_FILTER', label: 'Custom Filter' },
];

const AGGREGATION_FUNCTIONS = [
  { value: 'COUNT_DEVICES', label: 'Count Devices', requiresVariable: false },
  { value: 'COUNT_ONLINE', label: 'Count Online Devices', requiresVariable: false },
  { value: 'COUNT_OFFLINE', label: 'Count Offline Devices', requiresVariable: false },
  { value: 'PERCENT_ONLINE', label: 'Percent Online', requiresVariable: false },
  { value: 'SUM', label: 'Sum', requiresVariable: true },
  { value: 'AVG', label: 'Average', requiresVariable: true },
  { value: 'MIN', label: 'Minimum', requiresVariable: true },
  { value: 'MAX', label: 'Maximum', requiresVariable: true },
  { value: 'MEDIAN', label: 'Median', requiresVariable: true },
  { value: 'STDDEV', label: 'Standard Deviation', requiresVariable: true },
  { value: 'VARIANCE', label: 'Variance', requiresVariable: true },
  { value: 'PERCENTILE_95', label: '95th Percentile', requiresVariable: true },
  { value: 'PERCENTILE_99', label: '99th Percentile', requiresVariable: true },
  { value: 'RANGE', label: 'Range (Max - Min)', requiresVariable: true },
];

const OPERATORS = [
  { value: 'GT', label: 'Greater Than (>)' },
  { value: 'GTE', label: 'Greater Than or Equal (>=)' },
  { value: 'LT', label: 'Less Than (<)' },
  { value: 'LTE', label: 'Less Than or Equal (<=)' },
  { value: 'EQ', label: 'Equal (=)' },
];

const EVALUATION_INTERVALS = [
  { value: 'EVERY_MINUTE', label: 'Every Minute' },
  { value: 'EVERY_5_MINUTES', label: 'Every 5 Minutes' },
  { value: 'EVERY_15_MINUTES', label: 'Every 15 Minutes' },
  { value: 'EVERY_30_MINUTES', label: 'Every 30 Minutes' },
  { value: 'EVERY_HOUR', label: 'Every Hour' },
];

export const FleetRuleBuilderModal = ({ rule, onClose, onSave }: FleetRuleBuilderModalProps) => {
  const [formData, setFormData] = useState({
    name: rule?.name || '',
    description: rule?.description || '',
    selectorType: rule?.selectorType || 'ORGANIZATION',
    selectorValue: rule?.selectorValue || '',
    aggregationFunction: rule?.aggregationFunction || 'COUNT_DEVICES',
    aggregationVariable: rule?.aggregationVariable || '',
    aggregationParams: rule?.aggregationParams || {},
    operator: rule?.operator || 'GT',
    threshold: rule?.threshold || 0,
    enabled: rule?.enabled !== undefined ? rule.enabled : true,
    evaluationInterval: rule?.evaluationInterval || 'EVERY_5_MINUTES',
    cooldownMinutes: rule?.cooldownMinutes || 15,
    sendSms: rule?.sendSms || false,
    smsRecipients: rule?.smsRecipients || [],
  });

  const [smsRecipientInput, setSmsRecipientInput] = useState('');
  const [loading, setLoading] = useState(false);

  const selectedFunction = AGGREGATION_FUNCTIONS.find(f => f.value === formData.aggregationFunction);
  const requiresVariable = selectedFunction?.requiresVariable || false;
  const needsSelectorValue = formData.selectorType !== 'ORGANIZATION';

  const handleChange = (field: keyof FleetRuleFormData, value: string | number | boolean | string[]) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleAddSmsRecipient = () => {
    if (smsRecipientInput.trim()) {
      const recipients = [...formData.smsRecipients, smsRecipientInput.trim()];
      handleChange('smsRecipients', recipients);
      setSmsRecipientInput('');
    }
  };

  const handleRemoveSmsRecipient = (index: number) => {
    const recipients = formData.smsRecipients.filter((_, i: number) => i !== index);
    handleChange('smsRecipients', recipients);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await onSave(formData);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg max-w-3xl w-full max-h-[90vh] overflow-y-auto p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold text-gray-900">
            {rule ? 'Edit Fleet Rule' : 'Create Fleet Rule'}
          </h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Basic Information */}
          <div className="space-y-4">
            <h3 className="font-medium text-gray-900">Basic Information</h3>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Rule Name *
              </label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => handleChange('name', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="e.g., High average power consumption"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                rows={2}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="Optional description"
              />
            </div>
          </div>

          {/* Device Selection */}
          <div className="space-y-4">
            <h3 className="font-medium text-gray-900">Device Selection</h3>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Target Devices *
              </label>
              <select
                value={formData.selectorType}
                onChange={(e) => handleChange('selectorType', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                {SELECTOR_TYPES.map(type => (
                  <option key={type.value} value={type.value}>{type.label}</option>
                ))}
              </select>
            </div>

            {needsSelectorValue && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  {formData.selectorType === 'TAG' ? 'Tag Name' :
                   formData.selectorType === 'GROUP' ? 'Group Name' : 'Filter Expression'} *
                </label>
                <input
                  type="text"
                  required
                  value={formData.selectorValue}
                  onChange={(e) => handleChange('selectorValue', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder={formData.selectorType === 'TAG' ? 'production' :
                               formData.selectorType === 'GROUP' ? 'Building A' : 'status == "ONLINE"'}
                />
              </div>
            )}
          </div>

          {/* Aggregation */}
          <div className="space-y-4">
            <h3 className="font-medium text-gray-900">Aggregation</h3>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Aggregation Function *
              </label>
              <select
                value={formData.aggregationFunction}
                onChange={(e) => handleChange('aggregationFunction', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                {AGGREGATION_FUNCTIONS.map(func => (
                  <option key={func.value} value={func.value}>{func.label}</option>
                ))}
              </select>
            </div>

            {requiresVariable && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Variable to Aggregate *
                </label>
                <input
                  type="text"
                  required
                  value={formData.aggregationVariable}
                  onChange={(e) => handleChange('aggregationVariable', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="e.g., kwConsumption, voltage, temperature"
                />
              </div>
            )}
          </div>

          {/* Condition */}
          <div className="space-y-4">
            <h3 className="font-medium text-gray-900">Condition</h3>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Operator *
                </label>
                <select
                  value={formData.operator}
                  onChange={(e) => handleChange('operator', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                >
                  {OPERATORS.map(op => (
                    <option key={op.value} value={op.value}>{op.label}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Threshold *
                </label>
                <input
                  type="text"
                  inputMode="decimal"
                  pattern="[0-9]*\.?[0-9]*"
                  required
                  value={formData.threshold}
                  onChange={(e) => handleChange('threshold', parseFloat(e.target.value) || 0)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
            </div>
          </div>

          {/* Evaluation Settings */}
          <div className="space-y-4">
            <h3 className="font-medium text-gray-900">Evaluation Settings</h3>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Evaluation Interval *
                </label>
                <select
                  value={formData.evaluationInterval}
                  onChange={(e) => handleChange('evaluationInterval', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                >
                  {EVALUATION_INTERVALS.map(interval => (
                    <option key={interval.value} value={interval.value}>{interval.label}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Cooldown (minutes) *
                </label>
                <input
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  required
                  value={formData.cooldownMinutes}
                  onChange={(e) => handleChange('cooldownMinutes', parseInt(e.target.value) || 0)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
            </div>
          </div>

          {/* SMS Notifications */}
          <div className="space-y-4">
            <h3 className="font-medium text-gray-900">SMS Notifications</h3>

            <div className="flex items-center">
              <input
                type="checkbox"
                checked={formData.sendSms}
                onChange={(e) => handleChange('sendSms', e.target.checked)}
                className="h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <label className="ml-2 text-sm text-gray-700">
                Send SMS alerts when this rule triggers
              </label>
            </div>

            {formData.sendSms && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  SMS Recipients
                </label>
                <div className="flex space-x-2 mb-2">
                  <input
                    type="tel"
                    value={smsRecipientInput}
                    onChange={(e) => setSmsRecipientInput(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddSmsRecipient())}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="+15551234567"
                  />
                  <button
                    type="button"
                    onClick={handleAddSmsRecipient}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                  >
                    Add
                  </button>
                </div>
                <div className="space-y-1">
                  {formData.smsRecipients.map((recipient: string, index: number) => (
                    <div key={index} className="flex items-center justify-between bg-gray-50 px-3 py-2 rounded">
                      <span className="text-sm">{recipient}</span>
                      <button
                        type="button"
                        onClick={() => handleRemoveSmsRecipient(index)}
                        className="text-red-600 hover:text-red-700 text-sm"
                      >
                        Remove
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Enable/Disable */}
          <div className="flex items-center">
            <input
              type="checkbox"
              checked={formData.enabled}
              onChange={(e) => handleChange('enabled', e.target.checked)}
              className="h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
            />
            <label className="ml-2 text-sm text-gray-700">
              Enable this rule immediately
            </label>
          </div>

          {/* Actions */}
          <div className="flex space-x-3 pt-4 border-t">
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
              {loading ? 'Saving...' : (rule ? 'Update Rule' : 'Create Rule')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
