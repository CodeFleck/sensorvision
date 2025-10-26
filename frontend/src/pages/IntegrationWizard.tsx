import React, { useState } from 'react';
import {
  Cpu,
  Code,
  Zap,
  CheckCircle2,
  Copy,
  Download,
  ArrowLeft,
  ArrowRight,
  Sparkles,
  FileCode,
  Terminal,
  Wifi,
  AlertTriangle,
} from 'lucide-react';
import { apiService } from '../services/api';
import { clsx } from 'clsx';
import { config } from '../config';

type Platform = 'esp32' | 'python' | 'nodejs' | 'raspberry-pi' | 'arduino' | 'curl';

interface PlatformOption {
  id: Platform;
  name: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  color: string;
}

// Map platform colors to explicit Tailwind classes (for JIT compilation)
const platformColorClasses: Record<string, string> = {
  blue: 'text-blue-600',
  green: 'text-green-600',
  yellow: 'text-yellow-600',
  red: 'text-red-600',
  purple: 'text-purple-600',
  gray: 'text-gray-600',
};

const platforms: PlatformOption[] = [
  {
    id: 'esp32',
    name: 'ESP32 / Arduino',
    description: 'IoT microcontroller with WiFi',
    icon: Cpu,
    color: 'blue',
  },
  {
    id: 'python',
    name: 'Python',
    description: 'Raspberry Pi, servers, scripts',
    icon: FileCode,
    color: 'green',
  },
  {
    id: 'nodejs',
    name: 'Node.js / JavaScript',
    description: 'Web apps, Node-RED, servers',
    icon: Code,
    color: 'yellow',
  },
  {
    id: 'raspberry-pi',
    name: 'Raspberry Pi',
    description: 'Single-board computer projects',
    icon: Zap,
    color: 'red',
  },
  {
    id: 'arduino',
    name: 'Arduino',
    description: 'Microcontroller projects',
    icon: Cpu,
    color: 'purple',
  },
  {
    id: 'curl',
    name: 'cURL / HTTP',
    description: 'Command line, testing, scripts',
    icon: Terminal,
    color: 'gray',
  },
];

interface ToastMessage {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info';
}

export const IntegrationWizard: React.FC = () => {
  const [currentStep, setCurrentStep] = useState(1);
  const [selectedPlatform, setSelectedPlatform] = useState<Platform | null>(null);
  const [deviceId, setDeviceId] = useState('');
  const [deviceName, setDeviceName] = useState('');
  const [apiToken, setApiToken] = useState('');
  const [useExistingDevice, setUseExistingDevice] = useState(false);
  const [generatedCode, setGeneratedCode] = useState('');
  const [isTestingConnection, setIsTestingConnection] = useState(false);
  const [connectionSuccess, setConnectionSuccess] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [lastSetupDeviceId, setLastSetupDeviceId] = useState(''); // Track which device we set up
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const apiUrl = config.backendUrl;

  const totalSteps = 5;

  const showToast = (message: string, type: 'success' | 'error' | 'info') => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, message, type }]);
    // Auto-remove toast after 3 seconds
    setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 3000);
  };

  const goToNextStep = () => {
    if (currentStep < totalSteps) {
      setCurrentStep(currentStep + 1);
    }
  };

  const goToPreviousStep = () => {
    if (currentStep > 1) {
      // If going back to step 2 (device setup), clear the token to force fresh setup
      if (currentStep === 3) {
        setApiToken('');
        setGeneratedCode('');
      }
      setCurrentStep(currentStep - 1);
    }
  };

  const handlePlatformSelect = (platform: Platform) => {
    setSelectedPlatform(platform);
    goToNextStep();
  };

  const handleDeviceSetup = async () => {
    if (!deviceId || (!useExistingDevice && !deviceName)) return;

    // If we already have a token for THIS SAME device, just proceed to code generation
    // (Prevents re-creating if user clicks Continue multiple times)
    if (apiToken && generatedCode && lastSetupDeviceId === deviceId) {
      goToNextStep();
      return;
    }

    setLoading(true);
    try {
      let tokenResponse;
      let deviceExists = false;

      // First, check if device already exists (regardless of mode)
      try {
        await apiService.getDevice(deviceId);
        deviceExists = true;
      } catch (error) {
        // Device doesn't exist, which is fine if we're creating a new one
        deviceExists = false;
      }

      if (!useExistingDevice) {
        // User wants to create a new device
        if (deviceExists) {
          // Device already exists! Ask user what to do
          const userChoice = confirm(
            `A device with ID "${deviceId}" already exists.\n\n` +
            `Would you like to use this existing device?\n\n` +
            `Click OK to use the existing device, or Cancel to choose a different ID.`
          );

          if (!userChoice) {
            // User chose to cancel - let them pick a different ID
            setLoading(false);
            return;
          }

          // User chose to use existing device - switch to existing device mode
          setUseExistingDevice(true);

          // For existing device, rotate token to get the value
          tokenResponse = await apiService.rotateDeviceToken(deviceId);
        } else {
          // Device doesn't exist - create it
          try {
            await apiService.createDevice({
              externalId: deviceId,
              name: deviceName,
            });

            // IMPORTANT: Backend automatically generates a token during device creation
            // We need to rotate it to get the actual token value (create returns masked token)
            tokenResponse = await apiService.rotateDeviceToken(deviceId);
          } catch (createError) {
            // Ignore "already exists" errors - might happen in race conditions
            const createErrorMsg = createError instanceof Error ? createError.message : '';
            if (!createErrorMsg.includes('already exists') && !createErrorMsg.includes('unique')) {
              // Re-throw if it's a different error
              throw createError;
            }
            // Device was created elsewhere, rotate token to get the value
            console.log('Device was already created, rotating token...');
            tokenResponse = await apiService.rotateDeviceToken(deviceId);
          }
        }
      } else {
        // User wants to use an existing device
        if (!deviceExists) {
          alert(`Device "${deviceId}" does not exist. Please uncheck "Use existing device" to create it, or enter a valid device ID.`);
          setLoading(false);
          return;
        }

        // For existing devices, check if token exists
        try {
          const tokenInfo = await apiService.getDeviceTokenInfo(deviceId);
          // Device has a token - need to rotate to get the actual value
          if (tokenInfo.maskedToken) {
            tokenResponse = await apiService.rotateDeviceToken(deviceId);
          } else {
            // No token yet - generate one
            tokenResponse = await apiService.generateDeviceToken(deviceId);
          }
        } catch (error) {
          // If getDeviceTokenInfo fails, try to generate
          console.error('Error checking token:', error);
          alert('Failed to get device token. Please check your permissions.');
          setLoading(false);
          return;
        }
      }

      if (tokenResponse && tokenResponse.token) {
        setApiToken(tokenResponse.token);
        setLastSetupDeviceId(deviceId); // Remember which device we just set up
        generateCode(selectedPlatform!, deviceId, tokenResponse.token);
        goToNextStep();
      } else {
        alert('Failed to obtain device token. Please try again.');
      }
    } catch (error) {
      console.error('Failed to setup device:', error);

      // Log detailed error information for debugging
      if (error instanceof Error) {
        console.error('Error details:', {
          message: error.message,
          stack: error.stack,
          deviceId,
          useExistingDevice,
        });
      }

      const errorMessage = error instanceof Error ? error.message : 'Unknown error';

      // Provide user-friendly error messages
      if (errorMessage.includes('already exists')) {
        alert(
          `A device with ID "${deviceId}" already exists.\n\n` +
          `Please either:\n` +
          `1. Check "Use existing device" checkbox, or\n` +
          `2. Choose a different device ID`
        );
      } else if (errorMessage.includes('not found') || errorMessage.includes('404')) {
        alert(
          `Device "${deviceId}" was not found.\n\n` +
          `Please either:\n` +
          `1. Uncheck "Use existing device" to create a new device, or\n` +
          `2. Enter the correct device ID`
        );
      } else {
        alert(`Failed to setup device: ${errorMessage}`);
      }
    } finally {
      setLoading(false);
    }
  };

  const generateCode = (platform: Platform, devId: string, token: string) => {
    let code = '';

    switch (platform) {
      case 'esp32':
      case 'arduino':
        code = `#include <WiFi.h>
#include <HTTPClient.h>

// WiFi credentials
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// SensorVision configuration
const char* apiUrl = "${apiUrl}";
const char* apiKey = "${token}";
const char* deviceId = "${devId}";

void setup() {
  Serial.begin(115200);

  // Connect to WiFi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");
}

void loop() {
  // Read sensor data (example)
  float temperature = 23.5;  // Replace with actual sensor reading
  float humidity = 65.2;     // Replace with actual sensor reading

  // Send data to SensorVision
  sendData(temperature, humidity);

  delay(60000); // Send data every minute
}

void sendData(float temperature, float humidity) {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;

    // NOTE: If device ID contains special characters, URL-encode it
    String url = String(apiUrl) + "/api/v1/ingest/" + String(deviceId);
    http.begin(url);
    http.addHeader("X-API-Key", apiKey);
    http.addHeader("Content-Type", "application/json");

    String payload = "{\\"temperature\\":" + String(temperature, 1) +
                     ",\\"humidity\\":" + String(humidity, 1) + "}";

    int httpCode = http.POST(payload);

    if (httpCode == 200 || httpCode == 201) {
      Serial.println("Data sent successfully");
    } else {
      Serial.printf("Error sending data: %d\\n", httpCode);
    }

    http.end();
  }
}`;
        break;

      case 'python':
      case 'raspberry-pi':
        code = `import requests
import time
from urllib.parse import quote

# SensorVision configuration
API_URL = "${apiUrl}"
API_KEY = "${token}"
DEVICE_ID = "${devId}"

def send_data(temperature, humidity):
    """Send sensor data to SensorVision"""
    # URL-encode device ID to handle special characters
    url = f"{API_URL}/api/v1/ingest/{quote(DEVICE_ID)}"
    headers = {
        "X-API-Key": API_KEY,
        "Content-Type": "application/json"
    }
    data = {
        "temperature": temperature,
        "humidity": humidity
    }

    try:
        response = requests.post(url, json=data, headers=headers, timeout=10)
        if response.status_code in (200, 201):
            print("Data sent successfully")
            return True
        else:
            print(f"Error: {response.status_code} - {response.text}")
            return False
    except Exception as e:
        print(f"Failed to send data: {e}")
        return False

if __name__ == "__main__":
    print("Starting SensorVision data collection...")

    while True:
        # Read sensor data (example)
        temperature = 23.5  # Replace with actual sensor reading
        humidity = 65.2     # Replace with actual sensor reading

        # Send to SensorVision
        send_data(temperature, humidity)

        # Wait 60 seconds
        time.sleep(60)`;
        break;

      case 'nodejs':
        code = `const axios = require('axios');

// SensorVision configuration
const API_URL = '${apiUrl}';
const API_KEY = '${token}';
const DEVICE_ID = '${devId}';

async function sendData(temperature, humidity) {
  try {
    // URL-encode device ID to handle special characters
    const response = await axios.post(
      \`\${API_URL}/api/v1/ingest/\${encodeURIComponent(DEVICE_ID)}\`,
      {
        temperature,
        humidity
      },
      {
        headers: {
          'X-API-Key': API_KEY,
          'Content-Type': 'application/json'
        },
        timeout: 10000
      }
    );

    console.log('Data sent successfully:', response.data);
    return true;
  } catch (error) {
    console.error('Failed to send data:', error.message);
    return false;
  }
}

// Main loop
async function main() {
  console.log('Starting SensorVision data collection...');

  setInterval(async () => {
    // Read sensor data (example)
    const temperature = 23.5;  // Replace with actual sensor reading
    const humidity = 65.2;     // Replace with actual sensor reading

    await sendData(temperature, humidity);
  }, 60000); // Send data every minute
}

main();`;
        break;

      case 'curl':
        code = `#!/bin/bash

# SensorVision configuration
API_URL="${apiUrl}"
API_KEY="${token}"
DEVICE_ID="${devId}"

# Example sensor data
TEMPERATURE=23.5
HUMIDITY=65.2

# URL-encode device ID (bash handles special chars in variables when quoted)
# Send data to SensorVision
curl -X POST "$API_URL/api/v1/ingest/$DEVICE_ID" \\
  -H "X-API-Key: $API_KEY" \\
  -H "Content-Type: application/json" \\
  -d "{
    \\"temperature\\": $TEMPERATURE,
    \\"humidity\\": $HUMIDITY
  }"

# Example: Continuous monitoring (uncomment to use)
# while true; do
#   TEMPERATURE=$(echo "scale=1; 20 + $RANDOM % 10" | bc)  # Random temp 20-30
#   HUMIDITY=$(echo "scale=1; 50 + $RANDOM % 30" | bc)      # Random humidity 50-80
#
#   curl -X POST "$API_URL/api/v1/ingest/$DEVICE_ID" \\
#     -H "X-API-Key: $API_KEY" \\
#     -H "Content-Type: "application/json" \\
#     -d "{\\"temperature\\": $TEMPERATURE, \\"humidity\\": $HUMIDITY}"
#
#   sleep 60  # Wait 60 seconds
# done`;
        break;
    }

    setGeneratedCode(code);
  };

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(generatedCode);
      showToast('Code copied to clipboard!', 'success');
    } catch (err) {
      console.error('Failed to copy:', err);
      showToast('Failed to copy code. Please try again.', 'error');
    }
  };

  // Sanitize filename to remove invalid characters for Windows/macOS/Linux
  const sanitizeFilename = (name: string): string => {
    // Remove or replace characters that are invalid in filenames
    // Invalid chars: < > : " / \ | ? * and control characters (0-31)
    return name
      .replace(/[<>:"/\\|?*\x00-\x1F]/g, '_') // Replace invalid chars with underscore
      .replace(/\s+/g, '_') // Replace spaces with underscore
      .replace(/_{2,}/g, '_') // Replace multiple underscores with single
      .replace(/^_|_$/g, ''); // Remove leading/trailing underscores
  };

  const downloadCode = () => {
    const extensions: Record<Platform, string> = {
      'esp32': '.ino',
      'arduino': '.ino',
      'python': '.py',
      'raspberry-pi': '.py',
      'nodejs': '.js',
      'curl': '.sh',
    };

    const safeDeviceId = sanitizeFilename(deviceId);
    const filename = `sensorvision-${safeDeviceId}${extensions[selectedPlatform!]}`;
    const blob = new Blob([generatedCode], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  };

  const testConnection = async () => {
    setIsTestingConnection(true);
    setConnectionError(null);
    setConnectionSuccess(false);

    try {
      // Send test data (URL-encode device ID to handle special characters)
      const response = await fetch(`${apiUrl}/api/v1/ingest/${encodeURIComponent(deviceId)}`, {
        method: 'POST',
        headers: {
          'X-API-Key': apiToken,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          temperature: 23.5,
          humidity: 65.2,
          test: true,
        }),
      });

      if (response.ok) {
        setConnectionSuccess(true);
        goToNextStep();
      } else {
        const error = await response.text();
        setConnectionError(`Connection failed: ${error}`);
      }
    } catch (error) {
      setConnectionError(`Connection failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      setIsTestingConnection(false);
    }
  };

  const renderStep = () => {
    switch (currentStep) {
      case 1:
        return (
          <div>
            <div className="text-center mb-8">
              <Sparkles className="h-16 w-16 text-blue-600 mx-auto mb-4" />
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                Choose Your Platform
              </h2>
              <p className="text-gray-600">
                Select the platform you'll use to send sensor data
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {platforms.map((platform) => {
                const Icon = platform.icon;
                return (
                  <button
                    key={platform.id}
                    onClick={() => handlePlatformSelect(platform.id)}
                    className={clsx(
                      'p-6 rounded-lg border-2 transition-all hover:shadow-lg',
                      'text-left hover:border-blue-500'
                    )}
                  >
                    <Icon className={clsx('h-12 w-12 mb-3', platformColorClasses[platform.color])} />
                    <h3 className="text-lg font-semibold text-gray-900 mb-1">
                      {platform.name}
                    </h3>
                    <p className="text-sm text-gray-600">{platform.description}</p>
                  </button>
                );
              })}
            </div>
          </div>
        );

      case 2:
        return (
          <div className="max-w-2xl mx-auto">
            <div className="text-center mb-8">
              <Cpu className="h-16 w-16 text-blue-600 mx-auto mb-4" />
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                Device Setup
              </h2>
              <p className="text-gray-600">
                Create a new device or use an existing one
              </p>
            </div>

            <div className="bg-white rounded-lg shadow-md p-6">
              <div className="mb-6">
                <label className="flex items-center space-x-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={useExistingDevice}
                    onChange={(e) => setUseExistingDevice(e.target.checked)}
                    className="h-4 w-4 text-blue-600 rounded"
                  />
                  <span className="text-sm text-gray-700">
                    Use existing device (I already have a device ID)
                  </span>
                </label>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Device ID
                  </label>
                  <input
                    type="text"
                    value={deviceId}
                    onChange={(e) => setDeviceId(e.target.value)}
                    placeholder="e.g., sensor-001, weather-station-01"
                    className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    Unique identifier for your device
                  </p>
                </div>

                {!useExistingDevice && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Device Name
                    </label>
                    <input
                      type="text"
                      value={deviceName}
                      onChange={(e) => setDeviceName(e.target.value)}
                      placeholder="e.g., Living Room Sensor, Outdoor Weather Station"
                      className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      required
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      Friendly name for display
                    </p>
                  </div>
                )}

                <button
                  onClick={handleDeviceSetup}
                  disabled={loading || !deviceId || (!useExistingDevice && !deviceName)}
                  className="w-full bg-blue-600 text-white py-3 rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed font-medium transition-colors"
                >
                  {loading ? 'Setting up...' : 'Continue'}
                </button>
              </div>
            </div>
          </div>
        );

      case 3:
        return (
          <div className="max-w-4xl mx-auto">
            <div className="text-center mb-8">
              <FileCode className="h-16 w-16 text-blue-600 mx-auto mb-4" />
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                Copy Your Code
              </h2>
              <p className="text-gray-600">
                Ready-to-use code for {platforms.find((p) => p.id === selectedPlatform)?.name}
              </p>
            </div>

            <div className="bg-gray-900 rounded-lg overflow-hidden">
              <div className="bg-gray-800 px-4 py-3 flex items-center justify-between">
                <span className="text-gray-300 text-sm font-mono">
                  {selectedPlatform === 'esp32' || selectedPlatform === 'arduino'
                    ? `${deviceId}.ino`
                    : selectedPlatform === 'python' || selectedPlatform === 'raspberry-pi'
                    ? `${deviceId}.py`
                    : selectedPlatform === 'nodejs'
                    ? `${deviceId}.js`
                    : `${deviceId}.sh`}
                </span>
                <div className="flex space-x-2">
                  <button
                    onClick={copyToClipboard}
                    className="px-3 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 flex items-center space-x-1"
                  >
                    <Copy className="h-4 w-4" />
                    <span>Copy</span>
                  </button>
                  <button
                    onClick={downloadCode}
                    className="px-3 py-1.5 bg-green-600 text-white rounded text-sm hover:bg-green-700 flex items-center space-x-1"
                  >
                    <Download className="h-4 w-4" />
                    <span>Download</span>
                  </button>
                </div>
              </div>
              <pre className="p-4 overflow-x-auto text-sm text-gray-100 max-h-96">
                <code>{generatedCode}</code>
              </pre>
            </div>

            <div className="mt-8 flex justify-end">
              <button
                onClick={goToNextStep}
                className="px-6 py-3 bg-blue-600 text-white rounded-md hover:bg-blue-700 font-medium flex items-center space-x-2"
              >
                <span>Continue to Testing</span>
                <ArrowRight className="h-5 w-5" />
              </button>
            </div>
          </div>
        );

      case 4:
        return (
          <div className="max-w-2xl mx-auto">
            <div className="text-center mb-8">
              <Wifi className="h-16 w-16 text-blue-600 mx-auto mb-4" />
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                Test Connection
              </h2>
              <p className="text-gray-600">
                Verify that your device can send data to SensorVision
              </p>
            </div>

            <div className="bg-white rounded-lg shadow-md p-6">
              {!connectionSuccess && !connectionError && (
                <div className="text-center">
                  <p className="text-gray-700 mb-6">
                    Click the button below to send test data and verify your connection
                  </p>
                  <button
                    onClick={testConnection}
                    disabled={isTestingConnection}
                    className="px-8 py-3 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 font-medium transition-colors"
                  >
                    {isTestingConnection ? 'Testing...' : 'Test Connection'}
                  </button>
                </div>
              )}

              {connectionSuccess && (
                <div className="text-center">
                  <CheckCircle2 className="h-16 w-16 text-green-600 mx-auto mb-4" />
                  <h3 className="text-xl font-semibold text-gray-900 mb-2">
                    Connection Successful!
                  </h3>
                  <p className="text-gray-600 mb-2">
                    Test data received successfully
                  </p>
                  <div className="bg-green-50 border border-green-200 rounded-lg p-4 mt-4">
                    <p className="text-sm text-gray-700">
                      <strong>Device ID:</strong> {deviceId}
                    </p>
                    <p className="text-sm text-gray-700 mt-1">
                      <strong>Test Data:</strong> Temperature: 23.5°C, Humidity: 65.2%
                    </p>
                  </div>
                </div>
              )}

              {connectionError && (
                <div className="text-center">
                  <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
                    <p className="text-red-700">{connectionError}</p>
                  </div>
                  <button
                    onClick={testConnection}
                    className="px-8 py-3 bg-blue-600 text-white rounded-md hover:bg-blue-700 font-medium"
                  >
                    Try Again
                  </button>
                </div>
              )}
            </div>
          </div>
        );

      case 5:
        return (
          <div className="max-w-2xl mx-auto">
            <div className="text-center mb-8">
              <CheckCircle2 className="h-20 w-20 text-green-600 mx-auto mb-4" />
              <h2 className="text-3xl font-bold text-gray-900 mb-2">
                You're All Set!
              </h2>
              <p className="text-gray-600">
                Your device is ready to send data to SensorVision
              </p>
            </div>

            <div className="bg-white rounded-lg shadow-md p-6 space-y-6">
              <div className="flex items-start space-x-3">
                <CheckCircle2 className="h-6 w-6 text-green-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h3 className="font-semibold text-gray-900">Device Connected</h3>
                  <p className="text-sm text-gray-600">
                    Your device <strong>{deviceId}</strong> is successfully configured
                  </p>
                </div>
              </div>

              <div className="flex items-start space-x-3">
                <CheckCircle2 className="h-6 w-6 text-green-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h3 className="font-semibold text-gray-900">Code Ready</h3>
                  <p className="text-sm text-gray-600">
                    Upload the code to your device and start sending data
                  </p>
                </div>
              </div>

              <div className="flex items-start space-x-3">
                <CheckCircle2 className="h-6 w-6 text-green-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h3 className="font-semibold text-gray-900">Connection Tested</h3>
                  <p className="text-sm text-gray-600">
                    Successfully verified connection to SensorVision
                  </p>
                </div>
              </div>

              <div className="border-t pt-6">
                <h3 className="font-semibold text-gray-900 mb-3">Next Steps:</h3>
                <ul className="space-y-2 text-sm text-gray-700">
                  <li className="flex items-start">
                    <span className="mr-2">1.</span>
                    <span>View your device data on the <a href="/" className="text-blue-600 hover:underline">Dashboard</a></span>
                  </li>
                  <li className="flex items-start">
                    <span className="mr-2">2.</span>
                    <span>Set up <a href="/rules" className="text-blue-600 hover:underline">Alerts & Rules</a> for monitoring</span>
                  </li>
                  <li className="flex items-start">
                    <span className="mr-2">3.</span>
                    <span>Check <a href="/analytics" className="text-blue-600 hover:underline">Analytics</a> for historical data</span>
                  </li>
                </ul>
              </div>

              <div className="flex space-x-4 pt-4">
                <a
                  href="/"
                  className="flex-1 px-6 py-3 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-center font-medium"
                >
                  Go to Dashboard
                </a>
                <button
                  onClick={() => {
                    setCurrentStep(1);
                    setSelectedPlatform(null);
                    setDeviceId('');
                    setDeviceName('');
                    setApiToken('');
                    setConnectionSuccess(false);
                  }}
                  className="flex-1 px-6 py-3 border-2 border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 text-center font-medium"
                >
                  Add Another Device
                </button>
              </div>
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-6xl">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">
            Integration Wizard
          </h1>
          <p className="text-gray-600">
            Get your device connected in less than 5 minutes
          </p>
        </div>

        {/* Progress Bar */}
        <div className="mb-12">
          <div className="flex items-center justify-center">
            {[1, 2, 3, 4, 5].map((step) => (
              <React.Fragment key={step}>
                <div
                  className={clsx(
                    'flex items-center justify-center w-10 h-10 rounded-full font-semibold',
                    step < currentStep
                      ? 'bg-green-600 text-white'
                      : step === currentStep
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-200 text-gray-500'
                  )}
                >
                  {step < currentStep ? <CheckCircle2 className="h-6 w-6" /> : step}
                </div>
                {step < 5 && (
                  <div
                    className={clsx(
                      'w-16 h-1 mx-2',
                      step < currentStep ? 'bg-green-600' : 'bg-gray-200'
                    )}
                  />
                )}
              </React.Fragment>
            ))}
          </div>
          <div className="flex justify-between mt-4 text-sm text-gray-600 max-w-2xl mx-auto">
            <span>Platform</span>
            <span>Device</span>
            <span>Code</span>
            <span>Test</span>
            <span>Done</span>
          </div>
        </div>

        {/* Step Content */}
        <div className="mb-8">{renderStep()}</div>

        {/* Navigation Buttons */}
        {currentStep > 1 && currentStep < 5 && (
          <div className="flex justify-between max-w-2xl mx-auto mt-8">
            <button
              onClick={goToPreviousStep}
              className="px-6 py-2 border-2 border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 font-medium flex items-center space-x-2"
            >
              <ArrowLeft className="h-5 w-5" />
              <span>Back</span>
            </button>
          </div>
        )}
      </div>

      {/* Toast Notifications */}
      <div className="fixed bottom-4 right-4 z-50 space-y-2">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={clsx(
              'px-4 py-3 rounded-lg shadow-lg flex items-center gap-2 min-w-[300px]',
              'transform transition-all duration-300 ease-in-out',
              'animate-in slide-in-from-right',
              {
                'bg-green-500 text-white': toast.type === 'success',
                'bg-red-500 text-white': toast.type === 'error',
                'bg-blue-500 text-white': toast.type === 'info',
              }
            )}
          >
            {toast.type === 'success' && <CheckCircle2 className="w-5 h-5" />}
            {toast.type === 'error' && <AlertTriangle className="w-5 h-5" />}
            {toast.type === 'info' && <Sparkles className="w-5 h-5" />}
            <span className="flex-1">{toast.message}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default IntegrationWizard;
