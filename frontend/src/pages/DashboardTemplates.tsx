import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { LayoutV1 } from '../components/LayoutV1';
import { apiService } from '../services/api';
import { toast } from 'react-hot-toast';

interface DashboardTemplate {
  id: number;
  name: string;
  description: string;
  category: string;
  categoryDisplayName: string;
  icon: string;
  previewImageUrl?: string;
  isSystem: boolean;
  usageCount: number;
}

export const DashboardTemplates: React.FC = () => {
  const [templates, setTemplates] = useState<DashboardTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [showInstantiateModal, setShowInstantiateModal] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<DashboardTemplate | null>(null);
  const [dashboardName, setDashboardName] = useState('');
  const [deviceId, setDeviceId] = useState('');
  const navigate = useNavigate();

  const categories = [
    { value: 'ALL', label: 'All Templates' },
    { value: 'SMART_METER', label: 'Smart Meter' },
    { value: 'ENVIRONMENTAL', label: 'Environmental' },
    { value: 'INDUSTRIAL', label: 'Industrial' },
    { value: 'FLEET', label: 'Fleet Management' },
    { value: 'ENERGY', label: 'Energy Management' },
  ];

  useEffect(() => {
    loadTemplates();
  }, [selectedCategory]);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      const url = selectedCategory === 'ALL'
        ? '/dashboard-templates'
        : `/dashboard-templates/category/${selectedCategory}`;

      const data = await apiService.get<DashboardTemplate[]>(url);
      setTemplates(data);
    } catch (error) {
      console.error('Error loading templates:', error);
      toast.error('Failed to load dashboard templates');
    } finally {
      setLoading(false);
    }
  };

  const handleInstantiateClick = (template: DashboardTemplate) => {
    setSelectedTemplate(template);
    setDashboardName(template.name);
    setDeviceId('');
    setShowInstantiateModal(true);
  };

  const handleInstantiate = async () => {
    if (!selectedTemplate || !dashboardName) {
      toast.error('Please provide a dashboard name');
      return;
    }

    try {
      const response = await apiService.post<any>(
        `/dashboard-templates/${selectedTemplate.id}/instantiate`,
        {
          dashboardName,
          dashboardDescription: `Created from ${selectedTemplate.name} template`,
          deviceId: deviceId || undefined,
        }
      );

      toast.success('Dashboard created successfully!');
      setShowInstantiateModal(false);
      navigate(`/dashboards/${response.id}`);
    } catch (error) {
      console.error('Error instantiating template:', error);
      toast.error('Failed to create dashboard');
    }
  };

  return (
    <LayoutV1>
      <div className="space-y-6">
        {/* Header */}
        <div className="bg-white shadow rounded-lg p-6">
          <h1 className="text-2xl font-bold text-gray-900">Dashboard Templates</h1>
          <p className="mt-2 text-sm text-gray-600">
            Get started quickly with pre-built dashboard templates for common use cases
          </p>
        </div>

        {/* Category Filter */}
        <div className="bg-white shadow rounded-lg p-6">
          <div className="flex flex-wrap gap-2">
            {categories.map((cat) => (
              <button
                key={cat.value}
                onClick={() => setSelectedCategory(cat.value)}
                className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                  selectedCategory === cat.value
                    ? 'bg-indigo-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>
        </div>

        {/* Templates Grid */}
        {loading ? (
          <div className="flex justify-center items-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
          </div>
        ) : templates.length === 0 ? (
          <div className="bg-white shadow rounded-lg p-12 text-center">
            <p className="text-gray-500">No templates found in this category</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {templates.map((template) => (
              <div
                key={template.id}
                className="bg-white shadow rounded-lg overflow-hidden hover:shadow-lg transition-shadow"
              >
                {/* Template Preview */}
                <div className="h-48 bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
                  {template.previewImageUrl ? (
                    <img
                      src={template.previewImageUrl}
                      alt={template.name}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <span className="text-6xl">{template.icon || '📊'}</span>
                  )}
                </div>

                {/* Template Info */}
                <div className="p-6">
                  <div className="flex items-start justify-between mb-2">
                    <h3 className="text-lg font-semibold text-gray-900 flex-1">
                      {template.name}
                    </h3>
                    {template.isSystem && (
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        Official
                      </span>
                    )}
                  </div>

                  <p className="text-sm text-gray-600 mb-4 line-clamp-2">
                    {template.description}
                  </p>

                  <div className="flex items-center justify-between text-xs text-gray-500 mb-4">
                    <span>{template.categoryDisplayName}</span>
                    <span>Used {template.usageCount}× times</span>
                  </div>

                  <button
                    onClick={() => handleInstantiateClick(template)}
                    className="w-full bg-indigo-600 text-white py-2 px-4 rounded-lg hover:bg-indigo-700 transition-colors font-medium"
                  >
                    Use This Template
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Instantiate Modal */}
        {showInstantiateModal && selectedTemplate && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
              <div className="p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">
                  Create Dashboard from Template
                </h3>

                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Template
                    </label>
                    <div className="text-sm text-gray-600 bg-gray-50 p-3 rounded-lg">
                      {selectedTemplate.icon} {selectedTemplate.name}
                    </div>
                  </div>

                  <div>
                    <label htmlFor="dashboardName" className="block text-sm font-medium text-gray-700 mb-1">
                      Dashboard Name *
                    </label>
                    <input
                      type="text"
                      id="dashboardName"
                      value={dashboardName}
                      onChange={(e) => setDashboardName(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                      placeholder="My Dashboard"
                      required
                    />
                  </div>

                  <div>
                    <label htmlFor="deviceId" className="block text-sm font-medium text-gray-700 mb-1">
                      Device ID (Optional)
                    </label>
                    <input
                      type="text"
                      id="deviceId"
                      value={deviceId}
                      onChange={(e) => setDeviceId(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                      placeholder="device-001"
                    />
                    <p className="mt-1 text-xs text-gray-500">
                      Bind all widgets to this device. Leave blank to configure later.
                    </p>
                  </div>
                </div>

                <div className="mt-6 flex gap-3">
                  <button
                    onClick={() => setShowInstantiateModal(false)}
                    className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleInstantiate}
                    className="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                  >
                    Create Dashboard
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </LayoutV1>
  );
};
