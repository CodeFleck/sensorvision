import React, { useState, useEffect } from 'react';
import { Plus, Trash2, ChevronDown, ChevronRight, Lock, Shield, Eye, EyeOff, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import secretsService, { Secret, CreateSecretRequest } from '../../services/secretsService';

interface SecretsManagerProps {
  functionId: number;
}

const SecretsManager: React.FC<SecretsManagerProps> = ({ functionId }) => {
  const [secrets, setSecrets] = useState<Secret[]>([]);
  const [newSecrets, setNewSecrets] = useState<Array<{ key: string; value: string; showValue: boolean }>>([]);
  const [expanded, setExpanded] = useState(false);
  const [loading, setLoading] = useState(false);
  const [showSecurityInfo, setShowSecurityInfo] = useState(true);

  useEffect(() => {
    if (expanded) {
      loadSecrets();
    }
  }, [expanded, functionId]);

  const loadSecrets = async () => {
    try {
      setLoading(true);
      const data = await secretsService.getSecrets(functionId);
      setSecrets(data);
    } catch (error: any) {
      console.error('Failed to load secrets:', error);
      toast.error('Failed to load secrets');
    } finally {
      setLoading(false);
    }
  };

  const handleAddNewSecret = () => {
    setNewSecrets([...newSecrets, { key: '', value: '', showValue: false }]);
  };

  const handleRemoveNewSecret = (index: number) => {
    setNewSecrets(newSecrets.filter((_, i) => i !== index));
  };

  const handleNewSecretChange = (index: number, field: 'key' | 'value', value: string) => {
    const updated = [...newSecrets];
    updated[index][field] = value;
    setNewSecrets(updated);
  };

  const toggleShowValue = (index: number) => {
    const updated = [...newSecrets];
    updated[index].showValue = !updated[index].showValue;
    setNewSecrets(updated);
  };

  const handleSaveSecret = async (index: number) => {
    const secret = newSecrets[index];

    if (!secret.key.trim()) {
      toast.error('Secret key is required');
      return;
    }

    if (!secret.value.trim()) {
      toast.error('Secret value is required');
      return;
    }

    // Validate key format
    const keyPattern = /^[A-Z][A-Z0-9_]*$/;
    if (!keyPattern.test(secret.key)) {
      toast.error('Secret key must start with uppercase letter and contain only uppercase letters, numbers, and underscores (e.g., API_KEY, DATABASE_URL)');
      return;
    }

    try {
      const request: CreateSecretRequest = {
        secretKey: secret.key,
        secretValue: secret.value
      };

      await secretsService.createSecret(functionId, secret.key, request);
      toast.success(`Secret "${secret.key}" saved securely`);

      // Remove from new secrets list
      setNewSecrets(newSecrets.filter((_, i) => i !== index));

      // Reload secrets list
      await loadSecrets();
    } catch (error: any) {
      console.error('Failed to save secret:', error);
      toast.error(error.response?.data?.message || 'Failed to save secret');
    }
  };

  const handleDeleteSecret = async (secretKey: string) => {
    if (!confirm(`Delete secret "${secretKey}"? This action cannot be undone.`)) {
      return;
    }

    try {
      await secretsService.deleteSecret(functionId, secretKey);
      toast.success(`Secret "${secretKey}" deleted`);
      await loadSecrets();
    } catch (error: any) {
      console.error('Failed to delete secret:', error);
      toast.error('Failed to delete secret');
    }
  };

  return (
    <div className="border border-gray-200 rounded-md">
      <button
        type="button"
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-gray-50"
      >
        <div className="flex items-center gap-2">
          <Lock className="w-4 h-4 text-blue-600" />
          <span className="text-sm font-medium text-gray-700">
            Encrypted Secrets ({secrets.length})
          </span>
        </div>
        {expanded ? (
          <ChevronDown className="w-5 h-5 text-gray-500" />
        ) : (
          <ChevronRight className="w-5 h-5 text-gray-500" />
        )}
      </button>

      {expanded && (
        <div className="px-4 py-3 border-t border-gray-200 space-y-4">
          {/* Security Information Banner */}
          {showSecurityInfo && (
            <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
              <div className="flex items-start gap-3">
                <Shield className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <div className="flex items-center justify-between mb-2">
                    <h4 className="text-sm font-semibold text-blue-900">
                      🔒 End-to-End Encrypted Credential Storage
                    </h4>
                    <button
                      onClick={() => setShowSecurityInfo(false)}
                      className="text-blue-600 hover:text-blue-700 text-xs"
                    >
                      Hide
                    </button>
                  </div>
                  <ul className="text-xs text-blue-800 space-y-1.5">
                    <li className="flex items-start gap-2">
                      <span className="text-blue-600 mt-0.5">•</span>
                      <span><strong>AES-256-GCM encryption:</strong> Military-grade encryption at rest with 96-bit IV and 128-bit authentication tag</span>
                    </li>
                    <li className="flex items-start gap-2">
                      <span className="text-blue-600 mt-0.5">•</span>
                      <span><strong>Secure injection:</strong> Decrypted only at execution time and injected as environment variables</span>
                    </li>
                    <li className="flex items-start gap-2">
                      <span className="text-blue-600 mt-0.5">•</span>
                      <span><strong>Never logged:</strong> Secret values are never logged, returned in API responses, or stored in plaintext</span>
                    </li>
                    <li className="flex items-start gap-2">
                      <span className="text-blue-600 mt-0.5">•</span>
                      <span><strong>Access control:</strong> Only functions within your organization can access these secrets</span>
                    </li>
                    <li className="flex items-start gap-2">
                      <span className="text-blue-600 mt-0.5">•</span>
                      <span><strong>Usage:</strong> Access via <code className="bg-blue-100 px-1 rounded">os.environ.get('YOUR_KEY')</code> in Python or <code className="bg-blue-100 px-1 rounded">process.env.YOUR_KEY</code> in Node.js</span>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          )}

          {/* Existing Secrets List */}
          {loading ? (
            <div className="text-center py-4 text-sm text-gray-500">
              Loading secrets...
            </div>
          ) : secrets.length > 0 ? (
            <div className="space-y-2">
              <p className="text-xs text-gray-600 flex items-center gap-1">
                <AlertCircle className="w-3 h-3" />
                Secret values are encrypted and cannot be viewed after creation
              </p>
              {secrets.map((secret) => (
                <div key={secret.id} className="flex items-center gap-2 p-3 bg-gray-50 rounded-md border border-gray-200">
                  <Lock className="w-4 h-4 text-gray-400" />
                  <div className="flex-1">
                    <div className="font-mono text-sm text-gray-900">{secret.secretKey}</div>
                    <div className="text-xs text-gray-500">
                      Created {new Date(secret.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                  <button
                    onClick={() => handleDeleteSecret(secret.secretKey)}
                    className="p-1 text-red-600 hover:bg-red-50 rounded"
                    title="Delete secret"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-6 text-sm text-gray-500">
              No secrets configured. Add your first secret below.
            </div>
          )}

          {/* New Secrets Form */}
          {newSecrets.length > 0 && (
            <div className="space-y-2 border-t border-gray-200 pt-4">
              <p className="text-xs font-medium text-gray-700 mb-2">New Secrets:</p>
              {newSecrets.map((newSecret, index) => (
                <div key={index} className="space-y-2 p-3 bg-gray-50 rounded-md border border-gray-200">
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={newSecret.key}
                      onChange={(e) => handleNewSecretChange(index, 'key', e.target.value.toUpperCase())}
                      placeholder="API_KEY"
                      className="flex-1 px-3 py-2 text-sm font-mono border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                    />
                    <button
                      onClick={() => handleRemoveNewSecret(index)}
                      className="p-2 text-gray-600 hover:bg-gray-200 rounded"
                      title="Remove"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                  <div className="relative">
                    <input
                      type={newSecret.showValue ? "text" : "password"}
                      value={newSecret.value}
                      onChange={(e) => handleNewSecretChange(index, 'value', e.target.value)}
                      placeholder="Secret value (encrypted after saving)"
                      className="w-full px-3 py-2 pr-10 text-sm font-mono border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                    />
                    <button
                      type="button"
                      onClick={() => toggleShowValue(index)}
                      className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
                    >
                      {newSecret.showValue ? (
                        <EyeOff className="w-4 h-4" />
                      ) : (
                        <Eye className="w-4 h-4" />
                      )}
                    </button>
                  </div>
                  <button
                    onClick={() => handleSaveSecret(index)}
                    className="w-full px-3 py-1.5 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center justify-center gap-2"
                  >
                    <Lock className="w-3 h-3" />
                    Encrypt & Save
                  </button>
                  <p className="text-xs text-gray-500 italic">
                    Key must be uppercase with underscores (e.g., DATABASE_URL, STRIPE_API_KEY)
                  </p>
                </div>
              ))}
            </div>
          )}

          {/* Add Secret Button */}
          <button
            type="button"
            onClick={handleAddNewSecret}
            className="flex items-center gap-2 text-sm text-blue-600 hover:text-blue-700"
          >
            <Plus className="w-4 h-4" />
            Add Secret
          </button>

          {/* Helper Text */}
          <div className="text-xs text-gray-600 bg-gray-50 p-3 rounded-md border border-gray-200">
            <strong>💡 Usage Example:</strong>
            <pre className="mt-2 bg-white p-2 rounded border border-gray-200 overflow-x-auto">
              <code className="text-xs">
{`# Python
import os
api_key = os.environ.get('API_KEY')

# Node.js
const apiKey = process.env.API_KEY;`}
              </code>
            </pre>
          </div>
        </div>
      )}
    </div>
  );
};

export default SecretsManager;
