import { useEffect, useState } from 'react';
import { Plus, Edit, Trash2, ToggleLeft, ToggleRight } from 'lucide-react';
import { Rule, Device } from '../types';
import { apiService } from '../services/api';
import { RuleModal } from '../components/RuleModal';
import { clsx } from 'clsx';

export const Rules = () => {
  const [rules, setRules] = useState<Rule[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedRule, setSelectedRule] = useState<Rule | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [rulesData, devicesData] = await Promise.all([
        apiService.getRules().catch(() => []), // Handle if endpoint doesn't exist yet
        apiService.getDevices(),
      ]);
      setRules(rulesData);
      setDevices(devicesData);
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setSelectedRule(null);
    setIsModalOpen(true);
  };

  const handleEdit = (rule: Rule) => {
    setSelectedRule(rule);
    setIsModalOpen(true);
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this rule?')) {
      try {
        await apiService.deleteRule(id);
        await fetchData();
      } catch (error) {
        console.error('Failed to delete rule:', error);
      }
    }
  };

  const handleToggleEnabled = async (rule: Rule) => {
    try {
      await apiService.updateRule(rule.id, { enabled: !rule.enabled });
      await fetchData();
    } catch (error) {
      console.error('Failed to toggle rule:', error);
    }
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedRule(null);
    fetchData();
  };

  const getDeviceName = (deviceId: string) => {
    const device = devices.find(d => d.externalId === deviceId);
    return device ? device.name : deviceId;
  };

  const operatorLabels = {
    GT: '>',
    GTE: '≥',
    LT: '<',
    LTE: '≤',
    EQ: '=',
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading rules...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Rules & Automation</h1>
          <p className="text-gray-600 mt-1">Create conditional rules to monitor your devices and trigger alerts</p>
        </div>
        <button
          onClick={handleCreate}
          className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="h-4 w-4" />
          <span>Create Rule</span>
        </button>
      </div>

      {/* Rules Table */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Rule
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Device
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Condition
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Created
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {rules.map((rule) => (
              <tr key={rule.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div>
                    <div className="text-sm font-medium text-gray-900">{rule.name}</div>
                    <div className="text-sm text-gray-500">{rule.description}</div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {getDeviceName(rule.deviceId)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  <code className="bg-gray-100 px-2 py-1 rounded text-xs">
                    {rule.variable} {operatorLabels[rule.operator]} {rule.threshold}
                  </code>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <button
                    onClick={() => handleToggleEnabled(rule)}
                    className={clsx(
                      'flex items-center',
                      rule.enabled ? 'text-green-600' : 'text-gray-400'
                    )}
                  >
                    {rule.enabled ? (
                      <>
                        <ToggleRight className="h-5 w-5 mr-1" />
                        Enabled
                      </>
                    ) : (
                      <>
                        <ToggleLeft className="h-5 w-5 mr-1" />
                        Disabled
                      </>
                    )}
                  </button>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {new Date(rule.createdAt).toLocaleDateString()}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <div className="flex items-center justify-end space-x-2">
                    <button
                      onClick={() => handleEdit(rule)}
                      className="text-blue-600 hover:text-blue-900 p-1"
                    >
                      <Edit className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(rule.id)}
                      className="text-red-600 hover:text-red-900 p-1"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {rules.length === 0 && (
          <div className="text-center py-8 text-gray-500">
            No rules created yet. Create your first rule to get started with automation.
          </div>
        )}
      </div>

      {/* Rule Modal */}
      {isModalOpen && (
        <RuleModal
          rule={selectedRule}
          devices={devices}
          onClose={handleModalClose}
        />
      )}
    </div>
  );
};