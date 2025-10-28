import { IssueStatus } from '../types';

export interface StatusInfo {
  label: string;
  description: string;
  color: string;
  icon: string;
}

export const getStatusInfo = (status: IssueStatus): StatusInfo => {
  switch (status) {
    case 'SUBMITTED':
      return {
        label: 'Submitted',
        description: "We've received your ticket and will respond shortly",
        color: 'bg-blue-100 text-blue-800 border-blue-200',
        icon: '📬'
      };
    case 'IN_REVIEW':
      return {
        label: 'In Review',
        description: 'Our team is actively investigating your issue',
        color: 'bg-yellow-100 text-yellow-800 border-yellow-200',
        icon: '🔍'
      };
    case 'RESOLVED':
      return {
        label: 'Resolved',
        description: 'Solution implemented - please confirm it works for you',
        color: 'bg-green-100 text-green-800 border-green-200',
        icon: '✅'
      };
    case 'CLOSED':
      return {
        label: 'Closed',
        description: 'Issue resolved and confirmed',
        color: 'bg-gray-100 text-gray-800 border-gray-200',
        icon: '🔒'
      };
    default:
      return {
        label: status,
        description: '',
        color: 'bg-gray-100 text-gray-800 border-gray-200',
        icon: '❓'
      };
  }
};

export const getSeverityInfo = (severity: string) => {
  switch (severity) {
    case 'CRITICAL':
      return {
        label: 'Critical',
        color: 'bg-red-100 text-red-800',
        icon: '🔴'
      };
    case 'HIGH':
      return {
        label: 'High',
        color: 'bg-orange-100 text-orange-800',
        icon: '🟠'
      };
    case 'MEDIUM':
      return {
        label: 'Medium',
        color: 'bg-yellow-100 text-yellow-800',
        icon: '🟡'
      };
    case 'LOW':
      return {
        label: 'Low',
        color: 'bg-blue-100 text-blue-800',
        icon: '🟢'
      };
    default:
      return {
        label: severity,
        color: 'bg-gray-100 text-gray-800',
        icon: '⚪'
      };
  }
};
