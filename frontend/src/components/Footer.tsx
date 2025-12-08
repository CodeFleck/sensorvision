import { Bug } from 'lucide-react';

interface FooterProps {
  onReportIssue: () => void;
}

export const Footer = ({ onReportIssue }: FooterProps) => {
  return (
    <footer className="border-t border-white/5 bg-[#030712]/50">
      <div className="px-8 py-4">
        <div className="flex items-center justify-between">
          <div className="text-sm text-gray-500">
            <p>© {new Date().getFullYear()} SensorVision. All rights reserved.</p>
          </div>

          <div className="flex items-center space-x-4">
            <button
              onClick={onReportIssue}
              className="flex items-center px-4 py-2 text-sm font-medium text-blue-400 bg-blue-500/10 hover:bg-blue-500/20 border border-blue-500/20 hover:border-blue-500/40 rounded-xl transition-all duration-200"
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
