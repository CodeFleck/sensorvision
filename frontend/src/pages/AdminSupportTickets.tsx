import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { apiService } from '../services/api';
import { AdminIssue, IssueStatus, IssueSubmission, IssueComment, IssueCommentRequest } from '../types';

export const AdminSupportTickets: React.FC = () => {
  const [issues, setIssues] = useState<AdminIssue[]>([]);
  const [filteredIssues, setFilteredIssues] = useState<AdminIssue[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<IssueStatus | 'ALL'>('ALL');
  const [selectedIssue, setSelectedIssue] = useState<IssueSubmission | null>(null);
  const [comments, setComments] = useState<IssueComment[]>([]);
  const [newComment, setNewComment] = useState('');
  const [isInternalComment, setIsInternalComment] = useState(false);
  const [submittingComment, setSubmittingComment] = useState(false);
  const [screenshotUrl, setScreenshotUrl] = useState<string | null>(null);

  useEffect(() => {
    loadIssues();
  }, []);

  useEffect(() => {
    if (statusFilter === 'ALL') {
      setFilteredIssues(issues);
    } else {
      setFilteredIssues(issues.filter(issue => issue.status === statusFilter));
    }
  }, [statusFilter, issues]);

  const loadIssues = async () => {
    try {
      setLoading(true);
      const data = await apiService.getAdminIssues();
      setIssues(data);
      setFilteredIssues(data);
    } catch (error) {
      toast.error('Failed to load issues');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const viewIssueDetails = async (issue: AdminIssue) => {
    try {
      const [fullIssue, issueComments] = await Promise.all([
        apiService.getAdminIssueById(issue.id),
        apiService.getAdminIssueComments(issue.id),
      ]);
      setSelectedIssue(fullIssue);
      setComments(issueComments);

      // Load screenshot if available
      if (fullIssue.hasScreenshot) {
        try {
          const blob = await apiService.getIssueScreenshot(issue.id);
          setScreenshotUrl(URL.createObjectURL(blob));
        } catch (error) {
          console.error('Failed to load screenshot:', error);
        }
      }
    } catch (error) {
      toast.error('Failed to load issue details');
      console.error(error);
    }
  };

  const closeIssueDetails = () => {
    setSelectedIssue(null);
    setComments([]);
    setNewComment('');
    setIsInternalComment(false);
    if (screenshotUrl) {
      URL.revokeObjectURL(screenshotUrl);
      setScreenshotUrl(null);
    }
  };

  const updateStatus = async (newStatus: IssueStatus) => {
    if (!selectedIssue) return;

    try {
      const updated = await apiService.updateIssueStatus(selectedIssue.id, newStatus);
      setSelectedIssue(updated);
      await loadIssues(); // Refresh list
      toast.success(`Issue status updated to ${newStatus}`);
    } catch (error) {
      toast.error('Failed to update status');
      console.error(error);
    }
  };

  const submitComment = async () => {
    if (!selectedIssue || !newComment.trim()) return;

    try {
      setSubmittingComment(true);
      const commentRequest: IssueCommentRequest = {
        message: newComment,
        internal: isInternalComment,
      };
      const comment = await apiService.addAdminComment(selectedIssue.id, commentRequest);
      setComments([...comments, comment]);
      setNewComment('');
      setIsInternalComment(false);
      toast.success('Comment added successfully');
    } catch (error) {
      toast.error('Failed to add comment');
      console.error(error);
    } finally {
      setSubmittingComment(false);
    }
  };

  const getSeverityBadgeColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return 'bg-red-100 text-red-800';
      case 'HIGH': return 'bg-orange-100 text-orange-800';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800';
      case 'LOW': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusBadgeColor = (status: IssueStatus) => {
    switch (status) {
      case 'SUBMITTED': return 'bg-blue-100 text-blue-800';
      case 'IN_REVIEW': return 'bg-yellow-100 text-yellow-800';
      case 'RESOLVED': return 'bg-green-100 text-green-800';
      case 'CLOSED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getCategoryLabel = (category: string) => {
    switch (category) {
      case 'BUG': return 'Bug Report';
      case 'FEATURE_REQUEST': return 'Feature Request';
      case 'QUESTION': return 'Question';
      case 'OTHER': return 'Other';
      default: return category;
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading support tickets...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">Support Tickets</h1>
        <p className="text-gray-600">Manage user-submitted issues and support requests</p>
      </div>

      {/* Filters */}
      <div className="mb-4 flex gap-2">
        <button
          onClick={() => setStatusFilter('ALL')}
          className={`px-4 py-2 rounded-md ${statusFilter === 'ALL' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
        >
          All ({issues.length})
        </button>
        <button
          onClick={() => setStatusFilter('SUBMITTED')}
          className={`px-4 py-2 rounded-md ${statusFilter === 'SUBMITTED' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
        >
          Submitted ({issues.filter(i => i.status === 'SUBMITTED').length})
        </button>
        <button
          onClick={() => setStatusFilter('IN_REVIEW')}
          className={`px-4 py-2 rounded-md ${statusFilter === 'IN_REVIEW' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
        >
          In Review ({issues.filter(i => i.status === 'IN_REVIEW').length})
        </button>
        <button
          onClick={() => setStatusFilter('RESOLVED')}
          className={`px-4 py-2 rounded-md ${statusFilter === 'RESOLVED' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
        >
          Resolved ({issues.filter(i => i.status === 'RESOLVED').length})
        </button>
        <button
          onClick={() => setStatusFilter('CLOSED')}
          className={`px-4 py-2 rounded-md ${statusFilter === 'CLOSED' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
        >
          Closed ({issues.filter(i => i.status === 'CLOSED').length})
        </button>
      </div>

      {/* Issues Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                ID
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Title
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Category
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Severity
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                User
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Comments
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Created
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredIssues.length === 0 ? (
              <tr>
                <td colSpan={9} className="px-6 py-4 text-center text-gray-500">
                  No issues found
                </td>
              </tr>
            ) : (
              filteredIssues.map((issue) => (
                <tr key={issue.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    #{issue.id}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-900">
                    <div className="max-w-xs truncate" title={issue.title}>
                      {issue.title}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {getCategoryLabel(issue.category)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getSeverityBadgeColor(issue.severity)}`}>
                      {issue.severity}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getStatusBadgeColor(issue.status)}`}>
                      {issue.status.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    <div>{issue.username}</div>
                    <div className="text-xs text-gray-400">{issue.organizationName}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {issue.commentCount}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {new Date(issue.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <button
                      onClick={() => viewIssueDetails(issue)}
                      className="text-blue-600 hover:text-blue-800 font-medium"
                    >
                      View Details
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Issue Detail Modal */}
      {selectedIssue && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg w-full max-w-5xl max-h-[90vh] overflow-hidden flex flex-col">
            {/* Header */}
            <div className="p-6 border-b border-gray-200 flex justify-between items-start">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-2">
                  <h2 className="text-2xl font-bold text-gray-800">#{selectedIssue.id} - {selectedIssue.title}</h2>
                  {selectedIssue.hasScreenshot && (
                    <span className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded">
                      Has Screenshot
                    </span>
                  )}
                </div>
                <div className="flex gap-2 mb-2">
                  <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getSeverityBadgeColor(selectedIssue.severity)}`}>
                    {selectedIssue.severity}
                  </span>
                  <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getStatusBadgeColor(selectedIssue.status)}`}>
                    {selectedIssue.status.replace('_', ' ')}
                  </span>
                  <span className="px-2 py-1 text-xs font-semibold rounded-full bg-purple-100 text-purple-800">
                    {getCategoryLabel(selectedIssue.category)}
                  </span>
                </div>
                <div className="text-sm text-gray-600">
                  Submitted by <strong>{selectedIssue.username}</strong> ({selectedIssue.organizationName}) on {new Date(selectedIssue.createdAt).toLocaleString()}
                </div>
              </div>
              <button onClick={closeIssueDetails} className="text-gray-400 hover:text-gray-600">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto p-6">
              {/* Status Update */}
              <div className="mb-6 bg-gray-50 p-4 rounded-lg">
                <label className="block text-sm font-medium text-gray-700 mb-2">Update Status</label>
                <div className="flex gap-2">
                  {(['SUBMITTED', 'IN_REVIEW', 'RESOLVED', 'CLOSED'] as IssueStatus[]).map((status) => (
                    <button
                      key={status}
                      onClick={() => updateStatus(status)}
                      disabled={selectedIssue.status === status}
                      className={`px-4 py-2 rounded-md text-sm font-medium ${
                        selectedIssue.status === status
                          ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                          : 'bg-blue-600 text-white hover:bg-blue-700'
                      }`}
                    >
                      {status.replace('_', ' ')}
                    </button>
                  ))}
                </div>
              </div>

              {/* Description */}
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-2">Description</h3>
                <div className="bg-gray-50 p-4 rounded-lg whitespace-pre-wrap">{selectedIssue.description}</div>
              </div>

              {/* Technical Details */}
              {(selectedIssue.browserInfo || selectedIssue.pageUrl || selectedIssue.userAgent || selectedIssue.screenResolution) && (
                <div className="mb-6">
                  <h3 className="text-lg font-semibold text-gray-800 mb-2">Technical Details</h3>
                  <div className="bg-gray-50 p-4 rounded-lg space-y-2 text-sm">
                    {selectedIssue.pageUrl && (
                      <div><strong>Page URL:</strong> {selectedIssue.pageUrl}</div>
                    )}
                    {selectedIssue.browserInfo && (
                      <div><strong>Browser:</strong> {selectedIssue.browserInfo}</div>
                    )}
                    {selectedIssue.screenResolution && (
                      <div><strong>Screen Resolution:</strong> {selectedIssue.screenResolution}</div>
                    )}
                    {selectedIssue.userAgent && (
                      <div><strong>User Agent:</strong> <span className="text-xs break-all">{selectedIssue.userAgent}</span></div>
                    )}
                  </div>
                </div>
              )}

              {/* Screenshot */}
              {screenshotUrl && (
                <div className="mb-6">
                  <h3 className="text-lg font-semibold text-gray-800 mb-2">Screenshot</h3>
                  <img src={screenshotUrl} alt="Issue screenshot" className="border border-gray-300 rounded-lg max-w-full" />
                </div>
              )}

              {/* Comments Section */}
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-4">Conversation ({comments.length})</h3>
                <div className="space-y-4 mb-4">
                  {comments.length === 0 ? (
                    <div className="text-center text-gray-500 py-4">No comments yet</div>
                  ) : (
                    comments.map((comment) => (
                      <div
                        key={comment.id}
                        className={`p-4 rounded-lg ${comment.internal ? 'bg-yellow-50 border border-yellow-200' : 'bg-gray-50'}`}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div className="font-semibold text-gray-800">
                            {comment.authorName}
                            {comment.internal && (
                              <span className="ml-2 text-xs bg-yellow-200 text-yellow-800 px-2 py-1 rounded">
                                Internal Note
                              </span>
                            )}
                          </div>
                          <div className="text-xs text-gray-500">
                            {new Date(comment.createdAt).toLocaleString()}
                          </div>
                        </div>
                        <div className="text-gray-700 whitespace-pre-wrap">{comment.message}</div>
                      </div>
                    ))
                  )}
                </div>

                {/* Add Comment */}
                <div className="bg-gray-50 p-4 rounded-lg">
                  <label className="block text-sm font-medium text-gray-700 mb-2">Add Comment</label>
                  <textarea
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    rows={4}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Type your reply..."
                  />
                  <div className="mt-2 flex items-center justify-between">
                    <label className="flex items-center text-sm text-gray-700">
                      <input
                        type="checkbox"
                        checked={isInternalComment}
                        onChange={(e) => setIsInternalComment(e.target.checked)}
                        className="mr-2"
                      />
                      Internal note (not visible to user)
                    </label>
                    <button
                      onClick={submitComment}
                      disabled={!newComment.trim() || submittingComment}
                      className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {submittingComment ? 'Posting...' : 'Post Comment'}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
