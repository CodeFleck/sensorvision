import { useState, useEffect } from 'react';
import Joyride, { CallBackProps, STATUS, Step, ACTIONS, EVENTS } from 'react-joyride';
import { useNavigate, useLocation } from 'react-router-dom';

const TOUR_STORAGE_KEY = 'sensorvision_welcome_tour_completed';

interface WelcomeTourProps {
  isNewUser?: boolean;
  forceStart?: boolean;
  onComplete?: () => void;
}

// Define tour steps with navigation targets
const tourSteps: (Step & { navigateTo?: string })[] = [
  {
    target: 'body',
    content: (
      <div className="text-center">
        <h2 className="text-xl font-bold text-gray-900 mb-2">Welcome to SensorVision!</h2>
        <p className="text-gray-600">
          Let us show you around the platform. This quick tour will help you get started
          with monitoring your IoT devices.
        </p>
      </div>
    ),
    placement: 'center',
    disableBeacon: true,
  },
  {
    target: 'a[href="/"]',
    content: (
      <div>
        <h3 className="font-semibold text-gray-900 mb-1">Dashboard</h3>
        <p className="text-sm text-gray-600">
          Your home base for real-time monitoring. See live telemetry data from all your
          connected devices at a glance.
        </p>
      </div>
    ),
    placement: 'right',
    navigateTo: '/',
  },
  {
    target: 'a[href="/integration-wizard"]',
    content: (
      <div>
        <h3 className="font-semibold text-gray-900 mb-1">Integration Wizard</h3>
        <p className="text-sm text-gray-600">
          The fastest way to connect new devices. Get ready-to-use code snippets for
          Python, JavaScript, Arduino, and more.
        </p>
      </div>
    ),
    placement: 'right',
  },
  {
    target: 'a[href="/devices"]',
    content: (
      <div>
        <h3 className="font-semibold text-gray-900 mb-1">Devices</h3>
        <p className="text-sm text-gray-600">
          Manage all your connected devices. View status, configure settings, and
          organize devices into groups and tags.
        </p>
      </div>
    ),
    placement: 'right',
  },
  {
    target: 'a[href="/dashboards"]',
    content: (
      <div>
        <h3 className="font-semibold text-gray-900 mb-1">Widget Dashboards</h3>
        <p className="text-sm text-gray-600">
          Create custom dashboards with charts, gauges, and other widgets to visualize
          your data exactly how you need it.
        </p>
      </div>
    ),
    placement: 'right',
  },
  {
    target: 'a[href="/rules"]',
    content: (
      <div>
        <h3 className="font-semibold text-gray-900 mb-1">Rules & Alerts</h3>
        <p className="text-sm text-gray-600">
          Set up automated monitoring rules. Get notified via email or SMS when sensor
          values exceed thresholds.
        </p>
      </div>
    ),
    placement: 'right',
  },
  {
    target: 'a[href="/analytics"]',
    content: (
      <div>
        <h3 className="font-semibold text-gray-900 mb-1">Analytics</h3>
        <p className="text-sm text-gray-600">
          Analyze historical data with aggregations, trends, and insights. Export data
          for further analysis.
        </p>
      </div>
    ),
    placement: 'right',
  },
  {
    target: 'body',
    content: (
      <div className="text-center">
        <h2 className="text-xl font-bold text-gray-900 mb-2">You&apos;re all set!</h2>
        <p className="text-gray-600 mb-2">
          Start by connecting your first device using the Integration Wizard, or explore
          the dashboard to see sample data.
        </p>
        <p className="text-sm text-gray-500">
          You can restart this tour anytime from the Help menu.
        </p>
      </div>
    ),
    placement: 'center',
    disableBeacon: true,
  },
];

export const WelcomeTour = ({ isNewUser = false, forceStart = false, onComplete }: WelcomeTourProps) => {
  const [run, setRun] = useState(false);
  const [stepIndex, setStepIndex] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    let isMounted = true;

    // Check if tour should start
    if (forceStart) {
      setRun(true);
      setStepIndex(0);
      return () => { isMounted = false; };
    }

    // For new users, check if they've seen the tour
    if (isNewUser) {
      const hasSeenTour = localStorage.getItem(TOUR_STORAGE_KEY);
      if (!hasSeenTour) {
        // Small delay to let the page render first
        const timer = setTimeout(() => {
          if (isMounted) {
            setRun(true);
          }
        }, 500);
        return () => {
          isMounted = false;
          clearTimeout(timer);
        };
      }
    }

    return () => { isMounted = false; };
  }, [isNewUser, forceStart]);

  const handleJoyrideCallback = (data: CallBackProps) => {
    const { action, index, status, type } = data;

    // Handle step navigation
    if (type === EVENTS.STEP_AFTER) {
      const nextStep = tourSteps[index + 1];

      // If next step has a navigation target and we're not there, navigate
      if (nextStep?.navigateTo && location.pathname !== nextStep.navigateTo) {
        navigate(nextStep.navigateTo);
      }

      if (action === ACTIONS.NEXT) {
        setStepIndex(index + 1);
      } else if (action === ACTIONS.PREV) {
        setStepIndex(index - 1);
      }
    }

    // Handle tour completion or skip
    if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status as typeof STATUS.FINISHED | typeof STATUS.SKIPPED)) {
      setRun(false);
      localStorage.setItem(TOUR_STORAGE_KEY, 'true');
      onComplete?.();
    }

    // Handle close button
    if (action === ACTIONS.CLOSE) {
      setRun(false);
      localStorage.setItem(TOUR_STORAGE_KEY, 'true');
      onComplete?.();
    }
  };

  // Don't render if not running
  if (!run) return null;

  return (
    <Joyride
      steps={tourSteps}
      run={run}
      stepIndex={stepIndex}
      continuous
      showProgress
      showSkipButton
      disableOverlayClose
      spotlightClicks={false}
      callback={handleJoyrideCallback}
      styles={{
        options: {
          primaryColor: '#2563eb', // blue-600
          zIndex: 10000,
          arrowColor: '#fff',
          backgroundColor: '#fff',
          textColor: '#1f2937', // gray-800
          overlayColor: 'rgba(0, 0, 0, 0.5)',
        },
        tooltip: {
          borderRadius: '0.5rem',
          padding: '1.25rem',
        },
        tooltipContainer: {
          textAlign: 'left',
        },
        tooltipTitle: {
          fontSize: '1rem',
          fontWeight: 600,
        },
        tooltipContent: {
          padding: '0.5rem 0',
        },
        buttonNext: {
          backgroundColor: '#2563eb',
          borderRadius: '0.375rem',
          padding: '0.5rem 1rem',
          fontSize: '0.875rem',
          fontWeight: 500,
        },
        buttonBack: {
          color: '#6b7280',
          marginRight: '0.5rem',
          fontSize: '0.875rem',
        },
        buttonSkip: {
          color: '#9ca3af',
          fontSize: '0.875rem',
        },
        spotlight: {
          borderRadius: '0.5rem',
        },
        overlay: {
          mixBlendMode: undefined, // Fix for dark overlay
        },
      }}
      locale={{
        back: 'Back',
        close: 'Close',
        last: 'Finish',
        next: 'Next',
        skip: 'Skip Tour',
      }}
    />
  );
};

// Helper function to check if tour has been completed
export const hasTourBeenCompleted = (): boolean => {
  return localStorage.getItem(TOUR_STORAGE_KEY) === 'true';
};

// Helper function to reset tour (for "restart tour" feature)
export const resetTour = (): void => {
  localStorage.removeItem(TOUR_STORAGE_KEY);
};
