import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Activity, ArrowRight, Building2, User, Mail, Lock, Check, Zap, Shield, BarChart3, Eye, EyeOff } from 'lucide-react';

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
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
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

  // Input field component for consistency
  const InputField = ({
    id,
    name,
    type = 'text',
    icon: Icon,
    label,
    required = true,
    autoComplete,
    showToggle = false,
    isVisible = false,
    onToggle,
  }: {
    id: string;
    name: string;
    type?: string;
    icon?: React.ComponentType<{ className?: string }>;
    label: string;
    required?: boolean;
    autoComplete?: string;
    showToggle?: boolean;
    isVisible?: boolean;
    onToggle?: () => void;
  }) => (
    <div className="relative">
      <label
        htmlFor={id}
        className={`absolute transition-all duration-200 pointer-events-none ${
          Icon ? 'left-10' : 'left-4'
        } ${
          focusedField === name || formData[name as keyof typeof formData]
            ? `-top-2.5 !left-4 text-xs font-medium text-[#0969da] px-2 rounded`
            : 'top-3.5 text-[#4a535c]'
        }`}
        style={focusedField === name || formData[name as keyof typeof formData] ? { backgroundColor: '#ffffff' } : {}}
      >
        {label}
      </label>
      {Icon && (
        <div className="absolute left-3.5 top-4 text-[#4a535c]">
          <Icon className="h-4 w-4" />
        </div>
      )}
      <input
        id={id}
        name={name}
        type={showToggle ? (isVisible ? 'text' : 'password') : type}
        autoComplete={autoComplete}
        required={required}
        value={formData[name as keyof typeof formData]}
        onChange={handleChange}
        onFocus={() => setFocusedField(name)}
        onBlur={() => setFocusedField(null)}
        className={`w-full py-3.5 rounded-xl text-[#1f2328] placeholder-transparent focus:outline-none focus:ring-2 focus:ring-[#0969da]/20 transition-all duration-200 ${
          Icon ? 'pl-10 pr-4' : 'px-4'
        } ${showToggle ? 'pr-12' : ''}`}
        style={{
          backgroundColor: '#ffffff',
          border: focusedField === name ? '1px solid #0969da' : '1px solid #d0d7de'
        }}
      />
      {showToggle && onToggle && (
        <button
          type="button"
          onClick={onToggle}
          className="absolute right-4 top-1/2 -translate-y-1/2 text-[#4a535c] hover:text-[#1f2328] transition-colors"
          aria-label={isVisible ? 'Hide password' : 'Show password'}
        >
          {isVisible ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
        </button>
      )}
      {name === 'confirmPassword' && passwordsMatch && (
        <div className="absolute right-4 top-1/2 -translate-y-1/2 text-[#1a7f37]">
          <Check className="h-5 w-5" />
        </div>
      )}
    </div>
  );

  return (
    <div className="min-h-screen relative overflow-hidden bg-[#f7f8fa]">
      {/* Elegant Light Background with Subtle Gradient Mesh */}
      <div className="absolute inset-0">
        {/* Primary gradient - soft blue-teal wash */}
        <div
          className="absolute inset-0"
          style={{
            background: `
              radial-gradient(ellipse 100% 100% at 0% 0%, rgba(9, 105, 218, 0.08), transparent 50%),
              radial-gradient(ellipse 80% 80% at 100% 0%, rgba(20, 184, 166, 0.06), transparent 50%),
              radial-gradient(ellipse 60% 60% at 50% 100%, rgba(139, 92, 246, 0.04), transparent 50%)
            `
          }}
        />

        {/* Floating accent orb - top left */}
        <div
          className="absolute -top-40 -left-40 w-[600px] h-[600px] rounded-full opacity-40 animate-pulse-slow"
          style={{
            background: 'radial-gradient(circle, rgba(9, 105, 218, 0.12) 0%, transparent 70%)',
            filter: 'blur(80px)',
          }}
        />

        {/* Floating accent orb - bottom right */}
        <div
          className="absolute -bottom-32 -right-32 w-[500px] h-[500px] rounded-full opacity-30 animate-pulse-slow"
          style={{
            background: 'radial-gradient(circle, rgba(20, 184, 166, 0.15) 0%, transparent 70%)',
            filter: 'blur(60px)',
            animationDelay: '1.5s',
          }}
        />

        {/* Subtle grid pattern */}
        <div
          className="absolute inset-0 opacity-[0.35]"
          style={{
            backgroundImage: `
              linear-gradient(rgba(0,0,0,0.015) 1px, transparent 1px),
              linear-gradient(90deg, rgba(0,0,0,0.015) 1px, transparent 1px)
            `,
            backgroundSize: '64px 64px'
          }}
        />
      </div>

      <div className="relative min-h-screen flex">
        {/* Left Panel - Hero Branding */}
        <div className="hidden lg:flex lg:w-[55%] xl:w-[58%] flex-col justify-between p-12 xl:p-16 2xl:p-20">
          {/* Logo */}
          <div className="flex items-center gap-3 group cursor-pointer">
            <div className="relative">
              <div className="absolute inset-0 bg-[#0969da]/20 blur-xl rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
              <div className="relative p-2.5 rounded-xl bg-gradient-to-br from-[#0969da]/10 to-[#14b8a6]/10 border border-[#0969da]/15 shadow-sm">
                <Activity className="h-6 w-6 text-[#0969da]" />
              </div>
            </div>
            <span className="text-xl font-semibold text-[#1f2328] tracking-tight">SensorVision</span>
          </div>

          {/* Hero Content */}
          <div className="max-w-xl -mt-16">
            {/* Eyebrow */}
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#0969da]/8 border border-[#0969da]/12 mb-6">
              <div className="w-1.5 h-1.5 rounded-full bg-[#14b8a6] animate-pulse" />
              <span className="text-xs font-medium text-[#0969da] uppercase tracking-wider">Get Started Free</span>
            </div>

            <h1 className="text-[clamp(2.75rem,4.5vw,4rem)] font-bold text-[#1f2328] leading-[1.08] tracking-[-0.025em] mb-6">
              Start your
              <br />
              <span className="bg-gradient-to-r from-[#0969da] via-[#14b8a6] to-[#0969da] bg-clip-text text-transparent bg-[length:200%_auto] animate-gradient">
                IoT journey
              </span>
            </h1>

            <p className="text-lg text-[#4a535c] leading-relaxed mb-10 max-w-md">
              Join thousands of engineers monitoring their devices with real-time
              analytics and intelligent alerting.
            </p>

            {/* Feature Cards - Horizontal layout */}
            <div className="flex flex-col sm:flex-row gap-4">
              {[
                { icon: Zap, label: 'Free tier', desc: 'No credit card', color: '#f59e0b' },
                { icon: Shield, label: 'SOC 2', desc: 'Compliant', color: '#10b981' },
                { icon: BarChart3, label: 'Unlimited', desc: 'Devices', color: '#0969da' },
              ].map(({ icon: Icon, label, desc, color }) => (
                <div
                  key={label}
                  className="group flex-1 p-4 rounded-2xl bg-white/70 border border-[#d0d7de]/60 hover:border-[#d0d7de] hover:bg-white/90 hover:shadow-lg transition-all duration-300 cursor-default backdrop-blur-sm"
                >
                  <div
                    className="w-10 h-10 rounded-xl flex items-center justify-center mb-3 transition-transform duration-300 group-hover:scale-110"
                    style={{ backgroundColor: `${color}12` }}
                  >
                    <Icon className="h-5 w-5" style={{ color }} />
                  </div>
                  <div className="text-sm font-semibold text-[#1f2328] mb-0.5">{label}</div>
                  <div className="text-xs text-[#4a535c]">{desc}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Bottom Stats */}
          <div className="flex items-center gap-12">
            {[
              { value: '10K+', label: 'Active Users' },
              { value: '50M+', label: 'Events/Month' },
              { value: '99.9%', label: 'Uptime' },
            ].map(({ value, label }) => (
              <div key={label} className="group cursor-default">
                <div
                  className="text-3xl font-bold text-[#1f2328] tracking-tight mb-1 transition-colors duration-300 group-hover:text-[#0969da]"
                  style={{ fontVariantNumeric: 'tabular-nums' }}
                >
                  {value}
                </div>
                <div className="text-sm text-[#4a535c] font-medium">{label}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Panel - Register Form */}
        <div className="w-full lg:w-[45%] xl:w-[42%] flex items-center justify-center p-6 sm:p-10 lg:p-12 overflow-y-auto">
          <div className="w-full max-w-[420px]">
            {/* Mobile Logo */}
            <div className="lg:hidden flex items-center justify-center gap-3 mb-8">
              <div className="p-2 rounded-xl bg-gradient-to-br from-[#0969da]/10 to-[#14b8a6]/10 border border-[#0969da]/15">
                <Activity className="h-6 w-6 text-[#0969da]" />
              </div>
              <span className="text-xl font-semibold text-[#1f2328]">SensorVision</span>
            </div>

            {/* Form Card */}
            <div
              className="relative rounded-3xl p-8 sm:p-10"
              style={{
                background: 'rgba(255, 255, 255, 0.85)',
                backdropFilter: 'blur(20px)',
                WebkitBackdropFilter: 'blur(20px)',
                border: '1px solid rgba(208, 215, 222, 0.6)',
                boxShadow: `
                  0 0 0 1px rgba(255, 255, 255, 0.8) inset,
                  0 1px 2px rgba(31, 35, 40, 0.04),
                  0 4px 16px rgba(31, 35, 40, 0.08),
                  0 12px 40px rgba(31, 35, 40, 0.12)
                `
              }}
            >
              {/* Header */}
              <div className="mb-6">
                <h2 className="text-2xl font-bold text-[#1f2328] tracking-tight mb-2">
                  Create your account
                </h2>
                <p className="text-[#4a535c]">
                  Start monitoring your IoT devices in minutes
                </p>
              </div>

              {/* Error Alert */}
              {error && (
                <div
                  role="alert"
                  aria-live="assertive"
                  className="mb-6 p-4 rounded-xl bg-[#ffebe9] border border-[#cf222e]/20 animate-fadeIn"
                >
                  <p className="text-sm text-[#a40e26] font-medium">{error}</p>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                {/* Name fields */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <InputField
                    id="firstName"
                    name="firstName"
                    label="First Name"
                    autoComplete="given-name"
                  />
                  <InputField
                    id="lastName"
                    name="lastName"
                    label="Last Name"
                    autoComplete="family-name"
                  />
                </div>

                {/* Username */}
                <InputField
                  id="username"
                  name="username"
                  label="Username"
                  icon={User}
                  autoComplete="username"
                />

                {/* Email */}
                <InputField
                  id="email"
                  name="email"
                  type="email"
                  label="Email address"
                  icon={Mail}
                  autoComplete="email"
                />

                {/* Organization */}
                <InputField
                  id="organizationName"
                  name="organizationName"
                  label="Organization (optional)"
                  icon={Building2}
                  required={false}
                />

                {/* Password */}
                <div className="relative">
                  <label
                    htmlFor="password"
                    className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                      focusedField === 'password' || formData.password
                        ? '-top-2.5 !left-4 text-xs font-medium text-[#0969da] px-2 rounded'
                        : 'top-3.5 text-[#4a535c]'
                    }`}
                    style={focusedField === 'password' || formData.password ? { backgroundColor: '#ffffff' } : {}}
                  >
                    Password
                  </label>
                  <div className="absolute left-3.5 top-4 text-[#4a535c]">
                    <Lock className="h-4 w-4" />
                  </div>
                  <input
                    id="password"
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    required
                    value={formData.password}
                    onChange={handleChange}
                    onFocus={() => setFocusedField('password')}
                    onBlur={() => setFocusedField(null)}
                    className="w-full pl-10 pr-12 py-3.5 rounded-xl text-[#1f2328] placeholder-transparent focus:outline-none focus:ring-2 focus:ring-[#0969da]/20 transition-all duration-200"
                    style={{
                      backgroundColor: '#ffffff',
                      border: focusedField === 'password' ? '1px solid #0969da' : '1px solid #d0d7de'
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-[#4a535c] hover:text-[#1f2328] transition-colors"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                  </button>
                </div>

                {/* Confirm Password */}
                <div className="relative">
                  <label
                    htmlFor="confirmPassword"
                    className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                      focusedField === 'confirmPassword' || formData.confirmPassword
                        ? '-top-2.5 !left-4 text-xs font-medium text-[#0969da] px-2 rounded'
                        : 'top-3.5 text-[#4a535c]'
                    }`}
                    style={focusedField === 'confirmPassword' || formData.confirmPassword ? { backgroundColor: '#ffffff' } : {}}
                  >
                    Confirm Password
                  </label>
                  <div className="absolute left-3.5 top-4 text-[#4a535c]">
                    <Lock className="h-4 w-4" />
                  </div>
                  <input
                    id="confirmPassword"
                    name="confirmPassword"
                    type={showConfirmPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    required
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    onFocus={() => setFocusedField('confirmPassword')}
                    onBlur={() => setFocusedField(null)}
                    className="w-full pl-10 pr-12 py-3.5 rounded-xl text-[#1f2328] placeholder-transparent focus:outline-none focus:ring-2 focus:ring-[#0969da]/20 transition-all duration-200"
                    style={{
                      backgroundColor: '#ffffff',
                      border: focusedField === 'confirmPassword' ? '1px solid #0969da' : '1px solid #d0d7de'
                    }}
                  />
                  {passwordsMatch ? (
                    <div className="absolute right-4 top-1/2 -translate-y-1/2 text-[#1a7f37]">
                      <Check className="h-5 w-5" />
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute right-4 top-1/2 -translate-y-1/2 text-[#4a535c] hover:text-[#1f2328] transition-colors"
                      aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                    >
                      {showConfirmPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                    </button>
                  )}
                </div>

                {/* Submit Button */}
                <button
                  type="submit"
                  disabled={loading}
                  aria-busy={loading}
                  className="group w-full relative overflow-hidden px-6 py-4 rounded-xl font-semibold text-white transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed mt-6 shadow-lg shadow-[#0969da]/25 hover:shadow-xl hover:shadow-[#14b8a6]/30 hover:scale-[1.02]"
                >
                  {/* Animated gradient background - matches "IoT journey" text */}
                  <div className="absolute inset-0 bg-gradient-to-r from-[#0969da] via-[#14b8a6] to-[#0969da] bg-[length:200%_auto] animate-gradient rounded-xl" />

                  {/* Shimmer effect on hover */}
                  <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500">
                    <div
                      className="absolute inset-0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-1000"
                      style={{
                        background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent)'
                      }}
                    />
                  </div>

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
                        <ArrowRight className="h-4 w-4 group-hover:translate-x-1 transition-transform duration-200" />
                      </>
                    )}
                  </span>
                </button>
              </form>

              {/* Sign in link */}
              <p className="mt-6 text-center text-[#4a535c]">
                Already have an account?{' '}
                <Link
                  to="/login"
                  className="text-[#0969da] hover:text-[#0550ae] font-semibold transition-colors"
                >
                  Sign in
                </Link>
              </p>
            </div>

            {/* Trust Badges */}
            <div className="mt-6 flex items-center justify-center gap-6 opacity-80">
              <div className="flex items-center gap-2 text-xs text-[#4a535c]">
                <Shield className="h-4 w-4" />
                <span>256-bit SSL</span>
              </div>
              <div className="w-px h-4 bg-[#d0d7de]" />
              <div className="flex items-center gap-2 text-xs text-[#4a535c]">
                <span>SOC 2 Compliant</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
