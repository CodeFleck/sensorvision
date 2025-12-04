import { useState, useEffect } from 'react';
import { X, Key, Copy, RefreshCw, AlertCircle, Check, Eye, EyeOff } from 'lucide-react';
import { Device, DeviceTokenResponse } from '../types';
import { apiService } from '../services/api';

interface TokenModalProps {
  device: Device;
  isOpen: boolean;
  onClose: () => void;
}

export const TokenModal = ({ device, isOpen, onClose }: TokenModalProps) => {
  const [tokenInfo, setTokenInfo] = useState<DeviceTokenResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [showFullToken, setShowFullToken] = useState(false);
  const [generatedToken, setGeneratedToken] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      fetchTokenInfo();
    }
  }, [isOpen]);

  const fetchTokenInfo = async () => {
    setLoading(true);
    setError(null);
    setGeneratedToken(null);
    try {
      const data = await apiService.getDeviceTokenInfo(device.externalId);
      setTokenInfo(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch token info');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerate = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiService.generateDeviceToken(device.externalId);
      if (response.success && response.token) {
        setGeneratedToken(response.token);
        setTokenInfo(response);
      } else {
        setError(response.message || 'Failed to generate token');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate token');
    } finally {
      setLoading(false);
    }
  };

  const handleRotate = async () => {
    if (!window.confirm('Are you sure? This will invalidate the current token.')) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await apiService.rotateDeviceToken(device.externalId);
      if (response.success && response.token) {
        setGeneratedToken(response.token);
        setTokenInfo(response);
      } else {
        setError(response.message || 'Failed to rotate token');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to rotate token');
    } finally {
      setLoading(false);
    }
  };

  const handleRevoke = async () => {
    if (!window.confirm('Are you sure? This will permanently delete the token.')) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await apiService.revokeDeviceToken(device.externalId);
      if (response.success) {
        setGeneratedToken(null);
        await fetchTokenInfo();
      } else {
        setError(response.message || 'Failed to revoke token');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to revoke token');
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (!isOpen) return null;

  const hasToken = tokenInfo?.maskedToken || tokenInfo?.token || generatedToken;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <div className="flex items-center space-x-3">
            <Key className="h-6 w-6 text-blue-600" />
            <div>
              <h2 className="text-xl font-bold text-gray-900">API Token Management</h2>
              <p className="text-sm text-gray-500 mt-1">{device.name} ({device.externalId})</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-start space-x-3">
              <AlertCircle className="h-5 w-5 text-red-600 mt-0.5" />
              <div className="text-sm text-red-800">{error}</div>
            </div>
          )}

          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="text-gray-500">Loading...</div>
            </div>
          ) : generatedToken ? (
            <div className="space-y-4">
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <div className="flex items-start space-x-3">
                  <Check className="h-5 w-5 text-green-600 mt-0.5" />
                  <div className="flex-1">
                    <h3 className="text-sm font-medium text-green-900">Token Generated Successfully!</h3>
                    <p className="text-sm text-green-700 mt-1">
                      Save this token securely - it won't be shown again!
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Device API Token
                </label>
                <div className="flex items-center space-x-2">
                  <code className="flex-1 bg-white border border-gray-300 rounded px-3 py-2 text-sm font-mono break-all">
                    {showFullToken ? generatedToken : `${generatedToken.substring(0, 8)}...${generatedToken.substring(generatedToken.length - 4)}`}
                  </code>
                  <button
                    onClick={() => setShowFullToken(!showFullToken)}
                    className="p-2 text-gray-600 hover:text-gray-900"
                    title={showFullToken ? "Hide" : "Show"}
                  >
                    {showFullToken ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                  <button
                    onClick={() => copyToClipboard(generatedToken)}
                    className="p-2 text-blue-600 hover:text-blue-900"
                    title="Copy to clipboard"
                  >
                    {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                  </button>
                </div>
                {copied && (
                  <p className="text-xs text-green-600 mt-2">✓ Copied to clipboard!</p>
                )}
              </div>

              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h4 className="text-sm font-medium text-blue-900 mb-2">Usage Example:</h4>
                <code className="block text-xs font-mono text-blue-800 bg-white border border-blue-300 rounded p-3 overflow-x-auto">
                  curl -H "Authorization: Bearer {generatedToken.substring(0, 8)}..." \<br />
                  &nbsp;&nbsp;-X POST http://localhost:8080/api/v1/data/ingest \<br />
                  &nbsp;&nbsp;-d '{`{"deviceId":"${device.externalId}","variables":{"temp":23.5}}`}'
                </code>
              </div>
            </div>
          ) : hasToken ? (
            <div className="space-y-4">
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Current Token (Masked)
                </label>
                <code className="block bg-white border border-gray-300 rounded px-3 py-2 text-sm font-mono">
                  {tokenInfo?.maskedToken}
                </code>
              </div>

              {tokenInfo?.tokenCreatedAt && (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">
                        Created
                      </label>
                      <div className="text-sm text-gray-900">
                        {new Date(tokenInfo.tokenCreatedAt).toLocaleString()}
                      </div>
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">
                        Last Used
                      </label>
                      <div className="text-sm text-gray-900">
                        {tokenInfo.tokenLastUsedAt
                          ? new Date(tokenInfo.tokenLastUsedAt).toLocaleString()
                          : <span className="text-yellow-700 font-medium">Never used</span>
                        }
                      </div>
                    </div>
                  </div>

                  {/* Diagnostic warning when token has never been used */}
                  {!tokenInfo.tokenLastUsedAt && (
                    <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                      <div className="flex items-start space-x-3">
                        <AlertCircle className="h-5 w-5 text-yellow-600 mt-0.5 flex-shrink-0" />
                        <div>
                          <h4 className="text-sm font-medium text-yellow-900">Token Never Used</h4>
                          <p className="text-sm text-yellow-700 mt-1">
                            This token was created but no data has been received. If your device is sending data, check:
                          </p>
                          <ul className="text-sm text-yellow-700 mt-2 space-y-1 list-disc ml-4">
                            <li><strong>MQTT users:</strong> Include <code className="bg-yellow-100 px-1 rounded">apiToken</code> field in JSON payload</li>
                            <li><strong>HTTP users:</strong> Add <code className="bg-yellow-100 px-1 rounded">X-API-Key</code> header to requests</li>
                            <li>Verify the device ID matches exactly (case-sensitive)</li>
                            <li>Check network connectivity to the server</li>
                          </ul>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}

              <div className="flex space-x-3">
                <button
                  onClick={handleRotate}
                  disabled={loading}
                  className="flex-1 flex items-center justify-center space-x-2 bg-orange-600 text-white px-4 py-2 rounded-lg hover:bg-orange-700 transition-colors disabled:opacity-50"
                >
                  <RefreshCw className="h-4 w-4" />
                  <span>Rotate Token</span>
                </button>
                <button
                  onClick={handleRevoke}
                  disabled={loading}
                  className="flex-1 flex items-center justify-center space-x-2 bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50"
                >
                  <X className="h-4 w-4" />
                  <span>Revoke Token</span>
                </button>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <p className="text-sm text-yellow-800">
                  This device doesn't have an API token yet. Generate one to enable programmatic access.
                </p>
              </div>

              <button
                onClick={handleGenerate}
                disabled={loading}
                className="w-full flex items-center justify-center space-x-2 bg-blue-600 text-white px-4 py-3 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
              >
                <Key className="h-5 w-5" />
                <span>Generate API Token</span>
              </button>
            </div>
          )}

          {/* Info Section */}
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
            <h4 className="text-sm font-medium text-gray-900 mb-2">About API Tokens</h4>
            <ul className="text-sm text-gray-600 space-y-1">
              <li>• Use tokens to authenticate API requests without JWT</li>
              <li>• Tokens never expire but can be rotated or revoked</li>
              <li>• Store tokens securely - they grant full device access</li>
              <li>• Perfect for embedded devices and scripts</li>
            </ul>
          </div>
        </div>

        {/* Footer */}
        <div className="flex justify-end p-6 border-t border-gray-200">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
