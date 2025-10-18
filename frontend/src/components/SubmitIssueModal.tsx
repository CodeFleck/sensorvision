import React, { useState } from 'react';
import html2canvas from 'html2canvas';
import { IssueCategory, IssueSeverity, IssueSubmissionRequest } from '../types';
import { apiService } from '../services/api';

interface SubmitIssueModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const SubmitIssueModal: React.FC<SubmitIssueModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState<{
    title: string;
    description: string;
    category: IssueCategory;
    severity: IssueSeverity;
  }>({
    title: '',
    description: '',
    category: 'BUG',
    severity: 'MEDIUM',
  });

  const [screenshot, setScreenshot] = useState<string | null>(null);
  const [capturingScreenshot, setCapturingScreenshot] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const captureScreenshot = async () => {
    setCapturingScreenshot(true);
    setError(null);

    try {
      // Hide the modal temporarily to capture the page behind it
      const modal = document.querySelector('[data-modal="submit-issue"]') as HTMLElement;
      if (modal) {
        modal.style.display = 'none';
      }

      // Wait a moment for the page to re-render without modal
      await new Promise(resolve => setTimeout(resolve, 100));

      // Capture the page
      const canvas = await html2canvas(document.body, {
        useCORS: true,
        logging: false,
        scale: 0.5, // Reduce quality to save space
      });

      // Convert to base64
      const dataUrl = canvas.toDataURL('image/png');
      setScreenshot(dataUrl);

      // Show the modal again
      if (modal) {
        modal.style.display = 'flex';
      }
    } catch (err) {
      console.error('Failed to capture screenshot:', err);
      setError('Failed to capture screenshot. You can still submit without it.');
    } finally {
      setCapturingScreenshot(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // Gather browser information
      const browserInfo = `${navigator.userAgent.split(' ').slice(-2).join(' ')}`;
      const pageUrl = window.location.href;
      const userAgent = navigator.userAgent;
      const screenResolution = `${window.screen.width}x${window.screen.height}`;

      const requestData: IssueSubmissionRequest = {
        ...formData,
        screenshotBase64: screenshot || undefined,
        screenshotFilename: screenshot ? `screenshot-${Date.now()}.png` : undefined,
        browserInfo,
        pageUrl,
        userAgent,
        screenResolution,
      };

      await apiService.submitIssue(requestData);

      // Success! Show confirmation and close
      alert('Thank you! Your issue has been submitted successfully. Our team will review it shortly.');

      // Reset form
      setFormData({
        title: '',
        description: '',
        category: 'BUG',
        severity: 'MEDIUM',
      });
      setScreenshot(null);

      onSuccess?.();
      onClose();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to submit issue';
      setError(errorMessage);
      console.error('Failed to submit issue:', err);
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  const categoryOptions: { value: IssueCategory; label: string }[] = [
    { value: 'BUG', label: 'Bug Report' },
    { value: 'FEATURE_REQUEST', label: 'Feature Request' },
    { value: 'QUESTION', label: 'Question/Help' },
    { value: 'OTHER', label: 'Other' },
  ];

  const severityOptions: { value: IssueSeverity; label: string }[] = [
    { value: 'LOW', label: 'Low - Minor inconvenience' },
    { value: 'MEDIUM', label: 'Medium - Noticeable issue' },
    { value: 'HIGH', label: 'High - Significant problem' },
    { value: 'CRITICAL', label: 'Critical - System unusable' },
  ];

  return (
    <div
      data-modal="submit-issue"
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-lg p-6 w-full max-w-3xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-gray-800">Report an Issue</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
            disabled={loading}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex">
              <svg className="w-5 h-5 text-red-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
              <div className="text-sm text-red-800 whitespace-pre-wrap">{error}</div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Title */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Title <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              required
              maxLength={255}
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Brief summary of the issue"
              disabled={loading}
            />
          </div>

          {/* Category and Severity */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Category <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value as IssueCategory })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                disabled={loading}
              >
                {categoryOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Severity <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.severity}
                onChange={(e) => setFormData({ ...formData, severity: e.target.value as IssueSeverity })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                disabled={loading}
              >
                {severityOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description <span className="text-red-500">*</span>
            </label>
            <textarea
              required
              maxLength={5000}
              rows={6}
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Please provide detailed information about the issue, including steps to reproduce if applicable..."
              disabled={loading}
            />
            <div className="text-xs text-gray-500 mt-1">
              {formData.description.length} / 5000 characters
            </div>
          </div>

          {/* Screenshot Section */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Screenshot (Optional)
            </label>

            {!screenshot ? (
              <button
                type="button"
                onClick={captureScreenshot}
                disabled={capturingScreenshot || loading}
                className="flex items-center gap-2 px-4 py-2 text-blue-600 bg-blue-50 rounded-md hover:bg-blue-100 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                {capturingScreenshot ? 'Capturing...' : 'Capture Screenshot'}
              </button>
            ) : (
              <div className="border border-gray-300 rounded-md p-3">
                <div className="flex items-start justify-between mb-2">
                  <span className="text-sm text-gray-600 flex items-center gap-2">
                    <svg className="w-4 h-4 text-green-500" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                    Screenshot captured
                  </span>
                  <button
                    type="button"
                    onClick={() => setScreenshot(null)}
                    className="text-red-600 hover:text-red-800 text-sm"
                    disabled={loading}
                  >
                    Remove
                  </button>
                </div>
                <img
                  src={screenshot}
                  alt="Screenshot preview"
                  className="w-full h-auto max-h-64 object-contain rounded border border-gray-200"
                />
              </div>
            )}
            <p className="text-xs text-gray-500 mt-2">
              A screenshot helps us understand the issue better. The modal will be hidden during capture.
            </p>
          </div>

          {/* Rate Limit Notice */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <p className="text-sm text-blue-800">
              <strong>Note:</strong> You can submit up to 3 issue reports every 24 hours.
              This helps us manage support requests effectively.
            </p>
          </div>

          {/* Buttons */}
          <div className="flex justify-end gap-3 mt-6">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300 disabled:opacity-50"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              disabled={loading || capturingScreenshot}
            >
              {loading ? 'Submitting...' : 'Submit Issue'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
