import { useState, useEffect } from 'react';
import { Plus, Phone, Check, X, RefreshCw, Star, Power, Trash2, Shield } from 'lucide-react';
import { PhoneNumber } from '../types';
import { apiService } from '../services/api';
import toast from 'react-hot-toast';

export const PhoneNumbers = () => {
  const [phoneNumbers, setPhoneNumbers] = useState<PhoneNumber[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [verifyingPhone, setVerifyingPhone] = useState<string | null>(null);
  const [verificationCode, setVerificationCode] = useState('');

  useEffect(() => {
    fetchPhoneNumbers();
  }, []);

  const fetchPhoneNumbers = async () => {
    try {
      setLoading(true);
      const data = await apiService.getPhoneNumbers();
      setPhoneNumbers(data);
    } catch (error) {
      console.error('Failed to fetch phone numbers:', error);
      toast.error('Failed to load phone numbers');
    } finally {
      setLoading(false);
    }
  };

  const handleAddPhone = async (phoneNumber: string, countryCode: string) => {
    try {
      const response = await apiService.addPhoneNumber({ phoneNumber, countryCode });
      toast.success(response.message || 'Phone number added! Check your SMS for verification code.');
      setIsAddModalOpen(false);
      await fetchPhoneNumbers();

      // Auto-open verification modal
      setVerifyingPhone(response.data.id);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to add phone number');
    }
  };

  const handleVerify = async (phoneId: string, code: string) => {
    try {
      const response = await apiService.verifyPhoneNumber(phoneId, { code });
      toast.success(response.message || 'Phone number verified successfully!');
      setVerifyingPhone(null);
      setVerificationCode('');
      await fetchPhoneNumbers();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Verification failed');
    }
  };

  const handleResendCode = async (phoneId: string) => {
    try {
      const response = await apiService.resendVerificationCode(phoneId);
      toast.success(response.message || 'Verification code resent!');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to resend code');
    }
  };

  const handleSetPrimary = async (phoneId: string) => {
    try {
      const response = await apiService.setPrimaryPhone(phoneId);
      toast.success(response.message || 'Primary phone updated');
      await fetchPhoneNumbers();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to set as primary');
    }
  };

  const handleToggleEnabled = async (phoneId: string) => {
    try {
      const response = await apiService.togglePhoneEnabled(phoneId);
      toast.success(response.message || 'Phone status updated');
      await fetchPhoneNumbers();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to toggle phone');
    }
  };

  const handleDelete = async (phoneId: string) => {
    if (!window.confirm('Are you sure you want to remove this phone number?')) return;

    try {
      await apiService.deletePhoneNumber(phoneId);
      toast.success('Phone number removed');
      await fetchPhoneNumbers();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to remove phone number');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading phone numbers...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Phone Numbers</h1>
          <p className="text-gray-600 mt-1">Manage phone numbers for SMS alert notifications</p>
        </div>
        <button
          onClick={() => setIsAddModalOpen(true)}
          className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="h-5 w-5" />
          <span>Add Phone Number</span>
        </button>
      </div>

      {phoneNumbers.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <Phone className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No phone numbers yet</h3>
          <p className="text-gray-600 mb-4">Add a phone number to receive SMS alerts</p>
          <button
            onClick={() => setIsAddModalOpen(true)}
            className="inline-flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="h-5 w-5" />
            <span>Add Your First Phone Number</span>
          </button>
        </div>
      ) : (
        <div className="grid gap-4">
          {phoneNumbers.map((phone) => (
            <div key={phone.id} className="bg-white rounded-lg shadow p-6">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3">
                    <Phone className="h-5 w-5 text-gray-400" />
                    <span className="text-lg font-medium text-gray-900">{phone.phoneNumber}</span>
                    {phone.isPrimary && (
                      <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        <Star className="h-3 w-3 mr-1" />
                        Primary
                      </span>
                    )}
                    {phone.verified ? (
                      <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                        <Shield className="h-3 w-3 mr-1" />
                        Verified
                      </span>
                    ) : (
                      <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                        <Shield className="h-3 w-3 mr-1" />
                        Unverified
                      </span>
                    )}
                    {phone.enabled ? (
                      <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                        Enabled
                      </span>
                    ) : (
                      <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                        Disabled
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-500 mt-1">
                    Country Code: {phone.countryCode} • Added {new Date(phone.createdAt).toLocaleDateString()}
                  </p>
                </div>

                <div className="flex items-center space-x-2">
                  {!phone.verified && (
                    <button
                      onClick={() => setVerifyingPhone(phone.id)}
                      className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                      title="Verify phone number"
                    >
                      <Check className="h-5 w-5" />
                    </button>
                  )}
                  {!phone.isPrimary && phone.verified && (
                    <button
                      onClick={() => handleSetPrimary(phone.id)}
                      className="p-2 text-yellow-600 hover:bg-yellow-50 rounded-lg transition-colors"
                      title="Set as primary"
                    >
                      <Star className="h-5 w-5" />
                    </button>
                  )}
                  <button
                    onClick={() => handleToggleEnabled(phone.id)}
                    className={`p-2 rounded-lg transition-colors ${
                      phone.enabled ? 'text-green-600 hover:bg-green-50' : 'text-gray-600 hover:bg-gray-50'
                    }`}
                    title={phone.enabled ? 'Disable' : 'Enable'}
                  >
                    <Power className="h-5 w-5" />
                  </button>
                  <button
                    onClick={() => handleDelete(phone.id)}
                    className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                    title="Remove phone number"
                  >
                    <Trash2 className="h-5 w-5" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Phone Modal */}
      {isAddModalOpen && (
        <AddPhoneModal
          onClose={() => setIsAddModalOpen(false)}
          onAdd={handleAddPhone}
        />
      )}

      {/* Verify Phone Modal */}
      {verifyingPhone && (
        <VerifyPhoneModal
          phoneId={verifyingPhone}
          phoneNumber={phoneNumbers.find(p => p.id === verifyingPhone)?.phoneNumber || ''}
          onClose={() => {
            setVerifyingPhone(null);
            setVerificationCode('');
          }}
          onVerify={handleVerify}
          onResend={handleResendCode}
          code={verificationCode}
          setCode={setVerificationCode}
        />
      )}
    </div>
  );
};

// Add Phone Modal Component
interface AddPhoneModalProps {
  onClose: () => void;
  onAdd: (phoneNumber: string, countryCode: string) => void;
}

const AddPhoneModal = ({ onClose, onAdd }: AddPhoneModalProps) => {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [countryCode, setCountryCode] = useState('CA');
  const [loading, setLoading] = useState(false);

  // Map country codes to their dial codes
  const getDialCode = (country: string): string => {
    const dialCodes: Record<string, string> = {
      'US': '+1',
      'CA': '+1',
      'GB': '+44',
      'AU': '+61',
    };
    return dialCodes[country] || '+1';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      // Prepend appropriate country dial code
      const dialCode = getDialCode(countryCode).replace('+', '');
      const fullPhoneNumber = `+${dialCode}${phoneNumber}`;
      await onAdd(fullPhoneNumber, countryCode);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">Add Phone Number</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="countryCode" className="block text-sm font-medium text-gray-700 mb-1">
              Country Code
            </label>
            <select
              id="countryCode"
              value={countryCode}
              onChange={(e) => setCountryCode(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="US">United States (+1)</option>
              <option value="CA">Canada (+1)</option>
              <option value="GB">United Kingdom (+44)</option>
              <option value="AU">Australia (+61)</option>
            </select>
          </div>

          <div>
            <label htmlFor="phoneNumber" className="block text-sm font-medium text-gray-700 mb-1">
              Phone Number *
            </label>
            <div className="flex space-x-2">
              <div className="w-20 flex items-center justify-center bg-gray-100 border border-gray-300 rounded-lg px-3 py-2 text-gray-700 font-medium">
                {getDialCode(countryCode)}
              </div>
              <input
                type="tel"
                id="phoneNumber"
                required
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value.replace(/\D/g, ''))}
                placeholder={countryCode === 'US' || countryCode === 'CA' ? '5551234567' : 'Phone number'}
                maxLength={countryCode === 'US' || countryCode === 'CA' ? 10 : 15}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            <p className="mt-1 text-sm text-gray-500">
              {countryCode === 'US' || countryCode === 'CA'
                ? 'Enter 10-digit phone number (area code + number)'
                : 'Enter phone number without country code'}
            </p>
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-800">
              <strong>Note:</strong> A verification code will be sent to this number via SMS. Message and data rates may apply.
            </p>
          </div>

          <div className="flex space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {loading ? 'Adding...' : 'Add Phone Number'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

// Verify Phone Modal Component
interface VerifyPhoneModalProps {
  phoneId: string;
  phoneNumber: string;
  onClose: () => void;
  onVerify: (phoneId: string, code: string) => void;
  onResend: (phoneId: string) => void;
  code: string;
  setCode: (code: string) => void;
}

const VerifyPhoneModal = ({ phoneId, phoneNumber, onClose, onVerify, onResend, code, setCode }: VerifyPhoneModalProps) => {
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await onVerify(phoneId, code);
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setResending(true);
    try {
      await onResend(phoneId);
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">Verify Phone Number</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mb-6">
          <p className="text-gray-700">
            Enter the 6-digit verification code sent to:
          </p>
          <p className="text-lg font-medium text-gray-900 mt-2">{phoneNumber}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="code" className="block text-sm font-medium text-gray-700 mb-1">
              Verification Code *
            </label>
            <input
              type="text"
              id="code"
              required
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
              placeholder="123456"
              className="w-full px-3 py-2 text-center text-2xl tracking-widest border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div className="text-center">
            <button
              type="button"
              onClick={handleResend}
              disabled={resending}
              className="text-sm text-blue-600 hover:text-blue-700 disabled:opacity-50 inline-flex items-center space-x-1"
            >
              <RefreshCw className={`h-4 w-4 ${resending ? 'animate-spin' : ''}`} />
              <span>{resending ? 'Resending...' : 'Resend Code'}</span>
            </button>
          </div>

          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <p className="text-sm text-yellow-800">
              Code expires in 10 minutes. If you don&apos;t receive it, check your signal and try resending.
            </p>
          </div>

          <div className="flex space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || code.length !== 6}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {loading ? 'Verifying...' : 'Verify'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default PhoneNumbers;
