import { useState, useEffect } from 'react';
import { Copy, Check, Zap, Database, Wifi, Code, Terminal, Activity, AlertTriangle, Calculator, TrendingUp, Box, ArrowRight, ExternalLink, Home, ChevronRight, BookOpen, Lightbulb, Info } from 'lucide-react';
import { Link } from 'react-router-dom';

interface CodeBlockProps {
  code: string;
  language: string;
}

const CodeBlock = ({ code, language }: CodeBlockProps) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Language-specific syntax highlighting colors
  const getLanguageColor = (lang: string) => {
    const colors: Record<string, string> = {
      bash: 'text-green-400',
      json: 'text-yellow-300',
      python: 'text-blue-400',
      javascript: 'text-yellow-400',
      cpp: 'text-purple-400',
      html: 'text-orange-400',
    };
    return colors[lang] || 'text-gray-100';
  };

  return (
    <div className="relative group">
      <div className="absolute top-2 right-2 z-10 flex items-center gap-2">
        <span className="text-xs text-gray-400 bg-gray-800 px-2 py-1 rounded">{language}</span>
        <button
          onClick={handleCopy}
          className="p-2 bg-gray-700 hover:bg-gray-600 rounded-md transition-colors"
          title="Copy to clipboard"
        >
          {copied ? (
            <Check className="w-4 h-4 text-green-400" />
          ) : (
            <Copy className="w-4 h-4 text-gray-300" />
          )}
        </button>
      </div>
      <pre className="bg-gray-900 text-gray-100 p-4 pt-12 rounded-lg overflow-x-auto border border-gray-700">
        <code className={`language-${language} ${getLanguageColor(language)}`}>{code}</code>
      </pre>
    </div>
  );
};

interface CalloutProps {
  type: 'tip' | 'info' | 'warning';
  title: string;
  children: React.ReactNode;
}

const Callout = ({ type, title, children }: CalloutProps) => {
  const styles = {
    tip: {
      bg: 'bg-cyan-50',
      border: 'border-cyan-500',
      icon: <Lightbulb className="w-5 h-5 text-cyan-600" />,
      titleColor: 'text-cyan-900'
    },
    info: {
      bg: 'bg-blue-50',
      border: 'border-blue-500',
      icon: <Info className="w-5 h-5 text-blue-600" />,
      titleColor: 'text-blue-900'
    },
    warning: {
      bg: 'bg-yellow-50',
      border: 'border-yellow-500',
      icon: <AlertTriangle className="w-5 h-5 text-yellow-600" />,
      titleColor: 'text-yellow-900'
    }
  };

  const style = styles[type];

  return (
    <div className={`${style.bg} border-l-4 ${style.border} p-4 rounded-r-lg my-4`}>
      <div className="flex items-start gap-3">
        {style.icon}
        <div className="flex-1">
          <h4 className={`font-semibold ${style.titleColor} mb-1`}>{title}</h4>
          <div className="text-sm text-gray-700">{children}</div>
        </div>
      </div>
    </div>
  );
};

interface TableOfContentsItem {
  id: string;
  title: string;
  level: number;
}

const HowItWorks = () => {
  const [activeSection, setActiveSection] = useState('');
  const [scrollProgress, setScrollProgress] = useState(0);

  const tableOfContents: TableOfContentsItem[] = [
    { id: 'quick-start', title: 'Quick Start', level: 1 },
    { id: 'core-concepts', title: 'Core Concepts', level: 1 },
    { id: 'telemetry-structure', title: 'Telemetry Data Structure', level: 2 },
    { id: 'device-auth', title: 'Device Authentication', level: 2 },
    { id: 'variables', title: 'Variables', level: 2 },
    { id: 'timestamps', title: 'Timestamps', level: 2 },
    { id: 'context', title: 'Context', level: 2 },
    { id: 'integration-methods', title: 'Integration Methods', level: 1 },
    { id: 'rest-api', title: 'REST API', level: 2 },
    { id: 'mqtt', title: 'MQTT', level: 2 },
    { id: 'websocket', title: 'WebSocket', level: 2 },
    { id: 'integration-wizard', title: 'Integration Wizard', level: 1 },
    { id: 'advanced-features', title: 'Advanced Features', level: 1 },
    { id: 'rules-engine', title: 'Rules Engine', level: 2 },
    { id: 'synthetic-variables', title: 'Synthetic Variables', level: 2 },
    { id: 'analytics', title: 'Analytics', level: 2 },
    { id: 'platform-features', title: 'Platform Features', level: 1 },
    { id: 'architecture', title: 'Architecture', level: 1 },
    { id: 'resources', title: 'Resources', level: 1 },
  ];

  useEffect(() => {
    const handleScroll = () => {
      // Calculate scroll progress
      const windowHeight = window.innerHeight;
      const documentHeight = document.documentElement.scrollHeight;
      const scrollTop = window.scrollY;
      const progress = (scrollTop / (documentHeight - windowHeight)) * 100;
      setScrollProgress(Math.min(progress, 100));

      // Update active section
      const sections = tableOfContents.map(item => item.id);
      for (let i = sections.length - 1; i >= 0; i--) {
        const element = document.getElementById(sections[i]);
        if (element) {
          const rect = element.getBoundingClientRect();
          if (rect.top <= 100) {
            setActiveSection(sections[i]);
            break;
          }
        }
      }
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const scrollToSection = (id: string) => {
    const element = document.getElementById(id);
    if (element) {
      const offset = 80;
      const elementPosition = element.getBoundingClientRect().top;
      const offsetPosition = elementPosition + window.scrollY - offset;
      window.scrollTo({ top: offsetPosition, behavior: 'smooth' });
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Scroll progress bar */}
      <div className="fixed top-0 left-0 w-full h-1 bg-gray-200 z-50">
        <div
          className="h-full bg-gradient-to-r from-cyan-500 to-blue-600 transition-all duration-300"
          style={{ width: `${scrollProgress}%` }}
        />
      </div>

      {/* Breadcrumb */}
      <div className="bg-white border-b border-gray-200 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 py-3">
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <Link to="/" className="hover:text-cyan-600 transition-colors flex items-center gap-1">
              <Home className="w-4 h-4" />
              Home
            </Link>
            <ChevronRight className="w-4 h-4" />
            <Link to="/how-it-works" className="hover:text-cyan-600 transition-colors flex items-center gap-1">
              <BookOpen className="w-4 h-4" />
              Documentation
            </Link>
            <ChevronRight className="w-4 h-4" />
            <span className="text-cyan-600 font-medium">How It Works</span>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-8 flex gap-8">
        {/* Sidebar Table of Contents */}
        <aside className="hidden lg:block w-64 flex-shrink-0">
          <div className="sticky top-20">
            <div className="bg-white rounded-lg border border-gray-200 p-4 shadow-sm">
              <h3 className="text-sm font-semibold text-gray-900 mb-3 uppercase tracking-wider">
                On This Page
              </h3>
              <nav className="space-y-1">
                {tableOfContents.map((item) => (
                  <button
                    key={item.id}
                    onClick={() => scrollToSection(item.id)}
                    className={`
                      w-full text-left text-sm py-1.5 px-2 rounded transition-colors
                      ${item.level === 2 ? 'pl-4' : ''}
                      ${activeSection === item.id
                        ? 'text-cyan-600 bg-cyan-50 font-medium border-l-2 border-cyan-600'
                        : 'text-gray-600 hover:text-cyan-600 hover:bg-gray-50'
                      }
                    `}
                  >
                    {item.title}
                  </button>
                ))}
              </nav>
            </div>
          </div>
        </aside>

        {/* Main Content */}
        <main className="flex-1 max-w-4xl">
          {/* Hero Section */}
          <div className="text-center mb-12">
            <h1 className="text-4xl font-bold text-gray-900 mb-4">
              How Industrial Cloud Works
            </h1>
            <p className="text-xl text-gray-600 mb-8">
              Connect any device, collect telemetry data, and monitor in real-time in under 5 minutes
            </p>

            {/* Data Hierarchy Diagram */}
            <div className="bg-gradient-to-r from-cyan-50 to-blue-50 rounded-lg p-8 mb-8 border border-cyan-100">
              <h3 className="text-lg font-semibold text-gray-900 mb-6">Data Hierarchy</h3>
              <div className="flex items-center justify-center gap-4 flex-wrap text-sm">
                <div className="bg-white px-6 py-3 rounded-lg shadow-sm border border-cyan-200">
                  <Database className="w-5 h-5 text-cyan-600 mx-auto mb-1" />
                  <div className="font-medium">Organization</div>
                </div>
                <ArrowRight className="w-5 h-5 text-gray-400" />
                <div className="bg-white px-6 py-3 rounded-lg shadow-sm border border-cyan-200">
                  <Box className="w-5 h-5 text-cyan-600 mx-auto mb-1" />
                  <div className="font-medium">Devices</div>
                </div>
                <ArrowRight className="w-5 h-5 text-gray-400" />
                <div className="bg-white px-6 py-3 rounded-lg shadow-sm border border-cyan-200">
                  <Activity className="w-5 h-5 text-cyan-600 mx-auto mb-1" />
                  <div className="font-medium">Telemetry Records</div>
                </div>
                <ArrowRight className="w-5 h-5 text-gray-400" />
                <div className="bg-white px-6 py-3 rounded-lg shadow-sm border border-cyan-200 text-left">
                  <Terminal className="w-5 h-5 text-cyan-600 mb-2" />
                  <div className="text-xs text-gray-600">Variables (temperature, voltage)</div>
                  <div className="text-xs text-gray-600">Timestamp</div>
                  <div className="text-xs text-gray-600">Context (metadata)</div>
                </div>
              </div>
            </div>
          </div>

          {/* Quick Start Section */}
          <section id="quick-start" className="mb-12 scroll-mt-24">
            <div className="bg-gradient-to-r from-green-50 to-emerald-50 border-l-4 border-green-500 p-6 rounded-r-lg mb-6">
              <h2 className="text-2xl font-bold text-gray-900 mb-2 flex items-center gap-2">
                <Zap className="w-6 h-6 text-green-600" />
                Send Your First Data Point in 30 Seconds
              </h2>
            </div>

            <p className="text-gray-700 mb-4">
              The simplest way to get started is with a basic HTTP request. Here&apos;s a curl example:
            </p>

            <CodeBlock
              language="bash"
              code={`curl -X POST http://54.149.190.208:8080/api/v1/ingest/my-device-001 \\
  -H "X-API-Key: YOUR_DEVICE_TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{
    "temperature": 22.5,
    "humidity": 65.0,
    "voltage": 220.1
  }'`}
            />

            <Callout type="tip" title="PRO TIP: No Device Token Yet?">
              Use the <Link to="/integration-wizard" className="text-cyan-600 hover:text-cyan-800 font-medium underline">Integration Wizard</Link> to create your device and get a token automatically! It&apos;s the fastest way to get started.
            </Callout>
          </section>

          {/* Core Concepts Section */}
          <section id="core-concepts" className="mb-12 scroll-mt-24">
            <h2 className="text-3xl font-bold text-gray-900 mb-6 border-b-2 border-cyan-500 pb-2">Core Concepts</h2>

            {/* Telemetry Data Structure */}
            <div id="telemetry-structure" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Database className="w-5 h-5 text-cyan-600" />
                Telemetry Data Structure
              </h3>

              <div className="overflow-x-auto bg-white rounded-lg border border-gray-200 shadow-sm">
                <table className="min-w-full">
                  <thead className="bg-gradient-to-r from-cyan-50 to-blue-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Field</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Type</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Required</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Description</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    <tr className="hover:bg-gray-50">
                      <td className="px-6 py-4 text-sm font-mono text-cyan-600">deviceId</td>
                      <td className="px-6 py-4 text-sm text-gray-600">string</td>
                      <td className="px-6 py-4 text-sm text-green-600 font-medium">Yes</td>
                      <td className="px-6 py-4 text-sm text-gray-600">Unique device identifier (URL parameter)</td>
                    </tr>
                    <tr className="hover:bg-gray-50">
                      <td className="px-6 py-4 text-sm font-mono text-cyan-600">variables</td>
                      <td className="px-6 py-4 text-sm text-gray-600">object</td>
                      <td className="px-6 py-4 text-sm text-green-600 font-medium">Yes</td>
                      <td className="px-6 py-4 text-sm text-gray-600">Key-value pairs of sensor readings (temperature, voltage, etc.)</td>
                    </tr>
                    <tr className="hover:bg-gray-50">
                      <td className="px-6 py-4 text-sm font-mono text-cyan-600">_timestamp</td>
                      <td className="px-6 py-4 text-sm text-gray-600">ISO 8601</td>
                      <td className="px-6 py-4 text-sm text-gray-500">No</td>
                      <td className="px-6 py-4 text-sm text-gray-600">Data collection time (defaults to server time if omitted)</td>
                    </tr>
                    <tr className="hover:bg-gray-50">
                      <td className="px-6 py-4 text-sm font-mono text-cyan-600">_context</td>
                      <td className="px-6 py-4 text-sm text-gray-600">object</td>
                      <td className="px-6 py-4 text-sm text-gray-500">No</td>
                      <td className="px-6 py-4 text-sm text-gray-600">Additional metadata (location, firmware version, etc.)</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div className="mt-4">
                <p className="text-sm text-gray-700 mb-3">
                  <strong>Example with all fields:</strong>
                </p>
                <CodeBlock
                  language="json"
                  code={`{
  "temperature": 22.5,
  "humidity": 65.0,
  "voltage": 220.1,
  "_timestamp": "2025-10-24T10:30:00Z",
  "_context": {
    "location": "warehouse-01",
    "firmware": "v2.1.0"
  }
}`}
                />
              </div>
            </div>

            {/* Device Authentication */}
            <div id="device-auth" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4">Device Authentication</h3>
              <div className="grid md:grid-cols-2 gap-4">
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Device Tokens</h4>
                  <p className="text-sm text-gray-600">Each device has a unique API token for authentication</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Token Generation</h4>
                  <p className="text-sm text-gray-600">Automatic via Integration Wizard or manual via REST API</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Header Format</h4>
                  <p className="text-sm text-gray-600 font-mono text-cyan-600">X-API-Key: your-device-token</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Security</h4>
                  <p className="text-sm text-gray-600">Tokens are scoped per device and organization</p>
                </div>
              </div>
            </div>

            {/* Variables */}
            <div id="variables" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4">Variables (Sensor Readings)</h3>
              <ul className="space-y-2 text-gray-700">
                <li className="flex items-start gap-2">
                  <Check className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                  <div>
                    <strong>Native Variables:</strong> Direct sensor readings (temperature, voltage, current)
                  </div>
                </li>
                <li className="flex items-start gap-2">
                  <Check className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                  <div>
                    <strong>Synthetic Variables:</strong> Calculated metrics (power = voltage × current)
                  </div>
                </li>
                <li className="flex items-start gap-2">
                  <Check className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                  <div>
                    <strong>Data Types:</strong> Numeric (float/integer), string, boolean
                  </div>
                </li>
                <li className="flex items-start gap-2">
                  <Check className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                  <div>
                    <strong>Flexible Schema:</strong> Send any variable name/value pair
                  </div>
                </li>
              </ul>
            </div>

            {/* Timestamps */}
            <div id="timestamps" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4">Timestamps</h3>
              <div className="bg-gray-50 rounded-lg p-4 space-y-2 text-sm text-gray-700 border border-gray-200">
                <p><strong>Automatic:</strong> Server assigns current time if not provided</p>
                <p><strong>Custom:</strong> Provide <code className="bg-gray-200 px-2 py-1 rounded text-cyan-600">_timestamp</code> field in ISO 8601 format</p>
                <p><strong>Historical Data:</strong> Backfill data by setting past timestamps</p>
                <p><strong>Time Series:</strong> Optimized PostgreSQL storage with indexing</p>
              </div>
            </div>

            {/* Context */}
            <div id="context" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4">Context (Metadata)</h3>
              <p className="text-gray-700 mb-3">
                Context provides optional additional information about each data point.
              </p>
              <div className="grid md:grid-cols-2 gap-4 mb-4">
                <div>
                  <h4 className="font-semibold text-gray-900 mb-2">Use Cases</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>• Device location</li>
                    <li>• Firmware version</li>
                    <li>• Sensor calibration data</li>
                    <li>• Environmental conditions</li>
                  </ul>
                </div>
                <div>
                  <h4 className="font-semibold text-gray-900 mb-2">Example</h4>
                  <CodeBlock
                    language="json"
                    code={`{
  "location": "room-5",
  "floor": "3"
}`}
                  />
                </div>
              </div>
            </div>
          </section>

          {/* Integration Methods Section */}
          <section id="integration-methods" className="mb-12 scroll-mt-24">
            <h2 className="text-3xl font-bold text-gray-900 mb-6 border-b-2 border-cyan-500 pb-2">Integration Methods</h2>

            {/* REST API */}
            <div id="rest-api" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Code className="w-5 h-5 text-cyan-600" />
                REST API Integration
              </h3>

              {/* Basic curl */}
              <div className="mb-6">
                <h4 className="text-lg font-semibold text-gray-900 mb-3">Basic curl Example</h4>
                <CodeBlock
                  language="bash"
                  code={`curl -X POST http://54.149.190.208:8080/api/v1/ingest/sensor-42 \\
  -H "X-API-Key: abc123token" \\
  -H "Content-Type: application/json" \\
  -d '{"temperature": 25.3, "humidity": 58}'`}
                />
              </div>

              {/* Python SDK */}
              <div className="mb-6">
                <h4 className="text-lg font-semibold text-gray-900 mb-3">Python SDK</h4>
                <p className="text-sm text-gray-700 mb-3">
                  <strong>Installation:</strong>
                </p>
                <CodeBlock
                  language="bash"
                  code="pip install indcloud-sdk"
                />

                <p className="text-sm text-gray-700 mb-3 mt-4">
                  <strong>Basic Usage:</strong>
                </p>
                <CodeBlock
                  language="python"
                  code={`from indcloud import IndCloudClient, ClientConfig

# Configure with retry logic
config = ClientConfig(
    base_url="http://54.149.190.208:8080",
    device_id="sensor-42",
    api_key="abc123token",
    retry_attempts=3,
    retry_delay=1.0
)

client = IndCloudClient(config)

# Send data
data = {"temperature": 25.3, "humidity": 58}
response = client.send_telemetry(data)
print(f"Success: {response}")`}
                />

                <Callout type="info" title="Advanced Features">
                  <p>Configurable retry with exponential backoff, custom error handling, timestamp and context support</p>
                  <a href="https://github.com/indcloud/indcloud/tree/main/indcloud-sdk" target="_blank" rel="noopener noreferrer" className="text-cyan-600 hover:text-cyan-800 font-medium inline-flex items-center gap-1 mt-2">
                    Full Python SDK Documentation <ExternalLink className="w-3 h-3" />
                  </a>
                </Callout>
              </div>

              {/* JavaScript SDK */}
              <div className="mb-6">
                <h4 className="text-lg font-semibold text-gray-900 mb-3">JavaScript/TypeScript SDK</h4>
                <p className="text-sm text-gray-700 mb-3">
                  <strong>Installation:</strong>
                </p>
                <CodeBlock
                  language="bash"
                  code="npm install indcloud-sdk-js"
                />

                <p className="text-sm text-gray-700 mb-3 mt-4">
                  <strong>Node.js Usage:</strong>
                </p>
                <CodeBlock
                  language="javascript"
                  code={`const { IndCloudClient } = require('indcloud-sdk-js');

const client = new IndCloudClient({
  baseUrl: 'http://54.149.190.208:8080',
  deviceId: 'sensor-42',
  apiKey: 'abc123token'
});

// Send telemetry
await client.sendTelemetry({
  temperature: 25.3,
  humidity: 58
});`}
                />

                <p className="text-sm text-gray-700 mb-3 mt-4">
                  <strong>Browser Usage:</strong>
                </p>
                <CodeBlock
                  language="html"
                  code={`<script src="https://unpkg.com/indcloud-sdk-js/dist/umd/indcloud-sdk.min.js"></script>
<script>
  const client = new Industrial CloudSDK.IndCloudClient({
    baseUrl: 'http://54.149.190.208:8080',
    deviceId: 'sensor-42',
    apiKey: 'abc123token'
  });

  client.sendTelemetry({ temperature: 25.3 });
</script>`}
                />

                <Callout type="info" title="Full Documentation">
                  <a href="https://github.com/indcloud/indcloud/tree/main/indcloud-sdk-js" target="_blank" rel="noopener noreferrer" className="text-cyan-600 hover:text-cyan-800 font-medium inline-flex items-center gap-1">
                    Full JavaScript SDK Documentation <ExternalLink className="w-3 h-3" />
                  </a>
                </Callout>
              </div>
            </div>

            {/* MQTT Integration */}
            <div id="mqtt" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Wifi className="w-5 h-5 text-green-600" />
                MQTT Integration
              </h3>

              <div className="mb-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
                <h4 className="font-semibold text-gray-900 mb-2">Topic Structure</h4>
                <code className="text-sm bg-white px-3 py-1 rounded border border-gray-200 text-cyan-600">
                  indcloud/devices/&#123;deviceId&#125;/telemetry
                </code>
              </div>

              {/* Python MQTT */}
              <div className="mb-6">
                <h4 className="text-lg font-semibold text-gray-900 mb-3">Python MQTT Example</h4>
                <CodeBlock
                  language="python"
                  code={`import paho.mqtt.client as mqtt
import json

client = mqtt.Client()
client.username_pw_set("device-id", "your-token")
client.connect("54.149.190.208", 1883)

data = {
    "deviceId": "sensor-42",
    "timestamp": "2025-10-24T10:30:00Z",
    "variables": {
        "temperature": 25.3,
        "humidity": 58
    }
}

client.publish(
    "indcloud/devices/sensor-42/telemetry",
    json.dumps(data)
)`}
                />
              </div>

              {/* ESP32 MQTT */}
              <div className="mb-6">
                <h4 className="text-lg font-semibold text-gray-900 mb-3">ESP32/Arduino MQTT Example</h4>
                <CodeBlock
                  language="cpp"
                  code={`#include <WiFi.h>
#include <PubSubClient.h>

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  client.setServer("54.149.190.208", 1883);
  client.connect("sensor-42", "device-id", "your-token");
}

void loop() {
  String payload = "{\\"temperature\\":25.3,\\"humidity\\":58}";
  client.publish("indcloud/devices/sensor-42/telemetry",
                 payload.c_str());
  delay(60000); // Send every minute
}`}
                />
              </div>

              <Callout type="tip" title="PRO TIP: MQTT for IoT Devices">
                MQTT is ideal for resource-constrained devices like ESP32 and Arduino. It uses less bandwidth and battery power compared to HTTP, making it perfect for embedded systems.
              </Callout>
            </div>

            {/* WebSocket */}
            <div id="websocket" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Activity className="w-5 h-5 text-purple-600" />
                WebSocket Real-Time Subscriptions
              </h3>

              <p className="text-gray-700 mb-4">
                Subscribe to real-time telemetry updates using WebSockets. Perfect for live dashboards and monitoring applications.
              </p>

              <div className="mb-4">
                <h4 className="text-lg font-semibold text-gray-900 mb-3">JavaScript WebSocket Client</h4>
                <CodeBlock
                  language="javascript"
                  code={`const client = new IndCloudClient({
  baseUrl: 'http://54.149.190.208:8080',
  deviceId: 'sensor-42',
  apiKey: 'abc123token'
});

// Subscribe to real-time updates
await client.subscribe('sensor-42', (data) => {
  console.log('New telemetry:', data);
  // { deviceId, timestamp, temperature: 25.3, humidity: 58 }
});

// Unsubscribe when done
await client.unsubscribe('sensor-42');
await client.disconnect();`}
                />
              </div>

              <div className="p-4 bg-purple-50 rounded-lg border border-purple-200">
                <h4 className="font-semibold text-gray-900 mb-2">WebSocket URL</h4>
                <code className="text-sm bg-white px-3 py-1 rounded border border-gray-200 text-cyan-600">
                  ws://54.149.190.208:8080/ws/telemetry
                </code>
                <p className="text-sm text-gray-600 mt-2">
                  Authentication: Provide JWT token via query parameter or headers
                </p>
              </div>
            </div>
          </section>

          {/* Integration Wizard Section */}
          <section id="integration-wizard" className="mb-12 scroll-mt-24">
            <div className="bg-gradient-to-r from-cyan-500 to-blue-600 text-white rounded-lg p-8 shadow-lg">
              <div className="flex items-start gap-4">
                <Zap className="w-12 h-12 flex-shrink-0" />
                <div className="flex-1">
                  <h2 className="text-3xl font-bold mb-3">Integration Wizard (Zero-Config Setup)</h2>
                  <p className="text-blue-100 mb-4 text-lg">
                    The fastest way to connect your first device!
                  </p>

                  <div className="grid md:grid-cols-2 gap-3 mb-6">
                    <div className="flex items-center gap-2">
                      <Check className="w-5 h-5" />
                      <span>5-step visual setup process</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Check className="w-5 h-5" />
                      <span>Automatic device creation</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Check className="w-5 h-5" />
                      <span>Live code generation for 6 platforms</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Check className="w-5 h-5" />
                      <span>Real-time connection testing</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Check className="w-5 h-5" />
                      <span>Copy-to-clipboard & download</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Check className="w-5 h-5" />
                      <span>Works with existing devices</span>
                    </div>
                  </div>

                  <div className="bg-white/10 rounded-lg p-4 mb-4">
                    <p className="text-sm text-blue-100 mb-2">
                      <strong>Supported Platforms:</strong>
                    </p>
                    <p className="text-sm text-blue-50">
                      ESP32/Arduino (.ino), Python (.py), Node.js/JavaScript (.js), Raspberry Pi, cURL/Bash (.sh), Generic HTTP
                    </p>
                  </div>

                  <Link
                    to="/integration-wizard"
                    className="inline-flex items-center gap-2 bg-white text-cyan-600 px-6 py-3 rounded-lg font-semibold hover:bg-blue-50 transition-colors shadow-md"
                  >
                    Try Integration Wizard Now
                    <ArrowRight className="w-5 h-5" />
                  </Link>
                </div>
              </div>
            </div>
          </section>

          {/* Advanced Features Section */}
          <section id="advanced-features" className="mb-12 scroll-mt-24">
            <h2 className="text-3xl font-bold text-gray-900 mb-6 border-b-2 border-cyan-500 pb-2">Advanced Features</h2>

            {/* Rules Engine */}
            <div id="rules-engine" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-yellow-600" />
                Rules Engine & Alerts
              </h3>

              <p className="text-gray-700 mb-4">
                Create conditional monitoring rules to automatically trigger alerts when sensor values exceed thresholds.
              </p>

              <CodeBlock
                language="json"
                code={`{
  "name": "High Temperature Alert",
  "variable": "temperature",
  "operator": "GT",
  "threshold": 30.0,
  "severity": "HIGH",
  "enabled": true
}`}
              />

              <div className="mt-4 grid md:grid-cols-2 gap-4">
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Supported Operators</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">GT</code> (Greater Than)</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">GTE</code> (Greater Than or Equal)</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">LT</code> (Less Than)</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">LTE</code> (Less Than or Equal)</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">EQ</code> (Equal)</li>
                  </ul>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Alert Features</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>• Automatic severity calculation</li>
                    <li>• 5-minute cooldown (prevent spam)</li>
                    <li>• Real-time WebSocket notifications</li>
                    <li>• Alert history and acknowledgment</li>
                  </ul>
                </div>
              </div>
            </div>

            {/* Synthetic Variables */}
            <div id="synthetic-variables" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Calculator className="w-5 h-5 text-green-600" />
                Synthetic Variables (Derived Metrics)
              </h3>

              <p className="text-gray-700 mb-4">
                Calculate new metrics from existing variables using mathematical expressions.
              </p>

              <CodeBlock
                language="json"
                code={`{
  "name": "power",
  "expression": "voltage * current",
  "deviceId": "sensor-42"
}`}
              />

              <div className="mt-4 bg-green-50 rounded-lg p-4 border border-green-200">
                <h4 className="font-semibold text-gray-900 mb-2">Expression Features</h4>
                <ul className="text-sm text-gray-700 space-y-1">
                  <li>• Basic arithmetic: <code className="bg-white px-2 py-0.5 rounded border border-gray-200 text-cyan-600">+</code>, <code className="bg-white px-2 py-0.5 rounded border border-gray-200 text-cyan-600">-</code>, <code className="bg-white px-2 py-0.5 rounded border border-gray-200 text-cyan-600">*</code>, <code className="bg-white px-2 py-0.5 rounded border border-gray-200 text-cyan-600">/</code></li>
                  <li>• Variable references: Use telemetry variable names</li>
                  <li>• Automatic calculation on each data ingestion</li>
                  <li>• Stored alongside native variables</li>
                </ul>

                <h4 className="font-semibold text-gray-900 mb-2 mt-4">Example Use Cases</h4>
                <ul className="text-sm text-gray-700 space-y-1">
                  <li>• Power calculation: <code className="bg-white px-2 py-0.5 rounded border border-gray-200 text-cyan-600">voltage × current</code></li>
                  <li>• Energy consumption: <code className="bg-white px-2 py-0.5 rounded border border-gray-200 text-cyan-600">kwConsumption × duration</code></li>
                  <li>• Efficiency: <code className="bg-white px-2 py-0.5 rounded border border-gray-200 text-cyan-600">output / input × 100</code></li>
                </ul>
              </div>

              <Callout type="tip" title="PRO TIP: Reduce Client Load">
                Use synthetic variables to offload calculations from your IoT devices to the server. This saves battery power and reduces complexity in embedded systems.
              </Callout>
            </div>

            {/* Analytics */}
            <div id="analytics" className="mb-8 scroll-mt-24">
              <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <TrendingUp className="w-5 h-5 text-cyan-600" />
                Analytics & Historical Data
              </h3>

              <p className="text-gray-700 mb-4">
                Query aggregated historical data with flexible time intervals and aggregation types.
              </p>

              <CodeBlock
                language="bash"
                code={`GET /api/v1/analytics/sensor-42?
  variable=temperature&
  interval=HOURLY&
  aggregation=AVG&
  from=2025-10-01T00:00:00Z&
  to=2025-10-24T23:59:59Z`}
              />

              <div className="mt-4 grid md:grid-cols-2 gap-4">
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Aggregation Types</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">AVG</code> - Average value</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">MIN</code> - Minimum value</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">MAX</code> - Maximum value</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">SUM</code> - Sum of values</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">COUNT</code> - Number of data points</li>
                  </ul>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Time Intervals</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">HOURLY</code> - 1-hour buckets</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">DAILY</code> - 1-day buckets</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">WEEKLY</code> - 7-day buckets</li>
                    <li>• <code className="bg-gray-100 px-2 py-0.5 rounded text-cyan-600">MONTHLY</code> - 30-day buckets</li>
                  </ul>
                </div>
              </div>
            </div>
          </section>

          {/* Platform Features Section */}
          <section id="platform-features" className="mb-12 scroll-mt-24">
            <h2 className="text-3xl font-bold text-gray-900 mb-6 border-b-2 border-cyan-500 pb-2">Platform Features</h2>

            <p className="text-gray-700 mb-6">
              Industrial Cloud is more than just data ingestion. Discover our comprehensive suite of enterprise-grade features designed to help you build, manage, and scale your IoT solution.
            </p>

            {/* Device Management */}
            <div className="mb-8">
              <h3 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Box className="w-5 h-5 text-cyan-600" />
                Device Management
              </h3>
              <div className="grid md:grid-cols-2 gap-4">
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Device Health Scoring</h4>
                  <p className="text-sm text-gray-600">Automated health scores (0-100) based on uptime, alerts, data quality, and status. Recalculated every 5 minutes with health categories (EXCELLENT, GOOD, FAIR, POOR, CRITICAL).</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Device Groups & Tags</h4>
                  <p className="text-sm text-gray-600">Organize devices with hierarchical groups and custom tags. Supports bulk operations, filtering, and tag-based queries for efficient fleet management.</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Device Commands (MQTT)</h4>
                  <p className="text-sm text-gray-600">Send commands to devices via MQTT for bidirectional communication. Supports custom command/payload structures with device ownership verification.</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Location Tracking & Geofencing</h4>
                  <p className="text-sm text-gray-600">Real-time GPS tracking with location history. Create circular/polygon geofences with entry/exit alerts for location-based monitoring.</p>
                </div>
              </div>
            </div>

            {/* Data Management */}
            <div className="mb-8">
              <h3 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Database className="w-5 h-5 text-green-600" />
                Data Management
              </h3>
              <div className="grid md:grid-cols-2 gap-4">
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Data Retention Policies</h4>
                  <p className="text-sm text-gray-600">Automated data archival and deletion. Set retention periods, archive to S3/Azure Blob, schedule with cron expressions, and track execution history.</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Import/Export</h4>
                  <p className="text-sm text-gray-600">Bulk import telemetry via CSV, export data to Excel/CSV formats. Time range filtering, device bulk import, and error reporting.</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Variable Management</h4>
                  <p className="text-sm text-gray-600">Define variable metadata, units of measurement, data types, and display configuration. Centralized variable catalog for consistency.</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Events System</h4>
                  <p className="text-sm text-gray-600">Comprehensive event logging with audit trail. Track device creation, telemetry ingestion, alerts, and system events with filtering and search.</p>
                </div>
              </div>
            </div>

            {/* Extensibility */}
            <div className="mb-8">
              <h3 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Code className="w-5 h-5 text-purple-600" />
                Extensibility & Automation
              </h3>

              {/* Serverless Functions - Expanded Section */}
              <div className="mb-6">
                <div className="bg-gradient-to-r from-purple-50 to-indigo-50 rounded-lg p-6 border border-purple-200 mb-4">
                  <h4 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
                    <Zap className="w-5 h-5 text-purple-600" />
                    Serverless Functions
                  </h4>
                  <p className="text-gray-700 mb-4">
                    Extend Industrial Cloud with custom JavaScript code that runs in response to platform events. Write functions that process telemetry data, respond to alerts, run on schedules, or handle webhooks—without managing any infrastructure.
                  </p>

                  <div className="grid md:grid-cols-2 gap-4 mb-4">
                    <div className="bg-white rounded-lg p-4 border border-purple-100">
                      <h5 className="font-semibold text-gray-900 mb-2 text-sm">Event Triggers</h5>
                      <ul className="text-sm text-gray-600 space-y-1">
                        <li>• <strong>Telemetry Ingestion:</strong> Process every data point</li>
                        <li>• <strong>Alert Triggered:</strong> Custom alert handling</li>
                        <li>• <strong>Schedule (Cron):</strong> Periodic execution</li>
                        <li>• <strong>HTTP Webhook:</strong> External API calls</li>
                      </ul>
                    </div>
                    <div className="bg-white rounded-lg p-4 border border-purple-100">
                      <h5 className="font-semibold text-gray-900 mb-2 text-sm">Key Features</h5>
                      <ul className="text-sm text-gray-600 space-y-1">
                        <li>• Pay-per-execution pricing model</li>
                        <li>• Automatic scaling to zero</li>
                        <li>• Input/output tracking</li>
                        <li>• Execution history & logs</li>
                      </ul>
                    </div>
                  </div>

                  <div className="space-y-4">
                    <div>
                      <h5 className="font-semibold text-gray-900 mb-2 text-sm">Example: Temperature Anomaly Detection</h5>
                      <CodeBlock
                        language="javascript"
                        code={`// Triggered on telemetry ingestion
export async function onTelemetry(event) {
  const { deviceId, variables, timestamp } = event.data;

  // Detect temperature anomalies
  if (variables.temperature > 30 || variables.temperature < 10) {
    // Send custom notification
    await fetch('https://api.slack.com/webhook', {
      method: 'POST',
      body: JSON.stringify({
        text: \`⚠️ Anomaly: Device \${deviceId} temperature is \${variables.temperature}°C\`
      })
    });

    // Log to external system
    console.log('Anomaly detected', { deviceId, temperature: variables.temperature });
  }

  return { processed: true, anomalyDetected: variables.temperature > 30 };
}`}
                      />
                    </div>

                    <div>
                      <h5 className="font-semibold text-gray-900 mb-2 text-sm">Example: Scheduled Data Aggregation</h5>
                      <CodeBlock
                        language="javascript"
                        code={`// Triggered daily at midnight (cron: 0 0 * * *)
export async function dailyReport(event) {
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);

  // Fetch analytics data
  const response = await fetch(\`\${process.env.API_URL}/api/v1/analytics/sensor-42?
    variable=temperature&
    aggregation=AVG&
    interval=HOURLY&
    from=\${yesterday.toISOString()}\`);

  const data = await response.json();

  // Send email report
  await sendEmail({
    to: 'admin@company.com',
    subject: 'Daily Temperature Report',
    body: \`Average temperature: \${data.avgTemperature}°C\`
  });

  return { reportGenerated: true, dataPoints: data.length };
}`}
                      />
                    </div>

                    <div>
                      <h5 className="font-semibold text-gray-900 mb-2 text-sm">Example: Alert Response Handler</h5>
                      <CodeBlock
                        language="javascript"
                        code={`// Triggered when an alert fires
export async function onAlert(event) {
  const { alertId, deviceId, severity, message } = event.data;

  // Route alerts based on severity
  if (severity === 'CRITICAL') {
    // Send SMS via Twilio
    await sendSMS({
      to: '+1234567890',
      message: \`CRITICAL ALERT: \${message}\`
    });
  } else {
    // Send email for non-critical
    await sendEmail({
      to: 'support@company.com',
      subject: \`Alert: \${deviceId}\`,
      body: message
    });
  }

  // Create support ticket
  await createTicket({
    title: \`Device \${deviceId} Alert\`,
    description: message,
    priority: severity
  });

  return { notificationsSent: 2, ticketCreated: true };
}`}
                      />
                    </div>
                  </div>

                  <Callout type="tip" title="Common Use Cases">
                    <ul className="text-sm space-y-1">
                      <li>• Data enrichment and transformation</li>
                      <li>• Integration with external APIs (CRM, ERP, notification services)</li>
                      <li>• Custom business logic and validation</li>
                      <li>• Automated reporting and analytics</li>
                      <li>• Multi-system data synchronization</li>
                      <li>• Custom alert routing and escalation</li>
                    </ul>
                  </Callout>
                </div>
              </div>

              {/* Other extensibility features */}
              <div className="grid md:grid-cols-2 gap-4">
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Data Plugins</h4>
                  <p className="text-sm text-gray-600">Extensible plugin system for custom data ingestion. Built-in plugins: HTTP Webhook, LoRaWAN TTN, CSV Import. Create your own plugins with validation framework.</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Scheduled Reports</h4>
                  <p className="text-sm text-gray-600">Automated report generation with cron scheduling. Export to PDF/Excel/CSV, email delivery, execution history, and multiple report types (telemetry, analytics, alerts).</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Scheduled Tasks</h4>
                  <p className="text-sm text-gray-600">Cron-based task scheduling for automated operations. Configure recurring tasks for data processing, cleanup, and reporting.</p>
                </div>
              </div>
            </div>

            {/* Notifications */}
            <div className="mb-8">
              <h3 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-yellow-600" />
                Multi-Channel Notifications
              </h3>
              <div className="grid md:grid-cols-3 gap-4">
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Email Notifications</h4>
                  <p className="text-sm text-gray-600">SMTP-based email alerts with customizable templates</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">SMS (Twilio)</h4>
                  <p className="text-sm text-gray-600">Text message alerts via Twilio integration</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Slack Webhooks</h4>
                  <p className="text-sm text-gray-600">Send alerts to Slack channels</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Microsoft Teams</h4>
                  <p className="text-sm text-gray-600">Teams channel webhook integration</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Custom Webhooks</h4>
                  <p className="text-sm text-gray-600">HTTP webhooks to any endpoint</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Email Templates</h4>
                  <p className="text-sm text-gray-600">Visual email template builder with variable substitution and preview</p>
                </div>
              </div>
              <Callout type="info" title="Notification Preferences">
                Users can configure notification preferences per channel with delivery tracking and notification logs. Set up notification routing based on alert severity and type.
              </Callout>
            </div>

            {/* Dashboards & Visualization */}
            <div className="mb-8">
              <h3 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <TrendingUp className="w-5 h-5 text-cyan-600" />
                Dashboards & Visualization
              </h3>
              <div className="grid md:grid-cols-2 gap-4">
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Dashboard Templates</h4>
                  <p className="text-sm text-gray-600">Pre-built industry-specific dashboard templates. Instant setup from template marketplace with system and user-created templates.</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Dashboard Sharing</h4>
                  <p className="text-sm text-gray-600">Share dashboards with public/private links, permissions management, and embed capability for external sites.</p>
                </div>
              </div>
            </div>

            {/* Developer Tools */}
            <div className="mb-8">
              <h3 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Terminal className="w-5 h-5 text-blue-600" />
                Developer Tools
              </h3>
              <div className="grid md:grid-cols-2 gap-4">
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">API Playground</h4>
                  <p className="text-sm text-gray-600">Interactive API testing tool within the UI. Pre-populated endpoints for devices, telemetry, rules, analytics with request history and code examples.</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Webhook Tester</h4>
                  <p className="text-sm text-gray-600">Test webhook endpoints with configurable HTTP methods, custom headers, request bodies, response capture, and execution history.</p>
                </div>
              </div>
            </div>

            {/* Support & Admin */}
            <div className="mb-8">
              <h3 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Info className="w-5 h-5 text-orange-600" />
                Support & Administration
              </h3>
              <div className="grid md:grid-cols-2 gap-4">
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Support Ticket System</h4>
                  <p className="text-sm text-gray-600">Complete ticketing workflow with user submissions, admin management, status tracking (NEW, IN_PROGRESS, RESOLVED, CLOSED), comments, and screenshot attachments.</p>
                </div>
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <h4 className="font-semibold text-gray-900 mb-2">Canned Responses</h4>
                  <p className="text-sm text-gray-600">Pre-written response templates for support tickets with category-based organization and quick insertion for efficient support.</p>
                </div>
              </div>
            </div>

            <div className="bg-gradient-to-r from-cyan-50 to-blue-50 rounded-lg p-6 border border-cyan-200">
              <h4 className="text-lg font-semibold text-gray-900 mb-2">All Features Included</h4>
              <p className="text-sm text-gray-700">
                Every feature listed above is production-ready and available to all users. Access these capabilities through the main navigation menu, REST API, or integration SDKs. Need help implementing a specific feature? Check out the <Link to="/integration-wizard" className="text-cyan-600 hover:text-cyan-800 font-medium underline">Integration Wizard</Link> or contact support.
              </p>
            </div>
          </section>

          {/* Architecture Overview Section */}
          <section id="architecture" className="mb-12 scroll-mt-24">
            <h2 className="text-3xl font-bold text-gray-900 mb-6 border-b-2 border-cyan-500 pb-2">Architecture Overview</h2>

            {/* Data Flow Diagram */}
            <div className="bg-gradient-to-br from-gray-50 to-cyan-50 rounded-lg p-6 mb-6 border border-cyan-100">
              <h3 className="text-xl font-semibold text-gray-900 mb-4">Data Flow</h3>
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <div className="bg-white px-4 py-2 rounded-lg shadow-sm border border-gray-200 text-sm font-medium">
                    Device
                  </div>
                  <ArrowRight className="w-5 h-5 text-gray-400" />
                  <div className="bg-white px-4 py-2 rounded-lg shadow-sm border border-gray-200 text-sm">
                    REST / MQTT / WebSocket
                  </div>
                  <ArrowRight className="w-5 h-5 text-gray-400" />
                  <div className="bg-cyan-100 px-4 py-2 rounded-lg border border-cyan-300 text-sm font-medium">
                    Ingestion Service
                  </div>
                </div>
                <div className="ml-8 space-y-2 text-sm text-gray-700">
                  <div className="flex items-center gap-2">
                    <div className="w-1 h-1 bg-cyan-400 rounded-full"></div>
                    Database (PostgreSQL) - Time-series storage
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-1 h-1 bg-cyan-400 rounded-full"></div>
                    Rules Engine Evaluation - Check thresholds
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-1 h-1 bg-cyan-400 rounded-full"></div>
                    Synthetic Variable Calculation - Compute derived metrics
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-1 h-1 bg-cyan-400 rounded-full"></div>
                    WebSocket Broadcast - Real-time dashboard updates
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-1 h-1 bg-cyan-400 rounded-full"></div>
                    Prometheus Metrics Export - Monitoring
                  </div>
                </div>
              </div>
            </div>

            {/* Key Components */}
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                <h4 className="font-semibold text-gray-900 mb-2">Backend</h4>
                <p className="text-sm text-gray-600">Spring Boot (Java 17)</p>
              </div>
              <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                <h4 className="font-semibold text-gray-900 mb-2">Database</h4>
                <p className="text-sm text-gray-600">PostgreSQL with time-series optimization</p>
              </div>
              <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                <h4 className="font-semibold text-gray-900 mb-2">Messaging</h4>
                <p className="text-sm text-gray-600">MQTT (Mosquitto broker)</p>
              </div>
              <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                <h4 className="font-semibold text-gray-900 mb-2">Real-time</h4>
                <p className="text-sm text-gray-600">WebSocket connections</p>
              </div>
              <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                <h4 className="font-semibold text-gray-900 mb-2">Monitoring</h4>
                <p className="text-sm text-gray-600">Prometheus + Grafana</p>
              </div>
              <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                <h4 className="font-semibold text-gray-900 mb-2">Frontend</h4>
                <p className="text-sm text-gray-600">React + TypeScript + Vite</p>
              </div>
            </div>
          </section>

          {/* Additional Resources Section */}
          <section id="resources" className="mb-12 scroll-mt-24">
            <h2 className="text-3xl font-bold text-gray-900 mb-6 border-b-2 border-cyan-500 pb-2">Additional Resources</h2>

            <div className="grid md:grid-cols-2 gap-4 mb-6">
              <a
                href="https://github.com/indcloud/indcloud/tree/main/indcloud-sdk"
                target="_blank"
                rel="noopener noreferrer"
                className="bg-white border border-gray-200 rounded-lg p-4 hover:border-cyan-400 hover:shadow-md transition-all group"
              >
                <h4 className="font-semibold text-gray-900 mb-1 group-hover:text-cyan-600 flex items-center gap-2">
                  Python SDK Documentation
                  <ExternalLink className="w-4 h-4" />
                </h4>
                <p className="text-sm text-gray-600">Complete guide for Python integration with code examples</p>
              </a>

              <a
                href="https://github.com/indcloud/indcloud/tree/main/indcloud-sdk-js"
                target="_blank"
                rel="noopener noreferrer"
                className="bg-white border border-gray-200 rounded-lg p-4 hover:border-cyan-400 hover:shadow-md transition-all group"
              >
                <h4 className="font-semibold text-gray-900 mb-1 group-hover:text-cyan-600 flex items-center gap-2">
                  JavaScript SDK Documentation
                  <ExternalLink className="w-4 h-4" />
                </h4>
                <p className="text-sm text-gray-600">Node.js and browser integration guide with TypeScript support</p>
              </a>

              <Link
                to="/integration-wizard"
                className="bg-white border border-gray-200 rounded-lg p-4 hover:border-cyan-400 hover:shadow-md transition-all group"
              >
                <h4 className="font-semibold text-gray-900 mb-1 group-hover:text-cyan-600 flex items-center gap-2">
                  Integration Wizard
                  <Zap className="w-4 h-4" />
                </h4>
                <p className="text-sm text-gray-600">5-step guided setup with automatic code generation</p>
              </Link>

              <a
                href="https://github.com/indcloud/indcloud"
                target="_blank"
                rel="noopener noreferrer"
                className="bg-white border border-gray-200 rounded-lg p-4 hover:border-cyan-400 hover:shadow-md transition-all group"
              >
                <h4 className="font-semibold text-gray-900 mb-1 group-hover:text-cyan-600 flex items-center gap-2">
                  GitHub Repository
                  <ExternalLink className="w-4 h-4" />
                </h4>
                <p className="text-sm text-gray-600">View source code, report issues, and contribute</p>
              </a>
            </div>

            {/* Example Projects */}
            <div className="bg-gradient-to-r from-cyan-50 to-blue-50 rounded-lg p-6 border border-cyan-100">
              <h3 className="text-xl font-semibold text-gray-900 mb-4">Example Projects</h3>
              <div className="grid md:grid-cols-2 gap-3 text-sm text-gray-700">
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-green-600" />
                  <span>Smart meter monitoring with ESP32</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-green-600" />
                  <span>Raspberry Pi temperature logger</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-green-600" />
                  <span>Node.js server monitoring</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-green-600" />
                  <span>Python sensor data collector</span>
                </div>
              </div>
            </div>
          </section>

          {/* CTA Section */}
          <section className="text-center">
            <div className="bg-gradient-to-r from-cyan-500 to-blue-600 text-white rounded-lg p-8 shadow-lg">
              <h2 className="text-2xl font-bold mb-3">Ready to Get Started?</h2>
              <p className="text-blue-100 mb-6">
                Connect your first device in under 5 minutes using our Integration Wizard
              </p>
              <Link
                to="/integration-wizard"
                className="inline-flex items-center gap-2 bg-white text-cyan-600 px-8 py-3 rounded-lg font-semibold hover:bg-blue-50 transition-colors text-lg shadow-md"
              >
                <Zap className="w-5 h-5" />
                Start Integration Wizard
              </Link>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
};

export default HowItWorks;
