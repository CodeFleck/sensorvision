import React, { useState } from 'react';
import {
  X,
  Star,
  Download,
  ExternalLink,
  Award,
  Verified,
  Package,
  Calendar,
  GitBranch,
  FileText,
  Image as ImageIcon,
} from 'lucide-react';
import { PluginRegistry } from '../../services/pluginMarketplaceService';

interface PluginDetailsModalProps {
  open: boolean;
  plugin: PluginRegistry;
  onClose: () => void;
  onInstall: (pluginKey: string) => void;
}

const PluginDetailsModal: React.FC<PluginDetailsModalProps> = ({
  open,
  plugin,
  onClose,
  onInstall,
}) => {
  const [selectedScreenshot, setSelectedScreenshot] = useState<number>(0);

  if (!open) return null;

  const getCategoryColor = (category: string): string => {
    const colors: Record<string, string> = {
      DATA_INGESTION: 'bg-blue-100 text-blue-800',
      NOTIFICATION: 'bg-green-100 text-green-800',
      ANALYTICS: 'bg-purple-100 text-purple-800',
      INTEGRATION: 'bg-orange-100 text-orange-800',
      UTILITY: 'bg-gray-100 text-gray-800',
    };
    return colors[category] || 'bg-gray-100 text-gray-800';
  };

  const formatCategory = (category: string): string => {
    return category.replace(/_/g, ' ');
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">
        {/* Background overlay */}
        <div
          className="fixed inset-0 transition-opacity bg-gray-500 bg-opacity-75"
          onClick={onClose}
        />

        {/* Modal panel */}
        <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-4xl sm:w-full">
          {/* Header */}
          <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-4">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-4">
                {plugin.iconUrl ? (
                  <img
                    src={plugin.iconUrl}
                    alt={plugin.name}
                    className="w-16 h-16 rounded-lg bg-white p-2"
                  />
                ) : (
                  <div className="w-16 h-16 bg-white rounded-lg flex items-center justify-center">
                    <Package className="w-8 h-8 text-blue-600" />
                  </div>
                )}
                <div className="text-white">
                  <h2 className="text-2xl font-bold flex items-center gap-2">
                    {plugin.name}
                    {plugin.isOfficial && (
                      <Award className="w-5 h-5 text-yellow-300" title="Official Plugin" />
                    )}
                    {plugin.isVerified && (
                      <Verified className="w-5 h-5 text-green-300" title="Verified Plugin" />
                    )}
                  </h2>
                  <p className="text-blue-100 text-sm">by {plugin.author}</p>
                  <div className="flex items-center gap-4 mt-2 text-sm">
                    <div className="flex items-center gap-1">
                      <Star className="w-4 h-4 text-yellow-300 fill-current" />
                      <span>
                        {plugin.averageRating.toFixed(1)} ({plugin.ratingCount} reviews)
                      </span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Download className="w-4 h-4" />
                      <span>{plugin.downloads.toLocaleString()} downloads</span>
                    </div>
                  </div>
                </div>
              </div>
              <button
                onClick={onClose}
                className="text-white hover:text-gray-200 transition-colors"
              >
                <X className="w-6 h-6" />
              </button>
            </div>
          </div>

          {/* Body */}
          <div className="px-6 py-6 max-h-[70vh] overflow-y-auto">
            {/* Quick Info */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
              <div>
                <div className="text-xs text-gray-500 uppercase tracking-wide mb-1">Version</div>
                <div className="text-sm font-medium text-gray-900">{plugin.version}</div>
              </div>
              <div>
                <div className="text-xs text-gray-500 uppercase tracking-wide mb-1">Category</div>
                <span
                  className={`inline-flex px-2 py-1 text-xs font-medium rounded ${getCategoryColor(
                    plugin.category
                  )}`}
                >
                  {formatCategory(plugin.category)}
                </span>
              </div>
              <div>
                <div className="text-xs text-gray-500 uppercase tracking-wide mb-1">Published</div>
                <div className="text-sm font-medium text-gray-900">
                  {new Date(plugin.createdAt).toLocaleDateString()}
                </div>
              </div>
              <div>
                <div className="text-xs text-gray-500 uppercase tracking-wide mb-1">
                  Last Updated
                </div>
                <div className="text-sm font-medium text-gray-900">
                  {new Date(plugin.updatedAt).toLocaleDateString()}
                </div>
              </div>
            </div>

            {/* Description */}
            <div className="mb-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Description</h3>
              <p className="text-gray-700 leading-relaxed">{plugin.description}</p>
            </div>

            {/* Screenshots */}
            {plugin.screenshots && plugin.screenshots.length > 0 && (
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Screenshots</h3>
                <div className="space-y-3">
                  <div className="bg-gray-100 rounded-lg overflow-hidden">
                    <img
                      src={plugin.screenshots[selectedScreenshot]}
                      alt={`Screenshot ${selectedScreenshot + 1}`}
                      className="w-full h-auto"
                    />
                  </div>
                  {plugin.screenshots.length > 1 && (
                    <div className="flex gap-2 overflow-x-auto">
                      {plugin.screenshots.map((screenshot, index) => (
                        <button
                          key={index}
                          onClick={() => setSelectedScreenshot(index)}
                          className={`flex-shrink-0 w-20 h-20 rounded border-2 overflow-hidden transition-all ${
                            selectedScreenshot === index
                              ? 'border-blue-600'
                              : 'border-gray-300 opacity-60 hover:opacity-100'
                          }`}
                        >
                          <img
                            src={screenshot}
                            alt={`Thumbnail ${index + 1}`}
                            className="w-full h-full object-cover"
                          />
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Tags */}
            {plugin.tags && plugin.tags.length > 0 && (
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Tags</h3>
                <div className="flex flex-wrap gap-2">
                  {plugin.tags.map((tag, index) => (
                    <span
                      key={index}
                      className="px-3 py-1 bg-gray-100 text-gray-700 text-sm rounded-full"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Compatible Versions */}
            {plugin.compatibleVersions && plugin.compatibleVersions.length > 0 && (
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-2 flex items-center gap-2">
                  <GitBranch className="w-5 h-5" />
                  Compatible Versions
                </h3>
                <div className="flex flex-wrap gap-2">
                  {plugin.compatibleVersions.map((version, index) => (
                    <span
                      key={index}
                      className="px-2 py-1 bg-blue-50 text-blue-700 text-xs font-mono rounded"
                    >
                      {version}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Links */}
            <div className="mb-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-3">Resources</h3>
              <div className="space-y-2">
                {plugin.websiteUrl && (
                  <a
                    href={plugin.websiteUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-2 text-blue-600 hover:text-blue-800"
                  >
                    <ExternalLink className="w-4 h-4" />
                    Website
                  </a>
                )}
                {plugin.documentationUrl && (
                  <a
                    href={plugin.documentationUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-2 text-blue-600 hover:text-blue-800"
                  >
                    <FileText className="w-4 h-4" />
                    Documentation
                  </a>
                )}
                {plugin.repositoryUrl && (
                  <a
                    href={plugin.repositoryUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-2 text-blue-600 hover:text-blue-800"
                  >
                    <GitBranch className="w-4 h-4" />
                    Repository
                  </a>
                )}
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="bg-gray-50 px-6 py-4 flex items-center justify-end gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors"
            >
              Close
            </button>
            <button
              onClick={() => {
                onInstall(plugin.key);
                onClose();
              }}
              className="flex items-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <Download className="w-4 h-4" />
              Install Plugin
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PluginDetailsModal;
