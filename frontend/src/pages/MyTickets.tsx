import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { apiService } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { IssueSubmission, IssueComment, IssueCommentRequest, IssueStatus } from '../types';
import { getStatusInfo, getSeverityInfo } from '../utils/issueStatusHelpers';

export const MyTickets: React.FC = () => {
  const { user } = useAuth();
  const [tickets, setTickets] = useState<IssueSubmission[]>([]);
  const [filteredTickets, setFilteredTickets] = useState<IssueSubmission[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<IssueStatus | 'ALL'>('ALL');
  const [selectedTicket, setSelectedTicket] = useState<IssueSubmission | null>(null);
  const [comments, setComments] = useState<IssueComment[]>([]);
  const [newComment, setNewComment] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);

  useEffect(() => {
    loadTickets();
  }, []);

  useEffect(() => {
    if (statusFilter === 'ALL') {
      setFilteredTickets(tickets);
    } else {
      setFilteredTickets(tickets.filter(ticket => ticket.status === statusFilter));
    }
  }, [statusFilter, tickets]);

  const loadTickets = async () => {
    try {
      setLoading(true);
      const data = await apiService.getUserIssues();
      setTickets(data);
      setFilteredTickets(data);
    } catch (error) {
      toast.error('Failed to load your tickets');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const viewTicketDetails = async (ticket: IssueSubmission) => {
    try {
      const [fullTicket, ticketComments] = await Promise.all([
        apiService.getIssueById(ticket.id),
        apiService.getUserIssueComments(ticket.id),
      ]);
      setSelectedTicket(fullTicket);
      setComments(ticketComments);

      // Mark ticket as viewed (for unread badge tracking)
      try {
        await apiService.markTicketAsViewed(ticket.id);
      } catch (error) {
        console.error('Failed to mark ticket as viewed:', error);
        // Don't show error to user, this is background tracking
      }
    } catch (error) {
      toast.error('Failed to load ticket details');
      console.error(error);
    }
  };

  const closeTicketDetails = () => {
    setSelectedTicket(null);
    setComments([]);
    setNewComment('');
  };

  const submitComment = async () => {
    if (!selectedTicket || !newComment.trim()) return;

    try {
      setSubmittingComment(true);
      const commentRequest: IssueCommentRequest = {
        message: newComment,
      };
      const comment = await apiService.addUserComment(selectedTicket.id, commentRequest);
      setComments([...comments, comment]);
      setNewComment('');
      toast.success('Reply sent successfully');
    } catch (error) {
      toast.error('Failed to send reply');
      console.error(error);
    } finally {
      setSubmittingComment(false);
    }
  };

  // Removed - using getSeverityInfo helper instead

  // Removed - using getStatusInfo helper instead

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
        <div className="text-gray-600">Loading your tickets...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">My Support Tickets</h1>
        <p className="text-gray-600">View and manage your support requests</p>
      </div>

      {/* Filters */}
      <div className="mb-4 flex gap-2">
        <button
          onClick={() => setStatusFilter('ALL')}
          className={`px-4 py-2 rounded-md ${statusFilter === 'ALL' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
        >
          All ({tickets.length})
        </button>
        <button
          onClick={() => setStatusFilter('SUBMITTED')}
          className={`px-4 py-2 rounded-md ${statusFilter === 'SUBMITTED' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
        >
          Submitted ({tickets.filter(t => t.status === 'SUBMITTED').length})
        </button>
        <button
          onClick={() => setStatusFilter('IN_REVIEW')}
          className={`px-4 py-2 rounded-md ${statusFilter === 'IN_REVIEW' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
        >
          In Review ({tickets.filter(t => t.status === 'IN_REVIEW').length})
        </button>
        <button
          onClick={() => setStatusFilter('RESOLVED')}
          className={`px-4 py-2 rounded-md ${statusFilter === 'RESOLVED' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
        >
          Resolved ({tickets.filter(t => t.status === 'RESOLVED').length})
        </button>
        <button
          onClick={() => setStatusFilter('CLOSED')}
          className={`px-4 py-2 rounded-md ${statusFilter === 'CLOSED' ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'}`}
        >
          Closed ({tickets.filter(t => t.status === 'CLOSED').length})
        </button>
      </div>

      {/* Tickets List */}
      {filteredTickets.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <div className="text-gray-500 mb-4">
            {statusFilter === 'ALL'
              ? "You haven't submitted any support tickets yet."
              : `You don't have any ${statusFilter.toLowerCase().replace('_', ' ')} tickets.`}
          </div>
          <p className="text-sm text-gray-400">
            Click the "Report Issue" button in the sidebar to submit a new ticket.
          </p>
        </div>
      ) : (
        <div className="grid gap-4">
          {filteredTickets.map((ticket) => (
            <div
              key={ticket.id}
              className="bg-white rounded-lg shadow hover:shadow-md transition-shadow cursor-pointer"
              onClick={() => viewTicketDetails(ticket)}
            >
              <div className="p-6">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <h3 className="text-lg font-semibold text-gray-800">
                        #{ticket.id} - {ticket.title}
                      </h3>
                    </div>
                    <div className="flex gap-2 mb-3">
                      {(() => {
                        const statusInfo = getStatusInfo(ticket.status);
                        return (
                          <span className={`px-2 py-1 text-xs font-semibold rounded-full ${statusInfo.color}`}>
                            {statusInfo.icon} {statusInfo.label}
                          </span>
                        );
                      })()}
                      {(() => {
                        const severityInfo = getSeverityInfo(ticket.severity);
                        return (
                          <span className={`px-2 py-1 text-xs font-semibold rounded-full ${severityInfo.color}`}>
                            {severityInfo.icon} {severityInfo.label}
                          </span>
                        );
                      })()}
                      <span className="px-2 py-1 text-xs font-semibold rounded-full bg-purple-100 text-purple-800">
                        {getCategoryLabel(ticket.category)}
                      </span>
                    </div>
                    <p className="text-gray-600 text-sm line-clamp-2 mb-2">
                      {ticket.description}
                    </p>
                    <div className="text-xs text-gray-500">
                      Submitted {new Date(ticket.createdAt).toLocaleString()}
                      {ticket.updatedAt !== ticket.createdAt && (
                        <> • Updated {new Date(ticket.updatedAt).toLocaleString()}</>
                      )}
                    </div>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      viewTicketDetails(ticket);
                    }}
                    className="ml-4 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm font-medium"
                  >
                    View Details
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Ticket Detail Modal */}
      {selectedTicket && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
            {/* Header */}
            <div className="p-6 border-b border-gray-200 flex justify-between items-start">
              <div className="flex-1">
                <h2 className="text-2xl font-bold text-gray-800 mb-2">
                  #{selectedTicket.id} - {selectedTicket.title}
                </h2>

                {/* Status with description */}
                {(() => {
                  const statusInfo = getStatusInfo(selectedTicket.status);
                  return (
                    <div className={`mb-3 p-3 rounded-lg border ${statusInfo.color}`}>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-lg">{statusInfo.icon}</span>
                        <span className="font-semibold">{statusInfo.label}</span>
                      </div>
                      <p className="text-sm opacity-90">{statusInfo.description}</p>
                    </div>
                  );
                })()}

                <div className="flex gap-2 mb-2">
                  {(() => {
                    const severityInfo = getSeverityInfo(selectedTicket.severity);
                    return (
                      <span className={`px-2 py-1 text-xs font-semibold rounded-full ${severityInfo.color}`}>
                        {severityInfo.icon} {severityInfo.label}
                      </span>
                    );
                  })()}
                  <span className="px-2 py-1 text-xs font-semibold rounded-full bg-purple-100 text-purple-800">
                    {getCategoryLabel(selectedTicket.category)}
                  </span>
                </div>
                <div className="text-sm text-gray-600">
                  Submitted on {new Date(selectedTicket.createdAt).toLocaleString()}
                </div>
              </div>
              <button onClick={closeTicketDetails} className="text-gray-400 hover:text-gray-600">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto p-6">
              {/* Description */}
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-2">Your Message</h3>
                <div className="bg-gray-50 p-4 rounded-lg whitespace-pre-wrap">{selectedTicket.description}</div>
              </div>

              {/* Conversation */}
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-4">
                  Conversation {comments.length > 0 && `(${comments.length})`}
                </h3>

                {comments.length === 0 ? (
                  <div className="text-center text-gray-500 py-8 bg-gray-50 rounded-lg">
                    <p className="mb-2">No responses yet</p>
                    <p className="text-sm text-gray-400">Our support team will respond shortly</p>
                  </div>
                ) : (
                  <div className="space-y-4 mb-4">
                    {comments.map((comment) => {
                      // Check if this is the current user's comment
                      const isMyComment = user && comment.authorId === user.id;

                      // Display name logic: show "Support Team" for admins, user's name otherwise
                      let displayName = comment.authorName;
                      let displayBadge = null;

                      if (!isMyComment) {
                        // This is from support team
                        displayName = '🎧 Support Team';
                        displayBadge = <span className="ml-2 text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">Staff</span>;
                      }

                      return (
                        <div
                          key={comment.id}
                          className={`p-4 rounded-lg ${isMyComment ? 'bg-blue-50 border border-blue-200' : 'bg-green-50 border border-green-200'}`}
                        >
                          <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center gap-2">
                              <div className="font-semibold text-gray-800">
                                {displayName}
                                {displayBadge}
                                {isMyComment && <span className="ml-2 text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">You</span>}
                              </div>
                            </div>
                            <div className="text-xs text-gray-500">
                              {new Date(comment.createdAt).toLocaleString()}
                            </div>
                          </div>
                          <div className="text-gray-700 whitespace-pre-wrap">{comment.message}</div>
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* Add Reply - only if ticket is not closed */}
                {selectedTicket.status !== 'CLOSED' && (
                  <div className="bg-blue-50 p-4 rounded-lg">
                    <label className="block text-sm font-medium text-gray-700 mb-2">Add Reply</label>
                    <textarea
                      value={newComment}
                      onChange={(e) => setNewComment(e.target.value)}
                      rows={4}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      placeholder="Type your reply to the support team..."
                    />
                    <div className="mt-2 flex justify-end">
                      <button
                        onClick={submitComment}
                        disabled={!newComment.trim() || submittingComment}
                        className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {submittingComment ? 'Sending...' : 'Send Reply'}
                      </button>
                    </div>
                  </div>
                )}

                {selectedTicket.status === 'CLOSED' && (
                  <div className="bg-gray-100 p-4 rounded-lg text-center text-gray-600">
                    This ticket has been closed. If you need further assistance, please submit a new ticket.
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
