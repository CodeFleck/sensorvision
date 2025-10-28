import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { Search, X, Calendar, MessageSquare, AlertCircle, User, Building } from 'lucide-react';
import { apiService } from '../services/api';
import { AdminIssue, IssueStatus, IssueSubmission, IssueComment, IssueCommentRequest } from '../types';
import { CannedResponsePicker } from '../components/CannedResponsePicker';
import { getStatusInfo, getSeverityInfo } from '../utils/issueStatusHelpers';

export const AdminSupportTickets: React.FC = () => {
  const [issues, setIssues] = useState<AdminIssue[]>([]);
  const [filteredIssues, setFilteredIssues] = useState<AdminIssue[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<IssueStatus | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState<'cards' | 'table'>('cards');
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
    let filtered = issues;

    // Apply status filter
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(issue => issue.status === statusFilter);
    }

    // Apply search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(issue =>
        issue.title.toLowerCase().includes(query) ||
        issue.description.toLowerCase().includes(query) ||
        issue.username.toLowerCase().includes(query) ||
        issue.organizationName.toLowerCase().includes(query) ||
        issue.id.toString().includes(query)
      );
    }

    setFilteredIssues(filtered);
  }, [statusFilter, searchQuery, issues]);

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
      // Clear previous screenshot to prevent stale images from lingering
      if (screenshotUrl) {
        URL.revokeObjectURL(screenshotUrl);
        setScreenshotUrl(null);
      }

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
    <div className="p-4 sm:p-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-2">Support Tickets</h1>
        <p className="text-sm sm:text-base text-gray-600">Manage user-submitted issues and support requests</p>
      </div>

      {/* Search and Filter Section */}
      <div className="mb-6 space-y-4">
        {/* Search Bar */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            type="text"
            placeholder="Search by ID, title, description, user, or organization..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-10 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              <X className="h-5 w-5" />
            </button>
          )}
        </div>

        {/* Status Filters and View Toggle */}
        <div className="flex flex-wrap items-center gap-2 justify-between">
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setStatusFilter('ALL')}
              className={`px-3 sm:px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                statusFilter === 'ALL'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              All <span className="ml-1 opacity-75">({issues.length})</span>
            </button>
            <button
              onClick={() => setStatusFilter('SUBMITTED')}
              className={`px-3 sm:px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                statusFilter === 'SUBMITTED'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              New <span className="ml-1 opacity-75">({issues.filter(i => i.status === 'SUBMITTED').length})</span>
            </button>
            <button
              onClick={() => setStatusFilter('IN_REVIEW')}
              className={`px-3 sm:px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                statusFilter === 'IN_REVIEW'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              In Review <span className="ml-1 opacity-75">({issues.filter(i => i.status === 'IN_REVIEW').length})</span>
            </button>
            <button
              onClick={() => setStatusFilter('RESOLVED')}
              className={`px-3 sm:px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                statusFilter === 'RESOLVED'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              Resolved <span className="ml-1 opacity-75">({issues.filter(i => i.status === 'RESOLVED').length})</span>
            </button>
            <button
              onClick={() => setStatusFilter('CLOSED')}
              className={`px-3 sm:px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                statusFilter === 'CLOSED'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              Closed <span className="ml-1 opacity-75">({issues.filter(i => i.status === 'CLOSED').length})</span>
            </button>
          </div>

          {/* View Mode Toggle */}
          <div className="flex gap-1 bg-gray-100 rounded-lg p-1">
            <button
              onClick={() => setViewMode('cards')}
              className={`px-3 py-1.5 rounded text-sm font-medium transition-colors ${
                viewMode === 'cards'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Cards
            </button>
            <button
              onClick={() => setViewMode('table')}
              className={`px-3 py-1.5 rounded text-sm font-medium transition-colors ${
                viewMode === 'table'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Table
            </button>
          </div>
        </div>
      </div>

      {/* Results Summary */}
      {searchQuery && (
        <div className="mb-4 text-sm text-gray-600">
          Found <span className="font-semibold">{filteredIssues.length}</span> ticket{filteredIssues.length !== 1 ? 's' : ''} matching "{searchQuery}"
        </div>
      )}

      {/* Empty State */}
      {filteredIssues.length === 0 ? (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-8 sm:p-12 text-center">
          <div className="max-w-md mx-auto">
            {searchQuery ? (
              <>
                <Search className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-semibold text-gray-800 mb-2">No tickets found</h3>
                <p className="text-gray-600 mb-4">
                  No tickets match your search criteria. Try adjusting your search or filters.
                </p>
                <button
                  onClick={() => {
                    setSearchQuery('');
                    setStatusFilter('ALL');
                  }}
                  className="text-blue-600 hover:text-blue-700 font-medium"
                >
                  Clear all filters
                </button>
              </>
            ) : (
              <>
                <AlertCircle className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-semibold text-gray-800 mb-2">
                  {statusFilter === 'ALL'
                    ? "No support tickets"
                    : `No ${statusFilter.toLowerCase().replace('_', ' ')} tickets`}
                </h3>
                <p className="text-gray-600">
                  {statusFilter === 'ALL'
                    ? "No users have submitted support tickets yet."
                    : `There are no ${statusFilter.toLowerCase().replace('_', ' ')} tickets at the moment.`}
                </p>
              </>
            )}
          </div>
        </div>
      ) : viewMode === 'cards' ? (
        /* Card View */
        <div className="space-y-3">
          {filteredIssues.map((issue) => {
            const statusInfo = getStatusInfo(issue.status);
            const severityInfo = getSeverityInfo(issue.severity);
            const isNewTicket = issue.status === 'SUBMITTED' && issue.commentCount === 0;

            return (
              <div
                key={issue.id}
                className={`bg-white rounded-lg border hover:border-blue-300 hover:shadow-md transition-all cursor-pointer overflow-hidden ${
                  isNewTicket ? 'border-blue-400 ring-2 ring-blue-100' : 'border-gray-200'
                }`}
                onClick={() => viewIssueDetails(issue)}
              >
                <div className="flex">
                  {/* Left Border Color Indicator */}
                  <div className={`w-1.5 ${
                    issue.severity === 'CRITICAL' ? 'bg-red-500' :
                    issue.severity === 'HIGH' ? 'bg-orange-500' :
                    issue.severity === 'MEDIUM' ? 'bg-yellow-500' :
                    'bg-blue-500'
                  }`} />

                  <div className="flex-1 p-4 sm:p-5">
                    {/* Header Row */}
                    <div className="flex items-start justify-between gap-4 mb-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-2">
                          <span className="text-xs font-mono text-gray-500">#{issue.id}</span>
                          {isNewTicket && (
                            <span className="px-2 py-0.5 text-xs font-semibold bg-blue-600 text-white rounded-full">
                              NEW
                            </span>
                          )}
                          <h3 className="text-base sm:text-lg font-semibold text-gray-900 truncate">
                            {issue.title}
                          </h3>
                        </div>

                        {/* Badges */}
                        <div className="flex flex-wrap items-center gap-2 mb-3">
                          <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-medium rounded-full ${statusInfo.color}`}>
                            <span className="text-sm">{statusInfo.icon}</span>
                            <span className="hidden sm:inline">{statusInfo.label}</span>
                          </span>
                          <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-medium rounded-full ${severityInfo.color}`}>
                            <span className="text-sm">{severityInfo.icon}</span>
                            {severityInfo.label}
                          </span>
                          <span className="px-2.5 py-0.5 text-xs font-medium rounded-full bg-purple-100 text-purple-800">
                            {getCategoryLabel(issue.category)}
                          </span>
                        </div>

                        {/* User and Organization Info */}
                        <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-gray-600 mb-2">
                          <div className="flex items-center gap-1">
                            <User className="h-4 w-4 text-gray-400" />
                            <span className="font-medium">{issue.username}</span>
                          </div>
                          <div className="flex items-center gap-1">
                            <Building className="h-4 w-4 text-gray-400" />
                            <span>{issue.organizationName}</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Description Preview */}
                    <p className="text-sm text-gray-600 line-clamp-2 mb-3">
                      {issue.description}
                    </p>

                    {/* Footer Metadata */}
                    <div className="flex flex-wrap items-center gap-3 sm:gap-4 text-xs text-gray-500">
                      <div className="flex items-center gap-1">
                        <Calendar className="h-3.5 w-3.5" />
                        <span>{new Date(issue.createdAt).toLocaleDateString()}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <MessageSquare className="h-3.5 w-3.5" />
                        <span>{issue.commentCount} comment{issue.commentCount !== 1 ? 's' : ''}</span>
                      </div>
                      <div className="flex items-center gap-1 ml-auto text-blue-600 font-medium">
                        <span>View Details</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* Table View */
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 sm:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Ticket
                </th>
                <th className="px-4 sm:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider hidden lg:table-cell">
                  Category
                </th>
                <th className="px-4 sm:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-4 sm:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider hidden md:table-cell">
                  User
                </th>
                <th className="px-4 sm:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider hidden sm:table-cell">
                  Comments
                </th>
                <th className="px-4 sm:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider hidden xl:table-cell">
                  Created
                </th>
                <th className="px-4 sm:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredIssues.map((issue) => {
                const statusInfo = getStatusInfo(issue.status);
                const severityInfo = getSeverityInfo(issue.severity);

                return (
                  <tr key={issue.id} className="hover:bg-gray-50">
                    <td className="px-4 sm:px-6 py-4 text-sm">
                      <div className="flex items-start gap-2">
                        <span className="text-gray-500 font-mono">#{issue.id}</span>
                        <div className="min-w-0">
                          <div className="font-medium text-gray-900 truncate max-w-xs" title={issue.title}>
                            {issue.title}
                          </div>
                          <div className="flex gap-1 mt-1">
                            <span className={`inline-flex items-center gap-0.5 px-1.5 py-0.5 text-xs font-medium rounded ${severityInfo.color}`}>
                              {severityInfo.icon}
                            </span>
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 sm:px-6 py-4 whitespace-nowrap text-sm text-gray-600 hidden lg:table-cell">
                      {getCategoryLabel(issue.category)}
                    </td>
                    <td className="px-4 sm:px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex items-center gap-1 px-2 py-1 text-xs font-medium rounded-full ${statusInfo.color}`}>
                        <span>{statusInfo.icon}</span>
                        <span className="hidden sm:inline">{statusInfo.label}</span>
                      </span>
                    </td>
                    <td className="px-4 sm:px-6 py-4 text-sm text-gray-600 hidden md:table-cell">
                      <div className="font-medium">{issue.username}</div>
                      <div className="text-xs text-gray-400">{issue.organizationName}</div>
                    </td>
                    <td className="px-4 sm:px-6 py-4 whitespace-nowrap text-sm text-gray-600 text-center hidden sm:table-cell">
                      {issue.commentCount}
                    </td>
                    <td className="px-4 sm:px-6 py-4 whitespace-nowrap text-sm text-gray-600 hidden xl:table-cell">
                      {new Date(issue.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-4 sm:px-6 py-4 whitespace-nowrap text-sm">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          viewIssueDetails(issue);
                        }}
                        className="text-blue-600 hover:text-blue-800 font-medium"
                      >
                        View
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Issue Detail Modal */}
      {selectedIssue && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 overflow-y-auto">
          <div className="bg-white rounded-xl w-full max-w-5xl my-8 overflow-hidden flex flex-col shadow-2xl">
            {/* Header */}
            <div className="bg-gradient-to-r from-indigo-600 to-indigo-700 p-6 text-white">
              <div className="flex justify-between items-start gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-sm font-mono opacity-90">#{selectedIssue.id}</span>
                    {selectedIssue.hasScreenshot && (
                      <span className="text-xs bg-white bg-opacity-20 px-2 py-1 rounded">
                        Screenshot
                      </span>
                    )}
                  </div>
                  <h2 className="text-xl sm:text-2xl font-bold mb-4">
                    {selectedIssue.title}
                  </h2>
                  <div className="flex flex-wrap gap-2 mb-3">
                    {(() => {
                      const severityInfo = getSeverityInfo(selectedIssue.severity);
                      return (
                        <span className={`px-3 py-1 text-xs font-semibold rounded-full ${severityInfo.color}`}>
                          {severityInfo.icon} {severityInfo.label}
                        </span>
                      );
                    })()}
                    <span className="px-3 py-1 text-xs font-semibold rounded-full bg-purple-100 text-purple-800">
                      {getCategoryLabel(selectedIssue.category)}
                    </span>
                  </div>
                  <div className="text-sm text-indigo-100">
                    Submitted by <span className="font-semibold text-white">{selectedIssue.username}</span> ({selectedIssue.organizationName})
                    <span className="mx-2">•</span>
                    {new Date(selectedIssue.createdAt).toLocaleDateString()}
                  </div>
                </div>
                <button
                  onClick={closeIssueDetails}
                  className="text-white hover:text-indigo-100 transition-colors"
                  aria-label="Close"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>
            </div>

            {/* Status Banner */}
            {(() => {
              const statusInfo = getStatusInfo(selectedIssue.status);
              return (
                <div className={`px-6 py-3 border-b ${statusInfo.color} border-opacity-50`}>
                  <div className="flex items-center gap-3">
                    <span className="text-xl">{statusInfo.icon}</span>
                    <div className="flex-1">
                      <div className="font-semibold text-sm">{statusInfo.label}</div>
                    </div>
                  </div>
                </div>
              );
            })()}

            {/* Content */}
            <div className="flex-1 overflow-y-auto p-6 space-y-6">
              {/* Quick Actions - Status Update */}
              <div className="bg-gradient-to-r from-gray-50 to-gray-100 p-4 rounded-lg border border-gray-200">
                <label className="block text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                  <span>Quick Actions</span>
                  <span className="text-xs font-normal text-gray-500">(Update ticket status)</span>
                </label>
                <div className="flex flex-wrap gap-2">
                  {(['SUBMITTED', 'IN_REVIEW', 'RESOLVED', 'CLOSED'] as IssueStatus[]).map((status) => {
                    const statusInfo = getStatusInfo(status);
                    const isCurrentStatus = selectedIssue.status === status;

                    return (
                      <button
                        key={status}
                        onClick={() => updateStatus(status)}
                        disabled={isCurrentStatus}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                          isCurrentStatus
                            ? 'bg-white border-2 border-indigo-600 text-indigo-600 cursor-default shadow-sm'
                            : 'bg-white border border-gray-300 text-gray-700 hover:bg-indigo-50 hover:border-indigo-300 hover:text-indigo-600'
                        }`}
                      >
                        <span className="inline-flex items-center gap-1.5">
                          <span>{statusInfo.icon}</span>
                          <span>{statusInfo.label}</span>
                          {isCurrentStatus && <span className="text-xs">(Current)</span>}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Description */}
              <div>
                <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">User's Description</h3>
                <div className="bg-gray-50 border border-gray-200 p-4 rounded-lg whitespace-pre-wrap text-gray-700 leading-relaxed">
                  {selectedIssue.description}
                </div>
              </div>

              {/* Technical Details */}
              {(selectedIssue.browserInfo || selectedIssue.pageUrl || selectedIssue.userAgent || selectedIssue.screenResolution) && (
                <div>
                  <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Technical Details</h3>
                  <div className="bg-gray-50 border border-gray-200 p-4 rounded-lg space-y-3 text-sm">
                    {selectedIssue.pageUrl && (
                      <div className="flex gap-2">
                        <span className="font-semibold text-gray-700 min-w-[120px]">Page URL:</span>
                        <span className="text-gray-600 break-all">{selectedIssue.pageUrl}</span>
                      </div>
                    )}
                    {selectedIssue.browserInfo && (
                      <div className="flex gap-2">
                        <span className="font-semibold text-gray-700 min-w-[120px]">Browser:</span>
                        <span className="text-gray-600">{selectedIssue.browserInfo}</span>
                      </div>
                    )}
                    {selectedIssue.screenResolution && (
                      <div className="flex gap-2">
                        <span className="font-semibold text-gray-700 min-w-[120px]">Screen:</span>
                        <span className="text-gray-600">{selectedIssue.screenResolution}</span>
                      </div>
                    )}
                    {selectedIssue.userAgent && (
                      <div className="flex gap-2">
                        <span className="font-semibold text-gray-700 min-w-[120px]">User Agent:</span>
                        <span className="text-gray-600 text-xs break-all">{selectedIssue.userAgent}</span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Screenshot */}
              {screenshotUrl && (
                <div>
                  <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Screenshot</h3>
                  <div className="border-2 border-gray-200 rounded-lg overflow-hidden">
                    <img src={screenshotUrl} alt="Issue screenshot" className="w-full" />
                  </div>
                </div>
              )}

              {/* Comments Section */}
              <div>
                <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
                  Conversation {comments.length > 0 && `(${comments.length})`}
                </h3>

                {comments.length === 0 ? (
                  <div className="text-center py-12 bg-gray-50 border border-gray-200 rounded-lg mb-4">
                    <MessageSquare className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                    <p className="text-gray-600 font-medium mb-1">No comments yet</p>
                    <p className="text-sm text-gray-500">Be the first to respond to this ticket</p>
                  </div>
                ) : (
                  <div className="space-y-3 mb-4">
                    {comments.map((comment) => {
                      const isAdminComment = !comment.internal && comment.authorName !== selectedIssue.username;
                      const isUserComment = comment.authorName === selectedIssue.username;

                      return (
                        <div
                          key={comment.id}
                          className={`relative ${
                            comment.internal
                              ? ''
                              : isAdminComment
                                ? 'ml-8'
                                : 'mr-8'
                          }`}
                        >
                          <div className={`p-4 rounded-lg shadow-sm ${
                            comment.internal
                              ? 'bg-yellow-50 border-2 border-yellow-300'
                              : isAdminComment
                                ? 'bg-indigo-50 border border-indigo-200'
                                : 'bg-white border border-gray-200'
                          }`}>
                            <div className="flex items-start justify-between mb-2">
                              <div className="flex items-center gap-2">
                                {!comment.internal && isAdminComment && (
                                  <div className="w-8 h-8 bg-indigo-600 text-white rounded-full flex items-center justify-center text-xs font-bold">
                                    {comment.authorName.substring(0, 2).toUpperCase()}
                                  </div>
                                )}
                                {!comment.internal && isUserComment && (
                                  <div className="w-8 h-8 bg-gray-400 text-white rounded-full flex items-center justify-center text-xs font-bold">
                                    {comment.authorName.substring(0, 2).toUpperCase()}
                                  </div>
                                )}
                                <div>
                                  <div className="font-semibold text-sm text-gray-800">
                                    {comment.authorName}
                                    {comment.internal && ' (Internal)'}
                                  </div>
                                  <div className="text-xs text-gray-500">
                                    {new Date(comment.createdAt).toLocaleString()}
                                  </div>
                                </div>
                              </div>
                              {comment.internal && (
                                <span className="text-xs bg-yellow-200 text-yellow-900 px-2.5 py-1 rounded-full font-semibold">
                                  Internal Note
                                </span>
                              )}
                              {!comment.internal && isAdminComment && (
                                <span className="text-xs bg-indigo-100 text-indigo-700 px-2.5 py-1 rounded-full font-medium">
                                  Staff
                                </span>
                              )}
                              {!comment.internal && isUserComment && (
                                <span className="text-xs bg-gray-100 text-gray-700 px-2.5 py-1 rounded-full font-medium">
                                  User
                                </span>
                              )}
                            </div>
                            <div className="text-gray-700 whitespace-pre-wrap text-sm leading-relaxed">
                              {comment.message}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* Add Comment */}
                <div className="bg-gradient-to-br from-gray-50 to-white border-2 border-gray-200 p-4 rounded-lg shadow-sm">
                  <label className="block text-sm font-semibold text-gray-700 mb-3">Add Response</label>
                  <textarea
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    rows={4}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
                    placeholder="Type your response or select a template..."
                  />
                  <div className="mt-3 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                    <label className="flex items-center text-sm text-gray-700 cursor-pointer hover:text-gray-900">
                      <input
                        type="checkbox"
                        checked={isInternalComment}
                        onChange={(e) => setIsInternalComment(e.target.checked)}
                        className="mr-2 w-4 h-4 text-indigo-600 rounded focus:ring-indigo-500"
                      />
                      <span className="font-medium">Internal note</span>
                      <span className="ml-1 text-gray-500">(not visible to user)</span>
                    </label>
                    <div className="flex items-center gap-2 w-full sm:w-auto">
                      <CannedResponsePicker
                        onSelect={(template) => setNewComment(newComment + template)}
                        buttonClassName="px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-white hover:border-indigo-300 transition-colors font-medium"
                      />
                      <button
                        onClick={submitComment}
                        disabled={!newComment.trim() || submittingComment}
                        className="px-5 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium shadow-sm flex-1 sm:flex-none"
                      >
                        {submittingComment ? 'Posting...' : 'Post Comment'}
                      </button>
                    </div>
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
