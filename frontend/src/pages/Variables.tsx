import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

interface Variable {
  id: number;
  name: string;
  displayName: string;
  description: string;
  unit: string;
  dataType: 'NUMBER' | 'BOOLEAN' | 'STRING' | 'JSON';
  icon: string;
  color: string;
  minValue: number | null;
  maxValue: number | null;
  decimalPlaces: number;
  isSystemVariable: boolean;
  createdAt: string;
}

const Variables: React.FC = () => {
  const [variables, setVariables] = useState<Variable[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [editingVariable, setEditingVariable] = useState<Variable | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    displayName: '',
    description: '',
    unit: '',
    dataType: 'NUMBER' as Variable['dataType'],
    icon: '',
    color: '#3B82F6',
    minValue: '',
    maxValue: '',
    decimalPlaces: 2,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadVariables();
  }, []);

  const loadVariables = async () => {
    try {
      setLoading(true);
      const response = await apiService.get('/variables');
      setVariables(response.data);
      setError(null);
    } catch (err: any) {
      setError('Failed to load variables');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const payload = {
        ...formData,
        minValue: formData.minValue ? parseFloat(formData.minValue) : null,
        maxValue: formData.maxValue ? parseFloat(formData.maxValue) : null,
      };

      if (editingVariable) {
        await apiService.put(`/variables/${editingVariable.id}`, payload);
      } else {
        await apiService.post('/variables', payload);
      }
      await loadVariables();
      handleCloseModal();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save variable');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this variable?')) {
      return;
    }

    try {
      await apiService.delete(`/variables/${id}`);
      await loadVariables();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete variable');
    }
  };

  const handleEdit = (variable: Variable) => {
    setEditingVariable(variable);
    setFormData({
      name: variable.name,
      displayName: variable.displayName || '',
      description: variable.description || '',
      unit: variable.unit || '',
      dataType: variable.dataType,
      icon: variable.icon || '',
      color: variable.color || '#3B82F6',
      minValue: variable.minValue?.toString() || '',
      maxValue: variable.maxValue?.toString() || '',
      decimalPlaces: variable.decimalPlaces || 2,
    });
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingVariable(null);
    setFormData({
      name: '',
      displayName: '',
      description: '',
      unit: '',
      dataType: 'NUMBER',
      icon: '',
      color: '#3B82F6',
      minValue: '',
      maxValue: '',
      decimalPlaces: 2,
    });
    setError(null);
  };

  const handleCreateNew = () => {
    setEditingVariable(null);
    setFormData({
      name: '',
      displayName: '',
      description: '',
      unit: '',
      dataType: 'NUMBER',
      icon: '',
      color: '#3B82F6',
      minValue: '',
      maxValue: '',
      decimalPlaces: 2,
    });
    setShowModal(true);
  };

  const getDataTypeBadge = (dataType: string) => {
    const colors: Record<string, string> = {
      NUMBER: 'bg-blue-100 text-blue-800',
      BOOLEAN: 'bg-green-100 text-green-800',
      STRING: 'bg-yellow-100 text-yellow-800',
      JSON: 'bg-purple-100 text-purple-800',
    };
    return colors[dataType] || 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold">Variable Management</h1>
        <button
          onClick={handleCreateNew}
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
        >
          + Create Variable
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4">
          {error}
        </div>
      )}

      {loading && !showModal ? (
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Variable
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Display Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Type
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Unit
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Range
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {variables.map((variable) => (
                  <tr key={variable.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        {variable.color && (
                          <div
                            className="w-3 h-3 rounded-full mr-2"
                            style={{ backgroundColor: variable.color }}
                          />
                        )}
                        <div>
                          <div className="text-sm font-medium text-gray-900">
                            {variable.name}
                          </div>
                          {variable.isSystemVariable && (
                            <span className="text-xs text-gray-500">System</span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-sm text-gray-900">{variable.displayName}</div>
                      {variable.description && (
                        <div className="text-xs text-gray-500">{variable.description}</div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded ${getDataTypeBadge(
                          variable.dataType
                        )}`}
                      >
                        {variable.dataType}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {variable.unit || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {variable.minValue !== null || variable.maxValue !== null ? (
                        <span>
                          {variable.minValue ?? '∞'} to {variable.maxValue ?? '∞'}
                        </span>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button
                        onClick={() => handleEdit(variable)}
                        className="text-blue-600 hover:text-blue-800 mr-4"
                      >
                        Edit
                      </button>
                      {!variable.isSystemVariable && (
                        <button
                          onClick={() => handleDelete(variable.id)}
                          className="text-red-600 hover:text-red-800"
                        >
                          Delete
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 overflow-y-auto">
          <div className="bg-white rounded-lg p-8 max-w-2xl w-full mx-4 my-8">
            <h2 className="text-2xl font-bold mb-4">
              {editingVariable ? 'Edit Variable' : 'Create Variable'}
            </h2>
            <form onSubmit={handleSubmit}>
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Variable Name (Internal ID)
                  </label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="kwConsumption, temperature, pressure"
                    required
                    disabled={editingVariable?.isSystemVariable}
                  />
                </div>

                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Display Name
                  </label>
                  <input
                    type="text"
                    value={formData.displayName}
                    onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Power Consumption, Temperature, Pressure"
                  />
                </div>

                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Description
                  </label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    rows={2}
                    placeholder="Detailed description of what this variable measures"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Data Type
                  </label>
                  <select
                    value={formData.dataType}
                    onChange={(e) =>
                      setFormData({ ...formData, dataType: e.target.value as Variable['dataType'] })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="NUMBER">NUMBER</option>
                    <option value="BOOLEAN">BOOLEAN</option>
                    <option value="STRING">STRING</option>
                    <option value="JSON">JSON</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Unit
                  </label>
                  <input
                    type="text"
                    value={formData.unit}
                    onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="kW, °C, V, A, Hz"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Min Value
                  </label>
                  <input
                    type="number"
                    step="any"
                    value={formData.minValue}
                    onChange={(e) => setFormData({ ...formData, minValue: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Optional"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Max Value
                  </label>
                  <input
                    type="number"
                    step="any"
                    value={formData.maxValue}
                    onChange={(e) => setFormData({ ...formData, maxValue: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Optional"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Decimal Places
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="10"
                    value={formData.decimalPlaces}
                    onChange={(e) =>
                      setFormData({ ...formData, decimalPlaces: parseInt(e.target.value) })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Color
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="color"
                      value={formData.color}
                      onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                      className="h-10 w-16 rounded border border-gray-300 cursor-pointer"
                    />
                    <input
                      type="text"
                      value={formData.color}
                      onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      pattern="^#[0-9A-Fa-f]{6}$"
                    />
                  </div>
                </div>

                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Icon (optional)
                  </label>
                  <input
                    type="text"
                    value={formData.icon}
                    onChange={(e) => setFormData({ ...formData, icon: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Icon name or URL"
                  />
                </div>
              </div>

              <div className="flex gap-3 mt-6">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="flex-1 bg-gray-200 text-gray-700 py-2 rounded-md hover:bg-gray-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 disabled:bg-gray-400"
                >
                  {loading ? 'Saving...' : editingVariable ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Variables;
