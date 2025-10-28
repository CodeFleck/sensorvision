import React, { useState, useEffect } from 'react';
import { MessageSquare, X, ChevronDown, TrendingUp } from 'lucide-react';
import { apiService } from '../services/api';
import toast from 'react-hot-toast';

interface CannedResponse {
  id: number;
  title: string;
  body: string;
  category: string | null;
  useCount: number;
}

interface CannedResponsePickerProps {
  onSelect: (template: string) => void;
  buttonClassName?: string;
}

export const CannedResponsePicker: React.FC<CannedResponsePickerProps> = ({
  onSelect,
  buttonClassName = 'px-3 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [templates, setTemplates] = useState<CannedResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');

  useEffect(() => {
    if (isOpen) {
      loadTemplates();
    }
  }, [isOpen, categoryFilter]);

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

  const handleSelectTemplate = async (template: CannedResponse) => {
    onSelect(template.body);
    setIsOpen(false);

    // Track usage
    try {
      await apiService.markCannedResponseAsUsed(template.id);
    } catch (error) {
      console.error('Failed to track template usage:', error);
      // Don't show error to user, this is background tracking
    }
  };

  const categories = ['ALL', 'GENERAL', 'AUTHENTICATION', 'RESOLUTION', 'BUG', 'FEATURE_REQUEST'];

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={buttonClassName}
      >
        <div className="flex items-center gap-2">
          <MessageSquare className="h-4 w-4" />
          <span>Use Template</span>
          <ChevronDown className="h-4 w-4" />
        </div>
      </button>

      {isOpen && (
        <>
          {/* Backdrop */}
          <div
            data-testid="canned-response-backdrop"
            className="fixed inset-0 z-[100]"
            onClick={() => setIsOpen(false)}
          />

          {/* Dropdown */}
          <div className="absolute left-0 mt-2 w-[500px] bg-white rounded-lg shadow-2xl border border-gray-200 z-[110] max-h-[600px] overflow-hidden flex flex-col">
            {/* Header */}
            <div className="p-3 border-b border-gray-200 bg-gray-50">
              <div className="flex items-center justify-between mb-2">
                <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                  <MessageSquare className="h-4 w-4" />
                  Canned Responses
                </h3>
                <button
                  onClick={() => setIsOpen(false)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {/* Category Filter */}
              <select
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
                className="w-full px-2 py-1 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {categories.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat === 'ALL' ? 'All Categories' : cat.replace('_', ' ')}
                  </option>
                ))}
              </select>
            </div>

            {/* Templates List */}
            <div className="overflow-y-auto flex-1">
              {loading ? (
                <div className="p-4 text-center text-gray-500">
                  Loading templates...
                </div>
              ) : templates.length === 0 ? (
                <div className="p-4 text-center text-gray-500">
                  No templates found
                </div>
              ) : (
                <div className="divide-y divide-gray-100">
                  {templates.map((template) => (
                    <button
                      key={template.id}
                      onClick={() => handleSelectTemplate(template)}
                      className="w-full text-left p-3 hover:bg-blue-50 transition-colors"
                    >
                      <div className="flex items-start justify-between gap-2 mb-1">
                        <div className="font-medium text-gray-900 text-sm">
                          {template.title}
                        </div>
                        {template.useCount > 0 && (
                          <div className="flex items-center gap-1 text-xs text-gray-500">
                            <TrendingUp className="h-3 w-3" />
                            {template.useCount}
                          </div>
                        )}
                      </div>
                      {template.category && (
                        <div className="text-xs text-gray-500 mb-1">
                          {template.category.replace('_', ' ')}
                        </div>
                      )}
                      <div className="text-sm text-gray-600 line-clamp-4">
                        {template.body}
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Footer */}
            <div className="p-2 border-t border-gray-200 bg-gray-50 text-xs text-gray-500 text-center">
              Click a template to insert it into your reply
            </div>
          </div>
        </>
      )}
    </div>
  );
};
