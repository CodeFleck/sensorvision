import { useState, useEffect } from 'react';
import { Settings, DollarSign, MessageSquare, TrendingUp, AlertTriangle, RefreshCw, Save } from 'lucide-react';
import { SmsSettings as SmsSettingsType, SmsSettingsUpdateRequest } from '../types';
import { apiService } from '../services/api';
import toast from 'react-hot-toast';

export const SmsSettings = () => {
  const [settings, setSettings] = useState<SmsSettingsType | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [formData, setFormData] = useState<SmsSettingsUpdateRequest>({
    enabled: false,
    dailyLimit: 100,
    monthlyBudget: 50.00,
    alertOnBudgetThreshold: true,
    budgetThresholdPercentage: 80,
  });

  useEffect(() => {
    fetchSettings();
  }, []);

  const fetchSettings = async () => {
    try {
      setLoading(true);
      const data = await apiService.getSmsSettings();
      setSettings(data);
      setFormData({
        enabled: data.enabled,
        dailyLimit: data.dailyLimit,
        monthlyBudget: data.monthlyBudget,
        alertOnBudgetThreshold: data.alertOnBudgetThreshold,
        budgetThresholdPercentage: data.budgetThresholdPercentage,
      });
    } catch (error) {
      console.error('Failed to fetch SMS settings:', error);
      toast.error('Failed to load SMS settings');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      const updated = await apiService.updateSmsSettings(formData);
      setSettings(updated);
      toast.success('SMS settings updated successfully');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to update settings');
    } finally {
      setSaving(false);
    }
  };

  const handleResetCounters = async () => {
    if (!window.confirm('Are you sure you want to reset monthly SMS counters? This will reset the count and cost to zero.')) {
      return;
    }

    try {
      setResetting(true);
      await apiService.resetMonthlySmsCounters();
      toast.success('Monthly counters reset successfully');
      await fetchSettings();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to reset counters');
    } finally {
      setResetting(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked :
              type === 'number' ? parseFloat(value) : value,
    }));
  };

  const budgetUtilization = settings
    ? (settings.currentMonthCost / settings.monthlyBudget) * 100
    : 0;

  const budgetRemaining = settings
    ? Math.max(0, settings.monthlyBudget - settings.currentMonthCost)
    : 0;

  const getUtilizationColor = (percentage: number) => {
    if (percentage >= 90) return 'bg-red-600';
    if (settings?.budgetThresholdPercentage !== undefined && percentage >= settings.budgetThresholdPercentage) return 'bg-yellow-600';
    return 'bg-green-600';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading SMS settings...</div>
      </div>
    );
  }

  if (!settings) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-red-500">Failed to load SMS settings</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">SMS Settings</h1>
          <p className="text-gray-600 mt-1">Configure SMS alerts budget, limits, and monitoring</p>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={handleResetCounters}
            disabled={resetting}
            className="flex items-center space-x-2 bg-gray-600 text-white px-4 py-2 rounded-lg hover:bg-gray-700 disabled:opacity-50 transition-colors"
          >
            <RefreshCw className={`h-5 w-5 ${resetting ? 'animate-spin' : ''}`} />
            <span>{resetting ? 'Resetting...' : 'Reset Monthly Counters'}</span>
          </button>
        </div>
      </div>

      {/* Usage Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">SMS Sent Today</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{settings.currentDayCount}</p>
              <p className="text-xs text-gray-500 mt-1">of {settings.dailyLimit} limit</p>
            </div>
            <MessageSquare className="h-8 w-8 text-blue-600" />
          </div>
          <div className="mt-4">
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-blue-600 h-2 rounded-full"
                style={{ width: `${Math.min(100, (settings.currentDayCount / settings.dailyLimit) * 100)}%` }}
              />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">SMS This Month</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">{settings.currentMonthCount}</p>
              <p className="text-xs text-gray-500 mt-1">total sent</p>
            </div>
            <TrendingUp className="h-8 w-8 text-green-600" />
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Monthly Cost</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                ${settings.currentMonthCost.toFixed(2)}
              </p>
              <p className="text-xs text-gray-500 mt-1">of ${settings.monthlyBudget.toFixed(2)} budget</p>
            </div>
            <DollarSign className="h-8 w-8 text-green-600" />
          </div>
          <div className="mt-4">
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className={`h-2 rounded-full ${getUtilizationColor(budgetUtilization)}`}
                style={{ width: `${Math.min(100, budgetUtilization)}%` }}
              />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Budget Remaining</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                ${budgetRemaining.toFixed(2)}
              </p>
              <p className="text-xs text-gray-500 mt-1">
                {(100 - budgetUtilization).toFixed(1)}% remaining
              </p>
            </div>
            {budgetUtilization >= 90 ? (
              <AlertTriangle className="h-8 w-8 text-red-600" />
            ) : (
              <DollarSign className="h-8 w-8 text-gray-400" />
            )}
          </div>
        </div>
      </div>

      {/* Budget Alert Warning */}
      {budgetUtilization >= settings.budgetThresholdPercentage && (
        <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 rounded">
          <div className="flex">
            <AlertTriangle className="h-5 w-5 text-yellow-400" />
            <div className="ml-3">
              <h3 className="text-sm font-medium text-yellow-800">
                Budget Threshold Reached
              </h3>
              <p className="mt-1 text-sm text-yellow-700">
                Your SMS usage has exceeded {settings.budgetThresholdPercentage}% of the monthly budget.
                Consider increasing the budget or monitoring alert frequency.
              </p>
            </div>
          </div>
        </div>
      )}

      {budgetUtilization >= 100 && (
        <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded">
          <div className="flex">
            <AlertTriangle className="h-5 w-5 text-red-400" />
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">
                Monthly Budget Exceeded
              </h3>
              <p className="mt-1 text-sm text-red-700">
                Your monthly SMS budget has been exceeded. New SMS alerts will be blocked until the budget is increased or monthly counters are reset.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Configuration Form */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900 flex items-center">
            <Settings className="h-5 w-5 mr-2" />
            SMS Configuration
          </h2>
        </div>

        <div className="p-6 space-y-6">
          {/* Enable/Disable SMS */}
          <div className="flex items-center justify-between">
            <div>
              <label htmlFor="enabled" className="text-sm font-medium text-gray-900">
                Enable SMS Notifications
              </label>
              <p className="text-sm text-gray-500 mt-1">
                Allow SMS alerts to be sent when rules trigger
              </p>
            </div>
            <div className="flex items-center">
              <input
                type="checkbox"
                id="enabled"
                name="enabled"
                checked={formData.enabled}
                onChange={handleChange}
                className="h-5 w-5 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
            </div>
          </div>

          {/* Daily Limit */}
          <div>
            <label htmlFor="dailyLimit" className="block text-sm font-medium text-gray-900 mb-1">
              Daily SMS Limit
            </label>
            <p className="text-sm text-gray-500 mb-2">
              Maximum number of SMS messages that can be sent per day
            </p>
            <input
              type="number"
              id="dailyLimit"
              name="dailyLimit"
              min="1"
              max="10000"
              value={formData.dailyLimit}
              onChange={handleChange}
              className="w-full max-w-xs px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          {/* Monthly Budget */}
          <div>
            <label htmlFor="monthlyBudget" className="block text-sm font-medium text-gray-900 mb-1">
              Monthly SMS Budget (USD)
            </label>
            <p className="text-sm text-gray-500 mb-2">
              Maximum amount to spend on SMS per month (based on $0.0075 per SMS)
            </p>
            <div className="relative max-w-xs">
              <span className="absolute left-3 top-2 text-gray-500">$</span>
              <input
                type="number"
                id="monthlyBudget"
                name="monthlyBudget"
                min="0.01"
                step="0.01"
                value={formData.monthlyBudget}
                onChange={handleChange}
                className="w-full pl-7 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Approximately {Math.floor(formData.monthlyBudget! / 0.0075)} SMS messages per month
            </p>
          </div>

          {/* Budget Threshold Alert */}
          <div>
            <div className="flex items-center mb-2">
              <input
                type="checkbox"
                id="alertOnBudgetThreshold"
                name="alertOnBudgetThreshold"
                checked={formData.alertOnBudgetThreshold}
                onChange={handleChange}
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <label htmlFor="alertOnBudgetThreshold" className="ml-2 text-sm font-medium text-gray-900">
                Send email alert when budget threshold is reached
              </label>
            </div>

            {formData.alertOnBudgetThreshold && (
              <div className="ml-6">
                <label htmlFor="budgetThresholdPercentage" className="block text-sm font-medium text-gray-900 mb-1">
                  Budget Threshold Percentage
                </label>
                <p className="text-sm text-gray-500 mb-2">
                  Send alert when monthly cost reaches this percentage of budget
                </p>
                <div className="relative max-w-xs">
                  <input
                    type="number"
                    id="budgetThresholdPercentage"
                    name="budgetThresholdPercentage"
                    min="1"
                    max="100"
                    value={formData.budgetThresholdPercentage}
                    onChange={handleChange}
                    className="w-full pr-8 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  />
                  <span className="absolute right-3 top-2 text-gray-500">%</span>
                </div>
              </div>
            )}
          </div>

          {/* Last Reset Date */}
          {settings.lastResetDate && (
            <div className="bg-gray-50 rounded-lg p-4">
              <p className="text-sm text-gray-600">
                <strong>Last Counter Reset:</strong>{' '}
                {new Date(settings.lastResetDate).toLocaleString()}
              </p>
            </div>
          )}

          {/* Save Button */}
          <div className="flex justify-end pt-4 border-t">
            <button
              onClick={handleSave}
              disabled={saving}
              className="flex items-center space-x-2 bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              <Save className="h-5 w-5" />
              <span>{saving ? 'Saving...' : 'Save Settings'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Info Panel */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h3 className="text-sm font-semibold text-blue-900 mb-3">SMS Pricing Information</h3>
        <ul className="text-sm text-blue-800 space-y-2">
          <li>• Standard SMS cost: $0.0075 per message (US domestic)</li>
          <li>• Verification SMS cost: $0.05 per message</li>
          <li>• Daily limits reset at midnight UTC</li>
          <li>• Monthly counters track calendar month usage</li>
          <li>• Budget alerts are sent to the admin email configured in settings</li>
        </ul>
      </div>
    </div>
  );
};

export default SmsSettings;
