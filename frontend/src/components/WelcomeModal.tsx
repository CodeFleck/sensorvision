import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { X, Cpu, Zap, Bell, BarChart3, ArrowRight, Rocket } from 'lucide-react';

const WELCOME_MODAL_KEY = 'sensorvision_welcome_shown';

interface WelcomeModalProps {
  forceShow?: boolean;
}

/**
 * Welcome Modal shown to first-time users.
 * Uses localStorage to track whether it's been shown before.
 */
export const WelcomeModal = ({ forceShow = false }: WelcomeModalProps) => {
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    // Check if user has seen the welcome modal before
    const hasSeenWelcome = localStorage.getItem(WELCOME_MODAL_KEY);
    if (!hasSeenWelcome || forceShow) {
      // Small delay to let the page render first
      const timer = setTimeout(() => setIsOpen(true), 500);
      return () => clearTimeout(timer);
    }
  }, [forceShow]);

  const handleClose = () => {
    localStorage.setItem(WELCOME_MODAL_KEY, 'true');
    setIsOpen(false);
  };

  const handleGetStarted = () => {
    localStorage.setItem(WELCOME_MODAL_KEY, 'true');
    setIsOpen(false);
  };

  if (!isOpen) return null;

  const features = [
    {
      icon: Cpu,
      title: 'Connect Any Device',
      description: 'ESP32, Arduino, Raspberry Pi, or any HTTP/MQTT capable device',
    },
    {
      icon: Zap,
      title: 'Real-time Data',
      description: 'See your telemetry data stream live on customizable dashboards',
    },
    {
      icon: Bell,
      title: 'Smart Alerts',
      description: 'Get notified via email or SMS when values exceed thresholds',
    },
    {
      icon: BarChart3,
      title: 'Powerful Analytics',
      description: 'Aggregate, analyze, and visualize your IoT data over time',
    },
  ];

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/50 backdrop-blur-sm transition-opacity"
        onClick={handleClose}
      />

      {/* Modal */}
      <div className="flex min-h-full items-center justify-center p-4">
        <div className="relative bg-white rounded-2xl shadow-2xl max-w-2xl w-full overflow-hidden transform transition-all">
          {/* Close button */}
          <button
            onClick={handleClose}
            className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors z-10"
          >
            <X className="h-5 w-5" />
          </button>

          {/* Header */}
          <div className="bg-gradient-to-r from-blue-600 to-indigo-600 px-8 py-10 text-center">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-white/20 rounded-full mb-4">
              <Rocket className="h-8 w-8 text-white" />
            </div>
            <h2 className="text-2xl font-bold text-white mb-2">
              Welcome to SensorVision!
            </h2>
            <p className="text-blue-100">
              Your complete IoT monitoring and analytics platform
            </p>
          </div>

          {/* Features Grid */}
          <div className="px-8 py-8">
            <div className="grid grid-cols-2 gap-4">
              {features.map((feature) => (
                <div
                  key={feature.title}
                  className="flex items-start gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors"
                >
                  <div className="flex-shrink-0 w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                    <feature.icon className="h-5 w-5 text-blue-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 text-sm">
                      {feature.title}
                    </h3>
                    <p className="text-xs text-gray-600 mt-0.5">
                      {feature.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* CTA Section */}
          <div className="bg-gray-50 px-8 py-6 border-t border-gray-200">
            <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
              <Link
                to="/integration-wizard"
                onClick={handleGetStarted}
                className="inline-flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-semibold shadow-md hover:shadow-lg w-full sm:w-auto justify-center"
              >
                <Cpu className="h-5 w-5" />
                Connect Your First Device
                <ArrowRight className="h-5 w-5" />
              </Link>
              <button
                onClick={handleClose}
                className="inline-flex items-center gap-2 px-6 py-3 bg-white text-gray-600 rounded-lg hover:bg-gray-100 transition-colors font-medium border border-gray-200 w-full sm:w-auto justify-center"
              >
                I'll explore first
              </button>
            </div>
            <p className="text-center text-xs text-gray-500 mt-4">
              You can always access the Integration Wizard from the sidebar menu
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default WelcomeModal;
