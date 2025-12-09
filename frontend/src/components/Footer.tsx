import { Bug } from 'lucide-react';

interface FooterProps {
  onReportIssue: () => void;
}

import { Button } from './ui/Button';

export const Footer = ({ onReportIssue }: FooterProps) => {
  return (
    <footer className="border-t border-muted bg-secondary">
      <div className="px-8 py-4">
        <div className="flex items-center justify-between">
          <div className="text-sm text-primary/60">
            <p>© {new Date().getFullYear()} SensorVision. All rights reserved.</p>
          </div>

          <div className="flex items-center space-x-4">
            <Button
              onClick={onReportIssue}
              variant="secondary"
              size="sm"
            >
              <Bug className="mr-2 h-4 w-4" />
              Report Issue
            </Button>
          </div>
        </div>
      </div>
    </footer>
  );
};
