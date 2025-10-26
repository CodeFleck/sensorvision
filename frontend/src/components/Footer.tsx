import { Bug } from 'lucide-react';

interface FooterProps {
  onReportIssue: () => void;
}

export const Footer = ({ onReportIssue }: FooterProps) => {
  return (
    <footer className="bg-white border-t border-gray-200 mt-auto">
      <div className="px-8 py-4">
        <div className="flex items-center justify-between">
          <div className="text-sm text-gray-600">
            <p>© {new Date().getFullYear()} SensorVision. All rights reserved.</p>
          </div>

          <div className="flex items-center space-x-4">
            <button
              onClick={onReportIssue}
              className="flex items-center px-4 py-2 text-sm font-medium text-blue-700 bg-blue-50 hover:bg-blue-100 rounded-md transition-colors"
            >
              <Bug className="mr-2 h-4 w-4" />
              Report Issue
            </button>
          </div>
        </div>
      </div>
    </footer>
  );
};
