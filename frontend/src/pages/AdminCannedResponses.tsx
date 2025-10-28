import React, { useState, useEffect } from 'react';
import { MessageSquare, Plus, Edit2, Trash2, TrendingUp, X } from 'lucide-react';
import { apiService } from '../services/api';
import toast from 'react-hot-toast';

interface CannedResponse {
  id: number;
  title: string;
  body: string;
  category: string | null;
  active: boolean;
  useCount: number;
  createdAt: string;
  updatedAt: string;
}

interface CannedResponseFormData {
  title: string;
  body: string;
  category: string;
  active: boolean;
}

export const AdminCannedResponses: React.FC = () => {
  const [templates, setTemplates] = useState<CannedResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<CannedResponse | null>(null);
  const [formData, setFormData] = useState<CannedResponseFormData>({
    title: '',
    body: '',
    category: 'GENERAL',
    active: true,
  });

  const categories = ['GENERAL', 'AUTHENTICATION', 'RESOLUTION', 'BUG', 'FEATURE_REQUEST'];

  useEffect(() => {
    loadTemplates();
  }, [categoryFilter]);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      const params = categoryFilter !== 'ALL' ? { category: categoryFilter } : {};
      const data = await apiService.getCannedResponses(params);
      setTemplates(data);
    } catch (error) {
      toast.error('Failed to load templates');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const openCreateModal = () => {
    setEditingTemplate(null);
    setFormData({
      title: '',
      body: '',
      category: 'GENERAL',
      active: true,
    });
    setIsModalOpen(true);
  };

  const openEditModal = (template: CannedResponse) => {
    setEditingTemplate(template);
    setFormData({
      title: template.title,
      body: template.body,
      category: template.category || 'GENERAL',
      active: template.active,
    });
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingTemplate(null);
    setFormData({
      title: '',
      body: '',
      category: 'GENERAL',
      active: true,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.title.trim() || !formData.body.trim()) {
      toast.error('Title and body are required');
      return;
    }

    try {
      if (editingTemplate) {
        await apiService.updateCannedResponse(editingTemplate.id, formData);
        toast.success('Template updated successfully');
      } else {
        await apiService.createCannedResponse(formData);
        toast.success('Template created successfully');
      }
      closeModal();
      loadTemplates();
    } catch (error) {
      toast.error(editingTemplate ? 'Failed to update template' : 'Failed to create template');
      console.error(error);
    }
  };

  const handleDelete = async (id: number, title: string) => {
    if (!confirm(`Are you sure you want to delete "${title}"?`)) {
      return;
    }

    try {
      await apiService.deleteCannedResponse(id);
      toast.success('Template deleted successfully');
      loadTemplates();
    } catch (error) {
      toast.error('Failed to delete template');
      console.error(error);
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-6">
      {/* Header */}
      <div className="mb-6 flex items-start justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-800 mb-2 flex items-center gap-2">
            <MessageSquare className="h-8 w-8" />
            Canned Responses
          </h1>
          <p className="text-gray-600">Manage template responses for support tickets</p>
        </div>
        <button
          onClick={openCreateModal}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors font-medium shadow-sm"
        >
          <Plus className="h-5 w-5" />
          New Template
        </button>
      </div>

      {/* Category Filter */}
      <div className="mb-4">
        <select
          value={categoryFilter}
          onChange={(e) => setCategoryFilter(e.target.value)}
          className="px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="ALL">All Categories</option>
          {categories.map((cat) => (
            <option key={cat} value={cat}>
              {cat.replace('_', ' ')}
            </option>
          ))}
        </select>
      </div>

      {/* Templates List */}
      {loading ? (
        <div className="text-center py-12 text-gray-500">Loading templates...</div>
      ) : templates.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-12 text-center">
          <MessageSquare className="h-16 w-16 mx-auto text-gray-400 mb-4" />
          <p className="text-gray-500 text-lg">No templates found</p>
          <p className="text-gray-400 text-sm mt-2">Create your first template to get started</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {templates.map((template) => (
            <div
              key={template.id}
              className={`bg-white rounded-lg shadow-sm border p-4 hover:shadow-md transition-shadow ${
                !template.active ? 'opacity-60' : ''
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="font-semibold text-lg text-gray-900">{template.title}</h3>
                    {!template.active && (
                      <span className="text-xs bg-gray-200 text-gray-600 px-2 py-1 rounded">
                        Inactive
                      </span>
                    )}
                    {template.category && (
                      <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
                        {template.category.replace('_', ' ')}
                      </span>
                    )}
                    {template.useCount > 0 && (
                      <div className="flex items-center gap-1 text-sm text-gray-500">
                        <TrendingUp className="h-4 w-4" />
                        Used {template.useCount} times
                      </div>
                    )}
                  </div>
                  <p className="text-gray-700 whitespace-pre-wrap">{template.body}</p>
                  <div className="mt-2 text-xs text-gray-500">
                    Created: {new Date(template.createdAt).toLocaleDateString()} | Last updated:{' '}
                    {new Date(template.updatedAt).toLocaleDateString()}
                  </div>
                </div>
                <div className="flex items-center gap-2 ml-4">
                  <button
                    onClick={() => openEditModal(template)}
                    className="p-2 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded-md transition-colors"
                    title="Edit template"
                  >
                    <Edit2 className="h-5 w-5" />
                  </button>
                  <button
                    onClick={() => handleDelete(template.id, template.title)}
                    className="p-2 text-gray-600 hover:text-red-600 hover:bg-red-50 rounded-md transition-colors"
                    title="Delete template"
                  >
                    <Trash2 className="h-5 w-5" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create/Edit Modal */}
      {isModalOpen && (
        <>
          {/* Backdrop */}
          <div className="fixed inset-0 bg-black bg-opacity-50 z-40" onClick={closeModal} />

          {/* Modal */}
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
              {/* Header */}
              <div className="flex items-center justify-between p-6 border-b border-gray-200">
                <h2 className="text-2xl font-bold text-gray-900">
                  {editingTemplate ? 'Edit Template' : 'Create New Template'}
                </h2>
                <button
                  onClick={closeModal}
                  className="text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <X className="h-6 w-6" />
                </button>
              </div>

              {/* Form */}
              <form onSubmit={handleSubmit} className="p-6 space-y-4">
                {/* Title */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Title <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="e.g., Welcome Message"
                    required
                  />
                </div>

                {/* Category */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                  <select
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    {categories.map((cat) => (
                      <option key={cat} value={cat}>
                        {cat.replace('_', ' ')}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Body */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Template Body <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    value={formData.body}
                    onChange={(e) => setFormData({ ...formData, body: e.target.value })}
                    rows={8}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
                    placeholder="Type your template message here..."
                    required
                  />
                  <p className="mt-1 text-xs text-gray-500">
                    This text will be inserted when you select this template
                  </p>
                </div>

                {/* Active Status */}
                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="active"
                    checked={formData.active}
                    onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                    className="mr-2 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="active" className="text-sm text-gray-700">
                    Active (visible in template picker)
                  </label>
                </div>

                {/* Actions */}
                <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-200">
                  <button
                    type="button"
                    onClick={closeModal}
                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
                  >
                    {editingTemplate ? 'Update Template' : 'Create Template'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
