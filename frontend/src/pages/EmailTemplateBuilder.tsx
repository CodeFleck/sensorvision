import React, { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, Eye, Save, X, Mail, FileText } from 'lucide-react';
import toast from 'react-hot-toast';
import emailTemplateService, { EmailTemplateRequest, EmailTemplateResponse } from '../services/emailTemplateService';

const TEMPLATE_TYPES = [
  { value: 'ALERT', label: 'Alert Notification' },
  { value: 'NOTIFICATION', label: 'General Notification' },
  { value: 'REPORT', label: 'Report' },
  { value: 'WELCOME', label: 'Welcome Email' },
  { value: 'PASSWORD_RESET', label: 'Password Reset' },
  { value: 'DEVICE_ADDED', label: 'Device Added' },
  { value: 'DEVICE_OFFLINE', label: 'Device Offline' },
];

const SAMPLE_VARIABLES = {
  ALERT: { deviceName: 'Sensor-001', alertMessage: 'Temperature exceeded threshold', threshold: '30', value: '35', timestamp: new Date().toISOString() },
  NOTIFICATION: { userName: 'John Doe', message: 'Your action is required', link: 'https://sensorvision.com/action' },
  DEVICE_ADDED: { deviceName: 'New Sensor', deviceId: 'sensor-123', addedBy: 'Admin' },
  DEVICE_OFFLINE: { deviceName: 'Sensor-001', lastSeen: '2 hours ago' },
};

const EmailTemplateBuilder: React.FC = () => {
  const [templates, setTemplates] = useState<EmailTemplateResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<EmailTemplateResponse | null>(null);
  const [showPreview, setShowPreview] = useState(false);
  const [previewData, setPreviewData] = useState<{ subject: string; body: string } | null>(null);

  // Form state
  const [formData, setFormData] = useState<EmailTemplateRequest>({
    name: '',
    description: '',
    templateType: 'ALERT',
    subject: '',
    body: '',
    variables: [],
    isDefault: false,
    active: true,
  });

  useEffect(() => {
    loadTemplates();
  }, []);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      const response = await emailTemplateService.getTemplates(0, 100);
      setTemplates(response.content);
    } catch (error: any) {
      toast.error(`Failed to load templates: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingTemplate(null);
    setFormData({
      name: '',
      description: '',
      templateType: 'ALERT',
      subject: '',
      body: '',
      variables: [],
      isDefault: false,
      active: true,
    });
    setShowForm(true);
  };

  const handleEdit = (template: EmailTemplateResponse) => {
    setEditingTemplate(template);
    setFormData({
      name: template.name,
      description: template.description || '',
      templateType: template.templateType,
      subject: template.subject,
      body: template.body,
      variables: template.variables,
      isDefault: template.isDefault,
      active: template.active,
    });
    setShowForm(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      if (editingTemplate) {
        await emailTemplateService.updateTemplate(editingTemplate.id, formData);
        toast.success('Template updated successfully');
      } else {
        await emailTemplateService.createTemplate(formData);
        toast.success('Template created successfully');
      }
      setShowForm(false);
      loadTemplates();
    } catch (error: any) {
      toast.error(`Failed to save template: ${error.message}`);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this template?')) return;

    try {
      await emailTemplateService.deleteTemplate(id);
      toast.success('Template deleted successfully');
      loadTemplates();
    } catch (error: any) {
      toast.error(`Failed to delete template: ${error.message}`);
    }
  };

  const handlePreview = async (template: EmailTemplateResponse) => {
    try {
      const sampleData = SAMPLE_VARIABLES[template.templateType as keyof typeof SAMPLE_VARIABLES] || {};
      const preview = await emailTemplateService.previewTemplate(template.id, sampleData);
      setPreviewData(preview);
      setShowPreview(true);
    } catch (error: any) {
      toast.error(`Failed to preview template: ${error.message}`);
    }
  };

  const insertVariable = (variable: string) => {
    const textarea = document.getElementById('body') as HTMLTextAreaElement;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = formData.body;
    const before = text.substring(0, start);
    const after = text.substring(end);
    const newText = before + `{{${variable}}}` + after;

    setFormData({ ...formData, body: newText });

    // Set cursor position after inserted variable
    setTimeout(() => {
      textarea.selectionStart = textarea.selectionEnd = start + variable.length + 4;
      textarea.focus();
    }, 0);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Email Template Builder</h1>
          <p className="text-gray-600 mt-1">
            Create and manage customizable email templates for notifications
          </p>
        </div>
        <button
          onClick={handleCreate}
          className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          <Plus className="h-5 w-5" />
          <span>New Template</span>
        </button>
      </div>

      {/* Templates List */}
      {!showForm ? (
        <div className="bg-white rounded-lg shadow">
          <div className="p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Templates</h2>
            {loading ? (
              <div className="text-center py-8 text-gray-500">Loading...</div>
            ) : templates.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                No templates found. Create your first template to get started.
              </div>
            ) : (
              <div className="space-y-3">
                {templates.map((template) => (
                  <div
                    key={template.id}
                    className="flex items-center justify-between p-4 border border-gray-200 rounded-md hover:bg-gray-50"
                  >
                    <div className="flex-1">
                      <div className="flex items-center space-x-3">
                        <Mail className="h-5 w-5 text-blue-600" />
                        <div>
                          <h3 className="font-medium text-gray-900">{template.name}</h3>
                          <p className="text-sm text-gray-600">{template.description}</p>
                        </div>
                      </div>
                      <div className="flex items-center space-x-4 mt-2 ml-8">
                        <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
                          {TEMPLATE_TYPES.find(t => t.value === template.templateType)?.label}
                        </span>
                        {template.isDefault && (
                          <span className="text-xs bg-green-100 text-green-800 px-2 py-1 rounded">
                            Default
                          </span>
                        )}
                        {!template.active && (
                          <span className="text-xs bg-gray-100 text-gray-800 px-2 py-1 rounded">
                            Inactive
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => handlePreview(template)}
                        className="p-2 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded"
                        title="Preview"
                      >
                        <Eye className="h-5 w-5" />
                      </button>
                      <button
                        onClick={() => handleEdit(template)}
                        className="p-2 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded"
                        title="Edit"
                      >
                        <Edit className="h-5 w-5" />
                      </button>
                      <button
                        onClick={() => handleDelete(template.id)}
                        className="p-2 text-gray-600 hover:text-red-600 hover:bg-red-50 rounded"
                        title="Delete"
                      >
                        <Trash2 className="h-5 w-5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      ) : (
        /* Template Form */
        <div className="bg-white rounded-lg shadow">
          <div className="p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold text-gray-900">
                {editingTemplate ? 'Edit Template' : 'New Template'}
              </h2>
              <button
                onClick={() => setShowForm(false)}
                className="text-gray-600 hover:text-gray-900"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Name and Type */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Template Name *
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                    placeholder="e.g., High Temperature Alert"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Template Type *
                  </label>
                  <select
                    required
                    value={formData.templateType}
                    onChange={(e) => setFormData({ ...formData, templateType: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                  >
                    {TEMPLATE_TYPES.map((type) => (
                      <option key={type.value} value={type.value}>
                        {type.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Description */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Description
                </label>
                <input
                  type="text"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                  placeholder="Brief description of this template"
                />
              </div>

              {/* Subject */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email Subject *
                </label>
                <input
                  type="text"
                  required
                  value={formData.subject}
                  onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                  placeholder="e.g., Alert: {{deviceName}} - {{alertMessage}}"
                />
              </div>

              {/* Variable Helper */}
              <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
                <div className="flex items-start space-x-2">
                  <FileText className="h-5 w-5 text-blue-600 mt-0.5" />
                  <div className="flex-1">
                    <h4 className="text-sm font-medium text-blue-900 mb-2">Available Variables</h4>
                    <p className="text-sm text-blue-700 mb-2">
                      Click to insert variables into your email body:
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {['deviceName', 'alertMessage', 'threshold', 'value', 'timestamp', 'userName', 'link'].map(
                        (variable) => (
                          <button
                            key={variable}
                            type="button"
                            onClick={() => insertVariable(variable)}
                            className="text-xs bg-white border border-blue-300 text-blue-700 px-2 py-1 rounded hover:bg-blue-100"
                          >
                            {'{{'}{variable}{'}}'}
                          </button>
                        )
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Body */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email Body (HTML) *
                </label>
                <textarea
                  id="body"
                  required
                  value={formData.body}
                  onChange={(e) => setFormData({ ...formData, body: e.target.value })}
                  rows={12}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 font-mono text-sm"
                  placeholder="<html><body><h2>Alert: {{alertMessage}}</h2><p>Device: {{deviceName}}</p></body></html>"
                />
              </div>

              {/* Checkboxes */}
              <div className="flex items-center space-x-6">
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={formData.isDefault}
                    onChange={(e) => setFormData({ ...formData, isDefault: e.target.checked })}
                    className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                  <span className="text-sm text-gray-700">Set as default template for this type</span>
                </label>
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={formData.active}
                    onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                    className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                  <span className="text-sm text-gray-700">Active</span>
                </label>
              </div>

              {/* Actions */}
              <div className="flex items-center justify-end space-x-3 pt-4 border-t">
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                >
                  <Save className="h-5 w-5" />
                  <span>{editingTemplate ? 'Update' : 'Create'} Template</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Preview Modal */}
      {showPreview && previewData && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-hidden">
            <div className="flex items-center justify-between p-6 border-b">
              <h2 className="text-lg font-semibold text-gray-900">Email Preview</h2>
              <button
                onClick={() => setShowPreview(false)}
                className="text-gray-600 hover:text-gray-900"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="p-6 overflow-y-auto max-h-[calc(90vh-100px)]">
              <div className="mb-4">
                <h3 className="text-sm font-medium text-gray-700 mb-2">Subject:</h3>
                <p className="text-gray-900 bg-gray-50 p-3 rounded">{previewData.subject}</p>
              </div>
              <div>
                <h3 className="text-sm font-medium text-gray-700 mb-2">Body:</h3>
                <div
                  className="border border-gray-200 rounded p-4 bg-white"
                  dangerouslySetInnerHTML={{ __html: previewData.body }}
                />
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EmailTemplateBuilder;
