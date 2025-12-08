import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Activity, ArrowRight, Building2, User, Mail, Lock, Check } from 'lucide-react';

export const Register: React.FC = () => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
    organizationName: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [focusedField, setFocusedField] = useState<string | null>(null);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }

    setLoading(true);

    try {
      await register({
        username: formData.username,
        email: formData.email,
        password: formData.password,
        firstName: formData.firstName,
        lastName: formData.lastName,
        organizationName: formData.organizationName || undefined,
      });
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  const passwordsMatch = formData.password && formData.confirmPassword && formData.password === formData.confirmPassword;

  return (
    <div className="min-h-screen relative overflow-hidden bg-[#030712]">
      {/* Animated gradient background */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-1/2 -right-1/2 w-full h-full bg-gradient-to-bl from-emerald-600/20 via-transparent to-transparent rounded-full blur-3xl animate-pulse-slow" />
        <div className="absolute -bottom-1/2 -left-1/2 w-full h-full bg-gradient-to-tr from-blue-600/15 via-transparent to-transparent rounded-full blur-3xl animate-pulse-slow" style={{ animationDelay: '2s' }} />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-gradient-to-bl from-violet-600/10 to-transparent rounded-full blur-3xl animate-pulse-slow" style={{ animationDelay: '4s' }} />

        {/* Grid pattern overlay */}
        <div
          className="absolute inset-0 opacity-[0.02]"
          style={{
            backgroundImage: `linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px),
                              linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px)`,
            backgroundSize: '64px 64px'
          }}
        />
      </div>

      <div className="relative min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
        <div className="w-full max-w-lg">
          {/* Logo */}
          <div className="flex items-center justify-center gap-3 mb-8">
            <div className="relative">
              <div className="absolute inset-0 bg-blue-500/30 blur-xl rounded-full" />
              <Activity className="relative h-10 w-10 text-blue-400" />
            </div>
            <span className="text-2xl font-bold text-white tracking-tight">SensorVision</span>
          </div>

          {/* Glass card */}
          <div className="relative">
            {/* Glow effect behind card */}
            <div className="absolute -inset-1 bg-gradient-to-r from-emerald-600/20 via-blue-600/20 to-emerald-600/20 rounded-2xl blur-xl opacity-50" />

            <div className="relative bg-white/[0.03] backdrop-blur-xl border border-white/10 rounded-2xl p-8 shadow-2xl">
              <div className="mb-6">
                <h2 className="text-2xl font-bold text-white mb-2">Create your account</h2>
                <p className="text-gray-400">
                  Start monitoring your IoT devices in minutes
                </p>
              </div>

              {error && (
                <div
                  role="alert"
                  aria-live="assertive"
                  className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/20 backdrop-blur-sm"
                >
                  <p className="text-sm text-red-400">{error}</p>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                {/* Name fields */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="relative">
                    <label
                      htmlFor="firstName"
                      className={`absolute left-4 transition-all duration-200 pointer-events-none ${
                        focusedField === 'firstName' || formData.firstName
                          ? '-top-2.5 text-xs text-emerald-400 bg-[#030712] px-2'
                          : 'top-3 text-gray-500'
                      }`}
                    >
                      First Name
                    </label>
                    <div className="absolute left-3 top-3 text-gray-500">
                      <User className="h-4 w-4" />
                    </div>
                    <input
                      id="firstName"
                      name="firstName"
                      type="text"
                      required
                      value={formData.firstName}
                      onChange={handleChange}
                      onFocus={() => setFocusedField('firstName')}
                      onBlur={() => setFocusedField(null)}
                      className="w-full pl-10 pr-4 py-3 bg-white/[0.03] border border-white/10 rounded-xl text-white placeholder-transparent focus:outline-none focus:border-emerald-500/50 focus:ring-2 focus:ring-emerald-500/20 transition-all duration-200"
                    />
                  </div>
                  <div className="relative">
                    <label
                      htmlFor="lastName"
                      className={`absolute left-4 transition-all duration-200 pointer-events-none ${
                        focusedField === 'lastName' || formData.lastName
                          ? '-top-2.5 text-xs text-emerald-400 bg-[#030712] px-2'
                          : 'top-3 text-gray-500'
                      }`}
                    >
                      Last Name
                    </label>
                    <input
                      id="lastName"
                      name="lastName"
                      type="text"
                      required
                      value={formData.lastName}
                      onChange={handleChange}
                      onFocus={() => setFocusedField('lastName')}
                      onBlur={() => setFocusedField(null)}
                      className="w-full px-4 py-3 bg-white/[0.03] border border-white/10 rounded-xl text-white placeholder-transparent focus:outline-none focus:border-emerald-500/50 focus:ring-2 focus:ring-emerald-500/20 transition-all duration-200"
                    />
                  </div>
                </div>

                {/* Username */}
                <div className="relative">
                  <label
                    htmlFor="username"
                    className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                      focusedField === 'username' || formData.username
                        ? '-top-2.5 left-4 text-xs text-emerald-400 bg-[#030712] px-2'
                        : 'top-3 text-gray-500'
                    }`}
                  >
                    Username
                  </label>
                  <div className="absolute left-3 top-3 text-gray-500">
                    <User className="h-4 w-4" />
                  </div>
                  <input
                    id="username"
                    name="username"
                    type="text"
                    autoComplete="username"
                    required
                    value={formData.username}
                    onChange={handleChange}
                    onFocus={() => setFocusedField('username')}
                    onBlur={() => setFocusedField(null)}
                    className="w-full pl-10 pr-4 py-3 bg-white/[0.03] border border-white/10 rounded-xl text-white placeholder-transparent focus:outline-none focus:border-emerald-500/50 focus:ring-2 focus:ring-emerald-500/20 transition-all duration-200"
                  />
                </div>

                {/* Email */}
                <div className="relative">
                  <label
                    htmlFor="email"
                    className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                      focusedField === 'email' || formData.email
                        ? '-top-2.5 left-4 text-xs text-emerald-400 bg-[#030712] px-2'
                        : 'top-3 text-gray-500'
                    }`}
                  >
                    Email address
                  </label>
                  <div className="absolute left-3 top-3 text-gray-500">
                    <Mail className="h-4 w-4" />
                  </div>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    autoComplete="email"
                    required
                    value={formData.email}
                    onChange={handleChange}
                    onFocus={() => setFocusedField('email')}
                    onBlur={() => setFocusedField(null)}
                    className="w-full pl-10 pr-4 py-3 bg-white/[0.03] border border-white/10 rounded-xl text-white placeholder-transparent focus:outline-none focus:border-emerald-500/50 focus:ring-2 focus:ring-emerald-500/20 transition-all duration-200"
                  />
                </div>

                {/* Organization */}
                <div className="relative">
                  <label
                    htmlFor="organizationName"
                    className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                      focusedField === 'organizationName' || formData.organizationName
                        ? '-top-2.5 left-4 text-xs text-emerald-400 bg-[#030712] px-2'
                        : 'top-3 text-gray-500'
                    }`}
                  >
                    Organization (optional)
                  </label>
                  <div className="absolute left-3 top-3 text-gray-500">
                    <Building2 className="h-4 w-4" />
                  </div>
                  <input
                    id="organizationName"
                    name="organizationName"
                    type="text"
                    value={formData.organizationName}
                    onChange={handleChange}
                    onFocus={() => setFocusedField('organizationName')}
                    onBlur={() => setFocusedField(null)}
                    className="w-full pl-10 pr-4 py-3 bg-white/[0.03] border border-white/10 rounded-xl text-white placeholder-transparent focus:outline-none focus:border-emerald-500/50 focus:ring-2 focus:ring-emerald-500/20 transition-all duration-200"
                  />
                </div>

                {/* Password */}
                <div className="relative">
                  <label
                    htmlFor="password"
                    className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                      focusedField === 'password' || formData.password
                        ? '-top-2.5 left-4 text-xs text-emerald-400 bg-[#030712] px-2'
                        : 'top-3 text-gray-500'
                    }`}
                  >
                    Password
                  </label>
                  <div className="absolute left-3 top-3 text-gray-500">
                    <Lock className="h-4 w-4" />
                  </div>
                  <input
                    id="password"
                    name="password"
                    type="password"
                    autoComplete="new-password"
                    required
                    value={formData.password}
                    onChange={handleChange}
                    onFocus={() => setFocusedField('password')}
                    onBlur={() => setFocusedField(null)}
                    className="w-full pl-10 pr-4 py-3 bg-white/[0.03] border border-white/10 rounded-xl text-white placeholder-transparent focus:outline-none focus:border-emerald-500/50 focus:ring-2 focus:ring-emerald-500/20 transition-all duration-200"
                  />
                </div>

                {/* Confirm Password */}
                <div className="relative">
                  <label
                    htmlFor="confirmPassword"
                    className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                      focusedField === 'confirmPassword' || formData.confirmPassword
                        ? '-top-2.5 left-4 text-xs text-emerald-400 bg-[#030712] px-2'
                        : 'top-3 text-gray-500'
                    }`}
                  >
                    Confirm Password
                  </label>
                  <div className="absolute left-3 top-3 text-gray-500">
                    <Lock className="h-4 w-4" />
                  </div>
                  <input
                    id="confirmPassword"
                    name="confirmPassword"
                    type="password"
                    autoComplete="new-password"
                    required
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    onFocus={() => setFocusedField('confirmPassword')}
                    onBlur={() => setFocusedField(null)}
                    className="w-full pl-10 pr-4 py-3 bg-white/[0.03] border border-white/10 rounded-xl text-white placeholder-transparent focus:outline-none focus:border-emerald-500/50 focus:ring-2 focus:ring-emerald-500/20 transition-all duration-200"
                  />
                  {passwordsMatch && (
                    <div className="absolute right-3 top-3 text-emerald-400">
                      <Check className="h-4 w-4" />
                    </div>
                  )}
                </div>

                {/* Submit button */}
                <button
                  type="submit"
                  disabled={loading}
                  className="group w-full relative overflow-hidden px-6 py-3.5 rounded-xl font-medium text-white transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed mt-6"
                >
                  {/* Button gradient background */}
                  <div className="absolute inset-0 bg-gradient-to-r from-emerald-600 to-emerald-500 transition-all duration-300 group-hover:from-emerald-500 group-hover:to-blue-500" />
                  {/* Button glow */}
                  <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 bg-gradient-to-r from-emerald-600/50 to-blue-600/50 blur-xl" />

                  <span className="relative flex items-center justify-center gap-2">
                    {loading ? (
                      <>
                        <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                        </svg>
                        Creating account...
                      </>
                    ) : (
                      <>
                        Create account
                        <ArrowRight className="h-4 w-4 group-hover:translate-x-1 transition-transform" />
                      </>
                    )}
                  </span>
                </button>
              </form>

              {/* Sign in link */}
              <p className="mt-6 text-center text-gray-400">
                Already have an account?{' '}
                <Link to="/login" className="text-emerald-400 hover:text-emerald-300 font-medium transition-colors">
                  Sign in
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
