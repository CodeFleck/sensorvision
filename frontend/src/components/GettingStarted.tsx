import { Link } from 'react-router-dom';
import {
  Cpu,
  Zap,
  Bell,
  BarChart3,
  ArrowRight,
  BookOpen,
  Rocket,
  CheckCircle2
} from 'lucide-react';

interface GettingStartedProps {
  variant?: 'full' | 'compact';
}

/**
 * Getting Started component for onboarding new users.
 * Shows when users have no devices or as a help reference.
 */
export const GettingStarted = ({ variant = 'full' }: GettingStartedProps) => {
  const steps = [
    {
      number: 1,
      title: 'Connect Your Device',
      description: 'Use our Integration Wizard to generate code for ESP32, Arduino, Raspberry Pi, or any platform.',
      icon: Cpu,
      color: 'bg-blue-500',
    },
    {
      number: 2,
      title: 'Send Telemetry Data',
      description: 'Your device sends data via MQTT or REST API. We handle storage, processing, and real-time updates.',
      icon: Zap,
      color: 'bg-green-500',
    },
    {
      number: 3,
      title: 'Set Up Alerts',
      description: 'Create rules to trigger alerts when values exceed thresholds. Get notified via email or SMS.',
      icon: Bell,
      color: 'bg-yellow-500',
    },
    {
      number: 4,
      title: 'Analyze & Visualize',
      description: 'Build custom dashboards, view analytics, and gain insights from your IoT data.',
      icon: BarChart3,
      color: 'bg-purple-500',
    },
  ];

  if (variant === 'compact') {
    return (
      <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-lg border border-blue-100 p-6">
        <div className="flex items-start gap-4">
          <div className="flex-shrink-0">
            <div className="w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center">
              <Rocket className="h-6 w-6 text-white" />
            </div>
          </div>
          <div className="flex-1">
            <h3 className="text-lg font-semibold text-gray-900 mb-1">
              Ready to connect your first device?
            </h3>
            <p className="text-gray-600 text-sm mb-4">
              Our Integration Wizard generates ready-to-use code for your platform in under 5 minutes.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link
                to="/integration-wizard"
                className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium text-sm"
              >
                Start Integration Wizard
                <ArrowRight className="h-4 w-4" />
              </Link>
              <Link
                to="/how-it-works"
                className="inline-flex items-center gap-2 px-4 py-2 bg-white text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-medium text-sm border border-gray-200"
              >
                <BookOpen className="h-4 w-4" />
                Read Documentation
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 px-8 py-10 text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 bg-white/20 rounded-full mb-4">
          <Rocket className="h-8 w-8 text-white" />
        </div>
        <h2 className="text-2xl font-bold text-white mb-2">
          Welcome to Industrial Cloud
        </h2>
        <p className="text-blue-100 max-w-xl mx-auto">
          Your IoT monitoring platform is ready. Connect your first device and start
          collecting real-time data in just a few minutes.
        </p>
      </div>

      {/* Steps */}
      <div className="px-8 py-8">
        <h3 className="text-lg font-semibold text-gray-900 mb-6 text-center">
          Get Started in 4 Simple Steps
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {steps.map((step) => (
            <div key={step.number} className="relative">
              <div className="flex flex-col items-center text-center">
                <div className={`w-14 h-14 ${step.color} rounded-full flex items-center justify-center mb-4 shadow-lg`}>
                  <step.icon className="h-7 w-7 text-white" />
                </div>
                <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1 w-6 h-6 bg-gray-900 text-white rounded-full flex items-center justify-center text-xs font-bold">
                  {step.number}
                </div>
                <h4 className="font-semibold text-gray-900 mb-2">{step.title}</h4>
                <p className="text-sm text-gray-600">{step.description}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* CTA Section */}
      <div className="bg-gray-50 px-8 py-6 border-t border-gray-200">
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link
            to="/integration-wizard"
            className="inline-flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-semibold shadow-md hover:shadow-lg"
          >
            <Cpu className="h-5 w-5" />
            Start Integration Wizard
            <ArrowRight className="h-5 w-5" />
          </Link>
          <Link
            to="/how-it-works"
            className="inline-flex items-center gap-2 px-6 py-3 bg-white text-gray-700 rounded-lg hover:bg-gray-100 transition-colors font-medium border border-gray-300"
          >
            <BookOpen className="h-5 w-5" />
            View Documentation
          </Link>
        </div>

        {/* Quick Features */}
        <div className="mt-6 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-sm text-gray-600">
          <div className="flex items-center gap-1">
            <CheckCircle2 className="h-4 w-4 text-green-500" />
            <span>Free tier available</span>
          </div>
          <div className="flex items-center gap-1">
            <CheckCircle2 className="h-4 w-4 text-green-500" />
            <span>No credit card required</span>
          </div>
          <div className="flex items-center gap-1">
            <CheckCircle2 className="h-4 w-4 text-green-500" />
            <span>5-minute setup</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GettingStarted;
