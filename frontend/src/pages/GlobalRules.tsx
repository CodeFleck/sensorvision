import { useEffect, useState } from 'react';
import { Plus, Edit, Trash2, ToggleLeft, ToggleRight, Play, Activity } from 'lucide-react';
import { apiService } from '../services/api';
import { FleetRuleBuilderModal } from '../components/FleetRuleBuilderModal';
import { clsx } from 'clsx';
import toast from 'react-hot-toast';

interface GlobalRule {
  id: string;
  name: string;
  description: string;
  organizationId: number;
  selectorType: 'TAG' | 'GROUP' | 'ORGANIZATION' | 'CUSTOM_FILTER';
  selectorValue: string | null;
  aggregationFunction: string;
  aggregationVariable: string | null;
  aggregationParams: any;
  operator: 'GT' | 'GTE' | 'LT' | 'LTE' | 'EQ';
  threshold: number;
  enabled: boolean;
  evaluationInterval: string;
  cooldownMinutes: number;
  lastEvaluatedAt: string | null;
  lastTriggeredAt: string | null;
  sendSms: boolean;
  smsRecipients: string[] | null;
  createdAt: string;
  updatedAt: string;
}

export const GlobalRules = () => {
  const [rules, setRules] = useState<GlobalRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedRule, setSelectedRule] = useState<GlobalRule | null>(null);

  useEffect(() => {
    fetchRules();
  }, []);

  const fetchRules = async () => {
    try {
      const data = await apiService.getGlobalRules();
      setRules(data);
    } catch (error) {
      console.error('Failed to fetch global rules:', error);
      toast.error('Failed to load global rules');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setSelectedRule(null);
    setIsModalOpen(true);
  };

  const handleEdit = (rule: GlobalRule) => {
    setSelectedRule(rule);
    setIsModalOpen(true);
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this global rule?')) {
      try {
        await apiService.deleteGlobalRule(id);
        toast.success('Global rule deleted successfully');
        await fetchRules();
      } catch (error) {
        console.error('Failed to delete global rule:', error);
        toast.error('Failed to delete global rule');
      }
    }
  };

  const handleToggleEnabled = async (id: string) => {
    try {
      const rule = rules.find(r => r.id === id);
      if (!rule) return;

      const newState = !rule.enabled;
      await apiService.toggleGlobalRule(id);
      toast.success(`${rule.name} turned ${newState ? 'ON' : 'OFF'}`);
      await fetchRules();
    } catch (error) {
      console.error('Failed to toggle global rule:', error);
      toast.error('Failed to toggle global rule');
    }
  };

  const handleEvaluateNow = async (id: string) => {
    try {
      await apiService.evaluateGlobalRule(id);
      toast.success('Global rule evaluated successfully. Check Global Alerts for any new alerts.');
      await fetchRules();
    } catch (error) {
      console.error('Failed to evaluate global rule:', error);
      toast.error('Failed to evaluate global rule');
    }
  };

  const handleSave = async (ruleData: any) => {
    try {
      if (selectedRule) {
        // Update existing rule
        await apiService.updateGlobalRule(selectedRule.id, ruleData);
        toast.success('Global rule updated successfully');
      } else {
        // Create new rule
        await apiService.createGlobalRule(ruleData);
        toast.success('Global rule created successfully');
      }
      setIsModalOpen(false);
      setSelectedRule(null);
      await fetchRules();
    } catch (error) {
      console.error('Failed to save global rule:', error);
      toast.error(error instanceof Error ? error.message : 'Failed to save global rule');
      throw error; // Re-throw to let modal handle it
    }
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedRule(null);
  };

  const operatorLabels: Record<string, string> = {
    GT: '>',
    GTE: '≥',
    LT: '<',
    LTE: '≤',
    EQ: '=',
  };

  const selectorTypeLabels: Record<string, string> = {
    TAG: 'Tag',
    GROUP: 'Group',
    ORGANIZATION: 'Organization',
    CUSTOM_FILTER: 'Custom Filter',
  };

  const formatTimestamp = (timestamp: string | null) => {
    if (!timestamp) return 'Never';
    return new Date(timestamp).toLocaleString();
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading global rules...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Fleet-Wide Rules</h1>
          <p className="text-gray-600 mt-1">
            Monitor hundreds of devices simultaneously with aggregated conditions
          </p>
        </div>
        <button
          onClick={handleCreate}
          className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="w-5 h-5" />
          <span>Create Fleet Rule</span>
        </button>
      </div>

      {rules.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-12 text-center">
          <Activity className="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No Fleet Rules Yet</h3>
          <p className="text-gray-600 mb-6">
            Create your first fleet-wide rule to monitor device aggregations and health metrics
          </p>
          <button
            onClick={handleCreate}
            className="inline-flex items-center space-x-2 bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="w-5 h-5" />
            <span>Create Fleet Rule</span>
          </button>
        </div>
      ) : (
        <div className="grid gap-4">
          {rules.map((rule) => (
            <div
              key={rule.id}
              className={clsx(
                'bg-white rounded-lg shadow p-6 border-l-4 transition-all',
                rule.enabled ? 'border-green-500' : 'border-gray-300'
              )}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3">
                    <h3 className="text-lg font-semibold text-gray-900">{rule.name}</h3>
                    <span
                      className={clsx(
                        'px-2 py-1 text-xs font-medium rounded-full',
                        rule.enabled
                          ? 'bg-green-100 text-green-800'
                          : 'bg-gray-100 text-gray-800'
                      )}
                    >
                      {rule.enabled ? 'Enabled' : 'Disabled'}
                    </span>
                  </div>
                  {rule.description && (
                    <p className="text-gray-600 mt-1 text-sm">{rule.description}</p>
                  )}

                  <div className="mt-4 grid grid-cols-2 gap-4">
                    <div>
                      <span className="text-sm font-medium text-gray-700">Device Selector:</span>
                      <p className="text-sm text-gray-900">
                        {selectorTypeLabels[rule.selectorType]}
                        {rule.selectorValue && `: ${rule.selectorValue}`}
                      </p>
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-700">Condition:</span>
                      <p className="text-sm text-gray-900">
                        {rule.aggregationFunction}
                        {rule.aggregationVariable && `(${rule.aggregationVariable})`}{' '}
                        {operatorLabels[rule.operator]} {rule.threshold}
                      </p>
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-700">Evaluation Interval:</span>
                      <p className="text-sm text-gray-900">{rule.evaluationInterval}</p>
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-700">Last Evaluated:</span>
                      <p className="text-sm text-gray-900">
                        {formatTimestamp(rule.lastEvaluatedAt)}
                      </p>
                    </div>
                  </div>

                  {rule.lastTriggeredAt && (
                    <div className="mt-2">
                      <span className="text-sm font-medium text-gray-700">Last Triggered:</span>
                      <p className="text-sm text-red-600">
                        {formatTimestamp(rule.lastTriggeredAt)}
                      </p>
                    </div>
                  )}
                </div>

                <div className="flex items-center space-x-2 ml-4">
                  <button
                    onClick={() => handleEvaluateNow(rule.id)}
                    className="p-2 text-purple-600 hover:bg-purple-50 rounded-lg transition-colors"
                    title="Evaluate Now"
                  >
                    <Play className="w-5 h-5" />
                  </button>
                  <button
                    onClick={() => handleToggleEnabled(rule.id)}
                    className={clsx(
                      'p-2 rounded-lg transition-colors',
                      rule.enabled
                        ? 'text-green-600 hover:bg-green-50'
                        : 'text-gray-400 hover:bg-gray-50'
                    )}
                    title={rule.enabled ? 'Disable' : 'Enable'}
                  >
                    {rule.enabled ? (
                      <ToggleRight className="w-5 h-5" />
                    ) : (
                      <ToggleLeft className="w-5 h-5" />
                    )}
                  </button>
                  <button
                    onClick={() => handleEdit(rule)}
                    className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                    title="Edit"
                  >
                    <Edit className="w-5 h-5" />
                  </button>
                  <button
                    onClick={() => handleDelete(rule.id)}
                    className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                    title="Delete"
                  >
                    <Trash2 className="w-5 h-5" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {isModalOpen && (
        <FleetRuleBuilderModal
          rule={selectedRule}
          onClose={handleModalClose}
          onSave={handleSave}
        />
      )}
    </div>
  );
};
